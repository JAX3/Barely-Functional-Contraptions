package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Face-connected Hume Energy Cubes acting as one battery. Its buffer is the sum of the members' buffers, and its
 * transfer rate is {@code transferPerCube} times the number of cubes. Each cube still keeps its own share of the energy
 * (so a broken cube takes exactly its share with it); the group fills its members in order and drains them in reverse.
 *
 * <p>Groups are never saved: they are rebuilt on demand from the blocks in the world whenever a cube is placed, broken
 * or unloaded, so they always match what is actually connected.</p>
 */
public final class EnergyCubeGroup {
    private final List<EnergyCubeBlockEntity> members;
    private boolean valid = true;

    private EnergyCubeGroup(List<EnergyCubeBlockEntity> members) {
        this.members = Collections.unmodifiableList(members);
    }

    /**
     * Collects the cubes connected to {@code start} that are not already in a valid group, up to {@code maxGroupSize},
     * and makes them a group.
     */
    static EnergyCubeGroup build(Level level, EnergyCubeBlockEntity start) {
        int limit = Math.max(1, ModConfigs.getInt(ModConfigs.SERVER.energyCubeMaxGroupSize));
        List<EnergyCubeBlockEntity> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<EnergyCubeBlockEntity> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start.getBlockPos());
        while (!queue.isEmpty() && found.size() < limit) {
            EnergyCubeBlockEntity cube = queue.poll();
            found.add(cube);
            for (Direction dir : Direction.values()) {
                BlockPos next = cube.getBlockPos().relative(dir);
                if (!seen.add(next) || !level.isLoaded(next)) continue;
                if (level.getBlockEntity(next) instanceof EnergyCubeBlockEntity neighbour && !neighbour.isRemoved()
                        && !neighbour.hasValidGroup()) {
                    queue.add(neighbour);
                }
            }
        }
        EnergyCubeGroup group = new EnergyCubeGroup(found);
        for (EnergyCubeBlockEntity cube : found) cube.setGroup(group);
        return group;
    }

    public List<EnergyCubeBlockEntity> members() {
        return members;
    }

    public int size() {
        return members.size();
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        valid = false;
    }

    /** The cube that ticks for the whole group. */
    public EnergyCubeBlockEntity leader() {
        return members.getFirst();
    }

    public long amount() {
        long sum = 0;
        for (EnergyCubeBlockEntity cube : members) sum += cube.storage().getAmountAsLong();
        return sum;
    }

    public long capacity() {
        long sum = 0;
        for (EnergyCubeBlockEntity cube : members) sum += cube.storage().getCapacityAsLong();
        return sum;
    }

    /** FE per transfer (each insert or extract call, and the group's push each tick): per-cube rate x cubes. */
    public int rate() {
        long rate = (long) ModConfigs.getInt(ModConfigs.SERVER.energyCubeTransferPerCube) * size();
        return (int) Math.min(Integer.MAX_VALUE, rate);
    }

    /** 0..8 for the charge shown in the side windows: 0 only when empty, 8 only when full. */
    public int chargeLevel() {
        long capacity = capacity(), amount = amount();
        if (amount <= 0 || capacity <= 0) return 0;
        if (amount >= capacity) return EnergyCubeBlock.MAX_CHARGE;
        int steps = EnergyCubeBlock.MAX_CHARGE;
        return (int) Math.max(1, Math.min(steps - 1, Math.round(amount * (double) steps / capacity)));
    }

    public int insert(int amount, TransactionContext transaction) {
        int left = Math.min(amount, rate());
        int total = 0;
        for (EnergyCubeBlockEntity cube : members) {
            if (left <= 0) break;
            int got = cube.storage().insert(left, transaction);
            left -= got;
            total += got;
        }
        return total;
    }

    public int extract(int amount, TransactionContext transaction) {
        int left = Math.min(amount, rate());
        int total = 0;
        for (int i = members.size() - 1; i >= 0 && left > 0; i--) {
            int got = members.get(i).storage().extract(left, transaction);
            left -= got;
            total += got;
        }
        return total;
    }

    /**
     * Pushes up to {@link #rate()} FE into the energy receivers touching the group (never into other cubes, which would
     * just bounce energy back and forth). Returns the FE sent.
     */
    int push(ServerLevel level) {
        int budget = rate();
        int sent = 0;
        for (EnergyCubeBlockEntity cube : members) {
            for (Direction dir : Direction.values()) {
                if (sent >= budget || amount() <= 0) return sent;
                BlockPos target = cube.getBlockPos().relative(dir);
                if (!level.isLoaded(target) || level.getBlockState(target).getBlock() instanceof EnergyCubeBlock) continue;
                @Nullable EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, target, dir.getOpposite());
                if (handler == null) continue;
                int offer = (int) Math.min(budget - sent, amount());
                int accepts;
                try (Transaction simulate = Transaction.openRoot()) {
                    accepts = handler.insert(offer, simulate); // not committed: just asks how much fits
                }
                if (accepts <= 0) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int out = extract(accepts, transaction);
                    int in = handler.insert(out, transaction);
                    if (in == out) {
                        transaction.commit();
                        sent += in;
                    }
                }
            }
        }
        return sent;
    }
}
