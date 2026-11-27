package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** What the Mechanical Separator is currently doing; shown in the GUI and synced via the menu. */
public enum MachineStatus {
    IDLE(0xFF9AA0A6),
    PROCESSING(0xFF55D66B),
    NO_ENERGY(0xFFE8B23A),
    OUTPUT_FULL(0xFFE35D5D),
    INVALID_INPUT(0xFFE35D5D),
    REDSTONE_DISABLED(0xFFB04040),
    /** The Mechanical Separator is clogged with lint from the last wool (it clears itself, or shears clear it). */
    JAMMED(0xFFE8B23A),
    /** The Snow Sheep Chiller has frosted over, left full too long; it thaws once there's room (or a Blaze sheep warms it). */
    FROSTED(0xFF7FD8FF);

    /** ARGB colour of the status lamp and text. */
    public final int color;

    MachineStatus(int color) {
        this.color = color;
    }

    /** Which of the machines' status lamp sprites it shows (a jam shows the amber one). */
    public int lamp() {
        return this == JAMMED || this == FROSTED ? NO_ENERGY.ordinal() : ordinal();
    }

    public static MachineStatus byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IDLE;
    }

    public Component displayName() {
        return Component.translatable("gui.barelyfunctionalcontraptions.status." + name().toLowerCase(Locale.ROOT));
    }

    public Component description() {
        return Component.translatable("gui.barelyfunctionalcontraptions.status." + name().toLowerCase(Locale.ROOT) + ".desc");
    }
}
