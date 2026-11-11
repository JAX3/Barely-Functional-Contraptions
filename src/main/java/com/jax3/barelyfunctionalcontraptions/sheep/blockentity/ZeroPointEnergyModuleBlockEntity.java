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
public class ZeroPointEnergyModuleBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    private static final int SCAN_INTERVAL = 5;

    private final Storage energy = new Storage();
    /** Distance in blocks to the partner (2 = one block of gap), or 0 when not linked. */
    private int linkDistance;
    /** FE this module adds to its own buffer every tick (its half of the pair's output). */
    private int generating;
    private long lastTickPushed;
    private ZeroPointLink link = ZeroPointLink.NO_MODULE;
    /** How far along the column the last scan stopped (the partner, the blocking block or the range limit). */
    private int stoppedAt;
    /** FE that left the buffer since the last tick (pushed by it or pulled by neighbours), and the 1-second average. */
    private int energyAfterLastTick;
    private int outThisSecond;
    private int outRate;
    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> generating & 0xFFFF;
                case 5 -> generating >>> 16;
                case 6 -> getPairOutput() & 0xFFFF;
                case 7 -> getPairOutput() >>> 16;
                case 8 -> linkDistance;
                case 9 -> outRate & 0xFFFF;
                case 10 -> outRate >>> 16;
                case 11 -> getBlockState().getValue(ZeroPointEnergyModuleBlock.FACING) == Direction.UP ? 1 : 0;
                case 12 -> link.ordinal();
                case 13 -> stoppedAt;
                case 14 -> ModConfigs.getInt(ModConfigs.SERVER.zpeMaxRange);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };
    public static final int DATA_COUNT = 15;

    public ZeroPointEnergyModuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZERO_POINT_ENERGY_MODULE.get(), pos, state);
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.refreshLimits();
        if (Math.floorMod(level.getGameTime() + pos.asLong(), SCAN_INTERVAL) == 0) {
            updateLink(level, pos, state);
        }
        int pulled = Math.max(0, energyAfterLastTick - energy.getAmountAsInt()); // taken by neighbours since last tick
        if (generating > 0) energy.generate(generating);
        pushEnergy(level, pos);
        outThisSecond += pulled + (int) lastTickPushed;
        energyAfterLastTick = energy.getAmountAsInt();
        if (level.getGameTime() % 20 == 0) {
            outRate = outThisSecond / 20;
            outThisSecond = 0;
        }
    }

    /** Rescans the column and stores the result, syncing it to clients when it changes. */
    public void updateLink(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(ZeroPointEnergyModuleBlock.FACING);
        ZeroPointLink.Scan scan = scan(level, pos, facing);
        link = scan.link();
        stoppedAt = scan.stoppedAt();
        int distance = scan.distance();
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

    /** Distance to the module this one links with, or 0. */
    public static int findPartner(Level level, BlockPos pos, Direction facing) {
        return scan(level, pos, facing).distance();
    }

    /**
     * Scans the column in front of the emitter. The beam passes through air, glass and anything else that does not
     * fully block the view; the first module or solid block ends the search, and the result says which it was.
     */
    public static ZeroPointLink.Scan scan(Level level, BlockPos pos, Direction facing) {
        int maxRange = ModConfigs.getInt(ModConfigs.SERVER.zpeMaxRange);
        int minGap = ModConfigs.getInt(ModConfigs.SERVER.zpeMinGap);
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 1; i <= maxRange; i++) {
            cursor.move(facing);
            if (!level.isLoaded(cursor) || level.isOutsideBuildHeight(cursor)) return new ZeroPointLink.Scan(0, ZeroPointLink.NO_MODULE, i);
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.ZERO_POINT_ENERGY_MODULE.get())) {
                if (state.getValue(ZeroPointEnergyModuleBlock.FACING) != facing.getOpposite()) return new ZeroPointLink.Scan(0, ZeroPointLink.WRONG_FACING, i);
                if (i - 1 < minGap) return new ZeroPointLink.Scan(0, ZeroPointLink.TOO_CLOSE, i);
                return new ZeroPointLink.Scan(i, ZeroPointLink.LINKED, i);
            }
            if (state.canOcclude()) return new ZeroPointLink.Scan(0, ZeroPointLink.BLOCKED, i);
        }
        return new ZeroPointLink.Scan(0, ZeroPointLink.NO_MODULE, maxRange);
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

    public ZeroPointLink getLink() {
        return link;
    }

    /** FE/t that left the buffer, averaged over the last second. */
    public int getOutRate() {
        return outRate;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.zero_point_energy_module");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
                                                                         net.minecraft.world.entity.player.Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu(containerId, inventory, getBlockPos(), data);
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
