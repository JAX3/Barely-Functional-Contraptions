package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What a Wool-Lined Crate holds, kept on its item when it's broken: the kind of item (one of it), how many, whether it's
 * locked to that item, and how many Wool Bales are packed into it.
 */
public record CrateContents(ItemStack item, int count, boolean locked, int bales) {
    public static final CrateContents EMPTY = new CrateContents(ItemStack.EMPTY, 0, false, 0);

    public static final Codec<CrateContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(CrateContents::item),
            Codec.INT.optionalFieldOf("count", 0).forGetter(CrateContents::count),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(CrateContents::locked),
            Codec.INT.optionalFieldOf("bales", 0).forGetter(CrateContents::bales)
    ).apply(i, CrateContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrateContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, CrateContents::item,
            ByteBufCodecs.VAR_INT, CrateContents::count,
            ByteBufCodecs.BOOL, CrateContents::locked,
            ByteBufCodecs.VAR_INT, CrateContents::bales,
            CrateContents::new);

    public CrateContents {
        item = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1);
    }

    /** Whether there's anything worth keeping: items, a lock or bales. */
    public boolean isEmpty() {
        return count <= 0 && !locked && bales <= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CrateContents c && count == c.count && locked == c.locked && bales == c.bales
                && ItemStack.isSameItemSameComponents(item, c.item);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(ItemStack.hashItemAndComponents(item), count, locked, bales);
    }
}
