package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Machine Status Panel block: a thin screen that mounts on a wall, showing how the machines linked to it are doing
 * ({@link StatusPanelBlockEntity}). Link machines with the Barely Functional Wrench: click the panel with it, then each
 * machine (again to unlink), then the panel again to finish.
 * <p>
 * Panels facing the same way that fill a rectangle up to {@code maxWidth} x {@code maxHeight} (3 x 3) join into one
 * screen: the bezels between them go ({@link #JOINED_UP} and the rest say which sides join), its top-left panel (as you
 * look at it) holds the links for all of them, and it shows 4 machines for each panel. Anything else (an L, a gap, too
 * big) stays separate panels.
 */
public class StatusPanelBlock extends BaseEntityBlock {
    /**
     * Minecraft 26.1 and 26.2 ask every block entity block for a codec; 26.3 doesn't. Not an override, so it builds
     * for all of them.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    /** The way its screen faces (it hangs on the block behind it). */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Which sides (as you look at its screen) join to the next panel of the same screen. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty JOINED_UP =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("joined_up");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty JOINED_DOWN =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("joined_down");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty JOINED_LEFT =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("joined_left");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty JOINED_RIGHT =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("joined_right");

    private static final VoxelShape NORTH = Block.box(0, 0, 14, 16, 16, 16), SOUTH = Block.box(0, 0, 0, 16, 16, 2),
            EAST = Block.box(0, 0, 0, 2, 16, 16), WEST = Block.box(14, 0, 0, 16, 16, 16);

    public StatusPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(JOINED_UP, false).setValue(JOINED_DOWN, false)
                .setValue(JOINED_LEFT, false).setValue(JOINED_RIGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, JOINED_UP, JOINED_DOWN, JOINED_LEFT, JOINED_RIGHT);
    }

    /** The way that's left, as you look at a screen facing {@code facing}. */
    public static Direction left(Direction facing) {
        return facing.getClockWise();
    }

    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static int maxWidth() {
        return com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.statusPanelMaxWidth);
    }

    public static int maxHeight() {
        return com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.statusPanelMaxHeight);
    }

    /** The top-left panel of the screen {@code pos} is part of (itself, if it's on its own): the one that holds the links. */
    public static BlockPos master(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof StatusPanelBlock)) return pos;
        Direction left = left(state.getValue(FACING));
        BlockPos at = pos;
        for (int i = 0; i < 8 && level.getBlockState(at).getBlock() instanceof StatusPanelBlock && level.getBlockState(at).getValue(JOINED_LEFT); i++) at = at.relative(left);
        for (int i = 0; i < 8 && level.getBlockState(at).getBlock() instanceof StatusPanelBlock && level.getBlockState(at).getValue(JOINED_UP); i++) at = at.above();
        return at;
    }

    /** How many panels across and down the screen whose top-left panel is {@code master} is. */
    public static int[] size(BlockGetter level, BlockPos master) {
        BlockState state = level.getBlockState(master);
        if (!(state.getBlock() instanceof StatusPanelBlock)) return new int[]{1, 1};
        Direction right = right(state.getValue(FACING));
        int w = 1, h = 1;
        BlockPos at = master;
        while (w < 8 && level.getBlockState(at).getBlock() instanceof StatusPanelBlock && level.getBlockState(at).getValue(JOINED_RIGHT)) {
            at = at.relative(right);
            w++;
        }
        at = master;
        while (h < 8 && level.getBlockState(at).getBlock() instanceof StatusPanelBlock && level.getBlockState(at).getValue(JOINED_DOWN)) {
            at = at.below();
            h++;
        }
        return new int[]{w, h};
    }

    /** Every panel of the screen whose top-left panel is {@code master}, row by row. */
    public static java.util.List<BlockPos> members(BlockGetter level, BlockPos master) {
        int[] size = size(level, master);
        Direction right = right(level.getBlockState(master).getValue(FACING));
        java.util.List<BlockPos> list = new java.util.ArrayList<>();
        for (int row = 0; row < size[1]; row++) for (int col = 0; col < size[0]; col++) list.add(master.relative(right, col).below(row));
        return list;
    }

    /**
     * Works out again which panels around {@code start} (facing the same way, touching edge to edge on the wall) make
     * one screen: if they fill a rectangle no bigger than {@code maxWidth} x {@code maxHeight}, they join; otherwise
     * each is on its own. Then sets every one's joined sides to match.
     */
    public static void reform(Level level, BlockPos start) {
        BlockState first = level.getBlockState(start);
        if (!(first.getBlock() instanceof StatusPanelBlock)) return;
        Direction facing = first.getValue(FACING);
        Direction left = left(facing), right = right(facing);
        java.util.Set<BlockPos> group = new java.util.LinkedHashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        group.add(start);
        queue.add(start);
        while (!queue.isEmpty() && group.size() < 32) {
            BlockPos at = queue.poll();
            for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, left, right}) {
                BlockPos next = at.relative(d);
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof StatusPanelBlock && state.getValue(FACING) == facing && group.add(next.immutable())) queue.add(next.immutable());
            }
        }
        int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE, minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
        for (BlockPos p : group) {
            int col = (p.getX() - start.getX()) * right.getStepX() + (p.getZ() - start.getZ()) * right.getStepZ();
            int row = start.getY() - p.getY();
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }
        int w = maxCol - minCol + 1, h = maxRow - minRow + 1;
        boolean joined = group.size() > 1 && group.size() == w * h && w <= maxWidth() && h <= maxHeight();
        for (BlockPos p : group) {
            BlockState state = level.getBlockState(p);
            BlockState next = state.setValue(JOINED_UP, joined && group.contains(p.above())).setValue(JOINED_DOWN, joined && group.contains(p.below()))
                    .setValue(JOINED_LEFT, joined && group.contains(p.relative(left))).setValue(JOINED_RIGHT, joined && group.contains(p.relative(right)));
            if (next != state) level.setBlock(p, next, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) reform(level, pos);
    }

    /** Its neighbours on the wall regroup without it. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (level.getBlockState(pos).is(this)) return;
        Direction facing = state.getValue(FACING);
        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, left(facing), right(facing)}) reform(level, pos.relative(d));
    }

    /** Placed on a wall, it faces out from it; otherwise towards whoever places it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : PlacementFacing.horizontal(context));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatusPanelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.STATUS_PANEL.get(),
                (l, pos, s, be) -> StatusPanelBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** With an empty hand: how many machines are linked, and how to link more. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(master(level, pos)) instanceof StatusPanelBlockEntity be) {
            player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.status_panel.info",
                    be.links().size(), be.capacity()).withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How many linked machines have a problem, up to 15. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(master(level, pos)) instanceof StatusPanelBlockEntity be ? be.comparatorSignal() : 0;
    }
}
