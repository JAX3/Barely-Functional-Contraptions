package com.jax3.barelyfunctionalcontraptions.mycelial.mycelium;

/**
 * Pure rules for the escalating mycelium conversion probability. All probabilities are fractions
 * in {@code [0, 1]} (so 1% is {@code 0.01}).
 */
public final class MyceliumChance {
    /** Probabilities within this distance of 1.0 are treated as certain, removing floating point drift. */
    public static final double EPSILON = 1.0E-9;

    private MyceliumChance() {
    }

    public static double clamp(double value, double max) {
        double cap = Math.max(0.0D, Math.min(1.0D, max));
        if (Double.isNaN(value) || value < 0) {
            return 0.0D;
        }
        if (value >= cap - EPSILON) {
            return cap;
        }
        return value;
    }

    /**
     * The chance after one more unsuccessful eligible attempt.
     *
     * @param current      current chance
     * @param increase     configured increase per unsuccessful attempt (percentage points as a fraction)
     * @param growthFactor multiplier applied to the increase (fuel bonus, network bonus); 1 for none
     * @param max          maximum chance
     */
    public static double afterFailure(double current, double increase, double growthFactor, double max) {
        double step = Math.max(0.0D, increase) * Math.max(0.0D, growthFactor);
        if (!Double.isFinite(step)) {
            step = 1.0D;
        }
        return clamp(current + step, max);
    }

    /**
     * Decides whether an attempt succeeds. A chance of (effectively) 100% always succeeds.
     *
     * @param chance current chance
     * @param roll   a uniformly distributed random value in {@code [0, 1)}
     */
    public static boolean succeeds(double chance, double roll) {
        if (chance >= 1.0D - EPSILON) {
            return true;
        }
        if (chance <= 0) {
            return false;
        }
        return roll < chance;
    }

    /** The chance on the given (1-based) attempt assuming every earlier attempt failed. */
    public static double chanceOnAttempt(int attempt, double initial, double increase, double max) {
        double chance = clamp(initial, max);
        for (int i = 1; i < attempt; i++) {
            chance = afterFailure(chance, increase, 1.0D, max);
        }
        return chance;
    }
}
