package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Crate Controller: how many crates are in its store, how much they hold and how full, empty crates left, and its
 * problems (missing crates, crates that could burst, bulging ones, crates in another store). From what the client
 * already knows of it (it syncs how its store is doing, for its front).
 */
public enum CrateControllerProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("crate_controller");
    private static final int TEXT = 0xF2E6D0, GREY = 0xAAAAAA, GOLD = 0xFFC94A, RED = 0xE35D5D, GREEN = 0x7CFF8A;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof CrateControllerBlockEntity be)) return;
        String key = "jade.barelyfunctionalcontraptions.crate_controller.";
        tooltip.add(Component.translatable(key + "crates", be.crateCount(), CrateControllerBlockEntity.maxCrates()).withColor(TEXT));
        tooltip.add(Component.translatable(key + "stored", String.format(Locale.ROOT, "%,d", be.stored()),
                String.format(Locale.ROOT, "%,d", be.capacity()), be.percent()).withColor(be.percent() >= 90 ? GOLD : TEXT));
        tooltip.add(Component.translatable(key + "empties", be.empties()).withColor(be.empties() == 0 && be.crateCount() > 0 ? GOLD : GREY));
        boolean fine = true;
        if (be.missing() > 0) {
            tooltip.add(Component.translatable(key + "missing", be.missing()).withColor(RED));
            fine = false;
        }
        if (be.risky() > 0) {
            tooltip.add(Component.translatable(key + "risky", be.risky()).withColor(RED));
            fine = false;
        }
        if (be.bulging() > 0) tooltip.add(Component.translatable(key + "bulging", be.bulging()).withColor(GOLD));
        if (be.taken() > 0) tooltip.add(Component.translatable(key + "taken", be.taken()).withColor(GREY));
        if (fine && be.crateCount() > 0) tooltip.add(Component.translatable(key + "fine").withColor(GREEN));
        tooltip.add(Component.translatable(key + "signal", be.comparatorSignal()).withColor(GREY));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
