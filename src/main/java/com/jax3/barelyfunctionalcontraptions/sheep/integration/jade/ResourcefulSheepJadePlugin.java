package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.MechanicalSeparatorBlock;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration (optional: only loaded by Jade through {@link WailaPlugin}, so the mod runs without Jade).
 *
 * <ul>
 *     <li><b>Sheep</b> (any {@code #barelyfunctionalcontraptions:infusable} mob): infused resource, shearings left, cooldown.</li>
 *     <li><b>Mechanical Separator</b>: Jade's progress arrow plus the machine status and FE/t. Stored energy and the
 *     inventory are shown by Jade's built-in support for NeoForge's energy and item capabilities.</li>
 * </ul>
 * Each part can be toggled in Jade's settings (Plugins &gt; Resourceful Sheep).
 */
@WailaPlugin
public class ResourcefulSheepJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(SeparatorStatusDataProvider.INSTANCE, MechanicalSeparatorBlockEntity.class);
        registration.registerProgress(SeparatorProgressProvider.INSTANCE, MechanicalSeparatorBlockEntity.class);
        registration.registerBlockDataProvider(EnergyCableDataProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCableBlockEntity.class);
        registration.registerBlockDataProvider(PipeDataProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeBlockEntity.class);
        registration.registerBlockDataProvider(GlowPanelProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GlowPanelBlockEntity.class);
        registration.registerBlockDataProvider(BlazeFurnaceProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.class);
        registration.registerBlockDataProvider(WoolBurnerProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity.class);
        registration.registerBlockDataProvider(InfusionAltarProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity.class);
        registration.registerBlockDataProvider(WoolPackerProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity.class);
        registration.registerBlockDataProvider(EnergyMeterProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity.class);
        registration.registerBlockDataProvider(ThermalReactionChamberProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.class);
        registration.registerBlockDataProvider(EchoChamberProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.class);
        registration.registerBlockDataProvider(StaticGeneratorProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(StatusPanelProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.class);
        registration.registerBlockDataProvider(HerdCounterProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.class);
        registration.registerBlockDataProvider(GrassSeederProvider.Server.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.class);
        registration.registerEntityDataProvider(ThermalFatigueProvider.Server.INSTANCE, net.minecraft.world.entity.animal.sheep.Sheep.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(SheepInfusionProvider.INSTANCE, LivingEntity.class);
        registration.registerBlockComponent(SeparatorStatusProvider.INSTANCE, MechanicalSeparatorBlock.class);
        registration.registerProgressClient(SeparatorProgressProvider.INSTANCE);
        registration.registerBlockComponent(ZeroPointModuleProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock.class);
        registration.registerBlockComponent(EnergyCableProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCableBlock.class);
        registration.registerBlockComponent(PipeProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidPipeBlock.class);
        registration.registerBlockComponent(PipeProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.ItemPipeBlock.class);
        registration.registerBlockComponent(GlowPanelProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.GlowPanelBlock.class);
        registration.registerBlockComponent(BlazeFurnaceProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock.class);
        registration.registerBlockComponent(WoolBurnerProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolBurnerBlock.class);
        registration.registerBlockComponent(InfusionAltarProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.InfusionAltarBlock.class);
        registration.registerBlockComponent(WoolPackerProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolPackerBlock.class);
        registration.registerBlockComponent(EnergyMeterProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.class);
        registration.registerBlockComponent(ThermalReactionChamberProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.ThermalReactionChamberBlock.class);
        registration.registerBlockComponent(EchoChamberProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.class);
        EchoChamberProvider.redirect(registration);
        registration.registerBlockComponent(StaticGeneratorProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock.class);
        StaticGeneratorProvider.redirect(registration);
        registration.registerBlockComponent(StatusPanelProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.class);
        registration.registerBlockComponent(CrateProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.class);
        registration.registerBlockComponent(CrateControllerProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateControllerBlock.class);
        registration.registerBlockComponent(HerdCounterProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.HerdCounterBlock.class);
        registration.registerBlockComponent(GrassSeederProvider.Client.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.GrassSeederBlock.class);
        registration.registerEntityComponent(ThermalFatigueProvider.Client.INSTANCE, net.minecraft.world.entity.animal.sheep.Sheep.class);
    }
}
