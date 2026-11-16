package com.jax3.barelyfunctionalcontraptions.sheep.client;

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
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.PoweredFurnaceMenu;

/**
 * Window of the Powered Furnace. Left: the energy gauge. Middle: the input slot with a flame under it that lights while
 * Blaze sheep heat it, the smelting arrow and the output slot. Below: its speed and Blaze sheep, and the power it uses.
 * Bottom: the status lamp and line. Top right: the show-area button. Art: {@code textures/gui/powered_furnace.png}, see
 * {@code tools/sheep/generate_textures.py}.
 */
public class PoweredFurnaceScreen extends AbstractContainerScreen<PoweredFurnaceMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/powered_furnace.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    private static final int ARROW_X = 82, ARROW_Y = 31, ARROW_W = 32, ARROW_H = 16, ARROW_U = 176, ARROW_V = 80;
    private static final int FLAME_X = 57, FLAME_Y = 42, FLAME_SIZE = 14, FLAME_U = 176, FLAME_V = 100;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFB347, BLAZE_TEXT = 0xFFFFD25A;
    private static final int INFO_CENTRE = 107;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;
    private static final int AREA_X = 154, AREA_Y = 4, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;

    public PoweredFurnaceScreen(PoweredFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = PoweredFurnaceMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, PoweredFurnaceBlockEntity.AREA_COLOR, menu.containerId, PoweredFurnaceMenu.BUTTON_AREA));
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
        // The flame lights while Blaze sheep heat it.
        if (menu.getHeaters() > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + FLAME_X, y + FLAME_Y, FLAME_U, FLAME_V, FLAME_SIZE, FLAME_SIZE, TEX, TEX);
        }
        int arrow = Math.round(ARROW_W * Math.min(1F, menu.getProgress() / (float) menu.getMaxProgress()));
        if (arrow > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V, arrow, ARROW_H, TEX, TEX);
        MachineStatus lamp = menu.getStatus();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp.ordinal() * 8 % 64, LAMP_V + lamp.ordinal() / 8 * 8, 8, 8, TEX, TEX);
    }

    private static String times(int hundredths) {
        return String.format(Locale.ROOT, "%.2f", hundredths / 100.0).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.powered_furnace.speed", times(menu.getSpeed())), 63,
                menu.getHeaters() > 0 ? BLAZE_TEXT : TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.powered_furnace.sources", menu.getHeaters(), menu.getMaxHeaters()), 73, TEXT_DIM);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y + 1, menu.getStatus().color, false);
    }

    private String statusKey() {
        return "gui.barelyfunctionalcontraptions.powered_furnace.status." + menu.getStatus().name().toLowerCase(Locale.ROOT);
    }

    private Component statusText() {
        return Component.translatable(statusKey());
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
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.powered_furnace.input"));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.powered_furnace.input.desc", menu.getCookPercent()).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getEnergy()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.powered_furnace.energy.desc", menu.getEnergyPerTick()).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(FLAME_X, FLAME_Y, FLAME_SIZE, FLAME_SIZE, mouseX, mouseY) || isHovering(46, 60, 122, 24, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.powered_furnace.blaze", menu.getHeaters(), menu.getMaxHeaters()).withColor(BLAZE_TEXT));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.powered_furnace.blaze.desc", times(menu.getSpeed())).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(ARROW_X, ARROW_Y, ARROW_W, ARROW_H, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.progress", Math.round(100F * menu.getProgress() / menu.getMaxProgress())));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 130, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(menu.getStatus().color));
            lines.add(Component.translatable(statusKey() + ".desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(AREA_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isShowingArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
