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
    public static final DeferredItem<BlockItem> BLAZE_FURNACE = REGISTER.registerSimpleBlockItem(ModBlocks.BLAZE_FURNACE);
    public static final DeferredItem<BlockItem> CHILLER = REGISTER.registerSimpleBlockItem(ModBlocks.CHILLER);
    public static final DeferredItem<BlockItem> POWERED_FURNACE = REGISTER.registerSimpleBlockItem(ModBlocks.POWERED_FURNACE);
    public static final DeferredItem<BlockItem> WOOL_BURNER = REGISTER.registerSimpleBlockItem(ModBlocks.WOOL_BURNER);

    public static final DeferredItem<BlockItem> WOOL_PACKER = REGISTER.registerSimpleBlockItem(ModBlocks.WOOL_PACKER);
    public static final DeferredItem<BlockItem> INFUSION_ALTAR = REGISTER.registerSimpleBlockItem(ModBlocks.INFUSION_ALTAR);
    /** Work in progress, development builds only (see {@link ModBlocks#HUME_FIELD_GENERATOR}); get it with /give. */
    public static final DeferredItem<BlockItem> HUME_FIELD_GENERATOR = ModBlocks.HUME_FIELD_GENERATOR == null ? null
            : REGISTER.registerSimpleBlockItem(ModBlocks.HUME_FIELD_GENERATOR);
    public static final DeferredItem<BlockItem> ECHO_CHAMBER = REGISTER.registerSimpleBlockItem(ModBlocks.ECHO_CHAMBER);
    public static final DeferredItem<BlockItem> STATIC_GENERATOR = REGISTER.registerSimpleBlockItem(ModBlocks.STATIC_GENERATOR);
    public static final DeferredItem<BlockItem> COPPER_COMB = REGISTER.registerSimpleBlockItem(ModBlocks.COPPER_COMB);
    public static final DeferredItem<BlockItem> GRASS_SEEDER = REGISTER.registerSimpleBlockItem(ModBlocks.GRASS_SEEDER);
    public static final DeferredItem<BlockItem> HERD_COUNTER = REGISTER.registerSimpleBlockItem(ModBlocks.HERD_COUNTER);
    public static final DeferredItem<BlockItem> STATUS_PANEL = REGISTER.registerSimpleBlockItem(ModBlocks.STATUS_PANEL);
    public static final DeferredItem<BlockItem> CRATE = REGISTER.registerSimpleBlockItem(ModBlocks.CRATE);
    public static final DeferredItem<BlockItem> CRATE_CONTROLLER = REGISTER.registerSimpleBlockItem(ModBlocks.CRATE_CONTROLLER);
    /** Straps a Wool-Lined Crate shut so it never bursts. */
    public static final DeferredItem<net.minecraft.world.item.Item> CRATE_STRAP = REGISTER.registerSimpleItem("crate_strap");
    public static final DeferredItem<BlockItem> SHEEP_TREADMILL = REGISTER.registerSimpleBlockItem(ModBlocks.SHEEP_TREADMILL);
    public static final DeferredItem<BlockItem> THERMAL_REACTION_CHAMBER = REGISTER.registerSimpleBlockItem(ModBlocks.THERMAL_REACTION_CHAMBER);

    /** 9 resource wool in one; like resource wool, the {@link ModDataComponents#SHEEP_RESOURCE} component says which. */
    public static final DeferredItem<com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem> WOOL_BALE = REGISTER.registerItem("wool_bale",
            p -> new com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem(ModBlocks.WOOL_BALE.get(), p.useBlockDescriptionPrefix()));

    /** A sheep spawn egg per resource; the {@link ModDataComponents#SHEEP_RESOURCE} component says which, and the sheep comes infused. */
    public static final DeferredItem<com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem> RESOURCE_SHEEP_SPAWN_EGG = REGISTER.registerItem("resource_sheep_spawn_egg",
            com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem::new);

    /** The in-game guide. */
    public static final DeferredItem<com.jax3.barelyfunctionalcontraptions.sheep.items.GuideBookItem> CONTRAPTIONS_MANUAL = REGISTER.registerItem("contraptions_manual",
            com.jax3.barelyfunctionalcontraptions.sheep.items.GuideBookItem::new, p -> p.stacksTo(1));

    /** Woollen Armour, in the colour of the wool it's crafted from; the boots muffle the wearer. */
    public static final DeferredItem<net.minecraft.world.item.Item> WOOLLEN_HELMET = woollen("woollen_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final DeferredItem<net.minecraft.world.item.Item> WOOLLEN_CHESTPLATE = woollen("woollen_chestplate", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final DeferredItem<net.minecraft.world.item.Item> WOOLLEN_LEGGINGS = woollen("woollen_leggings", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final DeferredItem<net.minecraft.world.item.Item> WOOLLEN_BOOTS = woollen("woollen_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);

    private static DeferredItem<net.minecraft.world.item.Item> woollen(String name, net.minecraft.world.item.equipment.ArmorType type) {
        return REGISTER.registerItem(name, p -> new net.minecraft.world.item.Item(
                p.humanoidArmor(com.jax3.barelyfunctionalcontraptions.sheep.armor.WoollenArmor.MATERIAL, type)));
    }

    /**
     * An invisible placeholder for the main creative tab's section banners and the blanks after each section; the
     * {@link ModDataComponents#TAB_SECTION} component says which. It's never meant to be held: the tab won't let you
     * pick it up, and JEI and the search tab leave it out.
     */
    public static final DeferredItem<net.minecraft.world.item.Item> TAB_SECTION = REGISTER.registerItem("tab_section",
            net.minecraft.world.item.Item::new, p -> p.stacksTo(1));

    private ModItems() {}
}
