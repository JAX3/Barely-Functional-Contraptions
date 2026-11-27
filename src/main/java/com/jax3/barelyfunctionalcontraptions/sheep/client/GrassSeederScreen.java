package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.Status;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.GrassSeederMenu;
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
 * Window of the Grass Seeder. Left: the energy and water gauges. Middle: a map of the ground it tends (grass, short
 * grass, bare dirt, the seeder in the middle), with a bar under it of how green it is. Right: the Bone Meal slot, how
 * many blocks the open Bone Meal has left, how green its ground is and how many of its sheep are grazing. Bottom: the
 * status lamp and line. Top right: the show-area button. Art: {@code textures/gui/grass_seeder.png}, see
 * {@code tools/sheep/generate_textures.py}.
 */
public class GrassSeederScreen extends MachineScreen<GrassSeederMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/grass_seeder.png");
    private static final int TEX = 256;
    private static final int ENERGY_X = 9, WATER_X = 27, GAUGE_Y = 19, GAUGE_W = 14, GAUGE_H = 64, ENERGY_U = 176, WATER_U = 192;
    private static final int MAP_X = 47, MAP_Y = 19, MAP_SIZE = 51;
    private static final int BAR_X = 47, BAR_Y = 75, BAR_W = 51, BAR_H = 8;
    private static final int INFO_CENTRE = 136;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;
    private static final int AREA_X = 154, AREA_Y = 4, TRIM_X = 135, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFFFFD27F, WATER_TEXT = 0xFF7FB8FF;
    private static final int GRASS = 0xFF5AA13A, GRASS_LIGHT = 0xFF7CC24E, TUFT = 0xFF9BD86A, DIRT = 0xFF79553A, DIRT_DARK = 0xFF5E4029,
            OTHER = 0xFF2A2C34, GREEN_TEXT = 0xFF8FE07A, TALL = 0xFF3F7A2A, TALL_BLADE = 0xFF6FB043, OVERGROWN_TEXT = 0xFFE0B04A;

    public GrassSeederScreen(GrassSeederMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = GrassSeederMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isShowingArea, GrassSeederBlockEntity.AREA_COLOR, menu.containerId, GrassSeederMenu.BUTTON_AREA));
        addRenderableWidget(new AreaToggleButton(leftPos + TRIM_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                menu::isTrimming, GrassSeederBlockEntity.AREA_COLOR, menu.containerId, GrassSeederMenu.BUTTON_TRIM,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS), Component.translatable("gui.barelyfunctionalcontraptions.grass_seeder.trim.button")));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        gauge(graphics, x + ENERGY_X, y + GAUGE_Y, ENERGY_U, menu.getEnergy() / (float) menu.getCapacity());
        gauge(graphics, x + WATER_X, y + GAUGE_Y, WATER_U, menu.getWater() / (float) menu.getTankCapacity());

        // The ground map: a tile for each block it tends, the seeder in the middle.
        int size = menu.getRadius() * 2 + 1;
        if (size <= GrassSeederBlockEntity.MAP_MAX) {
            int tile = MAP_SIZE / size, offset = (MAP_SIZE - tile * size) / 2;
            int[] map = menu.getMap();
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    int tx = x + MAP_X + offset + col * tile, ty = y + MAP_Y + offset + row * tile;
                    if (row == size / 2 && col == size / 2) {
                        seeder(graphics, tx, ty, tile);
                    } else {
                        tile(graphics, tx, ty, tile, GrassSeederBlockEntity.tile(map, row * size + col), row * 7 + col * 3);
                    }
                }
            }
        } else {
            graphics.fill(x + MAP_X, y + MAP_Y, x + MAP_X + MAP_SIZE, y + MAP_Y + MAP_SIZE, OTHER);
        }

        // How green the ground is: grass (short grass lighter), then bare dirt.
        int ground = Math.max(1, menu.getGrass() + menu.getDirt());
        int green = Math.round(BAR_W * menu.getGrass() / (float) ground), tufts = Math.round(BAR_W * menu.getShortGrass() / (float) ground);
        if (menu.getGrass() + menu.getDirt() > 0) {
            graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, DIRT);
            graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + green, y + BAR_Y + BAR_H, GRASS);
            graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + tufts, y + BAR_Y + BAR_H, GRASS_LIGHT);
            graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + green, y + BAR_Y + 1, 0x40FFFFFF);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp(menu.getStatus()) * 8, LAMP_V, 8, 8, TEX, TEX);
    }

    private void gauge(GuiGraphicsExtractor graphics, int gx, int gy, int u, float fraction) {
        int height = fraction > 0 ? Math.max(1, Mth.ceil(Math.min(1F, fraction) * GAUGE_H)) : 0;
        if (height <= 0) return;
        int offset = GAUGE_H - height;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, gx, gy + offset, u, offset, GAUGE_W, height, TEX, TEX);
    }

    /** One block of the map: grass, grass with a tuft of short grass, speckled dirt, or something else. */
    private static void tile(GuiGraphicsExtractor graphics, int tx, int ty, int size, int kind, int seed) {
        int inner = size - 1;
        switch (kind) {
            case GrassSeederBlockEntity.TILE_DIRT -> {
                graphics.fill(tx, ty, tx + inner, ty + inner, DIRT);
                for (int k = 0; k < 3; k++) {
                    int sx = (seed * 5 + k * 7) % inner, sy = (seed * 3 + k * 5) % inner;
                    graphics.fill(tx + sx, ty + sy, tx + sx + 1, ty + sy + 1, DIRT_DARK);
                }
            }
            case GrassSeederBlockEntity.TILE_GRASS, GrassSeederBlockEntity.TILE_SHORT -> {
                graphics.fill(tx, ty, tx + inner, ty + inner, GRASS);
                graphics.fill(tx, ty, tx + inner, ty + 1, GRASS_LIGHT);
                if (kind == GrassSeederBlockEntity.TILE_SHORT) {
                    int cx = tx + inner / 2, cy = ty + inner / 2;
                    graphics.fill(cx - 2, cy, cx - 1, cy + 3, TUFT);
                    graphics.fill(cx, cy - 2, cx + 1, cy + 3, TUFT);
                    graphics.fill(cx + 2, cy - 1, cx + 3, cy + 3, TUFT);
                }
            }
            case GrassSeederBlockEntity.TILE_TALL -> {
                // Overgrown: dark grass, crowded with tall blades.
                graphics.fill(tx, ty, tx + inner, ty + inner, TALL);
                for (int k = 1; k < inner - 1; k += 2) graphics.fill(tx + k, ty + 1 + (k + seed) % 3, tx + k + 1, ty + inner - 1, TALL_BLADE);
            }
            default -> graphics.fill(tx, ty, tx + inner, ty + inner, OTHER);
        }
    }

    /** The seeder itself, in the middle of the map: a little blue-steel box with its brass sprinkler. */
    private static void seeder(GuiGraphicsExtractor graphics, int tx, int ty, int size) {
        int inner = size - 1;
        graphics.fill(tx, ty, tx + inner, ty + inner, 0xFF3F4766);
        graphics.fill(tx + 1, ty + 1, tx + inner - 1, ty + inner - 1, 0xFF5D6687);
        int c = inner / 2;
        graphics.fill(tx + 1, ty + c, tx + inner - 1, ty + c + 1, 0xFFF2B24A);
        graphics.fill(tx + c, ty + 1, tx + c + 1, ty + inner - 1, 0xFFF2B24A);
    }

    /** The status lamp sprite: grey, green, amber, red. */
    private static int lamp(Status status) {
        return switch (status) {
            case NO_GROUND -> 0;
            case TENDING -> 1;
            case ALL_GREEN -> 1;
            case OVERGROWN -> 2;
            case NO_FUEL -> 3;
        };
    }

    private Component statusText() {
        return Component.translatable("message.barelyfunctionalcontraptions.grass_seeder.status." + menu.getStatus().name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String key = "gui.barelyfunctionalcontraptions.grass_seeder.";
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable(key + "charges", menu.getBoneCharges()), 43, menu.getBoneCharges() > 0 ? TEXT_COLOR : TEXT_DIM);
        centred(graphics, Component.translatable(key + "grass", menu.getGrass(), menu.getGrass() + menu.getDirt()), 54,
                menu.getDirt() == 0 && menu.getGrass() > 0 ? GREEN_TEXT : TEXT_COLOR);
        if (menu.getTallGrass() > 0) centred(graphics, Component.translatable(key + "tall", menu.getTallGrass()), 64, OVERGROWN_TEXT);
        else centred(graphics, Component.translatable(key + "tufts", menu.getShortGrass()), 64, TEXT_DIM);
        centred(graphics, Component.translatable(key + "grazing", menu.getGrazing()), 74, menu.getGrazing() > 0 ? TEXT_COLOR : TEXT_DIM);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y + 1, menu.getStatus().color, false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, INFO_CENTRE - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        String key = "gui.barelyfunctionalcontraptions.grass_seeder.";
        List<Component> lines = new ArrayList<>();
        int perBlock = GrassSeederBlockEntity.energyPerBlock(), water = GrassSeederBlockEntity.waterPerBlock();
        if (hoveredSlot != null) {
            if (hoveredSlot.index == 0 && !hoveredSlot.hasItem()) {
                lines.add(Component.translatable(key + "slot"));
                lines.add(Component.translatable(key + "slot.desc", com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(
                        com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.seederBoneMealBlocks)).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(ENERGY_X - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "energy", fmt(menu.getEnergy()), fmt(menu.getCapacity())).withColor(ENERGY_TEXT));
            lines.add(Component.translatable(key + "energy.desc", fmt(perBlock), water).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(WATER_X - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "water", fmt(menu.getWater()), fmt(menu.getTankCapacity())).withColor(WATER_TEXT));
            lines.add(Component.translatable(key + "water.desc", water).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(MAP_X, MAP_Y, MAP_SIZE, MAP_SIZE, mouseX, mouseY)) {
            int size = menu.getRadius() * 2 + 1;
            if (size <= GrassSeederBlockEntity.MAP_MAX) {
                int tile = MAP_SIZE / size, offset = (MAP_SIZE - tile * size) / 2;
                int col = (mouseX - leftPos - MAP_X - offset) / tile, row = (mouseY - topPos - MAP_Y - offset) / tile;
                if (col >= 0 && row >= 0 && col < size && row < size) {
                    String kind = row == size / 2 && col == size / 2 ? "seeder" : switch (GrassSeederBlockEntity.tile(menu.getMap(), row * size + col)) {
                        case GrassSeederBlockEntity.TILE_DIRT -> "dirt";
                        case GrassSeederBlockEntity.TILE_GRASS -> "grass";
                        case GrassSeederBlockEntity.TILE_SHORT -> "short";
                        case GrassSeederBlockEntity.TILE_TALL -> "tall";
                        default -> "other";
                    };
                    lines.add(Component.translatable(key + "tile." + kind));
                }
            }
            lines.add(Component.translatable(key + "map.desc", size, size).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(BAR_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "bar", menu.getGrass(), menu.getGrass() + menu.getDirt(), menu.getShortGrass()).withColor(GREEN_TEXT));
            if (menu.getTallGrass() > 0) lines.add(Component.translatable(key + "bar.tall", menu.getTallGrass()).withColor(OVERGROWN_TEXT));
            lines.add(Component.translatable(key + "bar.desc", fmt(menu.getTended())).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(104, 72, 64, 11, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "grazing.title", menu.getGrazing()));
            lines.add(Component.translatable(key + "grazing.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 150, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(menu.getStatus().color));
            lines.add(Component.translatable(key + "status." + menu.getStatus().name().toLowerCase(Locale.ROOT) + ".desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(TRIM_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isTrimming() ? key + "trim.on" : key + "trim.off"));
            lines.add(Component.translatable(key + "trim.desc").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(key + "trim.click").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(AREA_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(menu.isShowingArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static String fmt(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
