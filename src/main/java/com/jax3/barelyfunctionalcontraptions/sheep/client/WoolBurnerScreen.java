package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.burner.WoolBurnerFuel;
import com.jax3.barelyfunctionalcontraptions.sheep.burner.WoolBurnerFuels;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.WoolBurnerMenu;
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
 * Window of the Wool Burner, in the slate style of the other machines. Left: the FE buffer. Middle: the flame (burning
 * down as the item burns, like a furnace's) over the fuel slot. Right: stored energy, FE/t and the time left on the
 * item burning. Bottom: a status lamp and line. Art: {@code textures/gui/wool_burner.png}, see
 * {@code tools/sheep/generate_textures.py}.
 */
public class WoolBurnerScreen extends AbstractContainerScreen<WoolBurnerMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/wool_burner.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    private static final int FLAME_X = 60, FLAME_Y = 24, FLAME_SIZE = 14, FLAME_U = 176, FLAME_V = 80;
    private static final int BOX_X = 46, BOX_Y = 16, BOX_W = 42, BOX_H = 68;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFB347;
    private static final int GEN_COLOR = 0xFF55D66B, TIME_COLOR = 0xFFFFD25A;
    private static final int INFO_CENTRE = 130;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;

    public WoolBurnerScreen(WoolBurnerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = WoolBurnerMenu.INVENTORY_Y - 11;
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

        // The flame burns down as the item burns, like a furnace's.
        int flame = menu.getBurnTime() > 0 ? Math.max(1, Mth.ceil(menu.getBurnFraction() * FLAME_SIZE)) : 0;
        if (flame > 0) {
            int offset = FLAME_SIZE - flame;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + FLAME_X, y + FLAME_Y + offset, FLAME_U, FLAME_V + offset, FLAME_SIZE, flame, TEX, TEX);
        }

        MachineStatus lamp = status();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp.ordinal() * 8 % 64, LAMP_V + lamp.ordinal() / 8 * 8, 8, 8, TEX, TEX);
    }

    private MachineStatus status() {
        if (menu.getBurnTime() > 0) return MachineStatus.PROCESSING;
        if (menu.isBufferFull() && menu.getSlot(0).hasItem()) return MachineStatus.NO_ENERGY;
        return MachineStatus.IDLE;
    }

    private Component statusText() {
        return Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.status." + status().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.stored", fe(menu.getEnergy())), 21, ENERGY_TEXT);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.capacity", fe(menu.getCapacity())), 31, TEXT_DIM);
        boolean burning = menu.getBurnTime() > 0;
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.zpe.generating", fe(burning ? menu.getPower() : 0)),
                50, burning ? GEN_COLOR : TEXT_DIM);
        centred(graphics, burning
                ? Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.left", seconds(menu.getBurnTime()))
                : Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.no_fuel"), 62, burning ? TIME_COLOR : TEXT_DIM);
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
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.slot"));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.slot.desc").withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getEnergy()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.energy.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(BOX_X, BOX_Y, BOX_W, BOX_H, mouseX, mouseY) || isHovering(92, 44, 76, 34, mouseX, mouseY)) {
            if (menu.getBurnTime() > 0) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.burning",
                        fe(menu.getPower()), seconds(menu.getBurnTime()), seconds(menu.getBurnTotal())));
            } else {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.no_fuel"));
            }
            // What burns, and how well, from the loaded fuels.
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.fuels").withStyle(ChatFormatting.GRAY));
            for (WoolBurnerFuel fuel : WoolBurnerFuels.all()) {
                if (fuel.ingredient().items().findAny().isEmpty()) continue;
                Component name = fuelName(fuel);
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.fuel_line", name, fe(fuel.power()), exactSeconds(fuel.burnTime()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 130, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(status().color));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.status." + status().name().toLowerCase(Locale.ROOT) + ".desc")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    /** A short name for a fuel: its item's name, or the first item's "and N more" for a tag. */
    @SuppressWarnings("deprecation")
    private static Component fuelName(WoolBurnerFuel fuel) {
        var items = fuel.ingredient().items().toList();
        if (items.size() > 1) {
            return Component.translatable("gui.barelyfunctionalcontraptions.wool_burner.any",
                    new net.minecraft.world.item.ItemStack(items.getFirst()).getHoverName(), items.size() - 1);
        }
        var stack = new net.minecraft.world.item.ItemStack(items.getFirst());
        if (fuel.ingredient().getCustomIngredient() instanceof com.jax3.barelyfunctionalcontraptions.sheep.recipe.ResourceWoolIngredient wool && wool.resource().isPresent()) {
            stack = com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(wool.resource().get(), 1);
        }
        return stack.getHoverName();
    }

    /** "2.5" for 50 ticks, "10" for 200. */
    private static String exactSeconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private static String seconds(int ticks) {
        return String.valueOf((ticks + 19) / 20);
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
