package com.jax3.barelyfunctionalcontraptions.sheep.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The in-game guide (the Contraptions Manual). Six tabs along the top of the left panel (Power, Logistics, Production,
 * Utility, Recipes, WIP), the open tab's chapters below them with their icons, and the chapter's text on the right, wrapped and
 * split into pages, with page buttons below. Recipes chapters show their recipes as slots instead, with item tooltips.
 * Drawn in the mod's dark slate style.
 */
public class GuideScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int HEIGHT = 214;
    private static final int LIST_WIDTH = 118;
    private static final int ROW = 15;
    /** The tab buttons along the top of the left panel. */
    private static final int TAB_Y = 21, TAB_W = 17, TAB_H = 20;
    /** The open tab's name, then its chapter list. */
    private static final int TAB_NAME_Y = 46, LIST_Y = 58;
    /** Space for the chapter list, below the tab name. */
    private static final int LIST_HEIGHT = HEIGHT - LIST_Y - 6;
    private static final int TEXT_WIDTH = WIDTH - LIST_WIDTH - 24;
    private static final int LINES = 17;

    private final List<GuideBook.Chapter> chapters;
    private GuideBook.Tab tab = GuideBook.Tab.UTILITY;
    /** Index into {@link #chapters} of the open chapter. */
    private int chapter;
    private final List<Button> tabButtons = new ArrayList<>();
    private int page;
    private List<List<FormattedCharSequence>> pages = List.of();
    /** The open chapter's recipes split into pages, for a Recipes chapter. */
    private List<List<GuideRecipes.Recipe>> recipePages = List.of();
    /** Height of the right panel's page area. */
    private static final int PAGE_HEIGHT = LINES * 9;
    private Button back;
    private Button forward;

    public GuideScreen() {
        super(Component.translatable("item.barelyfunctionalcontraptions.contraptions_manual"));
        this.chapters = GuideBook.load();
        if (!chapters.isEmpty()) tab = chapters.getFirst().tab();
    }

    /** Indexes into {@link #chapters} of the open tab's chapters, in order. */
    private List<Integer> inTab() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) if (chapters.get(i).tab() == tab) result.add(i);
        return result;
    }

    /** One line of the chapter list: a chapter (its index in {@link #chapters}), or a group heading. */
    private record Row(int chapter, String heading) {}

    /** The open tab's list: its chapters, with a heading wherever the group changes (the Power tab's tiers). */
    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        String group = "";
        for (int index : inTab()) {
            String g = chapters.get(index).group();
            if (!g.isEmpty() && !g.equals(group)) rows.add(new Row(-1, g));
            group = g;
            rows.add(new Row(index, ""));
        }
        return rows;
    }

    /** Height of one list row: 15, or less so that a longer list still fits in the window. */
    private int row() {
        return Math.min(ROW, LIST_HEIGHT / Math.max(1, rows().size()));
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return (height - HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int x = left() + 6;
        tabButtons.clear();
        GuideBook.Tab[] tabs = GuideBook.Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            GuideBook.Tab t = tabs[i];
            tabButtons.add(addRenderableWidget(Button.builder(Component.empty(), b -> openTab(t))
                    .bounds(left() + 6 + i * (TAB_W + 1), top() + TAB_Y, TAB_W, TAB_H).build()));
        }
        List<Integer> list = inTab();
        List<Row> rows = rows();
        int y = top() + LIST_Y;
        for (int i = 0; i < rows.size(); i++) {
            int index = rows.get(i).chapter();
            if (index < 0) continue;
            addRenderableWidget(Button.builder(Component.literal(chapters.get(index).title()), b -> openChapter(index))
                    .bounds(x + 18, y + i * row(), LIST_WIDTH - 20, row() - 1).build());
        }
        int textLeft = left() + LIST_WIDTH + 12;
        back = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(textLeft, top() + HEIGHT - 22, 20, 16).build());
        forward = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left() + WIDTH - 32, top() + HEIGHT - 22, 20, 16).build());
        // Keep the open chapter if it's in this tab (a resize rebuilds the widgets), else open the tab's first one.
        if (!list.isEmpty() && !list.contains(chapter)) {
            chapter = list.getFirst();
            page = 0;
        }
        if (!chapters.isEmpty()) {
            pages = paginate();
            page = Math.min(page, Math.max(0, pages.size() - 1));
        }
        updateButtons();
    }

    /** Shows chapter {@code index} (of all chapters) from its first page, switching to its tab. */
    public void openChapter(int index) {
        chapter = Math.max(0, Math.min(index, chapters.size() - 1));
        page = 0;
        pages = paginate();
        if (!chapters.isEmpty() && chapters.get(chapter).tab() != tab) {
            tab = chapters.get(chapter).tab();
            rebuildWidgets();
        }
        updateButtons();
    }

    /** Shows the chapter titled {@code title}, if there is one; the open tab's chapter first, when two share a title. */
    public void openChapter(String title) {
        for (int i : inTab()) {
            if (chapters.get(i).title().equals(title)) {
                openChapter(i);
                return;
            }
        }
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).title().equals(title)) {
                openChapter(i);
                return;
            }
        }
    }

    /** Switches to {@code t} and opens its first chapter. */
    public void openTab(GuideBook.Tab t) {
        tab = t;
        List<Integer> list = inTab();
        if (!list.isEmpty()) {
            chapter = list.getFirst();
            page = 0;
        }
        rebuildWidgets();
        setFocused(null);
    }

    private void turn(int by) {
        page = Math.max(0, Math.min(pages.size() - 1, page + by));
        updateButtons();
    }

    private void updateButtons() {
        if (back == null) return;
        back.visible = page > 0;
        forward.visible = page < pages.size() - 1;
    }

    /**
     * Wraps the chapter's paragraphs to the text width and splits them into pages. A heading always starts on the
     * same page as at least a few lines of what follows it, and a list ends with a blank line.
     */
    private List<List<FormattedCharSequence>> paginate() {
        List<List<FormattedCharSequence>> result = new ArrayList<>();
        recipePages = List.of();
        if (chapters.isEmpty()) return result;
        if (chapters.get(chapter).hasRecipes()) {
            recipePages = paginateRecipes(chapters.get(chapter).recipes());
            // One empty text page per recipe page, so the page count and buttons work the same.
            for (int i = 0; i < recipePages.size(); i++) result.add(List.of());
            return result;
        }
        List<FormattedCharSequence> current = new ArrayList<>();
        boolean afterBullet = false;
        for (String paragraph : chapters.get(chapter).text()) {
            boolean heading = paragraph.startsWith("## ");
            boolean bullet = paragraph.startsWith("- ");
            if (afterBullet && !bullet && !current.isEmpty() && current.size() < LINES) current.add(FormattedCharSequence.EMPTY);
            afterBullet = bullet;
            Component text = heading
                    ? Component.literal(paragraph.substring(3)).withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA)
                    : bullet ? Component.literal("\u2022 " + paragraph.substring(2)) : Component.literal(paragraph);
            List<FormattedCharSequence> lines = font.split(text, TEXT_WIDTH);
            // Headings need room for themselves and a few lines after; other paragraphs move whole if they fit a page.
            int needed = heading ? lines.size() + 4 : Math.min(lines.size(), LINES);
            if (!current.isEmpty() && current.size() + needed > LINES) {
                result.add(current);
                current = new ArrayList<>();
            }
            for (FormattedCharSequence line : lines) {
                if (current.size() >= LINES) {
                    result.add(current);
                    current = new ArrayList<>();
                }
                current.add(line);
            }
            if (!bullet && current.size() < LINES) current.add(FormattedCharSequence.EMPTY);
        }
        if (!current.isEmpty()) result.add(current);
        return result;
    }

    /** As many whole recipes on each page as fit. */
    private static List<List<GuideRecipes.Recipe>> paginateRecipes(List<GuideRecipes.Recipe> recipes) {
        List<List<GuideRecipes.Recipe>> result = new ArrayList<>();
        List<GuideRecipes.Recipe> current = new ArrayList<>();
        int used = 0;
        for (GuideRecipes.Recipe recipe : recipes) {
            if (!current.isEmpty() && used + recipe.height() > PAGE_HEIGHT) {
                result.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(recipe);
            used += recipe.height();
        }
        if (!current.isEmpty()) result.add(current);
        return result;
    }

    /** Draws a page of recipes from {@code (left, top)}, and the tooltip of the item under the mouse. */
    private void drawRecipes(GuiGraphicsExtractor g, List<GuideRecipes.Recipe> recipes, int left, int top, int mouseX, int mouseY) {
        long tick = net.minecraft.util.Util.getMillis() / 1000;
        ItemStack hovered = ItemStack.EMPTY;
        int y = top;
        for (GuideRecipes.Recipe recipe : recipes) {
            g.text(font, recipe.title().copy().withStyle(ChatFormatting.BOLD), left, y, 0xFFE8D8FF, false);
            int bodyTop = y + 11;
            int bodyLeft = left + 4;
            for (GuideRecipes.Slot slot : recipe.slots()) {
                int sx = bodyLeft + slot.x(), sy = bodyTop + slot.y();
                g.fill(sx, sy, sx + GuideRecipes.SLOT, sy + GuideRecipes.SLOT, 0xFF0F1013);
                g.fill(sx + 1, sy + 1, sx + GuideRecipes.SLOT - 1, sy + GuideRecipes.SLOT - 1, 0xFF2C2F37);
                ItemStack stack = slot.shown(tick);
                if (stack.isEmpty()) continue;
                g.item(stack, sx + 1, sy + 1);
                g.itemDecorations(font, stack, sx + 1, sy + 1, slot.label());
                if (mouseX >= sx && mouseX < sx + GuideRecipes.SLOT && mouseY >= sy && mouseY < sy + GuideRecipes.SLOT) hovered = stack;
            }
            drawArrow(g, bodyLeft + recipe.arrowX(), bodyTop + recipe.arrowY());
            int noteY = bodyTop + recipe.bodyHeight() + 3;
            for (Component note : recipe.notes()) {
                for (FormattedCharSequence line : font.split(note, TEXT_WIDTH - 4)) {
                    g.text(font, line, bodyLeft, noteY, 0xFF9AA3C4, false);
                    noteY += 9;
                }
            }
            y += recipe.height();
        }
        if (!hovered.isEmpty()) g.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
    }

    /** A small brass arrow, 22 wide, pointing right. */
    private static void drawArrow(GuiGraphicsExtractor g, int x, int y) {
        int colour = 0xFFD9B44A;
        g.fill(x, y + 7, x + 16, y + 9, colour);
        for (int i = 0; i < 6; i++) g.fill(x + 16 + i, y + 2 + i, x + 17 + i, y + 14 - i, colour);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = left(), y = top();
        g.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFF0F1013);
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF3A3D45);
        g.fill(x + 4, y + 18, x + LIST_WIDTH + 2, y + HEIGHT - 4, 0xFF1B1C21);
        g.fill(x + 6, y + TAB_Y + TAB_H + 2, x + LIST_WIDTH, y + TAB_Y + TAB_H + 3, 0xFF3A3D45);
        g.fill(x + LIST_WIDTH + 6, y + 18, x + WIDTH - 4, y + HEIGHT - 4, 0xFF1B1C21);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int x = left(), y = top();
        g.text(font, title, x + 8, y + 6, 0xFFE8D8FF, false);
        // Tab icons, the open tab framed and its name below the tabs.
        GuideBook.Tab[] tabs = GuideBook.Tab.values();
        for (int i = 0; i < tabs.length && i < tabButtons.size(); i++) {
            Button b = tabButtons.get(i);
            g.item(tabs[i].icon(), b.getX() + (TAB_W - 16) / 2, b.getY() + (TAB_H - 16) / 2);
            if (tabs[i] == tab) g.outline(b.getX() - 1, b.getY() - 1, TAB_W + 2, TAB_H + 2, 0xFFFFE58A);
            // The tab's name while the mouse is over it (drawn here rather than as a button tooltip, which would stay
            // up on the clicked tab while it has focus).
            if (b.isMouseOver(mouseX, mouseY)) g.setTooltipForNextFrame(font, tabs[i].title(), mouseX, mouseY);
        }
        Component tabName = tab.title().copy().withStyle(ChatFormatting.BOLD);
        g.text(font, tabName, x + 4 + (LIST_WIDTH - 2 - font.width(tabName)) / 2, y + TAB_NAME_Y, 0xFFFFE58A, false);
        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int rowY = y + LIST_Y + i * row();
            if (r.chapter() >= 0) {
                g.item(chapters.get(r.chapter()).icon(), x + 6, rowY - 1);
            } else {
                // A group heading: its name centred between two rules.
                Component heading = Component.literal(r.heading());
                int w = font.width(heading), cx = x + 4 + (LIST_WIDTH - 2) / 2, ty = rowY + (row() - 9) / 2 + 1;
                g.fill(x + 8, ty + 3, cx - w / 2 - 3, ty + 4, 0xFF6A6F80);
                g.fill(cx + w / 2 + 3, ty + 3, x + LIST_WIDTH - 2, ty + 4, 0xFF6A6F80);
                g.text(font, heading, cx - w / 2, ty, 0xFF9AA3C4, false);
            }
        }
        if (chapters.isEmpty()) {
            g.text(font, Component.translatable("guide.barelyfunctionalcontraptions.missing"), x + LIST_WIDTH + 12, y + 24, 0xFFFF8080, false);
            return;
        }
        int textLeft = x + LIST_WIDTH + 12;
        g.text(font, Component.literal(chapters.get(chapter).title()).withStyle(ChatFormatting.BOLD), textLeft, y + 6, 0xFFFFE58A, false);
        if (!recipePages.isEmpty()) drawRecipes(g, recipePages.get(Math.min(page, recipePages.size() - 1)), textLeft, y + 22, mouseX, mouseY);
        if (!pages.isEmpty()) {
            List<FormattedCharSequence> lines = pages.get(page);
            for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), textLeft, y + 24 + i * 9, 0xFFDDE2EE, false);
            Component number = Component.translatable("book.pageIndicator", page + 1, pages.size());
            g.text(font, number, x + LIST_WIDTH + 6 + (WIDTH - LIST_WIDTH - 10 - font.width(number)) / 2, y + HEIGHT - 18, 0xFF9AA3C4, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
