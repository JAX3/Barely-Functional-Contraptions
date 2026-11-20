package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.Status;
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
 * Grass Seeder: its status, how green its ground is (and how much short grass), the sheared sheep grazing in its area,
 * and how many blocks the Bone Meal it has open will still tend. Jade's own lines show its Bone Meal, water and FE.
 */
public final class GrassSeederProvider {
    public static final Identifier UID = ResourcefulSheep.id("grass_seeder");
    private static final int GREY = 0xAAAAAA, GREEN = 0x8FE07A;

    private GrassSeederProvider() {}

    public record Data(int status, int grass, int ground, int shortGrass, int grazing, int boneCharges, boolean area, int tallGrass, boolean trim) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.of(
                (buf, d) -> {
                    for (int v : new int[]{d.status, d.grass, d.ground, d.shortGrass, d.grazing, d.boneCharges}) ByteBufCodecs.VAR_INT.encode(buf, v);
                    ByteBufCodecs.BOOL.encode(buf, d.area);
                    ByteBufCodecs.VAR_INT.encode(buf, d.tallGrass);
                    ByteBufCodecs.BOOL.encode(buf, d.trim);
                },
                buf -> {
                    int[] v = new int[6];
                    for (int i = 0; i < v.length; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
                    boolean area = ByteBufCodecs.BOOL.decode(buf);
                    int tall = ByteBufCodecs.VAR_INT.decode(buf);
                    return new Data(v[0], v[1], v[2], v[3], v[4], v[5], area, tall, ByteBufCodecs.BOOL.decode(buf));
                });
    }

    public enum Server implements StreamServerDataProvider<BlockAccessor, Data> {
        INSTANCE;

        @Override
        public @Nullable Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof GrassSeederBlockEntity be)) return null;
            return new Data(be.status().ordinal(), be.grass(), be.grass() + be.dirt(), be.shortGrass(), be.grazing(), be.boneCharges(), be.showsArea(),
                    be.tallGrass(), be.trims());
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
            String key = "jade.barelyfunctionalcontraptions.grass_seeder.";
            Status status = Status.byId(d.status());
            tooltip.add(Component.translatable("message.barelyfunctionalcontraptions.grass_seeder.status." + status.name().toLowerCase(Locale.ROOT))
                    .withColor(status.color & 0xFFFFFF));
            if (d.ground() > 0) tooltip.add(Component.translatable(key + "grass", d.grass(), d.ground(), d.shortGrass()).withColor(GREEN));
            if (d.tallGrass() > 0) tooltip.add(Component.translatable(key + "tall", d.tallGrass()).withColor(0xE0B04A));
            if (d.trim()) tooltip.add(Component.translatable(key + "trim").withColor(GREY));
            if (d.grazing() > 0) tooltip.add(Component.translatable(key + "grazing", d.grazing()).withColor(0xDFE4F5));
            if (d.boneCharges() > 0) tooltip.add(Component.translatable(key + "charges", d.boneCharges()).withColor(GREY));
            if (d.area()) tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.area").withColor(GREY));
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
