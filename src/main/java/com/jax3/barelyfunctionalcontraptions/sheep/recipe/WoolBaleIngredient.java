package com.jax3.barelyfunctionalcontraptions.sheep.recipe;

import com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * An ingredient that matches a Wool Bale, the way {@link ResourceWoolIngredient} matches resource wool:
 *
 * <pre>{@code
 * { "neoforge:ingredient_type": "barelyfunctionalcontraptions:wool_bale", "resource": "barelyfunctionalcontraptions:warden" }
 * { "neoforge:ingredient_type": "barelyfunctionalcontraptions:wool_bale" }
 * }</pre>
 * With {@code resource} it matches that resource's bale; without, any bale.
 */
public record WoolBaleIngredient(Optional<Identifier> resource) implements ICustomIngredient {
    public static final MapCodec<WoolBaleIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.optionalFieldOf("resource").forGetter(WoolBaleIngredient::resource)
    ).apply(i, WoolBaleIngredient::new));

    public static final IngredientType<WoolBaleIngredient> TYPE = new IngredientType<>(CODEC);

    /** An ingredient for one resource's bale. */
    public static Ingredient of(Identifier resource) {
        return new WoolBaleIngredient(Optional.of(resource)).toVanilla();
    }

    @Override
    public boolean test(ItemStack stack) {
        if (!stack.is(ModItems.WOOL_BALE.get())) return false;
        Identifier id = stack.get(ModDataComponents.SHEEP_RESOURCE.get());
        return id != null && (resource.isEmpty() || resource.get().equals(id));
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(ModItems.WOOL_BALE.getDelegate());
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    @Override
    public SlotDisplay display() {
        if (resource.isPresent()) return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(WoolBaleItem.create(resource.get(), 1)));
        SheepResourceRegistry registry = SheepResourceRegistry.server().isEmpty() ? SheepResourceRegistry.client() : SheepResourceRegistry.server();
        var shown = registry.all().stream()
                .map(r -> (SlotDisplay) new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(WoolBaleItem.create(r.id(), 1))))
                .toList();
        return shown.isEmpty() ? ICustomIngredient.super.display() : new SlotDisplay.Composite(shown);
    }
}
