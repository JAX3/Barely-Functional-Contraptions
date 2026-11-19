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

    /** The screen that's open, or null. */
    public static net.minecraft.client.gui.screens.@org.jspecify.annotations.Nullable Screen screen(Minecraft mc) {
        return mc.gui.screen();
    }

    /** Opens {@code screen}, or closes the open one when it's null. */
    public static void setScreen(Minecraft mc, net.minecraft.client.gui.screens.@org.jspecify.annotations.Nullable Screen screen) {
        mc.gui.setScreen(screen);
    }

    /** Clears the toasts in the corner (dev harness). */
    public static void clearToasts(Minecraft mc) {
        mc.gui.toastManager().clear();
    }

    /** Clears the chat (dev harness). */
    public static void clearChat(Minecraft mc) {
        mc.gui.hud.getChat().clearMessages(false);
    }

    /** Hides or shows the HUD, as F1 does (dev harness). */
    public static void setHudHidden(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
    }

    /** The window's main render target (dev harness screenshots). */
    public static com.mojang.blaze3d.pipeline.RenderTarget mainTarget(Minecraft mc) {
        return mc.gameRenderer.mainRenderTarget();
    }
}
