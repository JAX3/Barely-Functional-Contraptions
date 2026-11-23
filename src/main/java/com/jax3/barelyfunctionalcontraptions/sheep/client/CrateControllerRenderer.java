package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateControllerBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
 * The Crate Controller's readout, on the little dark screen in its brass front: how many crates are in its store, and
 * how full it all is (green, amber past 90%, red when a crate is missing or could burst). Glows like a screen.
 */
public class CrateControllerRenderer implements BlockEntityRenderer<CrateControllerBlockEntity, CrateControllerRenderer.State> {
    private static final float DEPTH = 0.5F + 0.004F, WIDTH = 8.0F / 16.0F;
    private static final float TOP_Y = 11.4F / 16.0F - 0.5F, BOTTOM_Y = 8.6F / 16.0F - 0.5F;
    private static final int TEXT = 0xFFF2E6D0, OK = 0xFF7CFF8A, WARN = 0xFFFFC94A, BAD = 0xFFFF6B6B;

    public static final class State extends BlockEntityRenderState {
        int crates, percent, colour;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrateControllerBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(CrateControllerBlock.FACING);
        state.crates = be.crateCount();
        state.percent = be.percent();
        state.colour = be.missing() > 0 || be.risky() > 0 ? BAD : be.percent() >= 90 || (be.crateCount() > 0 && be.empties() == 0) ? WARN : OK;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-state.facing.toYRot()), 0, 0, 0);
        line(poseStack, collector, font, Component.translatable("block.barelyfunctionalcontraptions.crate_controller.crates", state.crates), TOP_Y, TEXT);
        line(poseStack, collector, font, Component.literal(state.percent + "%"), BOTTOM_Y, state.colour);
        poseStack.popPose();
    }

    private static void line(PoseStack poseStack, SubmitNodeCollector collector, Font font, Component text, float y, int colour) {
        var order = text.getVisualOrderText();
        float scale = Math.min(WIDTH / Math.max(1, font.width(order)), 0.22F / 16.0F);
        poseStack.pushPose();
        poseStack.translate(0.0F, y, DEPTH);
        poseStack.scale(scale, -scale, scale);
        collector.submitText(poseStack, -font.width(order) / 2.0F, -4.5F, order, false, Font.DisplayMode.POLYGON_OFFSET,
                LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
        poseStack.popPose();
    }
}
