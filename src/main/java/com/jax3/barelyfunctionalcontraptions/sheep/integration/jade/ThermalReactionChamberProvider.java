package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State;
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
 * Thermal Reaction Chamber: its status, output and pairs, each herd, the balance, how close it is to a meltdown or a
 * freeze, the boosts burning and any spent sheep. Stored energy, water and the boost slots come from Jade's own
 * support for NeoForge's energy, fluid and item capabilities.
 */
public final class ThermalReactionChamberProvider {
    public static final Identifier UID = ResourcefulSheep.id("thermal_reaction_chamber");
    private static final int HOT = 0xFFB46A, COLD = 0xBFE8FF, GREY = 0xAAAAAA;

    private ThermalReactionChamberProvider() {}

    public record Data(int state, int generating, int pairs, int maxPairs, int hot, int cold, int balance, int danger,
                       int hotBurn, int coldBurn, int spent, boolean area) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    for (int v : new int[]{d.state, d.generating, d.pairs, d.maxPairs, d.hot, d.cold, d.hotBurn, d.coldBurn, d.spent}) ByteBufCodecs.VAR_INT.encode(buf, v);
                    ByteBufCodecs.INT.encode(buf, d.balance);
                    ByteBufCodecs.INT.encode(buf, d.danger);
                    ByteBufCodecs.BOOL.encode(buf, d.area);
                },
                buf -> {
                    int[] v = new int[9];
                    for (int i = 0; i < 9; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
                    int balance = ByteBufCodecs.INT.decode(buf), danger = ByteBufCodecs.INT.decode(buf);
                    return new Data(v[0], v[1], v[2], v[3], v[4], v[5], balance, danger, v[6], v[7], v[8], ByteBufCodecs.BOOL.decode(buf));
                });
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ThermalReactionChamberBlockEntity be)) return null;
            int danger = be.instability() * 100 / Math.max(1, ThermalReactionChamberBlockEntity.unstableTicks());
            return new Data(be.state().ordinal(), be.generating(), be.pairs(), ThermalReactionChamberBlockEntity.maxPairs(), be.hotCount(), be.coldCount(),
                    be.balance(), danger, be.hotBurn(), be.coldBurn(), be.spent(), be.showsArea());
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
            String key = "jade.barelyfunctionalcontraptions.thermal_reaction_chamber.";
            State state = State.byId(d.state());
            tooltip.add(Component.translatable("gui.barelyfunctionalcontraptions.thermal_reaction_chamber.status." + state.name().toLowerCase(Locale.ROOT))
                    .withColor(state.color & 0xFFFFFF));
            if (d.generating() > 0) {
                tooltip.add(Component.translatable(key + "output", String.format(Locale.ROOT, "%,d", d.generating()), d.pairs(), d.maxPairs()).withColor(0xFFD27F));
            }
            tooltip.add(Component.translatable(key + "herds", d.hot(), d.cold()).withColor(GREY));
            if (d.balance() != 0) {
                tooltip.add(Component.translatable(key + (d.balance() > 0 ? "hot" : "cold"), Math.abs(d.balance())).withColor(d.balance() > 0 ? HOT : COLD));
            }
            if (d.danger() != 0 && state != State.OVERHEATED) {
                tooltip.add(Component.translatable(key + (d.danger() > 0 ? "meltdown" : "freeze"), Math.abs(d.danger())).withColor(d.danger() > 0 ? 0xFF6A4A : COLD));
            }
            if (d.hotBurn() > 0 || d.coldBurn() > 0) {
                tooltip.add(Component.translatable(key + "boosts", (d.hotBurn() + 19) / 20, (d.coldBurn() + 19) / 20).withColor(GREY));
            }
            if (d.spent() > 0) tooltip.add(Component.translatable(key + "spent", d.spent()).withColor(0xFF5555));
            if (d.area()) tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.area").withColor(GREY));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
