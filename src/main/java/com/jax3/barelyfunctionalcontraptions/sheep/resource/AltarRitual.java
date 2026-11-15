package com.jax3.barelyfunctionalcontraptions.sheep.resource;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * How a resource is infused at an Infusion Altar, from the {@code "altar"} block of its definition:
 *
 * <pre>{@code
 * "altar": {
 *   "tier": 2,                                                      // above [infusion_altar] handFeedingMaxTier (0) it can't be fed by hand
 *   "catalysts": [ { "ingredient": "minecraft:soul_sand", "count": 4 } ],  // up to 4, used up
 *   "energy": 200000                                                // FE, optional
 * }
 * }</pre>
 *
 * Every resource can be infused at an altar (tier 1 with no catalysts by default); only tier
 * those above {@code [infusion_altar] handFeedingMaxTier} must be (0 by default, so every resource).
 */
public record AltarRitual(int tier, List<SizedIngredient> catalysts, Optional<Integer> energy) {
    public static final int MAX_CATALYSTS = 4;
    public static final AltarRitual NONE = new AltarRitual(1, List.of(), Optional.empty());

    public static final Codec<AltarRitual> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(AltarRitual::tier),
            SizedIngredient.NESTED_CODEC.listOf(0, MAX_CATALYSTS).optionalFieldOf("catalysts", List.of()).forGetter(AltarRitual::catalysts),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("energy").forGetter(AltarRitual::energy)
    ).apply(i, AltarRitual::new));

    /** Whether this resource can only be infused at an altar, not by hand. */
    public boolean altarOnly() {
        return tier > ModConfigs.getInt(ModConfigs.SERVER.handFeedingMaxTier);
    }

    /** FE for one infusion: the definition's {@code energy}, else the config's default for its tier. */
    public int effectiveEnergy() {
        return energy.orElseGet(() -> tier >= 2 ? ModConfigs.getInt(ModConfigs.SERVER.altarHighTierEnergy) : ModConfigs.getInt(ModConfigs.SERVER.altarBaseEnergy));
    }
}
