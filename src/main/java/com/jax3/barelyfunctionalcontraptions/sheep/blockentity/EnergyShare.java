package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

/**
 * Spreads a job's FE over its ticks so the total comes out exact. When {@code total} doesn't divide evenly by
 * {@code ticks}, the first {@code total % ticks} ticks take one FE more: 1,000 FE over 30 ticks is 34 FE for 10 ticks
 * and 33 for the other 20, never 34 x 30 = 1,020.
 */
public final class EnergyShare {
    private EnergyShare() {}

    /** FE for tick {@code tick} (0-based) of a job taking {@code total} FE over {@code ticks} ticks. */
    public static int forTick(long total, int ticks, int tick) {
        if (ticks <= 0 || total <= 0) return 0;
        long base = total / ticks;
        return (int) Math.min(Integer.MAX_VALUE, base + (tick < total % ticks ? 1 : 0));
    }
}
