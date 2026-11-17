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

/** The mod's creative tabs: machines and tools in the manual's sections, then all the resource wool, Wool Bales and resource sheep spawn eggs. */
public final class ContraptionsCreativeTab {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BarelyFunctionalContraptions.MOD_ID);

    private static final int ROW = 9;

    /**
     * A section of the main tab: a banner row with the guide tab's title and icon, then its items, padded with blanks
     * to the end of the row so the next section starts on a new row. The client draws the banners and lets them fold
     * the section away ({@code CreativeTabSections}).
     */
    public record Section(String key, Supplier<ItemStack> icon, Supplier<List<ItemStack>> items) {
        /** The section's title: the Contraptions Manual tab of the same name. */
        public Component title() {
            return Component.translatable("guide.barelyfunctionalcontraptions.tab." + key);
        }
    }

    private static Supplier<List<ItemStack>> items(Supplier<?>... items) {
        return () -> java.util.Arrays.stream(items).map(i -> new ItemStack((net.minecraft.world.level.ItemLike) i.get())).toList();
    }

    /** The main tab's sections, as the Contraptions Manual sorts the machines. */
    public static final List<Section> SECTIONS = List.of(
            new Section("power", () -> new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE.get()), items(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.GLOW_PANEL,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.GLOW_LAMP,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOL_BURNER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.BLAZE_FURNACE,
                    com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIAL_GENERATOR,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ZERO_POINT_ENERGY_MODULE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CREATIVE_GENERATOR)),
            new Section("logistics", () -> new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ITEM_PIPE.get()), items(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ENERGY_CABLE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ENERGY_METER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_TANK,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_PIPE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.ITEM_PIPE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOL_PACKER)),
            new Section("production", () -> new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MECHANICAL_SEPARATOR.get()), items(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.INFUSION_ALTAR,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.POWERED_FURNACE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CHILLER,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MECHANICAL_SEPARATOR,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.AUTOMATIC_SHEARS,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_RESERVOIR)),
            new Section("utility", () -> new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()), items(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CONTRAPTIONS_MANUAL,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FUNCTIONAL_MACHINE_BLOCK,
                    com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH,
                    // Woollen Armour (white; craft or dye it for other colours)
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_HELMET,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_CHESTPLATE,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_LEGGINGS,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_BOOTS)));

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
            .displayItems((params, output) -> {
                // Split into the Contraptions Manual's sections, each under a banner row (see Section).
                int blanks = 0;
                for (Section section : SECTIONS) {
                    for (int column = 0; column < ROW; column++) output.accept(placeholder(section.key(), column), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    List<ItemStack> items = section.items().get();
                    items.forEach(output::accept);
                    for (int i = items.size(); i % ROW != 0; i++) output.accept(placeholder("", blanks++), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                }
            })
            .build());

    /**
     * Every resource wool type in its own tab, right after the main one, so the machines and tools aren't buried
     * under dozens of wools (more with data packs and the generate command).
     */
    public static final Supplier<CreativeModeTab> RESOURCE_WOOL = REGISTER.register("resource_wool", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + BarelyFunctionalContraptions.MOD_ID + ".resource_wool"))
            .withTabsBefore(BarelyFunctionalContraptions.id("main"))
            .icon(() -> ResourceWoolItem.create(BarelyFunctionalContraptions.id("gold"), 1))
            .displayItems((params, output) -> {
                SheepResourceRegistry loaded = SheepResourceRegistry.client();
                List<net.minecraft.resources.Identifier> ids = loaded.isEmpty()
                        ? BUILT_IN_WOOL.stream().map(BarelyFunctionalContraptions::id).toList()
                        : loaded.all().stream().map(SheepResource::id).toList();
                ids.forEach(id -> output.accept(ResourceWoolItem.create(id, 1)));
                // Then a bale of each.
                ids.forEach(id -> output.accept(com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(id, 1)));
                // Then a spawn egg of each, for a sheep that comes already infused.
                ids.forEach(id -> output.accept(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem.create(id, 1)));
            })
            .build());

    private ContraptionsCreativeTab() {}
}
