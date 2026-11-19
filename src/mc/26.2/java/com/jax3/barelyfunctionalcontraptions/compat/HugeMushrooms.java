package com.jax3.barelyfunctionalcontraptions.compat;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;

/** Grows a huge mushroom the way a bone-mealed mushroom does (Minecraft 26.2: configured features). */
public final class HugeMushrooms {
    private HugeMushrooms() {}

    /** Grows a huge red or brown mushroom with its stem at {@code stem}; false if there was no room or it was stopped. */
    public static boolean grow(ServerLevel level, RandomSource random, BlockPos stem, boolean red) {
        ResourceKey<ConfiguredFeature<?, ?>> key = red ? TreeFeatures.HUGE_RED_MUSHROOM : TreeFeatures.HUGE_BROWN_MUSHROOM;
        Optional<Holder.Reference<ConfiguredFeature<?, ?>>> feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(key);
        if (feature.isEmpty()) return false;
        BlockGrowFeatureEvent event = EventHooks.fireBlockGrowFeature(level, random, stem, feature.get());
        Holder<ConfiguredFeature<?, ?>> chosen = event.getFeature();
        if (event.isCanceled() || chosen == null) return false;
        return chosen.value().place(level, level.getChunkSource().getGenerator(), random, stem);
    }
}
