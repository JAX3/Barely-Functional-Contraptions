package com.jax3.barelyfunctionalcontraptions.mycelial.compat.jade;

import com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.mycelial.menu.GeneratorStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

/** Server half: a small snapshot of the generator's status (built from the same data as its GUI). */
public enum GeneratorDataProvider implements StreamServerDataProvider<BlockAccessor, GeneratorDataProvider.Data> {
    INSTANCE;

    public record Data(int state, double generation, String fuelItem, int burnRemaining) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    buf.writeVarInt(d.state);
                    buf.writeDouble(d.generation);
                    buf.writeUtf(d.fuelItem, 256);
                    buf.writeVarInt(d.burnRemaining);
                },
                buf -> new Data(buf.readVarInt(), buf.readDouble(), buf.readUtf(256), buf.readVarInt()));
    }

    @Override
    public @Nullable Data streamData(BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MycelialGeneratorBlockEntity generator)) return null;
        GeneratorStatus s = generator.createStatus();
        return new Data(s.state(), s.generation(), s.fuelItem(), s.burnRemaining());
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
        return Data.CODEC;
    }

    @Override
    public Identifier getUid() {
        return GeneratorInfoProvider.UID;
    }
}
