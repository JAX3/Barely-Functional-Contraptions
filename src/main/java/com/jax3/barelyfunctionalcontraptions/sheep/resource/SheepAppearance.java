package com.jax3.barelyfunctionalcontraptions.sheep.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
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
 * @param scale        how big the sheep is while infused, as a multiple of its normal size (0.25 to 4; default 1).
 *                     Applied through the {@code minecraft:scale} attribute, so its hitbox, eye height and step
 *                     height grow with it.
 * @param skullTexture a mob-head texture (the 64x32 layout skulls use, e.g. the skeleton's) drawn untinted over the
 *                     sheep's head, so its face is that mob's skull
 * @param sounds       sounds played instead of the sheep's bleat, hurt and death sounds while it is infused (even when
 *                     sheared)
 */
public record SheepAppearance(Optional<Identifier> woolTexture, List<Feature> features, int featureColor, double scale,
                              Optional<Identifier> skullTexture, Sounds sounds) {
    public static final double MIN_SCALE = 0.25;
    public static final double MAX_SCALE = 4.0;
    public static final SheepAppearance NONE = new SheepAppearance(Optional.empty(), List.of(), 0xFFFFFF, 1.0, Optional.empty(), Sounds.NONE);

    public static final Codec<SheepAppearance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.optionalFieldOf("wool_texture").forGetter(SheepAppearance::woolTexture),
            Feature.CODEC.listOf().optionalFieldOf("features", List.of()).forGetter(SheepAppearance::features),
            SheepResource.COLOR_CODEC.optionalFieldOf("feature_color", 0xFFFFFF).forGetter(SheepAppearance::featureColor),
            Codec.doubleRange(MIN_SCALE, MAX_SCALE).optionalFieldOf("scale", 1.0).forGetter(SheepAppearance::scale),
            Identifier.CODEC.optionalFieldOf("skull_texture").forGetter(SheepAppearance::skullTexture),
            Sounds.CODEC.optionalFieldOf("sounds", Sounds.NONE).forGetter(SheepAppearance::sounds)
    ).apply(i, SheepAppearance::new));

    /** The same appearance with another fleece texture. */
    public SheepAppearance withWoolTexture(Identifier texture) {
        return new SheepAppearance(Optional.of(texture), features, featureColor, scale, skullTexture, sounds);
    }

    public boolean has(Feature feature) {
        return features.contains(feature);
    }

    public boolean isEmpty() {
        return woolTexture.isEmpty() && features.isEmpty() && scale == 1.0 && skullTexture.isEmpty() && sounds.isEmpty();
    }

    /**
     * Sounds that replace the sheep's own: any sound event id, vanilla or modded, e.g.
     * {@code "sounds": {"ambient": "minecraft:entity.ghast.scream"}}. Each one left out keeps the sheep's sound (or
     * the screaming goat's, with the {@code screaming} feature).
     */
    public record Sounds(Optional<Holder<SoundEvent>> ambient, Optional<Holder<SoundEvent>> hurt, Optional<Holder<SoundEvent>> death) {
        public static final Sounds NONE = new Sounds(Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<Sounds> CODEC = RecordCodecBuilder.create(i -> i.group(
                SoundEvent.CODEC.optionalFieldOf("ambient").forGetter(Sounds::ambient),
                SoundEvent.CODEC.optionalFieldOf("hurt").forGetter(Sounds::hurt),
                SoundEvent.CODEC.optionalFieldOf("death").forGetter(Sounds::death)
        ).apply(i, Sounds::new));

        public boolean isEmpty() {
            return ambient.isEmpty() && hurt.isEmpty() && death.isEmpty();
        }
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
        /** Bleats, hurts and dies with a screaming goat's sounds (while infused, even when sheared). */
        SCREAMING;

        public static final Codec<Feature> CODEC = StringRepresentable.fromEnum(Feature::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
