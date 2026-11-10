package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeGroup;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Hume Energy Cube: stores Forge Energy, takes it in and gives it out on every face, and joins every cube it
 * touches into one bigger battery. Right-click to see the group's charge.
 */
public class EnergyCubeBlock extends BaseEntityBlock {
    /** How full the cube's group is, 0..4, drawn as the energy level in its window. */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public EnergyCubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCubeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.HUME_ENERGY_CUBE.get(),
                (l, pos, s, be) -> EnergyCubeBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    /** A new cube joins its neighbours: their groups are rebuilt to include it. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(this)) return; // only the charge display changed
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof EnergyCubeBlockEntity neighbour) neighbour.invalidateGroup();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnergyCubeBlockEntity be) {
            EnergyCubeGroup group = be.group();
            player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.energy_cube.info",
                    group.size(), fe(group.amount()), fe(group.capacity()), fe(group.rate())).withStyle(ChatFormatting.LIGHT_PURPLE));
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
        if (level.getBlockEntity(pos) instanceof EnergyCubeBlockEntity be) {
            EnergyCubeGroup group = be.group();
            long amount = group.amount(), capacity = group.capacity();
            return amount <= 0 || capacity <= 0 ? 0 : 1 + (int) (14L * amount / capacity);
        }
        return 0;
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
