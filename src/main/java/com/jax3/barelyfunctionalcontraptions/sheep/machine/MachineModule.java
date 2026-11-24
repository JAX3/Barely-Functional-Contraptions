package com.jax3.barelyfunctionalcontraptions.sheep.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One part of a machine that a {@link MachineFrame} looks after: an {@link EnergyModule}, a {@link FluidModule}, the
 * upgrade slots or a {@link MachineInventory}. A machine installs the modules it needs; the frame saves and loads them,
 * keeps them on the machine's item, syncs them to clients and drops what they hold when the machine is broken. Each
 * hook does nothing unless the module needs it.
 */
public interface MachineModule {
    /** Saves it with the machine (under the keys the machine has always used, so old worlds still load). */
    default void save(ValueOutput output) {}

    default void load(ValueInput input) {}

    /** What it keeps on the machine's item when it's broken or picked up. */
    default void collectComponents(DataComponentMap.Builder components) {}

    /** Takes back what {@link #collectComponents} kept, when the machine is placed again. */
    default void applyComponents(DataComponentGetter components) {}

    /** Drops from the saved data what's kept as a component instead. */
    default void removeComponents(ValueOutput output) {}

    /** What clients need (the outline, sounds), added to the machine's update tag. */
    default void writeSync(CompoundTag tag) {}

    /** Reads what {@link #writeSync} wrote, on a client. */
    default void readSync(ValueInput input) {}

    /** The machine was broken: drop what it holds that isn't kept on its item. */
    default void dropContents(Level level, BlockPos pos) {}
}
