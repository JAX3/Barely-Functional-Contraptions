package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity;
import net.minecraft.world.item.Items;

/**
 * The Snow Sheep Chiller block: freezes water into Ice with the cold of the Snow sheep around it. Its front window
 * frosts over while it freezes, and snowflakes drift from each sheep chilling it.
 *
 * <ul>
 *     <li>right-click with a Water Bucket to pour it in;</li>
 *     <li>sneak-right-click with an empty hand to show or hide the chilling area;</li>
 *     <li>right-click with an empty hand to open its window (water, cold, the Ice slot and the area button).</li>
 * </ul>
 */
public class ChillerBlock extends FacingMachineBlock {
    /**
     * Minecraft 26.2 asks every block entity block for a codec; 26.3 doesn't. Not an override, so it builds for both.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ChillerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, PlacementFacing.horizontal(context));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChillerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.CHILLER.get(),
                (l, pos, s, be) -> ChillerBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.WATER_BUCKET)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ChillerBlockEntity be) {
            if (be.fillFromBucket()) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            } else {
                player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.chiller.tank_full").withStyle(ChatFormatting.GRAY));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ChillerBlockEntity be)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) {
            be.toggleArea(player);
            return InteractionResult.SUCCESS;
        }
        player.openMenu(be, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof ChillerBlockEntity be)) return;
        // Snowflakes drifting from each Snow sheep chilling it into the chiller.
        Vec3 target = Vec3.atCenterOf(pos);
        for (int id : be.chillers()) {
            Entity sheep = level.getEntity(id);
            if (sheep == null || random.nextInt(2) != 0) continue;
            Vec3 from = sheep.position().add((random.nextDouble() - 0.5) * sheep.getBbWidth(),
                    sheep.getBbHeight() * (0.4 + random.nextDouble() * 0.5), (random.nextDouble() - 0.5) * sheep.getBbWidth());
            Vec3 velocity = target.subtract(from).scale(1.0 / 20.0);
            level.addParticle(ParticleTypes.SNOWFLAKE, from.x, from.y, from.z, velocity.x, velocity.y, velocity.z);
        }
        if (state.getValue(ACTIVE) && random.nextInt(3) == 0) {
            // A cold mist rolling off the front window.
            Direction facing = state.getValue(FACING);
            double side = random.nextDouble() * 0.6 - 0.3;
            double dx = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * 0.52 : side;
            double dz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * 0.52 : side;
            level.addParticle(ParticleTypes.SNOWFLAKE, target.x + dx, pos.getY() + 0.3 + random.nextDouble() * 0.4, target.z + dz,
                    facing.getStepX() * 0.02, -0.01, facing.getStepZ() * 0.02);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ChillerBlockEntity be ? be.comparatorSignal() : 0;
    }
}
