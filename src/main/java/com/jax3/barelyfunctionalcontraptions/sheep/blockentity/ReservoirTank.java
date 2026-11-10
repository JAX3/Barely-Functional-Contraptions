package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Fluid Reservoir's water tank. Water only, and output only: buckets, pipes and neighbours can take water out but
 * nothing can be poured in. The reservoir fills it itself with {@link #generate(int)}.
 */
public class ReservoirTank extends FluidStacksResourceHandler {
    public static final FluidResource WATER = FluidResource.of(Fluids.WATER);
    private final Runnable onChanged;

    public ReservoirTank(int capacity, Runnable onChanged) {
        super(1, capacity);
        this.onChanged = onChanged;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.equals(WATER);
    }

    /** Nothing can be poured in from outside. */
    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onChanged.run();
    }

    /** Adds up to {@code amount} mB of water. Returns the amount added. */
    public int generate(int amount) {
        int added = Math.max(0, Math.min(amount, capacity - getAmount()));
        if (added > 0) set(0, WATER, getAmount() + added);
        return added;
    }

    /** Removes up to {@code amount} mB right away (bucket filling). Returns the amount removed. */
    public int drain(int amount) {
        int removed = Math.max(0, Math.min(amount, getAmount()));
        if (removed > 0) {
            int left = getAmount() - removed;
            set(0, left > 0 ? WATER : FluidResource.EMPTY, left);
        }
        return removed;
    }

    /** Applies a changed config capacity, discarding water above a smaller one. */
    public void setCapacity(int newCapacity) {
        if (newCapacity == capacity) return;
        capacity = newCapacity;
        if (getAmount() > capacity) set(0, WATER, capacity);
    }

    public int getAmount() {
        return getAmountAsInt(0);
    }

    public int getCapacity() {
        return capacity;
    }
}
