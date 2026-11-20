package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * The Grass Seeder block: a squat machine on four legs with a seed hopper on top and a brass sprinkler, which keeps the
 * ground of a sheep pen green (see {@link GrassSeederBlockEntity}). The sprinkler drips while it works.
 *
 * <ul>
 *     <li>right-click with Bone Meal to load it, or with a Water Bucket to pour water in;</li>
 *     <li>sneak-right-click with an empty hand to show or hide its area;</li>
 *     <li>right-click with an empty hand to open its window.</li>
 * </ul>
 */
public class GrassSeederBlock extends BaseEntityBlock {
    /**
     * Minecraft 26.1 and 26.2 ask every block entity block for a codec; 26.3 doesn't. Not an override, so it builds
     * for all of them.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 4, 4, 4), Block.box(12, 0, 1, 15, 4, 4), Block.box(1, 0, 12, 4, 4, 15), Block.box(12, 0, 12, 15, 4, 15),
            Block.box(1, 4, 1, 15, 13, 15), Block.box(7, 13, 7, 9, 15, 9), Block.box(3, 15, 7, 13, 16, 9), Block.box(7, 15, 3, 9, 16, 13));

    public GrassSeederBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrassSeederBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.GRASS_SEEDER.get(),
                (l, pos, s, be) -> GrassSeederBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        boolean boneMeal = stack.is(Items.BONE_MEAL), water = stack.is(Items.WATER_BUCKET);
        if (!boneMeal && !water) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof GrassSeederBlockEntity be)) return InteractionResult.SUCCESS;
        if (boneMeal) {
            int taken = be.insertBoneMeal(player.getAbilities().instabuild ? stack.copy() : stack);
            if (taken > 0) level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            else player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.grass_seeder.full").withStyle(ChatFormatting.GRAY));
        } else if (be.fillFromBucket()) {
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        } else {
            player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.grass_seeder.tank_full").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof GrassSeederBlockEntity be)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) be.toggleArea(player);
        else player.openMenu(be, pos);
        return InteractionResult.SUCCESS;
    }

    /** Its sprinkler drips from the ends of its arms while it works. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        for (Direction arm : Direction.Plane.HORIZONTAL) {
            if (random.nextInt(3) != 0) continue;
            level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.5 + arm.getStepX() * 0.28, pos.getY() + 0.9,
                    pos.getZ() + 0.5 + arm.getStepZ() * 0.28, 0, 0, 0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How green its area is: 0 all dirt, 15 all grass. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof GrassSeederBlockEntity be ? be.comparatorSignal() : 0;
    }
}
