package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How tired a sheep is from the Sheep Treadmill: {@code walked} ticks on the belt since its last rest, and, once it's
 * worn out, the game time it may walk again ({@code restUntil}, 0 when it isn't resting).
 */
public record TreadmillWalker(int walked, long restUntil) {
    public static final TreadmillWalker FRESH = new TreadmillWalker(0, 0);
    public static final MapCodec<TreadmillWalker> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("walked", 0).forGetter(TreadmillWalker::walked),
            Codec.LONG.optionalFieldOf("rest_until", 0L).forGetter(TreadmillWalker::restUntil)
    ).apply(i, TreadmillWalker::new));

    public boolean resting(long gameTime) {
        return restUntil > gameTime;
    }
}
