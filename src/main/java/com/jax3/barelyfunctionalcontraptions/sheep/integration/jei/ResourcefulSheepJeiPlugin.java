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
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new InfusionCategory(gui), new SeparationCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        SheepResourceRegistry resources = SheepResourceRegistry.client();
        registration.addRecipes(InfusionCategory.TYPE, resources.all().stream().map(InfusionDisplay::new).toList());
        registration.addRecipes(SeparationCategory.TYPE, buildSeparations(resources));

        List<ItemStack> wool = resources.all().stream().map(r -> ResourceWoolItem.create(r.id(), 1)).toList();
        if (!wool.isEmpty()) {
            registration.addItemStackInfo(wool, Component.translatable("jei.barelyfunctionalcontraptions.info.resource_wool"));
        }
        List<ItemStack> bales = resources.all().stream().map(r -> com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(r.id(), 1)).toList();
        if (!bales.isEmpty()) {
            registration.addItemStackInfo(bales, Component.translatable("jei.barelyfunctionalcontraptions.info.wool_bale"));
        }
        registration.addItemStackInfo(new ItemStack(ModItems.WOOL_PACKER.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.wool_packer"));
        registration.addItemStackInfo(new ItemStack(ModItems.MECHANICAL_SEPARATOR.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.separator"));
        registration.addItemStackInfo(new ItemStack(ModItems.CREATIVE_GENERATOR.get()), Component.translatable("jei.barelyfunctionalcontraptions.info.creative_generator"));
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

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiDevHook.runtime = jeiRuntime;
        hideOwnTagPages(jeiRuntime.getRecipeManager());
    }

    /**
     * JEI lists every tag as a "tag recipe" page (Block Tags, Item Tags...), so looking up a machine showed this mod's
     * internal tags, such as {@code #barelyfunctionalcontraptions:wireless_power_receivers}. Those are configuration
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

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(SeparationCategory.TYPE, ModItems.MECHANICAL_SEPARATOR.get());
        registration.addCraftingStation(InfusionCategory.TYPE, Items.SHEARS);
    }
}
