package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

/** The Mechanical Separator block: a horizontally-facing machine with an ACTIVE state used for its textures. */
public class MechanicalSeparatorBlock extends FacingMachineBlock {
    /**
     * Minecraft 26.2 asks every block entity block for a codec; 26.3 doesn't. Not an override, so it builds for both.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public MechanicalSeparatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, com.jax3.barelyfunctionalcontraptions.util.PlacementFacing.horizontal(context));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MechanicalSeparatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.MECHANICAL_SEPARATOR.get(),
                (l, pos, s, be) -> MechanicalSeparatorBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** Shears clear a jam (snipping the lint out), wearing them a little. */
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(net.minecraft.world.item.Items.SHEARS) || !(level.getBlockEntity(pos) instanceof MechanicalSeparatorBlockEntity be) || !be.isJammed()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && be.clearJam()) {
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, player, hand == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MechanicalSeparatorBlockEntity be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MechanicalSeparatorBlockEntity be ? be.getComparatorSignal() : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.52;
        double y = pos.getY() + 0.35 + random.nextDouble() * 0.3;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.52;
        double spread = random.nextDouble() * 0.5 - 0.25;
        if (facing.getAxis() == Direction.Axis.X) z += spread; else x += spread;
        if (random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(0xD8F4FF, 0.6F), x, y, z, 0, 0.02, 0);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.05, 0);
        }
        if (random.nextInt(40) == 0 && !(level.getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable muffler && muffler.muffled())) { // silent with a Muffler card
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.4F, 0.8F, false);
        }
    }
}
