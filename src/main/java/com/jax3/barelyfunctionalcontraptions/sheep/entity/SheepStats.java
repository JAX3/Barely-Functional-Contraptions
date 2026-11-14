package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A sheep's bred stats, each from 1 (every wild sheep) up to {@code [sheep_stats] maxStat} (10 by default). Stored as a
 * data attachment on the sheep, saved and synced, and kept through shearing, infusing and dyeing.
 *
 * @param yield     more wool per shearing: each point above 1 adds {@code yieldPerPoint} of a wool on average
 * @param growth    wool grows back on its own, without eating grass, faster the higher it is
 * @param endurance an infusion lasts more shearings: each point above 1 adds {@code endurancePerPoint} of its uses
 */
public record SheepStats(int yield, int growth, int endurance) {
    public static final SheepStats WILD = new SheepStats(1, 1, 1);

    public static final MapCodec<SheepStats> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("yield", 1).forGetter(SheepStats::yield),
            Codec.INT.optionalFieldOf("growth", 1).forGetter(SheepStats::growth),
            Codec.INT.optionalFieldOf("endurance", 1).forGetter(SheepStats::endurance)
    ).apply(i, SheepStats::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SheepStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SheepStats::yield,
            ByteBufCodecs.VAR_INT, SheepStats::growth,
            ByteBufCodecs.VAR_INT, SheepStats::endurance,
            SheepStats::new);

    public static int max() {
        return ModConfigs.getInt(ModConfigs.SERVER.statsMax);
    }

    /** These stats kept within 1 and {@link #max()}. */
    public SheepStats clamped() {
        int max = max();
        return new SheepStats(Mth.clamp(yield, 1, max), Mth.clamp(growth, 1, max), Mth.clamp(endurance, 1, max));
    }

    public boolean isWild() {
        return yield <= 1 && growth <= 1 && endurance <= 1;
    }

    public int total() {
        return yield + growth + endurance;
    }

    /**
     * A lamb's stats: each is its parents' average (a half rounds up or down at random), then goes up one with
     * {@code increaseChance} or down one with {@code decreaseChance}. So breeding your best sheep together slowly
     * pushes the line up.
     */
    public static SheepStats bred(SheepStats a, SheepStats b, RandomSource random) {
        return new SheepStats(child(a.yield, b.yield, random), child(a.growth, b.growth, random), child(a.endurance, b.endurance, random)).clamped();
    }

    private static int child(int a, int b, RandomSource random) {
        int sum = a + b;
        int value = sum / 2 + (sum % 2 == 1 && random.nextBoolean() ? 1 : 0);
        double roll = random.nextDouble();
        double up = ModConfigs.getDouble(ModConfigs.SERVER.statsIncreaseChance);
        if (roll < up) value++;
        else if (roll < up + ModConfigs.getDouble(ModConfigs.SERVER.statsDecreaseChance)) value--;
        return value;
    }

    /** Wool per shearing for a base of {@code base}: Yield's extra share, with any fraction as a chance of one more. */
    public int woolCount(int base, RandomSource random) {
        if (base <= 0) return base;
        double extra = base * (yield - 1) * ModConfigs.getDouble(ModConfigs.SERVER.statsYieldPerPoint);
        int whole = (int) Math.floor(extra);
        return base + whole + (random.nextDouble() < extra - whole ? 1 : 0);
    }

    /** Shearings an infusion of {@code uses} lasts with this Endurance ({@code -1}, permanent, stays permanent). */
    public int infusionUses(int uses) {
        if (uses < 0) return uses;
        return Math.max(1, (int) Math.round(uses * (1 + (endurance - 1) * ModConfigs.getDouble(ModConfigs.SERVER.statsEndurancePerPoint))));
    }

    /** Chance per second that a sheared sheep with this Growth regrows its wool on its own (0 at Growth 1). */
    public double regrowChancePerSecond() {
        return (growth - 1) * ModConfigs.getDouble(ModConfigs.SERVER.statsGrowthPerPoint);
    }
}
