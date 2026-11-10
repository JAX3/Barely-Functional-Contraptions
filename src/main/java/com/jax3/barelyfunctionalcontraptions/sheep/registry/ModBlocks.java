package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CreativeGeneratorBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.MechanicalSeparatorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(ResourcefulSheep.MOD_ID);

    /** Iron-tier machine: needs a stone pickaxe or better, hardness 3.5 (like a furnace), blast resistance 6. */
    public static final DeferredBlock<MechanicalSeparatorBlock> MECHANICAL_SEPARATOR = REGISTER.registerBlock("mechanical_separator",
            MechanicalSeparatorBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.IRON)
                    .pushReaction(PushReaction.IMMOVEABLE)
                    .lightLevel(state -> state.getValue(MechanicalSeparatorBlock.ACTIVE) ? 7 : 0));

    /** Shears sheep in the 3x3 area in front of it into a 9-slot inventory. Stone pickaxe, like a dispenser. */
    public static final DeferredBlock<AutomaticShearsBlock> AUTOMATIC_SHEARS = REGISTER.registerBlock("automatic_shears",
            AutomaticShearsBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.IRON)
                    .lightLevel(state -> state.getValue(AutomaticShearsBlock.ACTIVE) ? 7 : 0));

    /** Makes water while sitting on an infinite water source; 10-bucket tank. Stone pickaxe, like the other machines. */
    public static final DeferredBlock<com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidReservoirBlock> FLUID_RESERVOIR = REGISTER.registerBlock("fluid_reservoir",
            com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidReservoirBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.IRON));

    /** Stores FE; touching cubes form one battery whose buffer and transfer rate grow with every cube. */
    public static final DeferredBlock<com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock> HUME_ENERGY_CUBE = REGISTER.registerBlock("hume_energy_cube",
            com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.IRON)
                    .lightLevel(state -> state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock.CHARGE)));

    /** Crafting component for every machine: the blue-steel casing on its own. Stone pickaxe, like the machines. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> FUNCTIONAL_MACHINE_BLOCK = REGISTER.registerSimpleBlock("functional_machine_block",
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.IRON));

    /** Creative-only infinite FE source: unbreakable in survival, no recipe. */
    public static final DeferredBlock<CreativeGeneratorBlock> CREATIVE_GENERATOR = REGISTER.registerBlock("creative_generator",
            CreativeGeneratorBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3_600_000.0F)
                    .noLootTable()
                    .sound(SoundType.IRON)
                    .lightLevel(state -> 10));

    private ModBlocks() {}
}
