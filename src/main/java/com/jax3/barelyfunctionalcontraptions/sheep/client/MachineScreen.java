package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.menu.MachineMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * A machine window that may have upgrade slots: draws them in a small panel just right of the window, labels the empty
 * ones, and counts clicks on the panel as inside the window (so a card dropped there isn't thrown on the ground).
 */
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    private static final int PANEL_EDGE = 0xFF14161A, PANEL_BG = 0xFF2B2F38, PANEL_LIGHT = 0xFF464C59;
    private static final int SLOT_EDGE = 0xFF111215, SLOT_BG = 0xFF3A3F4A, CARD_HINT = 0x40FFFFFF;
    /** The panel, relative to the window: it overlaps the window's right edge a little so it looks attached. */
    public static final int PANEL_X = 174, PANEL_Y = 4, PANEL_W = 32, PANEL_H = 48;

    protected MachineScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    /** The upgrade panel in screen coordinates (for JEI to keep its item list clear of it), or null without one. */
    public Rect2i upgradePanelArea() {
        return menu.hasUpgradeSlots() ? new Rect2i(leftPos + PANEL_X, topPos + PANEL_Y, PANEL_W, PANEL_H) : null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (!menu.hasUpgradeSlots()) return;
        int x = leftPos + PANEL_X, y = topPos + PANEL_Y;
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL_EDGE);
        graphics.fill(x + 1, y + 1, x + PANEL_W - 1, y + PANEL_H - 1, PANEL_BG);
        graphics.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, PANEL_LIGHT);
        for (Slot slot : menu.slots) {
            if (!menu.isUpgradeSlot(slot)) continue;
            int sx = leftPos + slot.x - 1, sy = topPos + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
            if (!slot.hasItem()) {
                // A faint card outline where one goes.
                graphics.fill(sx + 5, sy + 3, sx + 13, sy + 4, CARD_HINT);
                graphics.fill(sx + 5, sy + 14, sx + 13, sy + 15, CARD_HINT);
                graphics.fill(sx + 4, sy + 4, sx + 5, sy + 14, CARD_HINT);
                graphics.fill(sx + 13, sy + 4, sx + 14, sy + 14, CARD_HINT);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && menu.isUpgradeSlot(hoveredSlot) && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.barelyfunctionalcontraptions.upgrade_slot"),
                    Component.translatable("gui.barelyfunctionalcontraptions.upgrade_slot.desc").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        if (menu.hasUpgradeSlots() && mouseX >= left + PANEL_X && mouseX < left + PANEL_X + PANEL_W
                && mouseY >= top + PANEL_Y && mouseY < top + PANEL_Y + PANEL_H) return false;
        return super.hasClickedOutside(mouseX, mouseY, left, top);
    }
}
