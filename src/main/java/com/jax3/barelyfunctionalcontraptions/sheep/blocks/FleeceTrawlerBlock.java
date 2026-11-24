package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import org.jspecify.annotations.Nullable;

/**
 * The Fleece Trawler: a dockside winch in the machines' blue-steel casing with a drum of wool net on its front, set at
 * the edge of a pool, facing it ({@link FleeceTrawlerBlockEntity}). Tide sheep swimming in the pool drive it; it
 * needs no power.
 *
 * <ul>
 *     <li>right-click it with resource wool to put a lure in;</li>
 *     <li>right-click it with shears to cut a tangled net free;</li>
 *     <li>right-click it with an empty hand to take what it's caught (and hear how it's doing).</li>
 * </ul>
 */
public class FleeceTrawlerBlock extends BaseEntityBlock {
    /**
     * Minecraft 26.1 and 26.2 ask every block entity block for a codec; 26.3 doesn't. Not an override, so it builds
     * for all of them.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    /** The way its net faces: its pool is in front of it. */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public FleeceTrawlerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Placed at the water's edge, its net faces away from whoever places it: out over the water. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, PlacementFacing.horizontal(context).getOpposite());
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
        return new FleeceTrawlerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.FLEECE_TRAWLER.get(), (l, pos, s, be) -> FleeceTrawlerBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** Shears cut a tangled net free; resource wool goes in as a lure. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FleeceTrawlerBlockEntity be)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.is(Items.SHEARS) || stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_SHEAR)) {
            if (!be.tangled()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level instanceof ServerLevel server) {
                be.untangle(server, player);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                        : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                message(player, Component.translatable("message.barelyfunctionalcontraptions.fleece_trawler.untangled"));
            }
            return InteractionResult.SUCCESS;
        }
        if (com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.getResourceId(stack) != null) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (be.addLure(stack) > 0) {
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
                message(player, Component.translatable("message.barelyfunctionalcontraptions.fleece_trawler.lure", be.getItem(FleeceTrawlerBlockEntity.LURE).getHoverName()));
            } else {
                message(player, Component.translatable("message.barelyfunctionalcontraptions.fleece_trawler.lure_full"));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Hands over what it's caught, or says how it's doing. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof FleeceTrawlerBlockEntity be)) return InteractionResult.SUCCESS;
        be.look(server);
        if (be.giveCatch(player)) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.0F);
        }
        message(player, Component.translatable("message.barelyfunctionalcontraptions.fleece_trawler.summary",
                Component.translatable("status_panel.barelyfunctionalcontraptions.state." + be.statusKey()), be.sheepCount(), be.pool(), be.progressPercent()));
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, Component text) {
        player.sendOverlayMessage(text.copy().withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How much of its catch is waiting to be taken. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof FleeceTrawlerBlockEntity be ? be.comparatorSignal() : 0;
    }
}
