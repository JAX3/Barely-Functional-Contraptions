package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.burner.WoolBurnerFuels;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
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
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 * The Wool Burner block: its front (facing the player who placed it) glows while something burns, with smoke from the
 * chimney on top. Right-click with fuel to load it, or with an empty hand to open its window.
 */
public class WoolBurnerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public WoolBurnerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

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
        return new WoolBurnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.WOOL_BURNER.get(),
                (l, pos, s, be) -> WoolBurnerBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** Right-click with fuel: as much of the stack as fits goes in. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!WoolBurnerFuels.isFuel(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WoolBurnerBlockEntity be) {
            ItemStack slot = be.fuel();
            int room = slot.isEmpty() ? stack.getMaxStackSize()
                    : ItemStack.isSameItemSameComponents(slot, stack) ? slot.getMaxStackSize() - slot.getCount() : 0;
            int move = Math.min(room, stack.getCount());
            if (move > 0) {
                if (slot.isEmpty()) be.setItem(0, stack.copyWithCount(move));
                else slot.grow(move);
                if (!player.getAbilities().instabuild) stack.shrink(move);
                be.setChanged();
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
            } else {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WoolBurnerBlockEntity be) player.openMenu(be, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction facing = state.getValue(FACING);
        double side = random.nextDouble() * 0.6 - 0.3;
        double dx = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * 0.52 : side;
        double dz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * 0.52 : side;
        level.addParticle(ParticleTypes.FLAME, x + dx, y + random.nextDouble() * 6.0 / 16.0, z + dz, 0, 0, 0);
        // Smoke out of the chimney on top.
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.2, y + 1.05, z + (random.nextDouble() - 0.5) * 0.2, 0, 0.05, 0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
