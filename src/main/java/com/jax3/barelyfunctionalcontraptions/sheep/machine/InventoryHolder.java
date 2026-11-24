package com.jax3.barelyfunctionalcontraptions.sheep.machine;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A machine with a {@link MachineInventory}: it's the container hoppers, pipes and its window use, each call passed on
 * to the inventory and its slot rules.
 */
public interface InventoryHolder extends MachineContainer, WorldlyContainer {
    MachineInventory inventory();

    @Override
    default int getContainerSize() {
        return inventory().size();
    }

    @Override
    default ItemStack getItem(int slot) {
        return inventory().get(slot);
    }

    @Override
    default void putItem(int slot, ItemStack stack) {
        inventory().set(slot, stack);
    }

    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return inventory().accepts(slot, stack);
    }

    @Override
    default int[] getSlotsForFace(Direction side) {
        return inventory().allSlots();
    }

    @Override
    default boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    default boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return inventory().extractable(slot);
    }
}
