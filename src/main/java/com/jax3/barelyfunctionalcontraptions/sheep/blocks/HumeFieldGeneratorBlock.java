package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Work in progress: the planned Hume Field Generator, so far only its model. A steel base and cap on four pillars,
 * two glowing field coils round a glass shell, a core that flickers between "on" and "off", and a lens on top that
 * watches it. It does nothing yet; it's registered in development builds only.
 */
public class HumeFieldGeneratorBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),      // base
            Block.box(1, 3, 1, 15, 13, 15),     // pillars, coils and shell
            Block.box(2, 13, 2, 14, 15, 14),    // cap
            Block.box(6, 15, 6, 10, 16, 10));   // lens

    public HumeFieldGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
