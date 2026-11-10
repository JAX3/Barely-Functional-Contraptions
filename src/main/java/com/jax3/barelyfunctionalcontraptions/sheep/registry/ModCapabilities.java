package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.MECHANICAL_SEPARATOR.get(), (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.MECHANICAL_SEPARATOR.get(), (be, side) -> be.getEnergyHandler(side));
        // Automatic Shears: its vanilla Container, for pipes from other mods (vanilla hoppers use the Container directly).
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.AUTOMATIC_SHEARS.get(),
                (be, side) -> net.neoforged.neoforge.transfer.item.VanillaContainerWrapper.of(be));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.AUTOMATIC_SHEARS.get(), (be, side) -> be.getEnergyHandler(side));
        // Fluid Reservoir: water out on every side; empty buckets in / water buckets out for pipes.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_RESERVOIR.get(), (be, side) -> be.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.FLUID_RESERVOIR.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Hume Energy Cube: the whole connected group, in and out, on every face.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.HUME_ENERGY_CUBE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.CREATIVE_GENERATOR.get(), (be, side) -> be.getEnergyHandler());
    }
}
