package com.jax3.barelyfunctionalcontraptions.sheep.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** How a fluid looks: its still texture and tint, from the game's fluid models (so modded fluids work too). */
public final class FluidSprites {
    private FluidSprites() {}

    private static FluidModel model(Fluid fluid) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
    }

    /** The fluid's still texture on the block atlas. */
    public static TextureAtlasSprite still(Fluid fluid) {
        return model(fluid).stillMaterial().sprite();
    }

    /** The colour to tint the still texture with (opaque; white for untinted fluids like lava). */
    public static int tint(FluidStack stack) {
        var source = model(stack.getFluid()).fluidTintSource();
        return ARGB.opaque(source == null ? -1 : source.colorAsStack(stack));
    }

    /** Light the fluid gives off (lava glows), 0..15. */
    public static int lightLevel(FluidStack stack) {
        return stack.getFluid().getFluidType().getLightLevel(stack);
    }
}
