package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.State;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.StaticGeneratorMenu;
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
 * Window of the Static Generator. Left: the energy gauge. Right, the panel: the herd and the output, the dome's charge
 * bar (with the line past which it shocks things in its pen), and a chip for each of its Copper Combs, coloured by how
 * oxidised it is, with a sheen when it's waxed. Bottom: the status lamp and line. Top right: the show-pen button. Art:
 * {@code textures/gui/static_generator.png}, see {@code tools/sheep/generate_textures.py}.
 */
public class StaticGeneratorScreen extends AbstractContainerScreen<StaticGeneratorMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/static_generator.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64, GAUGE_U = 176, SHIMMER_V = 66;
    private static final int CHARGE_X = 51, CHARGE_Y = 43, CHARGE_W = 112, CHARGE_H = 8, CHARGE_V = 200;
    private static final int CHIP_X = 52, CHIP_Y = 58, CHIP_STEP = 28, CHIP_W = 22, CHIP_H = 18;
    private static final int CENTRE = 107;
    private static final int LAMP_X = 9, LAMP_Y = 90, LAMP_U = 186, LAMP_V = 194;
    private static final int AREA_X = 154, AREA_Y = 4, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFD27F, SPARK_TEXT = 0xFF8FD0FF;
    /** Fresh, exposed, weathered and oxidised copper. */
    private static final int[] COPPER = {0xFFD88A63, 0xFFA8826A, 0xFF6E9C80, 0xFF4FAE93};
    private static final String[] STAGES = {"fresh", "exposed", "weathered", "oxidized"};

    public StaticGeneratorScreen(StaticGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 108);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, StaticGeneratorBlockEntity.AREA_COLOR, menu.containerId, StaticGeneratorMenu.BUTTON_AREA));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;

        float fraction = menu.getEnergy() / (float) menu.getCapacity();
        int height = fraction > 0 ? Math.max(1, Mth.ceil(Math.min(1F, fraction) * GAUGE_H)) : 0;
        if (height > 0) {
            int offset = GAUGE_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, GAUGE_U, offset, GAUGE_W, height, TEX, TEX);
            if (height >= 2) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, GAUGE_U, SHIMMER_V + (int) (time / 4 % 4) * 2,
                        GAUGE_W, 2, TEX, TEX);
            }
        }

        // The dome's charge, flickering past the shock line.
        int width = Math.round(CHARGE_W * Math.min(1F, menu.getCharge() / (float) menu.getDomeCapacity()));
        boolean danger = menu.getState() == State.OVERCHARGED;
        if (width > 0 && !(danger && time % 6 < 2)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + CHARGE_X, y + CHARGE_Y, 0, CHARGE_V, width, CHARGE_H, TEX, TEX);
        }
        int line = x + CHARGE_X + Math.round(CHARGE_W * menu.getShockAt() / 1000F);
        graphics.fill(line, y + CHARGE_Y - 1, line + 1, y + CHARGE_Y + CHARGE_H + 1, 0xFFE35D5D);

        // A chip for each comb: its copper colour, a sheen when waxed.
        for (int i = 0; i < StaticGeneratorBlockEntity.MAX_SHOWN_COMBS; i++) {
            if (!menu.hasComb(i)) continue;
            int cx = x + CHIP_X + i * CHIP_STEP + 5, cy = y + CHIP_Y + 3;
            graphics.fill(cx, cy, cx + 12, cy + 12, COPPER[menu.getCombAge(i)]);
            graphics.fill(cx, cy, cx + 12, cy + 1, 0x60FFFFFF);
            if (menu.isCombWaxed(i)) {
                for (int k = 0; k < 6; k++) graphics.fill(cx + 2 + k, cy + 8 - k, cx + 3 + k, cy + 9 - k, 0xC0FFF4C0);
            }
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp(menu.getState()) * 8, LAMP_V, 8, 8, TEX, TEX);
    }

    /** The status lamp sprite for each state: grey, green, amber, red. */
    private static int lamp(State state) {
        return switch (state) {
            case NEEDS_COMBS, NEEDS_HERD -> 0;
            case CHARGING -> 1;
            case FULL -> 2;
            case OVERCHARGED -> 3;
        };
    }

    private Component statusText() {
        return Component.translatable("gui.barelyfunctionalcontraptions.static_generator.status." + menu.getState().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.static_generator.herd", menu.getHerd(), menu.getMaxSheep()), 20,
                menu.getHerd() > 0 ? SPARK_TEXT : TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.static_generator.rate", fe(menu.getGenerating())), 30,
                menu.getGenerating() > 0 ? ENERGY_TEXT : TEXT_DIM);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y, menu.getState().color, false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, CENTRE - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        String key = "gui.barelyfunctionalcontraptions.static_generator.";
        List<Component> lines = new ArrayList<>();
        if (isHovering(GAUGE_X - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "energy", fe(menu.getEnergy()), fe(menu.getCapacity())).withColor(ENERGY_TEXT));
            lines.add(Component.translatable(key + "energy.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(CENTRE - 50, 18, 100, 22, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "herd.title", menu.getHerd(), menu.getMaxSheep()).withColor(SPARK_TEXT));
            lines.add(Component.translatable(key + "herd.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(CHARGE_X - 1, CHARGE_Y - 1, CHARGE_W + 2, CHARGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "charge", fe(menu.getCharge()), fe(menu.getDomeCapacity())).withColor(SPARK_TEXT));
            lines.add(Component.translatable(key + "charge.desc", menu.getShockAt() / 10).withStyle(ChatFormatting.GRAY));
        } else if (mouseY >= topPos + CHIP_Y && mouseY < topPos + CHIP_Y + CHIP_H) {
            int i = (mouseX - leftPos - CHIP_X) / CHIP_STEP;
            int within = (mouseX - leftPos - CHIP_X) % CHIP_STEP;
            if (mouseX >= leftPos + CHIP_X && i < StaticGeneratorBlockEntity.MAX_SHOWN_COMBS && within < CHIP_W) {
                if (menu.hasComb(i)) {
                    int age = menu.getCombAge(i);
                    lines.add(Component.translatable(key + "comb", i + 1, Component.translatable(key + "comb." + STAGES[age])).withColor(COPPER[age]));
                    if (menu.isCombWaxed(i)) lines.add(Component.translatable(key + "comb.waxed").withStyle(ChatFormatting.YELLOW));
                    lines.add(Component.translatable(key + "comb.desc").withStyle(ChatFormatting.GRAY));
                } else {
                    lines.add(Component.translatable(key + "comb.none").withStyle(ChatFormatting.GRAY));
                }
            }
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 150, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(menu.getState().color));
            lines.add(Component.translatable(key + "status." + menu.getState().name().toLowerCase(Locale.ROOT) + ".desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(AREA_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isShowingArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static String fe(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
