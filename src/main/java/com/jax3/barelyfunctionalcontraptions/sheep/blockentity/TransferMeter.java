package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * How much a cable or pipe network has moved: this tick (inside transactions, so a cancelled transfer doesn't count),
 * and on average per tick over the last second.
 */
final class TransferMeter extends SnapshotJournal<Integer> {
    private int thisTick;
    private long lastTick = Long.MIN_VALUE;
    /** Moved since the last rate update, and the average per tick over the last second. */
    private long window;
    private int rate;

    /** Starts a new tick's count on the first transfer of each tick, and updates the rate once a second. */
    void startTick(Level level) {
        long now = level.getGameTime();
        if (now == lastTick || isInTransaction()) return;
        if (now % 20 == 0 && lastTick != Long.MIN_VALUE) {
            rate = (int) Math.min(Integer.MAX_VALUE, window / 20);
            window = 0;
        }
        lastTick = now;
        thisTick = 0;
    }

    /** Counts {@code amount} moved in {@code transaction}. */
    void record(int amount, TransactionContext transaction) {
        if (amount <= 0) return;
        updateSnapshots(transaction);
        thisTick += amount;
    }

    /** Moved so far this tick. */
    int thisTick() {
        return thisTick;
    }

    /** Average moved per tick over the last second. */
    int rate() {
        return rate;
    }

    @Override
    protected Integer createSnapshot() {
        return thisTick;
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        thisTick = snapshot;
    }

    @Override
    protected void onRootCommit(Integer originalState) {
        window += thisTick - originalState;
    }
}
