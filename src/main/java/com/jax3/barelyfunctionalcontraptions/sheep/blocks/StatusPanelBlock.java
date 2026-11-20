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

    private static final VoxelShape NORTH = Block.box(0, 0, 14, 16, 16, 16), SOUTH = Block.box(0, 0, 0, 16, 16, 2),
            EAST = Block.box(0, 0, 0, 2, 16, 16), WEST = Block.box(14, 0, 0, 16, 16, 16);

    public StatusPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StatusPanelBlockEntity be) {
            player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.status_panel.info",
                    be.links().size(), StatusPanelBlockEntity.maxLinks()).withStyle(ChatFormatting.GRAY));
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
        return level.getBlockEntity(pos) instanceof StatusPanelBlockEntity be ? be.comparatorSignal() : 0;
    }
}
