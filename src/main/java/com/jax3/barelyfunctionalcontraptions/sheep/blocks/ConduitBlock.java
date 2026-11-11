package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;

/**
 * Shared shape and connection logic for Energy Cables and Fluid Pipes ("conduits"). A conduit joins every conduit of
 * the same kind it touches, and plugs into any block that {@link #canPlugInto accepts it} on the side facing it. Arms
 * reach out to joined sides, with a plug plate on each plugged-in side.
 */
public abstract class ConduitBlock extends BaseEntityBlock {
    public static final Map<Direction, EnumProperty<CableConnection>> SIDES = new EnumMap<>(Direction.class);

    static {
        for (Direction dir : Direction.values()) {
            SIDES.put(dir, EnumProperty.create(dir.getSerializedName(), CableConnection.class));
        }
    }

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> PLUGS = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 5));
        ARMS.put(Direction.SOUTH, Block.box(6, 6, 11, 10, 10, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 6, 5, 10, 10));
        ARMS.put(Direction.EAST, Block.box(11, 6, 6, 16, 10, 10));
        ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 5, 10));
        ARMS.put(Direction.UP, Block.box(6, 11, 6, 10, 16, 10));
        PLUGS.put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 2));
        PLUGS.put(Direction.SOUTH, Block.box(4, 4, 14, 12, 12, 16));
        PLUGS.put(Direction.WEST, Block.box(0, 4, 4, 2, 12, 12));
        PLUGS.put(Direction.EAST, Block.box(14, 4, 4, 16, 12, 12));
        PLUGS.put(Direction.DOWN, Block.box(4, 0, 4, 12, 2, 12));
        PLUGS.put(Direction.UP, Block.box(4, 14, 4, 12, 16, 12));
    }

    private final Function<BlockState, VoxelShape> shapes;

    protected ConduitBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<CableConnection> side : SIDES.values()) state = state.setValue(side, CableConnection.NONE);
        registerDefaultState(state);
        shapes = getShapeForEachState(ConduitBlock::buildShape);
    }

    private static VoxelShape buildShape(BlockState state) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            CableConnection connection = state.getValue(SIDES.get(dir));
            if (connection.connected()) shape = Shapes.or(shape, ARMS.get(dir));
            if (connection == CableConnection.MACHINE) shape = Shapes.or(shape, PLUGS.get(dir));
        }
        return shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.apply(state);
    }

    /** Whether the block at {@code other} has the right kind of connection on {@code face} (the side facing the conduit). */
    protected abstract boolean canPlugInto(Level level, BlockPos other, Direction face);

    /** How a conduit at {@code pos} joins the block on side {@code dir}: the same kind of conduit, a block it plugs into, or nothing. */
    public CableConnection connectionTo(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        if (level.getBlockState(other).getBlock() == this) return CableConnection.CABLE;
        if (level instanceof Level real && real.isLoaded(other) && canPlugInto(real, other, dir.getOpposite())) {
            return CableConnection.MACHINE;
        }
        return CableConnection.NONE;
    }

    /** The conduit's state with every side worked out from the blocks around it. */
    public BlockState withConnections(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.values()) state = state.setValue(SIDES.get(dir), connectionTo(level, pos, dir));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (!(level instanceof Level)) {
            // World generation: only cables are known for certain.
            return neighborState.getBlock() == this ? state.setValue(SIDES.get(direction), CableConnection.CABLE) : state;
        }
        return state.setValue(SIDES.get(direction), connectionTo(level, pos, direction));
    }

    /** A neighbour that gains or loses its energy connection without changing block (rare) still updates the plugs. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level.isClientSide()) return;
        BlockState updated = withConnections(state, level, pos);
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_ALL);
    }

    /** Any change to a conduit (placed, or a side plugging in or out) makes its network rebuild. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.getBlockEntity(pos) instanceof Conduit conduit) conduit.invalidateNetwork();
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof Conduit neighbour) neighbour.invalidateNetwork();
        }
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    /** A conduit's block entity: forgets its network when the conduits around it change. */
    public interface Conduit {
        void invalidateNetwork();
    }
}
