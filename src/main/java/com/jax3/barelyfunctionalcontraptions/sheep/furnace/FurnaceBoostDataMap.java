package com.jax3.barelyfunctionalcontraptions.sheep.furnace;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jspecify.annotations.Nullable;

/**
 * Items that boost a Blaze Sheep Furnace, as the data map {@code barelyfunctionalcontraptions:blaze_furnace_boost}:
 *
 * <pre>{@code
 * data/<namespace>/data_maps/item/blaze_furnace_boost.json
 * { "values": { "minecraft:blaze_rod": { "multiplier": 2.0, "duration": 600 } } }
 * }</pre>
 *
 * The furnace burns one boost item at a time, only while it has heat to boost, and multiplies its output for
 * {@code duration} ticks.
 */
public final class FurnaceBoostDataMap {
    /**
     * @param multiplier what the furnace's output is multiplied by while the item burns
     * @param duration   ticks one item lasts
     */
    public record Boost(double multiplier, int duration) {
        public static final Codec<Boost> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0, 100).optionalFieldOf("multiplier", 2.0).forGetter(Boost::multiplier),
                ExtraCodecs.POSITIVE_INT.fieldOf("duration").forGetter(Boost::duration)
        ).apply(i, Boost::new));
    }

    public static final DataMapType<Item, Boost> TYPE = DataMapType.builder(ResourcefulSheep.id("blaze_furnace_boost"), Registries.ITEM, Boost.CODEC)
            .synced(Boost.CODEC, false)
            .build();

    private FurnaceBoostDataMap() {}

    public static @Nullable Boost of(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(TYPE);
    }
}
