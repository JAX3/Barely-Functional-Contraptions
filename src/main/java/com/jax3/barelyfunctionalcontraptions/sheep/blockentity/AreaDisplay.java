package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import net.minecraft.world.phys.AABB;

/**
 * A machine that works on an area (the Automatic Shears, the Blaze Sheep Furnace) and can show it as an outline.
 * Toggled with a sneak-right-click with an empty hand (or the shears' window button); the setting is saved and synced
 * to clients, where {@code AreaOutlineRenderer} draws the box.
 */
public interface AreaDisplay {
    /** True while the outline is switched on. */
    boolean showsArea();

    void setShowsArea(boolean show);

    /** The area, in world coordinates. */
    AABB displayedArea();

    /** Outline colour, ARGB. */
    int areaColor();

    default boolean toggleArea() {
        setShowsArea(!showsArea());
        return showsArea();
    }

    /** Toggles the area and tells {@code player} which way it went ("Showing / Hiding the working area"). */
    default boolean toggleArea(net.minecraft.world.entity.player.Player player) {
        boolean shown = toggleArea();
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.barelyfunctionalcontraptions.area." + (shown ? "shown" : "hidden"))
                .withColor(areaColor() & 0xFFFFFF));
        return shown;
    }
}
