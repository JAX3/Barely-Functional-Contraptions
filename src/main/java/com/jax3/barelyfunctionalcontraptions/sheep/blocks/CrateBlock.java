package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Wool-Lined Crate: a wooden crate lined with wool that holds lots of one kind of item and shows it on its front
 * ({@link CrateBlockEntity}). No power.
 *
 * <ul>
 *     <li>right-click its front with an item to put it in; twice quickly to put in every one you carry;</li>
 *     <li>left-click its front to take a stack out, sneak-left-click to take one;</li>
 *     <li>sneak-right-click with an empty hand to lock it to its item (or unlock it);</li>
 *     <li>sneak-right-click with dyed wool to line it with that colour (again with the same colour: back to its
 *     lining matching its item), with a Wool Bale to pack it bigger, with a Crate Strap to strap it so it never
 *     bursts.</li>
 * </ul>
 * Sneaking works on any side; storing and taking only on its front, so other sides place blocks and break it as usual.
 * The clicks are handled in {@link CrateEvents}, ahead of the held item, so sneaking with wool lines the crate instead
 * of placing the wool.
 */
public class CrateBlock extends BaseEntityBlock {
    /**
     * Minecraft 26.1 and 26.2 ask every block entity block for a codec; 26.3 doesn't. Not an override, so it builds
     * for all of them.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    /** The way its front (where it shows its item) faces. */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** The colour of the wool lining that shows round its front and lid, once it's been dyed ({@link #DYED}). */
    public static final EnumProperty<DyeColor> LINING = EnumProperty.create("lining", DyeColor.class);
    /**
     * Lined by hand with dyed wool, in {@link #LINING}'s colour. If not, its lining takes the colour of the item it
     * holds (worked out on the client from the item's texture; plain white while it's empty).
     */
    public static final BooleanProperty DYED = BooleanProperty.create("dyed");
    /** Over 90% full: its lid bulges, wool squeezing out under it. */
    public static final BooleanProperty BULGING = BooleanProperty.create("bulging");
    /** Strapped with a Crate Strap: it never bursts. */
    public static final BooleanProperty STRAPPED = BooleanProperty.create("strapped");
    /** Locked to its item: a little gold padlock in the corner of its front. */
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

    public CrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LINING, DyeColor.WHITE)
                .setValue(BULGING, false).setValue(STRAPPED, false).setValue(LOCKED, false).setValue(DYED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINING, BULGING, STRAPPED, LOCKED, DYED);
    }

    /** Its front faces whoever places it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, PlacementFacing.horizontal(context));
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
        return new CrateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.CRATE.get(), (l, pos, s, be) -> CrateBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** Placed with something in it: its lid shows how full it is straight away. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrateBlockEntity be) be.refreshLid();
    }

    // Players reach it through CrateEvents first; these are for anything that uses blocks without those events.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = use(level, pos, state, player, hand, hit.getDirection());
        return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return use(level, pos, state, player, InteractionHand.MAIN_HAND, hit.getDirection());
    }

    /**
     * A right-click on it with whatever {@code player} holds in {@code hand}, on {@code face}: PASS if the crate does
     * nothing with it (so the item, or the other hand, gets its turn).
     */
    public static InteractionResult use(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, Direction face) {
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity be)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            DyeColor colour = liningColour(held);
            if (colour != null) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                if (state.getValue(DYED) && colour == state.getValue(LINING)) {
                    // The same colour again: back to matching its item (the wool isn't used).
                    level.setBlock(pos, state.setValue(DYED, false), Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
                    message(player, Component.translatable("message.barelyfunctionalcontraptions.crate.lining_auto"));
                    return InteractionResult.SUCCESS;
                }
                level.setBlock(pos, state.setValue(LINING, colour).setValue(DYED, true), Block.UPDATE_ALL);
                held.consume(1, player);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                message(player, Component.translatable("message.barelyfunctionalcontraptions.crate.lining_dyed",
                        Component.translatable("color.minecraft." + colour.getName())));
                return InteractionResult.SUCCESS;
            }
            if (held.is(ModItems.WOOL_BALE.get())) {
                if (be.bales() >= CrateBlockEntity.maxBales()) {
                    if (!level.isClientSide()) message(player, Component.translatable("message.barelyfunctionalcontraptions.crate.bales_full", CrateBlockEntity.maxBales()));
                    return InteractionResult.SUCCESS;
                }
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                be.addBale();
                held.consume(1, player);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
                message(player, Component.translatable("message.barelyfunctionalcontraptions.crate.bale", be.bales(), CrateBlockEntity.maxBales(), be.stacks()));
                return InteractionResult.SUCCESS;
            }
            if (held.is(ModItems.CRATE_STRAP.get())) {
                if (state.getValue(STRAPPED)) return InteractionResult.PASS;
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                level.setBlock(pos, state.setValue(STRAPPED, true), Block.UPDATE_ALL);
                held.consume(1, player);
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
                message(player, Component.translatable("message.barelyfunctionalcontraptions.crate.strapped"));
                return InteractionResult.SUCCESS;
            }
            if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                if (be.item().isEmpty()) {
                    message(player, Component.translatable("message.barelyfunctionalcontraptions.crate.lock_empty"));
                    return InteractionResult.SUCCESS;
                }
                be.setLocked(!be.locked());
                level.playSound(null, pos, be.locked() ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
                message(player, Component.translatable("message.barelyfunctionalcontraptions.crate." + (be.locked() ? "locked" : "unlocked"),
                        be.item().getHoverName()));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (face != state.getValue(FACING)) return InteractionResult.PASS;
        // Its front: put the held item in (an empty hand only matters for the double-click).
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!held.isEmpty() && !be.accepts(held)) {
            if (!level.isClientSide()) {
                message(player, !CrateBlockEntity.canStore(held) ? Component.translatable("message.barelyfunctionalcontraptions.crate.cant_store", held.getHoverName())
                        : Component.translatable("message.barelyfunctionalcontraptions.crate.holds", be.item().getHoverName()));
            }
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel && be.store(serverLevel, player, held) < 0) {
            message(player, Component.translatable("message.barelyfunctionalcontraptions.crate.full", be.item().getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    /** The colour of a vanilla dyed wool, or null for anything else. */
    public static @Nullable DyeColor liningColour(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (DyeColor colour : DyeColor.values()) {
            if (stack.is(com.jax3.barelyfunctionalcontraptions.compat.Vanilla.wool(colour))) return colour;
        }
        return null;
    }

    private static void message(Player player, Component text) {
        player.sendOverlayMessage(text.copy().withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How full it is: 0 empty, 1 to 15 after that. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CrateBlockEntity be ? be.comparatorSignal() : 0;
    }

    /** In creative, a crate that still holds something drops itself with it, like a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        KeepsContents.dropInCreative(level, pos, state, player,
                level.getBlockEntity(pos) instanceof CrateBlockEntity be && !be.contents().isEmpty());
        return super.playerWillDestroy(level, pos, state, player);
    }
}
