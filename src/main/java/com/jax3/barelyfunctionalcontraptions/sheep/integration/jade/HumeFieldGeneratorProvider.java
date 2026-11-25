package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity.Status;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock;
import com.jax3.barelyfunctionalcontraptions.util.Numbers;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

/**
 * Hume Field Generator: its status, what its field made this tick out of its range, what was taken and lost, its
 * average, and that it has no buffer. Whichever of its twelve blocks you look at, Jade is pointed at the bottom
 * north-west one, which holds the block entity ({@link #redirect}).
 */
public final class HumeFieldGeneratorProvider {
    public static final Identifier UID = ResourcefulSheep.id("hume_field_generator");
    private static final int GREY = 0xAAAAAA, FIELD = 0xC99BFF, ENERGY = 0xFFD27F, LOST = 0xE35D5D;

    private HumeFieldGeneratorProvider() {}

    public record Data(int status, int generated, int delivered, int average, int min, int max, int receivers) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    for (int v : new int[]{d.status, d.generated, d.delivered, d.average, d.min, d.max, d.receivers}) ByteBufCodecs.VAR_INT.encode(buf, v);
                },
                buf -> {
                    int[] v = new int[7];
                    for (int i = 0; i < v.length; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
                    return new Data(v[0], v[1], v[2], v[3], v[4], v[5], v[6]);
                });
    }

    /** Points Jade at the block holding the generator's block entity, whichever of its blocks is being looked at. */
    public static void redirect(IWailaClientRegistration registration) {
        registration.addRayTraceCallback((HitResult hit, @Nullable Accessor<?> accessor, @Nullable Accessor<?> original) -> {
            if (!(accessor instanceof BlockAccessor block) || !(block.getBlock() instanceof HumeFieldGeneratorBlock)
                    || HumeFieldGeneratorBlock.isCore(block.getBlockState())) return accessor;
            BlockPos core = HumeFieldGeneratorBlock.origin(block.getPosition(), block.getBlockState());
            var level = block.getLevel();
            if (!(level.getBlockEntity(core) instanceof HumeFieldGeneratorBlockEntity be)) return accessor;
            BlockHitResult at = new BlockHitResult(block.getHitResult().getLocation(), block.getSide(), core, false);
            return registration.blockAccessor().from(block).blockState(level.getBlockState(core)).blockEntity(be).hit(at).build();
        });
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof HumeFieldGeneratorBlockEntity be)) return null;
            return new Data(be.status().ordinal(), be.generated(), be.delivered(), be.average(),
                    HumeFieldGeneratorBlockEntity.rangeMin(), HumeFieldGeneratorBlockEntity.rangeMax(), be.receivers());
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
            String key = "jade.barelyfunctionalcontraptions.hume_field_generator.";
            Status status = Status.byId(d.status());
            tooltip.add(Component.translatable("gui.barelyfunctionalcontraptions.hume_field_generator.status." + status.name().toLowerCase(Locale.ROOT))
                    .withColor(status.color & 0xFFFFFF));
            tooltip.add(Component.translatable(key + "field", Numbers.grouped(d.generated()), Numbers.grouped(d.min()), Numbers.grouped(d.max()))
                    .withColor(FIELD));
            if (d.generated() > 0) {
                tooltip.add(Component.translatable(key + "taken", Numbers.grouped(d.delivered())).withColor(ENERGY));
                int lost = d.generated() - d.delivered();
                if (lost > 0) tooltip.add(Component.translatable(key + "lost", Numbers.grouped(lost)).withColor(LOST));
            }
            tooltip.add(Component.translatable(key + "average", Numbers.grouped(d.average())).withColor(GREY));
            tooltip.add(Component.translatable(key + "no_buffer").withColor(GREY));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
