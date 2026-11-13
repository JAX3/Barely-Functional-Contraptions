package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Energy Meter's reading on its screens: the FE/t passing through, shortened (250, 12.5k, 1.2M), with
 * "FE/t" under it, glowing on the meter's one screen (the side that faces whoever placed it). When an end has
 * nothing to connect to, it shows NO INPUT, NO OUTPUT or NO IN/OUT in red instead.
 */
public class EnergyMeterRenderer implements BlockEntityRenderer<EnergyMeterBlockEntity, EnergyMeterRenderer.State> {
    /** The screen is 8x6 pixels; the text is fitted inside it. */
    private static final float SCREEN_WIDTH = 7.0F / 16.0F;
    private static final float SCREEN_CENTRE_Y = 7.5F / 16.0F;
    private static final int ON = 0xFF6FE3FF;
    private static final int OFF = 0xFF35506A;
    private static final int WARN = 0xFFFF5A5A;

    public static final class State extends BlockEntityRenderState {
        int rate;
        int missing;
        Direction in = Direction.DOWN;
        Direction out = Direction.UP;
        Direction screen = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EnergyMeterBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.rate = be.rate();
        state.missing = be.missing();
        state.in = be.inputFace();
        state.out = be.outputFace();
        state.screen = be.screenFace();
    }

    /** 250, 12.5k, 1.2M... */
    public static String shortRate(int rate) {
        if (rate < 1_000) return Integer.toString(rate);
        if (rate < 1_000_000) return String.format(Locale.ROOT, rate < 10_000 ? "%.1fk" : "%.0fk", rate / 1_000.0);
        return String.format(Locale.ROOT, "%.1fM", rate / 1_000_000.0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Font font = Minecraft.getInstance().font;
        boolean problem = state.rate == 0 && state.missing != 0;
        var number = Component.literal(problem ? "NO" : shortRate(state.rate)).getVisualOrderText();
        var unit = Component.literal(!problem ? "FE/t" : switch (state.missing) {
            case EnergyMeterBlockEntity.MISSING_INPUT -> "INPUT";
            case EnergyMeterBlockEntity.MISSING_OUTPUT -> "OUTPUT";
            default -> "IN/OUT";
        }).getVisualOrderText();
        int colour = problem ? WARN : state.rate > 0 ? ON : OFF;
        float numberScale = Math.min(SCREEN_WIDTH / Math.max(1, font.width(number)), 0.32F / 16.0F);
        float unitScale = Math.min(SCREEN_WIDTH / Math.max(1, font.width(unit)), 0.22F / 16.0F);
        Direction face = state.screen;
        if (face != state.in && face != state.out) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.rotateDegrees(Axis.YP, -face.toYRot());
            poseStack.translate(0.0F, SCREEN_CENTRE_Y - 0.5F, 0.5F + 0.002F);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.06F, 0.0F);
            poseStack.scale(numberScale, -numberScale, numberScale);
            collector.submitText(poseStack, -font.width(number) / 2.0F, -4.5F, number, false, Font.DisplayMode.POLYGON_OFFSET,
                    LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
            poseStack.popPose();
            poseStack.translate(0.0F, -0.1F, 0.0F);
            poseStack.scale(unitScale, -unitScale, unitScale);
            collector.submitText(poseStack, -font.width(unit) / 2.0F, -4.5F, unit, false, Font.DisplayMode.POLYGON_OFFSET,
                    LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
            poseStack.popPose();
        }
    }
}
