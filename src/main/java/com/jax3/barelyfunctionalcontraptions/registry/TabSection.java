package com.jax3.barelyfunctionalcontraptions.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Marks a placeholder stack in a sectioned creative tab: {@code section} is the section whose banner row it belongs to
 * (columns 0-8), or empty for a blank that pads the end of a section to a whole row; {@code index} keeps each
 * placeholder unique, since a tab can't hold two equal stacks.
 */
public record TabSection(String section, int index) {
    public static final Codec<TabSection> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("section").forGetter(TabSection::section),
            Codec.INT.fieldOf("index").forGetter(TabSection::index)
    ).apply(i, TabSection::new));
    public static final StreamCodec<ByteBuf, TabSection> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TabSection::section, ByteBufCodecs.VAR_INT, TabSection::index, TabSection::new);

    public boolean isBanner() {
        return !section.isEmpty();
    }
}
