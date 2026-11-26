package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity.Status;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity.Target;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.ImplosionChamberMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Window of the Implosion Chamber. Left: the energy gauge. Middle: its input, the Gravel it holds to crack again and its
 * output, with arrows between them that flare orange after a blast, and under them a fuse burning down to its next
 * spark. Below: its Explosive sheep (ready, regrowing) and its blasts and items cracked. Bottom: the status lamp and
 * line. Top right: the Sand/Gravel switch and the show-pen button. Art: {@code textures/gui/implosion_chamber.png}, see
 * {@code tools/sheep/generate_textures.py}.
 */
public class ImplosionChamberScreen extends AbstractContainerScreen<ImplosionChamberMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/implosion_chamber.png");
    private static final int TEX = 256;
    private static final int ENERGY_X = 9, GAUGE_Y = 19, GAUGE_W = 14, GAUGE_H = 64, ENERGY_U = 176;
    private static final int FUSE_X = 50, FUSE_Y = 50, FUSE_W = 92, FUSE_H = 4;
    private static final int INFO_CENTRE = 96;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;
    private static final int AREA_X = 154, TARGET_X = 135, BUTTON_Y = 4, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFD27F, EMBER_TEXT = 0xFFFFB45A;
    private static final int ARROW = 0xFF6A6F80, EMBER = 0xFFF07A2A, EMBER_HOT = 0xFFFFD27A, FUSE = 0xFFC9B89A, FUSE_BURNT = 0xFF3A3530;

    public ImplosionChamberScreen(ImplosionChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = ImplosionChamberMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + BUTTON_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, ImplosionChamberBlockEntity.AREA_COLOR, menu.containerId, ImplosionChamberMenu.BUTTON_AREA));
        addRenderableWidget(new AreaToggleButton(leftPos + TARGET_X, topPos + BUTTON_Y, TEXTURE, BUTTON_U, BUTTON_V,
                () -> menu.getTarget() == Target.GRAVEL, ImplosionChamberBlockEntity.AREA_COLOR, menu.containerId, ImplosionChamberMenu.BUTTON_TARGET,
                new ItemStack(Items.GRAVEL), Component.translatable("gui.barelyfunctionalcontraptions.implosion_chamber.target.button")));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        float energy = menu.getEnergy() / (float) menu.getCapacity();
        int height = energy > 0 ? Math.max(1, Mth.ceil(Math.min(1F, energy) * GAUGE_H)) : 0;
        if (height > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ENERGY_X, y + GAUGE_Y + GAUGE_H - height, ENERGY_U, GAUGE_H - height, GAUGE_W, height, TEX, TEX);

        // The arrows between the slots: orange while it glows after a blast. Stopping at Gravel, the second is dim.
        float glow = menu.getFlash() / (float) ImplosionChamberBlockEntity.FLASH_TICKS;
        int hot = glow > 0 ? lerp(ARROW, EMBER, glow) : ARROW;
        arrow(graphics, x + ImplosionChamberMenu.INPUT_X + 19, y + ImplosionChamberMenu.SLOT_Y + 7, hot);
        arrow(graphics, x + ImplosionChamberMenu.CRACKED_X + 19, y + ImplosionChamberMenu.SLOT_Y + 7, menu.getTarget() == Target.SAND ? hot : 0xFF3A3D48);

        // The fuse to its next spark: burnt from the left as the time runs down, a spark at its end.
        int burnt = Math.round(FUSE_W * (1F - menu.getCooldown() / (float) menu.getInterval()));
        graphics.fill(x + FUSE_X, y + FUSE_Y, x + FUSE_X + FUSE_W, y + FUSE_Y + FUSE_H, FUSE);
        graphics.fill(x + FUSE_X, y + FUSE_Y, x + FUSE_X + burnt, y + FUSE_Y + FUSE_H, FUSE_BURNT);
        if (menu.getStatus() == Status.CRACKING || menu.getStatus() == Status.WAITING) {
            int sx = x + FUSE_X + Math.min(FUSE_W - 2, burnt);
            graphics.fill(sx - 1, y + FUSE_Y - 1, sx + 2, y + FUSE_Y + FUSE_H + 1, EMBER);
            graphics.fill(sx, y + FUSE_Y, sx + 1, y + FUSE_Y + FUSE_H, EMBER_HOT);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp(menu.getStatus()) * 8, LAMP_V, 8, 8, TEX, TEX);
    }

    private static void arrow(GuiGraphicsExtractor graphics, int ax, int ay, int colour) {
        graphics.fill(ax, ay, ax + 9, ay + 2, colour);
        graphics.fill(ax + 7, ay - 2, ax + 9, ay + 4, colour);
        graphics.fill(ax + 9, ay - 1, ax + 10, ay + 3, colour);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF), g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF),
                bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** The status lamp sprite: grey, green, amber, red. */
    private static int lamp(Status status) {
        return switch (status) {
            case IDLE -> 0;
            case CRACKING -> 1;
            case WAITING -> 2;
            case NO_SHEEP, FULL, NO_POWER -> 3;
        };
    }

    private Component statusText() {
        return Component.translatable("gui.barelyfunctionalcontraptions.implosion_chamber.status." + menu.getStatus().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String key = "gui.barelyfunctionalcontraptions.implosion_chamber.";
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable(key + "making", Component.translatable(key + "target." + menu.getTarget().name().toLowerCase(Locale.ROOT))), 15, EMBER_TEXT);
        centred(graphics, Component.translatable(key + "sheep", menu.getReadySheep(), menu.getRegrowingSheep()), 60,
                menu.getReadySheep() > 0 ? TEXT_COLOR : TEXT_DIM);
        centred(graphics, Component.translatable(key + "blasts", fmt(menu.getBlasts()), fmt(menu.getCracked())), 72, TEXT_DIM);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y + 1, menu.getStatus().color, false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, INFO_CENTRE - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        String key = "gui.barelyfunctionalcontraptions.implosion_chamber.";
        List<Component> lines = new ArrayList<>();
        if (hoveredSlot != null && hoveredSlot.index < 3) {
            if (!hoveredSlot.hasItem() || hoveredSlot.index == ImplosionChamberBlockEntity.CRACKED) {
                String slot = switch (hoveredSlot.index) {
                    case ImplosionChamberBlockEntity.INPUT -> "input";
                    case ImplosionChamberBlockEntity.CRACKED -> "cracked";
                    default -> "output";
                };
                lines.add(Component.translatable(key + "slot." + slot));
                lines.add(Component.translatable(key + "slot." + slot + ".desc", ImplosionChamberBlockEntity.batch()).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(ENERGY_X - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "energy", fmt(menu.getEnergy()), fmt(menu.getCapacity())).withColor(ENERGY_TEXT));
            lines.add(Component.translatable(key + "energy.desc", fmt(ImplosionChamberBlockEntity.sparkEnergy())).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(FUSE_X - 1, FUSE_Y - 2, FUSE_W + 2, FUSE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "fuse", String.format(Locale.ROOT, "%.1f", menu.getCooldown() / 20.0)).withColor(EMBER_TEXT));
            lines.add(Component.translatable(key + "fuse.desc", String.format(Locale.ROOT, "%.0f", menu.getInterval() / 20.0)).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(40, 58, 112, 11, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "sheep", menu.getReadySheep(), menu.getRegrowingSheep()));
            lines.add(Component.translatable(key + "sheep.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 150, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(menu.getStatus().color));
            lines.add(Component.translatable(key + "status." + menu.getStatus().name().toLowerCase(Locale.ROOT) + ".desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(TARGET_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.getTarget() == Target.GRAVEL ? key + "target.gravel_on" : key + "target.sand_on"));
            lines.add(Component.translatable(key + "target.desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(key + "target.click").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(AREA_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isShowingArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static String fmt(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
