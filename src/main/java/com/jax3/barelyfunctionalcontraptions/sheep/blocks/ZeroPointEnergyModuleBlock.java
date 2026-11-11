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
 * Zero Point Energy Module. Placed like an end rod: on a floor it points up, under a ceiling it points down. A module
 * pointing up links with the nearest module above it that points down, in the same column and with only see-through
 * blocks (air, glass...) between them. A linked pair draws a blue beam across the gap and generates FE: the bigger the
 * gap, the more power. Each module pushes its share into the machines and cables touching it. Right-click opens
 * its GUI: stored energy, generation, the beam connection and why it isn't linked.
 */
public class ZeroPointEnergyModuleBlock extends BaseEntityBlock {
    /** Which way the emitter points: UP (floor module) or DOWN (ceiling module). */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.VERTICAL_DIRECTION;
    /** True while the module has a partner and is generating. */
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    private static final VoxelShape SHAPE_UP = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(4, 4, 4, 12, 8, 12));
    private static final VoxelShape SHAPE_DOWN = Shapes.or(Block.box(0, 12, 0, 16, 16, 16), Block.box(4, 8, 4, 12, 12, 12));

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
        Direction clicked = context.getClickedFace();
        Direction facing;
        if (clicked == Direction.DOWN) facing = Direction.DOWN;      // stuck under a ceiling: point down
        else if (clicked == Direction.UP) facing = Direction.UP;     // sat on a floor: point up
        else facing = context.getNearestLookingVerticalDirection().getOpposite(); // on a wall: point back at the player's eye line
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING) == Direction.DOWN ? SHAPE_DOWN : SHAPE_UP;
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
        boolean up = state.getValue(FACING) == Direction.UP;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        double y = pos.getY() + (up ? 0.55 : 0.45);
        level.addParticle(new DustParticleOptions(random.nextBoolean() ? 0x3FC8FF : 0xA8ECFF, 0.7F), x, y, z, 0, 0, 0);
    }
}
