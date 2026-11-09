package com.jax3.barelyfunctionalcontraptions.sheep.integration.jade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModTags;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows a sheep's infusion. Purely client-side: the infusion attachment is already synced to clients, so no extra
 * server request is needed.
 */
public enum SheepInfusionProvider implements IEntityComponentProvider {
    INSTANCE;

    public static final Identifier UID = ResourcefulSheep.id("sheep_infusion");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        if (!ModTags.isInfusable(entity)) return;
        SheepInfusion infusion = SheepInfusionEvents.getInfusion(entity);
        long now = accessor.getLevel().getGameTime();
        if (!infusion.isInfused()) {
            if (infusion.onCooldown(now)) {
                tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.cooldown", (infusion.cooldownUntil() - now + 19) / 20)
                        .withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        Optional<SheepResource> resource = SheepInfusionEvents.getInfusedResource(entity);
        Component name = resource.map(r -> (Component) r.name().copy().withColor(r.woolColor()))
                .orElse(Component.literal(infusion.resource().get().toString()));
        tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.infused", name));
        if (infusion.usesRemaining() < 0) {
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.permanent").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("jade.barelyfunctionalcontraptions.uses_left", infusion.usesRemaining()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
