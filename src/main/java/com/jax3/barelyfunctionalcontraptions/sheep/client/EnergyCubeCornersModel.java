package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.jspecify.annotations.Nullable;

/**
 * The inside corners of connected Hume Energy Cubes. A face whose two adjacent edges both connect to other cubes, but
 * whose diagonal block between them is not a cube (the inside of an L), gets a small inside-corner frame piece so the
 * frame strips of the two arms turn the corner instead of stopping short. Block states only know the six touching
 * neighbours, so this part of the multipart blockstate looks the diagonal up in the world when the chunk is built.
 */
public final class EnergyCubeCornersModel implements DynamicBlockStateModel {
    public static final Identifier TYPE = ResourcefulSheep.id("energy_cube_corners");
    private static final Direction[] FACES = Direction.values();
    private static final String[] CORNERS = {"tl", "tr", "bl", "br"};

    /** For each face: the block sides that are its texture's up, down, left and right, seen from outside the face. */
    private static Direction[] plane(Direction face) {
        return switch (face) {
            case NORTH -> new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
            case SOUTH -> new Direction[]{Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};
            case EAST -> new Direction[]{Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH};
            case WEST -> new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
            case UP -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            case DOWN -> new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST};
        };
    }

    /** The two plane directions meeting at a corner: tl = up+left, tr = up+right, bl = down+left, br = down+right. */
    private static Direction[] cornerSides(Direction face, int corner) {
        Direction[] p = plane(face);
        return new Direction[]{corner < 2 ? p[0] : p[1], corner % 2 == 0 ? p[2] : p[3]};
    }

    private final BlockStateModelPart[] parts; // [face * 4 + corner]

    private EnergyCubeCornersModel(BlockStateModelPart[] parts) {
        this.parts = parts;
    }

    /** Bit {@code face * 4 + corner} is set when that inside corner is drawn. */
    private static int cornerMask(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof EnergyCubeBlock)) return 0;
        int mask = 0;
        for (Direction face : FACES) {
            for (int corner = 0; corner < 4; corner++) {
                Direction[] sides = cornerSides(face, corner);
                if (state.getValue(EnergyCubeBlock.CONNECTED.get(sides[0])) && state.getValue(EnergyCubeBlock.CONNECTED.get(sides[1]))
                        && !(level.getBlockState(pos.relative(sides[0]).relative(sides[1])).getBlock() instanceof EnergyCubeBlock)) {
                    mask |= 1 << (face.ordinal() * 4 + corner);
                }
            }
        }
        return mask;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
        int mask = cornerMask(level, pos, state);
        for (int i = 0; mask != 0 && i < parts.length; i++) {
            if ((mask & (1 << i)) != 0) output.add(parts[i]);
        }
    }

    /** Lets the multipart model cache the geometry: it only depends on which corners are drawn. */
    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return cornerMask(level, pos, state);
    }

    @Override
    @Deprecated
    public Material.Baked particleMaterial() {
        return parts[0].particleMaterial();
    }

    @Override
    @Deprecated
    public int materialFlags() {
        return parts[0].materialFlags();
    }

    /** The unbaked form, referenced from the blockstate as {@code "type": "barelyfunctionalcontraptions:energy_cube_corners"}. */
    public record Unbaked() implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        private static Variant variant(int index) {
            return new Variant(ResourcefulSheep.id("block/hume_energy_cube/inner_"
                    + FACES[index / 4].getSerializedName() + "_" + CORNERS[index % 4]));
        }

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (int i = 0; i < FACES.length * 4; i++) variant(i).resolveDependencies(resolver);
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            BlockStateModelPart[] parts = new BlockStateModelPart[FACES.length * 4];
            for (int i = 0; i < parts.length; i++) parts[i] = variant(i).bake(baker);
            return new EnergyCubeCornersModel(parts);
        }
    }
}
