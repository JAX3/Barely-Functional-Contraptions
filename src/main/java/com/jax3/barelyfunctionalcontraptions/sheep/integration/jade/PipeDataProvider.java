package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeNetwork;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

/** Server half of the Fluid and Item Pipe tooltip: the network's size, how many blocks are plugged in and its rate. */
public enum PipeDataProvider implements StreamServerDataProvider<BlockAccessor, PipeDataProvider.Data> {
    INSTANCE;

    /**
     * @param sides each side that isn't plain Normal: its direction, its mode and its filter
     */
    public record Data(int cables, int endpoints, int rate, int maxRate, java.util.List<Side> sides) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::cables,
                ByteBufCodecs.VAR_INT, Data::endpoints,
                ByteBufCodecs.VAR_INT, Data::rate,
                ByteBufCodecs.VAR_INT, Data::maxRate,
                Side.CODEC.apply(ByteBufCodecs.list()), Data::sides,
                Data::new);
    }

    public record Side(net.minecraft.core.Direction dir, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide side) {
        static final StreamCodec<RegistryFriendlyByteBuf, Side> CODEC = StreamCodec.composite(
                net.minecraft.core.Direction.STREAM_CODEC, Side::dir,
                ByteBufCodecs.fromCodec(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.CODEC), Side::side,
                Side::new);
    }

    @Override
    public @Nullable Data streamData(BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof PipeBlockEntity<?> be)) return null;
        PipeNetwork<?> network = be.network();
        java.util.List<Side> sides = new java.util.ArrayList<>();
        be.configuredSides().forEach((dir, side) -> sides.add(new Side(dir, side)));
        return new Data(network.size(), network.endpoints().size(), network.rate(), network.transferRate(), sides);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
        return Data.CODEC;
    }

    @Override
    public Identifier getUid() {
        return PipeProvider.UID;
    }
}
