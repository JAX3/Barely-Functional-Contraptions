package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Pipes of one kind that touch, acting as one network: the fluid and item version of {@link CableNetwork}. The network
 * holds nothing. Whatever is put into any pipe goes straight out to the blocks plugged into the network ("endpoints"),
 * shared evenly, never back into the block it came from, and the network moves at most its kind's {@code transferRate}
 * per tick in total (mB for fluids, items for items).
 *
 * <p>Blocks that push out (the Fluid Reservoir, hoppers, the Mechanical Separator) push into pipes on their own. Each
 * tick the network also pulls from every endpoint slot holding something that block won't take back in: a pure source
 * such as the Automatic Shears' output-only inventory or the separator's output slots. Containers that take the same
 * thing in (chests, tanks, a generator's water tank, the separator's input slot) are never drained by the network, so
 * two of them on one network don't pass things back and forth.</p>
 *
 * <p>Each pipe side can be set to pull (drain the block, chests and tanks included), push (only fill it) or off, and
 * filtered to a list of items or fluids, which applies both ways; see {@link PipeSide}.</p>
 *
 * <p>Networks are never saved: they are rebuilt from the world whenever a pipe is placed, broken, plugged in or
 * unplugged, or unloaded.</p>
 */
public final class PipeNetwork<T extends Resource> {
    /** A block plugged into the network: its position, the face of it that touches the pipe, and that pipe side's settings. */
    public record Endpoint(BlockPos pos, Direction face, PipeSide side) {}

    private final List<PipeBlockEntity<T>> pipes;
    private final List<Endpoint> endpoints;
    private boolean valid = true;
    private final Moved moved = new Moved();
    private long lastTick = Long.MIN_VALUE;
    private long lastPull = Long.MIN_VALUE;
    private int rotation;
    private boolean delivering;
    private long window;
    private int rate;

    private final PipeKind<T> kind;

    private PipeNetwork(PipeKind<T> kind, List<PipeBlockEntity<T>> pipes, List<Endpoint> endpoints) {
        this.kind = kind;
        this.pipes = Collections.unmodifiableList(pipes);
        this.endpoints = Collections.unmodifiableList(endpoints);
    }

    /** Collects the pipes connected to {@code start} that are not already in a valid network, up to {@code maxNetworkSize}. */
    static <T extends Resource> PipeNetwork<T> build(Level level, PipeBlockEntity<T> start) {
        PipeKind<T> kind = start.kind();
        int limit = Math.max(1, kind.maxNetworkSize());
        List<PipeBlockEntity<T>> found = new ArrayList<>();
        List<Endpoint> ends = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<PipeBlockEntity<T>> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start.getBlockPos());
        while (!queue.isEmpty() && found.size() < limit) {
            PipeBlockEntity<T> pipe = queue.poll();
            found.add(pipe);
            BlockState state = pipe.getBlockState();
            for (Direction dir : Direction.values()) {
                BlockPos next = pipe.getBlockPos().relative(dir);
                CableConnection connection = state.hasProperty(ConduitBlock.SIDES.get(dir))
                        ? state.getValue(ConduitBlock.SIDES.get(dir)) : CableConnection.NONE;
                if (connection.plugged()) {
                    ends.add(new Endpoint(next, dir.getOpposite(), pipe.side(dir)));
                    continue;
                }
                if (!seen.add(next) || !level.isLoaded(next)) continue;
                if (level.getBlockEntity(next) instanceof PipeBlockEntity<?> other && other.kind() == kind && !other.isRemoved()
                        && !other.hasValidNetwork()) {
                    @SuppressWarnings("unchecked")
                    PipeBlockEntity<T> neighbour = (PipeBlockEntity<T>) other;
                    queue.add(neighbour);
                }
            }
        }
        PipeNetwork<T> network = new PipeNetwork<>(kind, found, ends);
        for (PipeBlockEntity<T> pipe : found) pipe.setNetwork(network);
        return network;
    }

    public List<PipeBlockEntity<T>> pipes() {
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

    public PipeKind<T> kind() {
        return kind;
    }

    /** Most the network moves per tick (mB or items). */
    public int transferRate() {
        return kind.transferRate();
    }

    /** Average amount per tick the network moved over the last second. */
    public int rate() {
        return rate;
    }

    /** How much the network can still move this tick. */
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

    private @Nullable ResourceHandler<T> handler(Level level, Endpoint end) {
        if (!level.isLoaded(end.pos())) return null;
        return level.getCapability(kind.capability(), end.pos(), end.face());
    }

    /**
     * Hands {@code amount} of {@code resource} inserted into a pipe on to the endpoints, inside {@code transaction}.
     * {@code from} is the block that inserted it (null if unknown). Returns how much was accepted.
     */
    public int deliver(Level level, @Nullable BlockPos from, T resource, int amount, TransactionContext transaction) {
        if (delivering || resource.isEmpty()) return 0;
        startTick(level);
        int allowed = Math.min(amount, remaining());
        if (allowed <= 0) return 0;
        delivering = true;
        int sent = 0;
        try {
            // Machines first, then storage (Fluid Tanks). Storage never fills from other storage, so two tanks on one
            // network don't pass the same fluid back and forth.
            boolean fromStorage = from != null && kind.isStorage(level.getBlockEntity(from));
            List<ResourceHandler<T>> machines = new ArrayList<>();
            List<ResourceHandler<T>> storage = new ArrayList<>();
            int n = endpoints.size();
            for (int i = 0; i < n; i++) {
                Endpoint end = endpoints.get((i + rotation) % n);
                if (end.pos().equals(from) || !end.side().mode().takesIn() || !end.side().allows(kind.idOf().apply(resource))) continue;
                ResourceHandler<T> handler = handler(level, end);
                if (handler == null) continue;
                if (kind.isStorage(level.getBlockEntity(end.pos()))) {
                    if (!fromStorage) storage.add(handler);
                } else {
                    machines.add(handler);
                }
            }
            sent = share(machines, resource, allowed, transaction);
            if (sent < allowed) sent += share(storage, resource, allowed - sent, transaction);
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
    private static <T extends Resource> int share(List<ResourceHandler<T>> targets, T resource, int amount, TransactionContext transaction) {
        int sent = 0;
        int left = targets.size();
        for (ResourceHandler<T> target : targets) {
            int portion = (int) Math.ceil((amount - sent) / (double) left--);
            if (portion > 0) sent += target.insert(resource, portion, transaction);
        }
        for (ResourceHandler<T> target : targets) {
            if (sent >= amount) break;
            sent += target.insert(resource, amount - sent, transaction);
        }
        return sent;
    }

    /**
     * True if {@code handler} won't take {@code resource} in although its slot isn't full: a pure source (a pump, the
     * Fluid Reservoir, the Automatic Shears, a separator's output), which the network may drain. A full container can't
     * be told apart from a source, so it is left alone. (Output-only slots report no capacity at all for things they
     * won't accept, so a capacity of 0 counts as "won't take it", not as full.)
     */
    private static <T extends Resource> boolean onlyGivesOut(ResourceHandler<T> handler, int slot, T resource) {
        long capacity = handler.getCapacityAsLong(slot, resource);
        if (capacity > 0 && handler.getAmountAsLong(slot) >= capacity) return false;
        try (Transaction simulate = Transaction.openRoot()) {
            return handler.insert(resource, 1, simulate) == 0;
        }
    }

    /** Once per game tick: pulls from every endpoint slot that only gives out, into the rest of the network. */
    void tick(ServerLevel level) {
        if (level.getGameTime() == lastPull) return;
        lastPull = level.getGameTime();
        startTick(level);
        int n = endpoints.size();
        if (n < 2) return;
        rotation = (rotation + 1) % n;
        for (int i = 0; i < n && remaining() > 0; i++) {
            Endpoint source = endpoints.get((i + rotation) % n);
            PipeSide.Mode mode = source.side().mode();
            if (mode == PipeSide.Mode.PUSH || mode == PipeSide.Mode.DISABLED) continue;
            ResourceHandler<T> handler = handler(level, source);
            if (handler == null) continue;
            for (int slot = 0; slot < handler.size() && remaining() > 0; slot++) {
                T resource = handler.getResource(slot);
                if (resource.isEmpty() || handler.getAmountAsLong(slot) <= 0 || !source.side().allows(kind.idOf().apply(resource))) continue;
                // Pull sides empty anything; normal sides only give up what the block won't take back in.
                if (mode != PipeSide.Mode.PULL && !onlyGivesOut(handler, slot, resource)) continue;
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

    /** Amount moved this tick, rolled back with the transactions that moved it. */
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
