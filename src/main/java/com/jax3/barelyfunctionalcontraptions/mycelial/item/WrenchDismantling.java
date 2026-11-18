package com.jax3.barelyfunctionalcontraptions.mycelial.item;

import com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * Dismantling with the Barely Functional Wrench: hitting any of this mod's blocks with the wrench breaks it at once,
 * and the block and everything that spills out of it (a machine's fuel, wool or output) go straight into the player's
 * inventory. What doesn't fit drops as usual. Blocks that can't be broken (the Creative Generator) still can't be.
 * Server config: {@code [wrench] dismantle_blocks}.
 */
public final class WrenchDismantling {
    /** Faster than any block's hardness needs, so the first hit breaks it. */
    private static final float INSTANT = 1.0E6F;
    /** How far from the block's centre a spilled item may land and still be collected. */
    private static final double REACH = 1.5;

    /** The block being dismantled right now, and who by: its drops are collected while this is set. */
    private record Dismantling(ServerPlayer player, BlockPos pos, long gameTime) {}

    private static @Nullable Dismantling current;

    private WrenchDismantling() {}

    /** Whether {@code player} holds the wrench and may dismantle {@code state} with it. */
    public static boolean canDismantle(Player player, BlockState state) {
        if (!ServerConfig.get(ServerConfig.WRENCH_DISMANTLE) || !player.getMainHandItem().is(ModItems.MYCELIUM_WRENCH.get())) return false;
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(BarelyFunctionalContraptions.MOD_ID);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (canDismantle(event.getEntity(), event.getState())) event.setNewSpeed(INSTANT);
    }

    /** The wrench counts as the right tool, so machines that need a pickaxe still drop. */
    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (canDismantle(event.getEntity(), event.getTargetBlock())) event.setCanHarvest(true);
    }

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !player.getAbilities().instabuild && canDismantle(player, event.getState())) {
            current = new Dismantling(player, event.getPos(), player.level().getGameTime());
        }
    }

    /** Items spilling out of the block being dismantled go to the player instead of the ground. */
    @SubscribeEvent
    public static void onItemSpawn(EntityJoinLevelEvent event) {
        Dismantling d = current;
        if (d == null || !(event.getEntity() instanceof ItemEntity item) || event.getLevel() != d.player().level()) return;
        if (event.getLevel().getGameTime() != d.gameTime()) {
            current = null; // a tick later: that break is over
            return;
        }
        if (item.position().distanceTo(Vec3.atCenterOf(d.pos())) > REACH) return;
        ItemStack stack = item.getItem();
        int before = stack.getCount();
        d.player().getInventory().add(stack);
        if (stack.getCount() == before) return;
        d.player().level().playSound(null, d.player().getX(), d.player().getY(), d.player().getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.2F, (d.player().getRandom().nextFloat() - d.player().getRandom().nextFloat()) * 1.4F + 2.0F);
        if (stack.isEmpty()) event.setCanceled(true);
        else item.setItem(stack);
    }
}
