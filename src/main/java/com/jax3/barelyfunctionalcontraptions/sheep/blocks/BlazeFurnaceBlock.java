package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceBoostDataMap;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.util.PlacementFacing;
import java.util.Locale;
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
 * The Blaze Sheep Furnace block: its front (facing the player who placed it) glows and flickers while it is making
 * power, and flames drift from each sheep heating it into the furnace.
 *
 * <ul>
 *     <li>right-click with a boost item (a Blaze Rod) to load it;</li>
 *     <li>sneak-right-click with an empty hand to show or hide the heating area;</li>
 *     <li>right-click with an empty hand to open its window (the heat column, energy, boost slot and area button).</li>
 * </ul>
 */
public class BlazeFurnaceBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public BlazeFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
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
        return new BlazeFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.BLAZE_FURNACE.get(),
                (l, pos, s, be) -> BlazeFurnaceBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (FurnaceBoostDataMap.of(stack) == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BlazeFurnaceBlockEntity be) {
            int added = be.insertBoost(stack, player);
            if (added > 0) {
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
            } else {
                player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.blaze_furnace.boost_full")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BlazeFurnaceBlockEntity be)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) {
            boolean shown = be.toggleArea();
            player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.area." + (shown ? "shown" : "hidden"))
                    .withStyle(ChatFormatting.GOLD));
            return InteractionResult.SUCCESS;
        }
        player.openMenu(be, pos);
        return InteractionResult.SUCCESS;
    }

    /** "Blaze Sheep Furnace: 150 FE/t from 2 heat sources (boosted x2 for 12s, 5 Blaze Rods waiting)". */
    public static Component statusMessage(BlazeFurnaceBlockEntity be) {
        String key = "message.barelyfunctionalcontraptions.blaze_furnace." + (be.heaterCount() > 0 ? "running" : "cold");
        var message = Component.translatable(key, be.generating(), be.heaterCount()).withStyle(be.heaterCount() > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        if (be.boostTicks() > 0) {
            message.append(Component.translatable("message.barelyfunctionalcontraptions.blaze_furnace.boosted",
                    multiplier(be.boostMultiplier()), (be.boostTicks() + 19) / 20).withStyle(ChatFormatting.YELLOW));
        }
        if (!be.boostItems().isEmpty()) {
            message.append(Component.translatable("message.barelyfunctionalcontraptions.blaze_furnace.waiting",
                    be.boostItems().getCount(), be.boostItems().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        return message;
    }

    public static String multiplier(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "");
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        // Flames and smoke at the front grate, like a lit furnace.
        Direction facing = state.getValue(FACING);
        double side = random.nextDouble() * 0.6 - 0.3;
        double dx = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * 0.52 : side;
        double dz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * 0.52 : side;
        double dy = random.nextDouble() * 6.0 / 16.0;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0, 0, 0);
        // Flames drifting from each sheep heating it into the furnace.
        if (level.getBlockEntity(pos) instanceof BlazeFurnaceBlockEntity be) {
            Vec3 target = Vec3.atCenterOf(pos);
            for (int id : be.heaters()) {
                Entity heater = level.getEntity(id);
                if (heater == null || random.nextInt(2) != 0) continue;
                Vec3 from = heater.position().add((random.nextDouble() - 0.5) * heater.getBbWidth(),
                        heater.getBbHeight() * (0.4 + random.nextDouble() * 0.5), (random.nextDouble() - 0.5) * heater.getBbWidth());
                Vec3 velocity = target.subtract(from).scale(1.0 / 20.0); // about a second to reach the furnace
                level.addParticle(ParticleTypes.SMALL_FLAME, from.x, from.y, from.z, velocity.x, velocity.y, velocity.z);
            }
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BlazeFurnaceBlockEntity be ? be.comparatorSignal() : 0;
    }
}
