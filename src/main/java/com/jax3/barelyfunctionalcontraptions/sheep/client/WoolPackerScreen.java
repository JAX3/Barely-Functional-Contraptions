package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.WoolPackerMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Window of the Wool Packer, in the slate style of the other machines. Left: the FE buffer. Middle: the wool slot, a
 * 3x3 of pips that light up as the 9 wool for a bale come in, the press arrow and the bale slot. Right of the pips:
 * FE/t and the time per bale. Bottom: a status lamp and line. Art: {@code textures/gui/wool_packer.png}, see
 * {@code tools/sheep/generate_textures.py}.
 */
public class WoolPackerScreen extends AbstractContainerScreen<WoolPackerMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/wool_packer.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    private static final int ARROW_X = 86, ARROW_Y = 31, ARROW_W = 32, ARROW_H = 16, ARROW_U = 176, ARROW_V = 80;
    /** The 9 pips, 5x5 each on a 7-pixel grid. */
    public static final int PIPS_X = 61, PIPS_Y = 53, PIP_STEP = 7, PIP_SIZE = 5, PIP_U = 176, PIP_V = 100;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, USE_COLOR = 0xFFC27CFF, TIME_COLOR = 0xFFFFD25A;
    private static final int INFO_CENTRE = 128;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;

    public WoolPackerScreen(WoolPackerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = WoolPackerMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;

        int height = menu.getEnergy() > 0 ? Math.max(1, Mth.ceil(menu.getEnergyFraction() * GAUGE_H)) : 0;
        if (height > 0) {
            int offset = GAUGE_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, FILL_V + offset, GAUGE_W, height, TEX, TEX);
            if (height >= 2) {
                int frame = (int) (time / 3 % 4);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, SHIMMER_V + frame * 2, GAUGE_W, 2, TEX, TEX);
            }
        }

        int arrow = Mth.ceil(menu.getProgressFraction() * ARROW_W);
        if (arrow > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V, arrow, ARROW_H, TEX, TEX);

        int lit = litPips();
        for (int i = 0; i < lit; i++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + PIPS_X + i % 3 * PIP_STEP, y + PIPS_Y + i / 3 * PIP_STEP,
                    PIP_U, PIP_V, PIP_SIZE, PIP_SIZE, TEX, TEX);
        }

        MachineStatus lamp = menu.getStatus();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp.ordinal() * 8 % 64, LAMP_V + lamp.ordinal() / 8 * 8, 8, 8, TEX, TEX);
    }

    /** Wool towards the next bale, 0-9. */
    private int litPips() {
        ItemStack input = menu.getSlot(WoolPackerBlockEntity.INPUT).getItem();
        if (input.isEmpty()) return 0;
        return Math.min(WoolBaleItem.WOOL_PER_BALE, input.getCount());
    }

    private Component statusText() {
        return Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.status." + menu.getStatus().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        boolean working = menu.getStatus() == MachineStatus.PROCESSING;
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.uses", fe(menu.getEnergyPerTick())),
                56, working ? USE_COLOR : TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.time", seconds(menu.getMaxProgress())),
                68, working ? TIME_COLOR : TEXT_DIM);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y + 1, menu.getStatus().color, false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, INFO_CENTRE - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        List<Component> lines = new ArrayList<>();
        if (hoveredSlot != null) {
            if (hoveredSlot.index == WoolPackerBlockEntity.INPUT && !hoveredSlot.hasItem()) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.input"));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.input.desc").withStyle(ChatFormatting.GRAY));
            } else if (hoveredSlot.index == WoolPackerBlockEntity.OUTPUT && !hoveredSlot.hasItem()) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.output"));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.output.desc").withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getEnergy()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.energy.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(PIPS_X - 2, PIPS_Y - 2, PIP_STEP * 3 + 2, PIP_STEP * 3 + 2, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.pips", litPips(), WoolBaleItem.WOOL_PER_BALE));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.pips.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(ARROW_X, ARROW_Y, ARROW_W, ARROW_H, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.progress",
                    Math.round(menu.getProgressFraction() * 100)));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.progress.desc",
                    fe((long) menu.getEnergyPerTick() * menu.getMaxProgress())).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 130, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(menu.getStatus().color));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_packer.status." + menu.getStatus().name().toLowerCase(Locale.ROOT) + ".desc")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    /** "2" for 40 ticks, "2.5" for 50. */
    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
