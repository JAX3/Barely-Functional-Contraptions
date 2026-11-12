package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidTankBlock;
import java.util.function.ToIntFunction;
import net.minecraft.world.level.block.Block;
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

/**
 * Connected textures for multiblock casings (the Hume Energy Cube and the Fluid Tank), worked out from the world when
 * the chunk is built. Each kind of block is described by a {@link Spec}: which block joins up, the folder its face
 * models live in, and how many fill levels its side faces have (the cube's energy windows; 1 for the tank, whose
 * fluid is drawn by a block entity renderer).
 *
 * <p>For each visible face, an edge drops its frame (so the window or plate runs on into the next cube) only when the
 * cube beyond that edge shows the same face too, i.e. there is no cube in front of it. Where the structure folds
 * inward (a wall standing on a slab, a step), the neighbour's face is hidden, so the edge keeps its frame. Inside
 * corners, where both edges continue but the diagonal cube's face does not, get a small corner piece so the frame turns
 * the corner. These checks need blocks up to two steps away, which block states cannot see, so the whole cube is this
 * one dynamic model picking from pre-baked face models.</p>
 */
public final class EnergyCubeModel implements DynamicBlockStateModel {
    /** One kind of connected casing: the model type id, its face model folder, its block, and its side fill levels. */
    public record Spec(Identifier type, String folder, Class<? extends Block> block, int levels, ToIntFunction<BlockState> level) {}

    public static final Spec CUBE = new Spec(ResourcefulSheep.id("energy_cube"), "hume_energy_cube", EnergyCubeBlock.class,
            EnergyCubeBlock.MAX_CHARGE + 1, state -> state.getBlock() instanceof EnergyCubeBlock ? state.getValue(EnergyCubeBlock.CHARGE) : 0);
    public static final Spec TANK = new Spec(ResourcefulSheep.id("fluid_tank"), "fluid_tank", FluidTankBlock.class, 1, state -> 0);
    /** Kept for the cube's registration. */
    public static final Identifier TYPE = CUBE.type();
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

    private final Spec spec;
    private final int levels;

    private boolean isCube(BlockAndTintGetter level, BlockPos pos) {
        return spec.block().isInstance(level.getBlockState(pos).getBlock());
    }

    /** True when the block at {@code pos} shows its {@code face}: it is one of ours with none of ours in front of that face. */
    private boolean showsFace(BlockAndTintGetter level, BlockPos pos, Direction face) {
        return isCube(level, pos) && !isCube(level, pos.relative(face));
    }

    /** Edge mask (bit 3 up, 2 down, 1 left, 0 right; set = continues into the next block) and corner mask for a face. */
    int faceKey(BlockAndTintGetter level, BlockPos pos, Direction face) {
        Direction[] p = plane(face);
        int edges = 0;
        for (int i = 0; i < 4; i++) {
            if (showsFace(level, pos.relative(p[i]), face)) edges |= 8 >> i;
        }
        int corners = 0;
        for (int c = 0; c < 4; c++) {
            Direction a = c < 2 ? p[0] : p[1], b = c % 2 == 0 ? p[2] : p[3];
            int bitA = 8 >> (c < 2 ? 0 : 1), bitB = 8 >> (c % 2 == 0 ? 2 : 3);
            if ((edges & bitA) != 0 && (edges & bitB) != 0 && !showsFace(level, pos.relative(a).relative(b), face)) {
                corners |= 1 << c;
            }
        }
        return edges | corners << 4;
    }

    private final BlockStateModelPart[] sides;   // [face][edges][charge], horizontal faces only
    private final BlockStateModelPart[] ends;    // [up/down][edges]
    private final BlockStateModelPart[] corners; // [face][corner]

    private EnergyCubeModel(Spec spec, BlockStateModelPart[] sides, BlockStateModelPart[] ends, BlockStateModelPart[] corners) {
        this.spec = spec;
        this.levels = spec.levels();
        this.sides = sides;
        this.ends = ends;
        this.corners = corners;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) {
        int charge = spec.level().applyAsInt(state);
        for (Direction face : FACES) {
            if (isCube(level, pos.relative(face))) continue; // hidden inside the structure
            int key = faceKey(level, pos, face);
            int edges = key & 15;
            output.add(face.getAxis().isVertical()
                    ? ends[(face == Direction.UP ? 0 : 1) * 16 + edges]
                    : sides[(face.get2DDataValue() * 16 + edges) * levels + charge]);
            for (int c = 0; c < 4; c++) {
                if ((key >> 4 & 1 << c) != 0) output.add(corners[face.ordinal() * 4 + c]);
            }
        }
    }

    /** Lets chunk meshing reuse geometry: it depends only on the charge and each face's edge and corner masks. */
    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        long key = spec.level().applyAsInt(state);
        for (Direction face : FACES) {
            key = key << 9 | (isCube(level, pos.relative(face)) ? 256 : faceKey(level, pos, face));
        }
        return key;
    }

    @Override
    @Deprecated
    public Material.Baked particleMaterial() {
        return ends[0].particleMaterial();
    }

    @Override
    @Deprecated
    public int materialFlags() {
        return sides[0].materialFlags() | corners[0].materialFlags();
    }

    /**
     * The unbaked form, referenced from the blockstate as {@code "type": "barelyfunctionalcontraptions:energy_cube"}
     * (or {@code fluid_tank}).
     */
    public record Unbaked(Spec spec) implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked(CUBE));
        public static final MapCodec<Unbaked> TANK_CODEC = MapCodec.unit(new Unbaked(TANK));

        private static String mask(int edges) {
            return "" + (edges >> 3 & 1) + (edges >> 2 & 1) + (edges >> 1 & 1) + (edges & 1);
        }

        private Variant model(String name) {
            return new Variant(ResourcefulSheep.id("block/" + spec.folder() + "/" + name));
        }

        private Variant side(int index) {
            int levels = spec.levels();
            int charge = index % levels, edges = index / levels % 16;
            Direction face = Direction.from2DDataValue(index / levels / 16);
            return model(face.getSerializedName() + "_" + mask(edges) + "_" + charge);
        }

        private Variant end(int index) {
            return model((index < 16 ? "up" : "down") + "_" + mask(index % 16));
        }

        private Variant corner(int index) {
            return model("inner_" + FACES[index / 4].getSerializedName() + "_" + CORNERS[index % 4]);
        }

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return spec == TANK ? TANK_CODEC : MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (int i = 0; i < 4 * 16 * spec.levels(); i++) side(i).resolveDependencies(resolver);
            for (int i = 0; i < 32; i++) end(i).resolveDependencies(resolver);
            for (int i = 0; i < 24; i++) corner(i).resolveDependencies(resolver);
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            BlockStateModelPart[] sides = new BlockStateModelPart[4 * 16 * spec.levels()];
            for (int i = 0; i < sides.length; i++) sides[i] = side(i).bake(baker);
            BlockStateModelPart[] ends = new BlockStateModelPart[32];
            for (int i = 0; i < ends.length; i++) ends[i] = end(i).bake(baker);
            BlockStateModelPart[] corners = new BlockStateModelPart[24];
            for (int i = 0; i < corners.length; i++) corners[i] = corner(i).bake(baker);
            return new EnergyCubeModel(spec, sides, ends, corners);
        }
    }
}
