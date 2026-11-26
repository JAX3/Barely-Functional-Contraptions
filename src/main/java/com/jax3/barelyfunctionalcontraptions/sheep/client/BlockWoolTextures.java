package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Fleeces made of their resource's block: for each sheep resource, a sheep wool texture (the vanilla fleece layout,
 * 64x32) filled with the texture of the block it grows ({@link SheepResource#growthStack}: a Redstone sheep's fleece is
 * Block of Redstone, a Wood sheep's is oak bark), shaded by the infused wool's own fluff so it still reads as wool.
 * Built on the client the first time a sheep of that resource is drawn, from whatever textures are loaded, so data-pack
 * resources, modded blocks and resource packs all work. Where the block's texture is see-through (a cobweb), the wool
 * shows through in the resource's colour.
 */
public final class BlockWoolTextures {
    private static final Identifier WOOL_TEXTURE = ResourcefulSheep.id("textures/entity/sheep/infused_wool.png");
    /** Each resource's fleece texture, or empty if its block has no texture to use. */
    private static final Map<Identifier, Optional<Identifier>> TEXTURES = new HashMap<>();

    private BlockWoolTextures() {}

    /** The fleece texture for {@code resource}, made the first time it's asked for (render thread only). */
    public static Optional<Identifier> textureFor(SheepResource resource) {
        return TEXTURES.computeIfAbsent(resource.id(), id -> Optional.ofNullable(build(resource)));
    }

    /** Forgets (and frees) every fleece made, so they're made again from the textures loaded next. */
    public static void clear() {
        var manager = Minecraft.getInstance().getTextureManager();
        TEXTURES.values().forEach(t -> t.ifPresent(manager::release));
        TEXTURES.clear();
    }

    private static @Nullable Identifier build(SheepResource resource) {
        ItemStack block = resource.growthStack();
        if (block.isEmpty()) return null;
        Minecraft mc = Minecraft.getInstance();
        try (InputStream in = mc.getResourceManager().open(WOOL_TEXTURE)) {
            NativeImage wool = NativeImage.read(in);
            ItemStackRenderState render = new ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(render, block, ItemDisplayContext.GUI, null, null, 0);
            var material = render.pickParticleMaterial(RandomSource.create(0));
            if (material == null) {
                wool.close();
                return null;
            }
            var contents = material.sprite().contents();
            NativeImage source = contents.getOriginalImage();
            int bw = contents.width(), bh = contents.height(); // the first frame of an animated texture
            int base = ARGB.opaque(resource.woolColor());
            // A themed sheep that asked for its block ("block_wool", the Explosive sheep's TNT) gets the block laid
            // out face by face, so each side shows the whole block face the right way up; the others tile it.
            int[][] fitted = resource.appearance().blockWool() ? fittedMap(wool.getWidth(), wool.getHeight(), bw, bh) : null;
            NativeImage out = new NativeImage(wool.getWidth(), wool.getHeight(), true);
            for (int y = 0; y < wool.getHeight(); y++) {
                for (int x = 0; x < wool.getWidth(); x++) {
                    int fluff = wool.getPixel(x, y);
                    int alpha = ARGB.alpha(fluff);
                    if (alpha == 0) {
                        out.setPixel(x, y, 0);
                        continue;
                    }
                    int grey = ARGB.red(fluff);
                    int sx = x % bw, sy = y % bh;
                    if (fitted != null && fitted[y * wool.getWidth() + x] != null) {
                        sx = fitted[y * wool.getWidth() + x][0];
                        sy = fitted[y * wool.getWidth() + x][1];
                    }
                    int pixel = source.getPixel(sx, sy);
                    // A see-through bit of the block (a cobweb's gaps): plain wool in the resource's colour.
                    int colour = ARGB.alpha(pixel) < 128 ? ARGB.multiply(base, ARGB.opaque(ARGB.color(grey, grey, grey)))
                            : shade(pixel, grey);
                    out.setPixel(x, y, ARGB.color(alpha, ARGB.red(colour), ARGB.green(colour), ARGB.blue(colour)));
                }
            }
            wool.close();
            Identifier id = ResourcefulSheep.id("dynamic/block_wool/" + resource.id().getNamespace() + "/" + resource.id().getPath().toLowerCase(Locale.ROOT));
            mc.getTextureManager().register(id, new DynamicTexture(id::toString, out));
            return id;
        } catch (Exception e) {
            ResourcefulSheep.LOGGER.warn("Couldn't make a block fleece for {}: {}", resource.id(), e.toString());
            return null;
        }
    }

    /**
     * For each pixel of the fleece texture (the vanilla sheep wool layout), where on the block's texture it shows when
     * the block is fitted to each face of the fleece: the head and legs show the block upright on each face; the body,
     * which the model lies along its length, shows it turned so it runs along the sheep (on a TNT fleece, the white
     * band runs down each flank and along its back). Null for pixels outside every face.
     */
    private static int[][] fittedMap(int width, int height, int bw, int bh) {
        int[][] map = new int[width * height][];
        // Head (texOffs 0,0; 6x6x6) and legs (texOffs 0,16; 4x6x4): every face upright.
        fitBox(map, width, height, bw, bh, 0, 0, 6, 6, 6, false);
        fitBox(map, width, height, bw, bh, 0, 16, 4, 6, 4, false);
        // Body (texOffs 28,8; 8x16x6, turned to lie along the sheep): its long faces run along the sheep.
        fitBox(map, width, height, bw, bh, 28, 8, 8, 16, 6, true);
        return map;
    }

    /**
     * Where pixel {@code i} of a face {@code size} pixels across lands on a block texture {@code texture} pixels
     * across: pixel for pixel from the block's middle where the face is smaller (so a TNT flank shows the white band and
     * its letters whole, not squashed), stretched where it's bigger.
     */
    private static int fit(int i, int size, int texture) {
        return size <= texture ? (texture - size) / 2 + i : i * texture / size;
    }

    /** Like {@link #fit}, but the face shows a {@code slice} of the texture's middle, squashed or stretched to fit. */
    private static int span(int i, int size, int texture, int slice) {
        return (texture - slice) / 2 + i * slice / size;
    }

    /** Fits the block's texture to each face of a model cube at {@code (u, v)}, {@code w x h x d}. */
    private static void fitBox(int[][] map, int width, int height, int bw, int bh, int u, int v, int w, int h, int d, boolean lying) {
        // {x0, y0, x1, y1, turned}: top, bottom, then the four sides (east, north, west, south).
        int[][] faces = {
                {u + d, v, u + d + w, v + d, 0}, {u + d + w, v, u + d + 2 * w, v + d, 0},
                {u, v + d, u + d, v + d + h, lying ? 1 : 0}, {u + d, v + d, u + d + w, v + d + h, lying ? 1 : 0},
                {u + d + w, v + d, u + 2 * d + w, v + d + h, lying ? 1 : 0}, {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h, lying ? 1 : 0}};
        for (int fi = 0; fi < faces.length; fi++) {
            int[] f = faces[fi];
            int fw = f[2] - f[0], fh = f[3] - f[1];
            // The west flank is drawn mirrored on the model, so it's flipped back to read the right way round.
            boolean flip = lying && fi == 4;
            for (int y = f[1]; y < f[3] && y < height; y++) {
                for (int x = f[0]; x < f[2] && x < width; x++) {
                    int lx = x - f[0], ly = y - f[1];
                    int sx, sy;
                    if (f[4] == 1) {
                        // Turned: the face's length (down the texture) is across the block, its depth up the block.
                        sx = fit(flip ? fh - 1 - ly : ly, fh, bw);
                        // Across the sheep, a slice a little taller than the face (10 of 16 rows on a TNT flank): the
                        // band and its letters with some of the red either side, only slightly squashed.
                        sy = span(lx, fw, bh, Math.min(bh, fw * 5 / 3));
                    } else {
                        sx = fit(lx, fw, bw);
                        sy = fit(ly, fh, bh);
                    }
                    map[y * width + x] = new int[]{Math.min(bw - 1, sx), Math.min(bh - 1, sy)};
                }
            }
        }
    }

    /** The block's colour, darkened a little where the wool's fluff is in shadow, so it still looks woolly. */
    private static int shade(int pixel, int grey) {
        float f = 0.72F + 0.28F * grey / 255F;
        return ARGB.color(255, (int) (ARGB.red(pixel) * f), (int) (ARGB.green(pixel) * f), (int) (ARGB.blue(pixel) * f));
    }
}
