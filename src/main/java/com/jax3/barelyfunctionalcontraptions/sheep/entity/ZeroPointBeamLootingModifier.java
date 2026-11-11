package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Built-in Looting for mobs killed by a Zero Point beam. Like the Looting enchantment's extra drops, every stack the
 * mob's own death loot table drops gets 0 to {@code beamLooting} extra items. Only the dying entity's own table is
 * touched (not equipment it was holding), and only when the killing blow was the beam.
 */
public final class ZeroPointBeamLootingModifier extends LootModifier {
    public static final MapCodec<ZeroPointBeamLootingModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .apply(i, ZeroPointBeamLootingModifier::new));

    public ZeroPointBeamLootingModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        int level = ModConfigs.getInt(ModConfigs.SERVER.zpeBeamLooting);
        if (level <= 0) return loot;
        DamageSource source = context.getOptional(LootContextParams.DAMAGE_SOURCE);
        if (source == null || !source.is(ZeroPointEnergyModuleBlockEntity.BEAM_DAMAGE)) return loot;
        Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
        if (entity == null) return loot;
        boolean ownDeathTable = entity.getLootTable()
                .map(key -> key.identifier().equals(context.getQueriedLootTableId())).orElse(false);
        if (!ownDeathTable) return loot;
        return applyLooting(loot, level, context.getRandom());
    }

    /** Adds 0 to {@code level} extra items to every stack, splitting anything over its stack size into more stacks. */
    public static ObjectArrayList<ItemStack> applyLooting(ObjectArrayList<ItemStack> loot, int level, RandomSource random) {
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>(loot.size());
        for (ItemStack stack : loot) {
            if (stack.isEmpty()) continue;
            int total = stack.getCount() + random.nextInt(level + 1);
            int max = stack.getMaxStackSize();
            while (total > 0) {
                int n = Math.min(total, max);
                out.add(stack.copyWithCount(n));
                total -= n;
            }
        }
        return out;
    }

    @Override
    public MapCodec<? extends LootModifier> codec() {
        return CODEC;
    }
}
