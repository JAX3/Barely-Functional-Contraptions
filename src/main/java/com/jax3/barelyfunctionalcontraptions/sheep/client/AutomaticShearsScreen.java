package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RedstoneMode;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.AutomaticShearsMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI of the Automatic Shears, in the same slate style as the Mechanical Separator. All art comes from
 * {@code textures/gui/automatic_shears.png} (256x256): the 176x193 background at (0,0) and sprites around it, see
 * {@code tools/sheep/generate_textures.py}. Energy gauge, status lamps and redstone button share the separator's
 * sprite positions.
 */
public class AutomaticShearsScreen extends AbstractContainerScreen<AutomaticShearsMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/automatic_shears.png");
    private static final int TEX = 256;

    // Vertical energy gauge (inner fill area, fills bottom-up) and its sprite.
    private static final int BAR_X = 11, BAR_Y = 19, BAR_W = 12, BAR_H = 64;
    private static final int BAR_U = 176, BAR_V = 96;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6;
    // Shears icon (2 frames of 16x16: open, closed) in its box, and the next-sweep bar under it.
    private static final int ICON_X = 41, ICON_Y = 23, ICON_SIZE = 16;
    private static final int ICON_U = 0, ICON_V = 194;
    private static final int SWEEP_X = 39, SWEEP_Y = 47, SWEEP_W = 50, SWEEP_H = 4;
    private static final int SWEEP_U = 0, SWEEP_V = 212;
    /** Centre line of the left part of the window, for the energy readout. */
    private static final int INFO_CENTRE = 63;
    // Status lamp (8x8 sprites per status) and redstone button.
    private static final int LAMP_X = 9, LAMP_Y = 87;
    private static final int LAMP_U = 186, LAMP_V = 194;
    private static final int BUTTON_X = 154, BUTTON_Y = 4, BUTTON_SIZE = 16;
    private static final int BUTTON_U = 176, BUTTON_V = 0;

    public AutomaticShearsScreen(AutomaticShearsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = AutomaticShearsMenu.INVENTORY_Y - 11;
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

        // Energy gauge fills bottom-up.
        int energyHeight = Mth.ceil(menu.getEnergyFraction() * BAR_H);
        if (energyHeight > 0) {
            int offset = BAR_H - energyHeight;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BAR_X, y + BAR_Y + offset, BAR_U, BAR_V + offset, BAR_W, energyHeight, TEX, TEX);
        }

        // Shears icon: snips open/closed while shearing, rests open otherwise.
        MachineStatus status = menu.getStatus();
        int frame = status == MachineStatus.PROCESSING && minecraft != null && minecraft.level != null
                ? (int) (minecraft.level.getGameTime() / 3 % 2) : 0;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + ICON_X, y + ICON_Y, ICON_U + frame * ICON_SIZE, ICON_V, ICON_SIZE, ICON_SIZE, TEX, TEX);

        // Next-sweep bar fills left to right while it waits; full while paused.
        int sweepWidth = Mth.ceil(menu.getSweepFraction() * SWEEP_W);
        if (sweepWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + SWEEP_X, y + SWEEP_Y, SWEEP_U, SWEEP_V, sweepWidth, SWEEP_H, TEX, TEX);
        }

        // Status lamp.
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + status.ordinal() * 8 % 64, LAMP_V + status.ordinal() / 8 * 8, 8, 8, TEX, TEX);

        // Redstone mode button (normal / hovered background + mode icon).
        boolean hovered = isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BUTTON_X, y + BUTTON_Y, BUTTON_U + (hovered ? 16 : 0), BUTTON_V + 64, BUTTON_SIZE, BUTTON_SIZE, TEX, TEX);
        int mode = menu.getRedstoneMode().ordinal();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BUTTON_X, y + BUTTON_Y, BUTTON_U + (mode % 4) * 16 % 64, BUTTON_V + (mode / 4) * 16, BUTTON_SIZE, BUTTON_SIZE, TEX, TEX);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Light text for the dark slate theme (the vanilla labels are dark grey).
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        // Stored FE over capacity, in the left part of the window.
        Component stored = Component.translatable("gui.barelyfunctionalcontraptions.shears.energy_stored", fe(menu.getEnergy()));
        Component capacity = Component.translatable("gui.barelyfunctionalcontraptions.shears.energy_capacity", fe(menu.getCapacity()));
        graphics.text(font, stored, INFO_CENTRE - font.width(stored) / 2, 56, TEXT_COLOR, false);
        graphics.text(font, capacity, INFO_CENTRE - font.width(capacity) / 2, 66, TEXT_DIM, false);
        // Status line, coloured like the lamp.
        MachineStatus status = menu.getStatus();
        graphics.text(font, statusName(status), LAMP_X + 13, LAMP_Y + 1, status.color, false);
    }

    private static Component statusName(MachineStatus status) {
        return Component.translatable("gui.barelyfunctionalcontraptions.shears.status." + status.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && hoveredSlot == null) {
            List<Component> lines = tooltipAt(mouseX, mouseY);
            if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private List<Component> tooltipAt(int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        boolean detailed = ModConfigs.getBool(ModConfigs.CLIENT.detailedMachineTooltips);
        if (isHovering(BAR_X - 2, BAR_Y - 2, BAR_W + 4, BAR_H + 4, mouseX, mouseY) || isHovering(34, 54, 58, 22, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getEnergy()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.shears.energy_per_shear", fe(menu.getEnergyPerShear())).withStyle(ChatFormatting.GRAY));
            if (detailed) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy_input",
                        fe(ModConfigs.getInt(ModConfigs.SERVER.automaticShearsMaxEnergyInput))).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.shears.energy_sides").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (isHovering(ICON_X - 2, ICON_Y - 2, ICON_SIZE + 4, ICON_SIZE + 4, mouseX, mouseY)
                || isHovering(SWEEP_X - 1, SWEEP_Y - 1, SWEEP_W + 2, SWEEP_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.shears.area"));
            if (menu.getStatus() == MachineStatus.IDLE || menu.getStatus() == MachineStatus.PROCESSING) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.shears.next_sweep",
                        String.format(Locale.ROOT, "%.1f", menu.getCooldown() / 20.0)).withStyle(ChatFormatting.GRAY));
            }
            if (detailed) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.shears.interval",
                        String.format(Locale.ROOT, "%.1f", menu.getInterval() / 20.0)).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 120, 10, mouseX, mouseY)) {
            MachineStatus status = menu.getStatus();
            lines.add(statusName(status).copy().withColor(status.color));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.shears.status." + status.name().toLowerCase(Locale.ROOT) + ".desc")
                    .withStyle(ChatFormatting.GRAY));
        } else if (isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            RedstoneMode mode = menu.getRedstoneMode();
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.redstone", mode.displayName()));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.redstone." + mode.name().toLowerCase(Locale.ROOT) + ".desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(menu.isPowered() ? "gui.barelyfunctionalcontraptions.redstone.powered" : "gui.barelyfunctionalcontraptions.redstone.unpowered").withStyle(ChatFormatting.DARK_GRAY));
            if (!ModConfigs.getBool(ModConfigs.SERVER.redstoneControlEnabled)) {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.redstone.locked").withStyle(ChatFormatting.RED));
            } else {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.redstone.click").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isHovering(BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE, event.x(), event.y())
                && minecraft != null && minecraft.gameMode != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AutomaticShearsMenu.BUTTON_REDSTONE);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
