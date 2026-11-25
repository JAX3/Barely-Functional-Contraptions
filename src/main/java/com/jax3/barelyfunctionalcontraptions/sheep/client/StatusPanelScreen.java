package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Level;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.Row;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.StatusPanelMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.network.RenamePanelLinkPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Window of the Machine Status Panel. A list of the machines linked to its screen, in the screen's order: each one's
 * lamp, name (the one it's been given, or its own), status, key number and how far away it is, with buttons to move it
 * up or down, switch its alert (whether its problems count for the comparator) and unlink it. Click a machine to name
 * it in the box under the list. Along the bottom: the sort order, the screen's colours, and what its comparator reads.
 * Drawn with flat fills, like its screen.
 */
public class StatusPanelScreen extends AbstractContainerScreen<StatusPanelMenu> {
    private static final int W = 256, H = 200;
    private static final int LIST_X = 6, LIST_Y = 18, LIST_W = 244, ROW_H = 20, VISIBLE = 7;
    private static final int BUTTONS_X = 172;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, LIST = 0xFF0D1118, SELECTED = 0xFF1E2A3A, HOVER = 0xFF161E2A;
    private static final int TEXT = 0xFFDFE4F5, DIM = 0xFF8A90A0, NUMBER = 0xFFBFE8FF, ALERT_ON = 0xFFE35D5D;

    private int scroll;
    private int selected = -1;
    private @Nullable EditBox name;

    public StatusPanelScreen(StatusPanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private @Nullable StatusPanelBlockEntity be() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof StatusPanelBlockEntity be ? be : null;
    }

    private List<Row> rows() {
        StatusPanelBlockEntity be = be();
        return be == null ? List.of() : be.rows();
    }

    private @Nullable Row rowAt(int slot) {
        List<Row> rows = rows();
        int i = scroll + slot;
        return i >= 0 && i < rows.size() ? rows.get(i) : null;
    }

    @Override
    protected void init() {
        super.init();
        String key = "gui.barelyfunctionalcontraptions.status_panel.";
        for (int slot = 0; slot < VISIBLE; slot++) {
            final int s = slot;
            int y = topPos + LIST_Y + slot * ROW_H;
            addRenderableWidget(new RowButton(leftPos + LIST_X, y, BUTTONS_X - LIST_X - 2, ROW_H, s));
            addRenderableWidget(new SmallButton(leftPos + BUTTONS_X, y + 4, 12, 12, Component.literal("▲"), () -> false, input -> act(s, StatusPanelMenu.UP)));
            addRenderableWidget(new SmallButton(leftPos + BUTTONS_X + 14, y + 4, 12, 12, Component.literal("▼"), () -> false, input -> act(s, StatusPanelMenu.DOWN)));
            addRenderableWidget(new SmallButton(leftPos + BUTTONS_X + 28, y + 4, 12, 12, Component.literal("!"),
                    () -> rowAt(s) != null && rowAt(s).alert(), input -> act(s, StatusPanelMenu.ALERT)));
            addRenderableWidget(new SmallButton(leftPos + BUTTONS_X + 42, y + 4, 12, 12, Component.literal("×"), () -> false, input -> act(s, StatusPanelMenu.UNLINK)));
        }
        name = new EditBox(font, leftPos + 8, topPos + 163, 150, 14, Component.translatable(key + "name"));
        name.setMaxLength(StatusPanelBlockEntity.MAX_NAME);
        name.setHint(Component.translatable(key + "name.hint").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(name);
        addRenderableWidget(new SmallButton(leftPos + 162, topPos + 163, 42, 14, Component.translatable(key + "save"), () -> false, input -> saveName()));
        addRenderableWidget(new SmallButton(leftPos + 206, topPos + 163, 44, 14, Component.translatable(key + "reset"), () -> false, input -> {
            if (name != null) name.setValue("");
            saveName();
        }));
        addRenderableWidget(new SmallButton(leftPos + 6, topPos + 181, 88, 14, Component.empty(), () -> false, input -> press(StatusPanelMenu.SORT)) {
            @Override
            public Component getMessage() {
                StatusPanelBlockEntity be = be();
                return Component.translatable(key + "sort." + (be == null ? "own" : be.sort().name().toLowerCase(Locale.ROOT)));
            }
        });
        addRenderableWidget(new SmallButton(leftPos + 96, topPos + 181, 54, 14, Component.empty(), () -> false, input -> press(StatusPanelMenu.THEME)) {
            @Override
            public Component getMessage() {
                StatusPanelBlockEntity be = be();
                return Component.translatable(key + "theme." + (be == null ? "classic" : be.theme().name().toLowerCase(Locale.ROOT)));
            }
        });
        addRenderableWidget(new SmallButton(leftPos + 152, topPos + 181, 98, 14, Component.empty(), () -> false, input -> press(StatusPanelMenu.SIGNAL)) {
            @Override
            public Component getMessage() {
                StatusPanelBlockEntity be = be();
                return Component.translatable(key + "signal." + (be == null ? "problems" : be.signal().name().toLowerCase(Locale.ROOT)));
            }
        });
        updateName();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /** A machine's button: the action, for the link the row in {@code slot} shows. */
    private void act(int slot, int action) {
        Row row = rowAt(slot);
        if (row == null) return;
        if (action == StatusPanelMenu.UNLINK && row.link() == selected) {
            selected = -1;
            updateName();
        } else if (action == StatusPanelMenu.UP || action == StatusPanelMenu.DOWN) {
            if (row.link() == selected) selected += action == StatusPanelMenu.UP ? -1 : 1;
        }
        press(action * StatusPanelMenu.ROW + row.link());
    }

    private void saveName() {
        if (selected < 0 || name == null) return;
        ClientPacketDistributor.sendToServer(new RenamePanelLinkPayload(menu.containerId, selected, name.getValue()));
    }

    /** The name box follows the selected machine. */
    private void updateName() {
        if (name == null) return;
        Row row = null;
        for (Row r : rows()) if (r.link() == selected) row = r;
        name.active = row != null;
        name.setEditable(row != null);
        name.setValue(row == null ? "" : row.custom());
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int max = Math.max(0, rows().size() - VISIBLE);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }

    /** While naming a machine, keys go to the name box (Enter saves); Escape still closes the window. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (name != null && name.canConsumeInput() && event.key() != 256) {
            if (event.key() == 257 || event.key() == 335) {
                saveName();
                return true;
            }
            name.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + W, y + H, EDGE);
        graphics.fill(x + 1, y + 1, x + W - 1, y + H - 1, PANEL);
        graphics.fill(x + LIST_X - 1, y + LIST_Y - 1, x + LIST_X + LIST_W + 1, y + LIST_Y + VISIBLE * ROW_H + 1, EDGE);
        graphics.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + VISIBLE * ROW_H, LIST);
        // The scroll bar.
        int total = rows().size();
        if (total > VISIBLE) {
            int barH = VISIBLE * ROW_H, thumb = Math.max(10, barH * VISIBLE / total);
            int top = (barH - thumb) * scroll / (total - VISIBLE);
            graphics.fill(x + LIST_X + LIST_W - 4, y + LIST_Y, x + LIST_X + LIST_W - 1, y + LIST_Y + barH, 0xFF22262E);
            graphics.fill(x + LIST_X + LIST_W - 4, y + LIST_Y + top, x + LIST_X + LIST_W - 1, y + LIST_Y + top + thumb, 0xFF6A7080);
        }
        // Rows past the end of the list hide their buttons.
        for (var child : children()) {
            if (child instanceof SmallButton b && b.slot >= 0) b.visible = rowAt(b.slot) != null;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String key = "gui.barelyfunctionalcontraptions.status_panel.";
        graphics.text(font, title, 8, 6, TEXT, false);
        StatusPanelBlockEntity be = be();
        if (be == null) return;
        Component count = Component.translatable(key + "count", be.rows().size(), be.capacity());
        graphics.text(font, count, W - 8 - font.width(count), 6, DIM, false);
        if (rows().isEmpty()) {
            Component none = Component.translatable(key + "empty");
            graphics.text(font, none, LIST_X + LIST_W / 2 - font.width(none) / 2, LIST_Y + 60, DIM, false);
        }
        for (int slot = 0; slot < VISIBLE; slot++) {
            Row row = rowAt(slot);
            if (row == null) continue;
            int y = LIST_Y + slot * ROW_H;
            int colour = row.report().level().color;
            graphics.fill(LIST_X + 4, y + 4, LIST_X + 9, y + 9, colour);
            String number = row.report().number();
            String distance = Component.translatable(key + "distance", (int) Math.round(Math.sqrt(row.pos().distSqr(menu.pos())))).getString();
            // The name and status get the room left of the number and distance, shrunk a little to fit if they need it.
            fitted(graphics, row.displayName().getString(), LIST_X + 13, y + 2, BUTTONS_X - 10 - font.width(number) - (LIST_X + 13),
                    row.custom().isEmpty() ? TEXT : 0xFFFFD27F);
            fitted(graphics, row.report().statusText().getString(), LIST_X + 13, y + 11, BUTTONS_X - 10 - font.width(distance) - (LIST_X + 13), colour);
            graphics.text(font, number, BUTTONS_X - 6 - font.width(number), y + 2, NUMBER, false);
            graphics.text(font, distance, BUTTONS_X - 6 - font.width(distance), y + 11, DIM, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        StatusPanelBlockEntity be = be();
        if (be == null) return;
        String key = "gui.barelyfunctionalcontraptions.status_panel.";
        List<Component> lines = new ArrayList<>();
        int localX = mouseX - leftPos, localY = mouseY - topPos;
        if (localY >= LIST_Y && localY < LIST_Y + VISIBLE * ROW_H && localX >= LIST_X && localX < LIST_X + LIST_W) {
            int slot = (localY - LIST_Y) / ROW_H;
            Row row = rowAt(slot);
            if (row != null) {
                int bx = localX - BUTTONS_X;
                if (bx >= 0 && bx < 56) {
                    String[] tips = {"up", "down", row.alert() ? "alert.on" : "alert.off", "unlink"};
                    lines.add(Component.translatable(key + "button." + tips[Math.min(3, bx / 14)]));
                    if (bx / 14 == 2) lines.add(Component.translatable(key + "button.alert.desc").withStyle(ChatFormatting.GRAY));
                } else {
                    lines.add(row.displayName().copy().withColor(TEXT));
                    if (!row.custom().isEmpty()) lines.add(Component.translatable(row.name()).withStyle(ChatFormatting.DARK_GRAY));
                    lines.add(row.report().statusText().copy().withColor(row.report().level().color)
                            .append(Component.literal("  " + row.report().number()).withColor(NUMBER)));
                    lines.add(Component.translatable(key + "at", row.pos().getX(), row.pos().getY(), row.pos().getZ()).withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable(key + "select").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        } else if (localY >= 181 && localY < 195) {
            String which = localX < 95 ? "sort" : localX < 151 ? "theme" : "signal";
            lines.add(Component.translatable(key + which + ".desc").withStyle(ChatFormatting.GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    /** A row of the list: highlighted when it's the selected machine; clicking it selects it for naming. */
    private final class RowButton extends AbstractButton {
        private final int slot;

        RowButton(int x, int y, int width, int height, int slot) {
            super(x, y, width, height, Component.empty());
            this.slot = slot;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            Row row = rowAt(slot);
            if (row == null) return;
            selected = row.link();
            updateName();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Row row = rowAt(slot);
            if (row == null) return;
            if (row.link() == selected) graphics.fill(getX(), getY(), getX() + width, getY() + height, SELECTED);
            else if (isHovered()) graphics.fill(getX(), getY(), getX() + width, getY() + height, HOVER);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** A small flat button with a label, lit while {@code on}; {@link #slot} is the list row it belongs to, or -1. */
    private class SmallButton extends AbstractButton {
        private final Supplier<Boolean> on;
        private final Consumer<InputWithModifiers> action;
        final int slot;

        SmallButton(int x, int y, int width, int height, Component label, Supplier<Boolean> on, Consumer<InputWithModifiers> action) {
            super(x, y, width, height, label);
            this.on = on;
            this.action = action;
            this.slot = y >= topPos + LIST_Y && y < topPos + LIST_Y + VISIBLE * ROW_H ? (y - topPos - LIST_Y) / ROW_H : -1;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            action.accept(input);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = on.get();
            graphics.fill(getX(), getY(), getX() + width, getY() + height, EDGE);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                    lit ? 0xFF4A2026 : isHoveredOrFocused() ? 0xFF4A5064 : 0xFF3A3F4E);
            var font = Minecraft.getInstance().font;
            Component message = getMessage();
            graphics.text(font, message, getX() + (width - font.width(message)) / 2 + 1, getY() + (height - 8) / 2 + 1, lit ? ALERT_ON : TEXT, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** Draws {@code text} in {@code maxWidth}: as it is if it fits, else smaller (down to three-quarters), else cut short. */
    private void fitted(GuiGraphicsExtractor graphics, String text, int x, int y, int maxWidth, int colour) {
        int width = font.width(text);
        if (width <= maxWidth) {
            graphics.text(font, text, x, y, colour, false);
            return;
        }
        float scale = Math.max(0.75F, maxWidth / (float) width);
        String shown = font.plainSubstrByWidth(text, (int) (maxWidth / scale));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y + (1 - scale) * 4);
        graphics.pose().scale(scale, scale);
        graphics.text(font, shown, 0, 0, colour, false);
        graphics.pose().popMatrix();
    }
}
