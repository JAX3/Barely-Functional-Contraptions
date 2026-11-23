package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * A Crate Controller: links Wool-Lined Crates into one store. Crates touching it, or touching a crate already in its
 * store, join by themselves; crates elsewhere are linked with the Barely Functional Wrench (and bring every crate
 * touching them). A crate is only ever in one controller's store ({@link CrateBlockEntity#claim}).
 *
 * <p>Pipes and hoppers on the controller reach the whole store: items in go to a crate already holding that item, then
 * to an empty unlocked crate, and only then into crates whose lids are bulging; items out come from any crate. If
 * nothing fits anywhere, the fullest unstrapped crate holding that item takes the strain, and may burst. A comparator
 * reads how full the whole store is, and a Machine Status Panel shows one line for it.</p>
 */
public class CrateControllerBlockEntity extends BlockEntity {
    /** Crates linked with the wrench (each brings the crates touching it). */
    private final List<BlockPos> links = new ArrayList<>();
    /** The crates in its store, as of its last look round. */
    private List<CrateBlockEntity> crates = List.of();
    private long lastRefresh = Long.MIN_VALUE;
    private final Handler handler = new Handler();

    // What it found on its last look round (synced to clients for its front, and read by the status panel and Jade).
    private int crateCount, empties, bulging, risky, missing, taken;
    private long stored, capacity;

    public CrateControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRATE_CONTROLLER.get(), pos, state);
    }

    public static int range() {
        return ModConfigs.getInt(ModConfigs.SERVER.crateControllerRange);
    }

    public static int maxCrates() {
        return ModConfigs.getInt(ModConfigs.SERVER.crateControllerMaxCrates);
    }

    // ------------------------------------------------------------------ its store

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, CrateControllerBlockEntity be) {
        if (level.getGameTime() % 20 == Math.floorMod(pos.asLong(), 20)) be.refresh(level);
    }

    /** The crates in its store, looked up again if it's been a second. */
    public List<CrateBlockEntity> crates() {
        if (level instanceof ServerLevel server && server.getGameTime() - lastRefresh >= 20) refresh(server);
        return crates;
    }

    /**
     * Looks round for its crates: the ones touching it and the ones it's linked to, then every crate touching those,
     * up to {@link #maxCrates}, claiming each (a crate in another controller's store is left out). Then works out how
     * full it all is.
     */
    public void refresh(ServerLevel level) {
        long now = level.getGameTime();
        lastRefresh = now;
        int limit = maxCrates();
        Set<BlockPos> seen = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<CrateBlockEntity> found = new ArrayList<>();
        int missingNow = 0, takenNow = 0;
        for (Direction d : Direction.values()) queue.add(worldPosition.relative(d));
        for (BlockPos link : links) {
            if (!level.isLoaded(link) || !(level.getBlockEntity(link) instanceof CrateBlockEntity)) missingNow++;
            else queue.add(link);
        }
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos p = queue.poll();
            if (!seen.add(p) || !level.isLoaded(p) || !(level.getBlockEntity(p) instanceof CrateBlockEntity crate)) continue;
            if (!crate.claim(level, worldPosition, now)) {
                takenNow++;
                continue;
            }
            found.add(crate);
            for (Direction d : Direction.values()) {
                BlockPos next = p.relative(d);
                if (!seen.contains(next)) queue.add(next);
            }
        }
        crates = List.copyOf(found);
        // How it's all doing.
        int emptiesNow = 0, bulgingNow = 0, riskyNow = 0;
        long storedNow = 0, capacityNow = 0;
        boolean bursting = ModConfigs.getBool(ModConfigs.SERVER.crateBursting);
        for (CrateBlockEntity crate : crates) {
            storedNow += crate.count();
            capacityNow += crate.capacity();
            if (crate.item().isEmpty()) emptiesNow++;
            if (crate.getBlockState().getValue(CrateBlock.BULGING)) bulgingNow++;
            if (bursting && crate.isFull() && !crate.getBlockState().getValue(CrateBlock.STRAPPED)) riskyNow++;
        }
        boolean changed = crates.size() != crateCount || emptiesNow != empties || bulgingNow != bulging || riskyNow != risky
                || missingNow != missing || takenNow != taken || storedNow != stored || capacityNow != capacity;
        crateCount = crates.size();
        empties = emptiesNow;
        bulging = bulgingNow;
        risky = riskyNow;
        missing = missingNow;
        taken = takenNow;
        stored = storedNow;
        capacity = capacityNow;
        if (changed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        if (crateCount >= ModAdvancements.DEWEY_DECIMAL_CRATES) {
            ModAdvancements.awardNearby(level, worldPosition, 8, ModAdvancements.DEWEY_DECIMAL_SHEEP);
        }
    }

    public int crateCount() {
        return crateCount;
    }

    public int empties() {
        return empties;
    }

    public int bulging() {
        return bulging;
    }

    /** Full crates that aren't strapped: one push too many and they may burst. */
    public int risky() {
        return risky;
    }

    /** Linked crates that are gone, or in chunks that aren't loaded. */
    public int missing() {
        return missing;
    }

    /** Crates next to its store that are in another controller's store. */
    public int taken() {
        return taken;
    }

    public long stored() {
        return stored;
    }

    public long capacity() {
        return capacity;
    }

    /** How full it all is, 0 to 100. */
    public int percent() {
        return capacity <= 0 ? 0 : (int) (100 * stored / capacity);
    }

    /** 0 with nothing stored, then 1 to 15 with how full it all is. */
    public int comparatorSignal() {
        if (stored <= 0 || capacity <= 0) return 0;
        return (int) Math.min(15, 1 + 14 * stored / capacity);
    }

    // ------------------------------------------------------------------ the wrench

    public enum LinkResult { LINKED, UNLINKED, ALREADY_IN, NOT_A_CRATE, TOO_FAR, FULL, TAKEN }

    /** Links the crate at {@code pos} to its store (and the crates touching it), or unlinks it if it's linked already. */
    public LinkResult toggleLink(ServerLevel level, BlockPos pos) {
        int index = links.indexOf(pos);
        if (index >= 0) {
            links.remove(index);
            setChanged();
            refresh(level);
            return LinkResult.UNLINKED;
        }
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) return LinkResult.NOT_A_CRATE;
        refresh(level);
        if (crates.contains(crate)) return LinkResult.ALREADY_IN;
        if (!pos.closerThan(worldPosition, range())) return LinkResult.TOO_FAR;
        if (crate.controller() != null && !crate.controller().equals(worldPosition)
                && level.getGameTime() - crate.claimedAt() <= CrateBlockEntity.CLAIM_TICKS) return LinkResult.TAKEN;
        if (crateCount >= maxCrates()) return LinkResult.FULL;
        links.add(pos.immutable());
        setChanged();
        refresh(level);
        return LinkResult.LINKED;
    }

    public List<BlockPos> links() {
        return List.copyOf(links);
    }

    // ------------------------------------------------------------------ pipes and hoppers

    public ResourceHandler<ItemResource> getItemHandler() {
        return handler;
    }

    /** The whole store as one item handler: a slot for each crate, and its own way of putting items in. */
    private final class Handler implements ResourceHandler<ItemResource> {
        private @Nullable CrateBlockEntity at(int index) {
            List<CrateBlockEntity> list = crates();
            if (index < 0 || index >= list.size()) return null;
            CrateBlockEntity crate = list.get(index);
            return crate.isRemoved() ? null : crate;
        }

        @Override
        public int size() {
            return crates().size();
        }

        @Override
        public ItemResource getResource(int index) {
            CrateBlockEntity crate = at(index);
            return crate == null ? ItemResource.EMPTY : crate.getItemHandler().getResource(0);
        }

        @Override
        public long getAmountAsLong(int index) {
            CrateBlockEntity crate = at(index);
            return crate == null ? 0 : crate.getItemHandler().getAmountAsLong(0);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            CrateBlockEntity crate = at(index);
            return crate == null ? 0 : crate.getItemHandler().getCapacityAsLong(0, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            CrateBlockEntity crate = at(index);
            return crate != null && crate.getItemHandler().isValid(0, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            CrateBlockEntity crate = at(index);
            return crate == null ? 0 : crate.getItemHandler().insert(0, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            CrateBlockEntity crate = at(index);
            return crate == null ? 0 : crate.getItemHandler().extract(0, resource, amount, transaction);
        }

        /**
         * Puts items where they belong: crates already holding them (not bulging), then empty unlocked crates, then the
         * bulging ones. If none of it fits, the fullest unstrapped crate holding it strains.
         */
        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) return 0;
            ItemStack stack = resource.toStack();
            List<CrateBlockEntity> list = crates();
            int left = amount;
            for (int pass = 0; pass < 3 && left > 0; pass++) {
                for (CrateBlockEntity crate : list) {
                    if (left <= 0) break;
                    if (crate.isRemoved() || crate.room(stack) <= 0) continue;
                    boolean holding = !crate.item().isEmpty(), bulges = crate.getBlockState().getValue(CrateBlock.BULGING);
                    boolean fits = switch (pass) {
                        case 0 -> holding && !bulges;
                        case 1 -> !holding;
                        default -> holding;
                    };
                    if (fits) left -= crate.getItemHandler().insert(0, resource, left, transaction);
                }
            }
            if (left == amount) {
                // Nowhere for it: the fullest crate holding it (or any full one) takes the strain.
                list.stream().filter(c -> !c.isRemoved() && c.isFull() && !c.getBlockState().getValue(CrateBlock.STRAPPED))
                        .max(Comparator.<CrateBlockEntity>comparingInt(c -> c.accepts(stack) ? 1 : 0).thenComparingInt(CrateBlockEntity::count))
                        .ifPresent(CrateBlockEntity::strain);
            }
            return amount - left;
        }
    }

    // ------------------------------------------------------------------ saving and syncing

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        links.clear();
        links.addAll(input.read("links", BlockPos.CODEC.listOf()).orElse(List.of()));
        crateCount = input.getIntOr("crates", 0);
        empties = input.getIntOr("empties", 0);
        bulging = input.getIntOr("bulging", 0);
        risky = input.getIntOr("risky", 0);
        missing = input.getIntOr("missing", 0);
        taken = input.getIntOr("taken", 0);
        stored = input.getLongOr("stored", 0);
        capacity = input.getLongOr("capacity", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("links", BlockPos.CODEC.listOf(), List.copyOf(links));
        output.putInt("crates", crateCount);
        output.putInt("empties", empties);
        output.putInt("bulging", bulging);
        output.putInt("risky", risky);
        output.putInt("missing", missing);
        output.putInt("taken", taken);
        output.putLong("stored", stored);
        output.putLong("capacity", capacity);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients get how its store is doing, for its front. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
