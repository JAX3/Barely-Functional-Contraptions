package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Zero Point Energy Module. Placed like an end rod, pointing away from the block it was placed against: up from a
 * floor, down from a ceiling, or sideways from a wall. A module links with the nearest module straight ahead of its
 * emitter that points back at it, with only see-through blocks (air, glass...) between them. A linked pair draws a
 * blue beam across the gap and generates FE: the bigger the gap, the more power. Each module pushes its share into the machines and cables touching it. Right-click opens
 * its GUI: stored energy, generation, the beam connection and why it isn't linked.
 */
public class ZeroPointEnergyModuleBlock extends BaseEntityBlock {
    /** Which way the emitter points: away from the block the module is attached to, in any of the six directions. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** True while the module has a partner and is generating. */
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    /** The plate against the attached face and the emitter on top of it, for each way the emitter can point. */
    private static final java.util.Map<Direction, VoxelShape> SHAPES = java.util.Map.of(
            Direction.UP, Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(4, 4, 4, 12, 8, 12)),
            Direction.DOWN, Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(4, 8, 4, 12, 12, 12)),
            Direction.NORTH, Shapes.or(Block.box(0, 0, 12, 16, 16, 16), Block.box(4, 4, 8, 12, 12, 12)),
            Direction.SOUTH, Shapes.or(Block.box(0, 0, 0, 16, 16, 4), Block.box(4, 4, 4, 12, 12, 8)),
            Direction.WEST, Shapes.or(Block.box(12, 0, 0, 16, 16, 16), Block.box(8, 4, 4, 12, 12, 12)),
            Direction.EAST, Shapes.or(Block.box(0, 0, 0, 4, 16, 16), Block.box(4, 4, 4, 8, 12, 12)));

    public ZeroPointEnergyModuleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINKED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Point away from the face it was placed on: up from a floor, down from a ceiling, out from a wall.
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZeroPointEnergyModuleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) {
            // Client: keeps the energy hum playing while the module is linked.
            return createTickerHelper(type, ModBlockEntities.ZERO_POINT_ENERGY_MODULE.get(), (l, pos, s, be) -> be.clientTick());
        }
        return createTickerHelper(type, ModBlockEntities.ZERO_POINT_ENERGY_MODULE.get(),
                (l, pos, s, be) -> be.serverTick(serverLevel, pos, s));
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                  net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ZeroPointEnergyModuleBlockEntity be) {
            player.openMenu(be, pos);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LINKED) || random.nextInt(2) != 0) return;
        // Just in front of the emitter's tip (half a block out from the attached face), spread across its face.
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.05 + (facing.getStepX() == 0 ? (random.nextDouble() - 0.5) * 0.5 : 0);
        double y = pos.getY() + 0.5 + facing.getStepY() * 0.05 + (facing.getStepY() == 0 ? (random.nextDouble() - 0.5) * 0.5 : 0);
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.05 + (facing.getStepZ() == 0 ? (random.nextDouble() - 0.5) * 0.5 : 0);
        level.addParticle(new DustParticleOptions(random.nextBoolean() ? 0x3FC8FF : 0xA8ECFF, 0.7F), x, y, z, 0, 0, 0);
    }
}
