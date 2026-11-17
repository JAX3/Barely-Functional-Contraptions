package com.jax3.barelyfunctionalcontraptions.sheep.armor;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Muffled: the entity makes no sound, and sculk sensors and Wardens can't hear it. The work is in {@link MuffledEvents}. */
public final class MuffledEffect extends MobEffect {
    public static final int COLOUR = 0xD8D2C4;

    public MuffledEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOUR);
    }
}
