package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparatingRecipe;
import com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI integration. Only loaded by JEI (via {@link JeiPlugin}), so the mod runs fine without JEI installed.
 *
 * <p>Recipes are built from the client copy of the sheep resource definitions (synced before JEI starts) and the
 * {@code barelyfunctionalcontraptions:separating} recipes received from the server, i.e. exactly the data the machine uses.</p>
 */
@JeiPlugin
public class ResourcefulSheepJeiPlugin implements IModPlugin {
    private static final Identifier UID = ResourcefulSheep.id("jei_resourceful_sheep");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Each resource wool variant is a distinct JEI ingredient.
        registration.registerFromDataComponentTypes(ModItems.RESOURCE_WOOL.get(), ModDataComponents.SHEEP_RESOURCE.get());
        registration.registerFromDataComponentTypes(ModItems.WOOL_BALE.get(), ModDataComponents.SHEEP_RESOURCE.get());
        registration.registerFromDataComponentTypes(ModItems.RESOURCE_SHEEP_SPAWN_EGG.get(), ModDataComponents.SHEEP_RESOURCE.get());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new InfusionCategory(gui), new AltarCategory(gui), new SeparationCategory(gui), new ThermalCategory(gui));
        registration.addRecipeCategories(new EchoCategory(gui), new StaticCategory(gui), new GrassSeederCategory(gui), new HerdCounterCategory(gui), new StatusPanelCategory(gui), new CrateCategory(gui), new CrateControllerCategory(gui), new FleeceTrawlerCategory(gui), new ImplosionCategory(gui));
        registration.addRecipeCategories(new HumeFieldCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        SheepResourceRegistry resources = SheepResourceRegistry.client();
        // By hand: every resource that can be fed by hand. At the altar: those that must be, or that take catalysts.
        registration.addRecipes(InfusionCategory.TYPE, resources.all().stream().filter(r -> !r.altar().altarOnly()).map(InfusionDisplay::new).toList());
        registration.addRecipes(AltarCategory.TYPE, resources.all().stream()
                .filter(r -> r.altar().altarOnly() || !r.altar().catalysts().isEmpty()).map(AltarDisplay::new).toList());
        registration.addRecipes(SeparationCategory.TYPE, buildSeparations(resources));

        List<ItemStack> wool = resources.all().stream().map(r -> ResourceWoolItem.create(r.id(), 1)).toList();
        if (!wool.isEmpty()) {
            registration.addItemStackInfo(wool, Component.translatable("jei.barelyfunctionalcontraptions.info.resource_wool"));
        }
        List<ItemStack> bales = resources.all().stream().map(r -> com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(r.id(), 1)).toList();
        if (!bales.isEmpty()) {
            registration.addItemStackInfo(bales, Component.translatable("jei.barelyfunctionalcontraptions.info.wool_bale"));
        }
        registration.addRecipes(mezz.jei.api.constants.RecipeTypes.CRAFTING, unpackingRecipes(resources));
        registration.addItemStackInfo(new ItemStack(ModItems.WOOL_PACKER.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.wool_packer"));
        // Upgrade cards: what each does and where it goes; and the machines that take them.
        for (var card : List.of(ModItems.UPGRADE_BASE, ModItems.SPEED_UPGRADE, ModItems.EFFICIENCY_UPGRADE, ModItems.CAPACITY_UPGRADE,
                ModItems.RANGE_UPGRADE, ModItems.YIELD_UPGRADE, ModItems.MUFFLER_UPGRADE)) {
            registration.addItemStackInfo(new ItemStack(card.get()), Component.translatable("jei.barelyfunctionalcontraptions.info." + card.getId().getPath()));
        }
        for (var machine : List.of(ModItems.MECHANICAL_SEPARATOR, ModItems.WOOL_PACKER, ModItems.INFUSION_ALTAR,
                ModItems.AUTOMATIC_SHEARS, ModItems.CHILLER)) {
            registration.addItemStackInfo(new ItemStack(machine.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.upgrade_slots." + machine.getId().getPath()));
        }
        registration.addItemStackInfo(new ItemStack(ModItems.MECHANICAL_SEPARATOR.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.separator"));
        registration.addItemStackInfo(new ItemStack(ModItems.CREATIVE_GENERATOR.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.creative_generator"));
        registration.addRecipes(ThermalCategory.TYPE, List.of(new ThermalDisplay()));
        registration.addItemStackInfo(new ItemStack(ModItems.THERMAL_REACTION_CHAMBER.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.thermal_reaction_chamber.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.thermal_reaction_chamber.2"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.thermal_reaction_chamber.3"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.thermal_reaction_chamber.4"));
        registration.addRecipes(StaticCategory.TYPE, List.of(new StaticDisplay()));
        registration.addItemStackInfo(new ItemStack(ModItems.STATIC_GENERATOR.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.static_generator.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.static_generator.2"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.static_generator.3"));
        registration.addItemStackInfo(new ItemStack(ModItems.COPPER_COMB.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.copper_comb"));
        registration.addRecipes(GrassSeederCategory.TYPE, List.of(new GrassSeederDisplay()));
        registration.addRecipes(ImplosionCategory.TYPE, List.of(
                new ImplosionDisplay(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE), new ItemStack(net.minecraft.world.item.Items.GRAVEL)),
                new ImplosionDisplay(new ItemStack(net.minecraft.world.item.Items.GRAVEL), new ItemStack(net.minecraft.world.item.Items.SAND))));
        registration.addRecipes(HerdCounterCategory.TYPE, List.of(new HerdCounterDisplay()));
        registration.addRecipes(StatusPanelCategory.TYPE, List.of(new StatusPanelDisplay()));
        registration.addRecipes(CrateCategory.TYPE, List.of(new CrateDisplay()));
        registration.addRecipes(CrateControllerCategory.TYPE, List.of(new CrateControllerDisplay()));
        List<FleeceTrawlerDisplay> trawler = new java.util.ArrayList<>();
        trawler.add(new FleeceTrawlerDisplay(null));
        resources.all().forEach(r -> trawler.add(new FleeceTrawlerDisplay(r)));
        registration.addRecipes(FleeceTrawlerCategory.TYPE, trawler);
        registration.addItemStackInfo(new ItemStack(ModItems.FLEECE_TRAWLER.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.fleece_trawler.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.fleece_trawler.2"));
        registration.addItemStackInfo(new ItemStack(ModItems.CRATE_CONTROLLER.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.crate_controller.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.crate_controller.2"));
        registration.addItemStackInfo(new ItemStack(ModItems.CRATE.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.wool_lined_crate.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.wool_lined_crate.2"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.wool_lined_crate.3"));
        registration.addItemStackInfo(new ItemStack(ModItems.CRATE_STRAP.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.crate_strap"));
        registration.addItemStackInfo(new ItemStack(ModItems.STATUS_PANEL.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.status_panel.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.status_panel.2"));
        registration.addItemStackInfo(new ItemStack(ModItems.HERD_COUNTER.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.herd_counter.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.herd_counter.2"));
        registration.addItemStackInfo(new ItemStack(ModItems.IMPLOSION_CHAMBER.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.implosion_chamber.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.implosion_chamber.2"));
        registration.addItemStackInfo(new ItemStack(ModItems.GRASS_SEEDER.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.grass_seeder.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.grass_seeder.2"));
        registration.addRecipes(EchoCategory.TYPE, List.of(new EchoDisplay()));
        registration.addItemStackInfo(new ItemStack(ModItems.ECHO_CHAMBER.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.echo_chamber.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.echo_chamber.2"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.echo_chamber.3"));
        registration.addRecipes(HumeFieldCategory.TYPE, List.of(new HumeFieldDisplay()));
        registration.addItemStackInfo(new ItemStack(ModItems.HUME_FIELD_GENERATOR.get()),
                Component.translatable("jei.barelyfunctionalcontraptions.info.hume_field_generator.1"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.hume_field_generator.2"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.hume_field_generator.3"),
                Component.translatable("jei.barelyfunctionalcontraptions.info.hume_field_generator.4"));
    }

    /**
     * Every wool type the separator can process (built-in, mushroom, command-generated and data-pack ones), sorted by
     * name, followed by explicit separating recipes such as plain wool to string.
     */
    static List<SeparationDisplay> buildSeparations(SheepResourceRegistry resources) {
        List<SeparationDisplay> displays = new ArrayList<>();
        List<RecipeHolder<SeparatingRecipe>> recipes = SeparationLookup.clientRecipes();
        List<SheepResource> sorted = resources.all().stream()
                .sorted(java.util.Comparator.comparing((SheepResource r) -> r.name().getString().toLowerCase(java.util.Locale.ROOT)))
                .toList();
        for (SheepResource resource : sorted) {
            ItemStack wool = ResourceWoolItem.create(resource.id(), 1);
            // An explicit recipe that matches this wool overrides the definition in the machine; show only that one.
            boolean overridden = recipes.stream().anyMatch(h -> h.value().ingredient().test(wool));
            if (!overridden) {
                displays.add(new SeparationDisplay(resource.id(), List.of(wool), resource.separation()));
            }
            // And its bale, right after it.
            ItemStack bale = com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(resource.id(), 1);
            if (recipes.stream().noneMatch(h -> h.value().ingredient().test(bale))) {
                displays.add(new SeparationDisplay(resource.id().withSuffix("_bale"), List.of(bale), resource.separation().baled()));
            }
        }
        for (RecipeHolder<SeparatingRecipe> holder : recipes) {
            List<ItemStack> inputs = Arrays.stream(holder.value().ingredient().items().map(ItemStack::new).toArray(ItemStack[]::new)).toList();
            displays.add(new SeparationDisplay(holder.id().identifier(), inputs, holder.value().toSeparation()));
        }
        return displays;
    }

    /**
     * The special bale-unpacking recipe can't list its inputs, so JEI gets one ordinary shapeless recipe per wool:
     * that wool's bale gives 9 of it.
     */
    static List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> unpackingRecipes(SheepResourceRegistry resources) {
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> recipes = new ArrayList<>();
        for (SheepResource resource : resources.all()) {
            var bale = net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(true, ModDataComponents.SHEEP_RESOURCE.get(), resource.id(), ModItems.WOOL_BALE.get());
            var wool = new net.minecraft.world.item.ItemStackTemplate(ModItems.RESOURCE_WOOL.get(),
                    com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.WOOL_PER_BALE,
                    net.minecraft.core.component.DataComponentPatch.builder().set(ModDataComponents.SHEEP_RESOURCE.get(), resource.id()).build());
            var recipe = new net.minecraft.world.item.crafting.ShapelessRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(false),
                    new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(net.minecraft.world.item.crafting.CraftingBookCategory.MISC, ""),
                    wool, List.of(bale));
            Identifier id = ResourcefulSheep.id("unpack_wool_bale/" + resource.id().getNamespace() + "/" + resource.id().getPath());
            recipes.add(new RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), recipe));
        }
        return recipes;
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiDevHook.runtime = jeiRuntime;
        hideOwnTagPages(jeiRuntime.getRecipeManager());
        // The creative tab's invisible section placeholder isn't an item anyone should see.
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                java.util.List.of(new net.minecraft.world.item.ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.TAB_SECTION.get())));
    }

    /**
     * JEI lists every tag as a "tag recipe" page (Block Tags, Item Tags...), so looking up a machine showed this mod's
     * internal tags (the ones data packs use to configure it). Those are configuration
     * for data packs, not something players need, so their pages are hidden. Vanilla and other mods' tags (like
     * {@code minecraft:mineable/pickaxe}) are shared by many blocks and are left alone.
     */
    static void hideOwnTagPages(mezz.jei.api.recipe.IRecipeManager recipes) {
        recipes.createRecipeCategoryLookup().includeHidden().get()
                .filter(category -> category.getRecipeType().getUid().getPath().startsWith("tag_recipes/"))
                .forEach(category -> hideOwnTags(recipes, category));
    }

    private static <T> void hideOwnTags(mezz.jei.api.recipe.IRecipeManager recipes, mezz.jei.api.recipe.category.IRecipeCategory<T> category) {
        java.util.List<T> own = recipes.createRecipeLookup(category.getRecipeType()).get()
                .filter(recipe -> {
                    net.minecraft.resources.Identifier id = category.getIdentifier(recipe); // the tag's id
                    return id != null && ResourcefulSheep.MOD_ID.equals(id.getNamespace());
                })
                .toList();
        if (!own.isEmpty()) recipes.hideRecipes(category.getRecipeType(), own);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiDevHook.runtime = null;
    }

    /** Keeps JEI's item list clear of the machines' upgrade panels. */
    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(com.jax3.barelyfunctionalcontraptions.sheep.client.MachineScreen.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<com.jax3.barelyfunctionalcontraptions.sheep.client.MachineScreen<?>>() {
                    @Override
                    public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(com.jax3.barelyfunctionalcontraptions.sheep.client.MachineScreen<?> screen) {
                        var area = screen.upgradePanelArea();
                        return area == null ? List.of() : List.of(area);
                    }
                });
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(SeparationCategory.TYPE, ModItems.MECHANICAL_SEPARATOR.get());
        registration.addCraftingStation(InfusionCategory.TYPE, Items.SHEARS);
        registration.addCraftingStation(InfusionCategory.TYPE, ModItems.INFUSION_ALTAR.get()); // tier 1 works there too
        registration.addCraftingStation(AltarCategory.TYPE, ModItems.INFUSION_ALTAR.get());
        registration.addCraftingStation(ThermalCategory.TYPE, ModItems.THERMAL_REACTION_CHAMBER.get());
        registration.addCraftingStation(EchoCategory.TYPE, ModItems.ECHO_CHAMBER.get());
        registration.addCraftingStation(HumeFieldCategory.TYPE, ModItems.HUME_FIELD_GENERATOR.get());
        registration.addCraftingStation(StaticCategory.TYPE, ModItems.STATIC_GENERATOR.get());
        registration.addCraftingStation(GrassSeederCategory.TYPE, ModItems.GRASS_SEEDER.get());
        registration.addCraftingStation(ImplosionCategory.TYPE, ModItems.IMPLOSION_CHAMBER.get());
        registration.addCraftingStation(HerdCounterCategory.TYPE, ModItems.HERD_COUNTER.get());
        registration.addCraftingStation(StatusPanelCategory.TYPE, ModItems.STATUS_PANEL.get());
        registration.addCraftingStation(CrateCategory.TYPE, ModItems.CRATE.get());
        registration.addCraftingStation(CrateControllerCategory.TYPE, ModItems.CRATE_CONTROLLER.get());
        registration.addCraftingStation(FleeceTrawlerCategory.TYPE, ModItems.FLEECE_TRAWLER.get());
    }
}
