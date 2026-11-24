package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity.Status;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Level;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.FleeceTrawlerMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Window of the Fleece Trawler, drawn in flat panels: left, the lure slot (and how many hauls the lure has left), its
 * Tide sheep (a pip for each, up to the most that count) and its pool (a bar up to a full 7x7x4); middle, the winch:
 * the haul's progress as a line being wound in, with a wave under it; right, its nine catch slots; along the bottom, the
 * status lamp and line. Top right: show or hide the pool.
 */
public class FleeceTrawlerScreen extends AbstractContainerScreen<FleeceTrawlerMenu> {
    /** The area button's art, shared with the Snow Sheep Chiller's window. */
    private static final Identifier BUTTON_TEXTURE = ResourcefulSheep.id("textures/gui/chiller.png");
    private static final int BUTTON_U = 176, BUTTON_V = 146, AREA_X = 154, AREA_Y = 4, BUTTON_SIZE = 16;
    private static final int BG = 0xFF2C2E36, EDGE = 0xFF17181C, PANEL = 0xFF23252C, SLOT = 0xFF3A3D47, SLOT_EDGE = 0xFF111215;
    private static final int TEXT = 0xFFDFE4F5, DIM = 0xFFAEB4C6, SEA = 0xFF5FE0C8, SEA_DARK = 0xFF1E5C57, ROPE = 0xFFC9A46A;
    private static final int WINCH_X = 52, WINCH_Y = 40, WINCH_W = 40;
    private static final int POOL_X = 8, POOL_Y = 78, POOL_W = 80;
    private static final int SHEEP_Y = 64;
    private static final int LAMP_X = 8, LAMP_Y = 90, PANEL_BOTTOM = 102;

    public FleeceTrawlerScreen(FleeceTrawlerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 196);
        this.inventoryLabelY = FleeceTrawlerMenu.INVENTORY_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, BUTTON_TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, FleeceTrawlerBlockEntity.AREA_COLOR, menu.containerId, FleeceTrawlerMenu.BUTTON_AREA));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, EDGE);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, BG);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 3, SEA_DARK);
        // The machine's panel, and the inventory below it.
        graphics.fill(x + 5, y + 15, x + imageWidth - 5, y + PANEL_BOTTOM, PANEL);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, slot.index == FleeceTrawlerBlockEntity.LURE ? SEA_DARK : SLOT);
        }
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;

        // The winch: a drum on the left, the line out to the right, wound in as the haul goes on; a wave under it.
        int wx = x + WINCH_X, wy = y + WINCH_Y;
        graphics.fill(wx - 6, wy - 6, wx, wy + 6, 0xFF5D6687);
        graphics.fill(wx - 5, wy - 5, wx - 1, wy + 5, 0xFF8D97BC);
        int out = Math.round(WINCH_W * (1F - menu.getProgress() / 100F));
        graphics.fill(wx, wy - 1, wx + out, wy, ROPE);
        if (menu.getStatus() == Status.HAULING) graphics.fill(wx + out - 1, wy - 2, wx + out + 1, wy + 2, SEA);
        for (int i = 0; i < WINCH_W + 8; i++) {
            int h = Math.round(1.5F + 1.5F * Mth.sin((i + time * (menu.getStatus() == Status.HAULING ? 0.6F : 0.1F)) * 0.45F));
            graphics.fill(wx - 6 + i, wy + 10 - h, wx - 5 + i, wy + 11, 0xFF2E7DB0);
        }

        // Its Tide sheep: a pip for each that could count, lit for those swimming.
        for (int i = 0; i < menu.getMaxSheep(); i++) {
            int px = x + 8 + i * 10;
            graphics.fill(px, y + SHEEP_Y, px + 8, y + SHEEP_Y + 6, SLOT_EDGE);
            graphics.fill(px + 1, y + SHEEP_Y + 1, px + 7, y + SHEEP_Y + 5, i < menu.getSheep() ? SEA : SLOT);
        }
        // Its pool: a bar up to a full-size pool, the least it needs marked.
        graphics.fill(x + POOL_X, y + POOL_Y, x + POOL_X + POOL_W, y + POOL_Y + 5, SLOT_EDGE);
        int pool = Math.round(POOL_W * Math.min(1F, menu.getPool() / (float) FleeceTrawlerBlockEntity.FULL_POOL));
        if (pool > 0) graphics.fill(x + POOL_X + 1, y + POOL_Y + 1, x + POOL_X + Math.max(2, pool) - 1, y + POOL_Y + 4, 0xFF2E7DB0);
        int min = Math.round(POOL_W * 18F / FleeceTrawlerBlockEntity.FULL_POOL);
        graphics.fill(x + POOL_X + min, y + POOL_Y - 1, x + POOL_X + min + 1, y + POOL_Y + 6, DIM);

        // The status lamp.
        graphics.fill(x + LAMP_X, y + LAMP_Y, x + LAMP_X + 6, y + LAMP_Y + 6, SLOT_EDGE);
        graphics.fill(x + LAMP_X + 1, y + LAMP_Y + 1, x + LAMP_X + 5, y + LAMP_Y + 5, level(menu.getStatus()).color);
    }

    private static Level level(Status status) {
        return switch (status) {
            case HAULING -> Level.RUNNING;
            case NO_SHEEP -> Level.WAITING;
            default -> Level.PROBLEM;
        };
    }

    private Component statusText() {
        return Component.translatable("status_panel.barelyfunctionalcontraptions.state." + menu.getStatus().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DIM, false);
        String key = "gui.barelyfunctionalcontraptions.fleece_trawler.";
        graphics.text(font, Component.translatable(key + "lure"), 8, 20, DIM, false);
        Component uses = menu.getSlot(FleeceTrawlerBlockEntity.LURE).hasItem()
                ? Component.translatable(key + "lure_uses", menu.getLureUsesLeft() > 0 ? menu.getLureUsesLeft() : menu.getLureUses())
                : Component.translatable(key + "no_lure");
        graphics.text(font, uses, 8, 50, menu.getSlot(FleeceTrawlerBlockEntity.LURE).hasItem() ? SEA : DIM, false);
        String percent = menu.getProgress() + "%";
        graphics.text(font, percent, WINCH_X + WINCH_W - font.width(percent), WINCH_Y - 14, DIM, false);
        graphics.text(font, menu.getSheep() + " / " + menu.getMaxSheep(), 8 + menu.getMaxSheep() * 10 + 2, SHEEP_Y - 1,
                menu.getSheep() > 0 ? SEA : DIM, false);
        graphics.text(font, Component.translatable(key + "pool", menu.getPool()), POOL_X + POOL_W + 3, POOL_Y - 2, DIM, false);
        graphics.text(font, statusText(), LAMP_X + 10, LAMP_Y - 1, level(menu.getStatus()).color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        String key = "gui.barelyfunctionalcontraptions.fleece_trawler.";
        List<Component> lines = new ArrayList<>();
        if (hoveredSlot != null) {
            if (hoveredSlot.index == FleeceTrawlerBlockEntity.LURE && !hoveredSlot.hasItem()) {
                lines.add(Component.translatable(key + "lure"));
                lines.add(Component.translatable(key + "lure.desc").withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(8, SHEEP_Y - 2, 84, 10, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "sheep", menu.getSheep(), menu.getMaxSheep()).withColor(SEA));
            lines.add(Component.translatable(key + "sheep.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(POOL_X, POOL_Y - 3, 130, 10, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "pool", menu.getPool()));
            lines.add(Component.translatable(key + "pool.desc", FleeceTrawlerBlockEntity.FULL_POOL).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(WINCH_X - 6, WINCH_Y - 20, WINCH_W + 10, 32, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "winch", menu.getProgress()));
            lines.add(Component.translatable(key + "hauls", menu.getHauls()).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 2, 130, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(level(menu.getStatus()).color));
            lines.add(Component.translatable(key + "status." + menu.getStatus().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(AREA_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isShowingArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
