package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu;
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
 * GUI of the Zero Point Energy Module, in the slate style of the other machines (laid out like the Hume Energy Cube's).
 * Left: the module's FE buffer. Middle: a diagram of the beam connection, with this module marked, its partner, and
 * whatever is stopping the link. Right: stored energy, generation and output. Bottom: a status lamp with the link
 * state. Art: {@code textures/gui/zero_point_energy_module.png}; see {@code tools/sheep/generate_textures.py}.
 */
public class ZeroPointModuleScreen extends AbstractContainerScreen<ZeroPointModuleMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/zero_point_energy_module.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    // Beam diagram: interior 47..74 x 17..82; modules are 16x8, centred on x 61.
    private static final int DIAGRAM_X = 46, DIAGRAM_Y = 16, DIAGRAM_W = 30, DIAGRAM_H = 68;
    private static final int MODULE_X = 53, TOP_Y = 18, BOTTOM_Y = 74, MODULE_U = 208;
    private static final int BEAM_X = 58, BEAM_U = 228, POINTER_U = 208, POINTER_V = 32;
    private static final int BLOCKER_U = 208, BLOCKER_V = 40, MISSING_U = 208, MISSING_V = 56;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, ENERGY_TEXT = 0xFF7FDBFF;
    private static final int GEN_COLOR = 0xFF55D66B, OUT_COLOR = 0xFFE8B23A, DASH_COLOR = 0xFF4A4E5A;
    private static final int INFO_CENTRE = 124;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;

    public ZeroPointModuleScreen(ZeroPointModuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = ZeroPointModuleMenu.INVENTORY_Y - 11;
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

        // Energy gauge.
        int height = menu.getAmount() > 0 ? Math.max(1, Mth.ceil(menu.getFillFraction() * GAUGE_H)) : 0;
        if (height > 0) {
            int offset = GAUGE_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, FILL_V + offset, GAUGE_W, height, TEX, TEX);
            if (height >= 2) {
                int frame = (int) (time / 3 % 4);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, SHIMMER_V + frame * 2, GAUGE_W, 2, TEX, TEX);
            }
        }

        extractDiagram(graphics, x, y, time);

        int lamp = statusLamp().ordinal();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + lamp * 8 % 64, LAMP_V + lamp / 8 * 8, 8, 8, TEX, TEX);
    }

    /**
     * The beam diagram. This module sits at the bottom of the column if it points up, at the top if it points down,
     * with a pointer beside it. The far end shows what the scan found: the partner (beam drawn between them), a
     * blocking block, a module facing the wrong way, a partner too close, or a dashed outline when nothing is there.
     */
    private void extractDiagram(GuiGraphicsExtractor graphics, int x, int y, long time) {
        boolean up = menu.facesUp();
        ZeroPointLink link = menu.getLink();
        boolean linked = link == ZeroPointLink.LINKED;
        int selfY = up ? BOTTOM_Y : TOP_Y;
        int farY = up ? TOP_Y : BOTTOM_Y;
        // The near end of the column (the emitter tip) and the far end, in GUI y.
        int nearEdge = up ? BOTTOM_Y : TOP_Y + 8;
        int farEdge = up ? TOP_Y + 8 : BOTTOM_Y;

        module(graphics, x, y, selfY, up, linked);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 47, y + selfY, POINTER_U, POINTER_V, 5, 7, TEX, TEX);

        switch (link) {
            case LINKED -> {
                module(graphics, x, y, farY, !up, true);
                int frame = (int) (time / 2 % 4);
                int top = Math.min(nearEdge, farEdge), length = Math.abs(nearEdge - farEdge);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BEAM_X, y + top, BEAM_U + frame * 6, 0, 6, length, TEX, TEX);
            }
            case BLOCKED -> {
                // Draw the block part of the way along: closer blocks lower down, always clear of both ends.
                float t = 0.4F + 0.45F * Mth.clamp((menu.getStoppedAt() - 1F) / Math.max(1, menu.getMaxRange() - 1), 0F, 1F);
                int blockCentre = Math.round(Mth.lerp(t, nearEdge, farEdge));
                dashed(graphics, x, y, nearEdge, blockCentre);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 55, y + blockCentre - 6, BLOCKER_U, BLOCKER_V, 12, 12, TEX, TEX);
            }
            case WRONG_FACING -> {
                dashed(graphics, x, y, nearEdge, farEdge);
                module(graphics, x, y, farY, up, false); // same way as this one
            }
            case TOO_CLOSE -> {
                int closeY = up ? selfY - 9 : selfY + 9; // right on top of (or under) this one
                module(graphics, x, y, closeY, !up, false);
            }
            case NO_MODULE -> {
                dashed(graphics, x, y, nearEdge, farEdge);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + MODULE_X, y + farY, MISSING_U, MISSING_V, 16, 8, TEX, TEX);
            }
        }
    }

    private void module(GuiGraphicsExtractor graphics, int x, int y, int moduleY, boolean pointsUp, boolean lit) {
        int v = (pointsUp ? 0 : 16) + (lit ? 8 : 0);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + MODULE_X, y + moduleY, MODULE_U, v, 16, 8, TEX, TEX);
    }

    /** A dotted line down the middle of the diagram, where the beam would be. */
    private static void dashed(GuiGraphicsExtractor graphics, int x, int y, int from, int to) {
        int top = Math.min(from, to), bottom = Math.max(from, to);
        for (int dy = top + 1; dy < bottom - 1; dy += 4) {
            graphics.fill(x + 60, y + dy, x + 62, y + Math.min(dy + 2, bottom), DASH_COLOR);
        }
    }

    private MachineStatus statusLamp() {
        if (menu.isBufferFull()) return MachineStatus.NO_ENERGY;
        return menu.getLink().lamp;
    }

    /** The status line beside the lamp. */
    private Component statusText() {
        ZeroPointLink link = menu.getLink();
        String dir = menu.facesUp() ? "above" : "below";
        return switch (link) {
            case LINKED -> menu.isBufferFull()
                    ? Component.translatable("gui.barelyfunctionalcontraptions.zpe.status.full")
                    : Component.translatable("gui.barelyfunctionalcontraptions.zpe.status.linked", menu.getLinkDistance() - 1);
            case BLOCKED -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.status.blocked." + dir, menu.getStoppedAt());
            case WRONG_FACING -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.status.wrong_facing");
            case TOO_CLOSE -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.status.too_close");
            case NO_MODULE -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.status.no_module." + dir, menu.getMaxRange());
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.stored", fe(menu.getAmount())), 21, ENERGY_TEXT);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.capacity", fe(menu.getCapacity())), 31, TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.zpe.generating", fe(menu.getGenerating())), 48, GEN_COLOR);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.zpe.pair", fe(menu.getPairOutput())), 58, TEXT_DIM);
        centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.cube.out", fe(menu.getOutRate())), 68, OUT_COLOR);
        graphics.text(font, statusText(), LAMP_X + 13, LAMP_Y + 1, statusLamp().color, false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, INFO_CENTRE - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty() || hoveredSlot != null) return;
        List<Component> lines = new ArrayList<>();
        String dir = menu.facesUp() ? "above" : "below";
        if (isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getAmount()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.cube.percent",
                    String.format(Locale.ROOT, "%.1f", menu.getFillFraction() * 100)).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(DIAGRAM_X, DIAGRAM_Y, DIAGRAM_W, DIAGRAM_H, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.zpe.beam.tooltip"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.zpe.beam.this." + (menu.facesUp() ? "up" : "down"))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(linkDescription(dir).withStyle(menu.getLink() == ZeroPointLink.LINKED ? ChatFormatting.AQUA : ChatFormatting.RED));
        } else if (isHovering(80, 44, 88, 34, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.zpe.generation.tooltip"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.zpe.generation.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LAMP_X, LAMP_Y - 1, 160, 10, mouseX, mouseY)) {
            lines.add(statusText().copy().withColor(statusLamp().color));
            lines.add(menu.isBufferFull()
                    ? Component.translatable("gui.barelyfunctionalcontraptions.zpe.status.full.desc").withStyle(ChatFormatting.GRAY)
                    : linkDescription(dir).withStyle(ChatFormatting.GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private net.minecraft.network.chat.MutableComponent linkDescription(String dir) {
        return switch (menu.getLink()) {
            case LINKED -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.desc.linked." + dir,
                    menu.getLinkDistance(), menu.getLinkDistance() - 1);
            case BLOCKED -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.desc.blocked." + dir, menu.getStoppedAt());
            case WRONG_FACING -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.desc.wrong_facing." + dir, menu.getStoppedAt());
            case TOO_CLOSE -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.desc.too_close");
            case NO_MODULE -> Component.translatable("gui.barelyfunctionalcontraptions.zpe.desc.no_module." + dir, menu.getMaxRange());
        };
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
