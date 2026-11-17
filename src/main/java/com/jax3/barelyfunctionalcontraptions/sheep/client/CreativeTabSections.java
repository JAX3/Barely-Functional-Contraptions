package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab;
import com.jax3.barelyfunctionalcontraptions.registry.TabSection;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jspecify.annotations.Nullable;

/**
 * The main creative tab's sections, drawn the way the Contraptions Manual sorts the machines: each section starts on
 * its own row under a coloured banner with its icon and title, and clicking the banner folds the section away (or
 * opens it again). The tab holds placeholder stacks for the banner rows and the blanks after each section
 * ({@link ContraptionsCreativeTab#SECTIONS}); here they're drawn over, kept out of tooltips, and can't be taken.
 * Folded sections are remembered until the game closes.
 */
public final class CreativeTabSections {
    /** The 9x5 grid of the creative screen's item picker. */
    private static final int ROW = 9, GRID = 45, SLOT = 18;
    private static final Map<String, Integer> COLOURS = Map.of(
            "power", 0xFFB8792A, "logistics", 0xFF2E86A0, "production", 0xFF468F46, "utility", 0xFF7552AE);
    private static final Set<String> folded = new HashSet<>();
    private static @Nullable Field scrollField;

    private CreativeTabSections() {}

    private static @Nullable TabSection section(ItemStack stack) {
        return stack.isEmpty() ? null : ContraptionsCreativeTab.section(stack);
    }

    /** Whether the open creative screen shows the main tab: only it holds section placeholders. */
    private static boolean showsSections(CreativeModeInventoryScreen screen) {
        for (ItemStack stack : screen.getMenu().items) {
            TabSection section = section(stack);
            if (section != null && section.isBanner()) return true;
        }
        return false;
    }

    /** The tab's stacks, leaving out what's under folded banners (the banners themselves always stay). */
    private static List<ItemStack> visible() {
        List<ItemStack> visible = new ArrayList<>();
        String current = "";
        for (ItemStack stack : ContraptionsCreativeTab.MAIN.get().getDisplayItems()) {
            TabSection section = section(stack);
            if (section != null && section.isBanner()) {
                current = section.section();
                visible.add(stack);
            } else if (!folded.contains(current)) {
                visible.add(stack);
            }
        }
        return visible;
    }

    private static float scroll(CreativeModeInventoryScreen screen) {
        try {
            if (scrollField == null) {
                scrollField = CreativeModeInventoryScreen.class.getDeclaredField("scrollOffs");
                scrollField.setAccessible(true);
            }
            return scrollField.getFloat(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    /** Keeps the grid in step with the folded sections: the screen refills it from the whole tab whenever it opens it. */
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen) || !showsSections(screen)) return;
        List<ItemStack> want = visible();
        var items = screen.getMenu().items;
        boolean same = items.size() == want.size();
        for (int i = 0; same && i < want.size(); i++) same = items.get(i) == want.get(i);
        if (same) return;
        items.clear();
        items.addAll(want);
        screen.getMenu().scrollTo(scroll(screen));
    }

    @SubscribeEvent
    public static void onForeground(ScreenEvent.Render.Foreground event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen) || !showsSections(screen)) return;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        List<Slot> slots = screen.getMenu().slots;
        for (int i = 0; i < Math.min(GRID, slots.size()); i += ROW) {
            Slot slot = slots.get(i);
            TabSection section = section(slot.getItem());
            if (section == null || !section.isBanner()) continue;
            ContraptionsCreativeTab.Section info = ContraptionsCreativeTab.SECTIONS.stream()
                    .filter(s -> s.key().equals(section.section())).findFirst().orElse(null);
            if (info == null) continue;
            int x = screen.getLeftPos() + slot.x - 1, y = screen.getTopPos() + slot.y - 1, w = ROW * SLOT, h = SLOT;
            boolean hovered = event.getMouseX() >= x && event.getMouseX() < x + w && event.getMouseY() >= y && event.getMouseY() < y + h;
            int colour = COLOURS.getOrDefault(info.key(), 0xFF6A6A6A);
            if (hovered) colour = blend(colour, 0xFFFFFFFF, 0.18F);
            graphics.fill(x, y, x + w, y + h, 0xFF1C1B22);                      // frame
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, colour);
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, blend(colour, 0xFFFFFFFF, 0.35F)); // lit top edge
            graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, blend(colour, 0xFF000000, 0.35F));
            graphics.fakeItem(info.icon().get(), x + 1, y + 1);
            graphics.text(font, info.title(), x + 20, y + 5, 0xFFFFFFFF, true);
            // The fold button: "-" when open, "+" when folded.
            int bx = x + w - 13;
            graphics.fill(bx, y + 4, bx + 10, y + 14, blend(colour, 0xFF000000, 0.45F));
            graphics.fill(bx + 2, y + 8, bx + 8, y + 10, 0xFFFFFFFF);
            if (folded.contains(info.key())) graphics.fill(bx + 4, y + 6, bx + 6, y + 12, 0xFFFFFFFF);
        }
    }

    private static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** Clicking a banner folds or opens its section; placeholders can't be picked up, moved or copied. */
    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;
        Slot slot = screen.getHoveredSlot();
        TabSection section = slot == null ? null : section(slot.getItem());
        if (section == null) return;
        event.setCanceled(true);
        if (section.isBanner() && event.getButton() == 0) {
            toggle(section.section());
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    /** Folds the section, or opens it if it's folded. */
    static void toggle(String key) {
        if (!folded.remove(key)) folded.add(key);
    }

    /** No dropping, hotbar-swapping or off-hand swapping a placeholder either. */
    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;
        Slot slot = screen.getHoveredSlot();
        if (slot == null || section(slot.getItem()) == null) return;
        var options = Minecraft.getInstance().options;
        boolean action = options.keyDrop.matches(event.getKeyEvent()) || options.keySwapOffhand.matches(event.getKeyEvent())
                || options.keyPickItem.matches(event.getKeyEvent());
        for (var key : options.keyHotbarSlots) action |= key.matches(event.getKeyEvent());
        if (action) event.setCanceled(true);
    }

    /** Placeholders have no tooltip. */
    @SubscribeEvent
    public static void onTooltip(RenderTooltipEvent.Pre event) {
        if (section(event.getItemStack()) != null) event.setCanceled(true);
    }
}
