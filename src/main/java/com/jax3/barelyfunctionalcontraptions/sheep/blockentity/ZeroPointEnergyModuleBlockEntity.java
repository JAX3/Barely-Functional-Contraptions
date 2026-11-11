package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A Zero Point Energy Module. Every few ticks it looks along its facing (up or down) for a partner: the nearest module
 * in the same column that faces back at it, with only see-through blocks in between and at least {@code minGap}
 * blocks of open space. A linked pair makes {@code energyPerBlock} FE/t for every block of gap; the floor module
 * keeps the larger half, the ceiling module the rest. Each module pushes its buffer into neighbouring receivers.
 *
 * <p>The link distance and rate are synced to the client, which draws the beam and shows them in Jade.</p>
 */
public class ZeroPointEnergyModuleBlockEntity extends BlockEntity {
    private static final int SCAN_INTERVAL = 5;

    private final Storage energy = new Storage();
    /** Distance in blocks to the partner (2 = one block of gap), or 0 when not linked. */
    private int linkDistance;
    /** FE this module adds to its own buffer every tick (its half of the pair's output). */
    private int generating;
    private long lastTickPushed;

    public ZeroPointEnergyModuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZERO_POINT_ENERGY_MODULE.get(), pos, state);
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.refreshLimits();
        if (Math.floorMod(level.getGameTime() + pos.asLong(), SCAN_INTERVAL) == 0) {
            updateLink(level, pos, state);
        }
        if (generating > 0) energy.generate(generating);
        pushEnergy(level, pos);
    }

    /** Rescans the column and stores the result, syncing it to clients when it changes. */
    public void updateLink(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(ZeroPointEnergyModuleBlock.FACING);
        int distance = findPartner(level, pos, facing);
        int pairRate = pairOutput(distance);
        // The floor module (facing up) keeps the odd FE so the pair adds up to exactly pairRate.
        int share = facing == Direction.UP ? (pairRate + 1) / 2 : pairRate / 2;
        if (distance == linkDistance && share == generating) return;
        linkDistance = distance;
        generating = share;
        boolean linked = distance > 0;
        if (state.getValue(ZeroPointEnergyModuleBlock.LINKED) != linked) {
            level.setBlock(pos, state.setValue(ZeroPointEnergyModuleBlock.LINKED, linked), Block.UPDATE_ALL);
        }
        setChanged();
        level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /**
     * Distance to the module this one links with, or 0. The beam passes through air, glass and anything else that
     * does not fully block the view; any other block, or a module facing the wrong way, ends the search.
     */
    public static int findPartner(Level level, BlockPos pos, Direction facing) {
        int maxRange = ModConfigs.getInt(ModConfigs.SERVER.zpeMaxRange);
        int minGap = ModConfigs.getInt(ModConfigs.SERVER.zpeMinGap);
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 1; i <= maxRange; i++) {
            cursor.move(facing);
            if (!level.isLoaded(cursor) || level.isOutsideBuildHeight(cursor)) return 0;
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.ZERO_POINT_ENERGY_MODULE.get())) {
                boolean facesBack = state.getValue(ZeroPointEnergyModuleBlock.FACING) == facing.getOpposite();
                return facesBack && i - 1 >= minGap ? i : 0;
            }
            if (state.canOcclude()) return 0;
        }
        return 0;
    }

    /** FE/t a pair this far apart makes in total (0 when not linked). */
    public static int pairOutput(int distance) {
        if (distance <= 0) return 0;
        long rate = (long) ModConfigs.getInt(ModConfigs.SERVER.zpeEnergyPerBlock) * (distance - 1);
        return (int) Math.min(Integer.MAX_VALUE, rate);
    }

    private void pushEnergy(ServerLevel level, BlockPos pos) {
        int budget = Math.min(energy.getAmountAsInt(), ModConfigs.getInt(ModConfigs.SERVER.zpeMaxOutput));
        long pushed = 0;
        for (Direction dir : Direction.values()) {
            if (budget <= 0) break;
            BlockPos target = pos.relative(dir);
            if (level.getBlockEntity(target) instanceof ZeroPointEnergyModuleBlockEntity) continue;
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, target, dir.getOpposite());
            if (handler == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = handler.insert(budget, tx);
                tx.commit();
                if (inserted > 0) {
                    energy.drain(inserted);
                    budget -= inserted;
                    pushed += inserted;
                }
            }
        }
        lastTickPushed = pushed;
    }

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    public int getLinkDistance() {
        return linkDistance;
    }

    /** This module's share of the pair's FE/t. */
    public int getGenerating() {
        return generating;
    }

    /** The whole pair's FE/t. */
    public int getPairOutput() {
        return pairOutput(linkDistance);
    }

    public int getEnergyStored() {
        return energy.getAmountAsInt();
    }

    public long getLastTickPushed() {
        return lastTickPushed;
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("energy").ifPresent(energy::deserialize);
        linkDistance = Math.max(0, input.getIntOr("LinkDistance", 0));
        generating = Math.max(0, input.getIntOr("Generating", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        output.putInt("LinkDistance", linkDistance);
        output.putInt("Generating", generating);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    /** Output-only buffer: neighbours may pull up to {@code maxOutput} FE/t, nothing can be inserted from outside. */
    private final class Storage extends SimpleEnergyHandler {
        Storage() {
            super(ModConfigs.getInt(ModConfigs.SERVER.zpeCapacity), 0, ModConfigs.getInt(ModConfigs.SERVER.zpeMaxOutput));
        }

        void refreshLimits() {
            this.capacity = ModConfigs.getInt(ModConfigs.SERVER.zpeCapacity);
            this.maxExtract = ModConfigs.getInt(ModConfigs.SERVER.zpeMaxOutput);
            if (this.energy > this.capacity) set(this.capacity);
        }

        void generate(int amount) {
            set((int) Math.min(capacity, (long) energy + amount));
        }

        void drain(int amount) {
            set(Math.max(0, energy - amount));
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    }
}
