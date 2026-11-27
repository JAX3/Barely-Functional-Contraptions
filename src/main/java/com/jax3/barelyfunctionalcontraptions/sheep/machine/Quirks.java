package com.jax3.barelyfunctionalcontraptions.sheep.machine;

/**
 * The odds of the machines' random quirks (the separator jamming, the shears nicking a sheep, the Blaze Sheep Furnace
 * lighting a fire). The GameTest server turns them all off ({@code -Dbarelyfunctionalcontraptions.quirksOff=true}) so
 * the other tests stay steady; a test that checks a quirk sets it on its own machine with an override.
 */
public final class Quirks {
    private Quirks() {}

    private static final boolean OFF = Boolean.getBoolean("barelyfunctionalcontraptions.quirksOff");

    /** No override: the config's odds apply. */
    public static final double NONE = -1;

    /** The odds to use: the machine's own {@code override} if set (0 to 1), else none in tests, else {@code configured}. */
    public static double chance(double configured, double override) {
        if (override >= 0) return override;
        return OFF ? 0 : configured;
    }

    /** Whether the random quirks are off (in GameTests), for quirks that aren't a chance, such as a timer. */
    public static boolean off(double override) {
        return override < 0 && OFF;
    }
}
