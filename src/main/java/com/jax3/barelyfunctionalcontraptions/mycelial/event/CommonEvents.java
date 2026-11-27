package com.jax3.barelyfunctionalcontraptions.mycelial.event;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelRegistry;
import com.jax3.barelyfunctionalcontraptions.mycelial.util.Matchers;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Keeps caches consistent with the config, tags and world lifecycle. */
public final class CommonEvents {
    private CommonEvents() {
    }

    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ServerConfig.SPEC) {
            invalidateCaches();
        }
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ServerConfig.SPEC) {
            MycelialPower.LOGGER.info("Mycelial Power server config reloaded; rebuilding the fuel list");
            invalidateCaches();
        }
    }

    private static void invalidateCaches() {
        FuelRegistry.invalidate();
        Matchers.invalidateAll();
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        FuelRegistry.invalidate();
        Matchers.invalidateAll();
    }
}
