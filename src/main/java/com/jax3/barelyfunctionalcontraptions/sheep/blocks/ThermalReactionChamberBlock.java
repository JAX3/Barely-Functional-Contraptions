package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Thermal Reaction Chamber block: the machine casing split down the middle, red-hot vents on its left (hot) side
 * and frosted vents on its right (cold) side, a gauge panel at the front, water and power at the back, and a window on
 * top where flame and snow chase each other while it reacts. Embers drift in from the Blaze sheep heating it and
 * snowflakes from the Snow sheep chilling it.
 *
 * <ul>
 *     <li>right-click with a Water Bucket to pour it in;</li>
 *     <li>sneak-right-click with an empty hand to show or hide both pens;</li>
 *     <li>right-click with an empty hand to open its window.</li>
 * </ul>
 */
public class ThermalReactionChamberBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty FROZEN = BooleanProperty.create("frozen");

    public ThermalReactionChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false).setValue(FROZEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, FROZEN);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermalReactionChamberBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.THERMAL_REACTION_CHAMBER.get(),
                (l, pos, s, be) -> ThermalReactionChamberBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.WATER_BUCKET)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ThermalReactionChamberBlockEntity be) {
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
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ThermalReactionChamberBlockEntity be)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) {
            be.toggleArea(player);
            return InteractionResult.SUCCESS;
        }
        player.openMenu(be, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof ThermalReactionChamberBlockEntity be)) return;
        Direction facing = state.getValue(FACING);
        Vec3 centre = Vec3.atCenterOf(pos);
        State current = be.state();
        boolean active = state.getValue(ACTIVE);
        // Embers from each Blaze sheep and snowflakes from each Snow sheep, drifting into their side of the chamber.
        if (active) {
            stream(level, random, be.hotSources(), face(centre, ThermalReactionChamberBlockEntity.hotSide(facing)), ParticleTypes.SMALL_FLAME);
            stream(level, random, be.coldSources(), face(centre, ThermalReactionChamberBlockEntity.coldSide(facing)), ParticleTypes.SNOWFLAKE);
            // Each vent sheds its own: embers rising off the hot side, snow falling off the cold side.
            vent(level, random, centre, ThermalReactionChamberBlockEntity.hotSide(facing), ParticleTypes.SMALL_FLAME, 0.02);
            vent(level, random, centre, ThermalReactionChamberBlockEntity.coldSide(facing), ParticleTypes.SNOWFLAKE, -0.02);
        }
        if (current == State.RUNNING_HOT || current == State.OVERHEATED) {
            // Steam puffing out of the top while it runs hot, smoke once it has overheated.
            ParticleOptions puff = current == State.OVERHEATED ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD;
            if (random.nextInt(2) == 0) {
                level.addParticle(puff, centre.x + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 1.05, centre.z + (random.nextDouble() - 0.5) * 0.6,
                        0, 0.06 + random.nextDouble() * 0.04, 0);
            }
            if (random.nextInt(20) == 0) level.playLocalSound(pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.8F, false);
        } else if (current == State.RUNNING_COLD || state.getValue(FROZEN)) {
            // Frost creeping over the casing.
            for (int i = 0; i < 2; i++) {
                Direction side = Direction.from2DDataValue(random.nextInt(4));
                Vec3 at = face(centre, side).add((random.nextDouble() - 0.5) * 0.9 * Math.abs(side.getStepZ()), (random.nextDouble() - 0.5) * 0.9,
                        (random.nextDouble() - 0.5) * 0.9 * Math.abs(side.getStepX()));
                level.addParticle(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 0, -0.01, 0);
            }
        }
    }

    /** The middle of {@code side}'s face, a hair outside it. */
    private static Vec3 face(Vec3 centre, Direction side) {
        return centre.add(side.getStepX() * 0.52, 0, side.getStepZ() * 0.52);
    }

    private static void stream(Level level, RandomSource random, int[] ids, Vec3 target, ParticleOptions particle) {
        for (int id : ids) {
            Entity sheep = level.getEntity(id);
            if (sheep == null || random.nextInt(3) != 0) continue;
            Vec3 from = sheep.position().add((random.nextDouble() - 0.5) * sheep.getBbWidth(),
                    sheep.getBbHeight() * (0.4 + random.nextDouble() * 0.5), (random.nextDouble() - 0.5) * sheep.getBbWidth());
            Vec3 velocity = target.subtract(from).scale(1.0 / 20.0);
            level.addParticle(particle, from.x, from.y, from.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static void vent(Level level, RandomSource random, Vec3 centre, Direction side, ParticleOptions particle, double rise) {
        if (random.nextInt(2) != 0) return;
        double along = (random.nextDouble() - 0.5) * 0.7;
        Vec3 at = face(centre, side).add(along * Math.abs(side.getStepZ()), (random.nextDouble() - 0.5) * 0.6, along * Math.abs(side.getStepX()));
        level.addParticle(particle, at.x, at.y, at.z, side.getStepX() * 0.02, rise, side.getStepZ() * 0.02);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How close it is to melting down or freezing: 0 steady, 15 about to go. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ThermalReactionChamberBlockEntity be ? be.comparatorSignal() : 0;
    }
}
