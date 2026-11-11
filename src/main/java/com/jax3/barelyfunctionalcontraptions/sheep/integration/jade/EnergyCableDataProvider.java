package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CableNetwork;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCableBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

/** Server half of the Energy Cable tooltip: the network's size, how many blocks are plugged in and its FE/t. */
public enum EnergyCableDataProvider implements StreamServerDataProvider<BlockAccessor, EnergyCableDataProvider.Data> {
    INSTANCE;

    public record Data(int cables, int endpoints, int rate, int maxRate) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::cables,
                ByteBufCodecs.VAR_INT, Data::endpoints,
                ByteBufCodecs.VAR_INT, Data::rate,
                ByteBufCodecs.VAR_INT, Data::maxRate,
                Data::new);
    }

    @Override
    public @Nullable Data streamData(BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof EnergyCableBlockEntity be)) return null;
        CableNetwork network = be.network();
        return new Data(network.size(), network.endpoints().size(), network.rate(), CableNetwork.transferRate());
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
        return Data.CODEC;
    }

    @Override
    public Identifier getUid() {
        return EnergyCableProvider.UID;
    }
}
