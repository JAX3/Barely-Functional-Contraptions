package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CreativeGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ResourcefulSheep.MOD_ID);

    public static final Supplier<BlockEntityType<MechanicalSeparatorBlockEntity>> MECHANICAL_SEPARATOR = REGISTER.register("mechanical_separator",
            () -> new BlockEntityType<>(MechanicalSeparatorBlockEntity::new, ModBlocks.MECHANICAL_SEPARATOR.get()));

    public static final Supplier<BlockEntityType<AutomaticShearsBlockEntity>> AUTOMATIC_SHEARS = REGISTER.register("automatic_shears",
            () -> new BlockEntityType<>(AutomaticShearsBlockEntity::new, ModBlocks.AUTOMATIC_SHEARS.get()));

    public static final Supplier<BlockEntityType<CreativeGeneratorBlockEntity>> CREATIVE_GENERATOR = REGISTER.register("creative_generator",
            () -> new BlockEntityType<>(CreativeGeneratorBlockEntity::new, ModBlocks.CREATIVE_GENERATOR.get()));

    public static final Supplier<BlockEntityType<com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity>> FLUID_RESERVOIR = REGISTER.register("fluid_reservoir",
            () -> new BlockEntityType<>(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity::new, ModBlocks.FLUID_RESERVOIR.get()));

    public static final Supplier<BlockEntityType<com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity>> HUME_ENERGY_CUBE = REGISTER.register("hume_energy_cube",
            () -> new BlockEntityType<>(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity::new, ModBlocks.HUME_ENERGY_CUBE.get()));

    public static final Supplier<BlockEntityType<com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity>> ZERO_POINT_ENERGY_MODULE = REGISTER.register("zero_point_energy_module",
            () -> new BlockEntityType<>(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity::new, ModBlocks.ZERO_POINT_ENERGY_MODULE.get()));

    public static final Supplier<BlockEntityType<com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCableBlockEntity>> ENERGY_CABLE = REGISTER.register("energy_cable",
            () -> new BlockEntityType<>(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCableBlockEntity::new, ModBlocks.ENERGY_CABLE.get()));

    public static final Supplier<BlockEntityType<com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidPipeBlockEntity>> FLUID_PIPE = REGISTER.register("fluid_pipe",
            () -> new BlockEntityType<>(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidPipeBlockEntity::new, ModBlocks.FLUID_PIPE.get()));

    private ModBlockEntities() {}
}
