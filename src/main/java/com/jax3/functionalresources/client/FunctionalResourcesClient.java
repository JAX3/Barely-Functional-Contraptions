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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FunctionalResourcesClient::onItemTooltip);
    }

    /** Machines with a front explain the Shift-to-reverse placement. */
    private static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.is(com.jax3.functionalresources.mycelial.registry.ModItems.MYCELIAL_GENERATOR.get())
                || stack.is(com.jax3.functionalresources.sheep.registry.ModItems.MECHANICAL_SEPARATOR.get())
                || stack.is(com.jax3.functionalresources.sheep.registry.ModItems.AUTOMATIC_SHEARS.get())) {
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.functionalresources.shift_place")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
