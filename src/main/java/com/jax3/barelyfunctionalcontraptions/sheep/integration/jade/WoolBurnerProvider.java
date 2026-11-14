package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

/** Wool Burner: what it is making and how long the item burning has left. Its energy and fuel are Jade's own bars. */
public final class WoolBurnerProvider {
    public static final Identifier UID = ResourcefulSheep.id("wool_burner");

    private WoolBurnerProvider() {}

    public record Data(int generating, int burnTime) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::generating,
                ByteBufCodecs.VAR_INT, Data::burnTime,
                Data::new);
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            return accessor.getBlockEntity() instanceof WoolBurnerBlockEntity be ? new Data(be.generating(), be.burnTime()) : null;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
            return Data.CODEC;
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    public enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Data data = Server.INSTANCE.decodeFromData(accessor).orElse(null);
            if (data == null) return;
            if (data.burnTime() > 0) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.wool_burner.burning", data.generating(), (data.burnTime() + 19) / 20)
                        .withColor(0xFF9A2E));
            } else {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.wool_burner.idle").withColor(0xAAAAAA));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
