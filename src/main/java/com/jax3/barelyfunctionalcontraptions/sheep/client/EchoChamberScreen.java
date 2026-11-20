package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.State;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.EchoChamberMenu;
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
 * Window of the Echo Chamber. Left: the energy gauge. Right, the listening panel: the herd and the output, the noise
 * meter (16 segments from silence to 15; amber is too quiet, green the sweet spot, red too loud, lit up to the noise
 * level now), the steadiness bar, and under it the drowsiness bar (towards its sheep falling asleep) and the shriek bar
 * (towards the room shrieking; after a shriek, the cooldown draining). Bottom: the status lamp and line. Top right: the
 * show-pen button. Art: {@code textures/gui/echo_chamber.png}, see {@code tools/sheep/generate_textures.py}.
 */
public class EchoChamberScreen extends AbstractContainerScreen<EchoChamberMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/echo_chamber.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64, GAUGE_U = 176, SHIMMER_V = 66;
    private static final int METER_X = 51, METER_Y = 42, SEGMENT = 7, SEGMENT_W = 6, SEGMENT_H = 8, SEGMENT_V = 200, LEVELS = 16;
    private static final int BAR_H = 3, STEADY_X = 51, STEADY_Y = 60, STEADY_W = 113;
    private static final int DROWSY_X = 51, SHRIEK_X = 110, METERS_Y = 71, METERS_W = 54;
    private static final int CENTRE = 107;
    private static final int LAMP_X = 9, LAMP_Y = 90, LAMP_U = 186, LAMP_V = 194;
    private static final int AREA_X = 154, AREA_Y = 4, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFD27F, SCULK_TEXT = 0xFF29DFEB,
            DROWSY_COLOR = 0xFF29DFEB, SHRIEK_COLOR = 0xFFE35D5D, COOLDOWN_COLOR = 0xFF8A2A30;

    public EchoChamberScreen(EchoChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 108);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, EchoChamberBlockEntity.AREA_COLOR, menu.containerId, EchoChamberMenu.BUTTON_AREA));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;

        // Energy.
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

        // The noise meter, lit up to the noise level; the top segment flickers, like a level meter.
        int noise = Mth.clamp(menu.getNoise(), 0, LEVELS - 1);
        for (int k = 1; k <= noise; k++) {
            if (k == noise && noise > 1 && time % 8 < 2) continue;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + METER_X + k * SEGMENT, y + METER_Y, k * SEGMENT, SEGMENT_V, SEGMENT_W, SEGMENT_H, TEX, TEX);
        }

        bar(graphics, x + STEADY_X, y + STEADY_Y, STEADY_W, menu.getSteadiness() / 100F, 0xFF55D66B);
        bar(graphics, x + DROWSY_X, y + METERS_Y, METERS_W, menu.getDrowsiness() / 1000F, DROWSY_COLOR);
        boolean cooling = menu.getState() == State.SHRIEKED;
        int shriek = menu.getShriek();
        boolean flash = !cooling && shriek > 700 && time % 10 < 5;
        bar(graphics, x + SHRIEK_X, y + METERS_Y, METERS_W, shriek / 1000F, cooling ? COOLDOWN_COLOR : flash ? 0xFFFFFFFF : SHRIEK_COLOR);

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp(menu.getState()) * 8, LAMP_V, 8, 8, TEX, TEX);
    }

    private static void bar(GuiGraphicsExtractor graphics, int bx, int by, int width, float fraction, int color) {
        int filled = Math.round(width * Mth.clamp(fraction, 0F, 1F));
        if (filled > 0) graphics.fill(bx, by, bx + filled, by + BAR_H, color);
    }

    /** The status lamp sprite for each state: grey, green, amber, red, purple. */
    private static int lamp(State state) {
        return switch (state) {
            case NEEDS_HERD -> 0;
            case LISTENING -> 1;
            case TOO_QUIET, TOO_LOUD, WAKING -> 2;
            case FULL -> 3;
            case SHRIEKED -> 4;
            case ASLEEP -> 5;
        };
    }

    private Component statusText() {
        String key = "gui.barelyfunctionalcontraptions.echo_chamber.status." + menu.getState().name().toLowerCase(Locale.ROOT);
        return menu.getState() == State.SHRIEKED ? Component.translatable(key, menu.getCooldownSeconds()) : Component.translatable(key);
    }

    private int statusColor() {
        return menu.getState().color;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.echo_chamber.herd", menu.getHerd(), menu.getMaxSheep()), 20,
                menu.getHerd() > 0 ? SCULK_TEXT : TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.echo_chamber.rate", fe(menu.getGenerating())), 30,
                menu.getGenerating() > 0 ? ENERGY_TEXT : TEXT_DIM);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y, statusColor(), false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, CENTRE - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        String key = "gui.barelyfunctionalcontraptions.echo_chamber.";
        List<Component> lines = new ArrayList<>();
        if (isHovering(GAUGE_X - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "energy", fe(menu.getEnergy()), fe(menu.getCapacity())).withColor(ENERGY_TEXT));
            lines.add(Component.translatable(key + "energy.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(CENTRE - 50, 18, 100, 22, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "herd.title", menu.getHerd(), menu.getMaxSheep()).withColor(SCULK_TEXT));
            lines.add(Component.translatable(key + "herd.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(METER_X - 1, METER_Y - 1, LEVELS * SEGMENT + 1, SEGMENT_H + 6, mouseX, mouseY)) {
            int noise = menu.getNoise();
            String zone = noise < menu.getQuietBelow() ? "quiet" : noise > menu.getLoudAbove() ? "loud" : "sweet";
            lines.add(Component.translatable(key + "noise", noise).withColor(SCULK_TEXT));
            lines.add(Component.translatable(key + "noise." + zone, menu.getQuietBelow(), menu.getLoudAbove()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(key + "noise.desc").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(STEADY_X - 1, STEADY_Y - 1, STEADY_W + 1, BAR_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "steady", menu.getSteadiness()).withColor(0xFF55D66B));
            lines.add(Component.translatable(key + "steady.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(DROWSY_X - 1, METERS_Y - 6, METERS_W + 1, BAR_H + 7, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "drowsy", menu.getDrowsiness() / 10).withColor(DROWSY_COLOR));
            lines.add(Component.translatable(key + "drowsy.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(SHRIEK_X - 1, METERS_Y - 6, METERS_W + 1, BAR_H + 7, mouseX, mouseY)) {
            if (menu.getState() == State.SHRIEKED) {
                lines.add(Component.translatable(key + "cooldown", menu.getCooldownSeconds()).withColor(SHRIEK_COLOR));
            } else {
                lines.add(Component.translatable(key + "shriek", menu.getShriek() / 10).withColor(SHRIEK_COLOR));
            }
            lines.add(Component.translatable(key + "shriek.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 150, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(statusColor()));
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
