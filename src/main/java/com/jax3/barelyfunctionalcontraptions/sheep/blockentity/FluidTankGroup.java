package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidTankBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Face-connected Fluid Tanks acting as one tank: the fluid version of {@link EnergyCubeGroup}. Its capacity is the sum
 * of the members' and its transfer rate is {@code transferPerTank} times the number of tanks. A group holds one fluid.
 *
 * <p>Each tank keeps its own share (so a broken tank takes exactly its share with it). The group fills from the bottom
 * up like a real tank: after any change the leader {@linkplain #settle() settles} the fluid so the lowest layer is
 * filled first and every tank in a layer holds the same amount. That is also what is drawn: each tank shows its own
 * share, so a group shows one level running across it.</p>
 *
 * <p>Tanks holding different fluids never join one group, even when they touch; each forms its own. Groups are never
 * saved, and are rebuilt from the world whenever a tank is placed, broken or unloaded.</p>
 */
public final class FluidTankGroup {
    /** Members from the bottom layer up. */
    private final List<FluidTankBlockEntity> members;
    private boolean valid = true;
    private boolean needsSettle = true;

    private FluidTankGroup(List<FluidTankBlockEntity> members) {
        members.sort(Comparator.comparingInt(t -> t.getBlockPos().getY()));
        this.members = Collections.unmodifiableList(members);
    }

    /**
     * Collects the tanks connected to {@code start} that are not already in a valid group and hold the same fluid (or
     * none), up to {@code maxGroupSize}, and makes them a group.
     */
    static FluidTankGroup build(Level level, FluidTankBlockEntity start) {
        int limit = Math.max(1, ModConfigs.getInt(ModConfigs.SERVER.tankMaxGroupSize));
        List<FluidTankBlockEntity> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<FluidTankBlockEntity> queue = new ArrayDeque<>();
        FluidResource fluid = start.storage().getResource(0);
        queue.add(start);
        seen.add(start.getBlockPos());
        while (!queue.isEmpty() && found.size() < limit) {
            FluidTankBlockEntity tank = queue.poll();
            found.add(tank);
            if (fluid.isEmpty()) fluid = tank.storage().getResource(0);
            for (Direction dir : Direction.values()) {
                BlockPos next = tank.getBlockPos().relative(dir);
                if (seen.contains(next) || !level.isLoaded(next)) continue;
                if (level.getBlockEntity(next) instanceof FluidTankBlockEntity neighbour && !neighbour.isRemoved()
                        && !neighbour.hasValidGroup()) {
                    FluidResource theirs = neighbour.storage().getResource(0);
                    if (!fluid.isEmpty() && !theirs.isEmpty() && !theirs.equals(fluid)) continue; // another fluid: its own group
                    if (fluid.isEmpty() && !theirs.isEmpty()) fluid = theirs;
                    seen.add(next);
                    queue.add(neighbour);
                }
            }
        }
        FluidTankGroup group = new FluidTankGroup(found);
        for (FluidTankBlockEntity tank : found) tank.setGroup(group);
        return group;
    }

    public List<FluidTankBlockEntity> members() {
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

    /** The tank that ticks for the whole group (the first one, at the bottom). */
    public FluidTankBlockEntity leader() {
        return members.getFirst();
    }

    void markChanged() {
        needsSettle = true;
    }

    /** The group's fluid, or empty. */
    public FluidResource fluid() {
        for (FluidTankBlockEntity tank : members) {
            FluidResource resource = tank.storage().getResource(0);
            if (!resource.isEmpty()) return resource;
        }
        return FluidResource.EMPTY;
    }

    public long amount() {
        long sum = 0;
        for (FluidTankBlockEntity tank : members) sum += tank.storage().getAmountAsLong(0);
        return sum;
    }

    public long capacity() {
        return (long) members.size() * ModConfigs.getInt(ModConfigs.SERVER.tankCapacityPerTank);
    }

    /** mB per transfer (each insert or extract call, and the group's push each tick): per-tank rate x tanks. */
    public int rate() {
        long rate = (long) ModConfigs.getInt(ModConfigs.SERVER.tankTransferPerTank) * size();
        return (int) Math.min(Integer.MAX_VALUE, rate);
    }

    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) return 0;
        FluidResource current = fluid();
        if (!current.isEmpty() && !current.equals(resource)) return 0;
        int left = Math.min(amount, rate());
        int total = 0;
        for (FluidTankBlockEntity tank : members) {
            if (left <= 0) break;
            int got = tank.storage().insert(0, resource, left, transaction);
            left -= got;
            total += got;
        }
        return total;
    }

    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0 || !resource.equals(fluid())) return 0;
        int left = Math.min(amount, rate());
        int total = 0;
        for (int i = members.size() - 1; i >= 0 && left > 0; i--) {
            int got = members.get(i).storage().extract(0, resource, left, transaction);
            left -= got;
            total += got;
        }
        return total;
    }

    /**
     * Spreads the group's fluid out the way it would settle in one big tank: the bottom layer fills first, and the
     * tanks of a layer share its fluid evenly. Only runs after something changed, outside any transaction.
     */
    void settle() {
        if (!needsSettle) return;
        needsSettle = false;
        FluidResource fluid = fluid();
        long left = amount();
        int perTank = ModConfigs.getInt(ModConfigs.SERVER.tankCapacityPerTank);
        int i = 0;
        while (i < members.size()) {
            int y = members.get(i).getBlockPos().getY();
            int end = i;
            while (end < members.size() && members.get(end).getBlockPos().getY() == y) end++;
            int count = end - i;
            long layer = Math.min(left, (long) count * perTank);
            left -= layer;
            for (int k = 0; k < count; k++) {
                int share = (int) (layer / count + (k < layer % count ? 1 : 0));
                members.get(i + k).settleTo(share > 0 ? fluid : FluidResource.EMPTY, share);
            }
            i = end;
        }
    }

    /**
     * Pushes up to {@link #rate()} mB into the fluid handlers touching the group (never into other tanks, which would
     * just pass fluid back and forth). Returns the mB sent.
     */
    int push(ServerLevel level) {
        FluidResource fluid = fluid();
        if (fluid.isEmpty()) return 0;
        int budget = rate();
        int sent = 0;
        for (FluidTankBlockEntity tank : members) {
            for (Direction dir : Direction.values()) {
                if (sent >= budget || amount() <= 0) return sent;
                BlockPos target = tank.getBlockPos().relative(dir);
                if (!level.isLoaded(target) || level.getBlockState(target).getBlock() instanceof FluidTankBlock) continue;
                @Nullable ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, target, dir.getOpposite());
                if (handler == null) continue;
                int offer = (int) Math.min(budget - sent, amount());
                int accepts;
                try (Transaction simulate = Transaction.openRoot()) {
                    accepts = handler.insert(fluid, offer, simulate);
                }
                if (accepts <= 0) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int out = extract(fluid, accepts, transaction);
                    int in = handler.insert(fluid, out, transaction);
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
