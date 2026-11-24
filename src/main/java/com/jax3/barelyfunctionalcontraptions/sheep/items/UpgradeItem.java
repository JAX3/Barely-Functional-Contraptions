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
        tooltip.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.upgrade.time", percent(type.time())).withStyle(type.time() <= 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.upgrade.energy", percent(type.energy())).withStyle(type.energy() <= 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.accept(Component.translatable(key + "desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.upgrade.machines").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String percent(double factor) {
        return Math.round(factor * 100) + "%";
    }
}
