package com.jax3.barelyfunctionalcontraptions.sheep.items;

import com.jax3.barelyfunctionalcontraptions.sheep.upgrade.UpgradeType;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** An upgrade card: goes in a machine's upgrade slot (see {@code MachineUpgrades}). */
public class UpgradeItem extends Item {
    private final UpgradeType type;

    public UpgradeItem(UpgradeType type, Properties properties) {
        super(properties.stacksTo(16));
        this.type = type;
    }

    public UpgradeType type() {
        return type;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        String key = "item.barelyfunctionalcontraptions." + type.id() + "_upgrade.";
        String tip = "tooltip.barelyfunctionalcontraptions.upgrade.";
        switch (type) {
            case SPEED, EFFICIENCY -> {
                tooltip.accept(Component.translatable(tip + "time", percent(type.time())).withStyle(type.time() <= 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
                tooltip.accept(Component.translatable(tip + "energy", percent(type.energy())).withStyle(type.energy() <= 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            case CAPACITY -> tooltip.accept(Component.translatable(tip + "capacity", percent(type.capacity())).withStyle(ChatFormatting.GREEN));
            case RANGE -> tooltip.accept(Component.translatable(tip + "range", type.range()).withStyle(ChatFormatting.GREEN));
            case YIELD -> tooltip.accept(Component.translatable(tip + "yield", type.yield()).withStyle(ChatFormatting.GREEN));
            case MUFFLER -> tooltip.accept(Component.translatable(tip + "muffler").withStyle(ChatFormatting.GREEN));
        }
        tooltip.accept(Component.translatable(key + "desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(tip + switch (type) {
            case RANGE -> "machines_range";
            case YIELD -> "machines_yield";
            case MUFFLER -> "machines_muffler";
            default -> "machines";
        }).withStyle(ChatFormatting.DARK_GRAY));
        if (type != UpgradeType.MUFFLER) tooltip.accept(Component.translatable(tip + "second").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String percent(double factor) {
        return Math.round(factor * 100) + "%";
    }
}
