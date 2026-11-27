package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Sheep Attractor: a half-slab lure (a sheaf of wheat on a steel plate) that clips onto any face of a sheep
 * machine. While one is fitted, the machine calls its sheep back into its working area when they wander a little way
 * out (see {@link com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepAttraction}); without one, it doesn't.
 *
 * <p>{@link #FACING} points away from the block it's fitted to (the face that was clicked). A machine counts as
 * fitted when an attractor sits on any of its blocks: for a machine of several blocks (the Static Generator's two,
 * the Echo Chamber's 27) every connected block of the same kind counts, so it can go on any side.</p>
 */
public class SheepAttractorBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** Most connected machine blocks it follows when looking for an attractor (the Echo Chamber has 27). */
    private static final int MAX_PARTS = 32;

    private static final VoxelShape UP = Block.box(0, 0, 0, 16, 8, 16), DOWN = Block.box(0, 8, 0, 16, 16, 16),
            NORTH = Block.box(0, 0, 8, 16, 16, 16), SOUTH = Block.box(0, 0, 0, 16, 16, 8),
            WEST = Block.box(8, 0, 0, 16, 16, 16), EAST = Block.box(0, 0, 0, 8, 16, 16);

    public SheepAttractorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    /** The half of its block next to what it's fitted to. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** The block an attractor at {@code pos} is fitted to. */
    public static BlockPos fittedTo(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    /** Whether an attractor is fitted to any of the machine's blocks: the one at {@code machine} and those connected to it of the same kind. */
    public static boolean fitted(BlockGetter level, BlockPos machine) {
        Block kind = level.getBlockState(machine).getBlock();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(machine.immutable());
        seen.add(machine.immutable());
        while (!queue.isEmpty()) {
            BlockPos part = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = part.relative(dir);
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof SheepAttractorBlock && state.getValue(FACING) == dir) return true;
                if (state.is(kind) && seen.size() < MAX_PARTS && seen.add(next)) queue.add(next);
            }
        }
        return false;
    }

    /**
     * The sheep machine an attractor at {@code pos} is fitted to (any of its blocks, for a machine of several), or
     * null if it's fitted to something else.
     */
    public static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.@Nullable AreaDisplay machine(BlockGetter level, BlockPos pos) {
        BlockState attractor = level.getBlockState(pos);
        if (!(attractor.getBlock() instanceof SheepAttractorBlock)) return null;
        BlockPos start = fittedTo(attractor, pos);
        Block kind = level.getBlockState(start).getBlock();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            BlockPos part = queue.poll();
            if (level.getBlockEntity(part) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AreaDisplay display
                    && display.attractsSheep()) return display;
            for (Direction dir : Direction.values()) {
                BlockPos next = part.relative(dir);
                if (level.getBlockState(next).is(kind) && seen.size() < MAX_PARTS && seen.add(next)) queue.add(next);
            }
        }
        return null;
    }
}
