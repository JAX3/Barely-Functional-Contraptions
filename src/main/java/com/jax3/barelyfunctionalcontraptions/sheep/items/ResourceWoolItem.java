package com.jax3.barelyfunctionalcontraptions.sheep.items;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.Separation;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * Resource-infused wool. One registered item; the {@code barelyfunctionalcontraptions:sheep_resource} component says which
 * resource it holds. Stacks with different resources never stack together because their components differ.
 */
public class ResourceWoolItem extends Item {
    public ResourceWoolItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Identifier resource, int count) {
        ItemStack stack = new ItemStack(ModItems.RESOURCE_WOOL.get(), count);
        stack.set(ModDataComponents.SHEEP_RESOURCE.get(), resource);
        return stack;
    }

    public static @Nullable Identifier getResourceId(ItemStack stack) {
        return stack.get(ModDataComponents.SHEEP_RESOURCE.get());
    }

    /** Looks the stack's resource up in the definitions for the given side. */
    public static Optional<SheepResource> getResource(ItemStack stack, boolean clientSide) {
        if (!(stack.getItem() instanceof ResourceWoolItem)) return Optional.empty();
        return SheepResourceRegistry.get(clientSide).byId(getResourceId(stack));
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier id = getResourceId(stack);
        if (id == null) return Component.translatable("item.barelyfunctionalcontraptions.resource_wool.unknown");
        // Names are resolved on whichever side is asking. The server copy is used when it has data (e.g. for chat
        // and death messages), otherwise the synced client copy, and finally the built-in translation key.
        Optional<SheepResource> resource = SheepResourceRegistry.server().byId(id).or(() -> SheepResourceRegistry.client().byId(id));
        return resource.map(SheepResource::woolDisplayName).orElseGet(() -> Component.translatable(
                "item.barelyfunctionalcontraptions.resource_wool.named",
                Component.translatableWithFallback("resource." + id.getNamespace() + "." + id.getPath().replace('/', '.'), id.toString())));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        Identifier id = getResourceId(stack);
        if (id == null) {
            builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.wool.no_resource").withStyle(ChatFormatting.RED));
            return;
        }
        Optional<SheepResource> resource = SheepResourceRegistry.client().byId(id).or(() -> SheepResourceRegistry.server().byId(id));
        if (resource.isEmpty()) {
            builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.wool.unknown_resource", id.toString()).withStyle(ChatFormatting.RED));
            return;
        }
        SheepResource r = resource.get();
        builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.wool.contains", r.name().copy().withColor(r.woolColor())).withStyle(ChatFormatting.GRAY));
        boolean detailed = ModConfigs.getBool(ModConfigs.CLIENT.detailedWoolTooltips) || flag.hasShiftDown();
        if (detailed) {
            Separation s = r.separation();
            builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.wool.separates_into").withStyle(ChatFormatting.GRAY));
            builder.accept(Component.literal("  ").append(Component.translatable("tooltip.barelyfunctionalcontraptions.output_line",
                    s.primaryLabel(), s.primary().create().getHoverName())).withStyle(ChatFormatting.DARK_AQUA));
            s.secondary().ifPresent(sec -> builder.accept(Component.literal("  ").append(Component.translatable("tooltip.barelyfunctionalcontraptions.output_line",
                    sec.count(), sec.create().getHoverName())).withStyle(ChatFormatting.DARK_AQUA)));
            s.bonus().ifPresent(b -> builder.accept(Component.literal("  ").append(Component.translatable("tooltip.barelyfunctionalcontraptions.chance_line",
                    b.item().count(), b.item().create().getHoverName(), Math.round(b.chance() * 100))).withStyle(ChatFormatting.DARK_AQUA)));
            builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.energy_time",
                    String.format(java.util.Locale.ROOT, "%,d", s.effectiveEnergy()), String.format(java.util.Locale.ROOT, "%.1f", s.effectiveTicks() / 20.0))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.hold_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        if (flag.isAdvanced()) {
            builder.accept(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
