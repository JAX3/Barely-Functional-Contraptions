package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** One Fluid Pipe: carries water and other fluids through its {@link PipeNetwork}. */
public class FluidPipeBlockEntity extends PipeBlockEntity<FluidResource> {
    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
    }

    @Override
    public PipeKind<FluidResource> kind() {
        return PipeKind.FLUID;
    }
}
