package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * For blocks whose item keeps what they hold (Hume Energy Cube, Fluid Tank). In survival their loot table already
 * copies it onto the dropped item; in creative nothing drops, so, like a shulker box, a block that still holds
 * something drops itself with it instead of losing it.
 */
final class KeepsContents {
    private KeepsContents() {}

    static void dropInCreative(Level level, BlockPos pos, BlockState state, Player player, boolean holdsSomething) {
        if (level.isClientSide() || !player.preventsBlockDrops() || !holdsSomething) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;
        ItemStack stack = new ItemStack(state.getBlock());
        stack.applyComponents(be.collectComponents());
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}
