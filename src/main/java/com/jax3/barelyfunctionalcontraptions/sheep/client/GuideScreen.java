package com.jax3.barelyfunctionalcontraptions.sheep.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The in-game guide (the Contraptions Manual): chapters down the left with their icons, the chapter's text on the
 * right, wrapped and split into pages, with page buttons below. Drawn in the mod's dark slate style.
 */
public class GuideScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int HEIGHT = 214;
    private static final int LIST_WIDTH = 118;
    private static final int ROW = 15;
    private static final int TEXT_WIDTH = WIDTH - LIST_WIDTH - 24;
    private static final int LINES = 17;

    private final List<GuideBook.Chapter> chapters;
    private int chapter;
    private int page;
    private List<List<FormattedCharSequence>> pages = List.of();
    private Button back;
    private Button forward;

    public GuideScreen() {
        super(Component.translatable("item.barelyfunctionalcontraptions.contraptions_manual"));
        this.chapters = GuideBook.load();
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
        int y = top() + 22;
        for (int i = 0; i < chapters.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal(chapters.get(i).title()), b -> openChapter(index))
                    .bounds(x + 18, y + i * ROW, LIST_WIDTH - 20, ROW - 1).build());
        }
        int textLeft = left() + LIST_WIDTH + 12;
        back = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(textLeft, top() + HEIGHT - 22, 20, 16).build());
        forward = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left() + WIDTH - 32, top() + HEIGHT - 22, 20, 16).build());
        openChapter(Math.min(chapter, Math.max(0, chapters.size() - 1)));
    }

    /** Shows chapter {@code index} from its first page. */
    public void openChapter(int index) {
        chapter = index;
        page = 0;
        pages = paginate();
        updateButtons();
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
        if (chapters.isEmpty()) return result;
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

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = left(), y = top();
        g.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFF0F1013);
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF3A3D45);
        g.fill(x + 4, y + 18, x + LIST_WIDTH + 2, y + HEIGHT - 4, 0xFF1B1C21);
        g.fill(x + LIST_WIDTH + 6, y + 18, x + WIDTH - 4, y + HEIGHT - 4, 0xFF1B1C21);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int x = left(), y = top();
        g.text(font, title, x + 8, y + 6, 0xFFE8D8FF, false);
        for (int i = 0; i < chapters.size(); i++) {
            g.item(chapters.get(i).icon(), x + 6, y + 22 + i * ROW - 1);
        }
        if (chapters.isEmpty()) {
            g.text(font, Component.translatable("guide.barelyfunctionalcontraptions.missing"), x + LIST_WIDTH + 12, y + 24, 0xFFFF8080, false);
            return;
        }
        int textLeft = x + LIST_WIDTH + 12;
        g.text(font, Component.literal(chapters.get(chapter).title()).withStyle(ChatFormatting.BOLD), textLeft, y + 6, 0xFFFFE58A, false);
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
