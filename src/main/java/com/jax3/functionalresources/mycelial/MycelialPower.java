package com.jax3.functionalresources.mycelial;

import com.jax3.functionalresources.FunctionalResources;
import com.jax3.functionalresources.mycelial.capability.ModCapabilities;
import com.jax3.functionalresources.mycelial.config.ClientConfig;
import com.jax3.functionalresources.mycelial.config.ServerConfig;
import com.jax3.functionalresources.mycelial.event.CommonEvents;
import com.jax3.functionalresources.mycelial.gametest.ModGameTests;
import com.jax3.functionalresources.mycelial.net.ModNetworking;
import com.jax3.functionalresources.mycelial.registry.ModBlockEntities;
import com.jax3.functionalresources.mycelial.registry.ModBlocks;
import com.jax3.functionalresources.mycelial.registry.ModDataComponents;
import com.jax3.functionalresources.mycelial.registry.ModItems;
import com.jax3.functionalresources.mycelial.registry.ModMenus;
import com.jax3.functionalresources.mycelial.registry.ModSounds;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mycelial Power module of Functional Resources (the Mycelial Generator and Mycelium Wrench). */
public final class MycelialPower {
    public static final String MOD_ID = FunctionalResources.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger("MycelialPower");

    private MycelialPower() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModGameTests.register(modBus);

        container.registerConfig(ModConfig.Type.SYNCED, ServerConfig.SPEC, FunctionalResources.MOD_ID + "-mycelial-synced.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, FunctionalResources.MOD_ID + "-mycelial-client.toml");

        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetworking::register);
        modBus.addListener(CommonEvents::onConfigLoading);
        modBus.addListener(CommonEvents::onConfigReloading);

        NeoForge.EVENT_BUS.addListener(CommonEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onServerTickPost);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
