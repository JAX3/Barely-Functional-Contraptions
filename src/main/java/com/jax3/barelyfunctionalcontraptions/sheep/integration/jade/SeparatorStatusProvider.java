package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RedstoneMode;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Client half: shows the separator status, FE/t while processing and a non-default redstone mode. */
public enum SeparatorStatusProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("separator_status");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        SeparatorStatusDataProvider.INSTANCE.decodeFromData(accessor).ifPresent(data -> {
            MachineStatus status = MachineStatus.byId(data.status());
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.status", status.displayName().copy().withColor(status.color)));
            if (status == MachineStatus.PROCESSING && data.energyPerTick() > 0) {
                tooltip.add(Component.translatable("gui.barelyfunctionalcontraptions.energy_usage",
                        String.format(Locale.ROOT, "%,d", data.energyPerTick())).withStyle(ChatFormatting.GRAY));
            }
            RedstoneMode mode = RedstoneMode.byId(data.redstoneMode());
            if (mode != RedstoneMode.IGNORED) {
                tooltip.add(Component.translatable("gui.barelyfunctionalcontraptions.redstone", mode.displayName()).withStyle(ChatFormatting.GRAY));
            }
        });
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
