package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GlowPanelBlockEntity;
import java.util.Locale;
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

/** Glow Panel: how much it is making and from which light. Its stored energy is Jade's own energy bar. */
public final class GlowPanelProvider {
    public static final Identifier UID = ResourcefulSheep.id("glow_panel");

    private GlowPanelProvider() {}

    public record Data(int generating, int source) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::generating,
                ByteBufCodecs.VAR_INT, Data::source,
                Data::new);
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            return accessor.getBlockEntity() instanceof GlowPanelBlockEntity be ? new Data(be.generating(), be.source().ordinal()) : null;
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
            var source = GlowPanelBlockEntity.Source.values()[Math.floorMod(data.source(), GlowPanelBlockEntity.Source.values().length)];
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.glow_panel." + source.name().toLowerCase(Locale.ROOT), data.generating())
                    .withColor(data.generating() > 0 ? 0x5EE8D6 : 0xAAAAAA));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
