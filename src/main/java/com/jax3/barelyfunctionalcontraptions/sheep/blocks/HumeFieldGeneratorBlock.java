package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Work in progress: the planned Hume Field Generator, so far only its model. A 2x2 machine, a block and a half tall: a
 * steel base and cap on four pillars, two glowing field coils round a glass shell, a core that flickers between "on"
 * and "off", and a lens on top that watches it. It does nothing yet; it's registered in development builds only.
 *
 * <p>It's four blocks, like a bed is two. {@link #PART} says which corner: 0 is the one placed (north-west, which draws
 * the whole model), 1 is east of it, 2 south and 3 south-east. Placing it fills the other three corners (it won't place
 * unless they're free), and breaking any corner removes them all.</p>
 */
public class HumeFieldGeneratorBlock extends Block {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

    /** The whole machine in pixels across its 2x2 area (0-32), 24 tall. */
    private static final VoxelShape WHOLE = Shapes.or(
            Block.box(0, 0, 0, 32, 4, 32),      // base
            Block.box(2, 4, 2, 30, 20, 30),     // pillars, coils and shell
            Block.box(3, 20, 3, 29, 23, 29),    // cap
            Block.box(12, 23, 12, 20, 24, 20)); // lens
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        VoxelShape cell = Shapes.box(0, 0, 0, 1, 1.5, 1);
        for (int part = 0; part < 4; part++) {
            SHAPES[part] = Shapes.join(WHOLE.move(-dx(part), 0, -dz(part)), cell, BooleanOp.AND);
        }
    }

    public HumeFieldGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 0));
    }

    static int dx(int part) {
        return part & 1;
    }

    static int dz(int part) {
        return part >> 1;
    }

    /** The corner placed first (part 0), which the others hang off. */
    private static BlockPos origin(BlockPos pos, int part) {
        return pos.offset(-dx(part), 0, -dz(part));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART)];
    }

    /** Places as corner 0 only when the three blocks east, south and south-east of it are free. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        for (int part = 1; part < 4; part++) {
            BlockPos other = pos.offset(dx(part), 0, dz(part));
            if (!level.getBlockState(other).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(other)) return null;
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        for (int part = 1; part < 4; part++) {
            level.setBlockAndUpdate(pos.offset(dx(part), 0, dz(part)), state.setValue(PART, part));
        }
    }

    /** A corner whose neighbouring corner has gone goes too, so breaking any corner removes the whole machine. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        int part = state.getValue(PART);
        BlockPos origin = origin(pos, part);
        for (int other = 0; other < 4; other++) {
            if (other == part || !neighbourPos.equals(origin.offset(dx(other), 0, dz(other)))) continue;
            if (!neighbourState.is(this) || neighbourState.getValue(PART) != other) return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos origin = origin(pos, state.getValue(PART));
            for (int part = 0; part < 4; part++) {
                BlockPos other = origin.offset(dx(part), 0, dz(part));
                if (!other.equals(pos) && level.getBlockState(other).is(this)) level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
