package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Echo Chamber, a late-game generator that turns the vibrations its Warden sheep hear into power (see
 * {@link EchoChamberBlockEntity}). A 3x3x3 machine in the blue-steel casing, lined
 * with sculk: a big resonator dish on the front whose rings pulse outward from a glowing heart, egg-crate
 * sound-dampening foam on the side walls with sculk creeping over it, a power port between louvred vents
 * at the back, and a sculk-covered roof with a catalyst dome in a crown of sculk fronds. The biggest two, the Warden
 * sheep's "ears", now and then give off a pop of sculk charge.
 *
 * <ul>
 *     <li>right-click any block of it to open its window;</li>
 *     <li>sneak-right-click with an empty hand to show or hide its listening pen.</li>
 * </ul>
 *
 * <p>It's 27 blocks, as a bed is two. {@link #FACING} is the way its front faces; {@link #PART} says which column
 * (0-8: {@code across + 3 * depth}, across 0-2 from its left to its right as you face it, depth 0-2 from the front back)
 * and {@link #LAYER} how high (0-2). The block placed is the front middle one at the bottom (part 1, layer 0); the rest
 * fill the space behind and above it, and it won't place unless that space is free. Breaking any of them removes them
 * all. The middle column draws the whole model (see tools/sheep/generate_echo_chamber_model.py); the rest draw
 * nothing. The middle block of the bottom layer holds the block entity.</p>
 */
public class EchoChamberBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, 2);
    public static final int SIZE = 3;
    /** The block that's placed: the front middle one, at the bottom. */
    public static final int FRONT_MIDDLE = 1;
    public static final int MIDDLE = 4;

    /** The roof is 12 pixels into the top layer; the dome in the middle reaches its top. */
    private static final VoxelShape ROOF = Block.box(0, 0, 0, 16, 12, 16);

    public EchoChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, FRONT_MIDDLE).setValue(LAYER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, LAYER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(LAYER) == SIZE - 1 && state.getValue(PART) != MIDDLE ? ROOF : Shapes.block();
    }

    /** Where block {@code part}, {@code layer} of a machine whose front middle bottom block is at {@code origin} goes. */
    public static BlockPos at(BlockPos origin, Direction facing, int part, int layer) {
        Direction right = facing.getCounterClockWise(); // the machine's right, as you face its front
        return origin.relative(right, part % SIZE - 1).relative(facing.getOpposite(), part / SIZE).above(layer);
    }

    /** The middle block of the bottom layer, which holds the block entity. */
    public static BlockPos core(BlockPos pos, BlockState state) {
        return at(origin(pos, state), state.getValue(FACING), MIDDLE, 0);
    }

    /** The machine's block entity, from any of its blocks. */
    public static @Nullable EchoChamberBlockEntity entity(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof EchoChamberBlock && level.getBlockEntity(core(pos, state)) instanceof EchoChamberBlockEntity be ? be : null;
    }

    /** Whether {@code state} is the block that holds the block entity. */
    public static boolean isCore(BlockState state) {
        return state.getValue(PART) == MIDDLE && state.getValue(LAYER) == 0;
    }

    /** Whether {@code state} is its power port, the middle of its back on the bottom layer. */
    public static boolean isPort(BlockState state) {
        return state.getValue(PART) == MIDDLE + SIZE && state.getValue(LAYER) == 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isCore(state) ? new EchoChamberBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel) || !isCore(state) || type != ModBlockEntities.ECHO_CHAMBER.get()) return null;
        return (l, pos, s, be) -> EchoChamberBlockEntity.serverTick(serverLevel, pos, s, (EchoChamberBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        EchoChamberBlockEntity be = entity(level, pos, state);
        if (be == null) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) be.toggleArea(player);
        else player.openMenu(be, be.getBlockPos());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** The noise level it hears, 0-15, from any block of it. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        EchoChamberBlockEntity be = entity(level, pos, state);
        return be == null ? 0 : be.comparatorSignal();
    }

    /** The front middle bottom block, which the others are laid out from. */
    private static BlockPos origin(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int part = state.getValue(PART);
        Direction right = facing.getCounterClockWise();
        return pos.relative(right, 1 - part % SIZE).relative(facing, part / SIZE).below(state.getValue(LAYER));
    }

    /** Places it facing the player, but only when the 3x3x3 space behind and above the clicked spot is free. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = PlacementFacing.horizontal(context);
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        for (int part = 0; part < SIZE * SIZE; part++) {
            for (int layer = 0; layer < SIZE; layer++) {
                if (part == FRONT_MIDDLE && layer == 0) continue;
                BlockPos other = at(pos, facing, part, layer);
                if (!level.getBlockState(other).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(other)
                        || other.getY() >= level.getMaxY()) return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        fill(level, pos, state);
    }

    /** Puts every block of the machine whose front middle bottom block is {@code origin} (the dev harness uses this too). */
    public static void fill(Level level, BlockPos origin, BlockState state) {
        Direction facing = state.getValue(FACING);
        for (int part = 0; part < SIZE * SIZE; part++) {
            for (int layer = 0; layer < SIZE; layer++) {
                level.setBlock(at(origin, facing, part, layer), state.setValue(PART, part).setValue(LAYER, layer), 3);
            }
        }
    }

    /** A block whose neighbouring block of the machine has gone goes too, so breaking any of them removes the lot. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockPos origin = origin(pos, state);
        Direction facing = state.getValue(FACING);
        for (int part = 0; part < SIZE * SIZE; part++) {
            for (int layer = 0; layer < SIZE; layer++) {
                if (!neighbourPos.equals(at(origin, facing, part, layer))) continue;
                if (!neighbourState.is(this) || neighbourState.getValue(FACING) != facing || neighbourState.getValue(PART) != part
                        || neighbourState.getValue(LAYER) != layer) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /**
     * Breaking any block of it removes the rest. Only the middle bottom block drops the machine (its loot table), so a
     * player breaking another block of it breaks that one too, with their tool: a pickaxe gets the drop, a bare hand or
     * creative doesn't, as for any machine.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos origin = origin(pos, state);
            Direction facing = state.getValue(FACING);
            BlockPos core = at(origin, facing, MIDDLE, 0);
            // The middle block first, quietly (no drops, no knock-on updates), so the others going can't drop it too.
            BlockState coreState = level.getBlockState(core);
            if (!core.equals(pos) && coreState.is(this)) {
                boolean drops = !player.getAbilities().instabuild && player.hasCorrectToolForDrops(coreState);
                if (drops) dropResources(coreState, level, core, level.getBlockEntity(core), player, player.getMainHandItem());
                level.setBlock(core, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
            for (int part = 0; part < SIZE * SIZE; part++) {
                for (int layer = 0; layer < SIZE; layer++) {
                    BlockPos other = at(origin, facing, part, layer);
                    if (!other.equals(pos) && level.getBlockState(other).is(this)) level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
                }
            }
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

    /** Pops of sculk charge off the tips of the ears, from the top middle block, which draws them. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != MIDDLE || state.getValue(LAYER) != SIZE - 1 || random.nextInt(3) != 0) return;
        // Quiet while its Warden sheep sleep, or while it recovers from a shriek.
        EchoChamberBlockEntity be = entity(level, pos, state);
        if (be != null && (be.state() == EchoChamberBlockEntity.State.ASLEEP || be.state() == EchoChamberBlockEntity.State.SHRIEKED)) return;
        // The ears stand 15 pixels either side of the middle and lean outwards; their tips are about 1.1 blocks up.
        Direction right = state.getValue(FACING).getCounterClockWise();
        double side = (random.nextBoolean() ? 1 : -1) * 1.35;
        level.addParticle(ParticleTypes.SCULK_CHARGE_POP,
                pos.getX() + 0.5 + right.getStepX() * side + (random.nextDouble() - 0.5) * 0.3,
                pos.getY() + 1.6 + random.nextDouble() * 0.4,
                pos.getZ() + 0.5 + right.getStepZ() * side + (random.nextDouble() - 0.5) * 0.3,
                0, 0.02, 0);
    }
}
