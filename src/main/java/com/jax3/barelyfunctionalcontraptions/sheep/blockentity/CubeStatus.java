package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

/** What a Hume Energy Cube group is doing, shown by its GUI lamp. Reuses the machine lamp sprites and colours. */
public enum CubeStatus {
    CHARGING(MachineStatus.PROCESSING),
    DISCHARGING(MachineStatus.NO_ENERGY),
    FULL(MachineStatus.OUTPUT_FULL),
    EMPTY(MachineStatus.REDSTONE_DISABLED),
    IDLE(MachineStatus.IDLE);

    public final MachineStatus lamp;

    CubeStatus(MachineStatus lamp) {
        this.lamp = lamp;
    }

    public int color() {
        return lamp.color;
    }

    public static CubeStatus byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IDLE;
    }

    /** From the group's state and the last second's average flow in and out. */
    public static CubeStatus of(long amount, long capacity, int in, int out) {
        if (in > out) return amount >= capacity ? FULL : CHARGING;
        if (out > in) return amount <= 0 ? EMPTY : DISCHARGING;
        if (amount <= 0) return EMPTY;
        if (amount >= capacity) return FULL;
        return IDLE;
    }
}
