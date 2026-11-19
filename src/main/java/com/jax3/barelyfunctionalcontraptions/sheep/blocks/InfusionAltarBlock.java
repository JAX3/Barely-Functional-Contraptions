package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Infusion Altar: a pedestal that infuses the sheep beside it. Right-click to open its window; its top glows and
 * sparkles while it infuses.
 */
public class InfusionAltarBlock extends BaseEntityBlock {
    /**
     * Minecraft 26.2 asks every block entity block for a codec; 26.3 doesn't. Not an override, so it builds for both.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** On for one redstone tick when the items for an infusion are all in, for automation. */
    public static final BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;
    /** A wide base, a narrower column and a wide top: 16 px tall like a full block. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(3, 4, 3, 13, 12, 13), Block.box(1, 12, 1, 15, 16, 15));

    public InfusionAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfusionAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.INFUSION_ALTAR.get(),
                (l, pos, s, be) -> InfusionAltarBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof InfusionAltarBlockEntity be)) return InteractionResult.SUCCESS;
        // Sneak-right-click with an empty hand shows or hides the area a sheep must stand in.
        if (player.isSecondaryUseActive()) be.toggleArea(player);
        else player.openMenu(be, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 1.05, z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.ENCHANT, x + random.nextGaussian() * 0.3, y + random.nextDouble() * 0.6, z + random.nextGaussian() * 0.3, 0, 0.05, 0);
        }
        if (random.nextInt(20) == 0) level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.5F, 1.2F, false);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** A redstone pulse on every side for one redstone tick when the resource and its catalysts are all in. */
    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof InfusionAltarBlockEntity be) || be.maxProgress() <= 0) return 0;
        return be.progress() * 15 / be.maxProgress();
    }
}
