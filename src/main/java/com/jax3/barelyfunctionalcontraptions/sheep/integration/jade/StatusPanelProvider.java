package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Level;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock;
import java.util.ArrayList;
import java.util.List;
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
 * Machine Status Panel (any panel of a screen): how many machines its screen shows out of how many it could, its size,
 * how many have a problem, and the first two problems (the machine's name and status).
 */
public final class StatusPanelProvider {
    public static final Identifier UID = ResourcefulSheep.id("status_panel");
    private static final int GREY = 0xAAAAAA;

    private StatusPanelProvider() {}

    public record Data(int linked, int capacity, int width, int height, int problems, int signal, List<String> names, List<String> statuses) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    for (int v : new int[]{d.linked, d.capacity, d.width, d.height, d.problems, d.signal}) ByteBufCodecs.VAR_INT.encode(buf, v);
                    ByteBufCodecs.VAR_INT.encode(buf, d.names.size());
                    for (int i = 0; i < d.names.size(); i++) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, d.names.get(i));
                        ByteBufCodecs.STRING_UTF8.encode(buf, d.statuses.get(i));
                    }
                },
                buf -> {
                    int[] v = new int[6];
                    for (int i = 0; i < v.length; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
                    int n = ByteBufCodecs.VAR_INT.decode(buf);
                    List<String> names = new ArrayList<>(), statuses = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        names.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                        statuses.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                    }
                    return new Data(v[0], v[1], v[2], v[3], v[4], v[5], names, statuses);
                });
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            var level = accessor.getLevel();
            var master = StatusPanelBlock.master(level, accessor.getPosition());
            if (!(level.getBlockEntity(master) instanceof StatusPanelBlockEntity be)) return null;
            int[] size = StatusPanelBlock.size(level, master);
            int problems = 0;
            List<String> names = new ArrayList<>(), statuses = new ArrayList<>();
            for (var row : be.rows()) {
                if (row.report().level() != Level.PROBLEM) continue;
                problems++;
                if (names.size() < 2) {
                    names.add(row.custom().isEmpty() ? "#" + row.name() : row.custom());
                    statuses.add(row.report().status());
                }
            }
            return new Data(be.rows().size(), be.capacity(), size[0], size[1], problems, be.comparatorSignal(), names, statuses);
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
            String key = "jade.barelyfunctionalcontraptions.status_panel.";
            tooltip.add(Component.translatable(key + "linked", d.linked(), d.capacity()).withColor(0xDFE4F5));
            if (d.width() * d.height() > 1) tooltip.add(Component.translatable(key + "size", d.width(), d.height()).withColor(GREY));
            tooltip.add(d.problems() == 0 ? Component.translatable(key + "fine").withColor(Level.RUNNING.color & 0xFFFFFF)
                    : Component.translatable(key + "problems", d.problems()).withColor(Level.PROBLEM.color & 0xFFFFFF));
            for (int i = 0; i < d.names().size(); i++) {
                String name = d.names().get(i);
                Component machine = name.startsWith("#") ? Component.translatable(name.substring(1)) : Component.literal(name);
                tooltip.add(Component.literal("  ").append(machine).append(Component.literal(": "))
                        .append(Component.translatable("status_panel.barelyfunctionalcontraptions.state." + d.statuses().get(i)))
                        .withColor(Level.PROBLEM.color & 0xFFFFFF));
            }
            tooltip.add(Component.translatable(key + "signal", d.signal()).withColor(GREY));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
