package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity.Status;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity.Target;
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

/**
 * Implosion Chamber: its status, what it's making, the Gravel it's holding to crack into Sand, and how many blasts it
 * has caught and items it has cracked. Jade's own lines show its slots and FE.
 */
public final class ImplosionChamberProvider {
    public static final Identifier UID = ResourcefulSheep.id("implosion_chamber");
    private static final int GREY = 0xAAAAAA, EMBER = 0xFFD27A;

    private ImplosionChamberProvider() {}

    public record Data(int status, int target, int cracked, long blasts, long total, boolean area) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    ByteBufCodecs.VAR_INT.encode(buf, d.status);
                    ByteBufCodecs.VAR_INT.encode(buf, d.target);
                    ByteBufCodecs.VAR_INT.encode(buf, d.cracked);
                    ByteBufCodecs.VAR_LONG.encode(buf, d.blasts);
                    ByteBufCodecs.VAR_LONG.encode(buf, d.total);
                    ByteBufCodecs.BOOL.encode(buf, d.area);
                },
                buf -> new Data(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.BOOL.decode(buf)));
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ImplosionChamberBlockEntity be)) return null;
            return new Data(be.status().ordinal(), be.target().ordinal(), be.inventory().get(ImplosionChamberBlockEntity.CRACKED).getCount(),
                    be.blasts(), be.crackedTotal(), be.showsArea());
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
            Data d = Server.INSTANCE.decodeFromData(accessor).orElse(null);
            if (d == null) return;
            String key = "jade.barelyfunctionalcontraptions.implosion_chamber.";
            Status status = Status.byId(d.status());
            tooltip.add(Component.translatable("gui.barelyfunctionalcontraptions.implosion_chamber.status." + status.name().toLowerCase(Locale.ROOT))
                    .withColor(status.color & 0xFFFFFF));
            tooltip.add(Component.translatable(key + "making", Component.translatable("gui.barelyfunctionalcontraptions.implosion_chamber.target."
                    + Target.byId(d.target()).name().toLowerCase(Locale.ROOT))).withColor(EMBER));
            if (d.cracked() > 0) tooltip.add(Component.translatable(key + "cracked", d.cracked()).withColor(GREY));
            if (d.blasts() > 0) tooltip.add(Component.translatable(key + "blasts", d.blasts(), d.total()).withColor(GREY));
            if (d.area()) tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.area").withColor(GREY));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
