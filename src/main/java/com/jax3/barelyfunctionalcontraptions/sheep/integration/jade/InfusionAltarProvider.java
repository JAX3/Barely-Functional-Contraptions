package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AltarStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity;
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

/** Infusion Altar: what it is doing and how far the infusion is. Its energy and slots are Jade's own. */
public final class InfusionAltarProvider {
    public static final Identifier UID = ResourcefulSheep.id("infusion_altar");

    private InfusionAltarProvider() {}

    public record Data(int status, int percent) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::status,
                ByteBufCodecs.VAR_INT, Data::percent,
                Data::new);
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof InfusionAltarBlockEntity be)) return null;
            int percent = be.maxProgress() > 0 ? be.progress() * 100 / be.maxProgress() : 0;
            return new Data(be.status().ordinal(), percent);
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
            AltarStatus status = AltarStatus.byId(data.status());
            Component name = status.displayName();
            if (status == AltarStatus.INFUSING) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.infusion_altar.infusing", data.percent()).withColor(status.color & 0xFFFFFF));
            } else {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.status", name).withColor(status.color & 0xFFFFFF));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
