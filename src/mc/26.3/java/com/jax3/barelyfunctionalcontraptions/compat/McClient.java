package com.jax3.barelyfunctionalcontraptions.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Client calls that differ between the Minecraft versions the mod builds for (Minecraft 26.3). Client only. */
public final class McClient {
    private McClient() {}

    /** Draws a model tinted {@code tint} (ARGB) with the given render type. */
    public static <S> void submitTintedModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack,
                                             RenderType renderType, int light, int overlay, int tint, int outlineColor) {
        collector.submitModel(model, state, poseStack, renderType, light, overlay, tint, null, outlineColor);
    }

    /** Moves the mouse to window pixel ({@code x}, {@code y}), as the window would (dev harness). */
    public static void mouseMove(Minecraft mc, long handle, double x, double y) {
        mc.mouseHandler.onMove(handle, x, y, 0, 0);
    }

    /** Presses (1) or releases (0) a mouse button, as the window would (dev harness). */
    public static void mouseButton(Minecraft mc, long handle, MouseButtonInfo button, int action) {
        mc.mouseHandler.onButton(handle, button, action);
    }
}
