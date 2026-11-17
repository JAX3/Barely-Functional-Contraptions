package com.jax3.barelyfunctionalcontraptions.sheep.armor;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModEffects;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Woollen Boots give Muffled while worn, and Muffled silences whoever has it:
 * <ul>
 *   <li>sounds the entity plays (footsteps, landing, hurting, eating, swimming...) are cancelled, on the server for
 *   everyone else and on the wearer's own client for themselves;</li>
 *   <li>its vibrations (game events) don't reach sculk sensors, shriekers or Wardens.</li>
 * </ul>
 */
public final class MuffledEvents {
    /** How long one top-up of Muffled lasts, and how often the boots top it up. */
    static final int DURATION = 40, REFRESH = 10;
    /** How close a positioned sound must be to an entity's feet to count as that entity's sound. */
    private static final double SAME_SPOT = 1.0E-3;

    private MuffledEvents() {}

    public static boolean isMuffled(Entity entity) {
        return entity instanceof LivingEntity living && living.hasEffect(ModEffects.MUFFLED);
    }

    public static boolean wearsWoollenBoots(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WOOLLEN_BOOTS.get());
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) return;
        if (living instanceof ArmorStand || living.tickCount % REFRESH != 0 || !living.isAlive()) return;
        if (!ModConfigs.SERVER.woollenBootsMuffle.get() || !wearsWoollenBoots(living)) return;
        MobEffectInstance current = living.getEffect(ModEffects.MUFFLED);
        if (current != null && current.getDuration() > REFRESH && !current.isAmbient()) return; // a longer one from elsewhere
        // Ambient and without particles, like a beacon's effects; the icon still shows.
        living.addEffect(new MobEffectInstance(ModEffects.MUFFLED, DURATION, 0, true, false, true));
    }

    /** A sound played from an entity: {@code level.playSound(except, entity, ...)}. */
    @SubscribeEvent
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (isMuffled(event.getEntity())) event.setCanceled(true);
    }

    /**
     * A sound played at a position. Entities play their own sounds (steps, hurt, eating...) at their exact
     * position, so a sound right at a muffled entity's feet is its own.
     */
    @SubscribeEvent
    public static void onSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        Level level = event.getLevel();
        Vec3 pos = event.getPosition();
        AABB spot = new AABB(pos, pos).inflate(SAME_SPOT);
        if (!level.getEntitiesOfClass(LivingEntity.class, spot,
                e -> e.position().distanceToSqr(pos) < SAME_SPOT * SAME_SPOT && isMuffled(e)).isEmpty()) {
            event.setCanceled(true);
        }
    }

    /** Vibrations: a muffled entity's steps, landings, eating and the like don't reach sculk or Wardens. */
    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (ModConfigs.SERVER.muffleVibrations.get() && isMuffled(event.getCause())) event.setCanceled(true);
    }
}
