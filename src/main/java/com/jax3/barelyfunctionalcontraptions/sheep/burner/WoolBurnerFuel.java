package com.jax3.barelyfunctionalcontraptions.sheep.burner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * One Wool Burner fuel, loaded from {@code data/<namespace>/barelyfunctionalcontraptions/wool_burner_fuel/<name>.json}:
 *
 * <pre>{@code
 * { "ingredient": "#minecraft:wool", "burn_time": 200, "power": 30 }
 * { "ingredient": { "neoforge:ingredient_type": "barelyfunctionalcontraptions:resource_wool",
 *                   "resource": "barelyfunctionalcontraptions:coal" }, "burn_time": 400, "power": 120 }
 * }</pre>
 *
 * The {@code ingredient} is any recipe ingredient: an item, a tag, or the mod's {@code resource_wool} ingredient for one
 * kind of resource wool. When several fuels match an item, the one that makes the most energy in all is used.
 *
 * @param ingredient what burns
 * @param burnTime   ticks one item burns for
 * @param power      FE/t while it burns
 */
public record WoolBurnerFuel(Ingredient ingredient, int burnTime, int power) {
    public static final Codec<WoolBurnerFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(WoolBurnerFuel::ingredient),
            ExtraCodecs.POSITIVE_INT.fieldOf("burn_time").forGetter(WoolBurnerFuel::burnTime),
            ExtraCodecs.POSITIVE_INT.fieldOf("power").forGetter(WoolBurnerFuel::power)
    ).apply(i, WoolBurnerFuel::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WoolBurnerFuel> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WoolBurnerFuel::ingredient,
            ByteBufCodecs.VAR_INT, WoolBurnerFuel::burnTime,
            ByteBufCodecs.VAR_INT, WoolBurnerFuel::power,
            WoolBurnerFuel::new);

    /** FE one item gives in all. */
    public long totalEnergy() {
        return (long) burnTime * power;
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }
}
