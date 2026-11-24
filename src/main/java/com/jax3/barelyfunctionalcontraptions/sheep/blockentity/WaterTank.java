package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A machine's water tank: water only, in only (buckets, pipes and a Fluid Reservoir fill it; the machine uses it with
 * {@link #drain}). Its capacity follows the config ({@link #resize}).
 */
public class WaterTank extends FluidStacksResourceHandler {
    public static final FluidResource WATER = FluidResource.of(Fluids.WATER);
    private final Runnable onChanged;

    public WaterTank(int capacity, Runnable onChanged) {
        super(1, capacity);
        this.onChanged = onChanged;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.equals(WATER);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onChanged.run();
    }

    public int getAmount() {
        return getAmountAsInt(0);
    }

    public int capacity() {
        return capacity;
    }

    /** Adds up to {@code amount} mB, as much as fits. */
    public void fill(int amount) {
        int added = Math.max(0, Math.min(amount, capacity - getAmount()));
        if (added > 0) set(0, WATER, getAmount() + added);
    }

    /** Empties a water bucket into it, if a whole bucket fits. */
    public boolean fillFromBucket() {
        if (capacity - getAmount() < 1000) return false;
        fill(1000);
        return true;
    }

    /** Uses {@code amount} mB, or all it has if that's less. */
    public void drain(int amount) {
        int left = Math.max(0, getAmount() - amount);
        set(0, left > 0 ? WATER : FluidResource.EMPTY, left);
    }

    /** Sets its capacity, losing any water over it. */
    public void resize(int newCapacity) {
        if (newCapacity == capacity) return;
        capacity = newCapacity;
        if (getAmount() > capacity) set(0, WATER, capacity);
    }
}
