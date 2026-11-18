package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * FE buffer of the separator, exposed through NeoForge's {@code Capabilities.Energy.BLOCK} (the transfer-API
 * {@code EnergyHandler}). Accepts energy from any FE source/cable; never outputs energy.
 *
 * <p>Capacity and insert rate follow the server config and are refreshed every tick, so changing the config takes
 * effect immediately (excess energy above a reduced capacity is clamped). The insert rate is never less than twice what
 * the wool or bale being separated uses a tick, so a costly one (a Wither Wool Bale, or a data pack's) can always be fed
 * fast enough to run at full speed, and catch up after a pause.</p>
 */
public class SeparatorEnergyStorage extends SimpleEnergyHandler {
    private final Runnable onChanged;

    public SeparatorEnergyStorage(Runnable onChanged) {
        super(ModConfigs.getInt(ModConfigs.SERVER.energyCapacity), ModConfigs.getInt(ModConfigs.SERVER.maxEnergyInput), 0);
        this.onChanged = onChanged;
    }

    public void refreshLimits(int neededPerTick) {
        this.capacity = Math.max(ModConfigs.getInt(ModConfigs.SERVER.energyCapacity), neededPerTick * 2);
        this.maxInsert = Math.max(ModConfigs.getInt(ModConfigs.SERVER.maxEnergyInput), neededPerTick * 2);
        if (this.energy > this.capacity) set(this.capacity);
    }

    public int getEnergy() {
        return energy;
    }

    /** Machine-internal consumption (bypasses maxExtract, which is 0 for external callers). */
    public boolean consume(int amount) {
        if (amount > energy) return false;
        set(energy - amount);
        return true;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }
}
