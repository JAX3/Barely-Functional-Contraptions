package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolPackerBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.WoolPackerMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jspecify.annotations.Nullable;

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
public class WoolPackerBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int INPUT = 0, OUTPUT = 1;
    public static final int DATA_COUNT = 9;
    private static final int[] SLOTS = {INPUT, OUTPUT};

    private final PackerEnergy energy = new PackerEnergy();
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
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
        energy.refreshLimits();
        maxProgress = ModConfigs.getInt(ModConfigs.SERVER.woolPackerTicksPerBale);
        int cost = ModConfigs.getInt(ModConfigs.SERVER.woolPackerEnergyPerBale);
        energyPerTick = (cost + maxProgress - 1) / maxProgress;

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
        } else if (!fits(bale)) {
            status = MachineStatus.OUTPUT_FULL;
        } else if (energy.getAmountAsInt() < energyPerTick) {
            status = MachineStatus.NO_ENERGY;
        } else {
            status = MachineStatus.PROCESSING;
            energy.consume(energyPerTick);
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                input.shrink(WoolBaleItem.WOOL_PER_BALE);
                if (input.isEmpty()) input = ItemStack.EMPTY;
                if (output.isEmpty()) output = bale;
                else output.grow(1);
            }
            setChanged();
        }
        boolean active = status == MachineStatus.PROCESSING;
        if (state.getValue(WoolPackerBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(WoolPackerBlock.ACTIVE, active), Block.UPDATE_ALL);
    }

    private boolean fits(ItemStack bale) {
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
        return input;
    }

    public ItemStack output() {
        return output;
    }

    public int getComparatorSignal() {
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

    // ------------------------------------------------------------------ container

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty() && output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == INPUT ? input : slot == OUTPUT ? output : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = stack.split(count);
        if (stack.isEmpty()) setItem(slot, ItemStack.EMPTY);
        setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == INPUT) {
            if (!ItemStack.isSameItemSameComponents(input, stack)) progress = 0;
            input = stack;
        } else if (slot == OUTPUT) {
            output = stack;
        }
        setChanged();
    }

    /** Resource wool goes in the input; the output is the packer's own. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT && isPackable(stack);
    }

    public static boolean isPackable(ItemStack stack) {
        return stack.getItem() instanceof ResourceWoolItem && ResourceWoolItem.getResourceId(stack) != null;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        input = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** Hoppers and pipes take bales only, never the wool waiting to be pressed. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, this);
    }

    // ------------------------------------------------------------------ energy

    private final class PackerEnergy extends SimpleEnergyHandler {
        PackerEnergy() {
            super(ModConfigs.getInt(ModConfigs.SERVER.woolPackerCapacity), ModConfigs.getInt(ModConfigs.SERVER.woolPackerMaxInput), 0);
        }

        void refreshLimits() {
            capacity = ModConfigs.getInt(ModConfigs.SERVER.woolPackerCapacity);
            maxInsert = ModConfigs.getInt(ModConfigs.SERVER.woolPackerMaxInput);
            if (energy > capacity) set(capacity);
        }

        void consume(int amount) {
            set(Math.max(0, energy - amount));
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(energy::deserialize);
        input = in.read("input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        progress = in.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        energy.serialize(out.child("energy"));
        if (!input.isEmpty()) out.store("input", ItemStack.OPTIONAL_CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.OPTIONAL_CODEC, output);
        out.putInt("progress", progress);
    }
}
