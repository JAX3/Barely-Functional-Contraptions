package com.jax3.barelyfunctionalcontraptions.sheep.furnace;

import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepFeatureEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * One heat source for the Blaze Sheep Furnace, loaded from
 * {@code data/<namespace>/barelyfunctionalcontraptions/furnace_heat/<name>.json}. Each file names exactly one of:
 *
 * <ul>
 *     <li>{@code "sheep"}: a sheep resource id, e.g. {@code "barelyfunctionalcontraptions:blaze"};</li>
 *     <li>{@code "entity"}: an entity type, a list of them, or an entity tag ({@code "#namespace:tag"});</li>
 *     <li>{@code "feature"}: an appearance feature, e.g. {@code "on_fire"}, so any infused sheep that shows it counts.</li>
 * </ul>
 *
 * <pre>{@code
 * { "sheep": "barelyfunctionalcontraptions:blaze", "power": 80, "wet_multiplier": 0.5 }
 * }</pre>
 *
 * <p>Infused sheep only count while they show their infused fleece: grown and not sheared. When more than one file
 * matches an entity, the most specific one is used: {@code sheep} over {@code entity} over {@code feature}. Data packs
 * replace a built-in source by using the same file path, and switch one off with {@code "power": 0}.</p>
 *
 * @param sheep         the sheep resource this applies to
 * @param entity        the entity types this applies to
 * @param feature       the appearance feature this applies to
 * @param power         FE/t one of these adds to a furnace (before the falloff for extra heat sources)
 * @param wetMultiplier what its power is multiplied by while it is in water or rain (default 0.5)
 */
public record FurnaceHeat(Optional<Identifier> sheep, Optional<HolderSet<EntityType<?>>> entity,
                          Optional<SheepAppearance.Feature> feature, int power, double wetMultiplier) {

    public static final Codec<FurnaceHeat> CODEC = RecordCodecBuilder.<FurnaceHeat>create(i -> i.group(
            Identifier.CODEC.optionalFieldOf("sheep").forGetter(FurnaceHeat::sheep),
            RegistryCodecs.holderSet(Registries.ENTITY_TYPE).optionalFieldOf("entity").forGetter(FurnaceHeat::entity),
            SheepAppearance.Feature.CODEC.optionalFieldOf("feature").forGetter(FurnaceHeat::feature),
            Codec.intRange(0, 1_000_000).fieldOf("power").forGetter(FurnaceHeat::power),
            Codec.doubleRange(0, 100).optionalFieldOf("wet_multiplier", 0.5).forGetter(FurnaceHeat::wetMultiplier)
    ).apply(i, FurnaceHeat::new)).validate(FurnaceHeat::validate);

    private static DataResult<FurnaceHeat> validate(FurnaceHeat heat) {
        int named = (heat.sheep.isPresent() ? 1 : 0) + (heat.entity.isPresent() ? 1 : 0) + (heat.feature.isPresent() ? 1 : 0);
        return named == 1 ? DataResult.success(heat)
                : DataResult.error(() -> "a furnace heat source needs exactly one of \"sheep\", \"entity\" or \"feature\"");
    }

    /** How specific this source is: lower wins when several match the same entity. */
    public int specificity() {
        return sheep.isPresent() ? 0 : entity.isPresent() ? 1 : 2;
    }

    /** True if {@code entity} is this kind of heat source right now. */
    public boolean matches(Entity entity) {
        if (!entity.isAlive()) return false;
        if (sheep.isPresent()) {
            // Only while it shows its infused fleece (grown, not sheared), like its other looks.
            return SheepFeatureEvents.appearance(entity).isPresent()
                    && SheepInfusionEvents.getInfusedResource(entity).map(SheepResource::id).filter(sheep.get()::equals).isPresent();
        }
        if (this.entity.isPresent()) {
            return entity instanceof LivingEntity && this.entity.get().contains(entity.getType().builtInRegistryHolder());
        }
        return feature.isPresent() && SheepFeatureEvents.has(entity, feature.get());
    }

    /** FE/t this source gives for {@code entity}, counting water and rain. */
    public double powerFor(Entity entity) {
        return entity.isInWaterOrRain() ? power * wetMultiplier : power;
    }
}
