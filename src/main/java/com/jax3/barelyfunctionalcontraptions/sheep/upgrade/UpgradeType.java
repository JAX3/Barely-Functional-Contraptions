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
    EFFICIENCY,
    /** Stores and takes in more FE. */
    CAPACITY,
    /** A bigger working area. */
    RANGE,
    /** More of each output (the Mechanical Separator). */
    YIELD,
    /** Silences the machine. */
    MUFFLER;

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
        return switch (this) {
            case SPEED -> ModConfigs.getDouble(ModConfigs.SERVER.speedUpgradeTime);
            case EFFICIENCY -> ModConfigs.getDouble(ModConfigs.SERVER.efficiencyUpgradeTime);
            default -> 1;
        };
    }

    /** Multiplier on a job's total FE with one card. */
    public double energy() {
        return switch (this) {
            case SPEED -> ModConfigs.getDouble(ModConfigs.SERVER.speedUpgradeEnergy);
            case EFFICIENCY -> ModConfigs.getDouble(ModConfigs.SERVER.efficiencyUpgradeEnergy);
            default -> 1;
        };
    }

    /** Multiplier on the energy buffer and the most FE taken in a tick, with one card. */
    public double capacity() {
        return this == CAPACITY ? ModConfigs.getDouble(ModConfigs.SERVER.capacityUpgradeMultiplier) : 1;
    }

    /** Extra of each output a job gives, with one card. */
    public int yield() {
        return this == YIELD ? ModConfigs.getInt(ModConfigs.SERVER.yieldUpgradeBonus) : 0;
    }

    /** Blocks added to the working area on every side, with one card. */
    public int range() {
        return this == RANGE ? ModConfigs.getInt(ModConfigs.SERVER.rangeUpgradeBlocks) : 0;
    }
}
