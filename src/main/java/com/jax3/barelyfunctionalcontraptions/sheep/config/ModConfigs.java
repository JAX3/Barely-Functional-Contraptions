package com.jax3.barelyfunctionalcontraptions.sheep.config;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RedstoneMode;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RelativeSide;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All global configuration for Resourceful Sheep.
 *
 * <p>The {@link #SERVER} config ({@code config/barelyfunctionalcontraptions-sheep-synced.toml}; a copy in {@code <world>/syncedconfig/} overrides it per world) and is
 * synchronised to connected clients by NeoForge, so values that affect what clients display (JEI energy figures,
 * wool colour blending) stay consistent in multiplayer. {@link #CLIENT} holds purely visual preferences.</p>
 *
 * <p>Per-resource values (feeding item, outputs, colours...) live in data-pack JSON, see
 * {@code SheepResourceManager}. Values here act as global defaults and multipliers on top of them.</p>
 */
public final class ModConfigs {
    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        var server = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = server.getLeft();
        SERVER_SPEC = server.getRight();
        var client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    private ModConfigs() {}

    /** Reads a config value, falling back to its default before the config file is loaded (e.g. on the main menu). */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        try {
            return value.get();
        } catch (IllegalStateException notLoaded) {
            return value.getDefault();
        }
    }

    public static int getInt(ModConfigSpec.IntValue value) {
        return get(value);
    }

    public static double getDouble(ModConfigSpec.DoubleValue value) {
        return get(value);
    }

    public static boolean getBool(ModConfigSpec.BooleanValue value) {
        return get(value);
    }

    public static final class Server {
        // ---------------------------------------------------------------- sheep
        public final ModConfigSpec.BooleanValue feedingEnabled;
        public final ModConfigSpec.IntValue feedingAmountOverride;
        public final ModConfigSpec.BooleanValue allowBabies;
        public final ModConfigSpec.BooleanValue breedingKeepsInfusion;
        public final ModConfigSpec.BooleanValue breedingNeedsBothParents;
        public final ModConfigSpec.BooleanValue allowFeedingShearedSheep;
        public final ModConfigSpec.BooleanValue allowReplacingInfusion;
        public final ModConfigSpec.BooleanValue sameResourceRefreshesUses;
        public final ModConfigSpec.BooleanValue infusionConsumedByShearing;
        public final ModConfigSpec.IntValue infusionUsesOverride;
        public final ModConfigSpec.DoubleValue infusionUsesMultiplier;
        public final ModConfigSpec.IntValue woolDropCountOverride;
        public final ModConfigSpec.DoubleValue shearingBonusChance;
        public final ModConfigSpec.IntValue shearingBonusAmount;
        public final ModConfigSpec.BooleanValue killDropsEnabled;
        public final ModConfigSpec.IntValue killWoolBonus;
        public final ModConfigSpec.BooleanValue killDropsResource;
        public final ModConfigSpec.BooleanValue preserveWoolColour;
        public final ModConfigSpec.BooleanValue feedingParticles;
        public final ModConfigSpec.IntValue infusionCooldownTicks;
        public final ModConfigSpec.BooleanValue creativeConsumesItems;
        public final ModConfigSpec.BooleanValue creativeIgnoresCooldown;
        public final ModConfigSpec.BooleanValue sneakToInspect;

        // ---------------------------------------------------------------- separator
        public final ModConfigSpec.IntValue energyCapacity;
        public final ModConfigSpec.IntValue maxEnergyInput;
        public final ModConfigSpec.IntValue defaultEnergyCost;
        public final ModConfigSpec.IntValue energyPerTickOverride;
        public final ModConfigSpec.IntValue defaultProcessingTicks;
        public final ModConfigSpec.IntValue defaultStringOutput;
        public final ModConfigSpec.DoubleValue primaryOutputMultiplier;
        public final ModConfigSpec.DoubleValue bonusPrimaryChance;
        public final ModConfigSpec.DoubleValue energyCostMultiplier;
        public final ModConfigSpec.DoubleValue processingSpeedMultiplier;
        public final ModConfigSpec.BooleanValue resetProgressWhenUnpowered;
        public final ModConfigSpec.ConfigValue<List<? extends String>> energyInputSides;
        public final ModConfigSpec.ConfigValue<List<? extends String>> itemInputSides;
        public final ModConfigSpec.ConfigValue<List<? extends String>> itemOutputSides;
        public final ModConfigSpec.BooleanValue autoPushOutputs;
        public final ModConfigSpec.IntValue autoPushInterval;
        public final ModConfigSpec.BooleanValue redstoneControlEnabled;
        public final ModConfigSpec.EnumValue<RedstoneMode> defaultRedstoneMode;
        public final ModConfigSpec.IntValue creativeGeneratorOutput;
        public final ModConfigSpec.IntValue glowPanelSunRate;
        public final ModConfigSpec.IntValue glowPanelLightRate;
        public final ModConfigSpec.IntValue glowPanelCapacity;
        public final ModConfigSpec.IntValue glowPanelOutput;
        public final ModConfigSpec.IntValue woolBurnerCapacity;
        public final ModConfigSpec.IntValue woolBurnerOutput;
        public final ModConfigSpec.BooleanValue statsEnabled;
        public final ModConfigSpec.IntValue statsMax;
        public final ModConfigSpec.DoubleValue statsIncreaseChance;
        public final ModConfigSpec.DoubleValue statsDecreaseChance;
        public final ModConfigSpec.DoubleValue statsYieldPerPoint;
        public final ModConfigSpec.DoubleValue statsGrowthPerPoint;
        public final ModConfigSpec.DoubleValue statsEndurancePerPoint;
        public final ModConfigSpec.IntValue handFeedingMaxTier;
        public final ModConfigSpec.IntValue altarBaseEnergy;
        public final ModConfigSpec.IntValue altarHighTierEnergy;
        public final ModConfigSpec.IntValue altarTicks;
        public final ModConfigSpec.IntValue altarCapacity;
        public final ModConfigSpec.IntValue altarMaxInput;
        public final ModConfigSpec.IntValue altarSlowness;
        public final ModConfigSpec.IntValue woolPackerCapacity;
        public final ModConfigSpec.IntValue woolPackerMaxInput;
        public final ModConfigSpec.IntValue woolPackerEnergyPerBale;
        public final ModConfigSpec.IntValue woolPackerTicksPerBale;
        public final ModConfigSpec.DoubleValue baleYield;
        public final ModConfigSpec.DoubleValue baleEnergy;
        public final ModConfigSpec.DoubleValue baleTime;
        public final ModConfigSpec.DoubleValue poweredFurnaceCookTime;
        public final ModConfigSpec.IntValue poweredFurnaceEnergyPerTick;
        public final ModConfigSpec.DoubleValue poweredFurnaceBlazeBonus;
        public final ModConfigSpec.IntValue poweredFurnaceMaxHeaters;
        public final ModConfigSpec.IntValue poweredFurnaceCapacity;
        public final ModConfigSpec.IntValue poweredFurnaceMaxInput;
        public final ModConfigSpec.BooleanValue woollenBootsMuffle;
        public final ModConfigSpec.IntValue treadmillPower;
        public final ModConfigSpec.IntValue treadmillTicksPerWheat;
        public final ModConfigSpec.IntValue treadmillWalkTicks;
        public final ModConfigSpec.IntValue treadmillRestTicks;
        public final ModConfigSpec.DoubleValue treadmillEnduranceBonus;
        public final ModConfigSpec.DoubleValue treadmillSpeedBonus;
        public final ModConfigSpec.DoubleValue treadmillShearedBonus;
        public final ModConfigSpec.IntValue treadmillCapacity;
        public final ModConfigSpec.IntValue treadmillOutput;
        public final ModConfigSpec.BooleanValue muffleVibrations;
        public final ModConfigSpec.IntValue chillerRadius;
        public final ModConfigSpec.IntValue chillerMaxSources;
        public final ModConfigSpec.DoubleValue chillerFalloff;
        public final ModConfigSpec.IntValue chillerTicksPerIce;
        public final ModConfigSpec.IntValue chillerWaterPerIce;
        public final ModConfigSpec.IntValue chillerTankCapacity;
        public final ModConfigSpec.ConfigValue<List<? extends String>> chillerColdSources;
        public final ModConfigSpec.IntValue blazeFurnaceRadius;
        public final ModConfigSpec.IntValue blazeFurnaceMaxHeaters;
        public final ModConfigSpec.DoubleValue blazeFurnaceFalloff;
        public final ModConfigSpec.IntValue blazeFurnaceCapacity;
        public final ModConfigSpec.IntValue blazeFurnaceOutput;
        public final ModConfigSpec.IntValue automaticShearsInterval;
        public final ModConfigSpec.IntValue automaticShearsEnergyPerShear;
        public final ModConfigSpec.IntValue automaticShearsEnergyCapacity;
        public final ModConfigSpec.IntValue automaticShearsMaxEnergyInput;
        public final ModConfigSpec.IntValue reservoirWaterPerSecond;
        public final ModConfigSpec.IntValue reservoirCapacity;
        public final ModConfigSpec.BooleanValue reservoirRequireInfinite;
        public final ModConfigSpec.IntValue reservoirOutputPerTick;
        public final ModConfigSpec.IntValue energyCubeCapacityPerCube;
        public final ModConfigSpec.IntValue energyCubeTransferPerCube;
        public final ModConfigSpec.IntValue energyCubeMaxGroupSize;
        public final ModConfigSpec.BooleanValue energyCubeAutoOutput;
        public final ModConfigSpec.IntValue zpeEnergyPerBlock;
        public final ModConfigSpec.IntValue zpeMinGap;
        public final ModConfigSpec.IntValue zpeMaxRange;
        public final ModConfigSpec.IntValue zpeCapacity;
        public final ModConfigSpec.IntValue zpeMaxOutput;
        public final ModConfigSpec.DoubleValue zpeBeamDamage;
        public final ModConfigSpec.IntValue zpeBeamBurnSeconds;
        public final ModConfigSpec.IntValue zpeBeamLooting;
        public final ModConfigSpec.BooleanValue zpeBeamPlayerKill;
        public final ModConfigSpec.IntValue cableTransferRate;
        public final ModConfigSpec.IntValue tankCapacityPerTank;
        public final ModConfigSpec.IntValue tankTransferPerTank;
        public final ModConfigSpec.IntValue tankMaxGroupSize;
        public final ModConfigSpec.BooleanValue tankAutoOutput;
        public final ModConfigSpec.IntValue cableMaxNetworkSize;
        public final ModConfigSpec.IntValue pipeTransferRate;
        public final ModConfigSpec.IntValue pipeMaxNetworkSize;
        public final ModConfigSpec.IntValue itemPipeTransferRate;
        public final ModConfigSpec.IntValue itemPipeMaxNetworkSize;

        // ---------------------------------------------------------------- /barelyfunctionalcontraptions generate
        public final ModConfigSpec.ConfigValue<List<? extends String>> generatorAllowedTabs;
        public final ModConfigSpec.BooleanValue generatorRequireStackable;

        Server(ModConfigSpec.Builder b) {
            b.comment("Resource infusion of sheep. Per-resource settings live in data/<namespace>/barelyfunctionalcontraptions/sheep_resources/*.json")
                    .push("sheep");
            feedingEnabled = b.comment("Allow players to infuse sheep by right-clicking them with a resource.")
                    .define("feedingEnabled", true);
            feedingAmountOverride = b.comment("Items consumed per feeding. -1 uses each resource definition's 'feeding_amount'.")
                    .defineInRange("feedingAmountOverride", -1, -1, 64);
            allowBabies = b.comment("Allow lambs to be infused (their wool is only dropped once grown).")
                    .define("allowBabies", false);
            breedingKeepsInfusion = b.comment("Lambs bred from infused sheep are born infused with a parent's resource, with a full set of",
                            "shearings. Two parents with different resources pass on one of the two at random.")
                    .define("breedingKeepsInfusion", true);
            breedingNeedsBothParents = b.comment("Only pass the infusion on when both parents are infused (otherwise one infused parent is enough).")
                    .define("breedingNeedsBothParents", false);
            allowFeedingShearedSheep = b.comment("Allow infusing a sheep that is currently sheared; its wool regrows infused.")
                    .define("allowFeedingShearedSheep", true);
            allowReplacingInfusion = b.comment("Allow feeding a different resource to an already-infused sheep, replacing the old infusion (the old one is lost).")
                    .define("allowReplacingInfusion", false);
            sameResourceRefreshesUses = b.comment("Feeding the same resource again tops the remaining shear count back up to full.")
                    .define("sameResourceRefreshesUses", true);
            infusionConsumedByShearing = b.comment("If true, each shearing uses up one 'use' of the infusion; when none remain the sheep returns to normal.",
                            "If false, infusions are PERMANENT (turns sheep into an unlimited generator - not recommended for survival packs).")
                    .define("infusionConsumedByShearing", true);
            infusionUsesOverride = b.comment("Shearings every infusion lasts, for every wool type (built-in, generated and data-pack ones).",
                            "-1 uses each resource definition's own 'infusion_uses' (9 for blocks, 1 for gems...). The multiplier below still applies.")
                    .defineInRange("infusionUsesOverride", 100, -1, 1_000_000);
            infusionUsesMultiplier = b.comment("Multiplier applied to each resource's 'infusion_uses' (number of shearings an infusion lasts). Result is rounded, minimum 1.")
                    .defineInRange("infusionUsesMultiplier", 1.0, 0.01, 100.0);
            woolDropCountOverride = b.comment("Infused wool items dropped per shearing. -1 uses each resource definition's 'wool_drop_count'.")
                    .defineInRange("woolDropCountOverride", -1, -1, 64);
            shearingBonusChance = b.comment("Chance (0-1) for a shearing to drop bonus infused wool.")
                    .defineInRange("shearingBonusChance", 0.0, 0.0, 1.0);
            shearingBonusAmount = b.comment("Number of bonus infused wool items when the shearing bonus triggers.")
                    .defineInRange("shearingBonusAmount", 1, 0, 64);
            killDropsEnabled = b.comment("Killing an infused sheep that still has its wool drops its resource wool and the resource itself, a little more than shearing it.")
                    .define("killDropsEnabled", true);
            killWoolBonus = b.comment("Extra resource wool a killed infused sheep drops on top of what one shearing gives.")
                    .defineInRange("killWoolBonus", 1, 0, 64);
            killDropsResource = b.comment("Whether a killed infused sheep also drops the resource itself (one Mechanical Separator roll of its primary output, e.g. 2-4 Raw Gold).")
                    .define("killDropsResource", true);
            preserveWoolColour = b.comment("If true an infused sheep's wool keeps a hint of its dye colour; if false it shows only the resource colour.")
                    .define("preserveWoolColour", true);
            feedingParticles = b.comment("Show resource-coloured particles and play a sound when a sheep is infused.")
                    .define("feedingParticles", true);
            infusionCooldownTicks = b.comment("Ticks after an infusion runs out before the sheep can be infused again. 0 disables.")
                    .defineInRange("infusionCooldownTicks", 0, 0, 24000 * 10);
            creativeConsumesItems = b.comment("Whether feeding consumes items for players in creative mode.")
                    .define("creativeConsumesItems", false);
            creativeIgnoresCooldown = b.comment("Whether creative-mode players bypass the infusion cooldown and replacement restrictions.")
                    .define("creativeIgnoresCooldown", true);
            sneakToInspect = b.comment("Sneak + right-click a sheep with an empty hand to see its infusion in the action bar.")
                    .define("sneakToInspect", true);
            b.pop();

            b.comment("Mechanical Separator machine").push("separator");
            energyCapacity = b.comment("Internal FE buffer size.")
                    .translation("barelyfunctionalcontraptions.configuration.separator.energyCapacity").defineInRange("energyCapacity", 100_000, 1, Integer.MAX_VALUE);
            maxEnergyInput = b.comment("Maximum FE accepted per tick (per insertion call).")
                    .translation("barelyfunctionalcontraptions.configuration.separator.maxEnergyInput").defineInRange("maxEnergyInput", 1_000, 1, Integer.MAX_VALUE);
            defaultEnergyCost = b.comment("Total FE to separate one wool when a resource/recipe does not specify 'energy_cost'.")
                    .defineInRange("defaultEnergyCost", 20_000, 0, Integer.MAX_VALUE);
            energyPerTickOverride = b.comment("If > 0, the machine always uses exactly this much FE per tick (total = this x processing ticks),",
                            "ignoring per-recipe energy costs. 0 = derive per-tick usage from each recipe's total energy cost.")
                    .defineInRange("energyPerTickOverride", 0, 0, Integer.MAX_VALUE);
            defaultProcessingTicks = b.comment("Processing time in ticks when a resource/recipe does not specify 'processing_ticks'.")
                    .defineInRange("defaultProcessingTicks", 100, 1, 72_000);
            defaultStringOutput = b.comment("String produced per wool when a resource does not specify 'secondary_count'.")
                    .defineInRange("defaultStringOutput", 2, 0, 64);
            primaryOutputMultiplier = b.comment("Multiplier on the primary (resource) output. Fractions are applied as a chance for one extra item.")
                    .defineInRange("primaryOutputMultiplier", 1.0, 0.0, 64.0);
            bonusPrimaryChance = b.comment("Global chance (0-1) to produce one extra primary output per operation, in addition to any per-resource bonus.")
                    .defineInRange("bonusPrimaryChance", 0.0, 0.0, 1.0);
            energyCostMultiplier = b.comment("Efficiency multiplier on total energy cost. 0.5 = half the FE per operation.")
                    .defineInRange("energyCostMultiplier", 1.0, 0.0, 100.0);
            processingSpeedMultiplier = b.comment("Speed multiplier. 2.0 = operations take half as many ticks (total energy is unchanged).")
                    .defineInRange("processingSpeedMultiplier", 1.0, 0.01, 100.0);
            resetProgressWhenUnpowered = b.comment("If true, progress resets when the machine runs out of energy instead of pausing.")
                    .define("resetProgressWhenUnpowered", false);
            String sideHelp = "Valid values: FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM (relative to the machine's front face).";
            energyInputSides = b.comment("Sides that accept FE.", sideHelp)
                    .defineListAllowEmpty("energyInputSides", List.of("BACK", "LEFT", "RIGHT", "TOP", "BOTTOM"), () -> "BACK", ModConfigs::isSide);
            itemInputSides = b.comment("Sides through which hoppers/pipes may insert wool.", sideHelp)
                    .defineListAllowEmpty("itemInputSides", List.of("TOP", "BACK", "LEFT", "RIGHT"), () -> "TOP", ModConfigs::isSide);
            itemOutputSides = b.comment("Sides through which hoppers/pipes may extract outputs, and that outputs are auto-pushed into.", sideHelp)
                    .defineListAllowEmpty("itemOutputSides", List.of("BOTTOM"), () -> "BOTTOM", ModConfigs::isSide);
            autoPushOutputs = b.comment("Automatically push outputs into inventories adjacent to output sides.")
                    .define("autoPushOutputs", true);
            autoPushInterval = b.comment("Ticks between automatic output pushes.")
                    .defineInRange("autoPushInterval", 10, 1, 1200);
            redstoneControlEnabled = b.comment("Allow players to change the redstone mode from the GUI. If false every machine uses defaultRedstoneMode.")
                    .define("redstoneControlEnabled", true);
            defaultRedstoneMode = b.comment("Redstone mode for newly placed machines.")
                    .defineEnum("defaultRedstoneMode", RedstoneMode.IGNORED);
            b.pop();

            b.comment("Creative Generator (creative-only infinite FE source for testing)").push("creative_generator");
            creativeGeneratorOutput = b.comment("FE pushed into each adjacent energy receiver per tick (receivers still apply their own input limits). 0 disables pushing.")
                    .defineInRange("creativeGeneratorOutput", 1_000_000, 0, Integer.MAX_VALUE);
            b.pop();

            b.comment("Glow Panel (makes FE from sunlight, or from block light such as a Glow Lamp, falling on its top)").push("glow_panel");
            glowPanelSunRate = b.comment("FE/t in full sunlight. Scales down with the sky light (time of day, weather, shade).")
                    .defineInRange("sunRate", 60, 0, 1_000_000);
            glowPanelLightRate = b.comment("FE/t under block light 15 (a Glow Lamp, glowstone...). Scales with the light level; the panel uses whichever of sun or block light gives more.")
                    .defineInRange("lightRate", 40, 0, 1_000_000);
            glowPanelCapacity = b.comment("FE each panel stores.")
                    .defineInRange("capacity", 10_000, 1, Integer.MAX_VALUE);
            glowPanelOutput = b.comment("Most FE a panel pushes into the blocks beside and below it per tick, in total.")
                    .defineInRange("outputPerTick", 1_000, 0, Integer.MAX_VALUE);
            b.pop();

            b.comment("Wool Burner (burns wool, string and Coal Wool for FE; fuels are data-driven, see wool_burner_fuel)").push("wool_burner");
            woolBurnerCapacity = b.comment("FE each Wool Burner stores. It stops burning while this is full, so no fuel is wasted.")
                    .defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE);
            woolBurnerOutput = b.comment("Most FE a Wool Burner pushes into the blocks around it per tick, in total.")
                    .defineInRange("outputPerTick", 1_000, 0, Integer.MAX_VALUE);
            b.pop();

            b.comment("Sheep stats from breeding: Yield, Growth and Endurance, each 1 (wild) to maxStat. A lamb gets its parents' average, with a chance of one more.").push("sheep_stats");
            statsEnabled = b.comment("Whether bred stats are passed on and have any effect. Off: every sheep acts like a wild one (stats already bred are kept).")
                    .define("enabled", true);
            statsMax = b.comment("Highest a stat can reach.")
                    .defineInRange("maxStat", 10, 1, 100);
            statsIncreaseChance = b.comment("Chance (0-1) that each of a lamb's stats comes out one higher than its parents' average.")
                    .defineInRange("increaseChance", 0.25, 0.0, 1.0);
            statsDecreaseChance = b.comment("Chance (0-1) that each of a lamb's stats comes out one lower than its parents' average.")
                    .defineInRange("decreaseChance", 0.05, 0.0, 1.0);
            statsYieldPerPoint = b.comment("Yield: extra wool per shearing for each point above 1, as a share of the normal amount. 0.15 = Yield 10 shears 2.35 times as much (fractions are a chance of one more).")
                    .defineInRange("yieldPerPoint", 0.15, 0.0, 10.0);
            statsGrowthPerPoint = b.comment("Growth: chance per second, for each point above 1, that a sheared sheep regrows its wool on its own without eating grass. 0.01 = Growth 10 regrows in about 11 seconds on average.")
                    .defineInRange("growthPerPoint", 0.01, 0.0, 1.0);
            statsEndurancePerPoint = b.comment("Endurance: extra shearings an infusion lasts for each point above 1, as a share of its normal uses. 0.1 = Endurance 10 lasts 1.9 times as long.")
                    .defineInRange("endurancePerPoint", 0.1, 0.0, 10.0);
            b.pop();

            b.comment("Infusion Altar (FE-powered; infuses the sheep beside it, with catalysts for the higher tiers; each resource's tier and catalysts are in its definition's 'altar' block)").push("infusion_altar");
            // Replaces altarOnlyFromTier (tier 2 and up altar-only), under a new name so older config files pick up the new default.
            handFeedingMaxTier = b.comment("The highest tier of sheep that can still be fed by hand; everything above it is infused at an Infusion Altar. Most sheep are tier 1; the Wither, Dragon and Warden sheep are tier 2.",
                            "0 = every sheep needs the altar (the default). 1 = tier 1 sheep can be fed by hand again, as before. 3 = every sheep can be fed by hand.")
                    .defineInRange("handFeedingMaxTier", 0, 0, 3);
            altarBaseEnergy = b.comment("FE a tier 1 infusion takes at the altar, when its definition doesn't say.")
                    .defineInRange("baseEnergy", 20_000, 0, Integer.MAX_VALUE);
            altarHighTierEnergy = b.comment("FE a tier 2 or 3 infusion takes, when its definition doesn't say.")
                    .defineInRange("highTierEnergy", 200_000, 0, Integer.MAX_VALUE);
            altarTicks = b.comment("Ticks one infusion takes at the altar (the energy is spread over them).")
                    .defineInRange("ticks", 100, 1, 72_000);
            altarCapacity = b.comment("FE the altar stores.")
                    .defineInRange("energyCapacity", 250_000, 1, Integer.MAX_VALUE);
            altarMaxInput = b.comment("Most FE the altar accepts per tick.")
                    .defineInRange("maxEnergyInput", 5_000, 1, Integer.MAX_VALUE);
            altarSlowness = b.comment("Level of Slowness on the sheep while the altar infuses it, so it doesn't wander off; taken off when the infusion ends. Only the sheep is slowed. 5 = nearly still, 0 = off.")
                    .defineInRange("sheepSlowness", 5, 0, 10);
            b.pop();

            b.comment("Wool Packer (FE-powered; presses 9 of the same resource wool into a Wool Bale) and Wool Bales in the Mechanical Separator").push("wool_packer");
            woolPackerCapacity = b.comment("FE each Wool Packer stores.")
                    .defineInRange("energyCapacity", 20_000, 1, Integer.MAX_VALUE);
            woolPackerMaxInput = b.comment("Most FE a Wool Packer accepts per tick.")
                    .defineInRange("maxEnergyInput", 1_000, 1, Integer.MAX_VALUE);
            woolPackerEnergyPerBale = b.comment("FE to press one bale. 0 = the packer needs no power.")
                    .defineInRange("energyPerBale", 1_000, 0, Integer.MAX_VALUE);
            woolPackerTicksPerBale = b.comment("Ticks to press one bale (20 = one second).")
                    .defineInRange("ticksPerBale", 40, 1, 72_000);
            baleYield = b.comment("What separating one bale gives, in pieces of wool's worth of outputs. One bale holds 9 wool, so above 9 is the reward for baling: 12 = a third more of every output. Counts are capped at a stack so a bale never jams the separator.")
                    .defineInRange("baleYield", 12.0, 1.0, 64.0);
            baleEnergy = b.comment("FE to separate one bale, in pieces of wool's worth: 15 = 15 times the wool's energy cost, more than its 9 wool would take.")
                    .defineInRange("baleEnergy", 15.0, 0.0, 1_000.0);
            baleTime = b.comment("Time to separate one bale, in pieces of wool's worth: 9 = as long as its 9 wool one at a time.")
                    .defineInRange("baleTime", 9.0, 0.05, 1_000.0);
            b.pop();

            b.comment("Blaze Sheep Furnace (makes FE from the heat of Blaze sheep in the area around and above it; heat sources are data-driven, see furnace_heat)").push("blaze_furnace");
            blazeFurnaceRadius = b.comment("How far out the heating area reaches from the furnace, in blocks: 1 = the 3x3 columns around it, 2 = 5x5 (default), and so on. It always covers the furnace's own level and the block above. The area outline follows it.")
                    .defineInRange("radius", 2, 0, 8);
            blazeFurnaceMaxHeaters = b.comment("Most heat sources (sheep) one furnace counts; the hottest are used.")
                    .defineInRange("maxHeatSources", 4, 1, 64);
            blazeFurnaceFalloff = b.comment("How much less each extra heat source adds: the 2nd gives (1 - falloff) of its power, the 3rd (1 - 2 x falloff) and so on. 0 = no falloff.")
                    .defineInRange("falloff", 0.125, 0.0, 1.0);
            blazeFurnaceCapacity = b.comment("FE each furnace stores.")
                    .defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE);
            blazeFurnaceOutput = b.comment("Most FE a furnace pushes into the blocks around it per tick, in total.")
                    .defineInRange("outputPerTick", 1_000, 0, Integer.MAX_VALUE);
            b.pop();

            b.comment("Powered Furnace (smelts anything a Furnace can, on FE; Blaze sheep around it speed it up)").push("powered_furnace");
            poweredFurnaceCookTime = b.comment("Its smelting time as a share of a Furnace's: 0.75 = three quarters of the time (a 10 s smelt takes 7.5 s).")
                    .defineInRange("cookTimeMultiplier", 0.75, 0.01, 10.0);
            poweredFurnaceEnergyPerTick = b.comment("FE it uses per tick while smelting.")
                    .defineInRange("energyPerTick", 20, 0, 1_000_000);
            poweredFurnaceBlazeBonus = b.comment("Extra speed from each Blaze sheep (heat source) in the area around it: 0.25 = 25% faster for the first; each one after it adds less, with the Blaze Sheep Furnace's falloff. The area is the furnace's ([blaze_furnace] radius).")
                    .defineInRange("blazeBonus", 0.25, 0.0, 10.0);
            poweredFurnaceMaxHeaters = b.comment("Most Blaze sheep it counts.")
                    .defineInRange("maxBlazeSheep", 4, 0, 64);
            poweredFurnaceCapacity = b.comment("FE it stores.")
                    .defineInRange("energyCapacity", 20_000, 1, Integer.MAX_VALUE);
            poweredFurnaceMaxInput = b.comment("Most FE it accepts per tick.")
                    .defineInRange("maxEnergyInput", 1_000, 1, Integer.MAX_VALUE);
            b.pop();

            b.comment("Sheep Treadmill (a sheep walks its belt for wheat and makes FE)").push("sheep_treadmill");
            treadmillPower = b.comment("FE/t while a sheep walks it, before the bonuses below.")
                    .defineInRange("power", 20, 0, 1_000_000);
            treadmillTicksPerWheat = b.comment("Ticks of walking one Wheat feeds (600 = 30 seconds). A Hay Bale feeds nine times as long.")
                    .defineInRange("ticksPerWheat", 600, 1, 72_000);
            treadmillWalkTicks = b.comment("Ticks a sheep walks before it's worn out and needs a rest (2400 = 2 minutes), for a sheep with 1 Endurance.")
                    .defineInRange("walkTicks", 2400, 1, 720_000);
            treadmillRestTicks = b.comment("Ticks a worn-out sheep rests before it will walk again (600 = 30 seconds).")
                    .defineInRange("restTicks", 600, 0, 720_000);
            treadmillEnduranceBonus = b.comment("How much longer a sheep walks for each Endurance point above 1: 0.25 = 25% longer per point.")
                    .defineInRange("enduranceBonus", 0.25, 0.0, 10.0);
            treadmillSpeedBonus = b.comment("Extra power for each level of Speed the sheep has: 0.2 = 20% more per level. Slowness takes as much off.")
                    .defineInRange("speedBonus", 0.2, 0.0, 10.0);
            treadmillShearedBonus = b.comment("Extra power while the sheep is sheared and running light: 0.25 = 25% more.")
                    .defineInRange("shearedBonus", 0.25, 0.0, 10.0);
            treadmillCapacity = b.comment("FE it stores.")
                    .defineInRange("capacity", 20_000, 1, Integer.MAX_VALUE);
            treadmillOutput = b.comment("Most FE it pushes into the blocks around it per tick.")
                    .defineInRange("output", 200, 0, Integer.MAX_VALUE);
            b.pop();

            b.comment("Woollen Armour (crafted from wool, in the wool's colour)").push("woollen_armor");
            woollenBootsMuffle = b.comment("Whether Woollen Boots give the Muffled effect while worn (no sounds from the wearer).")
                    .define("bootsMuffle", true);
            muffleVibrations = b.comment("Whether Muffled also hides the wearer's vibrations from sculk sensors, shriekers and Wardens.")
                    .define("muffleVibrations", true);
            b.pop();

            b.comment("Snow Sheep Chiller (freezes water into Ice with the cold of Snow sheep in the area around and above it; needs no power)").push("chiller");
            chillerRadius = b.comment("How far out the chilling area reaches, in blocks: 2 = the 5x5 columns around it (default). It covers the chiller's own level and the block above.")
                    .defineInRange("radius", 2, 0, 8);
            chillerMaxSources = b.comment("Most Snow sheep one chiller counts.")
                    .defineInRange("maxColdSources", 4, 1, 64);
            chillerFalloff = b.comment("How much less each extra Snow sheep adds to the speed: the 2nd gives (1 - falloff), the 3rd (1 - 2 x falloff) and so on. 0 = no falloff.")
                    .defineInRange("falloff", 0.125, 0.0, 1.0);
            chillerTicksPerIce = b.comment("Ticks to freeze one Ice with one Snow sheep (200 = 10 seconds). More sheep freeze faster.")
                    .defineInRange("ticksPerIce", 200, 1, 72_000);
            chillerWaterPerIce = b.comment("mB of water each Ice takes (1000 = a bucket).")
                    .defineInRange("waterPerIce", 1_000, 0, 1_000_000);
            chillerTankCapacity = b.comment("mB of water a chiller holds.")
                    .defineInRange("tankCapacity", 8_000, 1_000, 1_000_000);
            chillerColdSources = b.comment("Sheep resources whose sheep chill it (with their fleece on).")
                    .defineListAllowEmpty("coldSources", List.of("barelyfunctionalcontraptions:snow"), () -> "barelyfunctionalcontraptions:snow", o -> o instanceof String);
            b.pop();

            b.comment("Automatic Shears (FE-powered; shears sheep in the 3x3 area in front of it into its inventory)").push("automatic_shears");
            automaticShearsInterval = b.comment("Ticks between shearing sweeps (20 = once a second). Each sweep shears every ready sheep in the area.")
                    .defineInRange("shearInterval", 20, 1, 72_000);
            automaticShearsEnergyPerShear = b.comment("FE used for each sheep sheared. 0 = runs without power.")
                    .defineInRange("energyPerShear", 2_000, 0, Integer.MAX_VALUE);
            automaticShearsEnergyCapacity = b.comment("FE buffer.")
                    .translation("barelyfunctionalcontraptions.configuration.automatic_shears.energyCapacity").defineInRange("energyCapacity", 50_000, 1, Integer.MAX_VALUE);
            automaticShearsMaxEnergyInput = b.comment("FE accepted per tick (every face except the front).")
                    .translation("barelyfunctionalcontraptions.configuration.automatic_shears.maxEnergyInput").defineInRange("maxEnergyInput", 1_000, 1, Integer.MAX_VALUE);
            b.pop();

            b.comment("Fluid Reservoir (makes water while placed directly on top of an infinite water source)").push("fluid_reservoir");
            reservoirWaterPerSecond = b.comment("mB of water made per second (1000 = one bucket a second).")
                    .defineInRange("waterPerSecond", 1_000, 1, 1_000_000);
            reservoirCapacity = b.comment("Internal tank size in mB (10000 = 10 buckets).")
                    .defineInRange("capacity", 10_000, 1_000, 1_000_000);
            reservoirRequireInfinite = b.comment("Only make water over an infinite source: a water source with at least two neighbouring",
                            "water sources and something solid (or more water) under it, i.e. one that refills itself. false = any water source works.")
                    .define("requireInfiniteSource", true);
            reservoirOutputPerTick = b.comment("mB pushed each tick into neighbouring tanks (sides and top), e.g. a Mycelial Generator. 0 = no pushing.")
                    .defineInRange("outputPerTick", 1_000, 0, 1_000_000);
            b.pop();

            b.comment("Hume Energy Cube (FE storage; touching cubes join into one battery)").push("energy_cube");
            energyCubeCapacityPerCube = b.comment("FE each cube adds to its group's buffer.")
                    .defineInRange("capacityPerCube", 1_000_000, 1_000, Integer.MAX_VALUE);
            energyCubeTransferPerCube = b.comment("FE/t each cube adds to its group's transfer rate (in, out and pushing).")
                    .defineInRange("transferPerCube", 10_000, 1, Integer.MAX_VALUE);
            energyCubeMaxGroupSize = b.comment("Most cubes that join one group. Cubes beyond this form another group.")
                    .defineInRange("maxGroupSize", 64, 1, 4096);
            energyCubeAutoOutput = b.comment("Push energy into machines and cables touching the group every tick. false = they must pull it.")
                    .translation("barelyfunctionalcontraptions.configuration.energy_cube.autoOutput").define("autoOutput", true);
            b.pop();

            b.comment("Fluid Tank (stores one fluid; touching tanks join into one tank)").push("fluid_tank");
            tankCapacityPerTank = b.comment("mB each tank adds to its group's capacity (16000 = 16 buckets).")
                    .translation("barelyfunctionalcontraptions.configuration.fluid_tank.capacityPerTank")
                    .defineInRange("capacityPerTank", 16_000, 1_000, 100_000_000);
            tankTransferPerTank = b.comment("mB/t each tank adds to its group's transfer rate (in, out and pushing). 1000 = a bucket a tick.")
                    .translation("barelyfunctionalcontraptions.configuration.fluid_tank.transferPerTank")
                    .defineInRange("transferPerTank", 1_000, 1, 1_000_000);
            tankMaxGroupSize = b.comment("Most tanks that join one group. Tanks beyond this form another group.")
                    .translation("barelyfunctionalcontraptions.configuration.fluid_tank.maxGroupSize")
                    .defineInRange("maxGroupSize", 64, 1, 4096);
            tankAutoOutput = b.comment("Push fluid into machines and pipes touching the group every tick (never into other tanks).",
                            "false = they must pull it.")
                    .translation("barelyfunctionalcontraptions.configuration.fluid_tank.autoOutput")
                    .define("autoOutput", true);
            b.pop();

            b.comment("Zero Point Energy Module (two modules in one column, facing each other, generate FE across the gap)").push("zero_point_energy_module");
            zpeEnergyPerBlock = b.comment("FE/t a linked pair makes for every block of open space between the modules.",
                            "The two modules share one pool, so all of it can be taken from either module.",
                            "Default: a 10-block gap makes 10,000 FE/t.")
                    .defineInRange("energyPerBlock", 1000, 0, 1_000_000);
            zpeMinGap = b.comment("Fewest blocks of open space needed between the two modules before they link.")
                    .defineInRange("minGap", 1, 1, 256);
            zpeMaxRange = b.comment("Furthest apart (in blocks) two modules can be and still link.")
                    .defineInRange("maxRange", 64, 2, 384);
            zpeCapacity = b.comment("FE each module adds to the pair's shared buffer (a linked pair holds twice this).")
                    .translation("barelyfunctionalcontraptions.configuration.zero_point_energy_module.energyCapacity").defineInRange("energyCapacity", 50_000, 1, Integer.MAX_VALUE);
            zpeMaxOutput = b.comment("Most FE/t each module pushes into the machines and cables touching it (and that they can pull).",
                            "A linked module can always give out at least its pair's whole output, even when that is more than this,",
                            "so all of the pair's power can be taken from either module.")
                    .defineInRange("maxOutput", 100_000, 1, Integer.MAX_VALUE);
            zpeBeamDamage = b.comment("Damage the beam does to anything living that touches it (2 = one heart), at most twice a second",
                            "while it stays in the beam (the usual hurt cooldown). 0 = the beam is harmless and doesn't burn.")
                    .defineInRange("beamDamage", 4.0, 0.0, 1_000.0);
            zpeBeamBurnSeconds = b.comment("Seconds the beam sets things on fire for. 0 = no fire, only the beam's own damage.")
                    .defineInRange("beamBurnSeconds", 5, 0, 600);
            zpeBeamLooting = b.comment("Looting level for mobs the beam kills: each stack they drop gets 0 to this many extra items,",
                            "like a Looting sword. 0 = normal drops.")
                    .defineInRange("beamLooting", 1, 0, 10);
            zpeBeamPlayerKill = b.comment("Mobs the beam kills count as killed by a player: they drop experience and player-only loot",
                            "(like a zombie's rare iron ingot). The kill is credited to a fake player called [Zero Point Beam].")
                    .define("beamCountsAsPlayerKill", true);
            b.pop();

            b.comment("Energy Cable (touching cables form one network that carries FE between generators, cubes and machines)").push("energy_cable");
            cableTransferRate = b.comment("Most FE/t one cable network carries in total, however many cables and blocks are on it.")
                    .translation("barelyfunctionalcontraptions.configuration.energy_cable.transferRate").defineInRange("transferRate", 100_000, 1, Integer.MAX_VALUE);
            cableMaxNetworkSize = b.comment("Most cables in one network. Longer runs split into separate networks, which still pass",
                            "energy along only through the blocks plugged into both.")
                    .translation("barelyfunctionalcontraptions.configuration.energy_cable.maxNetworkSize").defineInRange("maxNetworkSize", 4096, 1, 1_000_000);
            b.pop();

            b.comment("Fluid Pipe (touching pipes form one network that carries fluids between tanks)").push("fluid_pipe");
            pipeTransferRate = b.comment("Most mB per tick one pipe network carries in total, however many pipes and blocks are on it.")
                    .translation("barelyfunctionalcontraptions.configuration.fluid_pipe.transferRate").defineInRange("transferRate", 4_000, 1, Integer.MAX_VALUE);
            pipeMaxNetworkSize = b.comment("Most pipes in one network. Longer runs split into separate networks, which only pass",
                            "fluid along through the blocks plugged into both.")
                    .translation("barelyfunctionalcontraptions.configuration.fluid_pipe.maxNetworkSize").defineInRange("maxNetworkSize", 4096, 1, 1_000_000);
            b.pop();

            b.comment("Item Pipe (touching pipes form one network that carries items between inventories)").push("item_pipe");
            itemPipeTransferRate = b.comment("Most items per tick one pipe network carries in total, however many pipes and blocks are on it.")
                    .translation("barelyfunctionalcontraptions.configuration.item_pipe.transferRate").defineInRange("transferRate", 16, 1, Integer.MAX_VALUE);
            itemPipeMaxNetworkSize = b.comment("Most pipes in one network. Longer runs split into separate networks, which only pass",
                            "items along through the blocks plugged into both.")
                    .translation("barelyfunctionalcontraptions.configuration.item_pipe.maxNetworkSize").defineInRange("maxNetworkSize", 4096, 1, 1_000_000);
            b.pop();

            b.comment("Which items /barelyfunctionalcontraptions generate may turn into wool types (feeding items and separator outputs).",
                    "Applies to generate, generate all, preview, material, item and hand. Existing definitions are not touched.").push("generator");
            generatorAllowedTabs = b.comment("Creative tabs an item must appear in. Default: Building Blocks, Ingredients and Food & Drinks.",
                            "Modded items count when their mod adds them to one of these tabs. Empty = any creative tab.")
                    .defineListAllowEmpty("allowedCreativeTabs",
                            List.of("minecraft:building_blocks", "minecraft:ingredients", "minecraft:food_and_drinks"),
                            () -> "minecraft:ingredients", o -> o instanceof String str && net.minecraft.resources.Identifier.tryParse(str) != null);
            generatorRequireStackable = b.comment("Blacklist every item that does not stack (max stack size 1): tools, armour, buckets, potions, stews...")
                    .define("requireStackable", true);
            b.pop();
        }

        public Set<RelativeSide> sides(ModConfigSpec.ConfigValue<List<? extends String>> value) {
            EnumSet<RelativeSide> set = EnumSet.noneOf(RelativeSide.class);
            for (String s : get(value)) {
                RelativeSide side = RelativeSide.parse(s);
                if (side != null) set.add(side);
            }
            return set;
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue ambientParticles;
        public final ModConfigSpec.DoubleValue dyeTintStrength;
        public final ModConfigSpec.BooleanValue showSheepSparkleOverlay;
        public final ModConfigSpec.BooleanValue showResourceGrowths;
        public final ModConfigSpec.BooleanValue detailedWoolTooltips;
        public final ModConfigSpec.BooleanValue detailedMachineTooltips;

        Client(ModConfigSpec.Builder b) {
            ambientParticles = b.comment("Occasionally emit resource-coloured particles from infused sheep.")
                    .define("ambientParticles", true);
            dyeTintStrength = b.comment("How strongly a sheep's dye colour shows through infused wool (0 = pure resource colour, 1 = pure dye colour).",
                            "Only applies when the server option preserveWoolColour is enabled.")
                    .defineInRange("dyeTintStrength", 0.3, 0.0, 1.0);
            showSheepSparkleOverlay = b.comment("Render the glinting accent overlay on infused sheep wool.")
                    .define("showSheepSparkleOverlay", true);
            showResourceGrowths = b.comment("Render small blocks of the infused resource growing out of the sheep's fleece (fewer as the infusion is used up).")
                    .define("showResourceGrowths", true);
            detailedWoolTooltips = b.comment("Show separation output, energy and time in resource wool tooltips (otherwise only while holding Shift).")
                    .define("detailedWoolTooltips", false);
            detailedMachineTooltips = b.comment("Show extended status tooltips (energy per tick, recipe time) in the Mechanical Separator GUI.")
                    .define("detailedMachineTooltips", true);
        }
    }

    private static boolean isSide(Object o) {
        return o instanceof String s && RelativeSide.parse(s) != null;
    }

    static String upper(String s) {
        return s.trim().toUpperCase(Locale.ROOT);
    }

    /** Small convenience for lambdas that need lazily-evaluated config values. */
    public static <T> Supplier<T> lazy(ModConfigSpec.ConfigValue<T> value) {
        return () -> get(value);
    }
}
