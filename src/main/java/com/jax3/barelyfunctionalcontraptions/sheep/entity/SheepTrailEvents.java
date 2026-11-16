package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Trails: a sheep infused with a resource that has a {@code trail_block} (the Snow Sheep's snow) leaves that block
 * where it walks, as a Snow Golem leaves snow. Only while its fleece is on, only into air the block can stand in, and
 * only while mobs may change blocks (the mobGriefing game rule).
 */
public final class SheepTrailEvents {
    private SheepTrailEvents() {}

    @SubscribeEvent
    public static void onSheepTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Sheep sheep) || !(sheep.level() instanceof ServerLevel level)) return;
        // Only when it has moved since last tick, so a sheep standing still doesn't need a lookup.
        if (sheep.getX() == sheep.xo && sheep.getZ() == sheep.zo) return;
        if (sheep.isSheared() || !sheep.isAlive()) return;
        Optional<Block> trail = SheepInfusionEvents.getInfusedResource(sheep).flatMap(SheepResource::trailBlock);
        if (trail.isEmpty() || !net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, sheep)) return;
        BlockPos pos = sheep.blockPosition();
        if (place(level, pos, trail.get().defaultBlockState())) {
            level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE, pos,
                    net.minecraft.world.level.gameevent.GameEvent.Context.of(sheep, level.getBlockState(pos)));
        }
    }

    /** Puts {@code state} at {@code pos} if it's air there and the block can stand on what's below. */
    static boolean place(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos)) return false;
        level.setBlockAndUpdate(pos, state);
        return true;
    }
}
