package com.jax3.barelyfunctionalcontraptions.mycelial.compat.jade;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.GeneratorState;
import com.jax3.barelyfunctionalcontraptions.mycelial.util.NumberFormatting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Client half: state, FE/t, burning fuel and network info. */
public enum GeneratorInfoProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = Identifier.fromNamespaceAndPath(MycelialPower.MOD_ID, "generator_info");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        GeneratorDataProvider.INSTANCE.decodeFromData(accessor).ifPresent(d -> {
            GeneratorState state = GeneratorState.byOrdinal(d.state());
            ChatFormatting stateColor = state == GeneratorState.RUNNING ? ChatFormatting.GREEN
                    : state == GeneratorState.ENERGY_FULL ? ChatFormatting.GOLD : ChatFormatting.RED;
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.state", Component.translatable(state.translationKey()).withStyle(stateColor)));
            if (state == GeneratorState.RUNNING) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.generation", NumberFormatting.compact(d.generation()))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (!d.fuelItem().isEmpty() && d.burnRemaining() > 0) {
                Identifier id = Identifier.tryParse(d.fuelItem());
                Component fuel = id == null ? Component.literal(d.fuelItem())
                        : BuiltInRegistries.ITEM.getOptional(id).map(item -> new ItemStack(item).getHoverName()).orElse(Component.literal(d.fuelItem()));
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.fuel", fuel, NumberFormatting.ticksToTime(d.burnRemaining()))
                        .withStyle(ChatFormatting.GRAY));
            }
        });
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
