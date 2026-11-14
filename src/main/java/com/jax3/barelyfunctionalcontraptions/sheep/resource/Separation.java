package com.jax3.barelyfunctionalcontraptions.sheep.resource;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * A fully resolved Mechanical Separator operation, regardless of whether it came from a sheep resource definition or
 * an explicit {@code barelyfunctionalcontraptions:separating} recipe. Machine logic and JEI both read from this type.
 *
 * <p>Energy and time are stored "raw" (as written in data, empty meaning "use the config default"); the
 * {@code effective*} methods apply the global config so every consumer sees identical numbers.</p>
 */
public record Separation(
        ItemStackTemplate primary,
        int primaryMax,
        Optional<ItemStackTemplate> secondary,
        Optional<ChanceOutput> bonus,
        Optional<Integer> energyCost,
        Optional<Integer> processingTicks) {

    /** Processing duration in ticks after the speed multiplier. Always at least 1. */
    public int effectiveTicks() {
        int base = processingTicks.orElseGet(() -> ModConfigs.getInt(ModConfigs.SERVER.defaultProcessingTicks));
        double speed = ModConfigs.getDouble(ModConfigs.SERVER.processingSpeedMultiplier);
        return Math.max(1, Mth.ceil(base / speed));
    }

    /** FE consumed per processing tick. */
    public int energyPerTick() {
        int fixed = ModConfigs.getInt(ModConfigs.SERVER.energyPerTickOverride);
        if (fixed > 0) return fixed;
        long total = (long) Math.ceil(energyCost.orElseGet(() -> ModConfigs.getInt(ModConfigs.SERVER.defaultEnergyCost))
                * ModConfigs.getDouble(ModConfigs.SERVER.energyCostMultiplier));
        return (int) Math.min(Integer.MAX_VALUE, (total + effectiveTicks() - 1) / effectiveTicks());
    }

    /** Total FE for one operation (per-tick usage x ticks, so it is exactly what the machine draws). */
    public long effectiveEnergy() {
        return (long) energyPerTick() * effectiveTicks();
    }

    /** Smallest primary count before the global multiplier (the count stored in {@link #primary}). */
    public int primaryMin() {
        return primary.count();
    }

    /** The configured primary count range before the global multiplier, e.g. 2-4. */
    public CountRange primaryRange() {
        return new CountRange(primaryMin(), Math.max(primaryMin(), primaryMax));
    }

    /** Average primary output count after the global multiplier (shown in JEI and tooltips). */
    public double expectedPrimaryCount() {
        return (primaryMin() + Math.max(primaryMin(), primaryMax)) / 2.0 * ModConfigs.getDouble(ModConfigs.SERVER.primaryOutputMultiplier);
    }

    /** Maximum number of primary items one operation can produce (used for output-space checks). */
    public int maxPrimaryCount() {
        double scaled = Math.max(primaryMin(), primaryMax) * ModConfigs.getDouble(ModConfigs.SERVER.primaryOutputMultiplier);
        return Mth.ceil(scaled) + (ModConfigs.getDouble(ModConfigs.SERVER.bonusPrimaryChance) > 0 ? 1 : 0);
    }

    /**
     * Rolls one operation's primary count: uniform in the range, then the global multiplier (fractions become a
     * chance of one more), then the global bonus chance. Never exceeds {@link #maxPrimaryCount()}.
     */
    public int rollPrimaryCount(net.minecraft.util.RandomSource random) {
        CountRange range = primaryRange();
        int base = range.min() + random.nextInt(range.max() - range.min() + 1);
        double scaled = base * ModConfigs.getDouble(ModConfigs.SERVER.primaryOutputMultiplier);
        int count = Mth.floor(scaled);
        if (random.nextDouble() < scaled - count) count++;
        if (random.nextDouble() < ModConfigs.getDouble(ModConfigs.SERVER.bonusPrimaryChance)) count++;
        return Math.min(count, maxPrimaryCount());
    }

    /** Range label after the multiplier, e.g. "2-4" (or "3" when fixed). */
    public String primaryLabel() {
        double mult = ModConfigs.getDouble(ModConfigs.SERVER.primaryOutputMultiplier);
        CountRange r = primaryRange();
        int lo = Mth.floor(r.min() * mult), hi = Mth.ceil(r.max() * mult);
        return lo == hi ? Integer.toString(lo) : lo + "-" + hi;
    }
    /**
     * The same operation for a Wool Bale of this wool: every output times {@code [wool_packer] baleYield}, and the
     * energy and time times {@code baleEnergy} and {@code baleTime} (all in pieces of wool's worth; a bale holds 9).
     * Each count is capped at one stack so a bale always fits the separator's output slots.
     */
    public Separation baled() {
        double yield = ModConfigs.getDouble(ModConfigs.SERVER.baleYield);
        double mult = ModConfigs.getDouble(ModConfigs.SERVER.primaryOutputMultiplier);
        int extra = ModConfigs.getDouble(ModConfigs.SERVER.bonusPrimaryChance) > 0 ? 1 : 0;
        int stack = primary.create().getMaxStackSize();
        // maxPrimaryCount() = ceil(max x mult) + extra must stay within a stack.
        int primaryCap = Math.max(1, (int) Math.floor((stack - extra) / Math.max(mult, 1e-9)));
        int min = Math.min(primaryCap, Math.max(1, (int) Math.round(primaryMin() * yield)));
        int max = Math.min(primaryCap, Math.max(min, (int) Math.round(Math.max(primaryMin(), primaryMax) * yield)));
        int energy = (int) Math.min(Integer.MAX_VALUE, Math.round(
                energyCost.orElseGet(() -> ModConfigs.getInt(ModConfigs.SERVER.defaultEnergyCost)) * ModConfigs.getDouble(ModConfigs.SERVER.baleEnergy)));
        int ticks = (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(
                processingTicks.orElseGet(() -> ModConfigs.getInt(ModConfigs.SERVER.defaultProcessingTicks)) * ModConfigs.getDouble(ModConfigs.SERVER.baleTime))));
        return new Separation(withCount(primary, min), max,
                secondary.map(s -> withCount(s, (int) Math.round(s.count() * yield))),
                bonus.map(b -> new ChanceOutput(withCount(b.item(), (int) Math.round(b.item().count() * yield)), b.chance())),
                Optional.of(energy), Optional.of(ticks));
    }

    /** {@code template} with {@code count} items, kept within 1 and a stack. */
    private static ItemStackTemplate withCount(ItemStackTemplate template, int count) {
        int stack = template.create().getMaxStackSize();
        return new ItemStackTemplate(template.item(), Math.max(1, Math.min(stack, count)), template.components());
    }
}
