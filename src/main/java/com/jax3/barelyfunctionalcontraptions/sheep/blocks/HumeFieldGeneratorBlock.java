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
 * Work in progress: the planned Hume Field Generator, so far only its model. A 2x2 machine two and a half blocks tall:
 * a steel base and cap on four pillars, three glowing field coils round a glass shell, a core that flickers between
 * "on" and "off", and a lens on top that watches it. It does nothing yet; it's registered in development builds only.
 *
 * <p>It's twelve blocks, as a bed is two: {@link #PART} says which corner (0 is the north-west one that was placed, 1
 * east of it, 2 south, 3 south-east) and {@link #LAYER} how high (0-2). Corner 0 of layer 0 draws the lower two blocks
 * of the model and corner 0 of layer 2 the cap, since a block model can't be more than two blocks tall. Placing it
 * fills the other eleven (it won't place unless they're free), and breaking any of them removes them all.</p>
 */
public class HumeFieldGeneratorBlock extends Block {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, 2);
    public static final int LAYERS = 3;

    /** The whole machine in pixels across its 2x2 area (0-32), 40 tall. */
    private static final VoxelShape WHOLE = Shapes.or(
            Block.box(0, 0, 0, 32, 4, 32),      // base
            Block.box(2, 4, 2, 30, 34, 30),     // pillars, coils and shell
            Block.box(3, 34, 3, 29, 38, 29),    // cap
            Block.box(12, 38, 12, 20, 40, 20)); // lens
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][LAYERS];

    static {
        VoxelShape cell = Shapes.block();
        for (int part = 0; part < 4; part++) {
            for (int layer = 0; layer < LAYERS; layer++) {
                SHAPES[part][layer] = Shapes.join(WHOLE.move(-dx(part), -layer, -dz(part)), cell, BooleanOp.AND);
            }
        }
    }

    public HumeFieldGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(LAYER, 0));
    }

    static int dx(int part) {
        return part & 1;
    }

    static int dz(int part) {
        return part >> 1;
    }

    /** The bottom corner placed first (part 0, layer 0), which the others hang off. */
    private static BlockPos origin(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        return pos.offset(-dx(part), -state.getValue(LAYER), -dz(part));
    }

    private static BlockPos at(BlockPos origin, int part, int layer) {
        return origin.offset(dx(part), layer, dz(part));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, LAYER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART)][state.getValue(LAYER)];
    }

    /** Places as the bottom north-west corner only when the 2x2x3 space east, south and above it is free. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        for (int part = 0; part < 4; part++) {
            for (int layer = 0; layer < LAYERS; layer++) {
                if (part == 0 && layer == 0) continue;
                BlockPos other = at(pos, part, layer);
                if (!level.getBlockState(other).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(other)
                        || other.getY() >= level.getMaxY()) return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        for (int part = 0; part < 4; part++) {
            for (int layer = 0; layer < LAYERS; layer++) {
                if (part != 0 || layer != 0) level.setBlockAndUpdate(at(pos, part, layer), state.setValue(PART, part).setValue(LAYER, layer));
            }
        }
    }

    /** A block whose neighbouring block of the machine has gone goes too, so breaking any of them removes the lot. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockPos origin = origin(pos, state);
        for (int part = 0; part < 4; part++) {
            for (int layer = 0; layer < LAYERS; layer++) {
                if (!neighbourPos.equals(at(origin, part, layer))) continue;
                if (!neighbourState.is(this) || neighbourState.getValue(PART) != part || neighbourState.getValue(LAYER) != layer) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos origin = origin(pos, state);
            for (int part = 0; part < 4; part++) {
                for (int layer = 0; layer < LAYERS; layer++) {
                    BlockPos other = at(origin, part, layer);
                    if (!other.equals(pos) && level.getBlockState(other).is(this)) level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
