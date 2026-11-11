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
        registration.registerBlockDataProvider(FluidPipeDataProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidPipeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(SheepInfusionProvider.INSTANCE, LivingEntity.class);
        registration.registerBlockComponent(SeparatorStatusProvider.INSTANCE, MechanicalSeparatorBlock.class);
        registration.registerProgressClient(SeparatorProgressProvider.INSTANCE);
        registration.registerBlockComponent(ZeroPointModuleProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock.class);
        registration.registerBlockComponent(EnergyCableProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCableBlock.class);
        registration.registerBlockComponent(FluidPipeProvider.INSTANCE, com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidPipeBlock.class);
    }
}
