package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The item slots of a machine's block entity, built on two methods it gives: {@link #getItem} (empty for a slot it
 * doesn't have) and {@link #putItem} (just stores the stack). The rest of {@link Container} (taking items out, setting,
 * clearing, emptiness and who may use it) follows from those, telling the machine through {@link #contentsChanged}.
 */
public interface MachineContainer extends Container {
    /** Stores {@code stack} in {@code slot} (nothing else: no change is signalled); ignores slots it doesn't have. */
    void putItem(int slot, ItemStack stack);

    /** Its items changed (by default, marks the block entity changed). */
    default void contentsChanged() {
        setChanged();
    }

    @Override
    default boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = stack.split(count);
        if (stack.isEmpty()) putItem(slot, ItemStack.EMPTY);
        contentsChanged();
        return taken;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = getItem(slot);
        putItem(slot, ItemStack.EMPTY);
        return taken;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        putItem(slot, stack);
        contentsChanged();
    }

    @Override
    default void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) putItem(slot, ItemStack.EMPTY);
        contentsChanged();
    }

    @Override
    default boolean stillValid(Player player) {
        return this instanceof BlockEntity be && Container.stillValidBlockEntity(be, player);
    }
}
