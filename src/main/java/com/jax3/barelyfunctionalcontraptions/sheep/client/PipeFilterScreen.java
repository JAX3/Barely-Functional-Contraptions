package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The filter grid of one pipe side: a 3x3 grid of ghost slots in the middle, a mode button and a clear button on the
 * left, and the player's inventory below. Drawn with plain fills in the mod's dark slate style.
 */
public class PipeFilterScreen extends AbstractContainerScreen<PipeFilterMenu> {
    private static final int BG = 0xFF3A3D45, OUTLINE = 0xFF0F1013, LIGHT = 0xFF5F6470, SHADOW = 0xFF202228;
    private static final int SLOT = 0xFF2A2C33, SLOT_DARK = 0xFF17181C, SLOT_LIGHT = 0xFF6A6F7C;
    private static final int TEXT = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6;

    private Button modeButton;

    public PipeFilterScreen(PipeFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = PipeFilterMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> press(PipeFilterMenu.BUTTON_MODE))
                .bounds(leftPos + 6, topPos + 22, 52, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.barelyfunctionalcontraptions.pipe_filter.clear"), b -> press(PipeFilterMenu.BUTTON_CLEAR))
                .bounds(leftPos + 6, topPos + 44, 52, 16).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        PipeSide.Mode mode = menu.mode();
        modeButton.setMessage(mode.displayName().copy().withColor(switch (mode) {
            case PULL -> 0xFFFF8A2A;
            case PUSH -> 0xFF4BE37A;
            case DISABLED -> 0xFF9AA3C4;
            case NORMAL -> 0xFFFFFFFF;
        }));
        modeButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.barelyfunctionalcontraptions.pipe_filter.mode." + mode.getSerializedName())));
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, OUTLINE);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, LIGHT);
        g.fill(x + 2, y + 2, x + imageWidth - 1, y + imageHeight - 1, SHADOW);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, BG);
        // The 3x3 grid, inside a recessed frame.
        int gx = x + PipeFilterMenu.GRID_X - 1, gy = y + PipeFilterMenu.GRID_Y - 1;
        g.fill(gx - 3, gy - 3, gx + 3 * 18 + 3, gy + 3 * 18 + 3, SLOT_DARK);
        g.fill(gx - 2, gy - 2, gx + 3 * 18 + 2, gy + 3 * 18 + 2, 0xFF1B1C21);
        for (int i = 0; i < PipeFilterMenu.GRID; i++) slot(g, gx + (i % 3) * 18, gy + (i / 3) * 18);
        // The player's inventory.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) slot(g, x + 7 + col * 18, y + PipeFilterMenu.INVENTORY_Y - 1 + row * 18);
        }
        for (int col = 0; col < 9; col++) slot(g, x + 7 + col * 18, y + PipeFilterMenu.INVENTORY_Y + 57);
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT_LIGHT);
        g.fill(x, y, x + 17, y + 17, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        Component hint = Component.translatable("gui.barelyfunctionalcontraptions.pipe_filter.hint");
        int right = PipeFilterMenu.GRID_X + 3 * 18 + 6;
        var lines = font.split(hint, imageWidth - right - 6);
        for (int i = 0; i < lines.size() && i < 7; i++) graphics.text(font, lines.get(i), right, 18 + i * 9, TEXT_DIM, false);
    }
}
