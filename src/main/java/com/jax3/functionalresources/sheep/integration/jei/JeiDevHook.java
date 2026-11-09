package com.jax3.functionalresources.sheep.integration.jei;

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
        var wool = com.jax3.functionalresources.sheep.registry.ModItems.RESOURCE_WOOL.get();
        long listed = rt.getIngredientManager().getAllItemStacks().stream().filter(s -> s.is(wool)).count();
        var recipes = rt.getRecipeManager().createRecipeLookup(SeparationCategory.TYPE).get().toList();
        long woolRecipes = recipes.stream().filter(d -> d.inputs().stream().anyMatch(s -> s.is(wool))).count();
        var missing = com.jax3.functionalresources.sheep.resource.SheepResourceRegistry.client().all().stream()
                .filter(r -> rt.getIngredientManager().getAllItemStacks().stream()
                        .noneMatch(s -> r.id().equals(com.jax3.functionalresources.sheep.items.ResourceWoolItem.getResourceId(s))))
                .map(r -> r.id().toString()).toList();
        return "wool types loaded=" + com.jax3.functionalresources.sheep.resource.SheepResourceRegistry.client().size()
                + ", resource wool in JEI list=" + listed + ", separation recipes=" + recipes.size()
                + " (with resource wool input=" + woolRecipes + "), missing from JEI list=" + missing;
    }

    /** Opens the Resource Infusion ({@code infusion = true}) or Mechanical Separation category. */
    public static boolean showCategory(boolean infusion) {
        IJeiRuntime rt = runtime;
        if (rt == null) return false;
        rt.getRecipesGui().showTypes(java.util.List.of(infusion ? InfusionCategory.TYPE : SeparationCategory.TYPE));
        return true;
    }
}
