package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

/**
 * Why a Zero Point Energy Module is or isn't linked, found while scanning its column. Shown by the GUI's status lamp
 * and beam diagram. Each reason reuses one of the machine lamp sprites and colours.
 */
public enum ZeroPointLink {
    LINKED(MachineStatus.PROCESSING),
    /** Nothing but see-through blocks (or unloaded chunks / the world's edge) within maxRange. */
    NO_MODULE(MachineStatus.IDLE),
    /** A solid block is in the way; {@code blockedAt} is how far along the column it is. */
    BLOCKED(MachineStatus.OUTPUT_FULL),
    /** The nearest module points the same way as this one. */
    WRONG_FACING(MachineStatus.OUTPUT_FULL),
    /** The nearest module faces back but there is less than minGap open space between them. */
    TOO_CLOSE(MachineStatus.OUTPUT_FULL);

    public final MachineStatus lamp;

    ZeroPointLink(MachineStatus lamp) {
        this.lamp = lamp;
    }

    public static ZeroPointLink byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NO_MODULE;
    }

    /** Result of one column scan: the partner's distance (0 unless linked), the reason, and where the scan stopped. */
    public record Scan(int distance, ZeroPointLink link, int stoppedAt) {
    }
}
