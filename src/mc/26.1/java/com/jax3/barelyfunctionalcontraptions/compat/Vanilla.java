package com.jax3.barelyfunctionalcontraptions.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.block.Block;

/** Vanilla entity types and coloured blocks, under the names Minecraft 26.2 later gave them. */
public final class Vanilla {
    public static final EntityType<Sheep> SHEEP = EntityType.SHEEP;
    public static final EntityType<Pig> PIG = EntityType.PIG;
    public static final EntityType<Cow> COW = EntityType.COW;
    public static final EntityType<MushroomCow> MOOSHROOM = EntityType.MOOSHROOM;
    public static final EntityType<Husk> HUSK = EntityType.HUSK;
    public static final EntityType<Blaze> BLAZE = EntityType.BLAZE;
    public static final EntityType<IronGolem> IRON_GOLEM = EntityType.IRON_GOLEM;
    public static final EntityType<ArmorStand> ARMOR_STAND = EntityType.ARMOR_STAND;

    public static Item wool(DyeColor color) { return item(color, "_wool"); }
    public static Item dye(DyeColor color) { return item(color, "_dye"); }
    public static Block stainedGlass(DyeColor color) { return block(color, "_stained_glass"); }
    public static Block stainedGlassPane(DyeColor color) { return block(color, "_stained_glass_pane"); }

    private static Item item(DyeColor color, String suffix) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getSerializedName() + suffix));
    }

    private static Block block(DyeColor color, String suffix) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(color.getSerializedName() + suffix));
    }

    private Vanilla() {}
}
