package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.util.Numbers;
import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
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
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.ChillerMenu;

/**
 * Window of the Snow Sheep Chiller. Left: the water gauge. Middle: the cold column, frost rising with the cold from
 * the Snow sheep, with a mark and pip for each one. Right: its speed and Snow sheep, a freezing arrow into the Ice slot,
 * and how long each Ice takes. Bottom: the status lamp and line. Top right: the show-area button. Art:
 * {@code textures/gui/chiller.png}, see {@code tools/sheep/generate_textures.py}.
 */
public class ChillerScreen extends AbstractContainerScreen<ChillerMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/chiller.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    private static final int COLUMN_X = 46, COLUMN_Y = 16, COLUMN_W = 30, COLUMN_H = 68;
    private static final int FROST_X = 48, FROST_BOTTOM = 83, FROST_W = 26, FROST_H = 64, FROST_U = 176, FROST_V = 80;
    private static final int ARROW_X = 104, ARROW_Y = 45, ARROW_W = 24, ARROW_H = 7, ARROW_U = 176, ARROW_V = 162;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, WATER_TEXT = 0xFF7FB8FF, COLD_TEXT = 0xFFBFE8FF;
    private static final int MARK_COLOR = 0xFF4A4E5A, PIP_ON = 0xFFBFE8FF;
    private static final int INFO_CENTRE = 124;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;
    private static final int AREA_X = 154, AREA_Y = 4, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;

    public ChillerScreen(ChillerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = ChillerMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, ChillerBlockEntity.AREA_COLOR, menu.containerId, ChillerMenu.BUTTON_AREA));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;

        // Water gauge.
        int height = menu.getWater() > 0 ? Math.max(1, Mth.ceil(menu.getWater() / (float) menu.getTankCapacity() * GAUGE_H)) : 0;
        if (height > 0) {
            int offset = GAUGE_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, FILL_V + offset, GAUGE_W, height, TEX, TEX);
            if (height >= 2) {
                int frame = (int) (time / 4 % 4);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, SHIMMER_V + frame * 2, GAUGE_W, 2, TEX, TEX);
            }
        }

        // Cold column: frost up to the current cold, a mark for each extra Snow sheep and a pip lit for each one in use.
        int frost = menu.getCold() > 0 ? Math.max(2, Mth.ceil(menu.getCold() / (float) menu.getMaxCold() * FROST_H)) : 0;
        if (frost > 0) {
            int frame = (int) (time / 6 % 3);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + FROST_X, y + FROST_BOTTOM - frost, FROST_U + frame * FROST_W, FROST_V, FROST_W, frost, TEX, TEX);
        }
        int max = menu.getMaxSources();
        for (int k = 1; k <= max; k++) {
            int level = ChillerBlockEntity.coldFrom(k);
            int my = y + FROST_BOTTOM - Math.round(FROST_H * Math.min(1F, level / (float) menu.getMaxCold()));
            boolean inFrost = FROST_BOTTOM - frost <= my - y;
            graphics.fill(x + FROST_X, my, x + FROST_X + FROST_W - 5, my + 1, inFrost ? 0x600F2A4A : MARK_COLOR);
            graphics.fill(x + FROST_X + FROST_W - 4, my - 1, x + FROST_X + FROST_W - 1, my + 2, 0xFF141519);
            graphics.fill(x + FROST_X + FROST_W - 3, my, x + FROST_X + FROST_W - 2, my + 1, k <= menu.getSources() ? PIP_ON : MARK_COLOR);
        }

        // Freezing arrow.
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V, ARROW_W, ARROW_H, TEX, TEX);
        int filled = Math.round(ARROW_W * Math.min(1F, menu.getProgress() / (float) menu.getTicksPerIce()));
        if (filled > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V + 10, filled, ARROW_H, TEX, TEX);

        MachineStatus lamp = menu.getStatus();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp.ordinal() * 8 % 64, LAMP_V + lamp.ordinal() / 8 * 8, 8, 8, TEX, TEX);
    }

    private Component statusText() {
        return Component.translatable("gui.barelyfunctionalcontraptions.chiller.status." + menu.getStatus().name().toLowerCase(Locale.ROOT));
    }

    /** Seconds per Ice at the current cold, or none without Snow sheep. */
    private String secondsPerIce() {
        if (menu.getCold() <= 0) return "-";
        return String.format(Locale.ROOT, "%.1f", menu.getTicksPerIce() * 100.0 / menu.getCold() / 20.0);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.chiller.sources", menu.getSources(), menu.getMaxSources()), 21,
                menu.getSources() > 0 ? COLD_TEXT : TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.chiller.speed", String.format(Locale.ROOT, "%.2f", menu.getCold() / 100.0)
                .replaceAll("0+$", "").replaceAll("\\.$", "")), 31, TEXT_DIM);
        graphics.text(font, Component.translatable("gui.barelyfunctionalcontraptions.chiller.per_ice", secondsPerIce()), 84, 66, TEXT_DIM, false);
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
            if (hoveredSlot.index == 0 && !hoveredSlot.hasItem()) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.chiller.slot"));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.chiller.slot.desc").withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.chiller.water", Numbers.grouped(menu.getWater()), Numbers.grouped(menu.getTankCapacity())).withColor(WATER_TEXT));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.chiller.water.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(COLUMN_X, COLUMN_Y, COLUMN_W, COLUMN_H, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.chiller.cold", menu.getSources(), menu.getMaxSources()).withColor(COLD_TEXT));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.chiller.cold.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(ARROW_X, ARROW_Y - 2, ARROW_W, ARROW_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.chiller.progress",
                    Math.round(100F * menu.getProgress() / menu.getTicksPerIce())));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 130, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(menu.getStatus().color));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.chiller.status." + menu.getStatus().name().toLowerCase(Locale.ROOT) + ".desc")
                    .withStyle(ChatFormatting.GRAY));
        } else if (isHovering(AREA_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isShowingArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

}
