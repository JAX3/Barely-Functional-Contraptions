package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A Wool-Lined Crate's clicks, ahead of the item in hand: so sneaking with dyed wool or a Wool Bale works on the crate
 * instead of placing the wool or bale (vanilla skips a block's own click handling while sneaking with an item), and a
 * left-click on its front takes items out instead of starting to break it.
 */
public final class CrateEvents {
    private CrateEvents() {}

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof CrateBlock)) return;
        InteractionResult result = CrateBlock.use(event.getLevel(), event.getPos(), state, event.getEntity(), event.getHand(),
                event.getHitVec().getDirection());
        if (result == InteractionResult.PASS) return;
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    /**
     * Its front, with something in it: takes a stack (sneaking, one) rather than breaking it. Breaking it is from any
     * other side, from the front once it's empty, or with the wrench (which picks it up whole).
     */
    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof CrateBlock) || event.getFace() != state.getValue(CrateBlock.FACING)) return;
        if (event.getEntity().getMainHandItem().is(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get())) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CrateBlockEntity be) || be.count() <= 0) return;
        event.setCanceled(true);
        if (event.getLevel() instanceof ServerLevel level) be.takeFor(level, event.getEntity(), event.getEntity().isShiftKeyDown());
    }
}
