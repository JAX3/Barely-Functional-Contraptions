package com.jax3.barelyfunctionalcontraptions.sheep.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The Contraptions Manual: right-click to open the in-game guide. The screen is client-only, so the client sets
 * {@link #openGuide} when it starts and this class never touches client code itself (dedicated servers stay safe).
 */
public class GuideBookItem extends Item {
    /** Opens the guide screen; set by the client at start-up, a no-op on a dedicated server. */
    public static Runnable openGuide = () -> {};

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) openGuide.run();
        return InteractionResult.SUCCESS;
    }
}
