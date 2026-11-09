package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import net.minecraft.resources.Identifier;

final class JeiTextures {
    /**
     * 256x256 sheet: (0,0) 16x16 infusion icon, (0,16) 32x24 sheep, (32,16)/(40,16) 8x40 energy bar empty/full, and
     * the slate pieces that make the JEI pages match the machine GUIs (see tools/sheep/generate_textures.py).
     */
    static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/jei.png");
    /** Light and dim text of the slate machine GUIs. */
    static final int TEXT = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6;

    private JeiTextures() {}

    static mezz.jei.api.gui.drawable.IDrawableStatic sprite(mezz.jei.api.helpers.IGuiHelper gui, int u, int v, int w, int h) {
        return gui.drawableBuilder(TEXTURE, u, v, w, h).setTextureSize(256, 256).build();
    }

    static mezz.jei.api.gui.drawable.IDrawableStatic slot(mezz.jei.api.helpers.IGuiHelper gui) {
        return sprite(gui, 176, 0, 18, 18);
    }

    /** 26x26 output frame; place it 4 pixels up-left of the slot's 18x18 frame (5 from the item). */
    static mezz.jei.api.gui.drawable.IDrawableStatic bigSlot(mezz.jei.api.helpers.IGuiHelper gui) {
        return sprite(gui, 196, 0, 26, 26);
    }

    static mezz.jei.api.gui.drawable.IDrawableStatic arrowEmpty(mezz.jei.api.helpers.IGuiHelper gui) {
        return sprite(gui, 176, 32, 24, 16);
    }

    /** The purple arrow filling left to right over {@code ticks}. */
    static mezz.jei.api.gui.drawable.IDrawableAnimated arrowFilling(mezz.jei.api.helpers.IGuiHelper gui, int ticks) {
        return gui.drawableBuilder(TEXTURE, 176, 48, 24, 16).setTextureSize(256, 256)
                .buildAnimated(Math.max(1, ticks), mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection.LEFT, false);
    }

    static mezz.jei.api.gui.drawable.IDrawableStatic plus(mezz.jei.api.helpers.IGuiHelper gui) {
        return sprite(gui, 204, 32, 13, 13);
    }
}
