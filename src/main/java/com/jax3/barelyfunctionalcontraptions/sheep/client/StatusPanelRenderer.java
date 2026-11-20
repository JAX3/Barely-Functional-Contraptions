package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Machine Status Panel's screen: two lines for each linked machine, its name with a status lamp, then its
 * status in its colour and its key number on the right. With nothing linked, it says how to link machines.
 */
public class StatusPanelRenderer implements BlockEntityRenderer<StatusPanelBlockEntity, StatusPanelRenderer.State> {
    /** The screen: 12 of the block's 16 pixels across, in front of the panel's face. */
    private static final float SCREEN_W = 12F / 16F, SCREEN_H = 12F / 16F;
    private static final float DEPTH = -6F / 16F + 0.003F;
    private static final float SCALE = 0.0085F;
    private static final int NAME = 0xFFDFE4F5, NUMBER = 0xFFBFE8FF, DIM = 0xFF6A7080;

    public static final class State extends BlockEntityRenderState {
        List<StatusPanelBlockEntity.Row> rows = List.of();
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StatusPanelBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.rows = be.rows();
        state.facing = be.getBlockState().getValue(StatusPanelBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Font font = Minecraft.getInstance().font;
        int width = Math.round(SCREEN_W / SCALE);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-state.facing.toYRot()), 0, 0, 0);
        poseStack.translate(-SCREEN_W / 2, SCREEN_H / 2, DEPTH);
        poseStack.scale(SCALE, -SCALE, SCALE);
        if (state.rows.isEmpty()) {
            text(collector, poseStack, font, Component.translatable("screen.barelyfunctionalcontraptions.status_panel.empty").getVisualOrderText(), 0, 2, DIM);
            text(collector, poseStack, font, Component.translatable("screen.barelyfunctionalcontraptions.status_panel.empty2").getVisualOrderText(), 0, 12, DIM);
        }
        int y = 1;
        for (StatusPanelBlockEntity.Row row : state.rows) {
            int colour = row.report().level().color;
            text(collector, poseStack, font, Component.literal("■").getVisualOrderText(), 0, y, colour);
            String name = font.plainSubstrByWidth(Component.translatable(row.name()).getString(), width - 9);
            text(collector, poseStack, font, Component.literal(name).getVisualOrderText(), 8, y, NAME);
            String number = row.report().number();
            int numberWidth = font.width(number);
            String status = font.plainSubstrByWidth(row.report().statusText().getString(), width - numberWidth - 4);
            text(collector, poseStack, font, Component.literal(status).getVisualOrderText(), 0, y + 9, colour);
            text(collector, poseStack, font, Component.literal(number).getVisualOrderText(), width - numberWidth, y + 9, NUMBER);
            y += 21;
        }
        poseStack.popPose();
    }

    private static void text(SubmitNodeCollector collector, PoseStack poseStack, Font font, FormattedCharSequence text, float x, float y, int colour) {
        collector.submitText(poseStack, x, y, text, false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
    }
}
