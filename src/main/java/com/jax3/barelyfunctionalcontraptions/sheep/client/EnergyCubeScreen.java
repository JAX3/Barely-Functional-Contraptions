package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CubeStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.EnergyCubeMenu;
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

/**
 * GUI of the Hume Energy Cube, in the slate style of the other machines. Art: {@code textures/gui/hume_energy_cube.png}
 * (256x256): the 176x193 background, the energy fill at (176,0), its surface shimmer at (176,66) and the shared status
 * lamps at (186,194); see {@code tools/sheep/generate_textures.py}.
 */
public class EnergyCubeScreen extends AbstractContainerScreen<EnergyCubeMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/hume_energy_cube.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFF9AF0;
    private static final int IN_COLOR = 0xFF55D66B, OUT_COLOR = 0xFFE8B23A;
    private static final int INFO_CENTRE = 108;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;

    public EnergyCubeScreen(EnergyCubeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = EnergyCubeMenu.INVENTORY_Y - 11;
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
        int height = menu.getAmount() > 0 ? Math.max(1, Mth.ceil(menu.getFillFraction() * GAUGE_H)) : 0;
        if (height > 0) {
            int offset = GAUGE_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, FILL_V + offset, GAUGE_W, height, TEX, TEX);
            if (height >= 2 && minecraft != null && minecraft.level != null) {
                int frame = (int) (minecraft.level.getGameTime() / 3 % 4);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, SHIMMER_V + frame * 2, GAUGE_W, 2, TEX, TEX);
            }
        }
        int lamp = menu.getStatus().lamp.ordinal();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp * 8 % 64, LAMP_V + lamp / 8 * 8, 8, 8, TEX, TEX);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.stored", fe(menu.getAmount())), 21, ENERGY_TEXT);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.capacity", fe(menu.getCapacity())), 31, TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.group", menu.getCubes(), fe(menu.getRate())), 44, TEXT_COLOR);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.in", fe(menu.getInRate())), 57, IN_COLOR);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.out", fe(menu.getOutRate())), 67, OUT_COLOR);
        CubeStatus status = menu.getStatus();
        graphics.text(font, statusName(status), LAMP_X + 13, LAMP_Y + 1, status.color(), false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, INFO_CENTRE - font.width(text) / 2, y, color, false);
    }

    private static Component statusName(CubeStatus status) {
        return Component.translatable("gui.barelyfunctionalcontraptions.cube.status." + status.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty() || hoveredSlot != null) return;
        List<Component> lines = new ArrayList<>();
        if (isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getAmount()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.cube.percent",
                    String.format(Locale.ROOT, "%.1f", menu.getFillFraction() * 100)).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(48, 40, 120, 16, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.cube.group.tooltip", menu.getCubes()));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.cube.group.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(48, 54, 120, 22, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.cube.flow.tooltip"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.cube.flow.desc", fe(menu.getRate())).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 120, 10, mouseX, mouseY)) {
            CubeStatus status = menu.getStatus();
            lines.add(statusName(status).copy().withColor(status.color()));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.cube.status." + status.name().toLowerCase(Locale.ROOT) + ".desc")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
