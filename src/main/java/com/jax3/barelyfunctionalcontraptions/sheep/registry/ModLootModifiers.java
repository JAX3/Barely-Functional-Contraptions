package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.InfusedSheepKillLootModifier;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.InfusedWoolLootModifier;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ResourcefulSheep.MOD_ID);

    public static final Supplier<MapCodec<InfusedWoolLootModifier>> INFUSED_WOOL = REGISTER.register("infused_wool", () -> InfusedWoolLootModifier.CODEC);
    public static final Supplier<MapCodec<InfusedSheepKillLootModifier>> INFUSED_SHEEP_KILL = REGISTER.register("infused_sheep_kill", () -> InfusedSheepKillLootModifier.CODEC);

    private ModLootModifiers() {}
}
