package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCableBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Energy Cables that touch, acting as one network. The network has no buffer: energy put into any cable goes straight
 * out to the blocks plugged into the network ("endpoints"), shared evenly, and the network moves at most
 * {@code transferRate} FE per tick in total.
 *
 * <p>Where the energy may go:</p>
 * <ul>
 *     <li>Energy from a generator (Mycelial Generator, Zero Point Energy Module, other mods' generators) goes to the
 *     machines first, then any leftover charges the Hume Energy Cubes on the network.</li>
 *     <li>Energy from a Hume Energy Cube only goes to machines, never into another cube, so two cubes on one network
 *     don't pass the same energy back and forth.</li>
 *     <li>It never goes back into the block it came from.</li>
 * </ul>
 * Generators and cubes push into cables on their own; each tick the network also pulls from every endpoint that has
 * energy to give, so sources set not to push (or other mods' generators) still work.
 *
 * <p>Like Hume Energy Cube groups, networks are never saved: they are rebuilt from the world whenever a cable is
 * placed, broken, plugged in or unplugged, or unloaded.</p>
 */
public final class CableNetwork {
    /** A block plugged into the network: its position and the face of it that touches the cable. */
    public record Endpoint(BlockPos pos, Direction face) {}

    private final List<EnergyCableBlockEntity> cables;
    private final List<Endpoint> endpoints;
    private boolean valid = true;
    private final Moved moved = new Moved();
    private long lastTick = Long.MIN_VALUE;
    private long lastPull = Long.MIN_VALUE;
    private int rotation;
    /** True while handing energy out, so a block that pushes straight back into the network can't recurse. */
    private boolean delivering;
    /** FE moved since the last rate update, and the average FE/t over the last second. */
    private long window;
    private int rate;

    private CableNetwork(List<EnergyCableBlockEntity> cables, List<Endpoint> endpoints) {
        this.cables = Collections.unmodifiableList(cables);
        this.endpoints = Collections.unmodifiableList(endpoints);
    }

    /** Collects the cables connected to {@code start} that are not already in a valid network, up to {@code maxNetworkSize}. */
    static CableNetwork build(Level level, EnergyCableBlockEntity start) {
        int limit = Math.max(1, ModConfigs.getInt(ModConfigs.SERVER.cableMaxNetworkSize));
        List<EnergyCableBlockEntity> found = new ArrayList<>();
        List<Endpoint> ends = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<EnergyCableBlockEntity> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start.getBlockPos());
        while (!queue.isEmpty() && found.size() < limit) {
            EnergyCableBlockEntity cable = queue.poll();
            found.add(cable);
            BlockState state = cable.getBlockState();
            for (Direction dir : Direction.values()) {
                BlockPos next = cable.getBlockPos().relative(dir);
                CableConnection connection = state.hasProperty(EnergyCableBlock.SIDES.get(dir))
                        ? state.getValue(EnergyCableBlock.SIDES.get(dir)) : CableConnection.NONE;
                if (connection.plugged()) {
                    ends.add(new Endpoint(next, dir.getOpposite()));
                    continue;
                }
                if (!seen.add(next) || !level.isLoaded(next)) continue;
                if (level.getBlockEntity(next) instanceof EnergyCableBlockEntity neighbour && !neighbour.isRemoved()
                        && !neighbour.hasValidNetwork()) {
                    queue.add(neighbour);
                }
            }
        }
        CableNetwork network = new CableNetwork(found, ends);
        for (EnergyCableBlockEntity cable : found) cable.setNetwork(network);
        return network;
    }

    public List<EnergyCableBlockEntity> cables() {
        return cables;
    }

    public List<Endpoint> endpoints() {
        return endpoints;
    }

    public int size() {
        return cables.size();
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
    }

    /** Most FE the network moves per tick. */
    public static int transferRate() {
        return ModConfigs.getInt(ModConfigs.SERVER.cableTransferRate);
    }

    /** Average FE/t the network moved over the last second. */
    public int rate() {
        return rate;
    }

    /** FE the network can still move this tick. */
    public int remaining() {
        return Math.max(0, transferRate() - moved.amount);
    }

    /** Hume Energy Cubes store energy rather than make it: cube-to-cube transfers are skipped. */
    static boolean isStorage(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EnergyCubeBlockEntity;
    }

    private void startTick(Level level) {
        long now = level.getGameTime();
        if (now == lastTick || moved.isInTransaction()) return;
        if (now % 20 == 0 && lastTick != Long.MIN_VALUE) {
            rate = (int) Math.min(Integer.MAX_VALUE, window / 20);
            window = 0;
        }
        lastTick = now;
        moved.amount = 0;
    }

    /**
     * Hands {@code amount} FE inserted into a cable on to the endpoints, inside {@code transaction}. {@code from} is the
     * block that inserted it (null if unknown). Returns how much was accepted.
     */
    public int deliver(Level level, @Nullable BlockPos from, int amount, TransactionContext transaction) {
        if (delivering) return 0;
        startTick(level);
        int allowed = Math.min(amount, remaining());
        if (allowed <= 0) return 0;
        boolean fromStorage = from != null && isStorage(level, from);
        delivering = true;
        int sent;
        try {
            List<EnergyHandler> machines = new ArrayList<>();
            List<EnergyHandler> storage = new ArrayList<>();
            int n = endpoints.size();
            for (int i = 0; i < n; i++) {
                Endpoint end = endpoints.get((i + rotation) % n);
                if (end.pos().equals(from) || !level.isLoaded(end.pos())) continue;
                EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, end.pos(), end.face());
                if (handler == null) continue;
                if (isStorage(level, end.pos())) {
                    if (!fromStorage) storage.add(handler);
                } else {
                    machines.add(handler);
                }
            }
            sent = share(machines, allowed, transaction);
            if (sent < allowed) sent += share(storage, allowed - sent, transaction);
        } finally {
            delivering = false;
        }
        if (sent > 0) {
            moved.updateSnapshots(transaction);
            moved.amount += sent;
        }
        return sent;
    }

    /** Splits {@code amount} evenly between {@code targets}, then gives what some couldn't take to the others. */
    private static int share(List<EnergyHandler> targets, int amount, TransactionContext transaction) {
        int total = 0;
        int left = targets.size();
        for (EnergyHandler target : targets) {
            int share = (int) Math.ceil((amount - total) / (double) left--);
            if (share > 0) total += target.insert(share, transaction);
        }
        for (EnergyHandler target : targets) {
            if (total >= amount) break;
            total += target.insert(amount - total, transaction);
        }
        return total;
    }

    /**
     * Once per game tick (whichever cable ticks first): pulls from every endpoint with energy to give, into the rest
     * of the network, within what is left of this tick's transfer rate.
     */
    void tick(ServerLevel level) {
        if (level.getGameTime() == lastPull) return;
        lastPull = level.getGameTime();
        startTick(level);
        int n = endpoints.size();
        if (n < 2) return;
        rotation = (rotation + 1) % n;
        for (int i = 0; i < n && remaining() > 0; i++) {
            Endpoint source = endpoints.get((i + rotation) % n);
            if (!level.isLoaded(source.pos())) continue;
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, source.pos(), source.face());
            if (handler == null) continue;
            int available;
            try (Transaction simulate = Transaction.openRoot()) {
                available = handler.extract(remaining(), simulate);
            }
            if (available <= 0) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int sent = deliver(level, source.pos(), available, tx);
                if (sent <= 0) continue;
                if (handler.extract(sent, tx) == sent) tx.commit();
            }
        }
    }

    /** FE moved this tick, rolled back with the transactions that moved it. */
    private final class Moved extends SnapshotJournal<Integer> {
        int amount;

        @Override
        protected Integer createSnapshot() {
            return amount;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            amount = snapshot;
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            window += amount - originalState;
        }
    }
}
