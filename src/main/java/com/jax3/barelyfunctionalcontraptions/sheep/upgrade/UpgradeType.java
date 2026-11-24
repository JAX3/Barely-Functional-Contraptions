package com.jax3.barelyfunctionalcontraptions.sheep.upgrade;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * The kinds of upgrade card. Each trades one thing for another: what it does to a job's time and to its total FE (as
 * multipliers, from {@code [upgrades]} in the config), and which machines take it (the block tag
 * {@code #barelyfunctionalcontraptions:accepts_upgrade/<name>}).
 */
public enum UpgradeType {
    /** Jobs take less time, but cost more FE. */
    SPEED,
    /** Jobs cost less FE, but take a little longer. */
    EFFICIENCY;

    private final TagKey<Block> machines = TagKey.create(Registries.BLOCK, ResourcefulSheep.id("accepts_upgrade/" + id()));

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The machines that take this card. */
    public TagKey<Block> machines() {
        return machines;
    }

    /** Multiplier on a job's time with one card. */
    public double time() {
        return ModConfigs.getDouble(this == SPEED ? ModConfigs.SERVER.speedUpgradeTime : ModConfigs.SERVER.efficiencyUpgradeTime);
    }

    /** Multiplier on a job's total FE with one card. */
    public double energy() {
        return ModConfigs.getDouble(this == SPEED ? ModConfigs.SERVER.speedUpgradeEnergy : ModConfigs.SERVER.efficiencyUpgradeEnergy);
    }
}
