package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
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
 * The Energy Meter block. {@link #FACING} is where the energy comes out (the glowing port); it comes in at the back.
 * It faces itself when placed ({@link #getStateForPlacement}), and the wrench flips or turns it. Right-click shows the reading; a comparator reads
 * the flow ({@link EnergyMeterBlockEntity#signalFor}).
 */
public class EnergyMeterBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;

    public EnergyMeterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Works out which way the energy should go, so the meter connects however it was placed:
     * <ol>
     *     <li>placed against a block with an energy connection: energy comes in from that block;</li>
     *     <li>otherwise, between two such blocks on opposite sides: energy comes in from the one with energy to give
     *     (a running generator, a charged cube) and goes out to the one that takes it;</li>
     *     <li>otherwise, next to just one: energy comes in from it;</li>
     *     <li>otherwise it points the way you look.</li>
     * </ol>
     * The Barely Functional Wrench flips it afterwards (sneak with the wrench to turn it to the next direction).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction against = context.getClickedFace().getOpposite();
        if (energyAt(level, pos, against) != null) return defaultBlockState().setValue(FACING, against.getOpposite());
        Direction best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Direction in : Direction.values()) {
            var source = energyAt(level, pos, in);
            var target = energyAt(level, pos, in.getOpposite());
            if (source == null || target == null) continue;
            int score = sourceScore(source) - sourceScore(target);
            if (best == null || score > bestScore) {
                best = in;
                bestScore = score;
            }
        }
        if (best != null) return defaultBlockState().setValue(FACING, best.getOpposite());
        for (Direction in : Direction.values()) {
            if (energyAt(level, pos, in) != null) return defaultBlockState().setValue(FACING, in.getOpposite());
        }
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    /** The energy connection of the block on side {@code dir} of {@code pos}, facing the meter. */
    private static net.neoforged.neoforge.transfer.energy.@Nullable EnergyHandler energyAt(Level level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        if (!level.isLoaded(other) || level.getBlockEntity(other) instanceof EnergyMeterBlockEntity) return null;
        return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, other, dir.getOpposite());
    }

    /** How much a block looks like a source: energy it could give, minus what it would take. */
    private static int sourceScore(net.neoforged.neoforge.transfer.energy.EnergyHandler handler) {
        try (var simulate = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int gives = handler.extract(Integer.MAX_VALUE / 4, simulate);
            int takes = handler.insert(Integer.MAX_VALUE / 4, simulate);
            return (gives > 0 ? 2 : 0) - (takes > 0 ? 1 : 0);
        }
    }

    /** The wrench: flips the meter end to end, or with sneak turns it to the next direction. */
    public static void turnWithWrench(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EnergyMeterBlock)) return;
        Direction facing = state.getValue(FACING);
        Direction next = player.isSecondaryUseActive() ? Direction.from3DDataValue((facing.get3DDataValue() + 1) % 6) : facing.getOpposite();
        level.setBlock(pos, state.setValue(FACING, next), Block.UPDATE_ALL);
        level.invalidateCapabilities(pos);
        player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.energy_meter.turned",
                Component.translatable("direction.barelyfunctionalcontraptions." + next.getOpposite().getSerializedName()),
                Component.translatable("direction.barelyfunctionalcontraptions." + next.getSerializedName())));
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
        return new EnergyMeterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.ENERGY_METER.get(),
                (l, pos, s, be) -> EnergyMeterBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnergyMeterBlockEntity be) {
            player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.energy_meter.reading",
                    String.format(Locale.ROOT, "%,d", be.rate()), String.format(Locale.ROOT, "%,d", be.total()))
                    .withStyle(be.rate() > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof EnergyMeterBlockEntity be ? EnergyMeterBlockEntity.signalFor(be.rate()) : 0;
    }
}
