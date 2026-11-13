package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(ResourcefulSheep.MOD_ID);

    /**
     * A single item for every kind of infused wool. The {@link ModDataComponents#SHEEP_RESOURCE} component identifies
     * the resource, so data packs can add new wool types at runtime without registering new items.
     */
    public static final DeferredItem<ResourceWoolItem> RESOURCE_WOOL = REGISTER.registerItem("resource_wool", ResourceWoolItem::new);

    public static final DeferredItem<BlockItem> FUNCTIONAL_MACHINE_BLOCK = REGISTER.registerSimpleBlockItem(ModBlocks.FUNCTIONAL_MACHINE_BLOCK);

    public static final DeferredItem<BlockItem> MECHANICAL_SEPARATOR = REGISTER.registerSimpleBlockItem(ModBlocks.MECHANICAL_SEPARATOR);

    public static final DeferredItem<BlockItem> AUTOMATIC_SHEARS = REGISTER.registerSimpleBlockItem(ModBlocks.AUTOMATIC_SHEARS);

    public static final DeferredItem<BlockItem> FLUID_RESERVOIR = REGISTER.registerSimpleBlockItem(ModBlocks.FLUID_RESERVOIR);

    public static final DeferredItem<BlockItem> HUME_ENERGY_CUBE = REGISTER.registerSimpleBlockItem(ModBlocks.HUME_ENERGY_CUBE);
    public static final DeferredItem<BlockItem> FLUID_TANK = REGISTER.registerSimpleBlockItem(ModBlocks.FLUID_TANK);
    public static final DeferredItem<BlockItem> ZERO_POINT_ENERGY_MODULE = REGISTER.registerSimpleBlockItem(ModBlocks.ZERO_POINT_ENERGY_MODULE);
    public static final DeferredItem<BlockItem> ENERGY_CABLE = REGISTER.registerSimpleBlockItem(ModBlocks.ENERGY_CABLE);
    public static final DeferredItem<BlockItem> FLUID_PIPE = REGISTER.registerSimpleBlockItem(ModBlocks.FLUID_PIPE);
    public static final DeferredItem<BlockItem> ITEM_PIPE = REGISTER.registerSimpleBlockItem(ModBlocks.ITEM_PIPE);

    public static final DeferredItem<BlockItem> CREATIVE_GENERATOR = REGISTER.registerSimpleBlockItem(ModBlocks.CREATIVE_GENERATOR,
            p -> p.rarity(net.minecraft.world.item.Rarity.EPIC));

    public static final DeferredItem<BlockItem> ENERGY_METER = REGISTER.registerSimpleBlockItem(ModBlocks.ENERGY_METER);

    public static final DeferredItem<BlockItem> GLOW_PANEL = REGISTER.registerSimpleBlockItem(ModBlocks.GLOW_PANEL);
    public static final DeferredItem<BlockItem> GLOW_LAMP = REGISTER.registerSimpleBlockItem(ModBlocks.GLOW_LAMP);

    private ModItems() {}
}
