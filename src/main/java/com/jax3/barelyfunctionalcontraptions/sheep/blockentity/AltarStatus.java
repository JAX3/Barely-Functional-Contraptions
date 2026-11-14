package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** What the Infusion Altar is doing; its window, lamp and Jade show it. */
public enum AltarStatus {
    IDLE(0xFF9AA0A6, 0),
    INFUSING(0xFF55D66B, 1),
    NO_ENERGY(0xFFE8B23A, 2),
    MISSING_CATALYSTS(0xFFE35D5D, 3),
    NO_SHEEP(0xFFE35D5D, 3),
    NOT_ENOUGH_ITEMS(0xFFE35D5D, 3),
    INVALID_INPUT(0xFFE35D5D, 4);

    /** ARGB colour of the status text. */
    public final int color;
    /** Which of the shared status lamps it lights (same order as the Mechanical Separator's). */
    public final int lamp;

    AltarStatus(int color, int lamp) {
        this.color = color;
        this.lamp = lamp;
    }

    public static AltarStatus byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IDLE;
    }

    public String key() {
        return "gui.barelyfunctionalcontraptions.infusion_altar.status." + name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable(key());
    }
}
