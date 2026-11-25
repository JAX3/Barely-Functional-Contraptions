package com.jax3.barelyfunctionalcontraptions.mycelial.capability;

import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlockEntities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes the generator's energy, fluid and item handlers to other mods (cables, pipes, hoppers). */
public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        // On the machine frame: its energy, water and items, on every side.
        com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame.registerCapabilities(event, ModBlockEntities.MYCELIAL_GENERATOR.get());
    }
}
