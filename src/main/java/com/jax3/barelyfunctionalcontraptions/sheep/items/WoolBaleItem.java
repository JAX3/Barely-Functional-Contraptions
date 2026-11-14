package com.jax3.barelyfunctionalcontraptions.sheep.items;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.Separation;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * A Wool Bale: 9 of one resource wool pressed together by the Wool Packer. Like resource wool it is one item, with the
 * {@code barelyfunctionalcontraptions:sheep_resource} component saying which wool. It can be placed as a block (which
 * keeps the resource) and separated whole in the Mechanical Separator for more than its 9 wool would give.
 */
public class WoolBaleItem extends BlockItem {
    /** Wool in one bale. */
    public static final int WOOL_PER_BALE = 9;

    public WoolBaleItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack create(Identifier resource, int count) {
        ItemStack stack = new ItemStack(ModItems.WOOL_BALE.get(), count);
        stack.set(ModDataComponents.SHEEP_RESOURCE.get(), resource);
        return stack;
    }

    /** The bale of the wool in {@code wool} (any count), or empty if it isn't resource wool. */
    public static ItemStack of(ItemStack wool) {
        Identifier id = wool.getItem() instanceof ResourceWoolItem ? ResourceWoolItem.getResourceId(wool) : null;
        return id == null ? ItemStack.EMPTY : create(id, 1);
    }

    /** The wool a bale is made of (one piece), or empty. */
    public static ItemStack wool(ItemStack bale) {
        Identifier id = bale.getItem() instanceof WoolBaleItem ? bale.get(ModDataComponents.SHEEP_RESOURCE.get()) : null;
        return id == null ? ItemStack.EMPTY : ResourceWoolItem.create(id, 1);
    }

    public static Optional<SheepResource> getResource(ItemStack stack, boolean clientSide) {
        if (!(stack.getItem() instanceof WoolBaleItem)) return Optional.empty();
        return SheepResourceRegistry.get(clientSide).byId(stack.get(ModDataComponents.SHEEP_RESOURCE.get()));
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier id = stack.get(ModDataComponents.SHEEP_RESOURCE.get());
        if (id == null) return Component.translatable("block.barelyfunctionalcontraptions.wool_bale");
        return Component.translatable("block.barelyfunctionalcontraptions.wool_bale.named", ResourceWoolItem.create(id, 1).getHoverName());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        @Nullable Identifier id = stack.get(ModDataComponents.SHEEP_RESOURCE.get());
        Optional<SheepResource> resource = id == null ? Optional.empty()
                : SheepResourceRegistry.client().byId(id).or(() -> SheepResourceRegistry.server().byId(id));
        if (resource.isEmpty()) {
            builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.wool.no_resource").withStyle(ChatFormatting.RED));
            return;
        }
        builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.wool_bale.contains", WOOL_PER_BALE,
                ResourceWoolItem.create(id, 1).getHoverName().copy().withColor(resource.get().woolColor())).withStyle(ChatFormatting.GRAY));
        boolean detailed = ModConfigs.getBool(ModConfigs.CLIENT.detailedWoolTooltips) || flag.hasShiftDown();
        if (!detailed) {
            builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.hold_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        Separation s = resource.get().separation().baled();
        builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.wool.separates_into").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("  ").append(Component.translatable("tooltip.barelyfunctionalcontraptions.output_line",
                s.primaryLabel(), s.primary().create().getHoverName())).withStyle(ChatFormatting.DARK_AQUA));
        s.secondary().ifPresent(sec -> builder.accept(Component.literal("  ").append(Component.translatable("tooltip.barelyfunctionalcontraptions.output_line",
                sec.count(), sec.create().getHoverName())).withStyle(ChatFormatting.DARK_AQUA)));
        s.bonus().ifPresent(b -> builder.accept(Component.literal("  ").append(Component.translatable("tooltip.barelyfunctionalcontraptions.chance_line",
                b.item().count(), b.item().create().getHoverName(), Math.round(b.chance() * 100))).withStyle(ChatFormatting.DARK_AQUA)));
        builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.energy_time",
                String.format(Locale.ROOT, "%,d", s.effectiveEnergy()), String.format(Locale.ROOT, "%.1f", s.effectiveTicks() / 20.0))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
