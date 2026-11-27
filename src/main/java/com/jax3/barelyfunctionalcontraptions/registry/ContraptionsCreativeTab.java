package com.jax3.barelyfunctionalcontraptions.registry;

import com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's creative tabs: machines and tools in the manual's sections, then all the resource wool, Wool Bales and resource sheep spawn eggs, each in its own section. */
public final class ContraptionsCreativeTab {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BarelyFunctionalContraptions.MOD_ID);

    private static final int ROW = 9;

    /**
     * A section of the main tab: a banner row with the guide tab's title and icon, then its items, padded with blanks
     * to the end of the row so the next section starts on a new row. The client draws the banners and lets them fold
     * the section away ({@code CreativeTabSections}).
     */
    public record Section(String key, String titleKey, int colour, Supplier<ItemStack> icon, Supplier<List<ItemStack>> items) {
        public Component title() {
            return Component.translatable(titleKey);
        }
    }

    /** A main tab section, titled after the Contraptions Manual tab of the same name. */
    private static Section guide(String key, int colour, Supplier<ItemStack> icon, Supplier<List<ItemStack>> items) {
        return new Section(key, "guide.barelyfunctionalcontraptions.tab." + key, colour, icon, items);
    }

    /** Puts the sections in a tab: each one's banner row, its items, then blanks to the end of the row. */
    private static void accept(CreativeModeTab.Output output, List<Section> sections) {
        int blanks = 0;
        for (Section section : sections) {
            for (int column = 0; column < ROW; column++) output.accept(placeholder(section.key(), column), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            List<ItemStack> items = section.items().get();
            items.forEach(output::accept);
            for (int i = items.size(); i % ROW != 0; i++) output.accept(placeholder("", blanks++), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }

    private static Supplier<List<ItemStack>> items(Supplier<?>... items) {
        return () -> java.util.Arrays.stream(items).map(i -> new ItemStack((net.minecraft.world.level.ItemLike) i.get())).toList();
    }

    /** The main tab's sections, as the Contraptions Manual sorts the machines. */
    public static final List<Section> SECTIONS = List.of(
            guide("power", 0xFFB8792A, () -> new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE.get()), items(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.GLOW_PANEL,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.SHEEP_TREADMILL,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.BLAZE_FURNACE,
                    com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIAL_GENERATOR,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.STATIC_GENERATOR,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.COPPER_COMB,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ZERO_POINT_ENERGY_MODULE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.THERMAL_REACTION_CHAMBER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ECHO_CHAMBER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_FIELD_GENERATOR,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CREATIVE_GENERATOR)),
            guide("logistics", 0xFF2E86A0, () -> new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ITEM_PIPE.get()), items(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ENERGY_CABLE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ENERGY_METER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_TANK,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_PIPE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ITEM_PIPE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOL_PACKER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CRATE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CRATE_CONTROLLER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CRATE_STRAP)),
            guide("production", 0xFF468F46, () -> new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MECHANICAL_SEPARATOR.get()), items(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.INFUSION_ALTAR,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CHILLER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.GRASS_SEEDER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLEECE_TRAWLER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.IMPLOSION_CHAMBER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MECHANICAL_SEPARATOR,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.AUTOMATIC_SHEARS,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_RESERVOIR,
                    // Upgrade cards for the processing machines
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.UPGRADE_BASE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.SPEED_UPGRADE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.EFFICIENCY_UPGRADE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CAPACITY_UPGRADE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.RANGE_UPGRADE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.YIELD_UPGRADE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MUFFLER_UPGRADE)),
            guide("utility", 0xFF7552AE, () -> new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()), items(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CONTRAPTIONS_MANUAL,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FUNCTIONAL_MACHINE_BLOCK,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.BLUE_STEEL,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.BLUE_STEEL_BLOCK,
                    com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HERD_COUNTER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.STATUS_PANEL,
                    // Woollen Boots (white; craft or dye them for other colours)
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_BOOTS)));

    /** The loaded sheep resources, or the built-in ones before a world (and its data packs) is loaded. */
    private static List<net.minecraft.resources.Identifier> resourceIds() {
        SheepResourceRegistry loaded = SheepResourceRegistry.client();
        return loaded.isEmpty() ? BUILT_IN_WOOL.stream().map(BarelyFunctionalContraptions::id).toList()
                : loaded.all().stream().map(SheepResource::id).toList();
    }

    /** The Resource Wool tab's sections. */
    public static final List<Section> WOOL_SECTIONS = List.of(
            new Section("wool", "itemGroup.barelyfunctionalcontraptions.section.wool", 0xFFB0862F,
                    () -> ResourceWoolItem.create(BarelyFunctionalContraptions.id("gold"), 1),
                    () -> resourceIds().stream().map(id -> ResourceWoolItem.create(id, 1)).toList()),
            new Section("bales", "itemGroup.barelyfunctionalcontraptions.section.bales", 0xFF8F6436,
                    () -> com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(BarelyFunctionalContraptions.id("gold"), 1),
                    () -> resourceIds().stream().map(id -> com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(id, 1)).toList()),
            new Section("eggs", "itemGroup.barelyfunctionalcontraptions.section.eggs", 0xFF3F8A72,
                    () -> com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem.create(BarelyFunctionalContraptions.id("gold"), 1),
                    () -> resourceIds().stream().map(id -> com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem.create(id, 1)).toList()));

    /** Each sectioned tab with its sections. */
    public static List<java.util.Map.Entry<Supplier<CreativeModeTab>, List<Section>>> sectionedTabs() {
        return List.of(java.util.Map.entry(MAIN, SECTIONS), java.util.Map.entry(RESOURCE_WOOL, WOOL_SECTIONS));
    }

    /** A banner (a section's key, one per column) or blank (an empty key) placeholder. */
    public static ItemStack placeholder(String section, int index) {
        ItemStack stack = new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.TAB_SECTION.get());
        stack.set(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.TAB_SECTION.get(), new TabSection(section, index));
        return stack;
    }

    /** The section placeholder on a stack, or null for any other stack. */
    public static @org.jspecify.annotations.Nullable TabSection section(ItemStack stack) {
        return stack.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.TAB_SECTION.get());
    }

    /** Built-in sheep resources, used to populate the tab before a world (and therefore the data pack) is loaded. */
    public static final List<String> BUILT_IN_WOOL = List.of("iron", "gold", "copper", "diamond", "emerald", "redstone", "lapis",
            "coal", "netherite", "stone", "wood", "quartz", "red_mushroom", "brown_mushroom");

    public static final Supplier<CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + BarelyFunctionalContraptions.MOD_ID))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> new ItemStack(ModBlocks.FUNCTIONAL_MACHINE_BLOCK.get()))
            // Split into the Contraptions Manual's sections, each under a banner row (see Section).
            .displayItems((params, output) -> accept(output, SECTIONS))
            .build());

    /**
     * Every resource wool type in its own tab, right after the main one, so the machines and tools aren't buried
     * under dozens of wools (more with data packs and the generate command).
     */
    public static final Supplier<CreativeModeTab> RESOURCE_WOOL = REGISTER.register("resource_wool", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + BarelyFunctionalContraptions.MOD_ID + ".resource_wool"))
            .withTabsBefore(BarelyFunctionalContraptions.id("main"))
            .icon(() -> ResourceWoolItem.create(BarelyFunctionalContraptions.id("gold"), 1))
            // Every resource's wool, then a bale of each, then a spawn egg of each (a sheep that comes already infused),
            // each section under its own banner.
            .displayItems((params, output) -> accept(output, WOOL_SECTIONS))
            .build());

    private ContraptionsCreativeTab() {}
}
