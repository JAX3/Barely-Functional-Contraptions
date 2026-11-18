package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.ThermalReactionChamberMenu;
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
 * Window of the Thermal Reaction Chamber. Left to right: the energy gauge, the hot side's thermometer (rising with
 * the Blaze sheep), the centre panel (pairs, output, the balance needle between hot and cold, and the danger bar
 * growing towards a meltdown or a freeze), the cold side's thermometer, and the water gauge. Under the panel: the
 * Blaze Rod and Blue Ice boost slots with their burn bars. Bottom: the status lamp and line. Top right: the
 * show-pens button. Art: {@code textures/gui/thermal_reaction_chamber.png}, see {@code tools/sheep/generate_textures.py}.
 */
public class ThermalReactionChamberScreen extends AbstractContainerScreen<ThermalReactionChamberMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/thermal_reaction_chamber.png");
    private static final int TEX = 256;
    private static final int GAUGE_W = 30, GAUGE_H = 64, GAUGE_Y = 19;
    private static final int ENERGY_X = 11, ENERGY_U = 176, WATER_X = 136, WATER_U = 206, FILL_V = 0, SHIMMER_V = 66;
    private static final int THERMO_W = 8, THERMO_H = 64, HOT_X = 48, COLD_X = 122, THERMO_Y = 19, HOT_U = 176, COLD_U = 186, THERMO_V = 90;
    private static final int SCALE_X = 64, SCALE_Y = 41, SCALE_W = 50, SCALE_H = 6, SCALE_U = 176, SCALE_V = 80;
    private static final int DANGER_X = 62, DANGER_Y = 51, DANGER_W = 54, DANGER_H = 4;
    private static final int BURN_Y = 81, BURN_W = 17, BURN_H = 3;
    private static final int CENTRE = 89;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, HOT_TEXT = 0xFFFFB46A, COLD_TEXT = 0xFFBFE8FF,
            ENERGY_TEXT = 0xFFFFD27F, WATER_TEXT = 0xFF7FB8FF;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;
    private static final int AREA_X = 154, AREA_Y = 4, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;

    public ThermalReactionChamberScreen(ThermalReactionChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = ThermalReactionChamberMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8; // left-aligned: centred, the long name runs under the pens button
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, ThermalReactionChamberBlockEntity.HOT_COLOR, menu.containerId, ThermalReactionChamberMenu.BUTTON_AREA));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;

        gauge(graphics, x + ENERGY_X, y + GAUGE_Y, ENERGY_U, menu.getEnergy() / (float) menu.getCapacity(), time);
        gauge(graphics, x + WATER_X, y + GAUGE_Y, WATER_U, menu.getWater() / (float) menu.getTankCapacity(), time);

        // Thermometers: a herd's heat or cold, against the most pairs it counts.
        thermometer(graphics, x + HOT_X, y + THERMO_Y, HOT_U, menu.getHot());
        thermometer(graphics, x + COLD_X, y + THERMO_Y, COLD_U, menu.getCold());

        // The balance scale and its needle: the middle is balanced, each step is an unpaired sheep or a lone boost.
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + SCALE_X, y + SCALE_Y, SCALE_U, SCALE_V, SCALE_W, SCALE_H, TEX, TEX);
        int step = Mth.clamp(menu.getBalance(), -3, 3);
        // Hot is on the left of the chamber, so it's on the left of the scale too.
        int needle = x + SCALE_X + SCALE_W / 2 - step * 8;
        if (menu.getState().reacting() && menu.getBalance() != 0 && time % 10 < 2) needle += time % 4 < 2 ? 1 : -1; // a nervous quiver
        graphics.fill(needle - 1, y + SCALE_Y - 3, needle + 1, y + SCALE_Y + SCALE_H + 2, 0xFF141519);
        graphics.fill(needle, y + SCALE_Y - 2, needle + 1, y + SCALE_Y + SCALE_H + 1, 0xFFFFFFFF);

        // Danger: grows from the middle towards the hot or cold end as it heads for a meltdown or a freeze.
        int danger = menu.getDanger();
        int half = DANGER_W / 2;
        int length = Math.round(half * Math.min(1F, Math.abs(danger) / 1000F));
        int mid = x + DANGER_X + half;
        if (length > 0) {
            boolean hot = danger > 0;
            int color = Math.abs(danger) > 700 && time % 10 < 5 ? 0xFFFFFFFF : hot ? 0xFFE8561A : 0xFF3F8FCF;
            if (hot) graphics.fill(mid - length, y + DANGER_Y, mid, y + DANGER_Y + DANGER_H, color);
            else graphics.fill(mid, y + DANGER_Y, mid + length, y + DANGER_Y + DANGER_H, color);
        }
        graphics.fill(mid, y + DANGER_Y - 1, mid + 1, y + DANGER_Y + DANGER_H + 1, 0xFF6A6E7A);

        // Burn left on each boost.
        burn(graphics, x + ThermalReactionChamberMenu.HOT_X, y + BURN_Y, menu.getHotBurn(), 0xFFFF9A2E);
        burn(graphics, x + ThermalReactionChamberMenu.COLD_X, y + BURN_Y, menu.getColdBurn(), 0xFF7FC8F0);

        State state = menu.getState();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp(state) * 8, LAMP_V, 8, 8, TEX, TEX);
    }

    private void gauge(GuiGraphicsExtractor graphics, int gx, int gy, int u, float fraction, long time) {
        int height = fraction > 0 ? Math.max(1, Mth.ceil(Math.min(1F, fraction) * GAUGE_H)) : 0;
        if (height <= 0) return;
        int offset = GAUGE_H - height;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, gx, gy + offset, u, FILL_V + offset, GAUGE_W, height, TEX, TEX);
        if (height >= 2) {
            int frame = (int) (time / 4 % 4);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, gx, gy + offset, u, SHIMMER_V + frame * 2, GAUGE_W, 2, TEX, TEX);
        }
    }

    private void thermometer(GuiGraphicsExtractor graphics, int tx, int ty, int u, int count) {
        int height = count > 0 ? Math.max(3, Math.round(THERMO_H * Math.min(1F, count / (float) menu.getMaxPairs()))) : 0;
        if (height > 0) {
            int offset = THERMO_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, tx, ty + offset, u, THERMO_V + offset, THERMO_W, height, TEX, TEX);
        }
        for (int k = 1; k < menu.getMaxPairs(); k++) { // a mark for each sheep
            int my = ty + THERMO_H - Math.round(THERMO_H * k / (float) menu.getMaxPairs());
            graphics.fill(tx, my, tx + 3, my + 1, 0x80000000);
        }
    }

    private static void burn(GuiGraphicsExtractor graphics, int bx, int by, int thousandths, int color) {
        int width = Math.round(BURN_W * Math.min(1F, thousandths / 1000F));
        if (width > 0) graphics.fill(bx - 1, by, bx - 1 + width, by + BURN_H, color);
    }

    /** The status lamp sprite for each state: grey, green, amber, red. */
    private static int lamp(State state) {
        return switch (state) {
            case NEEDS_HERDS -> 0;
            case REACTING -> 1;
            case RUNNING_HOT, RUNNING_COLD, NO_WATER -> 2;
            case FULL -> 3;
            case OVERHEATED, FROZEN -> 4;
        };
    }

    private Component statusText() {
        return Component.translatable("gui.barelyfunctionalcontraptions.thermal_reaction_chamber.status." + menu.getState().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.thermal_reaction_chamber.pairs", menu.getPairs(), menu.getMaxPairs()), 20,
                menu.getPairs() > 0 ? TEXT_COLOR : TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.thermal_reaction_chamber.rate", compact(menu.getGenerating())), 30,
                menu.getGenerating() > 0 ? ENERGY_TEXT : TEXT_DIM);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y + 1, menu.getState().color, false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, CENTRE - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        String key = "gui.barelyfunctionalcontraptions.thermal_reaction_chamber.";
        List<Component> lines = new ArrayList<>();
        if (hoveredSlot != null) {
            if (hoveredSlot.index < 2 && !hoveredSlot.hasItem()) {
                String slot = hoveredSlot.index == 0 ? "hot_slot" : "cold_slot";
                lines.add(Component.translatable(key + slot).withColor(hoveredSlot.index == 0 ? HOT_TEXT : COLD_TEXT));
                lines.add(Component.translatable(key + slot + ".desc").withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(ENERGY_X - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "energy", fe(menu.getEnergy()), fe(menu.getCapacity())).withColor(ENERGY_TEXT));
            lines.add(Component.translatable(key + "energy.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(WATER_X - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "water", fe(menu.getWater()), fe(menu.getTankCapacity())).withColor(WATER_TEXT));
            lines.add(Component.translatable(key + "water.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(HOT_X - 1, THERMO_Y - 1, THERMO_W + 2, THERMO_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "hot", menu.getHot()).withColor(HOT_TEXT));
            lines.add(Component.translatable(key + "hot.desc").withStyle(ChatFormatting.GRAY));
            spentLine(lines, key);
        } else if (isHovering(COLD_X - 1, THERMO_Y - 1, THERMO_W + 2, THERMO_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "cold", menu.getCold()).withColor(COLD_TEXT));
            lines.add(Component.translatable(key + "cold.desc").withStyle(ChatFormatting.GRAY));
            spentLine(lines, key);
        } else if (isHovering(SCALE_X - 2, SCALE_Y - 4, SCALE_W + 4, SCALE_H + 7, mouseX, mouseY)) {
            int balance = menu.getBalance();
            lines.add(balance == 0 ? Component.translatable(key + "balance.even")
                    : Component.translatable(key + (balance > 0 ? "balance.hot" : "balance.cold"), Math.abs(balance))
                    .withColor(balance > 0 ? HOT_TEXT : COLD_TEXT));
            lines.add(Component.translatable(key + "balance.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(DANGER_X, DANGER_Y - 2, DANGER_W, DANGER_H + 4, mouseX, mouseY)) {
            int danger = menu.getDanger();
            lines.add(danger == 0 ? Component.translatable(key + "danger.none")
                    : Component.translatable(key + (danger > 0 ? "danger.hot" : "danger.cold"), Math.abs(danger) / 10)
                    .withColor(danger > 0 ? HOT_TEXT : COLD_TEXT));
            lines.add(Component.translatable(key + "danger.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 150, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(menu.getState().color));
            lines.add(Component.translatable(key + "status." + menu.getState().name().toLowerCase(Locale.ROOT) + ".desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(AREA_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isShowingArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private void spentLine(List<Component> lines, String key) {
        if (menu.getSpent() > 0) lines.add(Component.translatable(key + "spent", menu.getSpent()).withStyle(ChatFormatting.RED));
    }

    /** Short enough for the centre panel: "8,050", then "13.1k". */
    private static String compact(int value) {
        return value < 10_000 ? fe(value) : String.format(Locale.ROOT, "%.1fk", value / 1000.0);
    }

    private static String fe(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
