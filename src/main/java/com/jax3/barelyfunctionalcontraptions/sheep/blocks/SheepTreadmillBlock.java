package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Sheep Treadmill: a belt between two rollers in the machines' blue-steel casing, one block wide and two long, with
 * a gantry at the front where a bundle of wheat hangs just out of a sheep's reach, and a generator housing at the back
 * with a copper flywheel. A sheep on the belt walks towards the wheat and makes power; see
 * {@link SheepTreadmillBlockEntity}.
 *
 * <p>It's two blocks, as a bed is: {@link #PART} 0 is the back, where it was placed (it draws the whole model and holds
 * the block entity), and 1 the front, the next block in the direction it {@link #FACING faces} (the way the player was
 * looking). Breaking either breaks both and drops the treadmill. {@link #RUNNING} is set on the back while a sheep
 * walks it: the belt moves and the sheep's legs go.</p>
 */
public class SheepTreadmillBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    /** Facing north: the back (belt, rails and the generator housing) and the front (belt and the wheat gantry). */
    private static final Map<Direction, VoxelShape> BACK = Shapes.rotateHorizontal(Shapes.or(
            Block.box(0, 0, 0, 16, 6, 16), Block.box(0, 0, 10, 16, 12, 16)));
    private static final Map<Direction, VoxelShape> FRONT = Shapes.rotateHorizontal(Shapes.or(
            Block.box(0, 0, 0, 16, 6, 16), Block.box(0, 6, 0, 2, 24, 2), Block.box(14, 6, 0, 16, 24, 2), Block.box(0, 22, 0, 16, 24, 2)));

    public SheepTreadmillBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, RUNNING);
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

    /** The back block, which holds the block entity. */
    public static BlockPos back(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? pos : pos.relative(state.getValue(FACING).getOpposite());
    }

    /** The treadmill's block entity, from either of its blocks. */
    public static @Nullable SheepTreadmillBlockEntity entity(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(back(pos, state)) instanceof SheepTreadmillBlockEntity be ? be : null;
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
        level.setBlockAndUpdate(pos.relative(state.getValue(FACING)), state.setValue(PART, 1).setValue(RUNNING, false));
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

    /**
     * Breaking the back drops the treadmill (its loot table) and takes the front with it; breaking the front breaks the
     * back as though it had been broken, so the treadmill and its wheat drop either way.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos other = other(pos, state);
            if (level.getBlockState(other).is(this)) {
                if (state.getValue(PART) == 1) level.destroyBlock(other, !player.getAbilities().instabuild, player);
                else level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
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

    // ------------------------------------------------------------------ block entity

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new SheepTreadmillBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel) || state.getValue(PART) != 0) return null;
        return createTickerHelper(type, ModBlockEntities.SHEEP_TREADMILL.get(),
                (l, pos, s, be) -> SheepTreadmillBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** Right-click with Wheat or a Hay Bale: as much as fits goes in. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (SheepTreadmillBlockEntity.feedValue(stack) <= 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
        SheepTreadmillBlockEntity be = entity(level, pos, state);
        if (!level.isClientSide() && be != null) {
            ItemStack slot = be.food();
            int room = slot.isEmpty() ? stack.getMaxStackSize()
                    : ItemStack.isSameItemSameComponents(slot, stack) ? slot.getMaxStackSize() - slot.getCount() : 0;
            int move = Math.min(room, stack.getCount());
            if (move <= 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (slot.isEmpty()) be.setItem(0, stack.copyWithCount(move));
            else slot.grow(move);
            if (!player.getAbilities().instabuild) stack.shrink(move);
            be.setChanged();
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        SheepTreadmillBlockEntity be = entity(level, pos, state);
        if (!level.isClientSide() && be != null) player.openMenu(be, be.getBlockPos());
        return InteractionResult.SUCCESS;
    }

    /** The belt's creak while it runs. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != 0 || !state.getValue(RUNNING) || random.nextInt(30) != 0) return;
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS,
                0.15F, 0.6F + random.nextFloat() * 0.2F, false);
    }

    /** A comparator reads how much food is left, from either block. */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(entity(level, pos, state));
    }
}
