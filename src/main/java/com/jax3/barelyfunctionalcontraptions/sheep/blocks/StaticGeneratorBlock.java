package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Static Generator: a mid-game generator two blocks tall. A blue-steel base with a charge gauge on its front, and a
 * rubber belt running up a column into a polished copper dome, like a Van de Graaff generator. Sheep rubbing against
 * the Copper Combs in its pen build up static, which the dome collects and arcs into power (see
 * {@link StaticGeneratorBlockEntity}).
 *
 * <ul>
 *     <li>right-click either half to open its window;</li>
 *     <li>sneak-right-click with an empty hand to show or hide its pen.</li>
 * </ul>
 * The lower half holds the block entity and drops the machine; breaking either half removes both.
 */
public class StaticGeneratorBlock extends BaseEntityBlock {
    /**
     * Minecraft 26.1 and 26.2 ask every block entity block for a codec; 26.3 doesn't. Not an override, so it builds
     * for all of them.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape DOME = Shapes.or(Block.box(5, 0, 5, 11, 6, 11), Block.box(2, 6, 2, 14, 15, 14));

    public StaticGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? DOME : Shapes.block();
    }

    /** The lower half, which holds the block entity, from either half. */
    public static BlockPos base(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    public static @Nullable StaticGeneratorBlockEntity entity(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof StaticGeneratorBlock && level.getBlockEntity(base(pos, state)) instanceof StaticGeneratorBlockEntity be ? be : null;
    }

    /** Facing the player, if there's room for the dome above. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        Level level = context.getLevel();
        if (above.getY() >= level.getMaxY() || !level.getBlockState(above).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, PlacementFacing.horizontal(context));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** Either half goes when the other does. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (direction == (lower ? Direction.UP : Direction.DOWN)) {
            boolean partner = neighbourState.is(this) && neighbourState.getValue(HALF) != state.getValue(HALF);
            if (!partner) return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /**
     * Only the lower half drops the machine (its loot table), so a player breaking the dome breaks the base too, with
     * their tool: a pickaxe gets the drop, a bare hand or creative doesn't.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (!player.getAbilities().instabuild && player.hasCorrectToolForDrops(lower)) {
                    dropResources(lower, level, below, level.getBlockEntity(below), player, player.getMainHandItem());
                }
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new StaticGeneratorBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel) || state.getValue(HALF) != DoubleBlockHalf.LOWER) return null;
        return createTickerHelper(type, ModBlockEntities.STATIC_GENERATOR.get(),
                (l, pos, s, be) -> StaticGeneratorBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        StaticGeneratorBlockEntity be = entity(level, pos, state);
        if (be == null) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) be.toggleArea(player);
        else player.openMenu(be, be.getBlockPos());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How charged its dome is: 0 empty, 15 full. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        StaticGeneratorBlockEntity be = entity(level, pos, state);
        return be == null ? 0 : be.comparatorSignal();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** The dome crackles while it charges, more as its charge builds. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER || !state.getValue(ACTIVE)) return;
        StaticGeneratorBlockEntity be = entity(level, pos, state);
        int sparks = 1 + (be == null ? 0 : be.comparatorSignal() / 4);
        for (int i = 0; i < sparks; i++) {
            if (random.nextInt(2) != 0) continue;
            double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + Math.cos(a) * Math.sin(b) * 0.42,
                    pos.getY() + 0.65 + Math.cos(b) * 0.3, pos.getZ() + 0.5 + Math.sin(a) * Math.sin(b) * 0.42, 0, 0, 0);
        }
    }
}
