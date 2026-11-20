package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.State;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
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
 * Echo Chamber (development builds only): its status, output and herd, the noise level and how steady it is, how
 * drowsy its sheep are or how close it is to shrieking, and its stored energy. Whichever of its 27 blocks you look at,
 * Jade is pointed at the middle one, which holds the block entity ({@link #redirect}).
 */
public final class EchoChamberProvider {
    public static final Identifier UID = ResourcefulSheep.id("echo_chamber");
    private static final int GREY = 0xAAAAAA, SCULK = 0x29DFEB;

    private EchoChamberProvider() {}

    public record Data(int state, int generating, int herd, int maxSheep, int noise, int quiet, int loud, int steady,
                       int drowsy, int shriek, int cooldown, int energy, int capacity, boolean area) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    for (int v : new int[]{d.state, d.generating, d.herd, d.maxSheep, d.noise, d.quiet, d.loud, d.steady, d.drowsy, d.shriek,
                            d.cooldown, d.energy, d.capacity}) ByteBufCodecs.VAR_INT.encode(buf, v);
                    ByteBufCodecs.BOOL.encode(buf, d.area);
                },
                buf -> {
                    int[] v = new int[13];
                    for (int i = 0; i < v.length; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
                    return new Data(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12], ByteBufCodecs.BOOL.decode(buf));
                });
    }

    /** Points Jade at the middle block of an Echo Chamber, whichever of its blocks is being looked at. */
    public static void redirect(IWailaClientRegistration registration) {
        registration.addRayTraceCallback((HitResult hit, @Nullable Accessor<?> accessor, @Nullable Accessor<?> original) -> {
            if (!(accessor instanceof BlockAccessor block) || !(block.getBlock() instanceof EchoChamberBlock)
                    || EchoChamberBlock.isCore(block.getBlockState())) return accessor;
            BlockPos core = EchoChamberBlock.core(block.getPosition(), block.getBlockState());
            var level = block.getLevel();
            if (!(level.getBlockEntity(core) instanceof EchoChamberBlockEntity be)) return accessor;
            BlockHitResult at = new BlockHitResult(block.getHitResult().getLocation(), block.getSide(), core, false);
            return registration.blockAccessor().from(block).blockState(level.getBlockState(core)).blockEntity(be).hit(at).build();
        });
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof EchoChamberBlockEntity be)) return null;
            int drowsy = be.asleep() ? 100 : be.drowsiness() * 100 / Math.max(1, ModConfigs.getInt(ModConfigs.SERVER.echoSleepTicks));
            int shriek = be.shriekMeter() * 100 / Math.max(1, ModConfigs.getInt(ModConfigs.SERVER.echoShriekTicks));
            var energy = be.getEnergyHandler();
            return new Data(be.state().ordinal(), be.generating(), be.herd(), EchoChamberBlockEntity.maxSheep(), be.noise(),
                    EchoChamberBlockEntity.quietBelow(), EchoChamberBlockEntity.loudAbove(), (int) Math.round(be.steadiness() * 100),
                    drowsy, shriek, (be.cooldown() + 19) / 20, energy.getAmountAsInt(), energy.getCapacityAsInt(), be.showsArea());
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
            String key = "jade.barelyfunctionalcontraptions.echo_chamber.";
            State state = State.byId(d.state());
            String status = "gui.barelyfunctionalcontraptions.echo_chamber.status." + state.name().toLowerCase(Locale.ROOT);
            tooltip.add((state == State.SHRIEKED ? Component.translatable(status, d.cooldown()) : Component.translatable(status)).withColor(state.color & 0xFFFFFF));
            if (d.generating() > 0) {
                tooltip.add(Component.translatable(key + "output", String.format(Locale.ROOT, "%,d", d.generating())).withColor(0xFFD27F));
            }
            tooltip.add(Component.translatable(key + "herd", d.herd(), d.maxSheep()).withColor(d.herd() > 0 ? SCULK : GREY));
            int zone = d.noise() < d.quiet() ? 0xE8B23A : d.noise() > d.loud() ? 0xE35D5D : 0x55D66B;
            tooltip.add(Component.translatable(key + "noise", d.noise(), d.quiet(), d.loud(), d.steady()).withColor(zone));
            if (state != State.ASLEEP && state != State.SHRIEKED && d.drowsy() > 0) {
                tooltip.add(Component.translatable(key + "drowsy", d.drowsy()).withColor(0x3FA9B5));
            }
            if (state != State.SHRIEKED && d.shriek() > 0) tooltip.add(Component.translatable(key + "shriek", d.shriek()).withColor(0xE35D5D));
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
