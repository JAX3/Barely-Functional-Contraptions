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
        // Glow Panel: its buffer, on every face (it only pushes sideways and down itself).
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.GLOW_PANEL.get(), (be, side) -> be.getEnergyHandler());
        // Sheep Treadmill: its buffer and food slot (in only), from either of its blocks.
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, be, side) -> {
            var treadmill = com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.entity(level, pos, state);
            return treadmill == null ? null : treadmill.getEnergyHandler();
        }, ModBlocks.SHEEP_TREADMILL.get());
        event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, be, side) -> {
            var treadmill = com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.entity(level, pos, state);
            return treadmill == null ? null : new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(treadmill, side);
        }, ModBlocks.SHEEP_TREADMILL.get());
        // Static Generator: its buffer (out only), on every face of its base.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.STATIC_GENERATOR.get(), (be, side) -> be.getEnergyHandler());
        // Echo Chamber: power out of its port, the middle of its back, on the back face only.
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, be, side) -> {
            if (!com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.isPort(state)
                    || side != state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING).getOpposite()) return null;
            var chamber = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.entity(level, pos, state);
            return chamber == null ? null : chamber.getEnergyHandler();
        }, ModBlocks.ECHO_CHAMBER.get());
        // Wool Burner: its buffer on every face, and its fuel slot (in only) for hoppers and pipes.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.WOOL_BURNER.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.WOOL_BURNER.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Wool Packer: its buffer (in only) on every face; wool in and bales out for hoppers and pipes.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.WOOL_PACKER.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.WOOL_PACKER.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Infusion Altar: its buffer (in only), and feeding items and catalysts in (never out) for hoppers and pipes.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.INFUSION_ALTAR.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.INFUSION_ALTAR.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Blaze Sheep Furnace: its buffer on every face, and its boost slot (in only) for hoppers and pipes.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.BLAZE_FURNACE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BLAZE_FURNACE.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Powered Furnace: power in on every face; items in to the input and out of the output, on every face.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.POWERED_FURNACE.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.POWERED_FURNACE.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Grass Seeder: power and water in, and Bone Meal in for hoppers and pipes, on every face.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.GRASS_SEEDER.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.GRASS_SEEDER.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.GRASS_SEEDER.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Snow Sheep Chiller: water in on every face (buckets, pipes, a Fluid Reservoir), and its Ice out for hoppers and pipes.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.CHILLER.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHILLER.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Thermal Reaction Chamber: power out and water in on every face, and its boost slots (in only) for hoppers and pipes.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.THERMAL_REACTION_CHAMBER.get(), (be, side) -> be.getEnergyHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.THERMAL_REACTION_CHAMBER.get(), (be, side) -> be.getTank());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.THERMAL_REACTION_CHAMBER.get(),
                (be, side) -> new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(be, side));
        // Energy Meter: in at the back, out at the front, nothing on the other faces.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ENERGY_METER.get(), (be, side) -> be.getEnergyHandler(side));
        // Wool-Lined Crate: its one slot, in and out, on every face.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CRATE.get(), (be, side) -> be.getItemHandler());
        // Fluid Tank: the whole connected group, in and out, on every face.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_TANK.get(), (be, side) -> be.getFluidHandler());
        // Zero Point Energy Module: output only, on every face.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ZERO_POINT_ENERGY_MODULE.get(), (be, side) -> be.getEnergyHandler());
        // Energy Cable: takes energy in on every face and passes it straight on through its network.
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.ENERGY_CABLE.get(), (be, side) -> be.getEnergyHandler(side));
        // Fluid Pipe: takes fluid in on every face and passes it straight on through its network.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_PIPE.get(), (be, side) -> be.getHandler(side));
        // Item Pipe: takes items in on every face and passes them straight on through its network.
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ITEM_PIPE.get(), (be, side) -> be.getHandler(side));
    }
}
