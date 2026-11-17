package com.jax3.barelyfunctionalcontraptions.sheep.recipe;

import com.jax3.barelyfunctionalcontraptions.sheep.armor.WoollenArmor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe whose result takes the colour of the wool it's made from: one colour gives that colour, and mixed
 * wool blends the colours, as dyeing leather does. Otherwise it's an ordinary shaped recipe (same JSON, with
 * {@code "type": "barelyfunctionalcontraptions:woollen_armor"}), so the recipe book and JEI treat it as one.
 */
public final class WoollenArmorRecipe extends ShapedRecipe {
    private final ItemStackTemplate result;

    public WoollenArmorRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.result = result;
    }

    public static final MapCodec<WoollenArmorRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
    ).apply(i, WoollenArmorRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WoollenArmorRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            WoollenArmorRecipe::new);

    /** Typed as a shaped recipe's serializer, since this is a shaped recipe. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final RecipeSerializer<ShapedRecipe> SERIALIZER = (RecipeSerializer) new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack stack = super.assemble(input);
        List<DyeColor> colours = WoollenArmor.colours(input);
        if (!colours.isEmpty()) stack.set(DataComponents.DYED_COLOR, DyedItemColor.applyDyes((DyedItemColor) null, colours));
        return stack;
    }

    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return SERIALIZER;
    }
}
