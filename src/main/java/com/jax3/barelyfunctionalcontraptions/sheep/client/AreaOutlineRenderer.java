package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AreaDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the working area of a machine ({@link AreaDisplay}: the Automatic Shears, the Blaze Sheep Furnace) as a
 * coloured box outline while it is switched on, with a faint grid line on each block boundary along the floor.
 */
public class AreaOutlineRenderer<T extends BlockEntity & AreaDisplay> implements BlockEntityRenderer<T, AreaOutlineRenderer.State> {
    /** Pulled in a hair so the lines don't hide inside the faces of blocks along the edge. */
    private static final double INSET = 0.01;

    public static final class State extends BlockEntityRenderState {
        boolean show;
        /** The area relative to the block's own corner. */
        AABB box = new AABB(0, 0, 0, 1, 1, 1);
        int color;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.show = be.showsArea();
        if (!state.show) return;
        BlockPos pos = be.getBlockPos();
        state.box = be.displayedArea().move(-pos.getX(), -pos.getY(), -pos.getZ()).deflate(INSET);
        state.color = be.areaColor();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.show) return;
        AABB b = state.box;
        int color = state.color;
        int faint = (color & 0x00FFFFFF) | 0x80000000;
        collector.submitCustomGeometry(poseStack, RenderTypes.LINES, (pose, buffer) -> {
            float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
            float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
            // The twelve edges of the box.
            line(buffer, pose, x0, y0, z0, x1, y0, z0, color);
            line(buffer, pose, x0, y1, z0, x1, y1, z0, color);
            line(buffer, pose, x0, y0, z1, x1, y0, z1, color);
            line(buffer, pose, x0, y1, z1, x1, y1, z1, color);
            line(buffer, pose, x0, y0, z0, x0, y1, z0, color);
            line(buffer, pose, x1, y0, z0, x1, y1, z0, color);
            line(buffer, pose, x0, y0, z1, x0, y1, z1, color);
            line(buffer, pose, x1, y0, z1, x1, y1, z1, color);
            line(buffer, pose, x0, y0, z0, x0, y0, z1, color);
            line(buffer, pose, x1, y0, z0, x1, y0, z1, color);
            line(buffer, pose, x0, y1, z0, x0, y1, z1, color);
            line(buffer, pose, x1, y1, z0, x1, y1, z1, color);
            // Block lines across the floor, so the 3x3 reads at a glance. Where the area reaches below the machine
            // (the shears also shear a block down), the grid is drawn at the machine's own level, on the ground.
            float gy = Math.max(y0, (float) INSET);
            if (gy > y0) {
                line(buffer, pose, x0, gy, z0, x1, gy, z0, color);
                line(buffer, pose, x0, gy, z1, x1, gy, z1, color);
                line(buffer, pose, x0, gy, z0, x0, gy, z1, color);
                line(buffer, pose, x1, gy, z0, x1, gy, z1, color);
            }
            for (double x = Math.ceil(b.minX); x < b.maxX; x++) {
                line(buffer, pose, (float) x, gy, z0, (float) x, gy, z1, faint);
            }
            for (double z = Math.ceil(b.minZ); z < b.maxZ; z++) {
                line(buffer, pose, x0, gy, (float) z, x1, gy, (float) z, faint);
            }
        });
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz, int color) {
        float nx = bx - ax, ny = by - ay, nz = bz - az;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length <= 0) return;
        nx /= length;
        ny /= length;
        nz /= length;
        buffer.addVertex(pose, ax, ay, az).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(2.5F);
        buffer.addVertex(pose, bx, by, bz).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(2.5F);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        return be.displayedArea().minmax(new AABB(be.getBlockPos()));
    }
}
