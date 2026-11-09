package com.jax3.functionalresources.sheep;

import com.jax3.functionalresources.FunctionalResources;
import com.jax3.functionalresources.sheep.config.ModConfigs;
import com.jax3.functionalresources.sheep.entity.SheepInfusionEvents;
import com.jax3.functionalresources.sheep.gametest.ModGameTests;
import com.jax3.functionalresources.sheep.network.ModNetwork;
import com.jax3.functionalresources.sheep.registry.ModAttachments;
import com.jax3.functionalresources.sheep.registry.ModBlockEntities;
import com.jax3.functionalresources.sheep.registry.ModBlocks;
import com.jax3.functionalresources.sheep.registry.ModCapabilities;
import com.jax3.functionalresources.sheep.registry.ModDataComponents;
import com.jax3.functionalresources.sheep.registry.ModItems;
import com.jax3.functionalresources.sheep.registry.ModLootModifiers;
import com.jax3.functionalresources.sheep.registry.ModMenus;
import com.jax3.functionalresources.sheep.registry.ModRecipes;
import com.jax3.functionalresources.sheep.resource.SheepResourceManager;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Resourceful Sheep module of Functional Resources.
 *
 * <p>Everything gameplay related is registered from here on the mod event bus; game-bus listeners live in
 * {@link SheepInfusionEvents} and {@link SheepResourceManager}. Client-only code is registered from
 * {@code ResourcefulSheepClient}, which is a separate {@code @Mod(dist = CLIENT)} entry point so that dedicated
 * servers never load client classes.</p>
 */
public final class ResourcefulSheep {
    public static final String MOD_ID = FunctionalResources.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    private ResourcefulSheep() {}

    public static void init(IEventBus modBus, ModContainer container) {
        ModDataComponents.REGISTER.register(modBus);
        ModAttachments.REGISTER.register(modBus);
        ModBlocks.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModBlockEntities.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModRecipes.BOOK_CATEGORIES.register(modBus);
        ModLootModifiers.REGISTER.register(modBus);

        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetwork::register);

        // Game tests (only active when the game test framework is enabled, e.g. ./gradlew runGameTestServer).
        ModGameTests.FUNCTIONS.register(modBus);
        modBus.addListener(ModGameTests::registerTests);

        container.registerConfig(ModConfig.Type.SYNCED, ModConfigs.SERVER_SPEC, FunctionalResources.MOD_ID + "-sheep-synced.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ModConfigs.CLIENT_SPEC, FunctionalResources.MOD_ID + "-sheep-client.toml");

        NeoForge.EVENT_BUS.register(SheepResourceManager.Events.class);
        NeoForge.EVENT_BUS.register(SheepInfusionEvents.class);
        NeoForge.EVENT_BUS.register(com.jax3.functionalresources.sheep.command.ResourcefulSheepCommand.class);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
