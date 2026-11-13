package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * One pipe (Fluid Pipe or Item Pipe). Pipes hold nothing themselves: anything put into a pipe is passed straight on
 * through its {@link PipeNetwork} to the blocks plugged into the network.
 */
public abstract class PipeBlockEntity<T extends Resource> extends BlockEntity implements ConduitBlock.Conduit, ConduitBlock.Configurable {
    private @Nullable PipeNetwork<T> network;
    /** Each side's mode and filter (only sides that differ from {@link PipeSide#DEFAULT} are stored). */
    private final Map<Direction, PipeSide> sides = new EnumMap<>(Direction.class);
    private final Map<Direction, ResourceHandler<T>> handlers = new EnumMap<>(Direction.class);
    private final ResourceHandler<T> unsided = new SideHandler(null);

    protected PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction dir : Direction.values()) handlers.put(dir, new SideHandler(dir));
    }

    /** What this pipe carries. */
    public abstract PipeKind<T> kind();

    /** The network this pipe belongs to, rebuilt from the world if it changed. */
    public PipeNetwork<T> network() {
        if (!hasValidNetwork()) PipeNetwork.build(level, this);
        return network;
    }

    boolean hasValidNetwork() {
        return network != null && network.isValid();
    }

    void setNetwork(PipeNetwork<T> network) {
        this.network = network;
    }

    @Override
    public void invalidateNetwork() {
        if (network != null) network.invalidate();
    }

    public void serverTick(ServerLevel level) {
        network().tick(level);
    }

    /** The capability on each face: accepts anything and passes it on through the network; never gives any out. None on a side that is off. */
    public @Nullable ResourceHandler<T> getHandler(@Nullable Direction side) {
        if (side == null) return unsided;
        return side(side).mode() == PipeSide.Mode.DISABLED ? null : handlers.get(side);
    }

    @Override
    public PipeSide side(Direction dir) {
        return sides.getOrDefault(dir, PipeSide.DEFAULT);
    }

    @Override
    public void setSide(Direction dir, PipeSide side) {
        if (side.isDefault()) sides.remove(dir);
        else sides.put(dir, side);
        setChanged();
        invalidateNetwork();
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    @Override
    public java.util.Optional<net.minecraft.resources.Identifier> filterEntry(net.minecraft.world.item.ItemStack stack) {
        // Pipes and cables in hand are being placed against the pipe, not filtered.
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem block && block.getBlock() instanceof ConduitBlock) return java.util.Optional.empty();
        return kind().filterEntry().apply(stack);
    }

    @Override
    public net.minecraft.network.chat.Component filterName(net.minecraft.resources.Identifier id) {
        return kind().nameOf().apply(id);
    }

    /** Every side that isn't plain Normal with no filter. */
    public Map<Direction, PipeSide> configuredSides() {
        return java.util.Collections.unmodifiableMap(sides);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        sides.clear();
        for (Direction dir : Direction.values()) {
            input.read("side_" + dir.getSerializedName(), PipeSide.CODEC).filter(s -> !s.isDefault()).ifPresent(s -> sides.put(dir, s));
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        sides.forEach((dir, side) -> output.store("side_" + dir.getSerializedName(), PipeSide.CODEC, side));
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

    /** One empty slot that takes anything (up to the network's rate) and passes it straight on. */
    private final class SideHandler implements ResourceHandler<T> {
        private final @Nullable Direction side;

        SideHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public T getResource(int index) {
            return kind().empty();
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, T resource) {
            // Holds nothing, so a fluid pipe reports no tank (and Jade shows none). An item pipe reports room for a
            // stack, since hoppers won't push into an inventory that looks full.
            return kind().slotCapacity();
        }

        @Override
        public boolean isValid(int index, T resource) {
            return true;
        }

        @Override
        public int insert(int index, T resource, int amount, TransactionContext transaction) {
            return insert(resource, amount, transaction);
        }

        @Override
        public int insert(T resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || resource.isEmpty() || level == null || level.isClientSide() || isRemoved()) return 0;
            BlockPos from = side == null ? null : getBlockPos().relative(side);
            return network().deliver(level, from, resource, amount, transaction);
        }

        @Override
        public int extract(int index, T resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(T resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
