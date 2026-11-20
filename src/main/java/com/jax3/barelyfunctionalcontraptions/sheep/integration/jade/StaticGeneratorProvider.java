package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.State;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CopperCombBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
 * Static Generator: its status, output and herd, how charged its dome is, its combs (how many, how oxidised the worst
 * one is, how many are waxed) and its stored energy. Looking at its dome, Jade is pointed at its base, which holds the
 * block entity ({@link #redirect}).
 */
public final class StaticGeneratorProvider {
    public static final Identifier UID = ResourcefulSheep.id("static_generator");
    private static final int GREY = 0xAAAAAA, SPARK = 0x5FB8FF;
    private static final String[] STAGES = {"fresh", "exposed", "weathered", "oxidized"};

    private StaticGeneratorProvider() {}

    public record Data(int state, int generating, int herd, int maxSheep, int charge, int domeCapacity, int combs, int worstAge,
                       int waxed, int energy, int capacity, boolean area) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    for (int v : new int[]{d.state, d.generating, d.herd, d.maxSheep, d.charge, d.domeCapacity, d.combs, d.worstAge, d.waxed,
                            d.energy, d.capacity}) ByteBufCodecs.VAR_INT.encode(buf, v);
                    ByteBufCodecs.BOOL.encode(buf, d.area);
                },
                buf -> {
                    int[] v = new int[11];
                    for (int i = 0; i < v.length; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
                    return new Data(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], ByteBufCodecs.BOOL.decode(buf));
                });
    }

    /** Points Jade at a Static Generator's base when its dome is being looked at. */
    public static void redirect(IWailaClientRegistration registration) {
        registration.addRayTraceCallback((HitResult hit, @Nullable Accessor<?> accessor, @Nullable Accessor<?> original) -> {
            if (!(accessor instanceof BlockAccessor block) || !(block.getBlock() instanceof StaticGeneratorBlock)
                    || block.getBlockState().getValue(StaticGeneratorBlock.HALF) == DoubleBlockHalf.LOWER) return accessor;
            BlockPos base = block.getPosition().below();
            var level = block.getLevel();
            if (!(level.getBlockEntity(base) instanceof StaticGeneratorBlockEntity be)) return accessor;
            BlockHitResult at = new BlockHitResult(block.getHitResult().getLocation(), block.getSide(), base, false);
            return registration.blockAccessor().from(block).blockState(level.getBlockState(base)).blockEntity(be).hit(at).build();
        });
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof StaticGeneratorBlockEntity be)) return null;
            int worst = 0, waxed = 0, combs = 0;
            for (BlockPos pos : be.combs()) {
                BlockState comb = accessor.getLevel().getBlockState(pos);
                if (!(comb.getBlock() instanceof CopperCombBlock)) continue;
                combs++;
                worst = Math.max(worst, comb.getValue(CopperCombBlock.AGE));
                if (comb.getValue(CopperCombBlock.WAXED)) waxed++;
            }
            var energy = be.getEnergyHandler();
            return new Data(be.state().ordinal(), be.generating(), be.herd(), StaticGeneratorBlockEntity.maxSheep(), be.charge(),
                    StaticGeneratorBlockEntity.domeCapacity(), combs, worst, waxed, energy.getAmountAsInt(), energy.getCapacityAsInt(), be.showsArea());
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
            String key = "jade.barelyfunctionalcontraptions.static_generator.";
            State state = State.byId(d.state());
            tooltip.add(Component.translatable("gui.barelyfunctionalcontraptions.static_generator.status." + state.name().toLowerCase(Locale.ROOT))
                    .withColor(state.color & 0xFFFFFF));
            if (d.generating() > 0) {
                tooltip.add(Component.translatable(key + "output", String.format(Locale.ROOT, "%,d", d.generating())).withColor(0xFFD27F));
            }
            tooltip.add(Component.translatable(key + "herd", d.herd(), d.maxSheep()).withColor(d.herd() > 0 ? SPARK : GREY));
            tooltip.add(Component.translatable(key + "charge", (int) ((long) d.charge() * 100 / Math.max(1, d.domeCapacity())))
                    .withColor(state == State.OVERCHARGED ? 0xE35D5D : SPARK));
            if (d.combs() > 0) {
                tooltip.add(Component.translatable(key + "combs", d.combs(),
                        Component.translatable("gui.barelyfunctionalcontraptions.static_generator.comb." + STAGES[Math.min(3, d.worstAge())]), d.waxed())
                        .withColor(0xD88A63));
            }
            tooltip.add(Component.translatable(key + "energy", String.format(Locale.ROOT, "%,d", d.energy()), String.format(Locale.ROOT, "%,d", d.capacity()))
                    .withColor(GREY));
            if (d.area()) tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.area").withColor(GREY));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
