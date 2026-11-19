package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Work in progress: the planned Echo Chamber, so far only its model. The machines' blue-steel casing lined with sculk:
 * a resonator dish on the front whose rings pulse outward, egg-crate sound-dampening foam on the sides with sculk
 * creeping over them, a power port and a sculk catalyst bay at the back, and two glowing sculk tendrils on top (the
 * Warden sheep's "ears") that now and then give off a pop of sculk charge. It does nothing yet; it's registered in
 * development builds only.
 */
public class EchoChamberBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public EchoChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) return;
        // The ears sit 4.5 pixels either side of the middle, across the way the chamber faces.
        Direction across = state.getValue(FACING).getClockWise();
        double side = (random.nextBoolean() ? 4.5 : -4.5) / 16.0;
        level.addParticle(ParticleTypes.SCULK_CHARGE_POP,
                pos.getX() + 0.5 + across.getStepX() * side + (random.nextDouble() - 0.5) * 0.15,
                pos.getY() + 1.35 + random.nextDouble() * 0.2,
                pos.getZ() + 0.5 + across.getStepZ() * side + (random.nextDouble() - 0.5) * 0.15,
                0, 0.02, 0);
    }
}
