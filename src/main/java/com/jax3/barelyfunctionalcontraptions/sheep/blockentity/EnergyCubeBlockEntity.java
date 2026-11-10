package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * One Hume Energy Cube. It holds its own share of energy ({@code capacityPerCube}); the energy capability on every
 * face is a view of the whole {@link EnergyCubeGroup} it is connected to, so FE can go in and out on any side of any
 * cube and the group's buffer and transfer rate grow with every cube added.
 */
public class EnergyCubeBlockEntity extends BlockEntity {
    /** Ticks between updates of the charge shown on the blocks. */
    private static final int DISPLAY_INTERVAL = 10;

    private final CubeStorage storage = new CubeStorage();
    private @Nullable EnergyCubeGroup group;
    private final EnergyHandler view = new GroupView();
    private int ticks;

    public EnergyCubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HUME_ENERGY_CUBE.get(), pos, state);
    }

    /** This cube's own share of the energy. */
    public SimpleEnergyHandler storage() {
        return storage;
    }

    /** The group this cube belongs to, rebuilt from the world if it changed. */
    public EnergyCubeGroup group() {
        if (!hasValidGroup()) {
            EnergyCubeGroup.build(level, this);
        }
        return group;
    }

    boolean hasValidGroup() {
        return group != null && group.isValid();
    }

    void setGroup(EnergyCubeGroup group) {
        this.group = group;
    }

    /** Forgets the current group (all members rebuild it next time it is used). */
    public void invalidateGroup() {
        if (group != null) group.invalidate();
    }

    /** The capability handed to cables and machines on every face. */
    public EnergyHandler getEnergyHandler() {
        return view;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, EnergyCubeBlockEntity be) {
        be.storage.refreshLimits();
        EnergyCubeGroup group = be.group();
        if (group.leader() != be) return;
        if (ModConfigs.getBool(ModConfigs.SERVER.energyCubeAutoOutput)) group.push(level);
        if (++be.ticks % DISPLAY_INTERVAL == 0) {
            int charge = group.chargeLevel();
            for (EnergyCubeBlockEntity cube : group.members()) {
                BlockState current = cube.getBlockState();
                if (current.getValue(EnergyCubeBlock.CHARGE) != charge) {
                    level.setBlock(cube.getBlockPos(), current.setValue(EnergyCubeBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invalidateGroup();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        invalidateGroup();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storage.set(Math.min(input.getIntOr("energy", 0), storage.capacity()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", storage.amount());
    }

    /** A broken cube keeps its own share of the energy in the item. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (storage.amount() > 0) {
            components.set(ModDataComponents.MACHINE_CONTENTS.get(), new MachineContents(storage.amount(), RedstoneMode.IGNORED));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        MachineContents contents = components.get(ModDataComponents.MACHINE_CONTENTS.get());
        if (contents != null) storage.set(Math.min(contents.energy(), storage.capacity()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("energy");
    }

    /** One cube's buffer; the group applies the transfer rate, so the cube itself has no limit. */
    private final class CubeStorage extends SimpleEnergyHandler {
        CubeStorage() {
            super(ModConfigs.getInt(ModConfigs.SERVER.energyCubeCapacityPerCube), Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        void refreshLimits() {
            capacity = ModConfigs.getInt(ModConfigs.SERVER.energyCubeCapacityPerCube);
            if (energy > capacity) set(capacity);
        }

        int amount() {
            return energy;
        }

        int capacity() {
            return capacity;
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    }

    /** The whole group, seen through this cube. Resolves the group on every call, so it never goes stale. */
    private final class GroupView implements EnergyHandler {
        @Override
        public long getAmountAsLong() {
            return group().amount();
        }

        @Override
        public long getCapacityAsLong() {
            return group().capacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return amount <= 0 ? 0 : group().insert(amount, transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return amount <= 0 ? 0 : group().extract(amount, transaction);
        }
    }
}
