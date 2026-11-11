package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Fluid Pipes that touch, acting as one network: the fluid version of {@link CableNetwork}. The network holds no
 * fluid. Fluid put into any pipe goes straight out to the blocks plugged into the network ("endpoints"), shared
 * evenly, never back into the block it came from, and the network moves at most {@code transferRate} mB per tick in
 * total (of any fluids).
 *
 * <p>Blocks that push fluid out (the Fluid Reservoir) push into pipes on their own. Each tick the network also pulls
 * from every endpoint that only gives fluid out (it won't take any back in), such as other mods' pumps. Tanks that take
 * fluid in as well are never drained by the network, so two tanks on one network don't pass the same fluid back and
 * forth and a generator's water stays in the generator.</p>
 *
 * <p>Networks are never saved: they are rebuilt from the world whenever a pipe is placed, broken, plugged in or
 * unplugged, or unloaded.</p>
 */
public final class FluidPipeNetwork {
    /** A block plugged into the network: its position and the face of it that touches the pipe. */
    public record Endpoint(BlockPos pos, Direction face) {}

    private final List<FluidPipeBlockEntity> pipes;
    private final List<Endpoint> endpoints;
    private boolean valid = true;
    private final Moved moved = new Moved();
    private long lastTick = Long.MIN_VALUE;
    private long lastPull = Long.MIN_VALUE;
    private int rotation;
    private boolean delivering;
    private long window;
    private int rate;

    private FluidPipeNetwork(List<FluidPipeBlockEntity> pipes, List<Endpoint> endpoints) {
        this.pipes = Collections.unmodifiableList(pipes);
        this.endpoints = Collections.unmodifiableList(endpoints);
    }

    /** Collects the pipes connected to {@code start} that are not already in a valid network, up to {@code maxNetworkSize}. */
    static FluidPipeNetwork build(Level level, FluidPipeBlockEntity start) {
        int limit = Math.max(1, ModConfigs.getInt(ModConfigs.SERVER.pipeMaxNetworkSize));
        List<FluidPipeBlockEntity> found = new ArrayList<>();
        List<Endpoint> ends = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<FluidPipeBlockEntity> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start.getBlockPos());
        while (!queue.isEmpty() && found.size() < limit) {
            FluidPipeBlockEntity pipe = queue.poll();
            found.add(pipe);
            BlockState state = pipe.getBlockState();
            for (Direction dir : Direction.values()) {
                BlockPos next = pipe.getBlockPos().relative(dir);
                CableConnection connection = state.hasProperty(ConduitBlock.SIDES.get(dir))
                        ? state.getValue(ConduitBlock.SIDES.get(dir)) : CableConnection.NONE;
                if (connection == CableConnection.MACHINE) {
                    ends.add(new Endpoint(next, dir.getOpposite()));
                    continue;
                }
                if (!seen.add(next) || !level.isLoaded(next)) continue;
                if (level.getBlockEntity(next) instanceof FluidPipeBlockEntity neighbour && !neighbour.isRemoved()
                        && !neighbour.hasValidNetwork()) {
                    queue.add(neighbour);
                }
            }
        }
        FluidPipeNetwork network = new FluidPipeNetwork(found, ends);
        for (FluidPipeBlockEntity pipe : found) pipe.setNetwork(network);
        return network;
    }

    public List<FluidPipeBlockEntity> pipes() {
        return pipes;
    }

    public List<Endpoint> endpoints() {
        return endpoints;
    }

    public int size() {
        return pipes.size();
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
    }

    /** Most mB the network moves per tick. */
    public static int transferRate() {
        return ModConfigs.getInt(ModConfigs.SERVER.pipeTransferRate);
    }

    /** Average mB/t the network moved over the last second. */
    public int rate() {
        return rate;
    }

    /** mB the network can still move this tick. */
    public int remaining() {
        return Math.max(0, transferRate() - moved.amount);
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

    private @Nullable ResourceHandler<FluidResource> handler(Level level, Endpoint end) {
        if (!level.isLoaded(end.pos())) return null;
        return level.getCapability(Capabilities.Fluid.BLOCK, end.pos(), end.face());
    }

    /**
     * Hands {@code amount} mB of {@code resource} inserted into a pipe on to the endpoints, inside {@code transaction}.
     * {@code from} is the block that inserted it (null if unknown). Returns how much was accepted.
     */
    public int deliver(Level level, @Nullable BlockPos from, FluidResource resource, int amount, TransactionContext transaction) {
        if (delivering || resource.isEmpty()) return 0;
        startTick(level);
        int allowed = Math.min(amount, remaining());
        if (allowed <= 0) return 0;
        delivering = true;
        int sent = 0;
        try {
            List<ResourceHandler<FluidResource>> targets = new ArrayList<>();
            int n = endpoints.size();
            for (int i = 0; i < n; i++) {
                Endpoint end = endpoints.get((i + rotation) % n);
                if (end.pos().equals(from)) continue;
                ResourceHandler<FluidResource> handler = handler(level, end);
                if (handler != null) targets.add(handler);
            }
            int left = targets.size();
            for (ResourceHandler<FluidResource> target : targets) {
                int share = (int) Math.ceil((allowed - sent) / (double) left--);
                if (share > 0) sent += target.insert(resource, share, transaction);
            }
            for (ResourceHandler<FluidResource> target : targets) {
                if (sent >= allowed) break;
                sent += target.insert(resource, allowed - sent, transaction);
            }
        } finally {
            delivering = false;
        }
        if (sent > 0) {
            moved.updateSnapshots(transaction);
            moved.amount += sent;
        }
        return sent;
    }

    /**
     * True if {@code handler} won't take {@code resource} in although its slot isn't full: a pure source (a pump, the
     * Fluid Reservoir), which the network may drain. A full tank can't be told apart from a source, so it is left alone.
     */
    private static boolean onlyGivesOut(ResourceHandler<FluidResource> handler, int slot, FluidResource resource) {
        if (handler.getAmountAsLong(slot) >= handler.getCapacityAsLong(slot, resource)) return false;
        try (Transaction simulate = Transaction.openRoot()) {
            return handler.insert(resource, 1, simulate) == 0;
        }
    }

    /** Once per game tick: pulls from every endpoint that only gives fluid out, into the rest of the network. */
    void tick(ServerLevel level) {
        if (level.getGameTime() == lastPull) return;
        lastPull = level.getGameTime();
        startTick(level);
        int n = endpoints.size();
        if (n < 2) return;
        rotation = (rotation + 1) % n;
        for (int i = 0; i < n && remaining() > 0; i++) {
            Endpoint source = endpoints.get((i + rotation) % n);
            ResourceHandler<FluidResource> handler = handler(level, source);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.size() && remaining() > 0; slot++) {
                FluidResource resource = handler.getResource(slot);
                if (resource.isEmpty() || handler.getAmountAsLong(slot) <= 0 || !onlyGivesOut(handler, slot, resource)) continue;
                int available;
                try (Transaction simulate = Transaction.openRoot()) {
                    available = handler.extract(resource, remaining(), simulate);
                }
                if (available <= 0) continue;
                try (Transaction tx = Transaction.openRoot()) {
                    int sent = deliver(level, source.pos(), resource, available, tx);
                    if (sent <= 0) continue;
                    if (handler.extract(resource, sent, tx) == sent) tx.commit();
                }
            }
        }
    }

    /** mB moved this tick, rolled back with the transactions that moved it. */
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
