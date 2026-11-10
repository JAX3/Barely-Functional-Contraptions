package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Used only by the dev screenshot harness to open JEI recipe pages. Never class-loaded unless JEI is installed. */
public final class JeiDevHook {
    static @Nullable IJeiRuntime runtime;

    private JeiDevHook() {}

    /** Shows the recipes producing ({@code output = true}) or using {@code stack}. Returns false if JEI is not ready. */
    public static boolean show(ItemStack stack, boolean output) {
        IJeiRuntime rt = runtime;
        if (rt == null) return false;
        var focus = rt.getJeiHelpers().getFocusFactory().createFocus(
                output ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, stack);
        rt.getRecipesGui().show(focus);
        return true;
    }

    /**
     * Diagnostic for the harness: how many resource wool variants JEI's ingredient list holds and how many
     * Mechanical Separation recipes it has, against the number of loaded wool types. Null if JEI is not ready.
     */
    public static @Nullable String separatorReport() {
        IJeiRuntime rt = runtime;
        if (rt == null) return null;
        var wool = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.RESOURCE_WOOL.get();
        long listed = rt.getIngredientManager().getAllItemStacks().stream().filter(s -> s.is(wool)).count();
        var recipes = rt.getRecipeManager().createRecipeLookup(SeparationCategory.TYPE).get().toList();
        long woolRecipes = recipes.stream().filter(d -> d.inputs().stream().anyMatch(s -> s.is(wool))).count();
        var missing = com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.client().all().stream()
                .filter(r -> rt.getIngredientManager().getAllItemStacks().stream()
                        .noneMatch(s -> r.id().equals(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.getResourceId(s))))
                .map(r -> r.id().toString()).toList();
        return "wool types loaded=" + com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.client().size()
                + ", resource wool in JEI list=" + listed + ", separation recipes=" + recipes.size()
                + " (with resource wool input=" + woolRecipes + "), missing from JEI list=" + missing;
    }

    /**
     * Diagnostic for the harness: this mod's tag pages still visible in JEI (should be none), and the tag pages a
     * Mechanical Separator still appears on. Null if JEI is not ready.
     */
    public static @Nullable String tagReport() {
        IJeiRuntime rt = runtime;
        if (rt == null) return null;
        var recipes = rt.getRecipeManager();
        var focus = rt.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK,
                new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MECHANICAL_SEPARATOR.get()));
        java.util.List<String> ownVisible = new java.util.ArrayList<>(), separatorTags = new java.util.ArrayList<>();
        recipes.createRecipeCategoryLookup().get()
                .filter(c -> c.getRecipeType().getUid().getPath().startsWith("tag_recipes/"))
                .forEach(c -> collect(recipes, c, focus, ownVisible, separatorTags));
        return "own tag pages visible=" + ownVisible + ", tag pages for the separator=" + separatorTags;
    }

    private static <T> void collect(mezz.jei.api.recipe.IRecipeManager recipes, mezz.jei.api.recipe.category.IRecipeCategory<T> category,
                                    mezz.jei.api.recipe.IFocus<?> focus, java.util.List<String> own, java.util.List<String> forSeparator) {
        recipes.createRecipeLookup(category.getRecipeType()).get().forEach(r -> {
            var id = category.getIdentifier(r);
            if (id != null && com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep.MOD_ID.equals(id.getNamespace())) own.add(id.toString());
        });
        recipes.createRecipeLookup(category.getRecipeType()).limitFocus(java.util.List.of(focus)).get()
                .forEach(r -> forSeparator.add(String.valueOf(category.getIdentifier(r))));
    }

    /** Opens the Resource Infusion ({@code infusion = true}) or Mechanical Separation category. */
    public static boolean showCategory(boolean infusion) {
        IJeiRuntime rt = runtime;
        if (rt == null) return false;
        rt.getRecipesGui().showTypes(java.util.List.of(infusion ? InfusionCategory.TYPE : SeparationCategory.TYPE));
        return true;
    }
}
