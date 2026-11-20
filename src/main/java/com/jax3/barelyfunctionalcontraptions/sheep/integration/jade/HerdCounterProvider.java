package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
 * Herd Counter: what it's counting and how many (with its comparator signal), the sheep in its pen (ready to shear,
 * sheared, lambs), and its three biggest resources.
 */
public final class HerdCounterProvider {
    public static final Identifier UID = ResourcefulSheep.id("herd_counter");
    private static final int GREY = 0xAAAAAA, LIT = 0x7CFF8A;

    private HerdCounterProvider() {}

    public record Data(int mode, int value, int target, int signal, int total, int ready, int sheared, int lambs, String filter,
                       List<String> keys, List<Integer> counts, boolean area) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    for (int v : new int[]{d.mode, d.value, d.target, d.signal, d.total, d.ready, d.sheared, d.lambs}) ByteBufCodecs.VAR_INT.encode(buf, v);
                    ByteBufCodecs.STRING_UTF8.encode(buf, d.filter);
                    ByteBufCodecs.VAR_INT.encode(buf, d.keys.size());
                    for (int i = 0; i < d.keys.size(); i++) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, d.keys.get(i));
                        ByteBufCodecs.VAR_INT.encode(buf, d.counts.get(i));
                    }
                    ByteBufCodecs.BOOL.encode(buf, d.area);
                },
                buf -> {
                    int[] v = new int[8];
                    for (int i = 0; i < v.length; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
                    String filter = ByteBufCodecs.STRING_UTF8.decode(buf);
                    int n = ByteBufCodecs.VAR_INT.decode(buf);
                    List<String> keys = new ArrayList<>();
                    List<Integer> counts = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        keys.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                        counts.add(ByteBufCodecs.VAR_INT.decode(buf));
                    }
                    return new Data(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], filter, keys, counts, ByteBufCodecs.BOOL.decode(buf));
                });
    }

    private static Component resourceName(String key) {
        if (HerdCounterBlockEntity.PLAIN.equals(key)) return Component.translatable("gui.barelyfunctionalcontraptions.herd_counter.plain");
        return SheepResourceRegistry.client().byId(Identifier.tryParse(key)).map(SheepResource::name).orElse(Component.literal(key));
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof HerdCounterBlockEntity be)) return null;
            List<String> keys = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (var row : be.rows()) {
                if (keys.size() == 3) break;
                keys.add(row.key());
                counts.add(row.count());
            }
            String filter = be.filterResource();
            return new Data(be.mode().ordinal(), be.value(), be.target(), be.comparatorSignal(), be.total(), be.ready(), be.sheared(), be.lambs(),
                    filter == null ? "" : filter, keys, counts, be.showsArea());
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
            String key = "jade.barelyfunctionalcontraptions.herd_counter.";
            Mode mode = Mode.byId(d.mode());
            MutableComponent counting = Component.translatable(key + "counting",
                    Component.translatable("message.barelyfunctionalcontraptions.herd_counter.mode." + mode.key()),
                    mode == Mode.FULL ? d.value() + "/" + d.target() : Integer.toString(d.value()), d.signal());
            tooltip.add(counting.withColor(LIT));
            if (!d.filter().isEmpty()) tooltip.add(Component.translatable(key + "filter", resourceName(d.filter())).withColor(0xFFD27F));
            tooltip.add(Component.translatable(key + "herd", d.total(), d.ready(), d.sheared(), d.lambs()).withColor(0xDFE4F5));
            for (int i = 0; i < d.keys().size(); i++) {
                tooltip.add(Component.literal("  ").append(resourceName(d.keys().get(i))).append(Component.literal(" x" + d.counts().get(i))).withColor(GREY));
            }
            if (d.area()) tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.area").withColor(GREY));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
