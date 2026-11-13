package com.jax3.barelyfunctionalcontraptions.sheep.recipe;

import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
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
 * An ingredient that matches resource wool, so any recipe (crafting, the separator, other mods' machines, KubeJS) can
 * ask for one kind of wool without spelling out its data component:
 *
 * <pre>{@code
 * { "neoforge:ingredient_type": "barelyfunctionalcontraptions:resource_wool", "resource": "barelyfunctionalcontraptions:iron" }
 * { "neoforge:ingredient_type": "barelyfunctionalcontraptions:resource_wool", "category": "gem" }
 * { "neoforge:ingredient_type": "barelyfunctionalcontraptions:resource_wool" }
 * }</pre>
 * With {@code resource} it matches that resource's wool; with {@code category} the wool of every loaded resource in
 * that category; with neither, any resource wool.
 */
public record ResourceWoolIngredient(Optional<Identifier> resource, Optional<SheepResource.Category> category) implements ICustomIngredient {
    public static final MapCodec<ResourceWoolIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.optionalFieldOf("resource").forGetter(ResourceWoolIngredient::resource),
            SheepResource.Category.CODEC.optionalFieldOf("category").forGetter(ResourceWoolIngredient::category)
    ).apply(i, ResourceWoolIngredient::new));

    public static final IngredientType<ResourceWoolIngredient> TYPE = new IngredientType<>(CODEC);

    /** An ingredient for one resource's wool. */
    public static Ingredient of(Identifier resource) {
        return new ResourceWoolIngredient(Optional.of(resource), Optional.empty()).toVanilla();
    }

    @Override
    public boolean test(ItemStack stack) {
        if (!stack.is(ModItems.RESOURCE_WOOL.get())) return false;
        Identifier id = ResourceWoolItem.getResourceId(stack);
        if (id == null) return false;
        if (resource.isPresent() && !resource.get().equals(id)) return false;
        if (category.isPresent()) {
            return lookup(id).map(r -> r.category() == category.get()).orElse(false);
        }
        return true;
    }

    /** The resource from whichever side's registry is loaded (the server's, or the client's copy). */
    private static Optional<SheepResource> lookup(Identifier id) {
        Optional<SheepResource> found = SheepResourceRegistry.server().byId(id);
        return found.isPresent() ? found : SheepResourceRegistry.client().byId(id);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return Stream.of(ModItems.RESOURCE_WOOL.getDelegate());
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
        if (resource.isPresent()) return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(ResourceWoolItem.create(resource.get(), 1)));
        SheepResourceRegistry registry = SheepResourceRegistry.server().isEmpty() ? SheepResourceRegistry.client() : SheepResourceRegistry.server();
        var shown = registry.all().stream()
                .filter(r -> category.isEmpty() || r.category() == category.get())
                .map(r -> (SlotDisplay) new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(ResourceWoolItem.create(r.id(), 1))))
                .toList();
        return shown.isEmpty() ? ICustomIngredient.super.display() : new SlotDisplay.Composite(shown);
    }
}
