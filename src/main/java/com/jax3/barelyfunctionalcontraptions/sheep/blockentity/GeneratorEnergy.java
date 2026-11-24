package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A generator's FE buffer: filled by the generator itself ({@link #generate}), never from outside, and emptied into its
 * neighbours ({@link #pushToNeighbours}) or one block ({@link #pushInto}). Its capacity follows the config
 * ({@link #resize}, called each tick).
 */
public class GeneratorEnergy extends SimpleEnergyHandler {
    private final Runnable onChanged;

    public GeneratorEnergy(int capacity, Runnable onChanged) {
        super(capacity, 0, Integer.MAX_VALUE);
        this.onChanged = onChanged;
    }

    /** Adds {@code amount} FE, up to its capacity. */
    public void generate(int amount) {
        int previous = energy;
        energy = (int) Math.min(capacity, (long) energy + Math.max(0, amount));
        if (energy != previous) onEnergyChanged(previous);
    }

    /** Sets its capacity, losing any FE over it. */
    public void resize(int newCapacity) {
        if (capacity == newCapacity) return;
        int previous = energy;
        capacity = newCapacity;
        energy = Math.min(energy, capacity);
        if (energy != previous) onEnergyChanged(previous);
    }

    /**
     * Pushes up to {@code max} FE (and no more than it holds) into the blocks round {@code pos}, side by side, skipping
     * the sides {@code skip} rejects (given the side and the block there); how much went.
     */
    public int pushToNeighbours(ServerLevel level, BlockPos pos, int max, BiPredicate<Direction, BlockPos> skip) {
        int budget = Math.min(max, getAmountAsInt()), moved = 0;
        for (Direction dir : Direction.values()) {
            if (budget - moved <= 0) break;
            BlockPos target = pos.relative(dir);
            if (skip.test(dir, target)) continue;
            moved += pushInto(level.getCapability(Capabilities.Energy.BLOCK, target, dir.getOpposite()), budget - moved);
        }
        return moved;
    }

    /** Pushes up to {@code max} FE (and no more than it holds) into {@code target}, if there is one; how much went. */
    public int pushInto(EnergyHandler target, int max) {
        int budget = Math.min(max, getAmountAsInt());
        if (target == null || budget <= 0) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = target.insert(budget, tx);
            if (moved > 0 && extract(moved, tx) == moved) {
                tx.commit();
                return moved;
            }
        }
        return 0;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }
}
