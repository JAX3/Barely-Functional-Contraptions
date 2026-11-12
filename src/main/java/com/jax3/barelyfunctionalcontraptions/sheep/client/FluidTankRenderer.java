package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidTankBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Draws the fluid inside a Fluid Tank, behind its glass. Each tank draws its own share as a box filling it from the
 * bottom; the group keeps its layers filled from the bottom up, so together the boxes show one level running across
 * the whole group. Faces towards a neighbouring tank are left out (the fluid carries on into it), and the outer faces
 * sit just inside the casing.
 */
public final class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.State> {
    /** How far the fluid sits inside the casing's outer faces. */
    private static final float INSET = 0.02F;

    public static final class State extends BlockEntityRenderState {
        @Nullable TextureAtlasSprite sprite;
        int color;
        int light;
        float x0, y0, z0, x1, y1, z1;
        /** Which faces to draw, indexed by {@link Direction#ordinal()}. */
        final boolean[] faces = new boolean[6];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidTankBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.sprite = null;
        FluidStack stack = be.getFluid();
        float fill = be.fillFraction();
        Level level = be.getLevel();
        if (stack.isEmpty() || fill <= 0 || level == null) return;
        BlockPos pos = be.getBlockPos();
        boolean[] joined = new boolean[6];
        for (Direction dir : Direction.values()) {
            joined[dir.ordinal()] = level.getBlockState(pos.relative(dir)).getBlock() instanceof FluidTankBlock;
        }
        state.sprite = FluidSprites.still(stack.getFluid());
        state.color = FluidSprites.tint(stack);
        int glow = FluidSprites.lightLevel(stack);
        state.light = glow > 0 ? LightCoordsUtil.withBlock(state.lightCoords, Math.max(glow, LightCoordsUtil.block(state.lightCoords))) : state.lightCoords;
        state.x0 = joined[Direction.WEST.ordinal()] ? 0 : INSET;
        state.x1 = joined[Direction.EAST.ordinal()] ? 1 : 1 - INSET;
        state.z0 = joined[Direction.NORTH.ordinal()] ? 0 : INSET;
        state.z1 = joined[Direction.SOUTH.ordinal()] ? 1 : 1 - INSET;
        state.y0 = joined[Direction.DOWN.ordinal()] ? 0 : INSET;
        float top = joined[Direction.UP.ordinal()] ? 1 : 1 - INSET;
        state.y1 = state.y0 + (top - state.y0) * fill;
        // The top surface shows unless the fluid carries on into a tank above that has some.
        boolean continuesUp = fill >= 1 && joined[Direction.UP.ordinal()]
                && level.getBlockEntity(pos.above()) instanceof FluidTankBlockEntity above && above.fillFraction() > 0;
        for (Direction dir : Direction.values()) {
            state.faces[dir.ordinal()] = dir == Direction.UP ? !continuesUp : !joined[dir.ordinal()];
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        TextureAtlasSprite sprite = state.sprite;
        if (sprite == null) return;
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockItemSheet(), (pose, buffer) -> {
            for (Direction dir : Direction.values()) {
                if (state.faces[dir.ordinal()]) face(pose, buffer, state, sprite, dir);
            }
        });
    }

    /** One face of the fluid box, its texture tiled by block position so neighbouring tanks line up. */
    private static void face(PoseStack.Pose pose, VertexConsumer buffer, State s, TextureAtlasSprite sprite, Direction dir) {
        float[][] corners = switch (dir) {
            case UP -> new float[][]{{s.x0, s.y1, s.z0}, {s.x0, s.y1, s.z1}, {s.x1, s.y1, s.z1}, {s.x1, s.y1, s.z0}};
            case DOWN -> new float[][]{{s.x0, s.y0, s.z0}, {s.x1, s.y0, s.z0}, {s.x1, s.y0, s.z1}, {s.x0, s.y0, s.z1}};
            case NORTH -> new float[][]{{s.x1, s.y0, s.z0}, {s.x0, s.y0, s.z0}, {s.x0, s.y1, s.z0}, {s.x1, s.y1, s.z0}};
            case SOUTH -> new float[][]{{s.x0, s.y0, s.z1}, {s.x1, s.y0, s.z1}, {s.x1, s.y1, s.z1}, {s.x0, s.y1, s.z1}};
            case WEST -> new float[][]{{s.x0, s.y0, s.z0}, {s.x0, s.y0, s.z1}, {s.x0, s.y1, s.z1}, {s.x0, s.y1, s.z0}};
            case EAST -> new float[][]{{s.x1, s.y0, s.z1}, {s.x1, s.y0, s.z0}, {s.x1, s.y1, s.z0}, {s.x1, s.y1, s.z1}};
        };
        // Drawn from both sides: through the glass you see the near side of the fluid and the far side behind it, as
        // you would looking into a glass of water, which also makes see-through fluids like water read clearly.
        for (int side = 0; side < 2; side++) {
            for (int i = 0; i < 4; i++) {
                float[] c = corners[side == 0 ? i : 3 - i];
            // Texture coordinates from the two in-plane axes, so each block shows one whole tile.
            float u, v;
            switch (dir.getAxis()) {
                case Y -> { u = c[0]; v = c[2]; }
                case X -> { u = c[2]; v = 1 - c[1]; }
                default -> { u = c[0]; v = 1 - c[1]; }
            }
            buffer.addVertex(pose, c[0], c[1], c[2])
                    .setColor(s.color)
                    .setUv(sprite.getU(u), sprite.getV(v))
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(s.light)
                    .setNormal(pose, side == 0 ? dir.getStepX() : -dir.getStepX(), side == 0 ? dir.getStepY() : -dir.getStepY(),
                            side == 0 ? dir.getStepZ() : -dir.getStepZ());
            }
        }
    }
}
