package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity.Status;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.HumeFieldGeneratorMenu;
import com.jax3.barelyfunctionalcontraptions.util.Numbers;
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
 * Window of the Hume Field Generator. It has no buffer, so instead of an energy gauge the left gauge shows what its
 * field made this tick, against its range (the band, with the average marked). Right, the field panel: the output,
 * what was taken and what was lost, and the trace of its last ticks, each bar what it made (purple where it was taken,
 * red where it was lost), with the range's ends dashed across it. Bottom: the status lamp and line. Art:
 * {@code textures/gui/hume_field_generator.png}, see {@code tools/sheep/generate_textures.py}.
 */
public class HumeFieldGeneratorScreen extends AbstractContainerScreen<HumeFieldGeneratorMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/hume_field_generator.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64, FILL_U = 176, SHIMMER_V = 66;
    private static final int TRACE_X = 51, TRACE_Y = 42, TRACE_H = 29, BAR_W = 3, BAR_STEP = 4;
    private static final int CENTRE = 107, PANEL_L = 52, PANEL_R = 162;
    private static final int LAMP_X = 9, LAMP_Y = 90, LAMP_U = 186, LAMP_V = 194;
    public static final int FIELD = 0xFFC99BFF, TAKEN = 0xFF9A52F0, LOST = 0xFFB8262F, RANGE = 0xFF5C2AA6, AVERAGE = 0xFFFFD27F;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFD27F, LOST_TEXT = 0xFFE35D5D;

    public HumeFieldGeneratorScreen(HumeFieldGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 108);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
    }

    /** Pixels up a {@code height}-tall gauge for {@code value} on a scale to the range's top. */
    private int scaled(int value, int height) {
        return Math.round(height * Mth.clamp(value / (float) Math.max(1, menu.getRangeMax()), 0F, 1F));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;

        // The field gauge: the range as a band, this tick's output filled, the average marked.
        int bottom = y + GAUGE_Y + GAUGE_H;
        int lo = scaled(menu.getRangeMin(), GAUGE_H), hi = scaled(menu.getRangeMax(), GAUGE_H);
        graphics.fill(x + GAUGE_X, bottom - hi, x + GAUGE_X + GAUGE_W, bottom - lo, 0x403A1E66);
        int height = menu.getGenerated() > 0 ? Math.max(1, scaled(menu.getGenerated(), GAUGE_H)) : 0;
        if (height > 0) {
            int offset = GAUGE_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, offset, GAUGE_W, height, TEX, TEX);
            if (height >= 2) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, SHIMMER_V + (int) (time / 2 % 4) * 2,
                        GAUGE_W, 2, TEX, TEX);
            }
        }
        if (menu.getAverage() > 0) {
            int avg = bottom - scaled(menu.getAverage(), GAUGE_H);
            graphics.fill(x + GAUGE_X - 1, avg, x + GAUGE_X + 4, avg + 1, AVERAGE);
            graphics.fill(x + GAUGE_X + GAUGE_W - 4, avg, x + GAUGE_X + GAUGE_W + 1, avg + 1, AVERAGE);
        }

        // The trace: the range's ends dashed across, then a bar for each tick, newest on the right.
        int traceBottom = y + TRACE_Y + TRACE_H;
        for (int end : new int[]{menu.getRangeMin(), menu.getRangeMax()}) {
            int ly = traceBottom - Math.max(1, scaled(end, TRACE_H));
            for (int dx = 0; dx < HumeFieldGeneratorBlockEntity.HISTORY * BAR_STEP; dx += 4) {
                graphics.fill(x + TRACE_X + dx, ly, x + TRACE_X + dx + 2, ly + 1, RANGE);
            }
        }
        for (int i = 0; i < HumeFieldGeneratorBlockEntity.HISTORY; i++) {
            int made = Math.round(menu.sampleHeight(i) * TRACE_H);
            if (made <= 0) continue;
            int taken = Math.round(made * menu.sampleTaken(i));
            int bx = x + TRACE_X + i * BAR_STEP;
            if (taken > 0) graphics.fill(bx, traceBottom - taken, bx + BAR_W, traceBottom, i == HumeFieldGeneratorBlockEntity.HISTORY - 1 ? FIELD : TAKEN);
            if (made > taken) graphics.fill(bx, traceBottom - made, bx + BAR_W, traceBottom - taken, LOST);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp(menu.getStatus()) * 8, LAMP_V, 8, 8, TEX, TEX);
    }

    /** The status lamp sprite for each status: green, amber, red, grey. */
    private static int lamp(Status status) {
        return switch (status) {
            case DELIVERING -> 1;
            case WASTING -> 2;
            case NOTHING_CONNECTED -> 3;
            case IDLE -> 0;
        };
    }

    private Component statusText() {
        return Component.translatable("gui.barelyfunctionalcontraptions.hume_field_generator.status." + menu.getStatus().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String key = "gui.barelyfunctionalcontraptions.hume_field_generator.";
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        Component field = Component.translatable(key + "field", Numbers.grouped(menu.getGenerated()));
        graphics.text(font, field, CENTRE - font.width(field) / 2, 20, menu.getGenerated() > 0 ? FIELD : TEXT_DIM, false);
        graphics.text(font, Component.translatable(key + "taken", Numbers.grouped(menu.getDelivered())), PANEL_L, 30,
                menu.getDelivered() > 0 ? ENERGY_TEXT : TEXT_DIM, false);
        Component lost = Component.translatable(key + "lost", Numbers.grouped(menu.getLost()));
        graphics.text(font, lost, PANEL_R - font.width(lost), 30, menu.getLost() > 0 ? LOST_TEXT : TEXT_DIM, false);
        graphics.text(font, Component.translatable(key + "average", Numbers.grouped(menu.getAverage())), PANEL_L, 74, AVERAGE, false);
        Component range = Component.translatable(key + "range", Numbers.grouped(menu.getRangeMin()), Numbers.grouped(menu.getRangeMax()));
        graphics.text(font, range, PANEL_R - font.width(range), 74, TEXT_DIM, false);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y, menu.getStatus().color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        String key = "gui.barelyfunctionalcontraptions.hume_field_generator.";
        List<Component> lines = new ArrayList<>();
        if (isHovering(GAUGE_X - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "field", Numbers.grouped(menu.getGenerated())).withColor(FIELD));
            lines.add(Component.translatable(key + "gauge.range", Numbers.grouped(menu.getRangeMin()), Numbers.grouped(menu.getRangeMax()))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(key + "gauge.average", Numbers.grouped(menu.getAverage())).withColor(AVERAGE));
            lines.add(Component.translatable(key + "gauge.desc").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(PANEL_L - 2, 18, PANEL_R - PANEL_L + 4, 20, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "taken.title", Numbers.grouped(menu.getDelivered()), Numbers.grouped(menu.getGenerated())).withColor(ENERGY_TEXT));
            if (menu.getLost() > 0) lines.add(Component.translatable(key + "lost.title", Numbers.grouped(menu.getLost())).withColor(LOST_TEXT));
            lines.add(Component.translatable(key + "buffer.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(TRACE_X - 1, TRACE_Y - 1, HumeFieldGeneratorBlockEntity.HISTORY * BAR_STEP + 1, TRACE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "trace", HumeFieldGeneratorBlockEntity.HISTORY).withColor(FIELD));
            lines.add(Component.translatable(key + "trace.taken").withColor(TAKEN));
            lines.add(Component.translatable(key + "trace.lost").withColor(LOST_TEXT));
            lines.add(Component.translatable(key + "trace.range").withColor(0xFF9A7ACF));
        } else if (isHovering(PANEL_L - 2, 72, PANEL_R - PANEL_L + 4, 11, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "gauge.average", Numbers.grouped(menu.getAverage())).withColor(AVERAGE));
            lines.add(Component.translatable(key + "gauge.range", Numbers.grouped(menu.getRangeMin()), Numbers.grouped(menu.getRangeMax()))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(key + "range.desc").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 150, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(menu.getStatus().color));
            lines.add(Component.translatable(key + "status." + menu.getStatus().name().toLowerCase(Locale.ROOT) + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(key + "receivers", menu.getReceivers()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
