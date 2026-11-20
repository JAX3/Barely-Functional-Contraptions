package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.compat.Vanilla;
import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Row;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.HerdCounterMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Window of the Herd Counter. Top left: its screen, the number its mode counts and the mode, a strip of pips for its
 * comparator signal under it, and a button for each mode. Right: the herd, a line of totals and a row for each resource
 * (most first): its wool, its name, how many and how many are ready to shear. Under it: the ghost filter slot and the
 * Full target, set with − and + (shift for 5). Top right: the show-area button. Art: {@code textures/gui/herd_counter.png},
 * see {@code tools/sheep/generate_textures.py}.
 */
public class HerdCounterScreen extends AbstractContainerScreen<HerdCounterMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/herd_counter.png");
    private static final int TEX = 256;
    private static final int SCREEN_X = 8, SCREEN_Y = 17, SCREEN_W = 58, SCREEN_H = 40;
    private static final int PIPS_X = 8, PIPS_Y = 61, PIPS_W = 58;
    private static final int MODE_X = 8, MODE_Y = 69, MODE_W = 18, MODE_H = 12, MODE_STEP_X = 20, MODE_STEP_Y = 14;
    private static final int LIST_X = 71, LIST_Y = 17, LIST_W = 98, ROW_Y = 28, ROW_H = 15, ROWS = 3;
    private static final int TARGET_DOWN_X = 138, TARGET_UP_X = 160, TARGET_Y = 79, TARGET_W = 10;
    private static final int AREA_X = 154, AREA_Y = 2, BUTTON_SIZE = 16, BUTTON_U = 176, BUTTON_V = 146;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, LIT = 0xFF7CFF8A, UNLIT = 0xFF2F6A3A, FILTERED = 0xFFFFD27F,
            BUTTON = 0xFF3A3F4E, BUTTON_HOVER = 0xFF4A5064, BUTTON_ON = 0xFF2F6A3A, READY = 0xFF8FE07A;

    public HerdCounterScreen(HerdCounterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = HerdCounterMenu.INVENTORY_Y - 11;
    }

    private @Nullable HerdCounterBlockEntity be() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof HerdCounterBlockEntity be ? be : null;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        addRenderableWidget(new AreaToggleButton(leftPos + AREA_X, topPos + AREA_Y, TEXTURE, BUTTON_U, BUTTON_V,
                () -> be() != null && be().showsArea(), HerdCounterBlockEntity.AREA_COLOR, menu.containerId, HerdCounterMenu.BUTTON_AREA));
        for (Mode mode : Mode.values()) {
            int i = mode.ordinal();
            addRenderableWidget(new SmallButton(leftPos + MODE_X + i % 3 * MODE_STEP_X, topPos + MODE_Y + i / 3 * MODE_STEP_Y, MODE_W, MODE_H,
                    Component.translatable("gui.barelyfunctionalcontraptions.herd_counter.button." + mode.key()),
                    () -> be() != null && be().mode() == mode, input -> press(mode.ordinal())));
        }
        addRenderableWidget(new SmallButton(leftPos + TARGET_DOWN_X, topPos + TARGET_Y, TARGET_W, 12, Component.literal("-"), () -> false,
                input -> press(input.hasShiftDown() ? HerdCounterMenu.BUTTON_TARGET_DOWN_5 : HerdCounterMenu.BUTTON_TARGET_DOWN)));
        addRenderableWidget(new SmallButton(leftPos + TARGET_UP_X, topPos + TARGET_Y, TARGET_W, 12, Component.literal("+"), () -> false,
                input -> press(input.hasShiftDown() ? HerdCounterMenu.BUTTON_TARGET_UP_5 : HerdCounterMenu.BUTTON_TARGET_UP)));
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        HerdCounterBlockEntity be = be();
        int signal = be == null ? 0 : be.comparatorSignal();
        int pip = PIPS_W / 15;
        for (int i = 0; i < 15; i++) {
            graphics.fill(x + PIPS_X + i * pip + 1, y + PIPS_Y, x + PIPS_X + i * pip + pip, y + PIPS_Y + 4, i < signal ? LIT : UNLIT);
        }
        if (be == null) return;
        // The herd: a row for each resource.
        List<Row> rows = be.rows();
        int shown = Math.min(ROWS, rows.size());
        for (int i = 0; i < shown; i++) {
            Row row = rows.get(i);
            int ry = y + ROW_Y + i * ROW_H;
            if (i == ROWS - 1 && rows.size() > ROWS) break; // the last row says how many more there are
            graphics.fakeItem(wool(row.key()), x + LIST_X + 1, ry - 1);
            // How many are ready to shear: a green bar under the name.
            int barW = 40, ready = Math.round(barW * row.ready() / (float) Math.max(1, row.count()));
            graphics.fill(x + LIST_X + 20, ry + 10, x + LIST_X + 20 + barW, ry + 12, 0xFF2A2C34);
            graphics.fill(x + LIST_X + 20, ry + 10, x + LIST_X + 20 + ready, ry + 12, READY);
        }
    }

    /** The wool a resource's row shows: its resource wool, or white wool for plain sheep. */
    private static ItemStack wool(String key) {
        if (HerdCounterBlockEntity.PLAIN.equals(key)) return new ItemStack(Vanilla.wool(DyeColor.WHITE));
        Identifier id = Identifier.tryParse(key);
        return id == null ? new ItemStack(Vanilla.wool(DyeColor.WHITE)) : ResourceWoolItem.create(id, 1);
    }

    private static Component name(String key) {
        if (HerdCounterBlockEntity.PLAIN.equals(key)) return Component.translatable("gui.barelyfunctionalcontraptions.herd_counter.plain");
        Identifier id = Identifier.tryParse(key);
        return SheepResourceRegistry.client().byId(id).map(SheepResource::name).orElse(Component.literal(key));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String key = "gui.barelyfunctionalcontraptions.herd_counter.";
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        HerdCounterBlockEntity be = be();
        if (be == null) return;
        // Its screen: the number, big, and the mode under it.
        boolean full = be.mode() == Mode.FULL;
        String number = full ? be.value() + "/" + be.target() : Integer.toString(be.value());
        int colour = full ? (be.value() >= be.target() ? LIT : UNLIT) : be.value() > 0 ? LIT : UNLIT;
        float scale = font.width(number) * 2 <= SCREEN_W - 6 ? 2F : 1.5F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(SCREEN_X + SCREEN_W / 2F, SCREEN_Y + 7);
        graphics.pose().scale(scale, scale);
        graphics.text(font, number, -font.width(number) / 2, 0, colour, false);
        graphics.pose().popMatrix();
        Component label = Component.translatable("screen.barelyfunctionalcontraptions.herd_counter." + be.mode().key());
        graphics.text(font, label, SCREEN_X + SCREEN_W / 2 - font.width(label) / 2, SCREEN_Y + 28, be.filterResource() != null ? FILTERED : colour, false);
        // The herd's totals, then a row for each resource.
        Component totals = Component.translatable(key + "totals", be.total(), be.ready());
        graphics.text(font, totals, LIST_X + 3, LIST_Y + 3, TEXT_COLOR, false);
        List<Row> rows = be.rows();
        if (rows.isEmpty()) {
            Component none = Component.translatable(key + "empty");
            graphics.text(font, none, LIST_X + LIST_W / 2 - font.width(none) / 2, ROW_Y + 16, TEXT_DIM, false);
        }
        for (int i = 0; i < Math.min(ROWS, rows.size()); i++) {
            int ry = ROW_Y + i * ROW_H;
            if (i == ROWS - 1 && rows.size() > ROWS) {
                Component more = Component.translatable(key + "more", rows.size() - ROWS + 1);
                graphics.text(font, more, LIST_X + LIST_W / 2 - font.width(more) / 2, ry + 3, TEXT_DIM, false);
                break;
            }
            Row row = rows.get(i);
            boolean filtered = row.key().equals(be.filterResource());
            graphics.text(font, font.plainSubstrByWidth(name(row.key()).getString(), 56), LIST_X + 20, ry + 1, filtered ? FILTERED : TEXT_COLOR, false);
            String count = "x" + row.count();
            graphics.text(font, count, LIST_X + LIST_W - 3 - font.width(count), ry + 1, TEXT_COLOR, false);
        }
        // The Full target, dim unless it's in Full mode.
        int dim = full ? TEXT_COLOR : TEXT_DIM;
        graphics.text(font, Component.translatable(key + "target"), 98, TARGET_Y + 2, dim, false);
        String target = Integer.toString(be.target());
        graphics.text(font, target, 154 - font.width(target) / 2 + 1, TARGET_Y + 2, full ? LIT : dim, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        HerdCounterBlockEntity be = be();
        if (be == null) return;
        String key = "gui.barelyfunctionalcontraptions.herd_counter.";
        List<Component> lines = new ArrayList<>();
        if (hoveredSlot != null && hoveredSlot.index == 0) {
            lines.add(Component.translatable(key + "filter"));
            lines.add(Component.translatable(be.filterResource() == null ? key + "filter.none" : key + "filter.set",
                    be.filterResource() == null ? "" : name(be.filterResource())).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(key + "filter.desc").withStyle(ChatFormatting.GRAY));
        } else if (hoveredSlot != null) {
            return;
        } else if (isHovering(SCREEN_X, SCREEN_Y, SCREEN_W, SCREEN_H, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "mode." + be.mode().key(), be.value(), be.target()).withColor(LIT));
            lines.add(Component.translatable(key + "mode." + be.mode().key() + ".desc", be.target()).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(PIPS_X, PIPS_Y - 1, PIPS_W, 6, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "signal", be.comparatorSignal()).withColor(LIT));
            lines.add(Component.translatable(key + "signal.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(MODE_X, MODE_Y, 3 * MODE_STEP_X, 2 * MODE_STEP_Y, mouseX, mouseY)) {
            int col = (mouseX - leftPos - MODE_X) / MODE_STEP_X, row = (mouseY - topPos - MODE_Y) / MODE_STEP_Y;
            Mode mode = Mode.byId(Math.min(5, row * 3 + col));
            lines.add(Component.translatable("message.barelyfunctionalcontraptions.herd_counter.mode." + mode.key()));
            lines.add(Component.translatable(key + "mode." + mode.key() + ".desc", be.target()).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LIST_X, LIST_Y, LIST_W, 10, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "totals.title", be.total()));
            lines.add(Component.translatable(key + "totals.desc", be.ready(), be.sheared(), be.lambs(), be.lowInfusion()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(key + "stats", fmt(be.averageYield()), fmt(be.averageGrowth()), fmt(be.averageEndurance()),
                    be.best().yield(), be.best().growth(), be.best().endurance()).withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(LIST_X, ROW_Y, LIST_W, ROWS * ROW_H, mouseX, mouseY)) {
            int i = (mouseY - topPos - ROW_Y) / ROW_H;
            List<Row> rows = be.rows();
            if (i >= 0 && i < rows.size() && !(i == ROWS - 1 && rows.size() > ROWS)) {
                Row row = rows.get(i);
                lines.add(name(row.key()).copy().append(Component.literal(" x" + row.count())));
                lines.add(Component.translatable(key + "row.ready", row.ready(), row.count()).withColor(READY));
                lines.add(Component.translatable(key + "row.stats", fmt(row.yield()), fmt(row.growth()), fmt(row.endurance())).withStyle(ChatFormatting.GRAY));
            } else if (rows.size() > ROWS) {
                for (int k = ROWS - 1; k < rows.size(); k++) lines.add(name(rows.get(k).key()).copy().append(Component.literal(" x" + rows.get(k).count())));
            }
        } else if (isHovering(96, TARGET_Y - 1, 74, 14, mouseX, mouseY)) {
            lines.add(Component.translatable(key + "target.title", be.target()));
            lines.add(Component.translatable(key + "target.desc").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(AREA_X, AREA_Y, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
            lines.add(Component.translatable(be.showsArea() ? "gui.barelyfunctionalcontraptions.area.shown" : "gui.barelyfunctionalcontraptions.area.hidden"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.area.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** A small flat button with a label, lit while {@code on}. */
    private static final class SmallButton extends AbstractButton {
        private final Supplier<Boolean> on;
        private final Consumer<InputWithModifiers> action;

        SmallButton(int x, int y, int width, int height, Component label, Supplier<Boolean> on, Consumer<InputWithModifiers> action) {
            super(x, y, width, height, label);
            this.on = on;
            this.action = action;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            action.accept(input);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = on.get();
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF17181C);
            graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, lit ? BUTTON_ON : isHoveredOrFocused() ? BUTTON_HOVER : BUTTON);
            var font = Minecraft.getInstance().font;
            graphics.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2 + 1, getY() + (height - 8) / 2 + 1,
                    lit ? LIT : TEXT_COLOR, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
