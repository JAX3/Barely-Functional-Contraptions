package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity;
import java.util.Locale;
import net.minecraft.ChatFormatting;
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

/** Energy Meter: the FE/t passing through, the total so far and the comparator signal. */
public final class EnergyMeterProvider {
    public static final Identifier UID = ResourcefulSheep.id("energy_meter");

    private EnergyMeterProvider() {}

    public record Data(int rate, long total) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::rate,
                ByteBufCodecs.VAR_LONG, Data::total,
                Data::new);
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            return accessor.getBlockEntity() instanceof EnergyMeterBlockEntity be ? new Data(be.rate(), be.total()) : null;
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
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.energy_meter.rate", String.format(Locale.ROOT, "%,d", data.rate()))
                    .withColor(data.rate() > 0 ? 0x3FC8FF : 0xAAAAAA));
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.energy_meter.total", String.format(Locale.ROOT, "%,d", data.total()))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.energy_meter.signal", EnergyMeterBlockEntity.signalFor(data.rate()))
                    .withStyle(ChatFormatting.DARK_RED));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
