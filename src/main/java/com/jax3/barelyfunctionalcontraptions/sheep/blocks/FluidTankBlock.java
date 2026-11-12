package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankGroup;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

/**
 * Fluid Tank: stores one fluid. Touching tanks join into one big tank (see {@link FluidTankGroup}) whose capacity and
 * transfer rate grow with every tank, and their casings join up like the Hume Energy Cube's: the frame drops where
 * tanks touch, so a group looks like one tank with one glass window, and the fluid inside is drawn as one level running
 * across it. Buckets and other fluid containers fill and empty it with a right-click; with an empty hand it opens the
 * GUI.
 */
public class FluidTankBlock extends BaseEntityBlock {
    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.FLUID_TANK.get(),
                (l, pos, s, be) -> FluidTankBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** A new tank joins its neighbours: their groups are rebuilt to include it. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(this)) return;
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof FluidTankBlockEntity neighbour) neighbour.invalidateGroup();
        }
    }

    /** Buckets and other fluid containers fill or empty the whole group. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Fluid.ITEM) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND; // not a fluid container: open the GUI instead
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity be
                && FluidUtil.interactWithFluidHandler(player, hand, pos, be.getFluidHandler(), null)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Opens the tank's GUI: the group's fluid, capacity, size and flow. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity be) {
            player.openMenu(be, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparators read how full the whole group is. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity be) {
            FluidTankGroup group = be.group();
            long amount = group.amount(), capacity = group.capacity();
            return amount <= 0 || capacity <= 0 ? 0 : 1 + (int) (14L * amount / capacity);
        }
        return 0;
    }

    /** See-through windows: neighbouring faces and light aren't blocked. */
    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
