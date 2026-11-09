package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * FE buffer of the Automatic Shears (NeoForge {@code Capabilities.Energy.BLOCK}). Accepts energy from any FE source or
 * cable and never outputs it. Capacity and insert rate follow {@code [automatic_shears]} in the config and are
 * refreshed every tick, like the separator's.
 */
public class ShearsEnergyStorage extends SimpleEnergyHandler {
    private final Runnable onChanged;

    public ShearsEnergyStorage(Runnable onChanged) {
        super(ModConfigs.getInt(ModConfigs.SERVER.automaticShearsEnergyCapacity), ModConfigs.getInt(ModConfigs.SERVER.automaticShearsMaxEnergyInput), 0);
        this.onChanged = onChanged;
    }

    public void refreshLimits() {
        this.capacity = ModConfigs.getInt(ModConfigs.SERVER.automaticShearsEnergyCapacity);
        this.maxInsert = ModConfigs.getInt(ModConfigs.SERVER.automaticShearsMaxEnergyInput);
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
