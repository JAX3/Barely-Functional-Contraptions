package com.jax3.barelyfunctionalcontraptions.mycelial.gametest;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * Registers the GameTest functions (Minecraft 1.21.5+ data-driven GameTests). Each function is paired with a
 * test instance in {@code data/barelyfunctionalcontraptions/test_instance/<name>.json}, which sets the structure, the
 * environment and the time limit. Run them with {@code gradlew runGameTestServer} or {@code /test runall}.
 */
public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, MycelialPower.MOD_ID);

    static {
        FUNCTIONS.register("single_generator_produces_energy", () -> GeneratorGameTests::singleGeneratorProducesEnergy);
        FUNCTIONS.register("touching_generators_stand_alone", () -> GeneratorGameTests::touchingGeneratorsStandAlone);
        FUNCTIONS.register("generator_without_water_stops", () -> GeneratorGameTests::generatorWithoutWaterStops);
        FUNCTIONS.register("mycelium_converts_dirt", () -> GeneratorGameTests::myceliumConvertsDirt);
        FUNCTIONS.register("converted_block_sprouts_huge_mushroom", () -> GeneratorGameTests::convertedBlockSproutsHugeMushroom);
        FUNCTIONS.register("state_survives_save_and_load", () -> GeneratorGameTests::stateSurvivesSaveAndLoad);
    }

    private ModGameTests() {
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
    }
}
