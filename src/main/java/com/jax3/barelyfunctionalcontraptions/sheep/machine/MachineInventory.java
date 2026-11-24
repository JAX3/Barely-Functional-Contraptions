package com.jax3.barelyfunctionalcontraptions.sheep.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A machine's internal item slots, each with its own rules: what may go in (by hand, hopper or pipe) and whether
 * hoppers and pipes may take it out. Built with {@link #builder}. Saved the way the machine always saved them: each slot
 * under its own name ({@code input}, {@code output}...), or all together as an item list. Its contents drop when the
 * machine is broken. A machine using it implements {@link InventoryHolder}, so hoppers see it as a container.
 */
public final class MachineInventory implements MachineModule {
    /** What one slot takes: {@code accepts} what may be put in; {@code extractable} whether hoppers and pipes take it out. */
    public record Slot(@Nullable String name, Predicate<ItemStack> accepts, boolean extractable) {}

    /** Called when a slot's stack is replaced (the old stack, the new one). */
    @FunctionalInterface
    public interface Listener {
        void replaced(int slot, ItemStack old, ItemStack now);
    }

    private final List<Slot> slots;
    private final NonNullList<ItemStack> items;
    private final boolean named;
    private final int[] all;
    private final Listener listener;

    private MachineInventory(List<Slot> slots, boolean named, Listener listener) {
        this.slots = List.copyOf(slots);
        this.items = NonNullList.withSize(slots.size(), ItemStack.EMPTY);
        this.named = named;
        this.listener = listener;
        this.all = new int[slots.size()];
        for (int i = 0; i < all.length; i++) all[i] = i;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int size() {
        return items.size();
    }

    /** The stack in {@code slot} (the live one: changing it changes the slot), or empty for a slot it doesn't have. */
    public ItemStack get(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    /** Puts {@code stack} in {@code slot}, telling the machine's listener; ignores slots it doesn't have. */
    public void set(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) return;
        ItemStack old = items.get(slot);
        items.set(slot, stack);
        listener.replaced(slot, old, stack);
    }

    /** Whether {@code stack} may go in {@code slot}. */
    public boolean accepts(int slot, ItemStack stack) {
        return slot >= 0 && slot < slots.size() && slots.get(slot).accepts().test(stack);
    }

    /** Whether hoppers and pipes may take from {@code slot}. */
    public boolean extractable(int slot) {
        return slot >= 0 && slot < slots.size() && slots.get(slot).extractable();
    }

    public int[] allSlots() {
        return all;
    }

    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public void save(ValueOutput output) {
        if (!named) {
            ContainerHelper.saveAllItems(output, items);
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) output.store(slots.get(i).name(), ItemStack.OPTIONAL_CODEC, items.get(i));
        }
    }

    @Override
    public void load(ValueInput input) {
        if (!named) {
            for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
            return;
        }
        for (int i = 0; i < items.size(); i++) items.set(i, input.read(slots.get(i).name(), ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, items);
    }

    public static final class Builder {
        private final List<Slot> slots = new ArrayList<>();
        private boolean named = true;
        private Listener listener = (slot, old, now) -> {};

        /** A slot things go in (and hoppers and pipes don't take out), saved as {@code name}. */
        public Builder input(String name, Predicate<ItemStack> accepts) {
            slots.add(new Slot(name, accepts, false));
            return this;
        }

        /** A slot only the machine fills, that hoppers and pipes take from, saved as {@code name}. */
        public Builder output(String name) {
            slots.add(new Slot(name, stack -> false, true));
            return this;
        }

        /** Any slot. */
        public Builder slot(@Nullable String name, Predicate<ItemStack> accepts, boolean extractable) {
            slots.add(new Slot(name, accepts, extractable));
            return this;
        }

        /** Saves the slots together as an item list (as vanilla containers do) instead of one name each. */
        public Builder savedAsList() {
            named = false;
            return this;
        }

        public Builder onReplaced(Listener listener) {
            this.listener = listener;
            return this;
        }

        public MachineInventory build() {
            return new MachineInventory(slots, named, listener);
        }
    }
}
