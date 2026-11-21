package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tint layer 0 of a Wool-Lined Crate: its wool lining. Dyed by hand, the dye's colour; otherwise the colour of the item
 * it holds (the average of the item's texture, brightened to show on wool), and plain white while it's empty.
 */
public enum CrateLiningTint implements BlockTintSource {
    INSTANCE;

    private static final int PLAIN = 0xFFF2F2F2;
    /** Each item's colour, worked out once (its texture doesn't change until resources reload). */
    private static final Map<Item, Integer> COLOURS = new ConcurrentHashMap<>();

    @Override
    public int color(BlockState state) {
        return state.getValue(CrateBlock.DYED) ? ARGB.opaque(state.getValue(CrateBlock.LINING).getTextureDiffuseColor()) : PLAIN;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (!state.getValue(CrateBlock.DYED) && level.getBlockEntity(pos) instanceof CrateBlockEntity be && !be.item().isEmpty()) {
            Integer known = COLOURS.get(be.item().getItem());
            if (known != null) return known;
            // Chunks are built off the main thread: work the colour out on it, then redraw the crate.
            ItemStack stack = be.item().copy();
            BlockPos at = pos.immutable();
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                colourOf(stack);
                if (mc.level != null) mc.level.sendBlockUpdated(at, mc.level.getBlockState(at), mc.level.getBlockState(at), 8);
            });
            return PLAIN;
        }
        return color(state);
    }

    /**
     * The colour a crate holding {@code stack} is lined: its texture's average colour, lifted so dark items still show.
     * Main thread only.
     */
    public static int colourOf(ItemStack stack) {
        return COLOURS.computeIfAbsent(stack.getItem(), item -> compute(stack));
    }

    /** Forgets the worked-out colours (textures may have changed). */
    public static void clear() {
        COLOURS.clear();
    }

    private static int compute(ItemStack stack) {
        try {
            ItemStackRenderState render = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(render, stack, ItemDisplayContext.GUI, null, null, 0);
            var material = render.pickParticleMaterial(RandomSource.create(0));
            if (material == null) return PLAIN;
            var image = material.sprite().contents().getOriginalImage();
            int w = material.sprite().contents().width(), h = material.sprite().contents().height();
            long r = 0, g = 0, b = 0, weight = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = image.getPixel(x, y);
                    int a = ARGB.alpha(argb);
                    if (a < 128) continue;
                    // Saturated pixels count more: a diamond is its cyan, not its white glints and dark outline.
                    int pr = ARGB.red(argb), pg = ARGB.green(argb), pb = ARGB.blue(argb);
                    int max = Math.max(pr, Math.max(pg, pb)), min = Math.min(pr, Math.min(pg, pb));
                    int wgt = 1 + (max - min) / 16;
                    r += (long) pr * wgt;
                    g += (long) pg * wgt;
                    b += (long) pb * wgt;
                    weight += wgt;
                }
            }
            if (weight == 0) return PLAIN;
            int cr = (int) (r / weight), cg = (int) (g / weight), cb = (int) (b / weight);
            // A little more colourful than the average, which is always duller than the item looks.
            int grey = (cr + cg + cb) / 3;
            cr = Math.clamp(grey + (cr - grey) * 3 / 2, 0, 255);
            cg = Math.clamp(grey + (cg - grey) * 3 / 2, 0, 255);
            cb = Math.clamp(grey + (cb - grey) * 3 / 2, 0, 255);
            // Lift dark colours toward white a little so they still read as a colour on the wool.
            int lum = (cr * 3 + cg * 6 + cb) / 10;
            if (lum < 90) {
                float t = (90 - lum) / 180F;
                cr += (int) ((255 - cr) * t);
                cg += (int) ((255 - cg) * t);
                cb += (int) ((255 - cb) * t);
            }
            return ARGB.color(255, cr, cg, cb);
        } catch (RuntimeException e) {
            return PLAIN;
        }
    }
}
