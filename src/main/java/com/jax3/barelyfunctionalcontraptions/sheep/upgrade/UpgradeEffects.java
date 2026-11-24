package com.jax3.barelyfunctionalcontraptions.sheep.upgrade;

import net.minecraft.util.Mth;

/**
 * What a machine's installed upgrades do: multipliers on each job's time and on its total FE, on its energy buffer and
 * input rate ({@link #capacity}), and blocks added to its working area on every side ({@link #range}). {@link #NONE}
 * with no cards in.
 */
public record UpgradeEffects(double time, double energy, double capacity, int range) {
    public static final UpgradeEffects NONE = new UpgradeEffects(1, 1, 1, 0);

    /** A job of {@code ticks} ticks with the upgrades: at least 1. */
    public int ticks(int ticks) {
        return Math.max(1, Mth.ceil(ticks * time - 1e-9));
    }

    /** A job costing {@code total} FE with the upgrades. */
    public long energy(long total) {
        return total <= 0 ? total : Math.max(0, (long) Math.ceil(total * energy - 1e-9));
    }

    /** As {@link #energy(long)}, for an int amount (capped). */
    public int energy(int total) {
        return (int) Math.min(Integer.MAX_VALUE, energy((long) total));
    }

    /**
     * FE a tick for a machine that charges by the tick (the Powered Furnace): {@code perTick} becomes enough that the
     * job, now {@link #time} as long, costs {@link #energy} times what it did.
     */
    public int perTick(int perTick) {
        return perTick <= 0 ? perTick : Math.max(1, (int) Math.round(perTick * energy / time));
    }

    /** An energy buffer or input rate of {@code amount} with the upgrades (capped). */
    public int scaled(int amount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(amount * capacity));
    }

    public boolean isNone() {
        return time == 1 && energy == 1 && capacity == 1 && range == 0;
    }
}
