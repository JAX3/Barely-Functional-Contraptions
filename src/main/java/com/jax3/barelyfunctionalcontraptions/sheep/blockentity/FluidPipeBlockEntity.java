package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * One Fluid Pipe. Pipes hold no fluid themselves: anything poured into a pipe is passed straight on through its
 * {@link FluidPipeNetwork} to the blocks plugged into the network.
 */
public class FluidPipeBlockEntity extends BlockEntity implements ConduitBlock.Conduit {
    private @Nullable FluidPipeNetwork network;
    private final Map<Direction, ResourceHandler<FluidResource>> handlers = new EnumMap<>(Direction.class);
    private final ResourceHandler<FluidResource> unsided = new SideHandler(null);

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PIPE.get(), pos, state);
        for (Direction dir : Direction.values()) handlers.put(dir, new SideHandler(dir));
    }

    /** The network this pipe belongs to, rebuilt from the world if it changed. */
    public FluidPipeNetwork network() {
        if (!hasValidNetwork()) FluidPipeNetwork.build(level, this);
        return network;
    }

    boolean hasValidNetwork() {
        return network != null && network.isValid();
    }

    void setNetwork(FluidPipeNetwork network) {
        this.network = network;
    }

    @Override
    public void invalidateNetwork() {
        if (network != null) network.invalidate();
    }

    public void serverTick(ServerLevel level) {
        network().tick(level);
    }

    /** The capability on each face: accepts fluid and passes it on through the network; never gives any out. */
    public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
        return side == null ? unsided : handlers.get(side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invalidateNetwork();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        invalidateNetwork();
    }

    /** One empty slot that takes any fluid (up to the network's rate) and passes it straight on. */
    private final class SideHandler implements ResourceHandler<FluidResource> {
        private final @Nullable Direction side;

        SideHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return 0; // holds nothing (and Jade shows no tank for it)
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return true;
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return insert(resource, amount, transaction);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || resource.isEmpty() || level == null || level.isClientSide() || isRemoved()) return 0;
            BlockPos from = side == null ? null : getBlockPos().relative(side);
            return network().deliver(level, from, resource, amount, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
