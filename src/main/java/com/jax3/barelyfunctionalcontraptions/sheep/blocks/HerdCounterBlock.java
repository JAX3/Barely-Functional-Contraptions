package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Herd Counter block: a slim panel in the machines' blue-steel casing with a little screen on its front and a tally
 * wheel on its side. It counts the sheep in the pen in front of its screen ({@link HerdCounterBlockEntity}).
 *
 * <ul>
 *     <li>right-click with an empty hand to open its window (what it counts, its filter, its Full target);</li>
 *     <li>right-click with a resource wool or spawn egg (or plain wool, or a sheep's egg) to count only that resource;
 *     again with the same one to count every sheep;</li>
 *     <li>sneak-right-click with an empty hand to show or hide its area.</li>
 * </ul>
 */
public class HerdCounterBlock extends FacingMachineBlock {
    /**
     * Minecraft 26.1 and 26.2 ask every block entity block for a codec; 26.3 doesn't. Not an override, so it builds
     * for all of them.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    /** The way its screen faces, and the side its pen is on. */

    private static final VoxelShape NORTH = Block.box(1, 0, 7, 15, 14, 15), SOUTH = Block.box(1, 0, 1, 15, 14, 9),
            EAST = Block.box(1, 0, 1, 9, 14, 15), WEST = Block.box(7, 0, 1, 15, 14, 15);

    public HerdCounterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Its screen faces whoever places it, so its pen is the side they're standing on. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, PlacementFacing.horizontal(context));
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HerdCounterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.HERD_COUNTER.get(),
                (l, pos, s, be) -> HerdCounterBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** A resource wool or spawn egg sets its filter, or clears it if it's the one already set. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        String key = HerdCounterBlockEntity.filterKey(stack);
        if (key == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof HerdCounterBlockEntity be)) return InteractionResult.SUCCESS;
        boolean clear = key.equals(HerdCounterBlockEntity.filterKey(be.filter()));
        be.setFilter(clear ? ItemStack.EMPTY : stack);
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, clear ? 0.8F : 1.4F);
        player.sendOverlayMessage(clear ? Component.translatable("message.barelyfunctionalcontraptions.herd_counter.filter_cleared").withStyle(ChatFormatting.GRAY)
                : Component.translatable("message.barelyfunctionalcontraptions.herd_counter.filter", stack.getHoverName()).withStyle(ChatFormatting.GRAY));
        return InteractionResult.SUCCESS;
    }

    /** Opens its window; a sneak-right-click shows or hides its area. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof HerdCounterBlockEntity be)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) {
            be.toggleArea(player);
            return InteractionResult.SUCCESS;
        }
        player.openMenu(be, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof HerdCounterBlockEntity be ? be.comparatorSignal() : 0;
    }
}
