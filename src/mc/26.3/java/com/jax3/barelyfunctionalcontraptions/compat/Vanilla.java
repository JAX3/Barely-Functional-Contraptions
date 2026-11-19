package com.jax3.barelyfunctionalcontraptions.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Vanilla entity types and coloured blocks, which Minecraft 26.2 moved into EntityTypes and colour collections. */
public final class Vanilla {
    public static final EntityType<Sheep> SHEEP = EntityTypes.SHEEP;
    public static final EntityType<Pig> PIG = EntityTypes.PIG;
    public static final EntityType<Cow> COW = EntityTypes.COW;
    public static final EntityType<MushroomCow> MOOSHROOM = EntityTypes.MOOSHROOM;
    public static final EntityType<Husk> HUSK = EntityTypes.HUSK;
    public static final EntityType<Blaze> BLAZE = EntityTypes.BLAZE;
    public static final EntityType<IronGolem> IRON_GOLEM = EntityTypes.IRON_GOLEM;
    public static final EntityType<ArmorStand> ARMOR_STAND = EntityTypes.ARMOR_STAND;

    public static Item wool(DyeColor color) { return Items.WOOL.pick(color); }
    public static Item dye(DyeColor color) { return Items.DYE.pick(color); }
    public static Block stainedGlass(DyeColor color) { return Blocks.STAINED_GLASS.pick(color); }
    public static Block stainedGlassPane(DyeColor color) { return Blocks.STAINED_GLASS_PANE.pick(color); }

    private Vanilla() {}
}
