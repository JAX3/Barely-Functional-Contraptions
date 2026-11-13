package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The Energy Meter: an inline block that passes Forge Energy straight through and measures it.
 *
 * <p>Energy goes in at the back ({@link #inputFace()}) and out at the front ({@link EnergyMeterBlock#FACING}). It has
 * no buffer: whatever is pushed into the back is handed on to the block in front within the same transaction, and
 * whatever the block in front pulls out comes from the block behind. Every FE that actually passes (committed
 * transactions only) is counted, giving the average FE/t over the last second, the total since it was placed, and a
 * comparator signal that rises with the flow on a log scale ({@link #signalFor}).</p>
 */
public class EnergyMeterBlockEntity extends BlockEntity {
    /** FE passed since the last rate update, the average FE/t over the last second, and the total ever passed. */
    private long window;
    private int rate;
    private long total;
    /** True while forwarding, so a loop back into this meter can't recurse. */
    private boolean passing;
    private final Counter counter = new Counter();
    private final EnergyHandler input = new Side(true);
    private final EnergyHandler output = new Side(false);

    public EnergyMeterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_METER.get(), pos, state);
    }

    public Direction outputFace() {
        return getBlockState().getValue(EnergyMeterBlock.FACING);
    }

    public Direction inputFace() {
        return outputFace().getOpposite();
    }

    /** The energy connection on {@code side}: the input at the back, the output at the front, nothing elsewhere. */
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        if (side == inputFace()) return input;
        if (side == outputFace()) return output;
        return null;
    }

    private @Nullable EnergyHandler neighbour(Direction face) {
        if (level == null) return null;
        BlockPos target = worldPosition.relative(face);
        if (!level.isLoaded(target) || level.getBlockEntity(target) instanceof EnergyMeterBlockEntity other && other.passing) return null;
        return level.getCapability(Capabilities.Energy.BLOCK, target, face.getOpposite());
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, EnergyMeterBlockEntity be) {
        if (level.getGameTime() % 20 != 0) return;
        int newRate = (int) Math.min(Integer.MAX_VALUE, be.window / 20);
        be.window = 0;
        if (newRate != be.rate) {
            int oldSignal = signalFor(be.rate);
            be.rate = newRate;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            if (signalFor(newRate) != oldSignal) level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** Average FE/t that passed through over the last second. */
    public int rate() {
        return rate;
    }

    /** Total FE that has passed through since the meter was placed. */
    public long total() {
        return total;
    }

    /**
     * Comparator signal for a flow of {@code rate} FE/t: 0 with no flow, then up on a log scale, so it is useful from a
     * trickle to a Zero Point pair: 1 FE/t gives 1, 10 gives 3, 100 gives 5, 1,000 gives 8, 10,000 gives 10,
     * 100,000 gives 13 and 1,000,000 or more gives 15.
     */
    public static int signalFor(int rate) {
        if (rate <= 0) return 0;
        return Mth.clamp((int) Math.round(Math.log10(rate) * 2.5), 1, 15);
    }

    private void count(int amount, TransactionContext transaction) {
        if (amount <= 0) return;
        counter.updateSnapshots(transaction);
        counter.pending += amount;
    }

    /** The meter's two energy connections. Neither stores anything; they pass energy to the other side. */
    private final class Side implements EnergyHandler {
        private final boolean in;

        Side(boolean in) {
            this.in = in;
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
            if (!in || passing || amount <= 0) return 0;
            EnergyHandler target = neighbour(outputFace());
            if (target == null) return 0;
            passing = true;
            try {
                int moved = target.insert(amount, transaction);
                count(moved, transaction);
                return moved;
            } finally {
                passing = false;
            }
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            if (in || passing || amount <= 0) return 0;
            EnergyHandler source = neighbour(inputFace());
            if (source == null) return 0;
            passing = true;
            try {
                int moved = source.extract(amount, transaction);
                count(moved, transaction);
                return moved;
            } finally {
                passing = false;
            }
        }
    }

    /** Energy counted in the current transaction; it only reaches the totals once the outermost one commits. */
    private final class Counter extends SnapshotJournal<Long> {
        long pending;

        @Override
        protected Long createSnapshot() {
            return pending;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            pending = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            long passed = pending - originalState;
            pending = 0;
            if (passed <= 0) return;
            window += passed;
            total += passed;
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        rate = Math.max(0, input.getIntOr("Rate", 0));
        total = Math.max(0L, input.getLongOr("Total", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Rate", rate);
        output.putLong("Total", total);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
