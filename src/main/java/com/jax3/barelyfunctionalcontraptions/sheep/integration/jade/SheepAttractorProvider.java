package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepAttraction;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepAttractorBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** The Sheep Attractor: which machine it calls sheep for, and how far, or that it isn't on a sheep machine. */
public enum SheepAttractorProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("sheep_attractor");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var machine = SheepAttractorBlock.machine(accessor.getLevel(), accessor.getPosition());
        if (machine instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.sheep_attractor.fitted", be.getBlockState().getBlock().getName(),
                    SheepAttraction.range()).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.sheep_attractor.loose").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
