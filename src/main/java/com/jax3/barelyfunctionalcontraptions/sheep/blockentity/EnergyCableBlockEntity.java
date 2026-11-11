package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * One Energy Cable. Cables hold no energy themselves: anything inserted into a cable is passed straight on through its
 * {@link CableNetwork} to the blocks plugged into the network, and the network ticks once per game tick to pull from
 * sources that don't push.
 */
public class EnergyCableBlockEntity extends BlockEntity implements com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.Conduit {
    private @Nullable CableNetwork network;
    private final Map<Direction, EnergyHandler> handlers = new EnumMap<>(Direction.class);
    private final EnergyHandler unsided = new SideHandler(null);

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE.get(), pos, state);
        for (Direction dir : Direction.values()) handlers.put(dir, new SideHandler(dir));
    }

    /** The network this cable belongs to, rebuilt from the world if it changed. */
    public CableNetwork network() {
        if (!hasValidNetwork()) CableNetwork.build(level, this);
        return network;
    }

    boolean hasValidNetwork() {
        return network != null && network.isValid();
    }

    void setNetwork(CableNetwork network) {
        this.network = network;
    }

    /** Forgets the current network (every cable in it rebuilds next time it is used). */
    @Override
    public void invalidateNetwork() {
        if (network != null) network.invalidate();
    }

    public void serverTick(ServerLevel level) {
        network().tick(level);
    }

    /** The capability on each face: accepts energy and passes it on through the network; never gives any out. */
    public EnergyHandler getEnergyHandler(@Nullable Direction side) {
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

    private final class SideHandler implements EnergyHandler {
        private final @Nullable Direction side;

        SideHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public long getAmountAsLong() {
            return 0;
        }

        @Override
        public long getCapacityAsLong() {
            return 0;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount <= 0 || level == null || level.isClientSide() || isRemoved()) return 0;
            BlockPos from = side == null ? null : getBlockPos().relative(side);
            return network().deliver(level, from, amount, transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
