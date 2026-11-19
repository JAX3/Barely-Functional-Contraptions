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
 * Draws a linked Zero Point Energy Module's emitter, slowly turning the opposite way to the beam, and the beam between
 * a linked pair. (An unlinked module's emitter is part of its block model and stands still.) Only the lead module
 * (the floor one, facing up) draws the beam,
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
        /** Which way the module points; the beam and emitter are drawn "up" and turned to point this way. */
        Direction facing = Direction.UP;
        /** Whether to draw the turning emitter (linked modules), and its angle in degrees about its own axis. */
        boolean emitter;
        float emitterAngle;
        int light;
    }

    /** The lit emitter's texture, on the block atlas. */
    private static final net.minecraft.resources.Identifier EMITTER_TEXTURE =
            com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep.id("block/zero_point_energy_module/emitter_on");
    /** Degrees the beam (the beacon's) and so the emitter turn per tick. */
    private static final float SPIN_PER_TICK = 2.25F;

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
        state.emitter = false;
        state.animationTime = be.getLevel() != null ? Math.floorMod(be.getLevel().getGameTime(), 40) + partialTicks : 0;
        if (!be.getBlockState().hasProperty(ZeroPointEnergyModuleBlock.FACING)) return;
        Direction facing = be.getBlockState().getValue(ZeroPointEnergyModuleBlock.FACING);
        state.facing = facing;
        if (be.getBlockState().getValue(ZeroPointEnergyModuleBlock.LINKED)) {
            // The beam turns one way about the lead module's axis; each emitter turns the other way. The partner
            // points the opposite way, so turning it the same way about its own axis turns it against the beam too.
            float time = be.getLevel() != null ? Math.floorMod(be.getLevel().getGameTime(), 160) + partialTicks : 0;
            state.emitter = true;
            state.emitterAngle = (ZeroPointEnergyModuleBlockEntity.isLead(facing) ? -1 : 1) * time * SPIN_PER_TICK;
            state.light = net.minecraft.util.LightCoordsUtil.FULL_BRIGHT;
        }
        int distance = be.getLinkDistance();
        if (!ZeroPointEnergyModuleBlockEntity.isLead(facing) || distance <= 0) return;
        // Both emitter tips are half a block into their blocks (the floor one at +0.5, the ceiling one at
        // distance + 0.5), so the beam is exactly as long as the distance between the two modules.
        float beamTop = distance + EMITTER_TIP;
        // Stained glass anywhere in the gap colours the whole beam, end to end, in the glass's (blended) colour.
        state.pieces.add(new Piece(EMITTER_TIP, beamTop, be.getBeamColor()));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.pieces.isEmpty() && !state.emitter) return;
        // Turn the block about its centre so "up" points along the emitter: the beacon helper always draws upwards.
        poseStack.pushPose();
        poseStack.rotateAround(state.facing.getRotation(), 0.5F, 0.5F, 0.5F);
        if (state.emitter) submitEmitter(state, poseStack, collector);
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

    /** The emitter: an 8x4x8 pixel block on the plate (4 to 8 px up), turned about its centre by the state's angle. */
    private static void submitEmitter(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        var sprite = net.minecraft.client.Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(EMITTER_TEXTURE);
        poseStack.pushPose();
        poseStack.rotateAround(com.mojang.math.Axis.YP.rotationDegrees(state.emitterAngle), 0.5F, 0, 0.5F);
        collector.submitCustomGeometry(poseStack, net.minecraft.client.renderer.Sheets.cutoutBlockItemSheet(), (pose, buffer) -> {
            float a = 4 / 16F, b = 12 / 16F, y0 = 4 / 16F, y1 = 8 / 16F;
            // Top: the middle 8x8 of the texture.
            quad(buffer, pose, sprite, state.light, 0, 1, 0, new float[][]{{a, y1, a}, {a, y1, b}, {b, y1, b}, {b, y1, a}}, 4, 4, 12, 12);
            // Sides: rows 0-4, columns 4-12 of the texture, as in the block model.
            quad(buffer, pose, sprite, state.light, 0, 0, -1, new float[][]{{b, y1, a}, {b, y0, a}, {a, y0, a}, {a, y1, a}}, 4, 0, 12, 4);
            quad(buffer, pose, sprite, state.light, 0, 0, 1, new float[][]{{a, y1, b}, {a, y0, b}, {b, y0, b}, {b, y1, b}}, 4, 0, 12, 4);
            quad(buffer, pose, sprite, state.light, -1, 0, 0, new float[][]{{a, y1, a}, {a, y0, a}, {a, y0, b}, {a, y1, b}}, 4, 0, 12, 4);
            quad(buffer, pose, sprite, state.light, 1, 0, 0, new float[][]{{b, y1, b}, {b, y0, b}, {b, y0, a}, {b, y1, a}}, 4, 0, 12, 4);
        });
        poseStack.popPose();
    }

    /**
     * One face, corners in order top-left, bottom-left, bottom-right, top-right as seen from outside, textured with the
     * pixel rectangle (u0, v0)-(u1, v1) of the sprite.
     */
    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                             int light, float nx, float ny, float nz, float[][] c, float u0, float v0, float u1, float v1) {
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, c[i][0], c[i][1], c[i][2])
                    .setColor(-1)
                    .setUv(sprite.getU(uv[i][0] / 16F), sprite.getV(uv[i][1] / 16F))
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
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
