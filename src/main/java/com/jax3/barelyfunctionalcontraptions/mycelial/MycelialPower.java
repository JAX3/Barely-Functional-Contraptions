package com.jax3.barelyfunctionalcontraptions.mycelial;

import com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.ModCapabilities;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ClientConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.event.CommonEvents;
import com.jax3.barelyfunctionalcontraptions.mycelial.gametest.ModGameTests;
import com.jax3.barelyfunctionalcontraptions.mycelial.net.ModNetworking;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModDataComponents;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModMenus;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModSounds;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mycelial Power module of Barely Functional Contraptions (the Mycelial Generator and Barely Functional Wrench). */
public final class MycelialPower {
    public static final String MOD_ID = BarelyFunctionalContraptions.MOD_ID;
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

        container.registerConfig(ModConfig.Type.SYNCED, ServerConfig.SPEC, BarelyFunctionalContraptions.MOD_ID + "-mycelial-synced.toml");
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, BarelyFunctionalContraptions.MOD_ID + "-mycelial-client.toml");

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
