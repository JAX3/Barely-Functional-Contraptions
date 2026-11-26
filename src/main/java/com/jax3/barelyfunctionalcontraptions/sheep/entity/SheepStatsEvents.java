package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Growth: a sheared sheep with Growth above 1 regrows its wool on its own, without needing grass. */
public final class SheepStatsEvents {
    private SheepStatsEvents() {}

    @SubscribeEvent
    public static void onSheepTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Sheep sheep) || !(sheep.level() instanceof ServerLevel level)) return;
        if ((sheep.tickCount + sheep.getId()) % 20 != 0 || !sheep.isSheared() || sheep.isBaby()) return; // once a second each
        double chance = SheepInfusionEvents.getStats(sheep).regrowChancePerSecond();
        // Explosive sheep regrow their wool quickly on their own (about every explosiveRegrowSeconds), so they're
        // ready to blast again: the Implosion Chamber runs on their pops.
        if (isExplosive(sheep)) {
            int seconds = com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.explosiveRegrowSeconds);
            if (seconds > 0) chance = 1.0 - (1.0 - chance) * (1.0 - 1.0 / seconds);
        }
        if (chance <= 0 || level.getRandom().nextDouble() >= chance) return;
        regrow(level, sheep);
    }

    /** Whether it's infused with a resource that has a fuse (sheared, so its looks don't show). */
    static boolean isExplosive(Sheep sheep) {
        return sheep.hasData(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.SHEEP_INFUSION)
                && SheepInfusionEvents.getInfusedResource(sheep).map(r -> r.appearance().has(
                        com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature.FUSE)).orElse(false);
    }

    static void regrow(ServerLevel level, Sheep sheep) {
        sheep.setSheared(false);
        level.playSound(null, sheep, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.6F, 1.2F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, sheep.getX(), sheep.getY() + sheep.getBbHeight() * 0.7, sheep.getZ(), 5, 0.35, 0.25, 0.35, 0.0);
    }
}
