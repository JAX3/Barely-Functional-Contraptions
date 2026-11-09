package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class ModTags {
    /** Entities that can be fed resources. Contains {@code minecraft:sheep}; modded sheep can be added by pack makers. */
    public static final TagKey<EntityType<?>> INFUSABLE = TagKey.create(Registries.ENTITY_TYPE, ResourcefulSheep.id("infusable"));

    /** Items that infused wool replaces in shearing loot. Defaults to {@code #minecraft:wool}. */
    public static final TagKey<Item> REPLACEABLE_WOOL = TagKey.create(Registries.ITEM, ResourcefulSheep.id("replaceable_wool"));

    /** Mobs the Automatic Shears will shear. Defaults to {@code #barelyfunctionalcontraptions:infusable} (sheep). */
    public static final TagKey<EntityType<?>> AUTO_SHEARABLE = TagKey.create(Registries.ENTITY_TYPE, ResourcefulSheep.id("auto_shearable"));

    private ModTags() {}

    public static boolean isInfusable(net.minecraft.world.entity.Entity entity) {
        return entity.getType().builtInRegistryHolder().is(INFUSABLE);
    }
}
