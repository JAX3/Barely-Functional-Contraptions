package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a Wool-Lined Crate's front: its item in the middle of the dark panel, and how many under it (gold when it's
 * locked to that item; the padlock itself is part of its block model). Lit by the light in front of it.
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity, CrateRenderer.State> {
    /** Just in front of the crate's front face. */
    private static final float PANEL_DEPTH = 0.5F + 0.004F;
    private static final float ITEM_Y = 9.5F / 16.0F - 0.5F, ITEM_SIZE = 0.42F;
    private static final float COUNT_Y = 4.5F / 16.0F - 0.5F, TEXT_WIDTH = 9.0F / 16.0F;
    private static final int COUNT = 0xFFF2E6D0, LOCKED = 0xFFFFC94A;

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        boolean hasItem, locked;
        int count;
        Direction facing = Direction.NORTH;
        int frontLight;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrateBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(CrateBlock.FACING);
        ItemStack stack = be.item();
        state.hasItem = !stack.isEmpty();
        state.count = be.count();
        state.locked = be.locked();
        state.item.clear();
        if (state.hasItem) {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.item, stack, ItemDisplayContext.GUI, be.getLevel(), null,
                    (int) be.getBlockPos().asLong());
        }
        state.frontLight = be.getLevel() == null ? LightCoordsUtil.FULL_BRIGHT
                : com.jax3.barelyfunctionalcontraptions.compat.McClient.lightAt(be.getLevel(), be.getBlockPos().relative(state.facing));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.hasItem) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-state.facing.toYRot()), 0, 0, 0);
        // The item, flattened onto the panel like a picture of it.
        poseStack.pushPose();
        poseStack.translate(0.0F, ITEM_Y, PANEL_DEPTH);
        poseStack.scale(ITEM_SIZE, ITEM_SIZE, 0.002F);
        state.item.submit(poseStack, collector, state.frontLight, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        // How many, under it.
        Font font = Minecraft.getInstance().font;
        String text = String.format(java.util.Locale.ROOT, "%,d", state.count);
        var line = Component.literal(text).getVisualOrderText();
        float scale = Math.min(TEXT_WIDTH / Math.max(1, font.width(line)), 0.24F / 16.0F);
        poseStack.translate(0.0F, COUNT_Y, PANEL_DEPTH + 0.002F);
        poseStack.scale(scale, -scale, scale);
        collector.submitText(poseStack, -font.width(line) / 2.0F, -4.5F, line, false, Font.DisplayMode.POLYGON_OFFSET,
                state.frontLight, state.locked ? LOCKED : COUNT, 0, 0);
        poseStack.popPose();
    }
}
