package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidPipeBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;

/**
 * Fluid Pipe. Works like the Energy Cable, for fluids: pipes join every pipe they touch into one network and plug into
 * any block with a fluid connection (the Fluid Reservoir, the Mycelial Generator's tank, other mods' tanks). Fluid
 * pushed into the network goes straight out to the blocks plugged into it, and the network pulls from blocks that
 * only give fluid out. See {@link com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidPipeNetwork}.
 */
public class FluidPipeBlock extends ConduitBlock {
    public FluidPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canPlugInto(Level level, BlockPos other, Direction face) {
        return level.getCapability(Capabilities.Fluid.BLOCK, other, face) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.FLUID_PIPE.get(), (l, pos, s, be) -> be.serverTick(serverLevel));
    }
}
