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
    private static final float DEPTH = -6F / 16F + 0.003F;
    private static final float SCALE = 0.0085F;
    /** The width a column needs for a machine's name and its number, in font pixels. */
    private static final float COLUMN_PX = 88;
    private static final int NAME = 0xFFDFE4F5, NUMBER = 0xFFBFE8FF, DIM = 0xFF6A7080;

    public static final class State extends BlockEntityRenderState {
        List<StatusPanelBlockEntity.Row> rows = List.of();
        Direction facing = Direction.NORTH;
        /** Whether this panel draws (the top-left one of its screen), and the screen's size in panels. */
        boolean master = true;
        StatusPanelBlockEntity.Theme theme = StatusPanelBlockEntity.Theme.CLASSIC;
        int width = 1, height = 1;
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
        state.theme = be.theme();
        var level = be.getLevel();
        state.master = level == null || StatusPanelBlock.master(level, be.getBlockPos()).equals(be.getBlockPos());
        int[] size = level == null ? new int[]{1, 1} : StatusPanelBlock.size(level, be.getBlockPos());
        state.width = size[0];
        state.height = size[1];
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(StatusPanelBlockEntity be) {
        return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(3);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.master) return; // the screen's top-left panel draws all of it
        Font font = Minecraft.getInstance().font;
        // The whole screen, inside the bezel round its outside edge. The rows go in as many columns, at as big a size,
        // as fills it best: one panel's four rows at the smallest, a big screen's few rows large.
        float screenW = state.width - 4F / 16F, screenH = state.height - 4F / 16F;
        int n = Math.max(1, state.rows.size());
        float scale = 0;
        int columns = 1;
        for (int c = 1; c <= state.width; c++) {
            int per = (n + c - 1) / c;
            float s = Math.min(Math.min(screenH / (per * 21F), screenW / c / COLUMN_PX), SCALE * 3);
            if (s > scale) {
                scale = s;
                columns = c;
            }
        }
        int perColumn = (n + columns - 1) / columns;
        int columnW = Math.round(screenW / columns / scale) - 4;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-state.facing.toYRot()), 0, 0, 0);
        poseStack.translate(-0.5F + 2F / 16F, 0.5F - 2F / 16F, DEPTH);
        poseStack.scale(scale, -scale, scale);
        if (state.rows.isEmpty()) {
            text(collector, poseStack, font, Component.translatable("screen.barelyfunctionalcontraptions.status_panel.empty").getVisualOrderText(), 0, 2, DIM);
            text(collector, poseStack, font, Component.translatable("screen.barelyfunctionalcontraptions.status_panel.empty2").getVisualOrderText(), 0, 12, DIM);
        }
        for (int i = 0; i < state.rows.size(); i++) {
            StatusPanelBlockEntity.Row row = state.rows.get(i);
            int x = (i / perColumn) * (columnW + 4) + (i / perColumn > 0 ? 2 : 0), y = 1 + (i % perColumn) * 21;
            int colour = row.report().level().color;
            text(collector, poseStack, font, Component.literal("\u25a0").getVisualOrderText(), x, y, colour);
            String name = font.plainSubstrByWidth(row.displayName().getString(), columnW - 9);
            text(collector, poseStack, font, Component.literal(name).getVisualOrderText(), x + 8, y, state.theme.name);
            String number = row.report().number();
            int numberWidth = font.width(number);
            String status = font.plainSubstrByWidth(row.report().statusText().getString(), columnW - numberWidth - 4);
            text(collector, poseStack, font, Component.literal(status).getVisualOrderText(), x, y + 9, colour);
            text(collector, poseStack, font, Component.literal(number).getVisualOrderText(), x + columnW - numberWidth, y + 9, state.theme.number);
        }
        poseStack.popPose();
    }

    private static void text(SubmitNodeCollector collector, PoseStack poseStack, Font font, FormattedCharSequence text, float x, float y, int colour) {
        collector.submitText(poseStack, x, y, text, false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
    }
}
