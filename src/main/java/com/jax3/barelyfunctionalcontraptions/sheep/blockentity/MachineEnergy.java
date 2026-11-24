package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import java.util.function.IntSupplier;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * A machine's FE buffer: filled from any FE source or cable, never giving any out, and used by the machine itself
 * ({@link #consume}, {@link #drain}). Its capacity and insert rate follow the config; {@link #refreshLimits()}, called
 * each tick, picks up changes at once (FE over a reduced capacity is lost).
 */
public class MachineEnergy extends SimpleEnergyHandler {
    private final IntSupplier configCapacity, configMaxInput;
    private final Runnable onChanged;

    public MachineEnergy(IntSupplier capacity, IntSupplier maxInput, Runnable onChanged) {
        super(capacity.getAsInt(), maxInput.getAsInt(), 0);
        this.configCapacity = capacity;
        this.configMaxInput = maxInput;
        this.onChanged = onChanged;
    }

    /** Takes its capacity and insert rate from the config again. */
    public void refreshLimits() {
        refreshLimits(0);
    }

    /** As {@link #refreshLimits()}, but holding and taking at least twice {@code neededPerTick} (so it can keep up). */
    public void refreshLimits(int neededPerTick) {
        capacity = Math.max(configCapacity.getAsInt(), neededPerTick * 2);
        maxInsert = Math.max(configMaxInput.getAsInt(), neededPerTick * 2);
        if (energy > capacity) set(capacity);
    }

    public int getEnergy() {
        return energy;
    }

    /** Uses {@code amount} FE if it has that much; false (using none) if not. */
    public boolean consume(int amount) {
        if (amount > energy) return false;
        set(energy - amount);
        return true;
    }

    /** Uses {@code amount} FE, or all it has if that's less. */
    public void drain(int amount) {
        set(Math.max(0, energy - amount));
    }

    /** Fills it at once (GameTests, the screenshot harness). */
    public void fill() {
        set(capacity);
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }
}
