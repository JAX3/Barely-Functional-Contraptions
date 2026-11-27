package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        // Machines built on the machine frame: each offers the energy, fluid and items of the modules it has switched on.
        com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame.registerCapabilities(event,
                ModBlockEntities.WOOL_PACKER.get(), ModBlockEntities.CHILLER.get(),
                ModBlockEntities.INFUSION_ALTAR.get(), ModBlockEntities.GRASS_SEEDER.get(), ModBlockEntities.IMPLOSION_CHAMBER.get(),
                ModBlockEntities.FLEECE_TRAWLER.get(), ModBlockEntities.AUTOMATIC_SHEARS.get(), ModBlockEntities.FLUID_RESERVOIR.get(),
                ModBlockEntities.MECHANICAL_SEPARATOR.get(),
                // the generators
                ModBlockEntities.GLOW_PANEL.get(), ModBlockEntities.STATIC_GENERATOR.get(),
                ModBlockEntities.BLAZE_FURNACE.get(), ModBlockEntities.THERMAL_REACTION_CHAMBER.get(), ModBlockEntities.ZERO_POINT_ENERGY_MODULE.get());
        // The Sheep Treadmill and Echo Chamber are on the frame too, but span several blocks, so they're offered per block.
        // Hume Energy Cube: the whole connected group, in and out, on every face.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.HUME_ENERGY_CUBE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.CREATIVE_GENERATOR.get(), (be, side) -> be.getEnergyHandler());
        // Sheep Treadmill: its buffer and food slot (in only), from either of its blocks.
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, be, side) -> {
            var treadmill = com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.entity(level, pos, state);
            return treadmill == null ? null : treadmill.energyFor(side);
        }, ModBlocks.SHEEP_TREADMILL.get());
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, be, side) -> {
            var treadmill = com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.entity(level, pos, state);
            return treadmill == null ? null : treadmill.itemsFor(side);
        }, ModBlocks.SHEEP_TREADMILL.get());
        // Echo Chamber: power out of its port, the middle of its back, on the back face only.
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, be, side) -> {
            if (!com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.isPort(state)
                    || side != state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING).getOpposite()) return null;
            var chamber = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.entity(level, pos, state);
            return chamber == null ? null : chamber.energyFor(side);
        }, ModBlocks.ECHO_CHAMBER.get());
        // Energy Meter: in at the back, out at the front, nothing on the other faces.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ENERGY_METER.get(), (be, side) -> be.getEnergyHandler(side));
        // Wool-Lined Crate: its one slot, in and out, on every face.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CRATE.get(), (be, side) -> be.getItemHandler());
        // Crate Controller: every crate in its store, in and out, on every face.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CRATE_CONTROLLER.get(), (be, side) -> be.getItemHandler());
        // Fluid Tank: the whole connected group, in and out, on every face.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_TANK.get(), (be, side) -> be.getFluidHandler());
        // Energy Cable: takes energy in on every face and passes it straight on through its network.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ENERGY_CABLE.get(), (be, side) -> be.getEnergyHandler(side));
        // Fluid Pipe: takes fluid in on every face and passes it straight on through its network.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_PIPE.get(), (be, side) -> be.getHandler(side));
        // Item Pipe: takes items in on every face and passes them straight on through its network.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ITEM_PIPE.get(), (be, side) -> be.getHandler(side));
    }
}
