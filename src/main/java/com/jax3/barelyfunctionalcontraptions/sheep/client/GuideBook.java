package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The guide's content: {@code assets/barelyfunctionalcontraptions/guide/<language>.json}, falling back to
 * {@code en_us.json}. A resource pack can replace or translate it. Format:
 *
 * <pre>{@code
 * { "chapters": [
 *     { "title": "Mycelial Generator", "icon": "barelyfunctionalcontraptions:mycelial_generator", "tab": "power",
 *       "text": ["A paragraph.", "## A heading", "- A list item"] } ] }
 * }</pre>
 * Paragraphs are wrapped and split into pages by the screen. A paragraph starting with {@code ## } is a heading.
 * {@code tab} is one of {@code power}, {@code logistics}, {@code productive} or {@code utility} (the default).
 */
public final class GuideBook {
    public record Chapter(String title, ItemStack icon, Tab tab, List<String> text) {}

    /** The guide's tabs, in the order they're shown. */
    public enum Tab {
        POWER, LOGISTICS, PRODUCTIVE, UTILITY;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public net.minecraft.network.chat.Component title() {
            return net.minecraft.network.chat.Component.translatable("guide.barelyfunctionalcontraptions.tab." + key());
        }

        /** The tab's icon. */
        public ItemStack icon() {
            return new ItemStack(switch (this) {
                case POWER -> com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE.get();
                case LOGISTICS -> com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ITEM_PIPE.get();
                case PRODUCTIVE -> com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MECHANICAL_SEPARATOR.get();
                case UTILITY -> com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get();
            });
        }

        /** The tab named {@code key} ("power"...), or Utility for anything else. */
        public static Tab byKey(String key) {
            for (Tab tab : values()) if (tab.key().equals(key)) return tab;
            return UTILITY;
        }
    }

    private GuideBook() {}

    public static List<Chapter> load() {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        List<Chapter> chapters = read(ResourcefulSheep.id("guide/" + language + ".json"));
        return chapters.isEmpty() ? read(ResourcefulSheep.id("guide/en_us.json")) : chapters;
    }

    private static List<Chapter> read(Identifier file) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return List.of();
        try (Reader reader = resource.get().openAsReader()) {
            return parse(JsonParser.parseReader(reader));
        } catch (Exception e) {
            ResourcefulSheep.LOGGER.warn("Couldn't read the guide {}: {}", file, e.toString());
            return List.of();
        }
    }

    /** Parses guide JSON; entries that aren't valid are skipped. */
    public static List<Chapter> parse(JsonElement json) {
        List<Chapter> chapters = new ArrayList<>();
        JsonArray array = json.getAsJsonObject().getAsJsonArray("chapters");
        for (JsonElement element : array) {
            JsonObject chapter = element.getAsJsonObject();
            String title = chapter.get("title").getAsString();
            ItemStack icon = Identifier.tryParse(chapter.has("icon") ? chapter.get("icon").getAsString() : "")
                    instanceof Identifier id ? BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(new ItemStack(Items.BOOK))
                    : new ItemStack(Items.BOOK);
            List<String> text = new ArrayList<>();
            for (JsonElement line : chapter.getAsJsonArray("text")) text.add(line.getAsString());
            Tab tab = Tab.byKey(chapter.has("tab") ? chapter.get("tab").getAsString() : "");
            chapters.add(new Chapter(title, icon, tab, List.copyOf(text)));
        }
        return chapters;
    }
}
