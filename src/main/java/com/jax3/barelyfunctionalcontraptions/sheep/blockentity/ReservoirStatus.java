package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

/** What the Fluid Reservoir is doing, shown by its GUI lamp and Jade. */
public enum ReservoirStatus {
    /** Over an infinite water source and making water. */
    FILLING(MachineStatus.PROCESSING),
    /** Tank full: waiting for buckets, pipes or a neighbour to take water. */
    FULL(MachineStatus.OUTPUT_FULL),
    /** Over a water source that would not refill itself (fewer than two neighbouring sources). */
    NOT_INFINITE(MachineStatus.NO_ENERGY),
    /** No water source block directly below. */
    NO_SOURCE(MachineStatus.INVALID_INPUT);

    /** The machine status whose lamp sprite and colour this status reuses. */
    public final MachineStatus lamp;

    ReservoirStatus(MachineStatus lamp) {
        this.lamp = lamp;
    }

    public int color() {
        return lamp.color;
    }

    public static ReservoirStatus byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NO_SOURCE;
    }
}
