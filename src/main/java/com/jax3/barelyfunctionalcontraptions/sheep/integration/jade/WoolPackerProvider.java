package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity;
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

/** Wool Packer: what it is doing and how far the bale being pressed is. Its energy and slots are Jade's own. */
public final class WoolPackerProvider {
    public static final Identifier UID = ResourcefulSheep.id("wool_packer");

    private WoolPackerProvider() {}

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
            if (!(accessor.getBlockEntity() instanceof WoolPackerBlockEntity be)) return null;
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
            MachineStatus status = MachineStatus.byId(data.status());
            Component name = Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.status." + status.name().toLowerCase(Locale.ROOT));
            if (status == MachineStatus.PROCESSING) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.wool_packer.pressing", data.percent()).withColor(status.color & 0xFFFFFF));
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
