package com.jax3.functionalresources.client;

import com.jax3.functionalresources.FunctionalResources;
import com.jax3.functionalresources.mycelial.client.MycelialPowerClient;
import com.jax3.functionalresources.sheep.client.ResourcefulSheepClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point. Never loaded on a dedicated server. */
@Mod(value = FunctionalResources.MOD_ID, dist = Dist.CLIENT)
public final class FunctionalResourcesClient {
    public FunctionalResourcesClient(IEventBus modBus, ModContainer container) {
        // One config screen lists every config file of the mod (both modules, synced and client).
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        MycelialPowerClient.init(modBus, container);
        ResourcefulSheepClient.init(modBus, container);
    }
}
