package com.jax3.barelyfunctionalcontraptions.mycelial.fuel;

import com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * Mycelial Generator fuels as a NeoForge data map, {@code barelyfunctionalcontraptions:mycelial_fuel}, so data packs,
 * KubeJS and modpack tools can add fuels without editing the config:
 *
 * <pre>{@code
 * data/<namespace>/data_maps/item/mycelial_fuel.json
 * { "values": { "minecraft:crimson_fungus": { "burn_time": 300, "energy": 1.1 },
 *               "#c:mushrooms": { "burn_time": 400 } } }
 * }</pre>
 * Fuels in the config's {@code fuels} list come first: an item listed there uses the config's numbers.
 */
public final class MycelialFuelDataMap {
    /**
     * @param burnTime  ticks one item burns for
     * @param energy    multiplier on the base FE/t while it burns
     * @param water     multiplier on water use
     * @param mycelium  multiplier on how fast the mycelium chance grows
     * @param scaling   added to the network scaling multiplier
     */
    public record Fuel(int burnTime, double energy, double water, double mycelium, double scaling) {
        public static final Codec<Fuel> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("burn_time").forGetter(Fuel::burnTime),
                Codec.doubleRange(0, 1_000).optionalFieldOf("energy", 1.0).forGetter(Fuel::energy),
                Codec.doubleRange(0, 1_000).optionalFieldOf("water", 1.0).forGetter(Fuel::water),
                Codec.doubleRange(0, 1_000).optionalFieldOf("mycelium", 1.0).forGetter(Fuel::mycelium),
                Codec.doubleRange(-1_000, 1_000).optionalFieldOf("scaling", 0.0).forGetter(Fuel::scaling)
        ).apply(i, Fuel::new));

        /** The same fuel as a {@link FuelDefinition} for item {@code id}. */
        public FuelDefinition toDefinition(String id) {
            return new FuelDefinition(id, false, burnTime, energy, water, mycelium, scaling);
        }
    }

    public static final DataMapType<Item, Fuel> TYPE = DataMapType.builder(BarelyFunctionalContraptions.id("mycelial_fuel"), Registries.ITEM, Fuel.CODEC)
            .synced(Fuel.CODEC, false)
            .build();

    private MycelialFuelDataMap() {}
}
