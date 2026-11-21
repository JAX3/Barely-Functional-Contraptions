package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;

/** Tint layer 0 of a Wool-Lined Crate: its wool lining, in the colour it's lined with. */
public enum CrateLiningTint implements BlockTintSource {
    INSTANCE;

    @Override
    public int color(BlockState state) {
        return ARGB.opaque(state.getValue(CrateBlock.LINING).getTextureDiffuseColor());
    }
}
