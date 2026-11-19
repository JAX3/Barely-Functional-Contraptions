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
        /** The areas relative to the block's own corner, and their colours. */
        java.util.List<AABB> boxes = java.util.List.of();
        int[] colors = new int[0];
        /** The attraction areas round them, drawn faintly, or none. */
        java.util.List<AABB> reaches = java.util.List.of();
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
        java.util.List<AABB> areas = be.displayedAreas();
        state.boxes = areas.stream().map(a -> a.move(-pos.getX(), -pos.getY(), -pos.getZ()).deflate(INSET)).toList();
        state.colors = new int[areas.size()];
        for (int i = 0; i < areas.size(); i++) state.colors[i] = be.areaColor(i);
        state.reaches = be.attractsSheep() && com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepAttraction.enabled()
                ? areas.stream().map(a -> com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepAttraction.reach(a)
                        .move(-pos.getX(), -pos.getY(), -pos.getZ()).deflate(INSET)).toList()
                : java.util.List.of();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.show) return;
        for (int i = 0; i < state.boxes.size(); i++) drawBox(state.boxes.get(i), state.colors[i], poseStack, collector);
        for (int i = 0; i < state.reaches.size(); i++) drawReach(state.reaches.get(i), state.colors[i], poseStack, collector);
    }

    /** The attraction area: a faint box with dashed edges and no floor grid, so it reads as "nearby", not "inside". */
    private static void drawReach(AABB b, int color, PoseStack poseStack, SubmitNodeCollector collector) {
        int faint = (color & 0x00FFFFFF) | 0x60000000;
        collector.submitCustomGeometry(poseStack, RenderTypes.LINES, (pose, buffer) -> {
            float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
            float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
            float[][] edges = {
                    {x0, y0, z0, x1, y0, z0}, {x0, y1, z0, x1, y1, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z1, x1, y1, z1},
                    {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                    {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
            for (float[] e : edges) {
                float length = (float) Math.sqrt((e[3] - e[0]) * (e[3] - e[0]) + (e[4] - e[1]) * (e[4] - e[1]) + (e[5] - e[2]) * (e[5] - e[2]));
                int dashes = Math.max(1, Math.round(length * 2)); // half-block dashes with gaps
                for (int d = 0; d < dashes; d += 2) {
                    float t0 = d / (float) dashes, t1 = Math.min(1F, (d + 1) / (float) dashes);
                    line(buffer, pose, e[0] + (e[3] - e[0]) * t0, e[1] + (e[4] - e[1]) * t0, e[2] + (e[5] - e[2]) * t0,
                            e[0] + (e[3] - e[0]) * t1, e[1] + (e[4] - e[1]) * t1, e[2] + (e[5] - e[2]) * t1, faint);
                }
            }
        });
    }

    private static void drawBox(AABB b, int color, PoseStack poseStack, SubmitNodeCollector collector) {
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
        AABB box = new AABB(be.getBlockPos());
        for (AABB area : be.displayedAreas()) {
            box = box.minmax(be.attractsSheep() ? com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepAttraction.reach(area) : area);
        }
        return box;
    }
}
