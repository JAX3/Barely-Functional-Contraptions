package com.jax3.barelyfunctionalcontraptions.sheep.armor;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Woollen Armour: a full set knitted from wool. Each piece takes the colour of the wool it was crafted from (mixed
 * wool blends, as dyes do on leather), can be dyed afterwards like leather, and keeps you from freezing in powder snow.
 * The boots give the Muffled effect while worn (see {@link MuffledEvents}).
 */
public final class WoollenArmor {
    /** The look on the body: {@code assets/barelyfunctionalcontraptions/equipment/wool.json}. */
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ResourcefulSheep.id("wool"));

    /** As tough as leather (it's soft), a little more enchantable, and mended with any wool. */
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(5, defense(1, 2, 3, 1, 3), 17,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ItemTags.WOOL, ASSET);

    /** The colour of undyed pieces (white wool's dye colour). */
    public static final int UNDYED = DyeColor.WHITE.getTextureDiffuseColor();

    private static Map<Item, DyeColor> woolColours;

    private WoollenArmor() {}

    private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet, int body) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    /** The dye colour of a vanilla coloured wool block, or null for anything else. */
    public static DyeColor woolColour(ItemStack stack) {
        if (woolColours == null) {
            Map<Item, DyeColor> map = new IdentityHashMap<>();
            for (DyeColor colour : DyeColor.values()) {
                map.put(Items.WOOL.pick(colour), colour);
            }
            woolColours = map;
        }
        return woolColours.get(stack.getItem());
    }

    /** The colours of all the wool in a crafting grid, one per block of wool (wool from other mods counts as white). */
    public static List<DyeColor> colours(CraftingInput input) {
        List<DyeColor> colours = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            DyeColor colour = woolColour(stack);
            colours.add(colour == null ? DyeColor.WHITE : colour);
        }
        return colours;
    }
}
