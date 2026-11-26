package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import net.minecraft.world.item.ItemStack;

/** One "Implosion Chamber" page: one blast cracks {@code input} into {@code output} (Cobblestone into Gravel, Gravel into Sand). */
public record ImplosionDisplay(ItemStack input, ItemStack output) {}
