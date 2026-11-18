package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.State;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.SheepTreadmillMenu;
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
 * Window of the Sheep Treadmill, in the slate style of the other machines. Left: the FE buffer. Middle: the wheat
 * (golden while a sheep walks) over the food slot, and the walking sheep's stamina bar. Right: stored energy, FE/t and
 * the sheep's stamina or rest. Bottom: a status lamp and line. Art: {@code textures/gui/sheep_treadmill.png}, see
 * {@code tools/sheep/generate_textures.py}.
 */
public class SheepTreadmillScreen extends AbstractContainerScreen<SheepTreadmillMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/sheep_treadmill.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    private static final int WHEAT_X = 60, WHEAT_Y = 24, WHEAT_SIZE = 14, WHEAT_U = 176, WHEAT_V = 80;
    private static final int STAMINA_X = 49, STAMINA_Y = 78, STAMINA_W = 36, STAMINA_H = 3, STAMINA_U = 176, STAMINA_V = 96;
    private static final int BOX_X = 46, BOX_Y = 16, BOX_W = 42, BOX_H = 68;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFD25A;
    private static final int GEN_COLOR = 0xFF55D66B, STAMINA_COLOR = 0xFF8FD5FF;
    private static final int INFO_CENTRE = 130;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;

    public SheepTreadmillScreen(SheepTreadmillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = SheepTreadmillMenu.INVENTORY_Y - 11;
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
        // The wheat turns golden while a sheep walks for it.
        if (menu.getState() == State.WALKING) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + WHEAT_X, y + WHEAT_Y, WHEAT_U, WHEAT_V, WHEAT_SIZE, WHEAT_SIZE, TEX, TEX);
        }
        int stamina = Mth.ceil(menu.getStamina() * STAMINA_W);
        if (stamina > 0 && (menu.getState() == State.WALKING || menu.getState() == State.FULL)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + STAMINA_X, y + STAMINA_Y, STAMINA_U, STAMINA_V, stamina, STAMINA_H, TEX, TEX);
        }
        MachineStatus lamp = lamp();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp.ordinal() * 8 % 64, LAMP_V + lamp.ordinal() / 8 * 8, 8, 8, TEX, TEX);
    }

    private MachineStatus lamp() {
        return switch (menu.getState()) {
            case WALKING -> MachineStatus.PROCESSING;
            case HUNGRY -> MachineStatus.INVALID_INPUT;
            case RESTING -> MachineStatus.NO_ENERGY;
            case FULL -> MachineStatus.OUTPUT_FULL;
            case NO_SHEEP -> MachineStatus.IDLE;
        };
    }

    private int lampColour() {
        return lamp().color;
    }

    private String stateKey() {
        return "gui.barelyfunctionalcontraptions.sheep_treadmill.status." + menu.getState().name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.stored", fe(menu.getEnergy())), 21, ENERGY_TEXT);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.capacity", fe(menu.getCapacity())), 31, TEXT_DIM);
        boolean walking = menu.getState() == State.WALKING;
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.zpe.generating", fe(menu.getGenerating())), 50, walking ? GEN_COLOR : TEXT_DIM);
        Component sheepLine = switch (menu.getState()) {
            case WALKING, FULL -> Component.translatable("gui.barelyfunctionalcontraptions.sheep_treadmill.stamina", Math.round(menu.getStamina() * 100));
            case RESTING -> Component.translatable("gui.barelyfunctionalcontraptions.sheep_treadmill.resting", (menu.getRestLeft() + 19) / 20);
            default -> Component.translatable("gui.barelyfunctionalcontraptions.sheep_treadmill.no_walker");
        };
        centred(graphics, sheepLine, 62, walking ? STAMINA_COLOR : TEXT_DIM);
        graphics.text(font, Component.translatable(stateKey()), LAMP_X + 13, LAMP_Y + 1, lampColour(), false);
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
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.sheep_treadmill.slot"));
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.sheep_treadmill.slot.desc").withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getEnergy()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.sheep_treadmill.energy.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(BOX_X, BOX_Y, BOX_W, BOX_H, mouseX, mouseY) || isHovering(92, 44, 76, 34, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.sheep_treadmill.how"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.sheep_treadmill.how.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 130, 10, mouseX, mouseY)) {
            lines.add(Component.translatable(stateKey()).withColor(lampColour()));
            lines.add(Component.translatable(stateKey() + ".desc").withStyle(ChatFormatting.GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
