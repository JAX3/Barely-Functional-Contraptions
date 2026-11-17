package com.jax3.barelyfunctionalcontraptions.gametest;

import net.neoforged.bus.api.IEventBus;

/**
 * Registers every GameTest (Mycelial Power, Resourceful Sheep and integration). Only called outside production, so
 * released jars, which leave the test code out (see {@code build.gradle}), never load it.
 */
public final class DevGameTests {
    private DevGameTests() {
    }

    public static void register(IEventBus modBus) {
        com.jax3.barelyfunctionalcontraptions.mycelial.gametest.ModGameTests.register(modBus);
        com.jax3.barelyfunctionalcontraptions.sheep.gametest.ModGameTests.FUNCTIONS.register(modBus);
        modBus.addListener(com.jax3.barelyfunctionalcontraptions.sheep.gametest.ModGameTests::registerTests);
        IntegrationGameTests.register(modBus);
    }
}
