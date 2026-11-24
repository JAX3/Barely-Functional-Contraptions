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
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity;

/**
 * The Powered Furnace block: smelts on Forge Energy, faster than a Furnace, and faster still with Blaze sheep around
 * it. Its mouth glows while it smelts, and flames drift from each Blaze sheep heating it.
 *
 * <ul>
 *     <li>sneak-right-click with an empty hand to show or hide the area Blaze sheep must stand in;</li>
 *     <li>right-click to open its window.</li>
 * </ul>
 */
public class PoweredFurnaceBlock extends FacingMachineBlock {
    /**
     * Minecraft 26.2 asks every block entity block for a codec; 26.3 doesn't. Not an override, so it builds for both.
     */
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public PoweredFurnaceBlock(Properties properties) {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PoweredFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.POWERED_FURNACE.get(),
                (l, pos, s, be) -> PoweredFurnaceBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof PoweredFurnaceBlockEntity be)) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive()) {
            be.toggleArea(player);
            return InteractionResult.SUCCESS;
        }
        player.openMenu(be, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        Direction facing = state.getValue(FACING);
        double side = random.nextDouble() * 0.5 - 0.25;
        double dx = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * 0.52 : side;
        double dz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * 0.52 : side;
        double dy = 0.1 + random.nextDouble() * 5.0 / 16.0;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0, 0, 0);
        if (level.getBlockEntity(pos) instanceof PoweredFurnaceBlockEntity be) {
            Vec3 target = Vec3.atCenterOf(pos);
            for (int id : be.heaters()) {
                Entity heater = level.getEntity(id);
                if (heater == null || random.nextInt(2) != 0) continue;
                Vec3 from = heater.position().add((random.nextDouble() - 0.5) * heater.getBbWidth(),
                        heater.getBbHeight() * (0.4 + random.nextDouble() * 0.5), (random.nextDouble() - 0.5) * heater.getBbWidth());
                Vec3 velocity = target.subtract(from).scale(1.0 / 20.0);
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
        return level.getBlockEntity(pos) instanceof PoweredFurnaceBlockEntity be ? be.getComparatorSignal() : 0;
    }
}
