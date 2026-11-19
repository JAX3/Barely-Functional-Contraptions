package com.jax3.barelyfunctionalcontraptions.compat;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.fml.config.ModConfig;
import org.jspecify.annotations.Nullable;

/**
 * The few game calls that differ between the Minecraft versions the mod builds for, for Minecraft 26.2. Every
 * supported version has this class, with the same members, in {@code src/mc/<version>/java}; the shared code only ever
 * calls these.
 */
public final class Mc {
    private Mc() {}

    /** The Minecraft version this build is for. */
    public static final String VERSION = "26.2";

    /** The config type that is saved once and synced to every client: on 26.2, the per-world server config. */
    public static final ModConfig.Type SYNCED_CONFIG = ModConfig.Type.SERVER;

    /** A block pistons can't move. */
    public static final PushReaction PUSH_IMMOVABLE = PushReaction.BLOCK;
    /** A block pistons break (and drop). */
    public static final PushReaction PUSH_BREAKS = PushReaction.DESTROY;

    /** A loot context's parameter, or null when it has none. */
    public static <T> @Nullable T lootParam(LootContext context, ContextKey<T> key) {
        return context.getOptionalParameter(key);
    }

    /** Reads a list or tag of entity types. */
    public static Codec<HolderSet<EntityType<?>>> entityTypes() {
        return RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE);
    }
}
