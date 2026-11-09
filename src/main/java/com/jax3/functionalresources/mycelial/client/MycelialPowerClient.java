package com.jax3.functionalresources.mycelial.client;

import com.jax3.functionalresources.mycelial.client.screen.MycelialGeneratorScreen;
import com.jax3.functionalresources.mycelial.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only entry point: screens and the in-game config screen. */
public final class MycelialPowerClient {
    private MycelialPowerClient() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        modBus.addListener(MycelialPowerClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(WrenchClientEffects::onClientTick);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MYCELIAL_GENERATOR.get(), MycelialGeneratorScreen::new);
    }
}
