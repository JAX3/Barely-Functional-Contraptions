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
                    int pixel = source.getPixel(x % bw, y % bh);
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

    /** The block's colour, darkened a little where the wool's fluff is in shadow, so it still looks woolly. */
    private static int shade(int pixel, int grey) {
        float f = 0.72F + 0.28F * grey / 255F;
        return ARGB.color(255, (int) (ARGB.red(pixel) * f), (int) (ARGB.green(pixel) * f), (int) (ARGB.blue(pixel) * f));
    }
}
