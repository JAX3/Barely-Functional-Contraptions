package com.jax3.barelyfunctionalcontraptions.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Client calls that differ between the Minecraft versions the mod builds for (Minecraft 26.2). Client only. */
public final class McClient {
    private McClient() {}

    /** Draws a model tinted {@code tint} (ARGB) with the given render type. */
    public static <S> void submitTintedModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack,
                                             RenderType renderType, int light, int overlay, int tint, int outlineColor) {
        collector.submitModel(model, state, poseStack, renderType, light, overlay, tint, null, outlineColor, null);
    }

    /** Moves the mouse to window pixel ({@code x}, {@code y}), as the window would (dev harness). */
    public static void mouseMove(Minecraft mc, long handle, double x, double y) {
        invoke(mc, "onMove", new Class<?>[]{long.class, double.class, double.class}, handle, x, y);
    }

    /** Presses (1) or releases (0) a mouse button, as the window would (dev harness). */
    public static void mouseButton(Minecraft mc, long handle, MouseButtonInfo button, int action) {
        invoke(mc, "onButton", new Class<?>[]{long.class, net.minecraft.client.input.MouseButtonInfo.class, int.class}, handle, button, action);
    }

    /** 26.2 keeps the mouse handler's input methods private; the dev harness reaches them by reflection. */
    private static void invoke(Minecraft mc, String name, Class<?>[] types, Object... args) {
        try {
            var method = net.minecraft.client.MouseHandler.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(mc.mouseHandler, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't call MouseHandler." + name, e);
        }
    }
}
