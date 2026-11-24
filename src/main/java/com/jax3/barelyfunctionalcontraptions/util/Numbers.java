package com.jax3.barelyfunctionalcontraptions.util;

import java.util.Locale;

/** Number formats shared by windows, tooltips, JEI and Jade. */
public final class Numbers {
    private Numbers() {}

    /** An amount of FE, mB or items with thousands separators: 12,480. */
    public static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
