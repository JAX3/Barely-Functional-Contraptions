package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolPackerBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.WoolPackerMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * The Wool Packer: presses 9 of the same resource wool from its input slot into one Wool Bale in its output slot.
 *
 * <ul>
 *     <li>Each bale takes {@code [wool_packer] energyPerBale} FE, spread over {@code ticksPerBale} ticks; progress
 *     pauses without power.</li>
 *     <li>It only starts a bale once 9 wool are in and the bale will fit, so nothing is lost or duplicated.</li>
 *     <li>Hoppers and pipes put wool in on any side and take bales out on any side. A comparator reads the input.</li>
 * </ul>
 */
public class WoolPackerBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame
        implements com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable, com.jax3.barelyfunctionalcontraptions.sheep.machine.InventoryHolder, MenuProvider {
    public static final int INPUT = 0, OUTPUT = 1;
    public static final int DATA_COUNT = 9;

    // Its modules: power in on every side, two upgrade slots, and a slot for resource wool and one for bales.
    private final MachineEnergy energy = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.EnergyModule.of(new MachineEnergy(
            () -> ModConfigs.getInt(ModConfigs.SERVER.woolPackerCapacity), () -> ModConfigs.getInt(ModConfigs.SERVER.woolPackerMaxInput), this::setChanged))).handler();
    private final com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades = install(new com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades(this));
    private final com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory.builder()
            .input("input", WoolPackerBlockEntity::isPackable)
            .output("output")
            .onReplaced((slot, old, now) -> {
                if (slot == INPUT && !ItemStack.isSameItemSameComponents(old, now)) progress = 0; // different wool starts over
            })
            .build());
    private int progress;
    private int maxProgress;
    private int energyPerTick;
    private MachineStatus status = MachineStatus.IDLE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> Math.min(0xFFFF, progress);
                case 5 -> Math.min(0xFFFF, maxProgress);
                case 6 -> energyPerTick & 0xFFFF;
                case 7 -> energyPerTick >>> 16;
                case 8 -> status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative; the client-side menu keeps its own SimpleContainerData copy.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public WoolPackerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOL_PACKER.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, WoolPackerBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.refreshLimits(0, upgrades.effects().capacity()); // a Capacity card holds and takes in more
        var fx = upgrades.effects();
        maxProgress = fx.ticks(ModConfigs.getInt(ModConfigs.SERVER.woolPackerTicksPerBale));
        int cost = fx.energy(ModConfigs.getInt(ModConfigs.SERVER.woolPackerEnergyPerBale));
        energyPerTick = (cost + maxProgress - 1) / maxProgress;

        ItemStack input = inventory.get(INPUT), output = inventory.get(OUTPUT);
        ItemStack bale = WoolBaleItem.of(input);
        if (input.isEmpty()) {
            status = MachineStatus.IDLE;
            progress = 0;
        } else if (bale.isEmpty() || !knownWool(input)) {
            status = MachineStatus.INVALID_INPUT;
            progress = 0;
        } else if (input.getCount() < WoolBaleItem.WOOL_PER_BALE) {
            status = MachineStatus.IDLE;
            progress = 0;
        } else if (!fits(output, bale)) {
            status = MachineStatus.OUTPUT_FULL;
        } else if (energy.getAmountAsInt() < EnergyShare.forTick(cost, maxProgress, progress)) {
            status = MachineStatus.NO_ENERGY;
        } else {
            status = MachineStatus.PROCESSING;
            energy.drain(EnergyShare.forTick(cost, maxProgress, progress));
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                input.shrink(WoolBaleItem.WOOL_PER_BALE);
                if (input.isEmpty()) inventory.set(INPUT, ItemStack.EMPTY);
                if (output.isEmpty()) inventory.set(OUTPUT, bale);
                else output.grow(1);
            }
            setChanged();
        }
        boolean active = status == MachineStatus.PROCESSING;
        if (state.getValue(WoolPackerBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(WoolPackerBlock.ACTIVE, active), Block.UPDATE_ALL);
    }

    private static boolean fits(ItemStack output, ItemStack bale) {
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, bale) && output.getCount() < output.getMaxStackSize());
    }

    /** Resource wool of a resource the server knows. */
    private static boolean knownWool(ItemStack stack) {
        Identifier id = ResourceWoolItem.getResourceId(stack);
        return id != null && SheepResourceRegistry.server().byId(id).isPresent();
    }

    // ------------------------------------------------------------------ accessors

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    public MachineStatus status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public int maxProgress() {
        return maxProgress;
    }

    public ItemStack input() {
        return inventory.get(INPUT);
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory() {
        return inventory;
    }

    public ItemStack output() {
        return inventory.get(OUTPUT);
    }

    public int getComparatorSignal() {
        ItemStack input = inventory.get(INPUT);
        if (input.isEmpty()) return 0;
        return Math.max(1, Math.round(15.0F * input.getCount() / input.getMaxStackSize()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.wool_packer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WoolPackerMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ its wool (the inventory module's rule)

    /** Resource wool goes in the input; the output is the packer's own. */
    public static boolean isPackable(ItemStack stack) {
        return stack.getItem() instanceof ResourceWoolItem && ResourceWoolItem.getResourceId(stack) != null;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); // its modules: energy, upgrades, input and output
        progress = in.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); // its modules
        out.putInt("progress", progress);
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades() {
        return upgrades;
    }
}
