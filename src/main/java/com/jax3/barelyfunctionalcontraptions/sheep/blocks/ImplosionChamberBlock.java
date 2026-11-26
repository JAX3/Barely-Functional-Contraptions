package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Implosion Chamber: a squat blast vessel in the machines' blue-steel casing, a heavy round door with a porthole on
 * its front that glows after each blast ({@link ImplosionChamberBlockEntity}). Explosive sheep in the pen round it do
 * the cracking.
 *
 * <ul>
 *     <li>right-click it with Cobblestone or Gravel to load it;</li>
 *     <li>right-click it with an empty hand to see how it's doing;</li>
 *     <li>sneak-right-click it with an empty hand to show or hide its pen.</li>
 * </ul>
 */
public class ImplosionChamberBlock extends FacingMachineBlock {
    /** Its porthole glows for a moment after a blast. */
    public static final BooleanProperty BLASTING = BooleanProperty.create("blasting");

    /**
     * Minecraft 26.1 and 26.2 ask every block entity block for a codec; 26.3 doesn't. Not an override, so it builds
     * for all of them.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    public ImplosionChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BLASTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BLASTING);
    }

    /** Its door faces whoever places it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, PlacementFacing.horizontal(context));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImplosionChamberBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.IMPLOSION_CHAMBER.get(), (l, pos, s, be) -> ImplosionChamberBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** Cobblestone or Gravel goes into its input. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (ImplosionChamberBlockEntity.crackedInto(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ImplosionChamberBlockEntity be)) return InteractionResult.SUCCESS;
        if (be.insertInput(player.getAbilities().instabuild ? stack.copy() : stack) > 0) {
            level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        } else {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.barelyfunctionalcontraptions.implosion_chamber.full")
                    .withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    /** Says how it's doing; a sneak-right-click shows or hides its pen. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ImplosionChamberBlockEntity be)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) be.toggleArea(player);
        else player.sendOverlayMessage(be.summary().copy().withStyle(ChatFormatting.GRAY));
        return InteractionResult.SUCCESS;
    }

    /** Smoke curls from its vent while its porthole glows. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BLASTING)) return;
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 1.05,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.04, 0);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How full its output is. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ImplosionChamberBlockEntity be ? be.comparatorSignal() : 0;
    }
}
