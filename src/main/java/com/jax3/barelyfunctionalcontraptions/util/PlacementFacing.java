package com.jax3.barelyfunctionalcontraptions.util;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Shared placement rule for every Barely Functional Contraptions block with a front (Mycelial Generator, Mechanical Separator,
 * Automatic Shears): the front faces the player who places it, or faces away from them while they hold Shift (sneak).
 */
public final class PlacementFacing {
    private PlacementFacing() {}

    /** The horizontal direction the block's front should face. */
    public static Direction horizontal(BlockPlaceContext context) {
        Direction towardsPlayer = context.getHorizontalDirection().getOpposite();
        Player player = context.getPlayer();
        return player != null && player.isSecondaryUseActive() ? towardsPlayer.getOpposite() : towardsPlayer;
    }
}
