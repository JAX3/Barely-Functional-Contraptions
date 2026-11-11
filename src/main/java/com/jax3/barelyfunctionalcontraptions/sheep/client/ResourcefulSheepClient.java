package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModTags;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Client-only entry point. Never loaded on a dedicated server. */
public final class ResourcefulSheepClient {
    private ResourcefulSheepClient() {}

    public static void init(IEventBus modBus, ModContainer container) {

        modBus.addListener(ResourcefulSheepClient::registerScreens);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterBlockStateModels event) ->
                event.registerModel(EnergyCubeModel.TYPE, EnergyCubeModel.Unbaked.MAP_CODEC));
        modBus.addListener(ResourcefulSheepClient::registerTintSources);
        modBus.addListener(ResourcefulSheepClient::registerRenderStateModifiers);
        modBus.addListener(ResourcefulSheepClient::addLayers);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities.ZERO_POINT_ENERGY_MODULE.get(), context -> new ZeroPointBeamRenderer()));

        NeoForge.EVENT_BUS.addListener(ResourcefulSheepClient::onRecipesReceived);
        NeoForge.EVENT_BUS.addListener(ResourcefulSheepClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ResourcefulSheepClient::onEntityTick);
        NeoForge.EVENT_BUS.addListener(ResourcefulSheepClient::onItemTooltip);

        com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.clientTickHandler = ZeroPointHumSound::tick;
        DevScreenshotHarness.registerIfEnabled();
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MECHANICAL_SEPARATOR.get(), MechanicalSeparatorScreen::new);
        event.register(ModMenus.AUTOMATIC_SHEARS.get(), AutomaticShearsScreen::new);
        event.register(ModMenus.FLUID_RESERVOIR.get(), FluidReservoirScreen::new);
        event.register(ModMenus.HUME_ENERGY_CUBE.get(), EnergyCubeScreen::new);
        event.register(ModMenus.ZERO_POINT_ENERGY_MODULE.get(), ZeroPointModuleScreen::new);
    }

    private static void registerTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(ResourcefulSheep.id("resource_wool"), ResourceWoolTintSource.MAP_CODEC);
    }

    private static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.<Sheep, SheepRenderState>registerEntityModifier(SheepRenderer.class, (sheep, state) -> InfusedWoolLayer.extract(sheep, state));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Vanilla sheep plus any modded entity whose renderer extends SheepRenderer.
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof SheepRenderer sheepRenderer) {
                sheepRenderer.addLayer(new InfusedWoolLayer(sheepRenderer, event.getEntityModels()));
            }
        }
    }

    private static void onRecipesReceived(RecipesReceivedEvent event) {
        RecipeMap map = event.getRecipeMap();
        SeparationLookup.setClientRecipes(map);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SheepResourceRegistry.setClient(new SheepResourceRegistry(Map.of()));
        SeparationLookup.setClientRecipes(RecipeMap.EMPTY);
    }

    private static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (event.getItemStack().is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CREATIVE_GENERATOR.get())) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.barelyfunctionalcontraptions.creative_generator")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (event.getItemStack().is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ZERO_POINT_ENERGY_MODULE.get())) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.barelyfunctionalcontraptions.zero_point_energy_module")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (event.getItemStack().is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ENERGY_CABLE.get())) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.barelyfunctionalcontraptions.energy_cable",
                    String.format(java.util.Locale.ROOT, "%,d", com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CableNetwork.transferRate()))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (event.getItemStack().is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE.get())) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.barelyfunctionalcontraptions.hume_energy_cube")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            var contents = event.getItemStack().get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.MACHINE_CONTENTS.get());
            if (contents != null && contents.energy() > 0) {
                event.getToolTip().add(2, net.minecraft.network.chat.Component.translatable("tooltip.barelyfunctionalcontraptions.hume_energy_cube.stored",
                        String.format(java.util.Locale.ROOT, "%,d", contents.energy())).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            }
        }
        if (event.getItemStack().is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_RESERVOIR.get())) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.barelyfunctionalcontraptions.fluid_reservoir")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (event.getItemStack().is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FUNCTIONAL_MACHINE_BLOCK.get())) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.barelyfunctionalcontraptions.functional_machine_block")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (event.getItemStack().is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.AUTOMATIC_SHEARS.get())) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.barelyfunctionalcontraptions.automatic_shears")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /** Occasional resource-coloured particles around infused sheep that still have wool. */
    private static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (!entity.hasData(ModAttachments.SHEEP_INFUSION) || !ModTags.isInfusable(entity)) return;
        if (!ModConfigs.getBool(ModConfigs.CLIENT.ambientParticles) || entity.tickCount % 10 != 0) return;
        if (entity.isInvisible() || living.getRandom().nextInt(4) != 0) return;
        if (entity instanceof Sheep sheep && sheep.isSheared()) return;
        SheepInfusion infusion = entity.getData(ModAttachments.SHEEP_INFUSION);
        if (!infusion.isInfused()) return;
        SheepInfusionEvents.getInfusedResource(entity).ifPresent(resource -> entity.level().addParticle(
                new DustParticleOptions(resource.accentColor(), 0.7F),
                entity.getRandomX(0.6), entity.getY() + entity.getBbHeight() * (0.55 + living.getRandom().nextDouble() * 0.4), entity.getRandomZ(0.6),
                0, 0.01, 0));
    }
}
