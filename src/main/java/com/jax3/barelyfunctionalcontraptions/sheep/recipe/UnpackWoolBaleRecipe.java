package com.jax3.barelyfunctionalcontraptions.sheep.recipe;

import com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Shapeless crafting: one Wool Bale on its own anywhere in the grid gives back its 9 resource wool, of the same
 * resource. A special recipe because the result's wool depends on the bale's component.
 *
 * <pre>{@code { "type": "barelyfunctionalcontraptions:unpack_wool_bale" }}</pre>
 */
public final class UnpackWoolBaleRecipe extends CustomRecipe {
    public static final UnpackWoolBaleRecipe INSTANCE = new UnpackWoolBaleRecipe();
    public static final MapCodec<UnpackWoolBaleRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, UnpackWoolBaleRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    @SuppressWarnings("deprecation")
    public static final RecipeSerializer<UnpackWoolBaleRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** The one bale in the grid, or empty if there isn't exactly one bale and nothing else. */
    private static ItemStack bale(CraftingInput input) {
        if (input.ingredientCount() != 1) return ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) return WoolBaleItem.wool(stack).isEmpty() ? ItemStack.EMPTY : stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !bale(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack wool = WoolBaleItem.wool(bale(input));
        return wool.isEmpty() ? ItemStack.EMPTY : wool.copyWithCount(WoolBaleItem.WOOL_PER_BALE);
    }

    @Override
    public RecipeSerializer<UnpackWoolBaleRecipe> getSerializer() {
        return SERIALIZER;
    }
}
