package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Wool-Lined Crate: what it holds out of how many (gold when locked), whether it's locked, its bales, its lining (dyed,
 * or matching its item), and
 * whether it's strapped or at risk of bursting. Everything comes from what the client already knows of it (its item,
 * count, lock and bales are synced for its front; its lining and strap are in its block state).
 */
public enum CrateProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("wool_lined_crate");
    private static final int TEXT = 0xF2E6D0, GOLD = 0xFFC94A, GREY = 0xAAAAAA, RED = 0xE35D5D;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof CrateBlockEntity be)) return;
        var state = accessor.getBlockState();
        String key = "jade.barelyfunctionalcontraptions.crate.";
        if (be.item().isEmpty()) {
            tooltip.add(Component.translatable(key + "empty", be.capacity()).withColor(GREY));
        } else {
            int percent = (int) (100L * be.count() / Math.max(1, be.capacity()));
            tooltip.add(Component.translatable(key + "holds", be.item().getHoverName(), String.format(Locale.ROOT, "%,d", be.count()),
                    String.format(Locale.ROOT, "%,d", be.capacity()), percent).withColor(be.locked() ? GOLD : TEXT));
        }
        if (be.locked()) tooltip.add(Component.translatable(key + "locked").withColor(GOLD));
        if (be.bales() > 0) tooltip.add(Component.translatable(key + "bales", be.bales(), CrateBlockEntity.maxBales(), be.stacks()).withColor(GREY));
        if (state.getValue(CrateBlock.DYED)) {
            tooltip.add(Component.translatable(key + "lining", Component.translatable("color.minecraft." + state.getValue(CrateBlock.LINING).getName()))
                    .withColor(lighter(state.getValue(CrateBlock.LINING).getTextureDiffuseColor())));
        } else if (!be.item().isEmpty()) {
            tooltip.add(Component.translatable(key + "lining_auto").withColor(
                    lighter(0xFFFFFF & com.jax3.barelyfunctionalcontraptions.sheep.client.CrateLiningTint.colourOf(be.item()))));
        }
        if (state.getValue(CrateBlock.TAGGED)) tooltip.add(Component.translatable(key + "in_store").withColor(0xC9923A));
        if (state.getValue(CrateBlock.STRAPPED)) {
            tooltip.add(Component.translatable(key + "strapped").withColor(GREY));
        } else if (be.isFull()) {
            tooltip.add(Component.translatable(key + "full").withColor(RED));
        } else if (state.getValue(CrateBlock.BULGING)) {
            tooltip.add(Component.translatable(key + "bulging").withColor(GOLD));
        }
    }

    /** {@code rgb} a little nearer white, so dark linings read on Jade's grey. */
    private static int lighter(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return ((r + (255 - r) * 2 / 5) << 16) | ((g + (255 - g) * 2 / 5) << 8) | (b + (255 - b) * 2 / 5);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
