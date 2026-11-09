package com.jax3.barelyfunctionalcontraptions.sheep.command;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Decides which items {@code /barelyfunctionalcontraptions generate} may use.
 *
 * <ul>
 *     <li><b>Creative tabs</b> ({@code [generator] allowedCreativeTabs}, default Building Blocks, Ingredients and
 *     Food &amp; Drinks): the resource a wool type produces, i.e. its separator output, must be listed in one of these
 *     tabs. Storage blocks such as an Iron Block feed the sheep, and their Raw Iron output is an ingredient.</li>
 *     <li><b>Non-stacking blacklist</b> ({@code [generator] requireStackable}, default on): neither the feeding item
 *     nor the output may have a max stack size of 1, so tools, armour, buckets, potions and stews are never used.</li>
 * </ul>
 *
 * @param tabItems items found in the allowed tabs, or {@code null} when any item is allowed
 */
public record GeneratorItemFilter(@Nullable Set<Item> tabItems, List<Identifier> tabs, boolean requireStackable) {
    /** Allows everything (the pre-filter behaviour). */
    public static final GeneratorItemFilter ANY = new GeneratorItemFilter(null, List.of(), false);

    /** Builds the filter from the sheep config, using the creative tab contents for this server's features. */
    public static GeneratorItemFilter fromConfig(MinecraftServer server) {
        List<Identifier> tabs = new ArrayList<>();
        for (String s : ModConfigs.get(ModConfigs.SERVER.generatorAllowedTabs)) {
            Identifier id = Identifier.tryParse(s);
            if (id != null) tabs.add(id);
        }
        return create(server.registryAccess(), server.getWorldData().enabledFeatures(), tabs,
                ModConfigs.getBool(ModConfigs.SERVER.generatorRequireStackable));
    }

    public static GeneratorItemFilter create(HolderLookup.Provider registries, FeatureFlagSet features, List<Identifier> tabs, boolean requireStackable) {
        if (tabs.isEmpty()) return new GeneratorItemFilter(null, List.of(), requireStackable);
        // Fills every tab (vanilla plus the items mods add through NeoForge's BuildCreativeModeTabContentsEvent).
        // Cheap when the parameters have not changed since the last build.
        CreativeModeTabs.tryRebuildTabContents(features, false, registries);
        Set<Item> items = new HashSet<>();
        for (Identifier id : tabs) {
            Optional<CreativeModeTab> tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(id);
            if (tab.isEmpty()) {
                ResourcefulSheep.LOGGER.warn("[generator] allowedCreativeTabs: unknown creative tab '{}' ignored", id);
                continue;
            }
            for (ItemStack stack : tab.get().getDisplayItems()) items.add(stack.getItem());
        }
        return new GeneratorItemFilter(Set.copyOf(items), List.copyOf(tabs), requireStackable);
    }

    public static boolean isStackable(Item item) {
        return new ItemStack(item).getMaxStackSize() > 1;
    }

    /** May this item be fed to a sheep? Only the non-stacking blacklist applies. */
    public boolean allowsFeeding(Item item) {
        return !requireStackable || isStackable(item);
    }

    /** May this item be a wool type's resource (separator output)? Needs both the tab check and the stack check. */
    public boolean allowsOutput(Item item) {
        return allowsFeeding(item) && (tabItems == null || tabItems.contains(item));
    }

    /** Why {@code item} cannot be used as a feeding item, or empty if it can. */
    public Optional<Component> feedingRejection(Item item) {
        if (!allowsFeeding(item)) {
            return Optional.of(Component.translatable("command.barelyfunctionalcontraptions.filter.not_stackable", new ItemStack(item).getHoverName()));
        }
        return Optional.empty();
    }

    /** Why {@code item} cannot be a resource output, or empty if it can. */
    public Optional<Component> outputRejection(Item item) {
        Optional<Component> feeding = feedingRejection(item);
        if (feeding.isPresent()) return feeding;
        if (tabItems != null && !tabItems.contains(item)) {
            return Optional.of(Component.translatable("command.barelyfunctionalcontraptions.filter.wrong_tab", new ItemStack(item).getHoverName(), tabNames()));
        }
        return Optional.empty();
    }

    private Component tabNames() {
        var out = Component.empty();
        for (int i = 0; i < tabs.size(); i++) {
            if (i > 0) out.append(", ");
            Identifier id = tabs.get(i);
            out.append(BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(id)
                    .map(CreativeModeTab::getDisplayName).orElse(Component.literal(id.toString())));
        }
        return out;
    }
}
