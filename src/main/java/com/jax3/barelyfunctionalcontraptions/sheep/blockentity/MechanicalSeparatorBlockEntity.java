package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.MechanicalSeparatorBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.MechanicalSeparatorMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.Separation;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Mechanical Separator logic: consumes FE to turn resource wool (or any item with a separating recipe) into its
 * extracted material plus string.
 *
 * <p>Progress pauses (or resets, per config) without power, and the machine never starts or finishes an operation
 * unless every output is guaranteed to fit, so items are never voided or duplicated.</p>
 */
public class MechanicalSeparatorBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame implements com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable, MenuProvider {
    /** Number of int fields synced to the menu (split into 16-bit halves where needed). */
    public static final int DATA_COUNT = 13;

    // Its modules: two upgrade slots, its four item slots (with sides from the config) and power in on the configured sides.
    private final com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades = install(new com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades(this));
    private final SeparatorInventory inventory = install(new SeparatorInventory(this::isProcessable, this::onContentsChanged));
    private final MachineEnergy energy = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.EnergyModule.of(new MachineEnergy(
            () -> ModConfigs.getInt(ModConfigs.SERVER.energyCapacity), () -> ModConfigs.getInt(ModConfigs.SERVER.maxEnergyInput), this::setChanged))
            .onSides((state, side) -> ModConfigs.SERVER.sides(ModConfigs.SERVER.energyInputSides)
                    .contains(RelativeSide.of(state.getValue(MechanicalSeparatorBlock.FACING), side)))).handler();
    private final SidedItemHandler[] sidedHandlers = {
            new SidedItemHandler(inventory, false, false),
            new SidedItemHandler(inventory, true, false),
            new SidedItemHandler(inventory, false, true),
            new SidedItemHandler(inventory, true, true)};

    private int progress;
    private int maxProgress;
    private int energyPerTick;
    private MachineStatus status = MachineStatus.IDLE;
    private RedstoneMode redstoneMode = ModConfigs.get(ModConfigs.SERVER.defaultRedstoneMode);
    private boolean powered;
    private ItemStack lastInput = ItemStack.EMPTY;
    private boolean inventoryDirty = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress & 0xFFFF;
                case 1 -> progress >>> 16;
                case 2 -> maxProgress & 0xFFFF;
                case 3 -> maxProgress >>> 16;
                case 4 -> energy.getEnergy() & 0xFFFF;
                case 5 -> energy.getEnergy() >>> 16;
                case 6 -> energy.getCapacityAsInt() & 0xFFFF;
                case 7 -> energy.getCapacityAsInt() >>> 16;
                case 8 -> energyPerTick & 0xFFFF;
                case 9 -> energyPerTick >>> 16;
                case 10 -> status.ordinal();
                case 11 -> redstoneMode.ordinal();
                case 12 -> powered ? 1 : 0;
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

    public MechanicalSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MECHANICAL_SEPARATOR.get(), pos, state);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, MechanicalSeparatorBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        // What it used last tick (the wool or bale in it now); a Capacity card holds and takes in more.
        energy.refreshLimits(energyPerTick, upgrades.effects().capacity());
        powered = level.hasNeighborSignal(pos);
        RedstoneMode mode = ModConfigs.getBool(ModConfigs.SERVER.redstoneControlEnabled) ? redstoneMode : ModConfigs.get(ModConfigs.SERVER.defaultRedstoneMode);

        ItemStack input = inventory.getStack(SeparatorInventory.INPUT);
        if (!ItemStack.isSameItemSameComponents(input, lastInput)) {
            // Different item inserted: never carry progress over to another recipe.
            progress = 0;
            lastInput = input.copyWithCount(1);
        }

        MachineStatus newStatus;
        Optional<Separation> separation = input.isEmpty() ? Optional.empty() : SeparationLookup.find(level, input);
        if (input.isEmpty()) {
            newStatus = MachineStatus.IDLE;
            progress = 0;
            maxProgress = 0;
            energyPerTick = 0;
        } else if (separation.isEmpty()) {
            newStatus = MachineStatus.INVALID_INPUT;
            progress = 0;
            maxProgress = 0;
            energyPerTick = 0;
        } else {
            Separation sep = separation.get();
            // Its upgrade cards change the job's time and total FE.
            var fx = upgrades.effects();
            maxProgress = fx.ticks(sep.effectiveTicks());
            long total = fx.energy(sep.totalEnergy());
            energyPerTick = (int) Math.min(Integer.MAX_VALUE, (total + maxProgress - 1) / maxProgress);
            int thisTick = EnergyShare.forTick(total, maxProgress, progress);
            if (!mode.allows(powered)) {
                newStatus = MachineStatus.REDSTONE_DISABLED;
            } else if (!outputsFit(sep)) {
                newStatus = MachineStatus.OUTPUT_FULL;
            } else if (energy.getEnergy() < thisTick) {
                newStatus = MachineStatus.NO_ENERGY;
                if (ModConfigs.getBool(ModConfigs.SERVER.resetProgressWhenUnpowered)) progress = 0;
            } else {
                newStatus = MachineStatus.PROCESSING;
                energy.consume(thisTick);
                progress++;
                if (progress >= maxProgress) {
                    finish(sep, level.getRandom());
                    progress = 0;
                }
                setChanged();
            }
        }
        status = newStatus;

        boolean active = status == MachineStatus.PROCESSING;
        if (state.getValue(MechanicalSeparatorBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(MechanicalSeparatorBlock.ACTIVE, active), 3);
        }

        if (ModConfigs.getBool(ModConfigs.SERVER.autoPushOutputs)
                && level.getGameTime() % ModConfigs.getInt(ModConfigs.SERVER.autoPushInterval) == 0) {
            pushOutputs(level, pos, state);
        }

        if (inventoryDirty) {
            inventoryDirty = false;
            setChanged();
        }
    }

    private boolean outputsFit(Separation sep) {
        // Room for the most a job can give, Yield cards included, so a job never finishes into a full slot.
        int extra = upgrades.effects().maxExtra();
        ItemStack primary = sep.primary().create();
        if (inventory.spaceFor(SeparatorInventory.PRIMARY, primary) < sep.maxPrimaryCount() + extra) return false;
        if (sep.secondary().isPresent()) {
            ItemStack secondary = sep.secondary().get().create();
            if (inventory.spaceFor(SeparatorInventory.SECONDARY, secondary) < secondary.getCount() + extra) return false;
        }
        if (sep.bonus().isPresent()) {
            ItemStack bonus = sep.bonus().get().item().create();
            if (inventory.spaceFor(SeparatorInventory.BONUS, bonus) < bonus.getCount()) return false;
        }
        return true;
    }

    private void finish(Separation sep, RandomSource random) {
        var fx = upgrades.effects();
        int primaryCount = sep.rollPrimaryCount(random) + fx.extra(random); // a Yield card adds to it
        // outputsFit() guaranteed room for maxPrimaryCount plus the most a Yield card adds, an upper bound of primaryCount.
        if (primaryCount > 0) inventory.addOutput(SeparatorInventory.PRIMARY, sep.primary().create().copyWithCount(primaryCount));
        sep.secondary().ifPresent(s -> {
            ItemStack secondary = s.create();
            inventory.addOutput(SeparatorInventory.SECONDARY, secondary.copyWithCount(secondary.getCount() + fx.extra(random)));
        });
        sep.bonus().ifPresent(b -> {
            if (random.nextFloat() < b.chance()) inventory.addOutput(SeparatorInventory.BONUS, b.item().create());
        });
        ItemStack input = inventory.getStack(SeparatorInventory.INPUT);
        inventory.setStack(SeparatorInventory.INPUT, input.getCount() > 1 ? input.copyWithCount(input.getCount() - 1) : ItemStack.EMPTY);
    }

    private void pushOutputs(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(MechanicalSeparatorBlock.FACING);
        for (RelativeSide side : ModConfigs.SERVER.sides(ModConfigs.SERVER.itemOutputSides)) {
            Direction dir = side.toDirection(facing);
            ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target == null) continue;
            for (int slot = SeparatorInventory.PRIMARY; slot < SeparatorInventory.SIZE; slot++) {
                ItemResource resource = inventory.getResource(slot);
                int amount = inventory.getAmountAsInt(slot);
                if (resource.isEmpty() || amount <= 0) continue;
                try (Transaction tx = Transaction.openRoot()) {
                    int inserted = target.insert(resource, amount, tx);
                    if (inserted > 0 && inventory.extract(slot, resource, inserted, tx) == inserted) {
                        tx.commit();
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ capabilities

    /** Item handler for a side, honouring the configured input/output sides. {@code null} side = full access. */
    @Override
    public @Nullable ResourceHandler<ItemResource> itemsFor(@Nullable Direction side) {
        if (side == null) return sidedHandlers[3];
        RelativeSide rel = RelativeSide.of(getBlockState().getValue(MechanicalSeparatorBlock.FACING), side);
        Set<RelativeSide> inputs = ModConfigs.SERVER.sides(ModConfigs.SERVER.itemInputSides);
        Set<RelativeSide> outputs = ModConfigs.SERVER.sides(ModConfigs.SERVER.itemOutputSides);
        boolean in = inputs.contains(rel), out = outputs.contains(rel);
        if (!in && !out) return null;
        // Any side pipes can reach may also give up the outputs (a pipe on its top, say), unless that's switched off.
        if (ModConfigs.getBool(ModConfigs.SERVER.extractFromAnySide)) out = true;
        return sidedHandlers[(in ? 1 : 0) + (out ? 2 : 0)];
    }

    public @Nullable ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return itemsFor(side);
    }

    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyFor(side);
    }

    // ------------------------------------------------------------------ accessors

    public SeparatorInventory getInventory() {
        return inventory;
    }

    public MachineEnergy getEnergy() {
        return energy;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    /** Ticks the current operation takes (0 when idle). */
    public int getMaxProgress() {
        return maxProgress;
    }

    /** FE drawn per processing tick for the current input (0 when idle). */
    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstoneMode() {
        if (!ModConfigs.getBool(ModConfigs.SERVER.redstoneControlEnabled)) return;
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    private boolean isProcessable(ItemStack stack) {
        return level != null && SeparationLookup.canProcess(level, stack);
    }

    private void onContentsChanged() {
        inventoryDirty = true;
        if (level != null && !level.isClientSide()) setChanged();
    }

    public int getComparatorSignal() {
        ItemStack input = inventory.getStack(SeparatorInventory.INPUT);
        if (input.isEmpty()) return 0;
        return Math.max(1, Math.round(15.0F * input.getCount() / input.getMaxStackSize()));
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); // its modules: upgrades, inventory, energy
        output.putInt("progress", progress);
        output.putInt("redstone_mode", redstoneMode.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); // its modules
        progress = input.getIntOr("progress", 0);
        redstoneMode = RedstoneMode.byId(input.getIntOr("redstone_mode", redstoneMode.ordinal()));
        lastInput = inventory.getStack(SeparatorInventory.INPUT).copyWithCount(1);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components); // its cards
        if (energy.getEnergy() > 0 || redstoneMode != RedstoneMode.IGNORED) {
            components.set(ModDataComponents.MACHINE_CONTENTS.get(), new MachineContents(energy.getEnergy(), redstoneMode));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components); // its cards first
        energy.refreshLimits(0, upgrades.effects().capacity()); // so the energy it kept fits a Capacity card's buffer
        MachineContents contents = components.get(ModDataComponents.MACHINE_CONTENTS.get());
        if (contents != null) {
            energy.set(Math.min(contents.energy(), energy.getCapacityAsInt()));
            redstoneMode = contents.redstoneMode();
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("energy");
        output.discard("redstone_mode");
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.mechanical_separator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MechanicalSeparatorMenu(containerId, playerInventory, this, data);
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades() {
        return upgrades;
    }

    // ------------------------------------------------------------------ client sync (its upgrade cards: Muffler)

    // Its update tag is the frame's: just what its upgrade cards do to it there (a Muffler silences it).

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ValueInput input) {
        upgrades.readSync(input);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        upgrades.readSync(input);
    }
}
