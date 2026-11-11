package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Zero Point Energy Module: whether it is linked, how far away its partner is and the FE/t the pair makes (all of it available from either module). Uses the link
 * data the block entity already syncs for the beam; stored FE comes from Jade's own energy capability support.
 */
public enum ZeroPointModuleProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("zero_point_module");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof ZeroPointEnergyModuleBlockEntity be)) return;
        int distance = be.getLinkDistance();
        if (distance <= 0) {
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.zpe.unlinked").withStyle(ChatFormatting.RED));
            return;
        }
        boolean up = accessor.getBlockState().getValue(ZeroPointEnergyModuleBlock.FACING) == Direction.UP;
        tooltip.add(Component.translatable(up ? "jade.barelyfunctionalcontraptions.zpe.linked_above" : "jade.barelyfunctionalcontraptions.zpe.linked_below",
                distance - 1).withColor(0x3FC8FF));
        tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.zpe.output",
                String.format(Locale.ROOT, "%,d", be.getPairOutput())).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
