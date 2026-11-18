package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * A sheep held on a running Sheep Treadmill doesn't move, so its legs wouldn't either: this walks them, after the
 * sheep's own animation update each tick, as if it were walking at a steady pace.
 */
public final class TreadmillWalkAnimation {
    private TreadmillWalkAnimation() {}

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Sheep sheep) || !sheep.level().isClientSide()) return;
        BlockPos pos = sheep.blockPosition();
        BlockState state = sheep.level().getBlockState(pos);
        if (!(state.getBlock() instanceof SheepTreadmillBlock)) return;
        BlockState back = sheep.level().getBlockState(SheepTreadmillBlock.back(pos, state));
        if (!(back.getBlock() instanceof SheepTreadmillBlock) || !back.getValue(SheepTreadmillBlock.RUNNING)) return;
        sheep.walkAnimation.update(0.6F, 1.0F, 1.0F);
    }
}
