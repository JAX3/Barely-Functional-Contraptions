package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBaleBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;

/** Tint layer 0 of a placed Wool Bale: the wool colour of the resource it is made of. */
public enum WoolBaleTint implements BlockTintSource {
    INSTANCE;

    private static final int BLANK = 0xFFE6E6E6;

    @Override
    public int color(BlockState state) {
        return BLANK;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof WoolBaleBlockEntity be) {
            return SheepResourceRegistry.client().byId(be.resource()).map(r -> ARGB.opaque(r.woolColor())).orElse(BLANK);
        }
        return BLANK;
    }
}
