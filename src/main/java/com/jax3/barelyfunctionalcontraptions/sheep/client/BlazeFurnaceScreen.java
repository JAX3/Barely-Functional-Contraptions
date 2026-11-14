package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.BlazeFurnaceMenu;
import java.util.ArrayList;
import java.util.Collections;
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
 * Window of the Blaze Sheep Furnace, laid out like the Zero Point Energy Module's. Left: the FE buffer. Middle: the
 * heat column, fire rising with the heat from the sheep, with a mark for each extra heat source and a pip lit for each
 * one in use; it glows while a boost burns. Right: stored energy, power made, heat sources and the boost. Bottom: a
 * status lamp and line, and the boost slot. Top right: the show-area button. Art: {@code textures/gui/blaze_furnace.png},
 * see {@code tools/sheep/generate_textures.py}.
 */
public class BlazeFurnaceScreen extends AbstractContainerScreen<BlazeFurnaceMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/blaze_furnace.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    // Heat column: interior 47..74 x 17..82; the fire (26x64, 3 frames at v 80) sits at x 48, bottom at y 83.
    private static final int COLUMN_X = 46, COLUMN_Y = 16, COLUMN_W = 30, COLUMN_H = 68;
    private static final int FIRE_X = 48, FIRE_BOTTOM = 83, FIRE_W = 26, FIRE_H = 64, FIRE_U = 176, FIRE_V = 80;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFB347;
    private static final int GEN_COLOR = 0xFF55D66B, BOOST_COLOR = 0xFFFFD25A, MARK_COLOR = 0xFF4A4E5A;
    private static final int INFO_CENTRE = 124;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;
    private static final int AREA_X = 154, AREA_Y = 4, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;
    private static final int AREA_ON = 0xFFFF8A1F;

    public BlazeFurnaceScreen(BlazeFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = BlazeFurnaceMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, AREA_ON, menu.containerId, BlazeFurnaceMenu.BUTTON_AREA));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;

        // Energy gauge.
        int height = menu.getEnergy() > 0 ? Math.max(1, Mth.ceil(menu.getEnergyFraction() * GAUGE_H)) : 0;
        if (height > 0) {
            int offset = GAUGE_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, FILL_V + offset, GAUGE_W, height, TEX, TEX);
            if (height >= 2) {
                int frame = (int) (time / 3 % 4);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, SHIMMER_V + frame * 2, GAUGE_W, 2, TEX, TEX);
            }
        }

        extractHeatColumn(graphics, x, y, time);

        MachineStatus lamp = status();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp.ordinal() * 8 % 64, LAMP_V + lamp.ordinal() / 8 * 8, 8, 8, TEX, TEX);
    }

    /**
     * The heat column: fire rising to the current heat, out of the most the hottest heat sources can give. A line
     * marks the heat each extra source brings it to, and a pip beside each line lights while that many are in use.
     */
    private void extractHeatColumn(GuiGraphicsExtractor graphics, int x, int y, long time) {
        int fire = menu.getHeat() > 0 ? Math.max(2, Mth.ceil(menu.getHeatFraction() * FIRE_H)) : 0;
        if (fire > 0) {
            int frame = (int) (time / 4 % 3);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + FIRE_X, y + FIRE_BOTTOM - fire, FIRE_U + frame * FIRE_W, FIRE_V, FIRE_W, fire, TEX, TEX);
            if (menu.getBoostTicks() > 0) {
                // Boosted: the fire pulses brighter.
                int alpha = 0x10 + (int) (0x20 * (0.5 + 0.5 * Math.sin(time * 0.4)));
                graphics.fill(x + FIRE_X, y + FIRE_BOTTOM - fire, x + FIRE_X + FIRE_W, y + FIRE_BOTTOM, (alpha << 24) | 0xFFF4C2);
            }
        }
        // A mark for each extra heat source, and a pip lit for each one in use.
        int max = menu.getMaxHeaters();
        int maxHeat = Math.max(1, menu.getMaxHeat());
        double falloff = falloff();
        for (int k = 1; k <= max; k++) {
            int level = BlazeFurnaceBlockEntity.combine(Collections.nCopies(k, (double) menu.getBestPower()), max, falloff);
            int my = y + FIRE_BOTTOM - Math.round(FIRE_H * Math.min(1F, level / (float) maxHeat));
            boolean inFire = FIRE_BOTTOM - fire <= my - y;
            graphics.fill(x + FIRE_X, my, x + FIRE_X + FIRE_W - 5, my + 1, inFire ? 0x60401008 : MARK_COLOR);
            boolean lit = k <= menu.getHeaters();
            graphics.fill(x + FIRE_X + FIRE_W - 4, my - 1, x + FIRE_X + FIRE_W - 1, my + 2, 0xFF141519);
            graphics.fill(x + FIRE_X + FIRE_W - 3, my, x + FIRE_X + FIRE_W - 2, my + 1, lit ? BOOST_COLOR : MARK_COLOR);
        }
    }

    /**
     * The falloff per extra heat source, worked out from the synced top of the column:
     * {@code maxHeat = best * (max - falloff * max (max - 1) / 2)}.
     */
    private double falloff() {
        int max = menu.getMaxHeaters();
        int best = menu.getBestPower();
        if (best <= 0 || max <= 1) return 0.0;
        return Mth.clamp((max - menu.getMaxHeat() / (double) best) / (max * (max - 1) / 2.0), 0.0, 1.0);
    }

    private MachineStatus status() {
        if (menu.getHeaters() == 0) return MachineStatus.IDLE;
        if (menu.isBufferFull()) return MachineStatus.NO_ENERGY;
        return MachineStatus.PROCESSING;
    }

    private Component statusText() {
        return switch (status()) {
            case IDLE -> Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.status.cold");
            case NO_ENERGY -> Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.status.full");
            default -> Component.translatable(menu.getBoostTicks() > 0
                    ? "gui.barelyfunctionalcontraptions.blaze_furnace.status.boosted" : "gui.barelyfunctionalcontraptions.blaze_furnace.status.burning");
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.stored", fe(menu.getEnergy())), 21, ENERGY_TEXT);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.capacity", fe(menu.getCapacity())), 31, TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.zpe.generating", fe(menu.getGenerating())),
                48, menu.getGenerating() > 0 ? GEN_COLOR : TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.sources", menu.getHeaters(), menu.getMaxHeaters()), 58, TEXT_DIM);
        if (menu.getBoostTicks() > 0) {
            centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.boost",
                    BlazeFurnaceBlock.multiplier(menu.getBoostMultiplier()), seconds(menu.getBoostTicks())), 68, BOOST_COLOR);
        } else {
            centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.no_boost"), 68, TEXT_DIM);
        }
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y + 1, status().color, false);
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
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.slot"));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.slot.desc").withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getEnergy()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.energy.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(COLUMN_X, COLUMN_Y, COLUMN_W, COLUMN_H, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.heat", fe(menu.getHeat()), fe(menu.getMaxHeat())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.heat.sources", menu.getHeaters(), menu.getMaxHeaters())
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.heat.desc").withStyle(ChatFormatting.GRAY));
            if (menu.getBoostTicks() > 0) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.heat.boosted",
                        BlazeFurnaceBlock.multiplier(menu.getBoostMultiplier()), fe(menu.getGenerating())).withStyle(ChatFormatting.YELLOW));
            }
        } else if (isHovering(80, 44, 88, 34, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.boost.tooltip"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.boost.desc").withStyle(ChatFormatting.GRAY));
            if (menu.getBoostTicks() > 0) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.boost.left",
                        seconds(menu.getBoostTicks()), seconds(menu.getBoostDuration())).withStyle(ChatFormatting.YELLOW));
            }
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 130, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(status().color));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.blaze_furnace.status." + status().name().toLowerCase(Locale.ROOT) + ".desc")
                    .withStyle(ChatFormatting.GRAY));
        } else if (isHovering(AREA_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isShowingArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static String seconds(int ticks) {
        return String.valueOf((ticks + 19) / 20);
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
