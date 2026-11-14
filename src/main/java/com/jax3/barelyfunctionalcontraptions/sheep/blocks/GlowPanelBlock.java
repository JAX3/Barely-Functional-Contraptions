package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GlowPanelBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Glow Panel: a thin panel (like a daylight detector) whose cells light up while it is making power, brighter the
 * more it makes: {@link #GLOW} 0 is dark, 1 dim (dusk, rain), 2 half (a Glow Lamp at night) and 3 full (noon sun).
 */
public class GlowPanelBlock extends BaseEntityBlock {
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public GlowPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GLOW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLOW);
    }

    /**
     * How brightly the cells glow for {@code generating} FE/t: its share of the best a panel can make (the larger of
     * {@code sunRate} and {@code lightRate}) in thirds, rounded up, so any power at all shows at least a dim glow.
     */
    public static int glowFor(int generating) {
        if (generating <= 0) return 0;
        int best = Math.max(ModConfigs.getInt(ModConfigs.SERVER.glowPanelSunRate), ModConfigs.getInt(ModConfigs.SERVER.glowPanelLightRate));
        if (best <= 0) return 3;
        return Math.clamp((generating * 3 + best - 1) / best, 1, 3);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlowPanelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.GLOW_PANEL.get(),
                (l, pos, s, be) -> GlowPanelBlockEntity.serverTick(serverLevel, pos, s, be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GlowPanelBlockEntity be) {
            player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.glow_panel." + be.source().name().toLowerCase(Locale.ROOT),
                    be.generating(), String.format(Locale.ROOT, "%,d", be.getEnergyHandler().getAmountAsInt()))
                    .withStyle(be.generating() > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }
}
