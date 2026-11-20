package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Copper Comb: a copper post with stiff bristles for a Static Generator's sheep to rub against. Like vanilla copper it
 * has four oxidation stages ({@link #AGE}: fresh, exposed, weathered, oxidised), but it only ages while a Static
 * Generator is working it, and each stage gives less charge. An axe scrapes it back a stage (or takes its wax off);
 * Honeycomb waxes it, which stops it ageing for good but insulates it a little. It keeps its stage and wax when broken.
 */
public class CopperCombBlock extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    public static final BooleanProperty WAXED = BooleanProperty.create("waxed");

    private static final VoxelShape POST = Shapes.or(Block.box(6, 0, 6, 10, 14, 10), Block.box(5, 14, 5, 11, 16, 11));
    private static final VoxelShape OUTLINE = Shapes.or(POST, Block.box(1, 2, 7.5, 15, 14, 8.5), Block.box(7.5, 2, 1, 8.5, 14, 15));

    public CopperCombBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(WAXED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, WAXED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    /** Only the post is solid: sheep stand right up against it, in the bristles. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return POST;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        boolean waxed = state.getValue(WAXED);
        int age = state.getValue(AGE);
        if (stack.is(ItemTags.AXES) && (waxed || age > 0)) {
            // Wax off first, as for vanilla copper; then a stage of oxidation per scrape.
            BlockState scraped = waxed ? state.setValue(WAXED, false) : state.setValue(AGE, age - 1);
            // (By name: they're held differently across the Minecraft versions the mod builds for.)
            var sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(
                    net.minecraft.resources.Identifier.withDefaultNamespace(waxed ? "item.axe.wax_off" : "item.axe.scrape"));
            if (sound != null) level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, waxed ? 3004 : 3005, pos, 0);
            if (!level.isClientSide()) {
                level.setBlock(pos, scraped, Block.UPDATE_ALL_IMMEDIATE);
                stack.hurtAndBreak(1, player, hand);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.HONEYCOMB) && !waxed) {
            level.playSound(player, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.levelEvent(player, 3003, pos, 0);
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WAXED, true), Block.UPDATE_ALL_IMMEDIATE);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** One stage older, as a Static Generator wears it (waxed or fully oxidised combs don't change). */
    public static BlockState aged(BlockState state) {
        if (state.getValue(WAXED) || state.getValue(AGE) >= 3) return state;
        return state.setValue(AGE, state.getValue(AGE) + 1);
    }
}
