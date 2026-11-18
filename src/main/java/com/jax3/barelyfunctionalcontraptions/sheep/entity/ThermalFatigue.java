package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How worn a sheep's fleece is from heating or chilling a Thermal Reaction Chamber, in ticks of reacting. Once it
 * reaches {@code [thermal_reaction_chamber] fatigueTicks} the fleece is spent: the sheep no longer counts until it is
 * infused again (at the Infusion Altar or by hand), which starts it fresh.
 */
public record ThermalFatigue(int ticks) {
    public static final ThermalFatigue FRESH = new ThermalFatigue(0);
    public static final MapCodec<ThermalFatigue> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("ticks", 0).forGetter(ThermalFatigue::ticks)
    ).apply(i, ThermalFatigue::new));

    public ThermalFatigue add(int amount) {
        return new ThermalFatigue((int) Math.min(Integer.MAX_VALUE, (long) ticks + amount));
    }

    /** Whether the fleece is worn out. Never, while {@code fatigueTicks} is 0. */
    public boolean spent() {
        int limit = ModConfigs.getInt(ModConfigs.SERVER.thermalFatigueTicks);
        return limit > 0 && ticks >= limit;
    }
}
