package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepFeatureEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The invisible light a glowing sheep leaves in the block it stands in (see {@link SheepAppearance.Feature#GLOWING}),
 * at {@link #LIGHT_LEVEL}; and the light an Energy sheep ({@link SheepAppearance.Feature#CHARGED}) gives off, at a
 * {@link #LEVEL} that follows its charge.
 *
 * <p>It can't be seen, touched or targeted, drops nothing and is replaced by anything placed there. It checks itself
 * every {@link #CHECK_TICKS} ticks and turns back into air as soon as no glowing sheep is standing in it, so it never
 * outlives the sheep, even across a save or when the chunk was unloaded in between (scheduled ticks are saved).</p>
 */
public class SheepGlowBlock extends Block {
    public static final int LIGHT_LEVEL = 12;
    public static final int CHECK_TICKS = 10;
    /** How bright it is. */
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty LEVEL =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("level", 1, 15);

    public SheepGlowBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, LIGHT_LEVEL));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    /** The light for a sheep at {@code level}. */
    public BlockState at(int level) {
        return defaultBlockState().setValue(LEVEL, Math.max(1, Math.min(15, level)));
    }

    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) level.scheduleTick(pos, this, CHECK_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean lit = !level.getEntitiesOfClass(Sheep.class, new AABB(pos),
                sheep -> sheep.blockPosition().equals(pos) && (SheepFeatureEvents.has(sheep, SheepAppearance.Feature.GLOWING)
                        || (SheepFeatureEvents.has(sheep, SheepAppearance.Feature.CHARGED)
                                && com.jax3.barelyfunctionalcontraptions.sheep.entity.EnergySheep.charge(sheep) > 0))).isEmpty();
        if (lit) level.scheduleTick(pos, this, CHECK_TICKS);
        else level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
