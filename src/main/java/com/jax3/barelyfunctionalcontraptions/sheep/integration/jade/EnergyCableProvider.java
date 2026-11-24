package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.util.Numbers;
import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Energy Cable: the network it belongs to and how much energy that network is moving. */
public enum EnergyCableProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("energy_cable");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        EnergyCableDataProvider.Data data = EnergyCableDataProvider.INSTANCE.decodeFromData(accessor).orElse(null);
        if (data == null) return;
        tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.cable.network", data.cables(), data.endpoints())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.cable.rate", Numbers.grouped(data.rate()), Numbers.grouped(data.maxRate()))
                .withColor(data.rate() > 0 ? 0x3FC8FF : 0xAAAAAA));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
