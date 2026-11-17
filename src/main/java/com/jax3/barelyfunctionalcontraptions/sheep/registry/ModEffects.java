package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.armor.MuffledEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Mob effects. Icons are in textures/mob_effect. */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, ResourcefulSheep.MOD_ID);

    /** Makes no sound and sets off no sculk; Woollen Boots give it while worn. */
    public static final DeferredHolder<MobEffect, MuffledEffect> MUFFLED = REGISTER.register("muffled", MuffledEffect::new);

    private ModEffects() {}
}
