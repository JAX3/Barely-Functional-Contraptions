package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparatingRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ResourcefulSheep.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ResourcefulSheep.MOD_ID);
    /** Custom ingredient types: {@code barelyfunctionalcontraptions:resource_wool}. */
    public static final DeferredRegister<net.neoforged.neoforge.common.crafting.IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES, ResourcefulSheep.MOD_ID);
    public static final java.util.function.Supplier<net.neoforged.neoforge.common.crafting.IngredientType<com.jax3.barelyfunctionalcontraptions.sheep.recipe.ResourceWoolIngredient>> RESOURCE_WOOL_INGREDIENT =
            INGREDIENT_TYPES.register("resource_wool", () -> com.jax3.barelyfunctionalcontraptions.sheep.recipe.ResourceWoolIngredient.TYPE);

    public static final DeferredRegister<RecipeBookCategory> BOOK_CATEGORIES = DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, ResourcefulSheep.MOD_ID);

    /** Explicit Mechanical Separator recipes ({@code "type": "barelyfunctionalcontraptions:separating"}). */
    public static final Supplier<RecipeType<SeparatingRecipe>> SEPARATING_TYPE = TYPES.register("separating",
            () -> RecipeType.simple(ResourcefulSheep.id("separating")));
    public static final Supplier<RecipeSerializer<SeparatingRecipe>> SEPARATING_SERIALIZER = SERIALIZERS.register("separating",
            () -> SeparatingRecipe.SERIALIZER);
    public static final Supplier<RecipeBookCategory> SEPARATING_CATEGORY = BOOK_CATEGORIES.register("separating", RecipeBookCategory::new);

    private ModRecipes() {}
}
