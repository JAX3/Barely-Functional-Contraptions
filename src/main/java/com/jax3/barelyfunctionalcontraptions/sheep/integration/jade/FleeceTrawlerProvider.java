package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Fleece Trawler: its status (in its colour), the Tide sheep swimming in its pool, its pool, how far through the haul,
 * and its lure with the hauls it has left. From what the client already knows (its catch, lure and numbers are synced).
 */
public enum FleeceTrawlerProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("fleece_trawler");
    private static final int GREY = 0xAAAAAA, SEA = 0x5FE0C8;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof FleeceTrawlerBlockEntity be)) return;
        var report = MachineReports.report(be);
        if (report != null) tooltip.add(report.statusText().copy().withColor(report.level().color & 0xFFFFFF));
        String key = "gui.barelyfunctionalcontraptions.fleece_trawler.";
        tooltip.add(Component.translatable(key + "sheep", be.sheepCount(),
                com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.trawlerMaxSheep))
                .withColor(be.sheepCount() > 0 ? SEA : GREY));
        tooltip.add(Component.translatable(key + "pool", be.pool()).withColor(GREY));
        tooltip.add(Component.translatable(key + "winch", be.progressPercent()).withColor(GREY));
        var lure = be.getItem(FleeceTrawlerBlockEntity.LURE);
        if (!lure.isEmpty()) {
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.fleece_trawler.lure", lure.getCount(), lure.getHoverName()).withColor(SEA));
        }
        tooltip.add(Component.translatable(key + "hauls", be.hauls()).withColor(GREY));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
