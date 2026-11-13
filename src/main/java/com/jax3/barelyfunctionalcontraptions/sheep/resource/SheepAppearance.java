package com.jax3.barelyfunctionalcontraptions.sheep.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/**
 * How an infused sheep looks and behaves beyond its wool colours: the optional {@code "appearance"} block of a
 * resource definition.
 *
 * <pre>{@code
 * "appearance": {
 *   "wool_texture": "barelyfunctionalcontraptions:textures/entity/sheep/hay_wool.png",
 *   "features": ["extra_heads", "glowing"],
 *   "feature_color": "#2B2B2B"
 * }
 * }</pre>
 *
 * @param woolTexture  fleece texture used instead of the greyscale infused wool (still tinted with {@code wool_color};
 *                     use {@code "#FFFFFF"} to show it as drawn). It uses the vanilla sheep wool layout (64x32).
 * @param features     extra body parts and behaviours, see {@link Feature}
 * @param featureColor tint for the extra body parts (wings, legs, tentacles, bones); white shows them as drawn
 */
public record SheepAppearance(Optional<Identifier> woolTexture, List<Feature> features, int featureColor) {
    public static final SheepAppearance NONE = new SheepAppearance(Optional.empty(), List.of(), 0xFFFFFF);

    public static final Codec<SheepAppearance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.optionalFieldOf("wool_texture").forGetter(SheepAppearance::woolTexture),
            Feature.CODEC.listOf().optionalFieldOf("features", List.of()).forGetter(SheepAppearance::features),
            SheepResource.COLOR_CODEC.optionalFieldOf("feature_color", 0xFFFFFF).forGetter(SheepAppearance::featureColor)
    ).apply(i, SheepAppearance::new));

    public boolean has(Feature feature) {
        return features.contains(feature);
    }

    public boolean isEmpty() {
        return woolTexture.isEmpty() && features.isEmpty();
    }

    /**
     * Something extra an infused sheep grows or does while it has its infused fleece. Lambs and sheared sheep show
     * none of them.
     */
    public enum Feature implements StringRepresentable {
        /** Two Wither Skeleton Skulls (the game's own texture, untinted) either side of its own head, like the Wither. */
        EXTRA_HEADS,
        /** A pair of flapping dragon wings on its back. */
        WINGS,
        /** Four long spider legs along its sides. */
        EXTRA_LEGS,
        /** Ghast tentacles hanging under its belly. */
        TENTACLES,
        /** Glowing eyes (in the accent colour) that show in the dark. */
        GLOWING_EYES,
        /** A bony skull face and bony legs. */
        BONES,
        /** Sculk tendrils on its head, and fleece strands that glow like sculk. */
        SCULK,
        /** Fleece that glows in the dark and lights up the block it stands in (light level 12). */
        GLOWING,
        /** Looks like it is on fire, without burning or taking damage. */
        ON_FIRE,
        /** Bleats with a screaming goat's voice. */
        SCREAMING;

        public static final Codec<Feature> CODEC = StringRepresentable.fromEnum(Feature::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
