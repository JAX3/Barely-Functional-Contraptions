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
 * Draws the beam between a linked pair of Zero Point Energy Modules. Only the floor module (facing up) draws it,
 * from the tip of its emitter to the tip of the ceiling module's, using the beacon beam's geometry and texture. It is
 * blue; stained glass anywhere in its path colours the whole beam, mixing colours like a beacon beam does.
 */
public class ZeroPointBeamRenderer implements BlockEntityRenderer<ZeroPointEnergyModuleBlockEntity, ZeroPointBeamRenderer.State> {
    public static final int BEAM_COLOR = ZeroPointEnergyModuleBlockEntity.BEAM_COLOR;
    /** Height of the emitter tip above the bottom of the module's block, for both modules (8 px: plate plus emitter). */
    private static final float EMITTER_TIP = 0.5F;

    public static final class State extends BlockEntityRenderState {
        /** Beam pieces, bottom to top; empty when no beam is drawn. */
        final java.util.List<Piece> pieces = new java.util.ArrayList<>();
        float animationTime;
        /** Which way the lead module points; the beam is drawn "up" and turned to point this way. */
        Direction facing = Direction.UP;
    }

    /** One single-coloured stretch of beam, from {@code bottom} to {@code top} blocks above the floor module. */
    record Piece(float bottom, float top, int color) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ZeroPointEnergyModuleBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.pieces.clear();
        state.animationTime = be.getLevel() != null ? Math.floorMod(be.getLevel().getGameTime(), 40) + partialTicks : 0;
        if (!be.getBlockState().hasProperty(ZeroPointEnergyModuleBlock.FACING)) return;
        Direction facing = be.getBlockState().getValue(ZeroPointEnergyModuleBlock.FACING);
        int distance = be.getLinkDistance();
        if (!ZeroPointEnergyModuleBlockEntity.isLead(facing) || distance <= 0) return;
        state.facing = facing;
        // Both emitter tips are half a block into their blocks (the floor one at +0.5, the ceiling one at
        // distance + 0.5), so the beam is exactly as long as the distance between the two modules.
        float beamTop = distance + EMITTER_TIP;
        // Stained glass anywhere in the gap colours the whole beam, end to end, in the glass's (blended) colour.
        state.pieces.add(new Piece(EMITTER_TIP, beamTop, be.getBeamColor()));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.pieces.isEmpty()) return;
        // Turn the block about its centre so "up" points along the emitter: the beacon helper always draws upwards.
        poseStack.pushPose();
        poseStack.rotateAround(state.facing.getRotation(), 0.5F, 0.5F, 0.5F);
        for (Piece piece : state.pieces) {
            float length = piece.top() - piece.bottom();
            poseStack.pushPose();
            poseStack.translate(0, piece.bottom(), 0);
            poseStack.scale(1, length, 1); // the beacon helper works in whole blocks; stretch one block to the piece
            BeaconRenderer.submitBeaconBeam(poseStack, collector, BeaconRenderer.BEAM_LOCATION, length, state.animationTime,
                    0, 1, piece.color(), 0.1F, 0.16F);
            poseStack.popPose();
        }
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
        Direction facing = be.getBlockState().hasProperty(ZeroPointEnergyModuleBlock.FACING)
                ? be.getBlockState().getValue(ZeroPointEnergyModuleBlock.FACING) : Direction.UP;
        return new AABB(pos).minmax(new AABB(pos.relative(facing, Math.max(1, be.getLinkDistance()))));
    }
}
