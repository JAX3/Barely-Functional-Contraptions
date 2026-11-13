package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * The player whose skin a sheep wears, stored on the sheep as a data attachment (saved and synced to clients).
 *
 * <p>Set by feeding the sheep a Melon Slice, which gives it the skin and head of one particular player
 * ({@link SheepInfusionEvents#MELON_SKIN_PLAYER}). Clients look the skin up by that profile, the same way a placed
 * Player Head does.</p>
 *
 * @param profile the player, empty when the sheep wears no skin
 */
public record SheepSkin(Optional<ResolvableProfile> profile) {
    public static final SheepSkin NONE = new SheepSkin(Optional.empty());

    public static final MapCodec<SheepSkin> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResolvableProfile.CODEC.optionalFieldOf("profile").forGetter(SheepSkin::profile)
    ).apply(i, SheepSkin::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SheepSkin> STREAM_CODEC = StreamCodec.composite(
            ResolvableProfile.STREAM_CODEC.apply(ByteBufCodecs::optional), SheepSkin::profile,
            SheepSkin::new);

    public static SheepSkin of(ResolvableProfile profile) {
        return new SheepSkin(Optional.of(profile));
    }
}
