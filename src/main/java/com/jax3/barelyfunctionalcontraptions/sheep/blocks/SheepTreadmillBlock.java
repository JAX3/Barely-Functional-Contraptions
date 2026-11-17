package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Work in progress: the planned Sheep Treadmill, so far only its model. A wooden belt between two rollers, one block
 * wide and two long, with a gantry at the front where a bundle of wheat hangs just out of a sheep's reach, and a
 * generator housing at the back with a copper flywheel. It does nothing yet; it's registered in development builds only.
 *
 * <p>It's two blocks, as a bed is: {@link #PART} 0 is the back, where it was placed (it draws the whole model), and 1
 * the front, the next block in the direction it {@link #FACING faces} (the way the player was looking). Breaking
 * either removes both.</p>
 */
public class SheepTreadmillBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);

    /** Facing north: the back (belt, rails and the generator housing) and the front (belt and the wheat gantry). */
    private static final Map<Direction, VoxelShape> BACK = Shapes.rotateHorizontal(Shapes.or(
            Block.box(0, 0, 0, 16, 6, 16), Block.box(0, 0, 10, 16, 12, 16)));
    private static final Map<Direction, VoxelShape> FRONT = Shapes.rotateHorizontal(Shapes.or(
            Block.box(0, 0, 0, 16, 6, 16), Block.box(0, 6, 0, 2, 24, 2), Block.box(14, 6, 0, 16, 24, 2), Block.box(0, 22, 0, 16, 24, 2)));

    public SheepTreadmillBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PART) == 0 ? BACK : FRONT).get(state.getValue(FACING));
    }

    /** The other block of the treadmill. */
    private static BlockPos other(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == 0 ? pos.relative(facing) : pos.relative(facing.getOpposite());
    }

    /** Places as the back, facing the way the player looks, when the block in front is free. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos front = context.getClickedPos().relative(facing);
        Level level = context.getLevel();
        if (!level.getBlockState(front).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(front)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        level.setBlockAndUpdate(pos.relative(state.getValue(FACING)), state.setValue(PART, 1));
    }

    /** When its other half goes, this half goes too. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (neighbourPos.equals(other(pos, state)) && (!neighbourState.is(this)
                || neighbourState.getValue(PART) == state.getValue(PART) || neighbourState.getValue(FACING) != state.getValue(FACING))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos other = other(pos, state);
            if (level.getBlockState(other).is(this)) level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
