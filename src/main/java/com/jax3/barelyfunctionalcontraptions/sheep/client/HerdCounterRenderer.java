package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.HerdCounterBlock;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Herd Counter's screen: the number its mode counts, big, with the mode under it (FULL shows the count out of
 * its target, lit once it's reached; a filtered count's label is gold), glowing green-on-black. And its area outline,
 * while that's switched on.
 */
public class HerdCounterRenderer implements BlockEntityRenderer<HerdCounterBlockEntity, HerdCounterRenderer.State> {
    private static final float SCREEN_WIDTH = 8.0F / 16.0F;
    private static final float SCREEN_CENTRE_Y = 9.5F / 16.0F;
    /** The front of the panel is a sixteenth in front of the block's middle. */
    private static final float SCREEN_DEPTH = 1.0F / 16.0F + 0.002F;
    private static final int ON = 0xFF7CFF8A, OFF = 0xFF2F6A3A, FILTERED = 0xFFFFD27F;

    private final AreaOutlineRenderer<HerdCounterBlockEntity> outline = new AreaOutlineRenderer<>();

    public static final class State extends BlockEntityRenderState {
        final AreaOutlineRenderer.State outline = new AreaOutlineRenderer.State();
        int value, target;
        Mode mode = Mode.COUNT;
        boolean filtered;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HerdCounterBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        outline.extractRenderState(be, state.outline, partialTicks, cameraPosition, breakProgress);
        state.value = be.value();
        state.target = be.target();
        state.mode = be.mode();
        state.filtered = be.filterResource() != null;
        state.facing = be.getBlockState().getValue(HerdCounterBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        outline.submit(state.outline, poseStack, collector, camera);
        Font font = Minecraft.getInstance().font;
        boolean full = state.mode == Mode.FULL;
        var number = Component.literal(full ? state.value + "/" + state.target : Integer.toString(state.value)).getVisualOrderText();
        var label = Component.translatable("screen.barelyfunctionalcontraptions.herd_counter." + state.mode.key()).getVisualOrderText();
        int colour = full ? (state.value >= state.target ? ON : OFF) : state.value > 0 ? ON : OFF;
        float numberScale = Math.min(SCREEN_WIDTH / Math.max(1, font.width(number)), 0.36F / 16.0F);
        float labelScale = Math.min(SCREEN_WIDTH / Math.max(1, font.width(label)), 0.2F / 16.0F);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-state.facing.toYRot()), 0, 0, 0);
        poseStack.translate(0.0F, SCREEN_CENTRE_Y - 0.5F, SCREEN_DEPTH);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.06F, 0.0F);
        poseStack.scale(numberScale, -numberScale, numberScale);
        collector.submitText(poseStack, -font.width(number) / 2.0F, -4.5F, number, false, Font.DisplayMode.POLYGON_OFFSET,
                LightCoordsUtil.FULL_BRIGHT, colour, 0, 0);
        poseStack.popPose();
        poseStack.translate(0.0F, -0.12F, 0.0F);
        poseStack.scale(labelScale, -labelScale, labelScale);
        collector.submitText(poseStack, -font.width(label) / 2.0F, -4.5F, label, false, Font.DisplayMode.POLYGON_OFFSET,
                LightCoordsUtil.FULL_BRIGHT, state.filtered ? FILTERED : colour, 0, 0);
        poseStack.popPose();
    }

    /** Its area reaches well past the block: keep drawing it while the block itself is off screen. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(HerdCounterBlockEntity be) {
        return outline.getRenderBoundingBox(be);
    }
}
