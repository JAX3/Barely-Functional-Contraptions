package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ReservoirStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.FluidReservoirMenu;
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
 * GUI of the Fluid Reservoir, in the same slate style as the other machines. All art comes from
 * {@code textures/gui/fluid_reservoir.png} (256x256): the 176x193 background at (0,0), the water fill at (176,0) and the
 * shared status lamps at (186,194); see {@code tools/sheep/generate_textures.py}.
 */
public class FluidReservoirScreen extends AbstractContainerScreen<FluidReservoirMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/fluid_reservoir.png");
    private static final int TEX = 256;
    // Water tank (inner fill area, fills bottom-up): one tick mark per bucket.
    private static final int TANK_X = 11, TANK_Y = 19, TANK_W = 30, TANK_H = 64;
    private static final int TANK_U = 176, TANK_V = 0;
    // Animated surface shimmer: 4 frames of 30x2 under the fill sprite.
    private static final int SHIMMER_V = 66;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, WATER_TEXT = 0xFF8FD3FF;
    private static final int INFO_CENTRE = 86;
    private static final int LAMP_X = 9, LAMP_Y = 87;
    private static final int LAMP_U = 186, LAMP_V = 194;

    public FluidReservoirScreen(FluidReservoirMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = FluidReservoirMenu.INVENTORY_Y - 11;
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

        int height = Mth.ceil(menu.getFillFraction() * TANK_H);
        if (height > 0) {
            int offset = TANK_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + TANK_X, y + TANK_Y + offset, TANK_U, TANK_V + offset, TANK_W, height, TEX, TEX);
            if (menu.getStatus() == ReservoirStatus.FILLING && height >= 2 && minecraft != null && minecraft.level != null) {
                int frame = (int) (minecraft.level.getGameTime() / 4 % 4);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + TANK_X, y + TANK_Y + offset, TANK_U, SHIMMER_V + frame * 2, TANK_W, 2, TEX, TEX);
            }
        }
        ReservoirStatus status = menu.getStatus();
        int lamp = status.lamp.ordinal();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp * 8 % 64, LAMP_V + lamp / 8 * 8, 8, 8, TEX, TEX);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        Component stored = Component.translatable("gui.barelyfunctionalcontraptions.reservoir.stored", mb(menu.getAmount()));
        Component capacity = Component.translatable("gui.barelyfunctionalcontraptions.reservoir.capacity", mb(menu.getCapacity()));
        Component buckets = Component.translatable("gui.barelyfunctionalcontraptions.reservoir.buckets",
                String.format(Locale.ROOT, "%.1f", menu.getAmount() / 1000.0));
        graphics.text(font, stored, INFO_CENTRE - font.width(stored) / 2, 26, WATER_TEXT, false);
        graphics.text(font, capacity, INFO_CENTRE - font.width(capacity) / 2, 36, TEXT_DIM, false);
        graphics.text(font, buckets, INFO_CENTRE - font.width(buckets) / 2, 52, TEXT_COLOR, false);
        if (menu.getStatus() == ReservoirStatus.FILLING) {
            Component rate = Component.translatable("gui.barelyfunctionalcontraptions.reservoir.rate", mb(menu.getWaterPerSecond()));
            graphics.text(font, rate, INFO_CENTRE - font.width(rate) / 2, 64, TEXT_DIM, false);
        }
        ReservoirStatus status = menu.getStatus();
        graphics.text(font, statusName(status), LAMP_X + 13, LAMP_Y + 1, status.color(), false);
    }

    private static Component statusName(ReservoirStatus status) {
        return Component.translatable("gui.barelyfunctionalcontraptions.reservoir.status." + status.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && hoveredSlot == null) {
            List<Component> lines = new ArrayList<>();
            if (isHovering(TANK_X - 2, TANK_Y - 2, TANK_W + 4, TANK_H + 4, mouseX, mouseY)) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.reservoir.tank", mb(menu.getAmount()), mb(menu.getCapacity())));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.reservoir.tank.desc").withStyle(ChatFormatting.GRAY));
            } else if (isHovering(LAMP_X, LAMP_Y - 1, 120, 10, mouseX, mouseY)) {
                ReservoirStatus status = menu.getStatus();
                lines.add(statusName(status).copy().withColor(status.color()));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.reservoir.status." + status.name().toLowerCase(Locale.ROOT) + ".desc")
                        .withStyle(ChatFormatting.GRAY));
            } else if (isHovering(FluidReservoirMenu.INPUT_X - 1, FluidReservoirMenu.INPUT_Y - 1, 18, FluidReservoirMenu.OUTPUT_Y - FluidReservoirMenu.INPUT_Y + 18, mouseX, mouseY)) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.reservoir.buckets_slot"));
            }
            if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private static String mb(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
