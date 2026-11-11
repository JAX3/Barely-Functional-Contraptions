package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the blue beam between a linked pair of Zero Point Energy Modules. Only the floor module (facing up) draws it,
 * from the tip of its emitter to the tip of the ceiling module's, using the beacon beam's geometry and texture.
 */
public class ZeroPointBeamRenderer implements BlockEntityRenderer<ZeroPointEnergyModuleBlockEntity, ZeroPointBeamRenderer.State> {
    public static final int BEAM_COLOR = 0xFF3FC8FF;
    /** Height of the emitter tip above the bottom of the module's block, for both modules (8 px: plate plus emitter). */
    private static final float EMITTER_TIP = 0.5F;

    public static final class State extends BlockEntityRenderState {
        float length;
        float animationTime;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ZeroPointEnergyModuleBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        boolean floorModule = be.getBlockState().hasProperty(ZeroPointEnergyModuleBlock.FACING)
                && be.getBlockState().getValue(ZeroPointEnergyModuleBlock.FACING) == Direction.UP;
        // Both emitter tips are half a block into their blocks (the floor one at +0.5, the ceiling one at
        // distance + 0.5), so the beam is exactly as long as the distance between the two modules.
        state.length = floorModule && be.getLinkDistance() > 0 ? be.getLinkDistance() : 0;
        state.animationTime = be.getLevel() != null ? Math.floorMod(be.getLevel().getGameTime(), 40) + partialTicks : 0;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.length <= 0) return;
        poseStack.pushPose();
        poseStack.translate(0, EMITTER_TIP, 0);
        poseStack.scale(1, state.length, 1); // the beacon helper works in whole blocks; stretch one block to the gap
        BeaconRenderer.submitBeaconBeam(poseStack, collector, BeaconRenderer.BEAM_LOCATION, state.length, state.animationTime,
                0, 1, BEAM_COLOR, 0.1F, 0.16F);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(ZeroPointEnergyModuleBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + Math.max(1, be.getLinkDistance()) + 1.0, pos.getZ() + 1.0);
    }
}
