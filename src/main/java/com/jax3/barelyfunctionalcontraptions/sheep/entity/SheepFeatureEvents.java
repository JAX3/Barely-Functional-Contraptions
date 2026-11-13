package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * What a resource's {@link SheepAppearance.Feature features} do in the world, beyond how the sheep is drawn:
 * glowing sheep light up the block they stand in, screaming sheep bleat like a screaming goat, and burning, glowing
 * and sculk sheep give off the matching particles.
 *
 * <p>Features only apply to grown sheep that still have their infused fleece, the same as how they're drawn.</p>
 */
public final class SheepFeatureEvents {
    private SheepFeatureEvents() {}

    /** How often a glowing sheep refreshes its light, in ticks. */
    public static final int GLOW_PLACE_TICKS = 5;

    private static final Map<SoundEvent, SoundEvent> SCREAMS = Map.of(
            SoundEvents.SHEEP_AMBIENT, SoundEvents.GOAT_SCREAMING_AMBIENT,
            SoundEvents.SHEEP_HURT, SoundEvents.GOAT_SCREAMING_HURT,
            SoundEvents.SHEEP_DEATH, SoundEvents.GOAT_SCREAMING_DEATH);

    /** The appearance of the sheep's resource, if it is a grown sheep that currently shows it. */
    public static Optional<SheepAppearance> appearance(Entity entity) {
        if (!(entity instanceof Sheep sheep) || sheep.isSheared() || sheep.isBaby()) return Optional.empty();
        if (!entity.hasData(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.SHEEP_INFUSION)) return Optional.empty();
        return SheepInfusionEvents.getInfusedResource(entity).map(SheepResource::appearance);
    }

    public static boolean has(Entity entity, Feature feature) {
        return appearance(entity).map(a -> a.has(feature)).orElse(false);
    }

    /** The {@code minecraft:scale} modifier an infused resource's {@code scale} adds. */
    public static final net.minecraft.resources.Identifier SCALE_MODIFIER = com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep.id("resource_scale");

    /**
     * Sizes the sheep for its infusion: a resource's {@code scale} is added to the sheep's {@code minecraft:scale}
     * attribute as a multiplier of its base value (so it stacks with anything else that changes the scale, and lambs
     * stay in proportion). The modifier is saved with the sheep, so it doesn't shrink for a moment after loading, and
     * it is removed as soon as the infusion ends or its resource is no longer loaded.
     *
     * <p>Unlike the looks, the size depends only on the infusion, not on the fleece: shearing doesn't shrink it.</p>
     */
    public static void updateScale(Sheep sheep) {
        var attribute = sheep.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (attribute == null) return;
        double scale = SheepInfusionEvents.getInfusedResource(sheep).map(r -> r.appearance().scale()).orElse(1.0);
        var current = attribute.getModifier(SCALE_MODIFIER);
        if (scale == 1.0) {
            if (current != null) attribute.removeModifier(SCALE_MODIFIER);
        } else if (current == null || current.amount() != scale - 1.0) {
            attribute.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    SCALE_MODIFIER, scale - 1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Sheep sheep) || entity.tickCount % GLOW_PLACE_TICKS != 0) return;
        if (!sheep.level().isClientSide() && sheep.hasData(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.SHEEP_INFUSION)) {
            updateScale(sheep);
        } else if (!sheep.level().isClientSide()) {
            // No infusion data at all (it's never been infused, or the data was removed): just clear any leftover size.
            var attribute = sheep.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
            if (attribute != null && attribute.getModifier(SCALE_MODIFIER) != null) attribute.removeModifier(SCALE_MODIFIER);
        }
        Optional<SheepAppearance> appearance = appearance(sheep);
        if (appearance.isEmpty() || appearance.get().features().isEmpty()) return;
        Level level = sheep.level();
        if (!level.isClientSide()) {
            if (appearance.get().has(Feature.GLOWING)) placeGlow(level, sheep.blockPosition());
            return;
        }
        if (sheep.isInvisible()) return;
        var random = sheep.getRandom();
        if (appearance.get().has(Feature.ON_FIRE) && random.nextInt(3) == 0) {
            level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME,
                    sheep.getRandomX(0.5), sheep.getY() + sheep.getBbHeight() * (0.4 + random.nextDouble() * 0.6), sheep.getRandomZ(0.5), 0, 0.03, 0);
        }
        if (appearance.get().has(Feature.GLOWING) && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.GLOW, sheep.getRandomX(0.6), sheep.getY() + sheep.getBbHeight() * (0.5 + random.nextDouble() * 0.5), sheep.getRandomZ(0.6), 0, 0.02, 0);
        }
        if (appearance.get().has(Feature.SCULK) && random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.SCULK_SOUL, sheep.getRandomX(0.5), sheep.getY() + sheep.getBbHeight(), sheep.getRandomZ(0.5), 0, 0.03, 0);
        }
    }

    /** Lights the block the sheep stands in, unless something other than air (or an older glow) is there. */
    public static void placeGlow(Level level, BlockPos pos) {
        BlockState there = level.getBlockState(pos);
        if (there.isAir() && level.getFluidState(pos).isEmpty()) {
            level.setBlock(pos, ModBlocks.SHEEP_GLOW.get().defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    /** Swaps a screaming sheep's bleats for a screaming goat's. Sheep sounds are played at the sheep's position. */
    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent.AtPosition event) {
        Holder<SoundEvent> sound = event.getSound();
        if (sound == null) return;
        SoundEvent scream = SCREAMS.get(sound.value());
        if (scream == null) return;
        Vec3 at = event.getPosition();
        boolean screaming = !event.getLevel().getEntitiesOfClass(Sheep.class, new AABB(at, at).inflate(0.01),
                sheep -> sheep.position().distanceToSqr(at) < 1.0E-4 && has(sheep, Feature.SCREAMING)).isEmpty();
        if (screaming) event.setSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(scream));
    }
}
