package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModTags;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.Separation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Killing an infused sheep that still has its wool drops more than shearing it would: the shearing amount of resource
 * wool plus {@code killWoolBonus}, and the resource itself (one Mechanical Separator roll of its primary output, e.g.
 * 2-4 Raw Gold). The vanilla wool block is removed, as when shearing.
 *
 * <p>It only touches the dying entity's own death loot table (identified by the damage source and the entity's
 * default loot table), so other tables that happen to carry the entity, such as shearing, are left alone. A sheared
 * sheep has no wool to give, so it drops only its normal loot.</p>
 */
public final class InfusedSheepKillLootModifier extends LootModifier {
    public static final MapCodec<InfusedSheepKillLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .apply(i, InfusedSheepKillLootModifier::new));

    public InfusedSheepKillLootModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!ModConfigs.getBool(ModConfigs.SERVER.killDropsEnabled)) return loot;
        if (context.getOptional(LootContextParams.DAMAGE_SOURCE) == null) return loot;
        Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
        if (entity == null || !ModTags.isInfusable(entity)) return loot;
        boolean ownDeathTable = entity.getLootTable()
                .map(key -> key.identifier().equals(context.getQueriedLootTableId())).orElse(false);
        if (!ownDeathTable) return loot;
        if (entity instanceof Sheep sheep && sheep.isSheared()) return loot;

        SheepInfusion infusion = SheepInfusionEvents.getInfusion(entity);
        if (!infusion.isInfused()) return loot;
        Optional<SheepResource> resource = SheepResourceRegistry.server().byId(infusion.resource().get());
        if (resource.isEmpty()) return loot; // wool type not loaded: plain sheep drops

        loot.removeIf(stack -> stack.is(ModTags.REPLACEABLE_WOOL));
        addDrops(resource.get(), context.getRandom(), loot::add);
        // The sheep is gone; clear the infusion so nothing else can pay out for it again.
        SheepInfusionEvents.setInfusion(entity, SheepInfusion.NONE);
        return loot;
    }

    /** Resource wool count dropped when an infused sheep is killed. */
    public static int woolCount(SheepResource resource) {
        return resource.effectiveWoolDropCount() + ModConfigs.getInt(ModConfigs.SERVER.killWoolBonus);
    }

    /** Adds the kill drops (resource wool, then the separator's primary output) for one infused sheep. */
    public static void addDrops(SheepResource resource, RandomSource random, Consumer<ItemStack> out) {
        split(ResourceWoolItem.create(resource.id(), 1), woolCount(resource), out);
        if (ModConfigs.getBool(ModConfigs.SERVER.killDropsResource)) {
            Separation separation = resource.separation();
            ItemStack primary = separation.primary().create();
            split(primary.copyWithCount(1), separation.rollPrimaryCount(random), out);
        }
    }

    private static void split(ItemStack template, int count, Consumer<ItemStack> out) {
        int max = template.getMaxStackSize();
        while (count > 0) {
            int n = Math.min(count, max);
            out.accept(template.copyWithCount(n));
            count -= n;
        }
    }

    @Override
    public MapCodec<? extends LootModifier> codec() {
        return CODEC;
    }
}
