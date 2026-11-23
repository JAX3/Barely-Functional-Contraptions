package com.jax3.barelyfunctionalcontraptions.sheep.gametest;

import com.jax3.barelyfunctionalcontraptions.compat.McTest;
import com.jax3.barelyfunctionalcontraptions.compat.Vanilla;
import com.google.gson.JsonParser;
import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RedstoneMode;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SeparatorInventory;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.Separation;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature;

/**
 * In-world integration tests, run with {@code ./gradlew runGameTestServer} (a real dedicated-server environment,
 * which also proves no client classes are loaded on the server).
 */
public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ResourcefulSheep.MOD_ID);
    private static final Identifier STRUCTURE = ResourcefulSheep.id("test_platform");
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, Consumer<GameTestHelper>> RELOAD_TESTS = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, Consumer<GameTestHelper>> SOLO_TESTS = new java.util.LinkedHashMap<>();
    /**
     * Echo Chamber and Static Generator tests: each in a batch of its own (so no other test's noise reaches an Echo
     * Chamber, and a Static Generator calling sheep can't lure another test's away), on a bigger empty structure.
     */
    private static final java.util.Map<String, Consumer<GameTestHelper>> ECHO_TESTS = new java.util.LinkedHashMap<>();
    private static final Identifier BIG_STRUCTURE = ResourcefulSheep.id("tall_empty");

    /** The one test that needs more than 400 ticks: a whole bale takes 45 seconds. */
    private static final String LONG_BALE_TEST = "separator_separates_a_whole_bale";

    static {
        TESTS.put("definitions_loaded", ModGameTests::definitionsLoaded);
        TESTS.put("feed_iron_block_infuses_sheep", ModGameTests::feedIronBlock);
        TESTS.put("unrecognised_item_is_ignored", ModGameTests::unrecognisedItemIgnored);
        TESTS.put("shearing_infused_sheep_drops_resource_wool", ModGameTests::shearInfused);
        TESTS.put("shearing_plain_sheep_drops_normal_wool", ModGameTests::shearPlain);
        TESTS.put("infusion_runs_out_after_uses", ModGameTests::infusionRunsOut);
        TESTS.put("dispenser_shears_infused_sheep", ModGameTests::dispenserShears);
        TESTS.put("infusion_survives_save_and_load", ModGameTests::infusionPersists);
        TESTS.put("infusion_survives_eating_grass", ModGameTests::infusionSurvivesEatingGrass);
        TESTS.put("unloaded_infusion_is_kept_but_replaceable", ModGameTests::unloadedInfusionKept);
        TESTS.put("automatic_shears_shear_sheep_in_front", ModGameTests::autoShearsShearsArea);
        TESTS.put("automatic_shears_stop_when_full", ModGameTests::autoShearsStopWhenFull);
        TESTS.put("automatic_shears_redstone_modes", ModGameTests::autoShearsRedstone);
        TESTS.put("automatic_shears_need_power", ModGameTests::autoShearsNeedPower);
        TESTS.put("automatic_shears_hopper_output_only_sheep", ModGameTests::autoShearsHopperAndTag);
        TESTS.put("replacing_infusion_is_refused_by_default", ModGameTests::replaceRefused);
        TESTS.put("separator_produces_raw_iron_and_string", ModGameTests::separatorProcesses);
        TESTS.put("separator_halts_without_energy", ModGameTests::separatorNoEnergy);
        TESTS.put("separator_automation_sides", ModGameTests::separatorAutomationSides);
        TESTS.put("separator_hopper_chain", ModGameTests::separatorHopperChain);
        TESTS.put("separator_never_overfills_output", ModGameTests::separatorOutputFull);
        TESTS.put("separator_state_survives_save_and_load", ModGameTests::separatorPersists);
        TESTS.put("separating_recipe_handles_plain_wool", ModGameTests::separatingRecipe);
        TESTS.put("config_multipliers_affect_values", ModGameTests::configMultipliers);
        TESTS.put("custom_definition_with_tag_parses", ModGameTests::customDefinitionParses);
        TESTS.put("creative_generator_powers_separator", ModGameTests::creativeGeneratorPowers);
        TESTS.put("generator_creates_valid_definitions", ModGameTests::generatorCreatesDefinitions);
        TESTS.put("generator_dedupe_removes_duplicate_wool", ModGameTests::generatorDedupe);
        TESTS.put("generator_pick_and_choose", ModGameTests::generatorPickAndChoose);
        TESTS.put("separator_output_rolls_two_to_four", ModGameTests::outputRange);
        TESTS.put("generator_filter_limits_tabs_and_stacking", ModGameTests::generatorFilter);
        TESTS.put("fluid_reservoir_fills_over_infinite_source", ModGameTests::reservoirFills);
        TESTS.put("fluid_reservoir_needs_infinite_source", ModGameTests::reservoirNeedsInfiniteSource);
        TESTS.put("fluid_reservoir_buckets_and_pipes", ModGameTests::reservoirBucketsAndPipes);
        TESTS.put("fluid_reservoir_config_rate_and_size", ModGameTests::reservoirConfig);
        TESTS.put("infused_sheep_grow_resource_blocks", ModGameTests::growthBlocks);
        TESTS.put("energy_cube_single", ModGameTests::cubeSingle);
        TESTS.put("energy_cube_group_scales", ModGameTests::cubeGroupScales);
        TESTS.put("energy_cube_break_splits_group", ModGameTests::cubeBreakSplits);
        TESTS.put("energy_cube_pushes_into_machines", ModGameTests::cubePushes);
        TESTS.put("energy_cube_max_group_size", ModGameTests::cubeMaxGroup);
        TESTS.put("energy_cube_window_shows_charge", ModGameTests::cubeWindow);
        TESTS.put("energy_cube_gui_values", ModGameTests::cubeGui);
        TESTS.put("energy_cube_stack_fills_bottom_up", ModGameTests::cubeStack);
        TESTS.put("creative_tabs_split_wool", ModGameTests::creativeTabs);
        TESTS.put("resource_spawn_eggs_spawn_infused_sheep", ModGameTests::spawnEggs);
        TESTS.put("killing_infused_sheep_drops_more_than_shearing", ModGameTests::killInfused);
        TESTS.put("machines_are_built_from_functional_machine_block", ModGameTests::machineBlockRecipes);
        TESTS.put("all_blocks_use_iron_block_sounds", ModGameTests::ironSounds);
        TESTS.put("zero_point_modules_generate_across_a_gap", ModGameTests::zeroPointModules);
        TESTS.put("zero_point_beam_burns_what_touches_it", ModGameTests::zeroPointBeamBurns);
        TESTS.put("zero_point_beam_kills_have_looting", ModGameTests::zeroPointBeamLooting);
        TESTS.put("zero_point_modules_link_sideways", ModGameTests::zeroPointSideways);
        TESTS.put("bred_lambs_keep_the_infusion", ModGameTests::breedingKeepsInfusion);
        TESTS.put("fluid_tank_single", ModGameTests::tankSingle);
        TESTS.put("fluid_tank_group_fills_bottom_up", ModGameTests::tankGroup);
        TESTS.put("fluid_tank_different_fluids_stay_apart", ModGameTests::tankFluidsApart);
        TESTS.put("fluid_tank_keeps_its_share_when_broken", ModGameTests::tankBreak);
        TESTS.put("cube_and_tank_keep_contents_through_a_player", ModGameTests::keepContentsThroughPlayer);
        TESTS.put("fluid_tank_buckets", ModGameTests::tankBuckets);
        TESTS.put("breeding_inheritance_rules", ModGameTests::breedingRules);
        TESTS.put("sheep_stats_are_bred", ModGameTests::sheepStatsBred);
        TESTS.put("sheep_stats_yield_endurance_growth", ModGameTests::sheepStatsEffects);
        TESTS.put("infusion_altar_infuses_a_wither_sheep", ModGameTests::altarWither);
        TESTS.put("infusion_altar_waits_for_what_it_needs", ModGameTests::altarWaits);
        TESTS.put("infusion_altar_pulses_when_ready", ModGameTests::altarPulse);
        TESTS.put("list_command_lists_every_sheep", ModGameTests::listCommand);
        TESTS.put("snow_sheep_leaves_snow", ModGameTests::snowSheep);
        TESTS.put("chiller_freezes_water_with_snow_sheep", ModGameTests::chiller);
        TESTS.put("powered_furnace_smelts_faster_with_blaze_sheep", ModGameTests::poweredFurnace);
        TESTS.put("woollen_armor_takes_the_wool_colour", ModGameTests::woollenArmorColour);
        TESTS.put("wrench_dismantles_machines_into_the_inventory", ModGameTests::wrenchDismantles);
        TESTS.put("woollen_boots_muffle_the_wearer", ModGameTests::woollenBootsMuffle);
        if (ModBlocks.HUME_FIELD_GENERATOR != null) TESTS.put("hume_field_generator_is_two_by_two", ModGameTests::humeField);
        TESTS.put("echo_chamber_is_three_by_three", ModGameTests::echoChamber);
        ECHO_TESTS.put("echo_chamber_counts_its_warden_sheep", ModGameTests::echoCountsHerd);
        ECHO_TESTS.put("echo_chamber_power_follows_the_noise", ModGameTests::echoPower);
        ECHO_TESTS.put("echo_chamber_hears_vibrations_but_not_through_wool", ModGameTests::echoHearing);
        ECHO_TESTS.put("echo_chamber_pushes_power_out_of_its_port", ModGameTests::echoPort);
        ECHO_TESTS.put("echo_chamber_falls_asleep_in_silence", ModGameTests::echoSleep);
        ECHO_TESTS.put("echo_chamber_shrieks_when_too_loud", ModGameTests::echoShriek);
        TESTS.put("echo_chamber_recipe_and_drop", ModGameTests::echoRecipeAndDrop);
        ECHO_TESTS.put("static_generator_counts_sheep_at_its_combs", ModGameTests::staticCountsSheep);
        ECHO_TESTS.put("static_generator_combs_oxidise_and_wax", ModGameTests::staticCombs);
        ECHO_TESTS.put("static_generator_arcs_into_its_buffer", ModGameTests::staticArcs);
        ECHO_TESTS.put("static_generator_shocks_knock_back_unless_insulated", ModGameTests::staticShocks);
        ECHO_TESTS.put("grass_seeder_regrasses_eaten_dirt_with_bone_meal", ModGameTests::seederBoneMeal);
        ECHO_TESTS.put("grass_seeder_runs_on_power_and_water_first", ModGameTests::seederPower);
        ECHO_TESTS.put("grass_seeder_grows_short_grass_then_rests", ModGameTests::seederShortGrass);
        ECHO_TESTS.put("grass_seeder_gets_sheared_sheep_grazing", ModGameTests::seederGrazing);
        ECHO_TESTS.put("grass_seeder_overgrows_unless_trimmed", ModGameTests::seederOvergrowth);
        ECHO_TESTS.put("automatic_shears_cant_see_sheep_in_tall_grass", ModGameTests::shearsTallGrass);
        ECHO_TESTS.put("herd_counter_counts_its_pen", ModGameTests::herdCounterCounts);
        ECHO_TESTS.put("herd_counter_full_mode_waits_for_its_target", ModGameTests::herdCounterFull);
        ECHO_TESTS.put("herd_counter_double_counts_dashing_sheep_unless_steady", ModGameTests::herdCounterSteady);
        ECHO_TESTS.put("status_panel_shows_its_linked_machines", ModGameTests::statusPanel);
        ECHO_TESTS.put("status_panels_join_into_one_screen_up_to_3x3", ModGameTests::statusPanelJoin);
        ECHO_TESTS.put("status_panel_window_names_orders_and_alerts", ModGameTests::statusPanelWindow);
        ECHO_TESTS.put("status_panel_links_big_machines_from_any_of_their_blocks", ModGameTests::statusPanelBigMachines);
        ECHO_TESTS.put("crate_holds_one_kind_of_item_and_locks_to_it", ModGameTests::crateStores);
        ECHO_TESTS.put("crate_bales_make_it_bigger_and_crates_stay_out", ModGameTests::crateBales);
        ECHO_TESTS.put("crate_lining_matches_its_item_unless_dyed", ModGameTests::crateLining);
        ECHO_TESTS.put("crate_fills_from_hoppers_and_pipes_and_reads_on_a_comparator", ModGameTests::crateAutomation);
        ECHO_TESTS.put("crate_bulges_then_bursts_unless_strapped", ModGameTests::crateBursts);
        ECHO_TESTS.put("crate_a_hopper_pushing_at_a_full_crate_strains_it", ModGameTests::crateHopperStrains);
        ECHO_TESTS.put("crate_controller_takes_in_the_crates_touching_it", ModGameTests::controllerJoins);
        ECHO_TESTS.put("crate_controller_puts_items_where_they_belong", ModGameTests::controllerRoutes);
        ECHO_TESTS.put("crate_controller_links_far_crates_with_their_neighbours", ModGameTests::controllerLinks);
        ECHO_TESTS.put("crate_controller_leaves_another_controllers_crates", ModGameTests::controllerTaken);
        ECHO_TESTS.put("crate_controller_strains_a_full_crate_and_reports_it", ModGameTests::controllerFull);
        ECHO_TESTS.put("crate_controller_fills_from_a_hopper", ModGameTests::controllerHopper);
        ECHO_TESTS.put("crate_controller_crates_go_free_when_it_goes", ModGameTests::controllerGone);
        ECHO_TESTS.put("crate_keeps_its_items_lining_and_strap_when_broken", ModGameTests::crateKeeps);
        TESTS.put("sheep_treadmill_is_two_blocks_long", ModGameTests::sheepTreadmill);
        TESTS.put("sheep_treadmill_makes_power_from_a_walking_sheep", ModGameTests::sheepTreadmillPower);
        TESTS.put("sheep_treadmill_calls_a_sheep_over", ModGameTests::sheepTreadmillAttracts);
        TESTS.put("separator_takes_power_fast_enough_for_a_wither_bale", ModGameTests::separatorFeedsCostlyBales);
        TESTS.put("thermal_reaction_chamber_makes_power_from_pairs", ModGameTests::thermalPower);
        TESTS.put("thermal_reaction_chamber_runs_dry_and_vents_steam", ModGameTests::thermalDry);
        TESTS.put("thermal_reaction_chamber_spent_fleece_stops_counting", ModGameTests::thermalFatigue);
        TESTS.put("thermal_reaction_chamber_keeps_its_balance_when_full", ModGameTests::thermalFull);
        TESTS.put("chiller_calls_a_stray_snow_sheep_back", ModGameTests::chillerAttracts);
        TESTS.put("wrench_has_its_common_tags", ModGameTests::wrenchTags);
        TESTS.put("thermal_reaction_chamber_calls_a_stray_back_to_even_its_herds", ModGameTests::thermalAttracts);
        SOLO_TESTS.put("thermal_reaction_chamber_melts_down_when_hot", ModGameTests::thermalMeltdown);
        SOLO_TESTS.put("thermal_reaction_chamber_freezes_solid_when_cold", ModGameTests::thermalFreeze);
        TESTS.put("energy_cable_connects_to_cables_and_machines", ModGameTests::cableConnects);
        TESTS.put("energy_cable_carries_generator_power", ModGameTests::cableCarriesPower);
        TESTS.put("energy_cable_cube_powers_machines_not_cubes", ModGameTests::cableCubeToMachine);
        TESTS.put("energy_cable_respects_transfer_rate", ModGameTests::cableRate);
        TESTS.put("energy_cable_break_splits_network", ModGameTests::cableBreakSplits);
        TESTS.put("energy_cable_carries_zero_point_power", ModGameTests::cableZeroPoint);
        TESTS.put("item_pipe_connects_to_pipes_and_inventories", ModGameTests::itemPipeConnects);
        TESTS.put("item_pipe_shears_to_separator_to_chest", ModGameTests::itemPipeChain);
        TESTS.put("item_pipe_hopper_pushes_but_chests_are_not_drained", ModGameTests::itemPipePushAndChests);
        TESTS.put("item_pipe_respects_transfer_rate", ModGameTests::itemPipeRate);
        TESTS.put("item_pipe_pull_side_drains_a_chest_through_a_filter", ModGameTests::itemPipePullFilter);
        TESTS.put("item_pipe_side_modes_and_destination_filters", ModGameTests::itemPipeSideModes);
        TESTS.put("wrench_sets_pipe_modes_and_filters", ModGameTests::wrenchConfiguresPipes);
        TESTS.put("themed_sheep_have_their_features", ModGameTests::themedSheep);
        TESTS.put("glowing_sheep_lights_the_block_it_stands_in", ModGameTests::glowingSheepLight);
        TESTS.put("ghast_sheep_screams_like_a_ghast", ModGameTests::ghastSheepSounds);
        TESTS.put("ghast_sheep_is_twice_the_size", ModGameTests::ghastSheepScale);
        TESTS.put("themed_sheep_advancements", ModGameTests::themedAdvancements);
        TESTS.put("energy_meter_measures_what_passes_through", ModGameTests::energyMeter);
        TESTS.put("energy_meter_faces_the_right_way_when_placed", ModGameTests::energyMeterPlacement);
        TESTS.put("glow_panel_runs_on_glow_lamps", ModGameTests::glowPanel);
        TESTS.put("recipe_support_wool_ingredient_and_fuel_data_map", ModGameTests::recipeSupport);
        TESTS.put("melon_slice_gives_a_sheep_the_skin", ModGameTests::melonSliceSkin);
        TESTS.put("blaze_furnace_heats_from_blaze_sheep", ModGameTests::blazeFurnaceHeat);
        TESTS.put("blaze_furnace_blaze_rod_boost", ModGameTests::blazeFurnaceBoost);
        TESTS.put("blaze_furnace_heat_sources_are_data", ModGameTests::blazeFurnaceData);
        TESTS.put("machines_show_their_working_area", ModGameTests::machineAreaToggle);
        TESTS.put("wool_burner_burns_wool_string_and_coal_wool", ModGameTests::woolBurnerFuels);
        TESTS.put("wool_burner_waits_while_full_and_takes_only_fuel", ModGameTests::woolBurnerFullAndHoppers);
        TESTS.put("wool_packer_presses_nine_wool_into_a_bale", ModGameTests::woolPackerPresses);
        TESTS.put("wool_packer_pauses_and_works_with_hoppers", ModGameTests::woolPackerPausesAndHoppers);
        TESTS.put("wool_bale_separates_for_more", ModGameTests::woolBaleSeparationNumbers);
        TESTS.put("wool_bale_block_keeps_its_wool", ModGameTests::woolBaleBlock);
        TESTS.put("wool_bale_unpacks_into_nine_wool", ModGameTests::woolBaleUnpacks);
        TESTS.put(LONG_BALE_TEST, ModGameTests::separatorSeparatesBale);
        RELOAD_TESTS.put("generated_resource_keeps_infusion", ModGameTests::generatedResourceKeepsInfusion);
        TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
        RELOAD_TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
        SOLO_TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
        ECHO_TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
    }

    private ModGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(ResourcefulSheep.id("sheep_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, ResourcefulSheep.id(name));
            int maxTicks = name.equals(LONG_BALE_TEST) ? 1200 : 400; // a whole bale takes 45 seconds
            event.registerTest(ResourcefulSheep.id(name), new FunctionGameTestInstance(fn, new TestData<>(env, STRUCTURE, maxTicks, 0, true)));
        }
        // Tests that run /barelyfunctionalcontraptions generate (which reloads data packs) get their own batch, so the reload
        // never happens in the middle of the other tests.
        Holder<TestEnvironmentDefinition<?>> reloadEnv = event.registerEnvironment(ResourcefulSheep.id("sheep_reload"), new TestEnvironmentDefinition.AllOf());
        for (String name : RELOAD_TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, ResourcefulSheep.id(name));
            event.registerTest(ResourcefulSheep.id(name), new FunctionGameTestInstance(fn, new TestData<>(reloadEnv, STRUCTURE, 1200, 0, true)));
        }
        // Long tests that explode or freeze the blocks around them each get a batch of their own, away from the others.
        for (String name : SOLO_TESTS.keySet()) {
            Holder<TestEnvironmentDefinition<?>> soloEnv = event.registerEnvironment(ResourcefulSheep.id("solo_" + name), new TestEnvironmentDefinition.AllOf());
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, ResourcefulSheep.id(name));
            event.registerTest(ResourcefulSheep.id(name), new FunctionGameTestInstance(fn, new TestData<>(soloEnv, STRUCTURE, 1200, 0, true)));
        }
        for (String name : ECHO_TESTS.keySet()) {
            Holder<TestEnvironmentDefinition<?>> echoEnv = event.registerEnvironment(ResourcefulSheep.id("solo_" + name), new TestEnvironmentDefinition.AllOf());
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, ResourcefulSheep.id(name));
            event.registerTest(ResourcefulSheep.id(name), new FunctionGameTestInstance(fn, new TestData<>(echoEnv, BIG_STRUCTURE, 400, 0, true)));
        }
    }


    // ------------------------------------------------------------------ energy cables

    /** Places a cable the way a player would: every side worked out from what is around it. */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCableBlockEntity cable(GameTestHelper h, BlockPos pos) {
        var block = ModBlocks.ENERGY_CABLE.get();
        h.setBlock(pos, block.withConnections(block.defaultBlockState(), h.getLevel(), h.absolutePos(pos)));
        return h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCableBlockEntity.class);
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection side(GameTestHelper h, BlockPos pos, net.minecraft.core.Direction dir) {
        return h.getBlockState(pos).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCableBlock.SIDES.get(dir));
    }

    /** Cables join cables, plug into blocks with an energy connection, ignore everything else, and follow changes. */
    private static void cableConnects(GameTestHelper h) {
        var CABLE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.CABLE;
        var MACHINE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.MACHINE;
        var NONE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.NONE;
        var E = net.minecraft.core.Direction.EAST;
        var W = net.minecraft.core.Direction.WEST;
        var U = net.minecraft.core.Direction.UP;
        cube(h, new BlockPos(1, 1, 2));
        h.setBlock(new BlockPos(3, 2, 2), net.minecraft.world.level.block.Blocks.STONE);
        var a = cable(h, new BlockPos(2, 1, 2));
        var b = cable(h, new BlockPos(3, 1, 2));
        h.assertTrue(side(h, new BlockPos(2, 1, 2), W) == MACHINE, "plugs into the cube");
        h.assertTrue(side(h, new BlockPos(2, 1, 2), E) == CABLE && side(h, new BlockPos(3, 1, 2), W) == CABLE, "joins the next cable");
        h.assertTrue(side(h, new BlockPos(3, 1, 2), U) == NONE && side(h, new BlockPos(3, 1, 2), E) == NONE, "ignores stone and air");
        h.assertTrue(a.network() == b.network() && a.network().size() == 2 && a.network().endpoints().size() == 1,
                "one network of 2 cables with the cube plugged in");
        // A machine placed next to a cable later plugs in; removing it unplugs.
        h.setBlock(new BlockPos(4, 1, 2), ModBlocks.MECHANICAL_SEPARATOR.get());
        h.assertTrue(side(h, new BlockPos(3, 1, 2), E) == MACHINE, "plugs into a separator placed beside it");
        h.assertTrue(a.network().endpoints().size() == 2, "the network sees the new machine");
        h.setBlock(new BlockPos(1, 1, 2), net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(side(h, new BlockPos(2, 1, 2), W) == NONE, "unplugs from a removed cube");
        h.assertTrue(a.network().endpoints().size() == 1, "the network forgets the removed cube");
        h.succeed();
    }

    /** Creative Generator -> 3 cables -> Hume Energy Cube: the cube charges, and the cables hold nothing themselves. */
    private static void cableCarriesPower(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 1, 2), ModBlocks.CREATIVE_GENERATOR.get());
        var first = cable(h, new BlockPos(1, 1, 2));
        cable(h, new BlockPos(2, 1, 2));
        cable(h, new BlockPos(3, 1, 2));
        var target = cube(h, new BlockPos(4, 1, 2));
        // Re-place the end cables so they plug into the blocks placed after them.
        cable(h, new BlockPos(3, 1, 2));
        cable(h, new BlockPos(1, 1, 2));
        h.succeedWhen(() -> {
            h.assertTrue(first.network().size() == 3 && first.network().endpoints().size() == 2, "3 cables, generator and cube plugged in");
            h.assertTrue(target.group().amount() >= 200_000, "the cube charged through the cables, has " + target.group().amount());
            h.assertTrue(first.getEnergyHandler(null).getAmountAsLong() == 0, "cables hold no energy");
            h.assertTrue(first.network().rate() > 0, "the network reports the FE/t it moves");
        });
    }

    /** A charged cube powers the shears through cables, but never fills the other cube on the same network. */
    private static void cableCubeToMachine(GameTestHelper h) {
        var source = cube(h, new BlockPos(0, 1, 2));
        source.storage().set(500_000);
        var other = cube(h, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(4, 1, 2), ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, net.minecraft.core.Direction.EAST));
        var shears = h.getBlockEntity(new BlockPos(4, 1, 2), com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.class);
        for (int x = 1; x <= 3; x++) cable(h, new BlockPos(x, 1, 2));
        h.succeedWhen(() -> {
            h.assertTrue(shears.getEnergy().getEnergy() >= 5_000, "shears charged through the cables, has " + shears.getEnergy().getEnergy());
            h.assertTrue(other.group().amount() == 0, "the second cube must not be filled from the first, has " + other.group().amount());
            h.assertTrue(source.group().amount() == 500_000 - shears.getEnergy().getEnergy(), "energy only moved, none made or lost");
        });
    }

    /** With transferRate = 1,000 a network never moves more than 1,000 FE in a tick. */
    private static void cableRate(GameTestHelper h) {
        int old = ModConfigs.SERVER.cableTransferRate.get();
        ModConfigs.SERVER.cableTransferRate.set(1_000);
        h.setBlock(new BlockPos(0, 1, 2), ModBlocks.CREATIVE_GENERATOR.get());
        var target = cube(h, new BlockPos(2, 1, 2));
        cable(h, new BlockPos(1, 1, 2));
        long[] start = new long[1];
        h.runAfterDelay(2, () -> start[0] = target.group().amount());
        h.runAfterDelay(12, () -> {
            try {
                long gained = target.group().amount() - start[0];
                h.assertTrue(gained > 0 && gained <= 10 * 1_000, "10 ticks at 1,000 FE/t, gained " + gained);
            } finally {
                ModConfigs.SERVER.cableTransferRate.set(old);
            }
            h.succeed();
        });
    }

    /** A linked Zero Point pair charges a Hume Energy Cube through cables. */
    private static void cableZeroPoint(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 1, 1), zpe(Direction.UP));
        h.setBlock(new BlockPos(1, 4, 1), zpe(Direction.DOWN));
        var target = cube(h, new BlockPos(4, 1, 1));
        for (int x = 2; x <= 3; x++) cable(h, new BlockPos(x, 1, 1));
        h.succeedWhen(() -> h.assertTrue(target.group().amount() >= 20_000,
                "the cube charged from the Zero Point pair through the cables, has " + target.group().amount()));
    }

    /** Breaking the middle cable splits the network: the cube stops charging. */
    private static void cableBreakSplits(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 1, 2), ModBlocks.CREATIVE_GENERATOR.get());
        var target = cube(h, new BlockPos(4, 1, 2));
        var first = cable(h, new BlockPos(1, 1, 2));
        cable(h, new BlockPos(2, 1, 2));
        var last = cable(h, new BlockPos(3, 1, 2));
        long[] stopped = new long[1];
        h.runAfterDelay(3, () -> {
            h.assertTrue(target.group().amount() > 0, "charging before the break");
            h.setBlock(new BlockPos(2, 1, 2), net.minecraft.world.level.block.Blocks.AIR);
            h.assertTrue(first.network() != last.network() && first.network().size() == 1, "split into two networks of 1");
            stopped[0] = target.group().amount();
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(target.group().amount() == stopped[0], "no energy crosses the gap");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ item pipes

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ItemPipeBlockEntity itemPipe(GameTestHelper h, BlockPos pos) {
        var block = ModBlocks.ITEM_PIPE.get();
        h.setBlock(pos, block.withConnections(block.defaultBlockState(), h.getLevel(), h.absolutePos(pos)));
        return h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ItemPipeBlockEntity.class);
    }

    private static int countIn(net.minecraft.world.Container container, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) if (container.getItem(i).is(item)) n += container.getItem(i).getCount();
        return n;
    }

    private static net.minecraft.world.Container chest(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, Blocks.CHEST);
        return (net.minecraft.world.Container) h.getBlockEntity(pos, BlockEntity.class);
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity shearsAt(GameTestHelper h, BlockPos pos, Direction facing) {
        h.setBlock(pos, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, facing));
        return h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.class);
    }

    /** Item pipes join item pipes, plug into inventories, and ignore Fluid Pipes, Energy Cables and stone. */
    private static void itemPipeConnects(GameTestHelper h) {
        var CABLE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.CABLE;
        var MACHINE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.MACHINE;
        var NONE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.NONE;
        chest(h, new BlockPos(1, 1, 2));
        h.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        cable(h, new BlockPos(2, 1, 1));
        var fluid = ModBlocks.FLUID_PIPE.get();
        h.setBlock(new BlockPos(2, 1, 3), fluid.withConnections(fluid.defaultBlockState(), h.getLevel(), h.absolutePos(new BlockPos(2, 1, 3))));
        var a = itemPipe(h, new BlockPos(2, 1, 2));
        var b = itemPipe(h, new BlockPos(3, 1, 2));
        h.assertTrue(side(h, new BlockPos(2, 1, 2), Direction.WEST) == MACHINE, "plugs into the chest");
        h.assertTrue(side(h, new BlockPos(2, 1, 2), Direction.EAST) == CABLE, "joins the next item pipe");
        h.assertTrue(side(h, new BlockPos(2, 1, 2), Direction.UP) == NONE, "ignores stone");
        h.assertTrue(side(h, new BlockPos(2, 1, 2), Direction.NORTH) == NONE && side(h, new BlockPos(2, 1, 2), Direction.SOUTH) == NONE,
                "doesn't join an Energy Cable or a Fluid Pipe");
        h.assertTrue(a.network() == b.network() && a.network().size() == 2 && a.network().endpoints().size() == 1,
                "one network of 2 pipes with the chest plugged in");
        shearsAt(h, new BlockPos(4, 1, 2), Direction.EAST);
        h.assertTrue(side(h, new BlockPos(3, 1, 2), Direction.EAST) == MACHINE && a.network().endpoints().size() == 2,
                "plugs into Automatic Shears placed later");
        h.succeed();
    }

    /**
     * Automatic Shears full of Iron Wool -> pipes -> a powered Mechanical Separator's side, and its bottom output ->
     * pipes -> a chest: the separator gets the wool and the chest ends up with Raw Iron.
     */
    private static void itemPipeChain(GameTestHelper h) {
        var shears = shearsAt(h, new BlockPos(0, 1, 2), Direction.WEST);
        shears.setItem(0, ResourceWoolItem.create(IRON, 4));
        MechanicalSeparatorBlockEntity separator = separator(h, new BlockPos(2, 2, 2));
        insertEnergy(separator.getEnergy(), 200_000);
        var chest = chest(h, new BlockPos(4, 1, 2));
        for (BlockPos pos : List.of(new BlockPos(1, 1, 2), new BlockPos(1, 2, 2), new BlockPos(2, 1, 2), new BlockPos(3, 1, 2))) itemPipe(h, pos);
        h.succeedWhen(() -> {
            h.assertTrue(shears.isEmpty(), "the pipes emptied the shears");
            h.assertTrue(countIn(chest, Items.RAW_IRON) >= 2, "Raw Iron from the separator reached the chest, has " + countIn(chest, Items.RAW_IRON));
        });
    }

    /** A hopper pushes into a pipe that carries the items on to a chest; the pipes never drain that chest into another. */
    private static void itemPipePushAndChests(GameTestHelper h) {
        h.setBlock(new BlockPos(1, 2, 2), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = h.getBlockEntity(new BlockPos(1, 2, 2), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
        var first = chest(h, new BlockPos(3, 1, 2));
        first.setItem(0, new ItemStack(Items.DIAMOND, 10));
        var second = chest(h, new BlockPos(3, 1, 4));
        itemPipe(h, new BlockPos(1, 1, 2));
        itemPipe(h, new BlockPos(2, 1, 2));
        itemPipe(h, new BlockPos(2, 1, 3));
        itemPipe(h, new BlockPos(2, 1, 4));
        h.succeedWhen(() -> {
            h.assertTrue(countIn(first, Items.COBBLESTONE) + countIn(second, Items.COBBLESTONE) == 5, "the hopper's 5 cobblestone reached the chests");
            h.assertTrue(countIn(first, Items.DIAMOND) == 10 && countIn(second, Items.DIAMOND) == 0, "the pipes don't move diamonds between chests");
        });
    }

    /** Sets one side of a pipe and updates its block state, as a right-click on it would. */
    private static void setPipeSide(GameTestHelper h, BlockPos pos, Direction dir, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide side) {
        var pipe = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeBlockEntity.class);
        pipe.setSide(dir, side);
        var block = (com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock) h.getBlockState(pos).getBlock();
        h.getLevel().setBlock(h.absolutePos(pos), block.withConnections(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos)), 3);
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide side(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode mode, Identifier... filter) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide(mode, List.of(filter));
    }

    /** A Pull side empties a chest (which a normal side never does), and its filter lets only iron through. */
    private static void itemPipePullFilter(GameTestHelper h) {
        var source = chest(h, new BlockPos(1, 1, 2));
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 10));
        source.setItem(1, new ItemStack(Items.GOLD_INGOT, 10));
        itemPipe(h, new BlockPos(2, 1, 2));
        itemPipe(h, new BlockPos(3, 1, 2));
        var target = chest(h, new BlockPos(4, 1, 2));
        setPipeSide(h, new BlockPos(2, 1, 2), Direction.WEST, side(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PULL, Identifier.withDefaultNamespace("iron_ingot")));
        h.assertTrue(h.getBlockState(new BlockPos(2, 1, 2)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.SIDES.get(Direction.WEST))
                == com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.PULL, "the pulling side shows a pull plug");
        h.succeedWhen(() -> {
            h.assertTrue(countIn(target, Items.IRON_INGOT) == 10, "all 10 iron pulled out of the chest, got " + countIn(target, Items.IRON_INGOT));
            h.assertTrue(countIn(source, Items.GOLD_INGOT) == 10 && countIn(target, Items.GOLD_INGOT) == 0, "the gold stays: the filter only lets iron through");
            // The setting is saved with the pipe.
            var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
            h.getBlockEntity(new BlockPos(2, 1, 2), BlockEntity.class).saveWithoutMetadata(saved);
            String tag = saved.buildResult().toString();
            h.assertTrue(tag.contains("pull") && tag.contains("minecraft:iron_ingot"), "the side's mode and filter are saved: " + tag);
        });
    }

    /**
     * Wool from Automatic Shears skips a chest whose side only lets diamonds in, a Push side is never pulled from, and
     * an Off side has no connection at all.
     */
    private static void itemPipeSideModes(GameTestHelper h) {
        var shears = shearsAt(h, new BlockPos(1, 1, 2), Direction.WEST);
        shears.setItem(0, new ItemStack(Vanilla.wool(DyeColor.WHITE), 8));
        itemPipe(h, new BlockPos(2, 1, 2));
        itemPipe(h, new BlockPos(3, 1, 2));
        var diamondsOnly = chest(h, new BlockPos(3, 1, 1));
        var anything = chest(h, new BlockPos(4, 1, 2));
        setPipeSide(h, new BlockPos(3, 1, 2), Direction.NORTH, side(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.NORMAL, Identifier.withDefaultNamespace("diamond")));
        // Clicks map to sides: the end of an arm, or the face of the core.
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.clickedSide(at,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at).add(-0.45, 0, 0), Direction.WEST, at, false)) == Direction.WEST, "a click on the west plug picks west");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.clickedSide(at,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 0.1875, 0), Direction.UP, at, false)) == Direction.UP, "a click on the top of the core picks up");
        h.runAfterDelay(20, () -> {
            h.assertTrue(countIn(anything, Vanilla.wool(DyeColor.WHITE)) == 8 && countIn(diamondsOnly, Vanilla.wool(DyeColor.WHITE)) == 0,
                    "all the wool went to the unfiltered chest: " + countIn(anything, Vanilla.wool(DyeColor.WHITE)) + " / " + countIn(diamondsOnly, Vanilla.wool(DyeColor.WHITE)));
            // Push: the pipe stops taking wool out of the shears.
            setPipeSide(h, new BlockPos(2, 1, 2), Direction.WEST, side(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PUSH));
            shears.setItem(0, new ItemStack(Vanilla.wool(DyeColor.WHITE), 8));
            h.runAfterDelay(20, () -> {
                h.assertTrue(countIn(shears, Vanilla.wool(DyeColor.WHITE)) == 8, "a Push side is never pulled from");
                // Off: no plug and no connection.
                setPipeSide(h, new BlockPos(4, 1, 2).west(), Direction.EAST, side(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.DISABLED));
                h.assertTrue(h.getBlockState(new BlockPos(3, 1, 2)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.SIDES.get(Direction.EAST))
                        == com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.NONE, "an Off side has no plug");
                h.assertTrue(h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, h.absolutePos(new BlockPos(3, 1, 2)), Direction.EAST) == null,
                        "and offers no connection to the chest");
                h.succeed();
            });
        });
    }

    /** The Barely Functional Wrench sets pipe sides; an empty hand or a held item no longer does. */
    private static void wrenchConfiguresPipes(GameTestHelper h) {
        chest(h, new BlockPos(1, 1, 2));
        var pipe = itemPipe(h, new BlockPos(2, 1, 2));
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        var westPlug = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at).add(-0.45, 0, 0), Direction.WEST, at, false);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()));
        java.util.function.Supplier<net.minecraft.world.InteractionResult> wrench = () -> com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.useWrench(
                h.getLevel(), at, player, InteractionHand.MAIN_HAND, westPlug);
        var PULL = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PULL;
        var PUSH = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PUSH;
        // Wrench: cycle the mode, and the plug follows.
        h.assertTrue(wrench.get().consumesAction() && pipe.side(Direction.WEST).mode() == PULL, "one wrench click: Pull, got " + pipe.side(Direction.WEST).mode());
        h.assertTrue(h.getBlockState(new BlockPos(2, 1, 2)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.SIDES.get(Direction.WEST))
                == com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.PULL, "the plug turns orange");
        wrench.get();
        h.assertTrue(pipe.side(Direction.WEST).mode() == PUSH, "a second click: Push");
        // Wrench with an item in the other hand: toggles it in the filter.
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT));
        wrench.get();
        h.assertTrue(pipe.side(Direction.WEST).filter().equals(List.of(Identifier.withDefaultNamespace("iron_ingot"))) && pipe.side(Direction.WEST).mode() == PUSH,
                "the off-hand iron is added to the filter, mode kept: " + pipe.side(Direction.WEST));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GOLD_INGOT));
        wrench.get();
        h.assertTrue(pipe.side(Direction.WEST).filter().size() == 2, "gold added too");
        wrench.get();
        h.assertTrue(pipe.side(Direction.WEST).filter().size() == 1, "clicking with gold again removes it");
        // The filter grid (sneak + wrench opens it): drive the menu as the screen would.
        var menu = new com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu(1, player.getInventory(), pipe, Direction.WEST);
        h.assertTrue(menu.getSlot(0).getItem().is(Items.IRON_INGOT) && menu.mode() == PUSH, "the grid shows the filter and the mode");
        menu.setCarried(new ItemStack(Items.DIAMOND, 5));
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        h.assertTrue(pipe.side(Direction.WEST).filter().equals(List.of(Identifier.withDefaultNamespace("diamond"))) && menu.getSlot(0).getItem().is(Items.DIAMOND),
                "clicking a slot with diamonds puts diamond there: " + pipe.side(Direction.WEST));
        h.assertTrue(menu.getCarried().getCount() == 5, "the carried diamonds aren't used up");
        menu.clicked(4, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        h.assertTrue(pipe.side(Direction.WEST).filter().size() == 1, "the same item twice is only listed once");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().setItem(9, new ItemStack(Items.EMERALD, 3));
        int emeraldSlot = menu.slots.indexOf(menu.slots.stream().filter(sl -> sl.container == player.getInventory() && sl.getContainerSlot() == 9).findFirst().orElseThrow());
        menu.quickMoveStack(player, emeraldSlot);
        h.assertTrue(pipe.side(Direction.WEST).filter().size() == 2 && menu.getSlot(1).getItem().is(Items.EMERALD)
                && player.getInventory().getItem(9).getCount() == 3, "shift-clicking emeralds adds them and keeps them in the inventory");
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        h.assertTrue(pipe.side(Direction.WEST).filter().equals(List.of(Identifier.withDefaultNamespace("emerald"))), "clicking with nothing carried removes the entry");
        menu.clickMenuButton(player, com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu.BUTTON_MODE);
        h.assertTrue(pipe.side(Direction.WEST).mode() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.DISABLED && menu.mode() == pipe.side(Direction.WEST).mode(),
                "the mode button cycles Push to Off");
        h.assertTrue(h.getBlockState(new BlockPos(2, 1, 2)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.SIDES.get(Direction.WEST))
                == com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.NONE, "and the plug follows");
        menu.clickMenuButton(player, com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu.BUTTON_CLEAR);
        h.assertTrue(pipe.side(Direction.WEST).filter().isEmpty(), "Clear empties the filter");
        menu.clickMenuButton(player, com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu.BUTTON_MODE);
        menu.clickMenuButton(player, com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu.BUTTON_MODE);
        menu.clickMenuButton(player, com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu.BUTTON_MODE);
        h.assertTrue(pipe.side(Direction.WEST).mode() == PUSH, "round to Push again");
        // Without the wrench nothing changes: an empty hand, or an item, on the plug.
        Player bare = h.makeMockPlayer(GameType.SURVIVAL);
        h.getBlockState(new BlockPos(2, 1, 2)).useWithoutItem(h.getLevel(), bare, westPlug);
        bare.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        h.getBlockState(new BlockPos(2, 1, 2)).useItemOn(new ItemStack(Items.DIAMOND), h.getLevel(), bare, InteractionHand.MAIN_HAND, westPlug);
        h.assertTrue(pipe.side(Direction.WEST).mode() == PUSH && pipe.side(Direction.WEST).filter().isEmpty(), "hands and items no longer configure pipes");
        // Through the wrench item itself, as a real right-click does (it runs before the block is used).
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getMainHandItem().onItemUseFirst(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, westPlug));
        h.assertTrue(pipe.side(Direction.WEST).mode() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.DISABLED,
                "right-clicking with the wrench item cycles Push to Off, got " + pipe.side(Direction.WEST).mode());
        // A click on a side with nothing to plug into (the top of the pipe's middle) uses the nearest plugged side.
        var top = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 0.19, 0), Direction.UP, at, false);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var beforeTop = pipe.side(Direction.WEST).mode();
        com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.useWrench(h.getLevel(), at, player, InteractionHand.MAIN_HAND, top);
        h.assertTrue(pipe.side(Direction.UP).isDefault() && pipe.side(Direction.WEST).mode() == beforeTop.next(), "a click on the middle changes the plugged west side");
        h.succeed();
    }

    /** With transferRate = 1 an item network never moves more than one item a tick. */
    private static void itemPipeRate(GameTestHelper h) {
        int old = ModConfigs.SERVER.itemPipeTransferRate.get();
        ModConfigs.SERVER.itemPipeTransferRate.set(1);
        var shears = shearsAt(h, new BlockPos(1, 1, 2), Direction.WEST);
        shears.setItem(0, new ItemStack(Vanilla.wool(DyeColor.WHITE), 64));
        var chest = chest(h, new BlockPos(3, 1, 2));
        itemPipe(h, new BlockPos(2, 1, 2));
        int[] start = new int[1];
        h.runAfterDelay(2, () -> start[0] = countIn(chest, Vanilla.wool(DyeColor.WHITE)));
        h.runAfterDelay(12, () -> {
            try {
                int moved = countIn(chest, Vanilla.wool(DyeColor.WHITE)) - start[0];
                h.assertTrue(moved > 0 && moved <= 10, "10 ticks at 1 item a tick, moved " + moved);
            } finally {
                ModConfigs.SERVER.itemPipeTransferRate.set(old);
            }
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ themed sheep (appearance features)

    private static void themedSheep(GameTestHelper h) {
        var registry = SheepResourceRegistry.server();
        java.util.Map<String, List<Feature>> expected = new java.util.LinkedHashMap<>();
        expected.put("hay", List.of());
        expected.put("snow", List.of());
        expected.put("wither", List.of(Feature.EXTRA_HEADS));
        expected.put("dragon", List.of(Feature.WINGS));
        expected.put("glow_ink", List.of(Feature.GLOWING));
        expected.put("skeleton", List.of(Feature.BONES));
        expected.put("wither_skeleton", List.of(Feature.BONES));
        expected.put("spider", List.of(Feature.EXTRA_LEGS, Feature.GLOWING_EYES));
        expected.put("warden", List.of(Feature.SCULK));
        expected.put("blaze", List.of(Feature.ON_FIRE));
        expected.put("ghast", List.of(Feature.TENTACLES));
        expected.forEach((name, features) -> {
            SheepResource resource = registry.byId(ResourcefulSheep.id(name)).orElseThrow(() -> new AssertionError("missing themed sheep " + name));
            for (var feature : features) {
                h.assertTrue(resource.appearance().has(feature), name + " should have " + feature.getSerializedName() + ", has " + resource.appearance().features());
            }
        });
        h.assertTrue(registry.byId(ResourcefulSheep.id("hay")).orElseThrow().appearance().woolTexture().isPresent(), "hay sheep use the hay-bale fleece");
        h.assertTrue(registry.byFeedingItem(new ItemStack(Items.NETHER_STAR)).map(r -> r.id().getPath()).orElse("").equals("wither"), "nether stars make a Wither sheep");
        h.assertTrue(registry.byId(ResourcefulSheep.id("wither")).orElseThrow().separation().primary().create().is(Items.NETHER_STAR), "Wither wool separates into nether stars");
        // Resources without an appearance block (the original 14) are unchanged.
        h.assertTrue(registry.byId(IRON).orElseThrow().appearance().isEmpty(), "iron has no extra features");
        h.succeed();
    }

    private static void glowingSheepLight(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 1, 2);
        Sheep sheep = sheepAt(h, rel, ResourcefulSheep.id("glow_ink"));
        BlockPos at = h.absolutePos(rel);
        var glow = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.SHEEP_GLOW.get();
        h.runAfterDelay(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepFeatureEvents.GLOW_PLACE_TICKS * 2, () -> {
            h.assertTrue(h.getLevel().getBlockState(at).is(glow), "a glowing sheep lights the block it stands in, found " + h.getLevel().getBlockState(at));
            // The light itself is worked out on the light engine's own thread, which can fall behind on a busy test server.
            waitFor(h, 60, () -> h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at) == com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepGlowBlock.LIGHT_LEVEL,
                    () -> "the light is level 12, got " + h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at), () -> {
                // Sheared: no more glow, and the light goes out on its next check.
                sheep.setSheared(true);
                h.runAfterDelay(com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepGlowBlock.CHECK_TICKS + 2, () -> {
                    h.assertTrue(h.getLevel().getBlockState(at).isAir(), "the light goes out once the sheep is sheared, found " + h.getLevel().getBlockState(at));
                    h.succeed();
                });
            });
        });
    }

    /** Runs {@code then} as soon as {@code done} holds, checking every tick; fails with {@code why} after {@code ticks}. */
    private static void waitFor(GameTestHelper h, int ticks, java.util.function.BooleanSupplier done, java.util.function.Supplier<String> why, Runnable then) {
        if (done.getAsBoolean()) {
            then.run();
        } else if (ticks <= 0) {
            h.fail(why.get());
        } else {
            h.runAfterDelay(1, () -> waitFor(h, ticks - 1, done, why, then));
        }
    }

    private static void melonSliceSkin(GameTestHelper h) {
        var skinAttachment = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.SHEEP_SKIN;
        java.util.UUID id = SheepInfusionEvents.MELON_SKIN_PLAYER;
        // Anyone else's Melon Slice does nothing and isn't eaten.
        Sheep sheep = sheep(h);
        Player stranger = playerHolding(h, new ItemStack(Items.MELON_SLICE, 3));
        feed(stranger, sheep);
        h.assertTrue(!sheep.hasData(skinAttachment) || sheep.getData(skinAttachment).profile().isEmpty(), "only that player can give the skin");
        h.assertFalse(SheepInfusionEvents.isMelonSheep(sheep), "not a melon sheep yet");
        h.assertTrue(stranger.getMainHandItem().getCount() == 3, "another player's slice isn't eaten, left " + stranger.getMainHandItem().getCount());
        // That player's Melon Slice gives the sheep their skin and head, and is eaten.
        Player player = playerHolding(h, new ItemStack(Items.MELON_SLICE, 3));
        player.setUUID(id);
        feed(player, sheep);
        var skin = sheep.getData(skinAttachment).profile();
        h.assertTrue(skin.isPresent() && skin.get().partialProfile().id().equals(id), "the sheep wears " + id + ", got " + skin);
        h.assertTrue(player.getMainHandItem().getCount() == 2, "one slice is eaten, left " + player.getMainHandItem().getCount());
        h.assertTrue(SheepInfusionEvents.isMelonSheep(sheep), "it is now a melon sheep (melon fleece)");
        h.assertTrue(!SheepInfusionEvents.getInfusion(sheep).isInfused(), "a melon slice is only a look, not an infusion");
        // A sheep that already has the skin doesn't eat another.
        feed(player, sheep);
        h.assertTrue(player.getMainHandItem().getCount() == 2, "no second slice is eaten, left " + player.getMainHandItem().getCount());
        // Player Heads don't change the skin any more.
        Sheep other = sheepAt(h, new BlockPos(1, 1, 1), null);
        feed(playerHolding(h, new ItemStack(Items.PLAYER_HEAD)), other);
        h.assertTrue(!other.hasData(skinAttachment) || other.getData(skinAttachment).profile().isEmpty(), "a Player Head does nothing");
        // The skin is saved with the sheep.
        var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        sheep.saveWithoutId(saved);
        Sheep copy = Vanilla.SHEEP.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.LOAD);
        copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved.buildResult()));
        h.assertTrue(copy.getData(skinAttachment).profile().map(p -> p.partialProfile().id().equals(id)).orElse(false), "the skin survives saving and loading");
        h.succeed();
    }

    private static void themedAdvancements(GameTestHelper h) {
        var server = h.getLevel().getServer();
        for (String name : List.of("hay", "snow", "wither", "dragon", "glow_ink", "skeleton", "wither_skeleton", "spider", "warden", "blaze", "ghast")) {
            var adv = server.getAdvancements().get(ResourcefulSheep.id("contraptions/sheep/" + name));
            h.assertTrue(adv != null && adv.value().parent().map(ResourcefulSheep.id("contraptions/infuse_sheep")::equals).orElse(false),
                    "themed advancement " + name + " is loaded under Infuse a Sheep");
        }
        var every = server.getAdvancements().get(ResourcefulSheep.id("contraptions/every_wool"));
        h.assertTrue(every != null && every.value().criteria().size() == 25, "the every-wool challenge has one criterion per built-in wool");
        h.assertTrue(every.value().criteria().keySet().containsAll(SheepResourceRegistry.server().all().stream()
                .filter(r -> r.id().getNamespace().equals(ResourcefulSheep.MOD_ID)).map(r -> r.id().getPath()).toList()), "every built-in resource is in the challenge");

        var player = new net.minecraft.server.level.ServerPlayer(server, h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "adv-sheep-test"), net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override
            public void sendSystemMessage(net.minecraft.network.chat.Component message, boolean overlay) {}
        };
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 1, 3))));
        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.onInfused(player, SheepResourceRegistry.server().byId(ResourcefulSheep.id("wither")).orElseThrow());
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, "contraptions/infuse_sheep"), "infusing grants Wool You Look at That");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, "contraptions/sheep/wither"), "a Nether Star grants Three Heads Are Better Than One");
        h.assertFalse(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, "contraptions/sheep/dragon"), "but not the dragon one");
        // Each criterion is met by picking up that resource's wool (the trigger checks the item that changed), and only that.
        java.util.function.Function<String, ItemStack> wool = name -> {
            ItemStack stack = new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.RESOURCE_WOOL.get());
            stack.set(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.SHEEP_RESOURCE.get(), ResourcefulSheep.id(name));
            return stack;
        };
        for (var entry : every.value().criteria().entrySet()) {
            var criterion = entry.getValue();
            h.assertTrue(McTest.inventoryChangeMatches(criterion, player.getInventory(), wool.apply(entry.getKey())), entry.getKey() + " wool is recognised");
            String other = entry.getKey().equals("iron") ? "gold" : "iron";
            h.assertFalse(McTest.inventoryChangeMatches(criterion, player.getInventory(), wool.apply(other)), entry.getKey() + " isn't met by " + other + " wool");
        }
        h.succeed();
    }

    private static void ghastSheepScale(GameTestHelper h) {
        Sheep ghast = sheepAt(h, new BlockPos(2, 1, 2), ResourcefulSheep.id("ghast"));
        float plainWidth = Vanilla.SHEEP.getWidth();
        int wait = com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepFeatureEvents.GLOW_PLACE_TICKS * 2;
        h.runAfterDelay(wait, () -> {
            h.assertTrue(Math.abs(ghast.getScale() - 2.0F) < 1.0E-4, "a Ghast sheep is twice the size, scale " + ghast.getScale());
            h.assertTrue(Math.abs(ghast.getBbWidth() - plainWidth * 2) < 1.0E-3, "its hitbox doubles too, width " + ghast.getBbWidth());
            // Shearing doesn't shrink it; losing the infusion does.
            ghast.setSheared(true);
            SheepInfusionEvents.setInfusion(ghast, SheepInfusion.NONE);
            h.runAfterDelay(wait, () -> {
                h.assertTrue(Math.abs(ghast.getScale() - 1.0F) < 1.0E-4, "back to normal size once the infusion is gone, scale " + ghast.getScale());
                h.assertTrue(ghast.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE)
                        .getModifier(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepFeatureEvents.SCALE_MODIFIER) == null, "the modifier is removed");
                h.succeed();
            });
        });
    }

    private static void ghastSheepSounds(GameTestHelper h) {
        Sheep ghast = sheepAt(h, new BlockPos(1, 1, 1), ResourcefulSheep.id("ghast"));
        Sheep plain = sheepAt(h, new BlockPos(3, 1, 3), null);
        java.util.function.BiFunction<Sheep, net.minecraft.sounds.SoundEvent, Identifier> play = (sheep, sound) -> {
            var event = new net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition(h.getLevel(), sheep.position(),
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
            com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepFeatureEvents.onSound(event);
            return event.getSound().value().location();
        };
        h.assertTrue(play.apply(ghast, net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT).equals(net.minecraft.sounds.SoundEvents.GHAST_SCREAM.location()), "a Ghast sheep's bleat is the ghast's scream");
        h.assertTrue(play.apply(ghast, net.minecraft.sounds.SoundEvents.SHEEP_HURT).equals(net.minecraft.sounds.SoundEvents.GHAST_HURT.location()), "it hurts like a ghast");
        h.assertTrue(play.apply(ghast, net.minecraft.sounds.SoundEvents.SHEEP_DEATH).equals(net.minecraft.sounds.SoundEvents.GHAST_DEATH.location()), "it dies like a ghast");
        h.assertTrue(play.apply(plain, net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT).equals(net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT.location()), "a plain sheep still bleats");
        // Sheared, it keeps its voice.
        ghast.setSheared(true);
        h.assertTrue(play.apply(ghast, net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT).equals(net.minecraft.sounds.SoundEvents.GHAST_SCREAM.location()), "a sheared Ghast sheep still screams");
        // The "screaming" feature on its own gives the screaming goat's sounds; listed sounds win over it.
        var screaming = new com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance(Optional.empty(), List.of(Feature.SCREAMING), 0xFFFFFF, 1.0, Optional.empty(),
                com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Sounds.NONE);
        var replaced = com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepFeatureEvents.replacement(screaming, net.minecraft.sounds.SoundEvents.SHEEP_HURT);
        h.assertTrue(replaced.isPresent() && replaced.get().value() == net.minecraft.sounds.SoundEvents.GOAT_SCREAMING_HURT, "screaming = screaming goat, got " + replaced);
        h.succeed();
    }

    // ------------------------------------------------------------------ breeding

    private static Sheep sheepAt(GameTestHelper h, BlockPos pos, @org.jspecify.annotations.Nullable Identifier infusion) {
        Sheep sheep = h.spawn(Vanilla.SHEEP, pos);
        sheep.setNoAi(true);
        if (infusion != null) {
            int uses = SheepResourceRegistry.server().byId(infusion).orElseThrow().effectiveInfusionUses();
            SheepInfusionEvents.setInfusion(sheep, SheepInfusion.NONE.withResource(infusion, uses).afterShearing(0, 0));
        }
        return sheep;
    }

    /** Breeds two sheep the vanilla way and returns the lamb. */
    private static Sheep breed(GameTestHelper h, Sheep a, Sheep b) {
        var before = new java.util.HashSet<>(h.getLevel().getEntitiesOfClass(Sheep.class, a.getBoundingBox().inflate(4), Sheep::isBaby));
        a.spawnChildFromBreeding(h.getLevel(), b);
        return h.getLevel().getEntitiesOfClass(Sheep.class, a.getBoundingBox().inflate(4), Sheep::isBaby).stream()
                .filter(lamb -> !before.contains(lamb)).findFirst().orElseThrow(() -> new AssertionError("no lamb was born"));
    }

    /**
     * Breeding two Iron sheep gives an Iron lamb with a full set of shearings (the parents had used one each), one
     * Iron parent and a plain one also give an Iron lamb, and two plain sheep give a plain lamb.
     */
    private static void breedingKeepsInfusion(GameTestHelper h) {
        Sheep lamb = breed(h, sheepAt(h, new BlockPos(1, 1, 1), IRON), sheepAt(h, new BlockPos(1, 1, 2), IRON));
        // A full set for this lamb: the resource's uses, raised by the Endurance it was born with.
        int full = SheepInfusionEvents.infusionUses(lamb, SheepResourceRegistry.server().byId(IRON).orElseThrow());
        SheepInfusion infusion = SheepInfusionEvents.getInfusion(lamb);
        h.assertTrue(infusion.resource().map(IRON::equals).orElse(false), "two Iron parents make an Iron lamb, got " + infusion);
        h.assertTrue(infusion.usesRemaining() == full, "the lamb gets a full " + full + " shearings, got " + infusion.usesRemaining());
        Sheep one = breed(h, sheepAt(h, new BlockPos(4, 1, 1), IRON), sheepAt(h, new BlockPos(4, 1, 2), null));
        h.assertTrue(SheepInfusionEvents.getInfusion(one).resource().map(IRON::equals).orElse(false), "one Iron parent is enough by default");
        Sheep plain = breed(h, sheepAt(h, new BlockPos(1, 1, 4), null), sheepAt(h, new BlockPos(1, 1, 5), null));
        h.assertFalse(SheepInfusionEvents.getInfusion(plain).isInfused(), "two plain sheep make a plain lamb");
        h.succeed();
    }

    /**
     * Iron and Gold parents pass on either one at random (both turn up over 40 lambs). With breedingNeedsBothParents
     * one infused parent isn't enough; with breedingKeepsInfusion off nothing is passed on.
     */
    private static void breedingRules(GameTestHelper h) {
        Identifier gold = ResourcefulSheep.id("gold");
        Sheep iron = sheepAt(h, new BlockPos(1, 1, 1), IRON), golden = sheepAt(h, new BlockPos(1, 1, 2), gold), plain = sheepAt(h, new BlockPos(1, 1, 3), null);
        var random = net.minecraft.util.RandomSource.create(42);
        java.util.Set<Identifier> seen = new java.util.HashSet<>();
        for (int i = 0; i < 40; i++) {
            SheepInfusionEvents.inheritedResource(iron, golden, random).ifPresent(r -> seen.add(r.id()));
        }
        h.assertTrue(seen.equals(java.util.Set.of(IRON, gold)), "mixed parents pass on either resource, saw " + seen);
        boolean oldBoth = ModConfigs.SERVER.breedingNeedsBothParents.get(), oldKeep = ModConfigs.SERVER.breedingKeepsInfusion.get();
        try {
            ModConfigs.SERVER.breedingNeedsBothParents.set(true);
            h.assertTrue(SheepInfusionEvents.inheritedResource(iron, plain, random).isEmpty(), "needs both parents: one isn't enough");
            h.assertTrue(SheepInfusionEvents.inheritedResource(iron, golden, random).isPresent(), "needs both parents: two are");
            ModConfigs.SERVER.breedingNeedsBothParents.set(false);
            ModConfigs.SERVER.breedingKeepsInfusion.set(false);
            h.assertTrue(SheepInfusionEvents.inheritedResource(iron, iron, random).isEmpty(), "turned off, nothing is passed on");
        } finally {
            ModConfigs.SERVER.breedingNeedsBothParents.set(oldBoth);
            ModConfigs.SERVER.breedingKeepsInfusion.set(oldKeep);
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ sheep stats

    /** Runs {@code body} with the stat chances set, putting them back after. */
    private static void withStatChances(double up, double down, Runnable body) {
        double oldUp = ModConfigs.SERVER.statsIncreaseChance.get(), oldDown = ModConfigs.SERVER.statsDecreaseChance.get();
        try {
            ModConfigs.SERVER.statsIncreaseChance.set(up);
            ModConfigs.SERVER.statsDecreaseChance.set(down);
            body.run();
        } finally {
            ModConfigs.SERVER.statsIncreaseChance.set(oldUp);
            ModConfigs.SERVER.statsDecreaseChance.set(oldDown);
        }
    }

    /**
     * Lambs average their parents' stats with a chance of one more: wild sheep give 1s, or 2s when the chance comes
     * up; 4 and 7 average to 5 or 6; nothing passes the cap; the stats are saved on the lamb from real breeding.
     */
    private static void sheepStatsBred(GameTestHelper h) {
        var random = net.minecraft.util.RandomSource.create(7);
        com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats wild = com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.WILD;
        var mid = new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(4, 4, 4);
        var high = new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(7, 7, 7);
        var top = new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(10, 10, 10);
        withStatChances(0, 0, () -> {
            var child = com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.bred(wild, wild, random);
            h.assertTrue(child.equals(wild), "two wild sheep with no luck make a wild lamb, got " + child);
            for (int i = 0; i < 20; i++) {
                var c = com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.bred(mid, high, random);
                h.assertTrue(c.yield() >= 5 && c.yield() <= 6, "4 and 7 average to 5 or 6, got " + c);
            }
        });
        withStatChances(1, 0, () -> {
            h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.bred(wild, wild, random).equals(new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(2, 2, 2)),
                    "with the chance always coming up, wild parents give 2s");
            h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.bred(top, top, random).equals(top), "never past the cap of 10");
            // Real breeding: two 7s make 8s, saved on the lamb.
            Sheep a = sheepAt(h, new BlockPos(1, 1, 1), null), b = sheepAt(h, new BlockPos(1, 1, 2), null);
            SheepInfusionEvents.setStats(a, high);
            SheepInfusionEvents.setStats(b, high);
            Sheep lamb = breed(h, a, b);
            var stats = SheepInfusionEvents.getStats(lamb);
            h.assertTrue(stats.equals(new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(8, 8, 8)), "two 7s bred make an 8 lamb, got " + stats);
            h.assertTrue(SheepInfusionEvents.describe(lamb).getString().contains("Yield 8"), "inspecting it shows its stats: " + SheepInfusionEvents.describe(lamb).getString());
        });
        withStatChances(0, 1, () -> h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.bred(wild, wild, random).equals(wild), "never below 1"));
        h.succeed();
    }

    /**
     * Yield 10 shears 2.35 times the wool (infused and plain); Endurance 10 makes an infusion last 1.9 times as long;
     * Growth 10 regrows wool 9% of seconds and Growth 1 never on its own.
     */
    private static void sheepStatsEffects(GameTestHelper h) {
        var best = new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(10, 10, 10);
        // Yield on Iron Wool (1 a shearing): 2.35, so 2 or 3.
        Sheep iron = sheepAt(h, new BlockPos(1, 1, 1), IRON);
        SheepInfusionEvents.setStats(iron, best);
        int ironWool = count(shear(h, iron), st -> IRON.equals(ResourceWoolItem.getResourceId(st)));
        h.assertTrue(ironWool == 2 || ironWool == 3, "Yield 10 shears 2-3 Iron Wool, got " + ironWool);
        // Yield on plain wool: vanilla's 1-3 becomes at least 2.
        Sheep plain = sheepAt(h, new BlockPos(3, 1, 1), null);
        SheepInfusionEvents.setStats(plain, best);
        int white = count(shear(h, plain), st -> st.is(Vanilla.wool(DyeColor.WHITE)));
        h.assertTrue(white >= 2 && white <= 8, "Yield 10 shears more plain wool, got " + white);
        // Endurance: feeding Iron to an Endurance-10 sheep.
        Sheep tough = sheepAt(h, new BlockPos(5, 1, 1), null);
        SheepInfusionEvents.setStats(tough, best);
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), tough);
        int base = SheepResourceRegistry.server().byId(IRON).orElseThrow().effectiveInfusionUses();
        int uses = SheepInfusionEvents.getInfusion(tough).usesRemaining();
        h.assertTrue(uses == Math.round(base * 1.9), "Endurance 10 makes " + base + " shearings last " + Math.round(base * 1.9) + ", got " + uses);
        // Growth.
        h.assertTrue(Math.abs(best.regrowChancePerSecond() - 0.09) < 1e-9, "Growth 10: 9% a second, got " + best.regrowChancePerSecond());
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats.WILD.regrowChancePerSecond() == 0, "Growth 1 never regrows on its own");
        Sheep shorn = sheepAt(h, new BlockPos(1, 1, 4), null);
        shorn.setSheared(true);
        h.runAfterDelay(100, () -> {
            h.assertTrue(shorn.isSheared(), "a wild sheep with no grass stays sheared");
            // With the chance at 100%, a Growth-2 sheep regrows within a second.
            double old = ModConfigs.SERVER.statsGrowthPerPoint.get();
            ModConfigs.SERVER.statsGrowthPerPoint.set(1.0);
            SheepInfusionEvents.setStats(shorn, new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(1, 2, 1));
            h.runAfterDelay(25, () -> {
                ModConfigs.SERVER.statsGrowthPerPoint.set(old);
                h.assertFalse(shorn.isSheared(), "Growth regrows the wool without grass");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ Infusion Altar

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity altar(GameTestHelper h, BlockPos pos, boolean powered, ItemStack... items) {
        h.setBlock(pos, ModBlocks.INFUSION_ALTAR.get());
        var be = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity.class);
        for (int i = 0; i < items.length; i++) be.setItem(i, items[i]);
        if (powered) {
            var energy = (net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler) be.getEnergyHandler();
            energy.set(energy.getCapacityAsInt());
        }
        return be;
    }

    /**
     * A Nether Star can't be fed by hand any more (tier 2); at a powered altar with its catalysts the sheep beside it
     * becomes a Wither sheep, and the star, the catalysts and exactly 200,000 FE are used.
     */
    private static void altarWither(GameTestHelper h) {
        Identifier wither = ResourcefulSheep.id("wither");
        Sheep byHand = sheepAt(h, new BlockPos(6, 1, 6), null);
        Player player = playerHolding(h, new ItemStack(Items.NETHER_STAR));
        feed(player, byHand);
        h.assertFalse(SheepInfusionEvents.getInfusion(byHand).isInfused(), "a Nether Star can't be fed by hand");
        h.assertTrue(player.getMainHandItem().is(Items.NETHER_STAR), "and the star isn't used");

        BlockPos pos = new BlockPos(2, 1, 2);
        var be = altar(h, pos, true, new ItemStack(Items.NETHER_STAR, 2), new ItemStack(Items.SOUL_SAND, 5),
                new ItemStack(Items.BONE_BLOCK, 2), new ItemStack(Items.WITHER_ROSE, 1));
        Sheep sheep = sheepAt(h, new BlockPos(3, 1, 2), null);
        Player watcher = playerHolding(h, ItemStack.EMPTY);
        int start = be.getEnergyHandler().getAmountAsInt();
        h.runAfterDelay(20, () -> {
            h.assertTrue(be.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AltarStatus.INFUSING, "it infuses, was " + be.status());
            var slow = sheep.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
            h.assertTrue(slow != null && slow.getAmplifier() == 4, "the sheep is slowed while it's infused, has " + slow);
            h.assertTrue(watcher.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) == null, "only the sheep is slowed");
        });
        h.succeedWhen(() -> {
            h.assertTrue(SheepInfusionEvents.getInfusion(sheep).resource().map(wither::equals).orElse(false), "the sheep beside it becomes a Wither sheep");
            h.assertTrue(sheep.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) == null, "the Slowness is taken off when it's done");
            h.assertTrue(be.getItem(0).getCount() == 1, "one Nether Star used, left " + be.getItem(0));
            h.assertTrue(be.getItem(1).getCount() == 1 && be.getItem(2).isEmpty() && be.getItem(3).isEmpty(), "4 soul sand, 2 bone blocks and the rose used");
            h.assertTrue(start - be.getEnergyHandler().getAmountAsInt() == 200_000, "200,000 FE used, used " + (start - be.getEnergyHandler().getAmountAsInt()));
        });
    }

    /**
     * The Sheep Treadmill is two blocks long: placed facing the way the player
     * looks, it fills the block in front, won't place when that's taken, and breaking either half removes both.
     */
    private static void sheepTreadmill(GameTestHelper h) {
        var block = ModBlocks.SHEEP_TREADMILL.get();
        var part = com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.PART;
        var facing = com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.FACING;
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.STONE);
        java.util.function.BiConsumer<Player, BlockPos> use = (player, rel) -> {
            BlockPos floor = h.absolutePos(rel);
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor), net.minecraft.core.Direction.UP, floor, false)));
        };
        Player player = playerHolding(h, new ItemStack(ModItems.SHEEP_TREADMILL.get()));
        Direction looking = player.getDirection();
        use.accept(player, new BlockPos(3, 0, 3));
        BlockPos back = new BlockPos(3, 1, 3), front = back.relative(looking);
        h.assertTrue(h.getBlockState(back).is(block) && h.getBlockState(back).getValue(part) == 0 && h.getBlockState(back).getValue(facing) == looking,
                "the back is where it was placed, facing " + looking + ", was " + h.getBlockState(back));
        h.assertTrue(h.getBlockState(front).is(block) && h.getBlockState(front).getValue(part) == 1, "the front is the next block that way, was " + h.getBlockState(front));
        // Blocked: something in the way in front.
        BlockPos back2 = new BlockPos(1, 1, 1);
        h.setBlock(back2.relative(looking), Blocks.STONE);
        Player other = playerHolding(h, new ItemStack(ModItems.SHEEP_TREADMILL.get()));
        use.accept(other, new BlockPos(1, 0, 1));
        h.assertFalse(h.getBlockState(back2).is(block), "it won't place when the block in front is taken");
        h.getLevel().destroyBlock(h.absolutePos(front), true);
        h.runAfterDelay(2, () -> {
            h.assertTrue(h.getBlockState(back).isAir(), "breaking the front removes the back too, was " + h.getBlockState(back));
            // One treadmill item for the pair: only the back's loot table drops it.
            int dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(back)).inflate(3),
                    e -> e.getItem().is(ModItems.SHEEP_TREADMILL.get())).stream().mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(dropped == 1, "breaking it drops one treadmill, got " + dropped);
            h.succeed();
        });
    }

    /**
     * The work-in-progress Hume Field Generator (development builds only) takes a 2x2 area three blocks high: placing it
     * fills the eleven blocks east, south and above it, it won't place where one is taken, and breaking any removes all.
     */
    private static void humeField(GameTestHelper h) {
        var block = ModBlocks.HUME_FIELD_GENERATOR.get();
        var part = com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock.PART;
        // A solid floor to click on (the empty structure's floor would be replaced instead).
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 3))) h.setBlock(p, Blocks.STONE);
        Player player = playerHolding(h, new ItemStack(ModItems.HUME_FIELD_GENERATOR.get()));
        BlockPos floor = h.absolutePos(new BlockPos(1, 0, 1));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor), net.minecraft.core.Direction.UP, floor, false);
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        var layer = com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock.LAYER;
        for (int p = 0; p < 4; p++) {
            for (int l = 0; l < 3; l++) {
                BlockPos at = new BlockPos(1 + (p & 1), 1 + l, 1 + (p >> 1));
                h.assertTrue(h.getBlockState(at).is(block) && h.getBlockState(at).getValue(part) == p && h.getBlockState(at).getValue(layer) == l,
                        "corner " + p + " layer " + l + " at " + at + ", was " + h.getBlockState(at));
            }
        }
        // Blocked: a stone where a corner would go.
        h.setBlock(new BlockPos(5, 3, 2), Blocks.STONE); // in its top layer
        Player other = playerHolding(h, new ItemStack(ModItems.HUME_FIELD_GENERATOR.get()));
        BlockPos floor2 = h.absolutePos(new BlockPos(4, 0, 1));
        other.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(other, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor2), net.minecraft.core.Direction.UP, floor2, false)));
        h.assertFalse(h.getBlockState(new BlockPos(4, 1, 1)).is(block), "it won't place where a corner is taken");
        // Breaking the middle of the south-east column takes the rest with it.
        h.getLevel().destroyBlock(h.absolutePos(new BlockPos(2, 2, 2)), false);
        h.runAfterDelay(2, () -> {
            for (int p = 0; p < 4; p++) {
                for (int l = 0; l < 3; l++) {
                    BlockPos at = new BlockPos(1 + (p & 1), 1 + l, 1 + (p >> 1));
                    h.assertTrue(h.getBlockState(at).isAir(), "corner " + p + " layer " + l + " is gone too, was " + h.getBlockState(at));
                }
            }
            h.succeed();
        });
    }

    /**
     * The work-in-progress Echo Chamber fills the 3x3x3 space behind and above where it's placed, facing the player;
     * breaking any block of it removes the lot, and it won't place where any of that space is taken.
     */
    private static void echoChamber(GameTestHelper h) {
        var block = ModBlocks.ECHO_CHAMBER.get();
        var part = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.PART;
        var layer = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.LAYER;
        var facingProperty = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING;
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.STONE);
        // The middle of the platform, so it fits whichever way the mock player faces.
        BlockPos floor = h.absolutePos(new BlockPos(3, 0, 3));
        Runnable place = () -> {
            Player player = playerHolding(h, new ItemStack(ModItems.ECHO_CHAMBER.get()));
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor), Direction.UP, floor, false)));
        };
        place.run();
        BlockPos origin = floor.above();
        net.minecraft.world.level.block.state.BlockState placed = h.getLevel().getBlockState(origin);
        h.assertTrue(placed.is(block), "the front middle block is where it was placed, was " + placed);
        Direction facing = placed.getValue(facingProperty);
        java.util.List<BlockPos> all = new java.util.ArrayList<>();
        for (int p = 0; p < 9; p++) {
            for (int l = 0; l < 3; l++) {
                BlockPos at = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.at(origin, facing, p, l);
                net.minecraft.world.level.block.state.BlockState state = h.getLevel().getBlockState(at);
                h.assertTrue(state.is(block) && state.getValue(part) == p && state.getValue(layer) == l && state.getValue(facingProperty) == facing,
                        "column " + p + " layer " + l + " at " + at + ", was " + state);
                all.add(at);
            }
        }
        // (Test platforms can be rotated, so this checks world positions: its bottom layer stands on the stone floor.)
        h.assertTrue(all.stream().distinct().count() == 27 && all.stream().filter(at -> at.getY() == origin.getY())
                .allMatch(at -> h.getLevel().getBlockState(at.below()).is(Blocks.STONE)), "27 different blocks, standing on the floor");
        // Breaking the top back corner takes the rest with it.
        h.getLevel().destroyBlock(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.at(origin, facing, 8, 2), false);
        h.runAfterDelay(2, () -> {
            for (BlockPos at : all) h.assertTrue(h.getLevel().getBlockState(at).isAir(), at + " is gone too, was " + h.getLevel().getBlockState(at));
            // Blocked: a stone where its top back corner would go.
            h.getLevel().setBlockAndUpdate(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.at(origin, facing, 8, 2), Blocks.STONE.defaultBlockState());
            place.run();
            h.assertFalse(h.getLevel().getBlockState(origin).is(block), "it won't place where any of its space is taken");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Echo Chamber

    /**
     * Builds an Echo Chamber facing north in the middle of the big empty structure, on a stone floor, and returns its
     * block entity. (Test structures can be turned, so this works in world positions: the pen runs north of the dish.)
     */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity buildEchoChamber(GameTestHelper h) {
        BlockPos front = h.absolutePos(new BlockPos(6, 1, 6));
        for (BlockPos p : BlockPos.betweenClosed(front.offset(-5, -1, -5), front.offset(5, -1, 4))) h.getLevel().setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.fill(h.getLevel(), front, ModBlocks.ECHO_CHAMBER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING, Direction.NORTH));
        return (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity) h.getLevel().getBlockEntity(front.south());
    }

    /** A sheep standing still at world position {@code at}, infused with {@code resource} (spawned in world positions too). */
    private static Sheep echoSheep(GameTestHelper h, BlockPos at, String resource) {
        Sheep sheep = Vanilla.SHEEP.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        sheep.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        sheep.setNoAi(true);
        Identifier id = ResourcefulSheep.id(resource);
        int uses = SheepResourceRegistry.server().byId(id).orElseThrow().effectiveInfusionUses();
        SheepInfusionEvents.setInfusion(sheep, SheepInfusion.NONE.withResource(id, uses).afterShearing(0, 0));
        h.getLevel().addFreshEntity(sheep);
        return sheep;
    }

    /** It counts the grown, fleeced Warden sheep in the pen in front of its dish, and no others. */
    private static void echoCountsHerd(GameTestHelper h) {
        var be = buildEchoChamber(h);
        BlockPos front = be.getBlockPos().north();
        echoSheep(h, front.north(2), "warden");
        echoSheep(h, front.north(3).west(), "warden");
        echoSheep(h, front.north(2).east(2), "iron");                // not a Warden sheep
        echoSheep(h, front.north(4), "warden").setSheared(true);     // no fleece
        echoSheep(h, front.north(3).west(4), "warden");              // outside the pen
        echoSheep(h, front.north(4).east(), "warden").setAge(-24000); // a lamb
        be.scan(h.getLevel(), be.getBlockPos(), Direction.NORTH);
        h.assertTrue(be.herd() == 2, "two Warden sheep count, got " + be.herd());
        h.succeed();
    }

    /** The output follows the herd, the noise level and how steady it is; a comparator reads the noise level. */
    private static void echoPower(GameTestHelper h) {
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.power(1, 7, 1.0) == 8_000, "one sheep, steady, in the sweet spot: 8,000");
        int[] herd = {8_000, 14_000, 18_000, 20_000};
        for (int n = 1; n <= 4; n++) {
            int got = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.power(n, 7, 1.0);
            h.assertTrue(got == herd[n - 1], n + " sheep make " + herd[n - 1] + ", got " + got);
        }
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.power(5, 7, 1.0) == 20_000, "a fifth sheep adds nothing");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.power(4, 0, 1.0) == 0, "silence makes nothing");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.power(4, 3, 1.0) == 12_000, "too quiet (3 of 5): three fifths");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.power(4, 12, 1.0) == 12_000, "too loud (2 over): two fifths less");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.power(4, 15, 1.0) == 0, "far too loud makes nothing");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.power(4, 7, 0.0) == 10_000, "an erratic noise gets half");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.steadiness(new int[]{2, 2, 2, 2, 2}) == 1.0, "the same every fifth is perfectly steady");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.steadiness(new int[]{0, 0, 10, 0, 0}) == 0.0, "one burst is not steady at all");
        // In the world: two Warden sheep hearing a steady level 10 (four vibrations every two seconds).
        var be = buildEchoChamber(h);
        BlockPos front = be.getBlockPos().north();
        echoSheep(h, front.north(2), "warden");
        echoSheep(h, front.north(3), "warden");
        be.hearSteadily(4);
        h.runAfterDelay(2, () -> {
            h.assertTrue(be.noise() == 10 && be.herd() == 2, "noise 10 and two sheep, got " + be.noise() + " and " + be.herd());
            h.assertTrue(be.generating() == 14_000 && be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.State.LISTENING,
                    "listening at 14,000 FE/t, got " + be.state() + " " + be.generating());
            var state = h.getLevel().getBlockState(front);
            h.assertTrue(state.getAnalogOutputSignal(h.getLevel(), front, Direction.NORTH) == 10, "a comparator reads the noise level, 10");
            h.succeed();
        });
    }

    /** It hears vibrations near its dish, but not its own Warden sheep moving, and not through a wall of wool. */
    private static void echoHearing(GameTestHelper h) {
        var be = buildEchoChamber(h);
        BlockPos front = be.getBlockPos().north();
        net.minecraft.server.level.ServerLevel level = h.getLevel();
        Sheep listener = echoSheep(h, front.north(2), "warden");
        be.scan(level, be.getBlockPos(), Direction.NORTH);
        int before = be.hearingNow();
        BlockPos noteAt = front.north(4).above();
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.NOTE_BLOCK_PLAY, noteAt, net.minecraft.world.level.gameevent.GameEvent.Context.of(Blocks.NOTE_BLOCK.defaultBlockState()));
        h.assertTrue(be.hearingNow() == before + 1, "a note block 4 blocks out is heard");
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.STEP, listener.blockPosition(), net.minecraft.world.level.gameevent.GameEvent.Context.of(listener));
        h.assertTrue(be.hearingNow() == before + 1, "its own Warden sheep's steps aren't counted");
        Sheep stranger = echoSheep(h, front.north(3).west(2), "iron");
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.STEP, stranger.blockPosition(), net.minecraft.world.level.gameevent.GameEvent.Context.of(stranger));
        h.assertTrue(be.hearingNow() == before + 2, "another sheep's steps are");
        // A note block shut in a box of wool can't be heard.
        BlockPos boxed = front.north(4).east(3).above();
        var wool = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("white_wool")).defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(boxed.offset(-1, -1, -1), boxed.offset(1, 1, 1))) {
            if (!p.equals(boxed)) level.setBlockAndUpdate(p, wool);
        }
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.NOTE_BLOCK_PLAY, boxed, net.minecraft.world.level.gameevent.GameEvent.Context.of(Blocks.NOTE_BLOCK.defaultBlockState()));
        h.assertTrue(be.hearingNow() == before + 2, "nothing gets through the wool");
        h.succeed();
    }

    /**
     * Too quiet for long enough and its Warden sheep fall asleep and it stops; noise back in the sweet spot wakes them,
     * they take a moment to settle, and then it makes power again.
     */
    private static void echoSleep(GameTestHelper h) {
        var be = buildEchoChamber(h);
        echoSheep(h, be.getBlockPos().north(3), "warden");
        int sleepTicks = ModConfigs.getInt(ModConfigs.SERVER.echoSleepTicks), wakeTicks = ModConfigs.getInt(ModConfigs.SERVER.echoWakeTicks);
        be.hearSteadily(0);
        be.setMeters(sleepTicks - 3, 0); // nearly asleep already
        h.runAfterDelay(5, () -> {
            h.assertTrue(be.asleep() && be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.State.ASLEEP
                    && be.generating() == 0, "silence sends its sheep to sleep, got " + be.state());
            be.hearSteadily(3); // noise level 7: back in the sweet spot
            h.runAfterDelay(2, () -> {
                h.assertTrue(!be.asleep() && be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.State.WAKING
                        && be.generating() == 0, "the noise wakes them, and they settle first, got " + be.state());
                h.runAfterDelay(wakeTicks, () -> {
                    be.hearSteadily(3);
                    h.runAfterDelay(1, () -> {
                        h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.State.LISTENING
                                && be.generating() > 0, "then it listens again, got " + be.state() + " at " + be.generating() + " FE/t");
                        h.succeed();
                    });
                });
            });
        });
    }

    /**
     * Too loud for long enough and the room shrieks: it stops for its cooldown, and its shriek meter empties. (The
     * Darkness it gives nearby players is vanilla's own Warden helper; the screenshot harness shows it on a real player.)
     */
    private static void echoShriek(GameTestHelper h) {
        var be = buildEchoChamber(h);
        echoSheep(h, be.getBlockPos().north(3), "warden");
        int shriekTicks = ModConfigs.getInt(ModConfigs.SERVER.echoShriekTicks), cooldown = ModConfigs.getInt(ModConfigs.SERVER.echoShriekCooldown);
        be.hearSteadily(7); // noise level 15: far too loud
        be.setMeters(0, shriekTicks - 3);
        h.runAfterDelay(5, () -> {
            h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.State.SHRIEKED
                    && be.generating() == 0, "it shrieks and stops, got " + be.state());
            h.assertTrue(be.cooldown() > cooldown - 10 && be.shriekMeter() == 0, "for its cooldown, with the meter emptied, got " + be.cooldown() + " / " + be.shriekMeter());
            be.hearSteadily(3); // calm again: it still waits out the cooldown
            h.runAfterDelay(20, () -> {
                h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity.State.SHRIEKED && be.generating() == 0,
                        "it stays stopped until the cooldown is over, got " + be.state());
                h.succeed();
            });
        });
    }

    /**
     * Its recipe (Iron Blocks and an Echo Shard; Echo Shards round a Warden Wool Bale; Blackstone round a Functional
     * Machine Block) makes an Echo Chamber (another resource's bale won't do); breaking any block of a placed one drops one Echo Chamber, not one for each of its 27 blocks, and a
     * player needs a pickaxe for it.
     */
    private static void echoRecipeAndDrop(GameTestHelper h) {
        ItemStack shard = new ItemStack(Items.ECHO_SHARD), e = ItemStack.EMPTY;
        ItemStack machine = new ItemStack(ModItems.FUNCTIONAL_MACHINE_BLOCK.get());
        ItemStack warden = com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(ResourcefulSheep.id("warden"), 1);
        ItemStack iron = com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(ResourcefulSheep.id("iron"), 1);
        ItemStack ironBlock = new ItemStack(Items.IRON_BLOCK), blackstone = new ItemStack(Items.BLACKSTONE);
        ItemStack made = craft(h, 3, 3, List.of(ironBlock, shard, ironBlock, shard, warden, shard, blackstone, machine, blackstone));
        h.assertTrue(made.is(ModItems.ECHO_CHAMBER.get()), "the recipe makes an Echo Chamber, got " + made);
        h.assertTrue(craft(h, 3, 3, List.of(ironBlock, shard, ironBlock, shard, iron, shard, blackstone, machine, blackstone)).isEmpty(), "an Iron Wool Bale won't do");
        // Placed on the platform, then broken from its top corner.
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.STONE);
        BlockPos front = h.absolutePos(new BlockPos(3, 1, 1));
        com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.fill(h.getLevel(), front, ModBlocks.ECHO_CHAMBER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING, Direction.NORTH));
        BlockPos corner = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.at(front, Direction.NORTH, 8, 2);
        h.getLevel().destroyBlock(corner, true);
        h.runAfterDelay(3, () -> {
            var area = new net.minecraft.world.phys.AABB(front).inflate(6);
            int dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                    i -> i.getItem().is(ModItems.ECHO_CHAMBER.get())).stream().mapToInt(i -> i.getItem().getCount()).sum();
            h.assertTrue(dropped == 1, "one Echo Chamber drops, got " + dropped);
            h.assertTrue(h.getLevel().getBlockState(front).isAir(), "and the whole machine is gone");
            for (var item : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)) item.discard();
            // A player in survival mining its front corner with a pickaxe gets one too; with a bare hand, none.
            var state = ModBlocks.ECHO_CHAMBER.get().defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING, Direction.NORTH);
            var miner = McTest.mockServerPlayer(h, GameType.SURVIVAL);
            com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.fill(h.getLevel(), front, state);
            miner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            h.assertTrue(miner.gameMode.destroyBlock(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.at(front, Direction.NORTH, 0, 0)), "it breaks");
            com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.fill(h.getLevel(), front, state);
            miner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.assertTrue(miner.gameMode.destroyBlock(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.at(front, Direction.NORTH, 2, 1)), "it breaks by hand too");
            h.runAfterDelay(3, () -> {
                int mined = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                        i -> i.getItem().is(ModItems.ECHO_CHAMBER.get())).stream().mapToInt(i -> i.getItem().getCount()).sum();
                h.assertTrue(mined == 1, "one from the pickaxe and none by hand, got " + mined);
                h.assertTrue(h.getLevel().getBlockState(front.south()).isAir(), "and it's all gone");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ Static Generator

    /** A Static Generator in the middle of the platform (world positions, as test platforms can be turned). */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity buildStaticGenerator(GameTestHelper h) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.GRASS_BLOCK);
        BlockPos at = h.absolutePos(new BlockPos(3, 1, 3));
        var block = ModBlocks.STATIC_GENERATOR.get();
        var half = com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock.HALF;
        h.getLevel().setBlockAndUpdate(at, block.defaultBlockState());
        h.getLevel().setBlockAndUpdate(at.above(), block.defaultBlockState().setValue(half, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        return (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity) h.getLevel().getBlockEntity(at);
    }

    /** It counts grown sheep with their fleece on next to a comb in its pen, at their resource's charge, and no others. */
    private static void staticCountsSheep(GameTestHelper h) {
        var be = buildStaticGenerator(h);
        BlockPos at = be.getBlockPos(), comb = at.east(2);
        var level = h.getLevel();
        be.scan(level, at);
        h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.State.NEEDS_COMBS
                || be.combs().isEmpty(), "no combs yet");
        level.setBlockAndUpdate(comb, ModBlocks.COPPER_COMB.get().defaultBlockState());
        echoSheep(h, comb.south(), "iron");            // next to the comb: 100
        echoSheep(h, comb.north(), "copper");          // next to the comb: 150
        echoSheep(h, comb.west(), "iron").setSheared(true); // no fleece
        echoSheep(h, at.west(2).north(2), "iron");     // in the pen but nowhere near a comb
        be.scan(level, at);
        h.assertTrue(be.combs().size() == 1 && be.herd() == 2, "one comb and two sheep at it, got " + be.combs().size() + " and " + be.herd());
        h.assertTrue(be.generating() == 250, "Copper 150 and plain 100 make 250, got " + be.generating());
        h.succeed();
    }

    /** Comb stages give 100/75/50/25%, waxed 85% of that; an axe scrapes and Honeycomb waxes; worked combs wear, waxed ones don't. */
    private static void staticCombs(GameTestHelper h) {
        var comb = ModBlocks.COPPER_COMB.get().defaultBlockState();
        var age = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CopperCombBlock.AGE;
        var waxed = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CopperCombBlock.WAXED;
        double[] expected = {1.0, 0.75, 0.5, 0.25};
        for (int a = 0; a < 4; a++) {
            h.assertTrue(Math.abs(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.efficiency(comb.setValue(age, a)) - expected[a]) < 1e-9,
                    "stage " + a + " gives " + expected[a]);
        }
        h.assertTrue(Math.abs(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.efficiency(comb.setValue(waxed, true)) - 0.85) < 1e-9, "waxed fresh gives 85%");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CopperCombBlock.aged(comb.setValue(age, 1)).getValue(age) == 2, "it ages a stage");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CopperCombBlock.aged(comb.setValue(waxed, true)).getValue(age) == 0, "but not when waxed");
        // Honeycomb waxes it; an axe takes the wax off, then scrapes a stage back.
        var be = buildStaticGenerator(h);
        BlockPos at = be.getBlockPos().east(2);
        var level = h.getLevel();
        level.setBlockAndUpdate(at, comb.setValue(age, 2));
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        java.util.function.Consumer<ItemStack> use = stack -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            level.getBlockState(at).useItemOn(stack, level, player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.UP, at, false));
        };
        use.accept(new ItemStack(Items.HONEYCOMB));
        h.assertTrue(level.getBlockState(at).getValue(waxed) && level.getBlockState(at).getValue(age) == 2, "Honeycomb waxes it");
        use.accept(new ItemStack(Items.IRON_AXE));
        h.assertTrue(!level.getBlockState(at).getValue(waxed) && level.getBlockState(at).getValue(age) == 2, "an axe takes the wax off first");
        use.accept(new ItemStack(Items.IRON_AXE));
        h.assertTrue(level.getBlockState(at).getValue(age) == 1, "then scrapes a stage back");
        // Worked by a sheep, it wears; a waxed comb beside it, also worked, doesn't.
        BlockPos other = be.getBlockPos().west(2);
        level.setBlockAndUpdate(other, comb.setValue(waxed, true));
        echoSheep(h, at.south(), "iron");
        echoSheep(h, other.south(), "iron");
        h.runAfterDelay(25, () -> {
            h.assertTrue(be.wear(at) > 0 && be.wear(other) == 0, "the bare comb wears and the waxed one doesn't, got " + be.wear(at) + " and " + be.wear(other));
            h.succeed();
        });
    }

    /** Its dome arcs the charge into its buffer, which goes out of its base into a Hume Energy Cube beside it. */
    /** A Grass Seeder on a 7x7 grass floor; {@code dirt} of its ground blocks (by offset from it) eaten down to dirt. */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity buildSeeder(GameTestHelper h, int[]... dirt) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.GRASS_BLOCK);
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(6, 2, 6))) h.setBlock(p, Blocks.AIR);
        BlockPos at = h.absolutePos(new BlockPos(3, 1, 3));
        h.getLevel().setBlockAndUpdate(at, ModBlocks.GRASS_SEEDER.get().defaultBlockState());
        for (int[] d : dirt) h.getLevel().setBlockAndUpdate(at.offset(d[0], -1, d[1]), Blocks.DIRT.defaultBlockState());
        return (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity) h.getLevel().getBlockEntity(at);
    }

    /** With no fuel it leaves eaten dirt bare; a Bone Meal regrasses it, a block every turn, eight blocks to the Bone Meal. */
    private static void seederBoneMeal(GameTestHelper h) {
        var be = buildSeeder(h, new int[]{1, 0}, new int[]{-2, 2}, new int[]{0, -1});
        BlockPos at = be.getBlockPos();
        var level = h.getLevel();
        be.work(level, at);
        h.assertTrue(be.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.Status.NO_FUEL && be.dirt() == 3,
                "no fuel: the dirt stays, got " + be.status() + " with " + be.dirt() + " dirt");
        h.assertTrue(be.insertBoneMeal(new ItemStack(Items.BONE_MEAL, 1)) == 1, "it takes Bone Meal");
        for (int i = 0; i < 3; i++) be.work(level, at);
        for (int[] d : new int[][]{{1, 0}, {-2, 2}, {0, -1}}) {
            h.assertTrue(level.getBlockState(at.offset(d[0], -1, d[1])).is(Blocks.GRASS_BLOCK), "the dirt at " + d[0] + "," + d[1] + " is grass again");
        }
        h.assertTrue(be.boneMeal().isEmpty() && be.boneCharges() == 5, "one Bone Meal opened, 5 of its 8 blocks left, got " + be.boneMeal() + " and " + be.boneCharges());
        h.assertTrue(be.tended() == 3 && be.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.Status.TENDING,
                "three blocks tended, got " + be.tended());
        h.succeed();
    }

    /** With power and water it uses them (800 FE and 10 mB a block) and keeps its Bone Meal. */
    private static void seederPower(GameTestHelper h) {
        var be = buildSeeder(h, new int[]{2, 2});
        BlockPos at = be.getBlockPos();
        be.insertBoneMeal(new ItemStack(Items.BONE_MEAL, 4));
        be.fillEnergy();
        be.getTank().fill(1000);
        int energy = be.getEnergyHandler().getAmountAsInt();
        be.work(h.getLevel(), at);
        h.assertTrue(h.getLevel().getBlockState(at.offset(2, -1, 2)).is(Blocks.GRASS_BLOCK), "the dirt is grass again");
        h.assertTrue(energy - be.getEnergyHandler().getAmountAsInt() == 800 && be.getTank().getAmount() == 990,
                "800 FE and 10 mB used, got " + (energy - be.getEnergyHandler().getAmountAsInt()) + " FE and " + be.getTank().getAmount() + " mB left");
        h.assertTrue(be.boneMeal().getCount() == 4 && be.boneCharges() == 0, "its Bone Meal kept, got " + be.boneMeal());
        h.succeed();
    }

    /** Once it's all grass it grows short grass on 40% of it (10 of 24), then rests without using fuel. */
    private static void seederShortGrass(GameTestHelper h) {
        var be = buildSeeder(h);
        BlockPos at = be.getBlockPos();
        be.setTrim(true); // so it doesn't start overgrowing once it's green
        be.insertBoneMeal(new ItemStack(Items.BONE_MEAL, 8));
        for (int i = 0; i < 14; i++) be.work(h.getLevel(), at);
        int planted = 0;
        for (BlockPos p : com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.ground(at)) {
            if (h.getLevel().getBlockState(p.above()).is(Blocks.SHORT_GRASS)) planted++;
        }
        h.assertTrue(planted == 10 && be.tended() == 10, "short grass on 10 of 24, got " + planted + " (" + be.tended() + " tended)");
        h.assertTrue(be.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.Status.ALL_GREEN, "then it rests, got " + be.status());
        h.assertTrue(be.boneMeal().getCount() == 6 && be.boneCharges() == 6, "10 blocks of Bone Meal used, got " + be.boneMeal().getCount() + " + " + be.boneCharges());
        h.succeed();
    }

    /**
     * A sheared sheep in its area grazes (eating the grass under it to dirt) and regrows its wool much sooner than on its
     * own; one with its fleece on is left alone.
     */
    private static void seederGrazing(GameTestHelper h) {
        var be = buildSeeder(h);
        BlockPos at = be.getBlockPos();
        var level = h.getLevel();
        be.insertBoneMeal(new ItemStack(Items.BONE_MEAL, 64));
        Sheep sheared = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        sheared.snapTo(at.getX() + 2.5, at.getY(), at.getZ() + 0.5, 0, 0);
        sheared.setNoAi(true); // so only the seeder makes it graze
        sheared.setSheared(true);
        level.addFreshEntity(sheared);
        Sheep fleeced = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        fleeced.snapTo(at.getX() - 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
        fleeced.setNoAi(true);
        level.addFreshEntity(fleeced);
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.grazeChance() > 0.1,
                "a sheared sheep grazes about 1 in 9 turns, got " + com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.grazeChance());
        // 200 turns: the chance it never grazes is about 4 in 100 billion.
        for (int i = 0; i < 200 && sheared.isSheared(); i++) be.work(level, at);
        h.assertTrue(!sheared.isSheared(), "the sheared sheep grazed and regrew its wool");
        h.assertTrue(!fleeced.isSheared() && level.getBlockState(fleeced.blockPosition().below()).is(Blocks.GRASS_BLOCK), "the fleeced sheep was left alone");
        h.succeed();
    }

    /**
     * Left green with nothing to do and Trim off, its short grass shoots up into tall grass; with Trim on, it cuts it all
     * back to short grass and stays green.
     */
    private static void seederOvergrowth(GameTestHelper h) {
        var be = buildSeeder(h);
        BlockPos at = be.getBlockPos();
        var level = h.getLevel();
        be.insertBoneMeal(new ItemStack(Items.BONE_MEAL, 64));
        boolean overgrown = false;
        for (int i = 0; i < 300; i++) {
            be.work(level, at);
            overgrown |= be.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.Status.OVERGROWN;
        }
        h.assertTrue(overgrown && be.tallGrass() > 10, "it overgrew, got " + be.tallGrass() + " tall grass");
        int tall = 0;
        for (BlockPos p : com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.ground(at)) {
            if (level.getBlockState(p.above()).is(Blocks.TALL_GRASS) && level.getBlockState(p.above(2)).is(Blocks.TALL_GRASS)) tall++;
        }
        h.assertTrue(tall == be.tallGrass(), "the tall grass is in the world, both halves: " + tall);
        be.setTrim(true);
        for (int i = 0; i < 60; i++) be.work(level, at);
        h.assertTrue(be.tallGrass() == 0 && be.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.Status.ALL_GREEN,
                "trimmed back, got " + be.tallGrass() + " tall grass and " + be.status());
        for (BlockPos p : com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.ground(at)) {
            h.assertTrue(!level.getBlockState(p.above(2)).is(Blocks.TALL_GRASS), "no tall grass left at " + p);
        }
        h.succeed();
    }

    /** A Herd Counter facing south in the middle of a grass floor (world positions: the pen is south of it). */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity buildHerdCounter(GameTestHelper h) {
        BlockPos at = h.absolutePos(new BlockPos(3, 1, 1));
        var level = h.getLevel();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-3, -1, -2), at.offset(3, -1, 5))) level.setBlockAndUpdate(p, Blocks.GRASS_BLOCK.defaultBlockState());
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-3, 0, -2), at.offset(3, 3, 5))) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(at, ModBlocks.HERD_COUNTER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.HerdCounterBlock.FACING, Direction.SOUTH));
        return (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity) level.getBlockEntity(at);
    }

    private static Sheep plainSheep(GameTestHelper h, BlockPos at) {
        Sheep sheep = Vanilla.SHEEP.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        sheep.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        sheep.setNoAi(true);
        h.getLevel().addFreshEntity(sheep);
        return sheep;
    }

    /**
     * It counts the sheep in front of its screen, not the one behind it: every sheep, ready and sheared ones, lambs,
     * ones low on infusion, and how many of each resource; a filter narrows each mode to one resource.
     */
    private static void herdCounterCounts(GameTestHelper h) {
        var be = buildHerdCounter(h);
        BlockPos at = be.getBlockPos();
        var level = h.getLevel();
        echoSheep(h, at.offset(-1, 0, 2), "iron");
        echoSheep(h, at.offset(1, 0, 2), "iron");
        echoSheep(h, at.offset(0, 0, 3), "iron").setSheared(true);
        plainSheep(h, at.offset(-2, 0, 3)).setAge(-24000);
        Sheep gold = echoSheep(h, at.offset(2, 0, 3), "gold");
        SheepInfusionEvents.setInfusion(gold, new SheepInfusion(Optional.of(ResourcefulSheep.id("gold")), 1, 0)); // one shearing left
        plainSheep(h, at.offset(0, 0, -2)); // behind its screen: not in its pen
        var state = level.getBlockState(at);
        be.count(level, at, state);
        h.assertTrue(be.total() == 5 && be.lambs() == 1 && be.sheared() == 1 && be.ready() == 3 && be.lowInfusion() == 1,
                "5 sheep: 1 lamb, 1 sheared, 3 ready, 1 low on infusion; got " + be.total() + ", " + be.lambs() + ", " + be.sheared() + ", " + be.ready() + ", " + be.lowInfusion());
        h.assertTrue(be.byResource().getOrDefault(ResourcefulSheep.id("iron").toString(), 0) == 3
                        && be.byResource().keySet().iterator().next().equals(ResourcefulSheep.id("iron").toString()),
                "3 iron sheep, the most, got " + be.byResource());
        h.assertTrue(be.value() == 5 && be.comparatorSignal() == 5, "Count: 5, signal 5, got " + be.value() + " / " + be.comparatorSignal());
        be.setFilter(ResourceWoolItem.create(ResourcefulSheep.id("iron"), 1));
        be.count(level, at, state);
        h.assertTrue(be.value() == 3, "Count, iron only: 3, got " + be.value());
        be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.READY);
        be.count(level, at, state);
        h.assertTrue(be.value() == 2, "Ready, iron only: 2, got " + be.value());
        be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.SHEARED);
        be.count(level, at, state);
        h.assertTrue(be.value() == 1, "Sheared, iron only: 1, got " + be.value());
        be.setFilter(ItemStack.EMPTY);
        be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.LAMBS);
        be.count(level, at, state);
        h.assertTrue(be.value() == 1, "Lambs: 1, got " + be.value());
        be.setFilter(new ItemStack(Items.SHEEP_SPAWN_EGG));
        be.count(level, at, state);
        h.assertTrue(be.value() == 1, "Lambs, plain only (a sheep's egg): 1, got " + be.value());
        be.setFilter(ItemStack.EMPTY);
        be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.LOW_INFUSION);
        be.count(level, at, state);
        h.assertTrue(be.value() == 1 && be.comparatorSignal() == 1, "Low on infusion: 1, got " + be.value());
        h.succeed();
    }

    /**
     * A sheep dashing past is now and then counted twice (and otherwise once); with Steady on, the count is averaged, and
     * settles back to exactly 1 once the sheep stops.
     */
    private static void herdCounterSteady(GameTestHelper h) {
        var be = buildHerdCounter(h);
        BlockPos at = be.getBlockPos();
        var level = h.getLevel();
        var state = level.getBlockState(at);
        Sheep sheep = plainSheep(h, at.offset(0, 0, 2));
        int min = Integer.MAX_VALUE, max = 0;
        for (int i = 0; i < 60; i++) {
            sheep.setDeltaMovement(0.5, 0, 0);
            be.count(level, at, state);
            min = Math.min(min, be.value());
            max = Math.max(max, be.value());
        }
        h.assertTrue(min == 1 && max == 2, "a dashing sheep is counted once, and now and then twice: got " + min + " to " + max);
        sheep.setDeltaMovement(0, 0, 0);
        be.count(level, at, state);
        h.assertTrue(be.value() == 1 && !be.doubled(), "standing still, once: got " + be.value());
        be.setSteady(true);
        for (int i = 0; i < 5; i++) {
            sheep.setDeltaMovement(0.5, 0, 0);
            be.count(level, at, state);
        }
        h.assertTrue(be.value() >= 1 && be.value() <= 2, "steady while it dashes: between 1 and 2, got " + be.value());
        for (int i = 0; i < 5; i++) {
            sheep.setDeltaMovement(0, 0, 0);
            be.count(level, at, state);
        }
        h.assertTrue(be.value() == 1, "steady once it stops: 1, got " + be.value());
        h.succeed();
    }

    /**
     * A Machine Status Panel shows each machine linked to it with its status and key number, counts the ones with a
     * problem on its comparator, reads a broken one as Missing, unlinks on a second click, refuses a block that isn't a
     * machine, and holds 4.
     */
    private static void statusPanel(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(1, 1, 1));
        for (BlockPos p : BlockPos.betweenClosed(base.offset(0, -1, 0), base.offset(5, -1, 5))) level.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
        BlockPos panelPos = base.offset(0, 1, 0);
        level.setBlockAndUpdate(panelPos, ModBlocks.STATUS_PANEL.get().defaultBlockState());
        var panel = (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity) level.getBlockEntity(panelPos);
        BlockPos furnace = base.offset(2, 0, 0), seeder = base.offset(4, 0, 3), cube = base.offset(2, 0, 3), stone = base.offset(0, 0, 4);
        level.setBlockAndUpdate(furnace, ModBlocks.POWERED_FURNACE.get().defaultBlockState());
        level.setBlockAndUpdate(seeder, ModBlocks.GRASS_SEEDER.get().defaultBlockState());
        level.setBlockAndUpdate(cube, ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState());
        level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        ((com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity) level.getBlockEntity(seeder)).work(level, seeder); // bare dirt, no fuel
        var LINKED = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.LinkResult.LINKED;
        h.assertTrue(panel.toggleLink(level, furnace) == LINKED && panel.toggleLink(level, seeder) == LINKED && panel.toggleLink(level, cube) == LINKED,
                "three machines linked");
        h.assertTrue(panel.toggleLink(level, stone) == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.LinkResult.NOT_A_MACHINE,
                "stone isn't a machine");
        var rows = panel.rows();
        h.assertTrue(rows.size() == 3, "three rows, got " + rows.size());
        h.assertTrue(rows.get(0).name().equals(ModBlocks.POWERED_FURNACE.get().getDescriptionId()) && rows.get(0).report().status().equals("idle"),
                "the furnace, idle: got " + rows.get(0));
        h.assertTrue(rows.get(1).report().status().equals("no_fuel")
                        && rows.get(1).report().level() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Level.PROBLEM,
                "the seeder, out of fuel (a problem): got " + rows.get(1).report());
        h.assertTrue(rows.get(2).report().number().equals("0%"), "the cube, 0% stored: got " + rows.get(2).report());
        h.assertTrue(panel.comparatorSignal() == 1, "one problem on its comparator, got " + panel.comparatorSignal());
        level.setBlockAndUpdate(furnace, Blocks.AIR.defaultBlockState());
        panel.refresh(level, panelPos, level.getBlockState(panelPos));
        h.assertTrue(panel.rows().get(0).report().status().equals("missing") && panel.comparatorSignal() == 2, "the broken furnace reads Missing: two problems");
        h.assertTrue(panel.toggleLink(level, furnace) == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.LinkResult.UNLINKED
                && panel.rows().size() == 2, "a second click unlinks it");
        BlockPos[] more = {base.offset(5, 0, 0), base.offset(5, 0, 1), base.offset(5, 0, 2)};
        for (BlockPos m : more) level.setBlockAndUpdate(m, ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState());
        panel.toggleLink(level, more[0]);
        panel.toggleLink(level, more[1]);
        h.assertTrue(panel.toggleLink(level, more[2]) == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.LinkResult.FULL,
                "a panel holds 4");
        h.succeed();
    }

    /**
     * Nine panels facing the same way in a 3x3 join into one screen: its top-left panel (as you look at it) holds the
     * links (taking over any another panel had) and shows 36; the comparator reads the same on every panel. Broken, the
     * top-left panel's links go to its neighbour and the rest stop joining (not a rectangle); four across don't join.
     */
    private static void statusPanelJoin(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(1, 1, 1));
        var panelState = ModBlocks.STATUS_PANEL.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.FACING, Direction.SOUTH);
        for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) level.setBlockAndUpdate(base.offset(x, y, 0), panelState);
        // Facing south, you look north at it: its left is west, so its top-left panel is the west end of the top row.
        BlockPos topLeft = base.offset(0, 2, 0), middle = base.offset(1, 1, 0);
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.master(level, middle).equals(topLeft), "the top-left panel holds the screen");
        var mid = level.getBlockState(middle);
        h.assertTrue(mid.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.JOINED_UP)
                && mid.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.JOINED_DOWN)
                && mid.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.JOINED_LEFT)
                && mid.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.JOINED_RIGHT), "the middle panel joins on every side");
        var master = (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity) level.getBlockEntity(topLeft);
        var other = (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity) level.getBlockEntity(middle);
        h.assertTrue(master.capacity() == 36 && other.capacity() == 36, "a 3x3 screen shows 36, got " + master.capacity());
        java.util.List<BlockPos> cubes = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            BlockPos c = base.offset(i, -1, 3);
            level.setBlockAndUpdate(c, ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState());
            cubes.add(c);
        }
        for (int i = 0; i < 5; i++) master.toggleLink(level, cubes.get(i));
        other.adopt(java.util.List.of(cubes.get(5))); // a link another panel had before it joined
        master.refresh(level, topLeft, level.getBlockState(topLeft));
        h.assertTrue(master.links().size() == 6 && other.links().isEmpty() && master.rows().size() == 6, "the top-left panel takes every link: got " + master.links().size());
        other.refresh(level, middle, mid);
        h.assertTrue(other.rows().isEmpty(), "the other panels draw nothing of their own");
        h.assertTrue(level.getBlockState(middle).getAnalogOutputSignal(level, middle, Direction.NORTH) == master.comparatorSignal(), "every panel reads the screen's signal");
        // Break the top-left panel: its links go next door, and what's left (an L) stops joining.
        level.setBlockAndUpdate(topLeft, Blocks.AIR.defaultBlockState());
        var neighbour = (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity) level.getBlockEntity(base.offset(1, 2, 0));
        h.assertTrue(neighbour.links().size() == 6, "its links went to its neighbour: got " + neighbour.links().size());
        h.assertTrue(!level.getBlockState(middle).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.JOINED_UP),
                "eight panels in an L don't join");
        // Four across don't join either.
        level.setBlockAndUpdate(topLeft, panelState);
        h.assertTrue(level.getBlockState(middle).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.JOINED_UP), "3x3 again: joined");
        for (int y = 0; y < 3; y++) level.setBlockAndUpdate(base.offset(3, y, 0), panelState);
        h.assertTrue(!level.getBlockState(middle).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.JOINED_UP),
                "4 across is too wide: they stay separate");
        h.succeed();
    }

    /**
     * Its window's settings: moving a machine changes its order, a name shows instead of the block's (and an empty one
     * goes back), an alert switched off takes that machine's problem off the comparator, Problems first puts problems
     * at the top, and Working counts working machines instead. The names, order and settings survive a save and load.
     */
    private static void statusPanelWindow(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(1, 1, 1));
        for (BlockPos p : BlockPos.betweenClosed(base.offset(0, -1, 0), base.offset(5, -1, 5))) level.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
        BlockPos panelPos = base.offset(0, 1, 0);
        level.setBlockAndUpdate(panelPos, ModBlocks.STATUS_PANEL.get().defaultBlockState());
        var panel = (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity) level.getBlockEntity(panelPos);
        BlockPos cube = base.offset(2, 0, 0), seeder = base.offset(4, 0, 3), furnace = base.offset(2, 0, 3);
        level.setBlockAndUpdate(cube, ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState());
        level.setBlockAndUpdate(seeder, ModBlocks.GRASS_SEEDER.get().defaultBlockState());
        level.setBlockAndUpdate(furnace, ModBlocks.POWERED_FURNACE.get().defaultBlockState());
        level.setBlockAndUpdate(furnace.north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState());
        ((com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity) level.getBlockEntity(furnace)).setItem(0, new ItemStack(Items.RAW_IRON, 64));
        ((com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity) level.getBlockEntity(seeder)).work(level, seeder); // no fuel: a problem
        panel.toggleLink(level, cube);
        panel.toggleLink(level, seeder);
        panel.toggleLink(level, furnace);
        h.runAfterDelay(20, () -> {
            panel.refresh(level, panelPos, level.getBlockState(panelPos));
            h.assertTrue(panel.comparatorSignal() == 1, "one problem, got " + panel.comparatorSignal());
            panel.move(level, 2, -1);
            h.assertTrue(panel.links().get(1).equals(furnace) && panel.links().get(2).equals(seeder), "the furnace moved up: " + panel.links());
            panel.rename(level, 0, "  Main bank  ");
            h.assertTrue(panel.rows().get(0).custom().equals("Main bank") && panel.rows().get(0).displayName().getString().equals("Main bank"), "renamed");
            panel.toggleAlert(level, 2);
            h.assertTrue(!panel.rows().get(2).alert() && panel.comparatorSignal() == 0, "the seeder's alert off: its problem doesn't count");
            panel.toggleAlert(level, 2);
            panel.cycleSort(level); // problems first
            h.assertTrue(panel.rows().get(0).pos().equals(seeder) && panel.rows().get(0).link() == 2, "problems first: the seeder on top, still link 2");
            panel.toggleSignal(level); // working
            h.assertTrue(panel.comparatorSignal() == 1, "Working: the furnace, got " + panel.comparatorSignal());
            // Saved and loaded again.
            var saved = panel.saveWithFullMetadata(level.registryAccess());
            var copy = (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity)
                    net.minecraft.world.level.block.entity.BlockEntity.loadStatic(panelPos, level.getBlockState(panelPos), saved, level.registryAccess());
            h.assertTrue(copy != null && copy.links().equals(panel.links()) && copy.sort() == panel.sort() && copy.signal() == panel.signal(), "saved and loaded");
            panel.rename(level, 0, "");
            h.assertTrue(panel.rows().stream().noneMatch(r -> r.link() == 0 && !r.custom().isEmpty()), "an empty name: its own name again");
            h.succeed();
        });
    }

    /**
     * A Static Generator links from its dome as well as its base (both are one machine: the second click unlinks it), and
     * an Echo Chamber from any of its blocks.
     */
    private static void statusPanelBigMachines(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(base, ModBlocks.STATUS_PANEL.get().defaultBlockState());
        var panel = (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity) level.getBlockEntity(base);
        BlockPos generator = base.offset(3, 0, 0);
        var gen = ModBlocks.STATIC_GENERATOR.get().defaultBlockState();
        level.setBlockAndUpdate(generator, gen);
        level.setBlockAndUpdate(generator.above(), gen.setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var LINKED = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.LinkResult.LINKED;
        var UNLINKED = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.LinkResult.UNLINKED;
        h.assertTrue(panel.toggleLink(level, generator.above()) == LINKED && panel.links().equals(java.util.List.of(generator)),
                "the dome links the generator (its base): got " + panel.links());
        h.assertTrue(panel.rows().get(0).name().equals(ModBlocks.STATIC_GENERATOR.get().getDescriptionId()), "it shows the Static Generator");
        h.assertTrue(panel.toggleLink(level, generator) == UNLINKED && panel.links().isEmpty(), "the base is the same machine: unlinked");
        // And with the wrench, the way a player does it: the panel, the dome, the panel again.
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(base.south(2)));
        ItemStack wrench = new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        for (BlockPos click : new BlockPos[]{base, generator.above(), base}) {
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(click), Direction.NORTH, click, false);
            player.gameMode.useItemOn(player, level, wrench, InteractionHand.MAIN_HAND, hit);
        }
        h.assertTrue(panel.links().equals(java.util.List.of(generator)), "a wrench click on the dome links it: got " + panel.links());
        panel.toggleLink(level, generator);
        BlockPos front = base.offset(5, 0, 0);
        com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.fill(level, front, ModBlocks.ECHO_CHAMBER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING, Direction.SOUTH));
        BlockPos corner = front.offset(1, 2, 0);
        h.assertTrue(level.getBlockState(corner).is(ModBlocks.ECHO_CHAMBER.get()), "the chamber's top corner");
        h.assertTrue(panel.toggleLink(level, corner) == LINKED && panel.rows().get(0).name().equals(ModBlocks.ECHO_CHAMBER.get().getDescriptionId()),
                "a corner of the Echo Chamber links it: got " + panel.links());
        h.succeed();
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity crate(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel.below(), Blocks.STONE);
        h.setBlock(rel, ModBlocks.CRATE.get().defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.FACING, Direction.SOUTH));
        return h.getBlockEntity(rel, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity.class);
    }

    /** 2,048 cobblestone and no dirt; it hands back a stack at a time; locked, it keeps cobblestone when emptied. */
    private static void crateStores(GameTestHelper h) {
        var be = crate(h, new BlockPos(1, 1, 1));
        h.assertTrue(be.capacity() == 32 * 64, "32 stacks of 64: " + be.capacity());
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 64);
        h.assertTrue(be.add(cobble) == 64 && cobble.isEmpty() && be.count() == 64, "it takes a stack of cobblestone");
        h.assertFalse(be.accepts(new ItemStack(Items.DIRT)), "then only cobblestone");
        h.assertTrue(be.room(new ItemStack(Items.COBBLESTONE)) == 2048 - 64, "room for the rest");
        ItemStack out = be.take(100);
        h.assertTrue(out.is(Items.COBBLESTONE) && out.getCount() == 64 && be.count() == 0, "a take is a stack at most: " + out);
        h.assertTrue(be.item().isEmpty() && be.accepts(new ItemStack(Items.DIRT)), "empty and unlocked, it takes anything");
        be.add(new ItemStack(Items.COBBLESTONE, 3));
        be.setLocked(true);
        be.take(3);
        h.assertTrue(be.item().is(Items.COBBLESTONE) && be.locked() && !be.accepts(new ItemStack(Items.DIRT)),
                "locked, it keeps cobblestone when empty");
        be.setLocked(false);
        h.assertTrue(be.item().isEmpty(), "unlocked when empty: it forgets it");
        // Unstackables: 32 of them.
        h.assertTrue(be.room(new ItemStack(Items.IRON_SWORD)) == 32, "32 swords: " + be.room(new ItemStack(Items.IRON_SWORD)));
        // A player's double-click puts in every matching item they carry.
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setItem(3, new ItemStack(Items.COBBLESTONE, 40));
        player.getInventory().setItem(7, new ItemStack(Items.COBBLESTONE, 20));
        player.getInventory().setItem(8, new ItemStack(Items.DIRT, 5));
        ItemStack held = new ItemStack(Items.COBBLESTONE, 10);
        be.store(h.getLevel(), player, held);
        h.assertTrue(be.count() == 10 && player.getInventory().getItem(3).getCount() == 40, "one click: what's in hand");
        be.store(h.getLevel(), player, ItemStack.EMPTY);
        h.assertTrue(be.count() == 70 && player.getInventory().getItem(3).isEmpty() && player.getInventory().getItem(8).getCount() == 5,
                "a second click straight after: all the cobblestone, not the dirt: " + be.count());
        h.succeed();
    }

    /**
     * A new crate's lining matches its item (not dyed); a sneak-click with lime wool dyes it lime and uses the wool; again
     * with lime it goes back to matching its item and keeps the wool.
     */
    private static void crateLining(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        crate(h, at);
        var DYED = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.DYED;
        var LINING = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.LINING;
        h.assertFalse(h.getBlockState(at).getValue(DYED), "a new crate's lining follows its item");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.jax3.barelyfunctionalcontraptions.compat.Vanilla.wool(net.minecraft.world.item.DyeColor.LIME), 2));
        var use = (java.util.function.Supplier<net.minecraft.world.InteractionResult>) () -> com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.use(
                h.getLevel(), h.absolutePos(at), h.getBlockState(at), player, InteractionHand.MAIN_HAND, Direction.EAST);
        h.assertTrue(use.get().consumesAction(), "the sneak-click takes");
        h.assertTrue(h.getBlockState(at).getValue(DYED) && h.getBlockState(at).getValue(LINING) == net.minecraft.world.item.DyeColor.LIME
                && player.getMainHandItem().getCount() == 1, "dyed lime, one wool used: " + h.getBlockState(at));
        use.get();
        h.assertTrue(!h.getBlockState(at).getValue(DYED) && player.getMainHandItem().getCount() == 1,
                "lime again: back to matching its item, wool kept: " + h.getBlockState(at));
        h.succeed();
    }

    /** Each Wool Bale adds 32 stacks, up to 4; a crate with something in it can't go in another. */
    private static void crateBales(GameTestHelper h) {
        var be = crate(h, new BlockPos(1, 1, 1));
        for (int i = 0; i < 4; i++) h.assertTrue(be.addBale(), "bale " + (i + 1) + " packs in");
        h.assertFalse(be.addBale(), "but not a fifth");
        h.assertTrue(be.stacks() == 160 && be.capacity() == 160 * 64, "160 stacks: " + be.stacks());
        ItemStack full = new ItemStack(ModItems.CRATE.get());
        full.set(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.CRATE_CONTENTS.get(),
                new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.DIRT), 5, false, 0));
        h.assertFalse(be.accepts(full), "a crate of dirt can't go in a crate");
        h.assertTrue(be.accepts(new ItemStack(ModItems.CRATE.get())), "an empty one can");
        h.assertFalse(be.accepts(new ItemStack(Items.SHULKER_BOX)), "nor can a shulker box");
        h.succeed();
    }

    /** A hopper above fills it, the item capability fills and empties it, and a comparator reads how full it is. */
    private static void crateAutomation(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        var be = crate(h, at);
        h.setBlock(at.above(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));
        var hopper = h.getBlockEntity(at.above(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.OAK_LOG, 5));
        var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, h.absolutePos(at), Direction.EAST);
        h.assertTrue(handler != null, "pipes reach it from the side");
        h.succeedWhen(() -> {
            h.assertTrue(be.count() == 5 && be.item().is(Items.OAK_LOG), "the hopper empties into it: " + be.count());
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                int in = handler.insert(0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.OAK_LOG), 1000, tx);
                h.assertTrue(in == 1000, "a pipe puts in 1000: " + in);
                h.assertTrue(handler.insert(0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.DIRT), 1, tx) == 0, "but no dirt");
                tx.commit();
            }
            h.assertTrue(be.count() == 1005, "committed: " + be.count());
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                handler.extract(0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.OAK_LOG), 500, tx);
                // not committed: nothing moves
            }
            h.assertTrue(be.count() == 1005, "an aborted take leaves it: " + be.count());
            int signal = h.getLevel().getBlockState(h.absolutePos(at)).getAnalogOutputSignal(h.getLevel(), h.absolutePos(at), Direction.NORTH);
            h.assertTrue(signal == 1 + 14 * 1005 / 2048, "the comparator reads how full: " + signal);
        });
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity controller(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel.below(), Blocks.STONE);
        h.setBlock(rel, ModBlocks.CRATE_CONTROLLER.get());
        return h.getBlockEntity(rel, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.class);
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents contents(net.minecraft.world.item.Item item, int count, boolean locked) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(item), count, locked, 0);
    }

    /** Crates touching it, and touching those, join its store and get a brass tag; one across a gap doesn't. */
    private static void controllerJoins(GameTestHelper h) {
        var c = controller(h, new BlockPos(1, 1, 1));
        crate(h, new BlockPos(2, 1, 1));
        crate(h, new BlockPos(3, 1, 1));
        h.setBlock(new BlockPos(3, 2, 1), ModBlocks.CRATE.get());
        crate(h, new BlockPos(5, 1, 1));
        c.refresh(h.getLevel());
        h.assertTrue(c.crateCount() == 3, "three touching crates join: " + c.crateCount());
        var TAGGED = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.TAGGED;
        h.assertTrue(h.getBlockState(new BlockPos(2, 1, 1)).getValue(TAGGED) && h.getBlockState(new BlockPos(3, 2, 1)).getValue(TAGGED), "with brass tags");
        h.assertFalse(h.getBlockState(new BlockPos(5, 1, 1)).getValue(TAGGED), "the one across the gap stays out");
        h.assertTrue(c.empties() == 3 && c.capacity() == 3 * 2048, "three empty crates of 2,048: " + c.capacity());
        h.succeed();
    }

    /** Cobblestone goes to the cobblestone crate, iron to an empty one (not the one locked to dirt), dirt to the dirt one. */
    private static void controllerRoutes(GameTestHelper h) {
        var c = controller(h, new BlockPos(1, 1, 1));
        var cobble = crate(h, new BlockPos(2, 1, 1));
        var dirt = crate(h, new BlockPos(3, 1, 1));
        var empty = crate(h, new BlockPos(4, 1, 1));
        cobble.setContents(contents(Items.COBBLESTONE, 100, false));
        dirt.setContents(contents(Items.DIRT, 0, true));
        c.refresh(h.getLevel());
        var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, h.absolutePos(new BlockPos(1, 1, 1)), Direction.UP);
        h.assertTrue(handler != null && handler.size() == 3, "pipes see a slot for each crate");
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            h.assertTrue(handler.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COBBLESTONE), 50, tx) == 50, "50 cobblestone in");
            h.assertTrue(handler.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.IRON_INGOT), 10, tx) == 10, "10 iron in");
            h.assertTrue(handler.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.DIRT), 5, tx) == 5, "5 dirt in");
            tx.commit();
        }
        h.assertTrue(cobble.count() == 150, "the cobblestone went to its crate: " + cobble.count());
        h.assertTrue(empty.item().is(Items.IRON_INGOT) && empty.count() == 10, "the iron to the empty crate: " + empty.contents());
        h.assertTrue(dirt.item().is(Items.DIRT) && dirt.count() == 5, "the dirt to the crate locked to it: " + dirt.contents());
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            h.assertTrue(handler.extract(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COBBLESTONE), 64, tx) == 64, "a pipe takes 64 cobblestone");
            tx.commit();
        }
        h.assertTrue(cobble.count() == 86, "out of its crate: " + cobble.count());
        h.succeed();
    }

    /** The wrench links a crate across the room, and the crate touching it comes too; unlinked, they go. */
    private static void controllerLinks(GameTestHelper h) {
        var c = controller(h, new BlockPos(1, 1, 1));
        crate(h, new BlockPos(5, 1, 4));
        crate(h, new BlockPos(6, 1, 4));
        h.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        var LINKED = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.LinkResult.LINKED;
        h.assertTrue(c.toggleLink(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 3)))
                == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.LinkResult.NOT_A_CRATE, "stone isn't a crate");
        h.assertTrue(c.toggleLink(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 4))) == LINKED && c.crateCount() == 2,
                "the far crate and its neighbour join: " + c.crateCount());
        h.assertTrue(c.toggleLink(h.getLevel(), h.absolutePos(new BlockPos(6, 1, 4)))
                == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.LinkResult.ALREADY_IN, "its neighbour's in already");
        h.assertTrue(c.toggleLink(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 4)))
                == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.LinkResult.UNLINKED && c.crateCount() == 0, "unlinked, both go");
        h.succeed();
    }

    /** Two controllers either side of one crate: the first has it, the second leaves it (and says so). */
    private static void controllerTaken(GameTestHelper h) {
        var a = controller(h, new BlockPos(1, 1, 1));
        crate(h, new BlockPos(2, 1, 1));
        var b = controller(h, new BlockPos(3, 1, 1));
        a.refresh(h.getLevel());
        b.refresh(h.getLevel());
        h.assertTrue(a.crateCount() == 1 && b.crateCount() == 0 && b.taken() == 1, "the crate stays with the first: " + a.crateCount() + " / " + b.crateCount());
        h.assertTrue(b.toggleLink(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)))
                == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity.LinkResult.TAKEN, "and the wrench can't take it");
        h.succeed();
    }

    /** Its only crate full: cobblestone has nowhere to go, the crate strains, and the panel and comparator say so. */
    private static void controllerFull(GameTestHelper h) {
        var c = controller(h, new BlockPos(1, 1, 1));
        var full = crate(h, new BlockPos(2, 1, 1));
        full.setContents(contents(Items.COBBLESTONE, 2048, false));
        c.refresh(h.getLevel());
        var handler = c.getItemHandler();
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            h.assertTrue(handler.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COBBLESTONE), 1, tx) == 0, "nothing fits");
        }
        h.assertTrue(full.straining(), "so the full crate strains");
        var report = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.report(c);
        h.assertTrue(report != null && report.status().equals("could_burst") && report.number().equals("100%"), "the status panel: " + report);
        int signal = h.getLevel().getBlockState(h.absolutePos(new BlockPos(1, 1, 1))).getAnalogOutputSignal(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), Direction.NORTH);
        h.assertTrue(signal == 15, "the comparator reads it full: " + signal);
        h.succeed();
    }

    /** A hopper on the controller fills the crates in its store. */
    private static void controllerHopper(GameTestHelper h) {
        var c = controller(h, new BlockPos(1, 1, 1));
        var crate = crate(h, new BlockPos(2, 1, 1));
        c.refresh(h.getLevel());
        h.setBlock(new BlockPos(1, 2, 1), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));
        h.getBlockEntity(new BlockPos(1, 2, 1), net.minecraft.world.level.block.entity.HopperBlockEntity.class).setItem(0, new ItemStack(Items.WHEAT, 4));
        h.succeedWhen(() -> h.assertTrue(crate.item().is(Items.WHEAT) && crate.count() == 4, "the wheat goes into the crate: " + crate.contents()));
    }

    /** With its controller gone, a crate loses its tag within a few seconds and can join another store. */
    private static void controllerGone(GameTestHelper h) {
        var c = controller(h, new BlockPos(1, 1, 1));
        crate(h, new BlockPos(2, 1, 1));
        c.refresh(h.getLevel());
        var TAGGED = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.TAGGED;
        h.assertTrue(h.getBlockState(new BlockPos(2, 1, 1)).getValue(TAGGED), "tagged");
        h.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
        h.succeedWhen(() -> h.assertFalse(h.getBlockState(new BlockPos(2, 1, 1)).getValue(TAGGED), "its tag comes off"));
    }

    /** A hopper pushing at a full crate strains it (each second of that is a burst roll), and leaves the hopper full. */
    private static void crateHopperStrains(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        var be = crate(h, at);
        be.setContents(new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.REDSTONE), 2048, false, 0));
        h.setBlock(at.above(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));
        var hopper = h.getBlockEntity(at.above(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.REDSTONE, 5));
        h.succeedWhen(() -> {
            h.assertTrue(be.straining() || be.count() < 2048, "the hopper strains it (or it burst): " + be.count());
            h.assertTrue(hopper.getItem(0).getCount() == 5 || be.count() < 2048, "and nothing goes in while it's full");
        });
    }

    /** Past 90% its lid bulges; a full crate pushed over and over bursts, scattering 8 and losing its lock; a strapped one never does. */
    private static void crateBursts(GameTestHelper h) {
        var BULGING = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.BULGING;
        var be = crate(h, new BlockPos(1, 1, 1));
        var strapped = crate(h, new BlockPos(4, 1, 1));
        h.setBlock(new BlockPos(4, 1, 1), h.getBlockState(new BlockPos(4, 1, 1)).setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.STRAPPED, true));
        strapped = h.getBlockEntity(new BlockPos(4, 1, 1), com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity.class);
        be.add(new ItemStack(Items.COBBLESTONE, 64));
        h.assertFalse(h.getBlockState(new BlockPos(1, 1, 1)).getValue(BULGING), "a stack in: flat lid");
        be.setContents(new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.COBBLESTONE), 1900, true, 0));
        h.assertTrue(h.getBlockState(new BlockPos(1, 1, 1)).getValue(BULGING), "over 90%: it bulges");
        be.setContents(new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.COBBLESTONE), 2048, true, 0));
        strapped.setContents(new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.COBBLESTONE), 2048, true, 0));
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        // 2% a push: 600 pushes all but certainly burst it (1 in 180,000 that it holds).
        for (int i = 0; i < 600 && be.isFull(); i++) {
            be.store(h.getLevel(), player, new ItemStack(Items.COBBLESTONE));
            strapped.store(h.getLevel(), player, new ItemStack(Items.COBBLESTONE));
        }
        h.assertTrue(be.count() == 2040 && !be.locked(), "it burst: 8 out and its lock broken, holds " + be.count());
        var spilled = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(1, 1, 1))).inflate(3));
        h.assertTrue(spilled.stream().mapToInt(e -> e.getItem().getCount()).sum() == 8, "8 cobblestone scattered: " + spilled.size());
        h.assertTrue(strapped.isFull() && strapped.locked(), "the strapped one held");
        h.succeed();
    }

    /** Broken, it drops itself with its items, lock and bales, its lining and its strap; placed again, it has them all back. */
    private static void crateKeeps(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 1, 1);
        var be = crate(h, at);
        BlockPos abs = h.absolutePos(at);
        h.getLevel().setBlockAndUpdate(abs, h.getLevel().getBlockState(abs).setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.LINING,
                net.minecraft.world.item.DyeColor.LIME).setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.DYED, true)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.STRAPPED, true));
        be = h.getBlockEntity(at, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity.class);
        be.setContents(new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.REDSTONE), 1500, true, 2));
        var drops = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(abs), h.getLevel(), abs, be);
        h.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.CRATE.get()), "it drops itself: " + drops);
        ItemStack dropped = drops.get(0);
        var contents = dropped.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.CRATE_CONTENTS.get());
        h.assertTrue(contents != null && contents.count() == 1500 && contents.item().is(Items.REDSTONE) && contents.locked() && contents.bales() == 2,
                "with its 1,500 redstone, lock and bales: " + contents);
        var blockState = dropped.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        h.assertTrue(blockState != null && "lime".equals(blockState.properties().get("lining")) && "true".equals(blockState.properties().get("dyed"))
                        && "true".equals(blockState.properties().get("strapped")),
                "and its lime lining and strap: " + blockState);
        // Placed again by a player.
        h.setBlock(at, Blocks.AIR);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, dropped);
        BlockPos below = abs.below();
        dropped.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false)));
        var state = h.getLevel().getBlockState(abs);
        h.assertTrue(state.is(ModBlocks.CRATE.get()) && state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.LINING)
                == net.minecraft.world.item.DyeColor.LIME && state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.DYED)
                && state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.STRAPPED)
                && state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.BULGING) == false, "placed lime and strapped: " + state);
        var again = h.getBlockEntity(at, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity.class);
        h.assertTrue(again.count() == 1500 && again.locked() && again.bales() == 2, "and full again: " + again.contents());
        var report = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.report(again);
        h.assertTrue(report != null && report.status().equals("stored") && report.number().equals("1500"), "a status panel shows it: " + report);
        h.succeed();
    }

    /** Full mode reads 0 until it counts its target, then 15. */
    private static void herdCounterFull(GameTestHelper h) {
        var be = buildHerdCounter(h);
        BlockPos at = be.getBlockPos();
        var level = h.getLevel();
        be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.FULL);
        be.setTarget(3);
        plainSheep(h, at.offset(-1, 0, 2));
        plainSheep(h, at.offset(1, 0, 2));
        be.count(level, at, level.getBlockState(at));
        h.assertTrue(be.value() == 2 && be.comparatorSignal() == 0, "2 of 3: signal 0, got " + be.comparatorSignal());
        plainSheep(h, at.offset(0, 0, 4));
        be.count(level, at, level.getBlockState(at));
        h.assertTrue(be.value() == 3 && be.comparatorSignal() == 15, "3 of 3: signal 15, got " + be.comparatorSignal());
        h.assertTrue(level.getBlockState(at).getAnalogOutputSignal(level, at, Direction.NORTH) == 15, "the comparator reads it");
        h.succeed();
    }

    /** Automatic Shears can't see a sheep standing in tall grass; once it's cut, they shear it. */
    private static void shearsTallGrass(GameTestHelper h) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.GRASS_BLOCK);
        BlockPos shearsPos = new BlockPos(3, 1, 1);
        h.setBlock(shearsPos, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH));
        var shears = h.getBlockEntity(shearsPos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.class);
        shears.getEnergy().set(50_000);
        BlockPos grass = h.absolutePos(new BlockPos(3, 1, 3));
        var level = h.getLevel();
        net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, Blocks.TALL_GRASS.defaultBlockState(), grass, 3);
        Sheep sheep = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        sheep.snapTo(grass.getX() + 0.5, grass.getY(), grass.getZ() + 0.5, 0, 0);
        sheep.setNoAi(true);
        level.addFreshEntity(sheep);
        BlockPos abs = h.absolutePos(shearsPos);
        h.assertTrue(shears.shearArea(level, abs, Direction.SOUTH) == 0 && !sheep.isSheared(), "hidden in tall grass: not sheared");
        level.setBlock(grass.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(grass, Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(shears.shearArea(level, abs, Direction.SOUTH) == 1 && sheep.isSheared(), "cut down: sheared");
        h.succeed();
    }

    private static void staticArcs(GameTestHelper h) {
        var be = buildStaticGenerator(h);
        BlockPos at = be.getBlockPos();
        var level = h.getLevel();
        level.setBlockAndUpdate(at.east(2), ModBlocks.COPPER_COMB.get().defaultBlockState());
        echoSheep(h, at.east(2).south(), "copper");
        level.setBlockAndUpdate(at.west(), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState());
        h.succeedWhen(() -> {
            var cube = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, at.west(), Direction.EAST);
            h.assertTrue(be.generating() == 150, "a Copper sheep makes 150 FE/t, got " + be.generating());
            h.assertTrue(cube != null && cube.getAmountAsLong() > 0, "the cube beside its base fills");
        });
    }

    /** Overcharged, it knocks anything in its pen back; one in a full set of Woollen Armour stays put. Sheep are left alone. */
    private static void staticShocks(GameTestHelper h) {
        var be = buildStaticGenerator(h);
        BlockPos at = be.getBlockPos();
        var level = h.getLevel();
        be.fillBuffer();
        be.setCharge(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.domeCapacity());
        // Armor stands: they don't wander, wear armour, and get pushed like anything else.
        var bare = Vanilla.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var wrapped = Vanilla.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        bare.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
        wrapped.snapTo(at.getX() - 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        wrapped.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.WOOLLEN_HELMET.get()));
        wrapped.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.WOOLLEN_CHESTPLATE.get()));
        wrapped.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.WOOLLEN_LEGGINGS.get()));
        wrapped.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.WOOLLEN_BOOTS.get()));
        level.addFreshEntity(bare);
        level.addFreshEntity(wrapped);
        Sheep sheep = echoSheep(h, at.south(2), "iron");
        double sheepStart = sheep.getZ();
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.insulation(wrapped) == 4, "a full set of Woollen Armour");
        double bareStart = bare.getX(), wrappedStart = wrapped.getX();
        h.runAfterDelay(14, () -> {
            h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.State.OVERCHARGED
                    || be.charge() < com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.domeCapacity(), "it's overcharged and shocking");
            h.assertTrue(bare.getX() - bareStart > 1.0, "the bare stand is knocked away from the dome, moved " + (bare.getX() - bareStart));
            h.assertTrue(Math.abs(wrapped.getX() - wrappedStart) < 0.6, "the insulated one isn't, moved " + (wrapped.getX() - wrappedStart));
            h.assertTrue(Math.abs(sheep.getZ() - sheepStart) < 0.1, "and the sheep is left alone");
            h.succeed();
        });
    }

    /** It pushes its power out of the port in the middle of its back, and offers power on that face only. */
    private static void echoPort(GameTestHelper h) {
        var be = buildEchoChamber(h);
        BlockPos core = be.getBlockPos(), port = core.south(), behind = port.south();
        net.minecraft.server.level.ServerLevel level = h.getLevel();
        h.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, port, Direction.SOUTH) != null, "the port offers power on its back face");
        h.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, port, Direction.EAST) == null, "but not on its sides");
        h.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, port.east(), Direction.SOUTH) == null, "and no other block of it does");
        level.setBlockAndUpdate(behind, ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState());
        echoSheep(h, core.north(3), "warden");
        be.hearSteadily(3);
        h.succeedWhen(() -> {
            var cube = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, behind, Direction.NORTH);
            h.assertTrue(cube != null && cube.getAmountAsLong() > 0, "the cube behind its port fills up");
        });
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity poweredFurnace(GameTestHelper h, BlockPos pos, boolean powered, ItemStack input) {
        h.setBlock(pos, ModBlocks.POWERED_FURNACE.get());
        var be = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity.class);
        be.setItem(0, input);
        if (powered) {
            var energy = (net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler) be.getEnergyHandler();
            energy.set(energy.getCapacityAsInt());
        }
        return be;
    }

    /**
     * The Powered Furnace smelts in 0.55 of a Furnace's time (Raw Iron: 110 ticks instead of 200), faster with Blaze
     * sheep around it (two: x1.47, about 75 ticks), not at all without power, and only what a Furnace can smelt.
     */
    private static void poweredFurnace(GameTestHelper h) {
        var alone = poweredFurnace(h, new BlockPos(1, 1, 1), true, new ItemStack(Items.RAW_IRON, 4));
        var blazing = poweredFurnace(h, new BlockPos(6, 1, 1), true, new ItemStack(Items.RAW_IRON, 4));
        sheepAt(h, new BlockPos(5, 1, 1), ResourcefulSheep.id("blaze"));
        sheepAt(h, new BlockPos(6, 1, 2), ResourcefulSheep.id("blaze"));
        var unpowered = poweredFurnace(h, new BlockPos(1, 1, 6), false, new ItemStack(Items.RAW_IRON, 4));
        h.assertFalse(alone.canPlaceItem(0, new ItemStack(Items.DIRT)), "dirt can't be smelted, so it doesn't go in");
        h.assertTrue(alone.canPlaceItem(0, new ItemStack(Items.RAW_IRON)), "raw iron can");
        int start = alone.getEnergyHandler().getAmountAsInt();
        h.runAfterDelay(95, () -> {
            h.assertTrue(blazing.heaterCount() == 2 && blazing.speed() > 140, "two Blaze sheep speed it up, speed " + blazing.speed() + " from " + blazing.heaterCount());
            h.assertTrue(blazing.output().is(Items.IRON_INGOT), "with Blaze sheep the first ingot is out by 95 ticks, got " + blazing.output());
            h.assertTrue(alone.output().isEmpty(), "alone it's not done yet (110 ticks), got " + alone.output());
        });
        h.runAfterDelay(125, () -> {
            h.assertTrue(alone.output().is(Items.IRON_INGOT) && alone.output().getCount() == 1, "alone, one ingot by 125 ticks (a Furnace takes 200), got " + alone.output());
            h.assertTrue(alone.storedExperience() > 0, "and it keeps the experience for whoever takes it");
            int used = start - alone.getEnergyHandler().getAmountAsInt();
            h.assertTrue(used >= 110 * 20 && used <= 125 * 20, "20 FE/t while smelting, used " + used);
            h.assertTrue(unpowered.output().isEmpty() && unpowered.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus.NO_ENERGY,
                    "nothing without power, status " + unpowered.status());
            h.succeed();
        });
    }

    /** Crafts with whatever crafting recipe matches the grid, or EMPTY. */
    private static ItemStack craft(GameTestHelper h, int width, int height, List<ItemStack> grid) {
        var input = net.minecraft.world.item.crafting.CraftingInput.of(width, height, grid);
        return h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel())
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    /**
     * Hitting a machine with the wrench breaks it at once and puts it, with what spills out of it, in the player's
     * inventory; other blocks aren't sped up, and without the wrench a machine breaks normally.
     */
    private static void wrenchDismantles(GameTestHelper h) {
        var player = McTest.mockServerPlayer(h, GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()));
        BlockPos burnerPos = new BlockPos(2, 1, 2);
        h.setBlock(burnerPos, ModBlocks.WOOL_BURNER.get());
        var burner = h.getBlockEntity(burnerPos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity.class);
        burner.setItem(0, new ItemStack(Vanilla.wool(net.minecraft.world.item.DyeColor.WHITE), 5));
        var state = h.getBlockState(burnerPos);
        h.assertTrue(player.getDestroySpeed(state) >= state.getDestroySpeed(h.getLevel(), h.absolutePos(burnerPos)) * 30, "the wrench breaks it in one hit");
        h.assertTrue(player.getDestroySpeed(Blocks.STONE.defaultBlockState()) <= 1.0F, "but doesn't speed up breaking stone");
        h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(burnerPos)), "it breaks");
        h.assertTrue(h.getBlockState(burnerPos).isAir(), "the Wool Burner is gone");
        h.assertTrue(player.getInventory().contains(new ItemStack(ModItems.WOOL_BURNER.get())), "and in the player's inventory");
        h.assertTrue(player.getInventory().countItem(Vanilla.wool(net.minecraft.world.item.DyeColor.WHITE).asItem()) == 5, "with the wool that was in it");
        var around = new net.minecraft.world.phys.AABB(h.absolutePos(burnerPos)).inflate(2);
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, around).isEmpty(), "nothing is left on the ground");
        h.succeed();
    }

    /** A Sheep Treadmill facing north with its back at {@code back}, and a grown sheep on its belt (or none). */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity treadmill(GameTestHelper h, BlockPos back,
                                                                                                             boolean withSheep, int wheat) {
        var block = ModBlocks.SHEEP_TREADMILL.get();
        var base = block.defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.FACING, Direction.NORTH);
        h.setBlock(back, base);
        h.setBlock(back.north(), base.setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.PART, 1));
        var be = h.getBlockEntity(back, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.class);
        if (wheat > 0) be.setItem(0, new ItemStack(Items.WHEAT, wheat));
        if (withSheep) {
            Sheep sheep = h.spawn(Vanilla.SHEEP, new net.minecraft.world.phys.Vec3(back.getX() + 0.5, back.getY() + 0.4, back.getZ() + 0.1));
            sheep.setNoAi(true);
        }
        return be;
    }

    private static Sheep sheepOn(GameTestHelper h, BlockPos back) {
        var area = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.beltArea(h.absolutePos(back), Direction.NORTH);
        return h.getLevel().getEntitiesOfClass(Sheep.class, area.inflate(0.5)).getFirst();
    }

    /**
     * A Wither Wool Bale needs more FE a tick than the separator's 1,000 FE/t input limit: the limit rises to twice what
     * the bale needs, so a Creative Generator beside it keeps it running without a pause.
     */
    private static void separatorFeedsCostlyBales(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.MECHANICAL_SEPARATOR.get());
        var sep = h.getBlockEntity(pos, MechanicalSeparatorBlockEntity.class);
        sep.getInventory().setStack(SeparatorInventory.INPUT, com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(ResourcefulSheep.id("wither"), 1));
        h.setBlock(pos.east(), ModBlocks.CREATIVE_GENERATOR.get());
        java.util.List<com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus> seen = new java.util.ArrayList<>();
        h.onEachTick(() -> seen.add(sep.getStatus()));
        h.runAfterDelay(60, () -> {
            int perTick = sep.getEnergyPerTick();
            h.assertTrue(perTick > 1_000, "a Wither bale needs more than 1,000 FE/t, needs " + perTick);
            var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, h.absolutePos(pos), Direction.UP);
            int accepted;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                accepted = handler.insert(Integer.MAX_VALUE, tx); // not committed: just how much it would take
            }
            h.assertTrue(accepted >= Math.min(2 * perTick, handler.getCapacityAsInt() - handler.getAmountAsInt()) || accepted == 0,
                    "it takes up to twice that a tick, took " + accepted);
            long waiting = seen.stream().skip(5).filter(st -> st == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus.NO_ENERGY).count();
            h.assertTrue(waiting == 0, "and never runs short of power, short " + waiting + " ticks: " + seen);
            h.assertTrue(sep.getProgress() >= 50, "it keeps going, progress " + sep.getProgress());
            h.succeed();
        });
    }

    /** A treadmill with food calls a grown sheep standing a few blocks away over to its belt, where it starts walking. */
    private static void sheepTreadmillAttracts(GameTestHelper h) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.STONE);
        var be = treadmill(h, new BlockPos(1, 1, 2), false, 4);
        Sheep sheep = h.spawn(Vanilla.SHEEP, new BlockPos(5, 1, 5));
        h.succeedWhen(() -> h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.State.WALKING,
                "the sheep came over and is walking it, state " + be.state() + ", sheep at " + h.relativePos(sheep.blockPosition())));
    }

    /**
     * The Sheep Treadmill: a sheep on the belt with wheat walks it for 20 FE/t, held in the middle of the belt; a sheared
     * one runs lighter for 25; with no wheat it's hungry and makes nothing; a worn-out sheep stops to rest; and wheat is
     * eaten as it walks.
     */
    private static void sheepTreadmillPower(GameTestHelper h) {
        BlockPos plainPos = new BlockPos(1, 1, 2), shornPos = new BlockPos(3, 1, 2), hungryPos = new BlockPos(5, 1, 2), tiredPos = new BlockPos(1, 1, 5);
        var plain = treadmill(h, plainPos, true, 1);
        var shorn = treadmill(h, shornPos, true, 1);
        var hungry = treadmill(h, hungryPos, true, 0);
        var tired = treadmill(h, tiredPos, true, 1);
        var empty = treadmill(h, new BlockPos(5, 1, 5), false, 1);
        sheepOn(h, shornPos).setSheared(true);
        // This one has nearly walked its limit.
        Sheep weary = sheepOn(h, tiredPos);
        weary.setData(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.TREADMILL_WALKER,
                new com.jax3.barelyfunctionalcontraptions.sheep.entity.TreadmillWalker(
                        com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.walkLimit(weary) - 5, 0));
        h.runAfterDelay(20, () -> {
            h.assertTrue(plain.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.State.WALKING
                    && plain.generating() == 20, "a sheep walks it for 20 FE/t, was " + plain.state() + " " + plain.generating());
            h.assertTrue(plain.getEnergyHandler().getAmountAsInt() >= 19 * 20, "and it stores what it makes, " + plain.getEnergyHandler().getAmountAsInt());
            h.assertTrue(plain.food().isEmpty(), "the sheep ate the wheat to walk");
            h.assertTrue(h.getBlockState(plainPos).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.RUNNING), "the belt runs");
            var lane = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.lane(h.absolutePos(plainPos), Direction.NORTH);
            h.assertTrue(sheepOn(h, plainPos).position().distanceTo(lane) < 0.01, "the sheep is held in the middle of the belt");
            h.assertTrue(shorn.generating() == 25, "a sheared sheep runs lighter: 25 FE/t, got " + shorn.generating());
            h.assertTrue(hungry.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.State.HUNGRY
                    && hungry.getEnergyHandler().getAmountAsInt() == 0, "without wheat it's hungry and makes nothing, was " + hungry.state());
            h.assertTrue(tired.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.State.RESTING
                    && weary.getData(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.TREADMILL_WALKER).resting(h.getLevel().getGameTime()),
                    "a worn-out sheep stops to rest, was " + tired.state());
            h.assertTrue(h.getLevel().getServer().getAdvancements().get(ResourcefulSheep.id(
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.SHEEP_TREADMILL)) != null, "its advancement, Running Ewe Ragged, is loaded");
            h.assertTrue(empty.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity.State.NO_SHEEP
                    && empty.food().getCount() == 1, "with no sheep nothing happens and no wheat is eaten");
            h.succeed();
        });
    }

    /**
     * Woollen Armour takes the colour of its wool: all red gives red, red and blue blend, and every piece crafts from
     * its leather shape. Dyeing it afterwards works like leather.
     */
    private static void woollenArmorColour(GameTestHelper h) {
        ItemStack red = new ItemStack(Vanilla.wool(net.minecraft.world.item.DyeColor.RED)), blue = new ItemStack(Vanilla.wool(net.minecraft.world.item.DyeColor.BLUE)), e = ItemStack.EMPTY;
        ItemStack helmet = craft(h, 3, 2, List.of(red, red, red, red, e, red));
        h.assertTrue(helmet.is(ModItems.WOOLLEN_HELMET.get()), "five wool make a Woollen Beanie, got " + helmet);
        int allRed = net.minecraft.world.item.component.DyedItemColor.applyDyes((net.minecraft.world.item.component.DyedItemColor) null,
                java.util.Collections.nCopies(5, net.minecraft.world.item.DyeColor.RED)).rgb();
        var colour = helmet.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        h.assertTrue(colour != null && colour.rgb() == allRed, "red wool gives a red beanie, got " + colour);
        ItemStack mixed = craft(h, 3, 2, List.of(red, blue, red, blue, e, red));
        var mixedColour = mixed.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        h.assertTrue(mixedColour != null && mixedColour.rgb() != allRed, "red and blue wool blend, got " + mixedColour);
        ItemStack w = new ItemStack(Vanilla.wool(net.minecraft.world.item.DyeColor.WHITE));
        h.assertTrue(craft(h, 3, 3, List.of(w, e, w, w, w, w, w, w, w)).is(ModItems.WOOLLEN_CHESTPLATE.get()), "eight wool make a jumper");
        h.assertTrue(craft(h, 3, 3, List.of(w, w, w, w, e, w, w, e, w)).is(ModItems.WOOLLEN_LEGGINGS.get()), "seven wool make leggings");
        h.assertTrue(craft(h, 3, 2, List.of(w, e, w, w, e, w)).is(ModItems.WOOLLEN_BOOTS.get()), "four wool make boots");
        ItemStack dyed = craft(h, 2, 1, List.of(helmet, new ItemStack(Vanilla.dye(net.minecraft.world.item.DyeColor.YELLOW))));
        var dyedColour = dyed.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        h.assertTrue(dyed.is(ModItems.WOOLLEN_HELMET.get()) && dyedColour != null && dyedColour.rgb() != allRed, "it can be dyed like leather, got " + dyedColour);
        h.assertTrue(helmet.is(net.minecraft.tags.ItemTags.HEAD_ARMOR) && ModItems.WOOLLEN_BOOTS.get().getDefaultInstance().is(net.minecraft.tags.ItemTags.FREEZE_IMMUNE_WEARABLES),
                "it's armour, and wool keeps you warm in powder snow");
        // A little better than leather: more armour on the jumper and leggings, and more durability.
        java.util.function.ToDoubleFunction<net.minecraft.world.item.Item> armour = item -> item.getDefaultInstance()
                .getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY)
                .compute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0, net.minecraft.world.entity.EquipmentSlot.CHEST);
        h.assertTrue(armour.applyAsDouble(ModItems.WOOLLEN_CHESTPLATE.get()) > armour.applyAsDouble(Items.LEATHER_CHESTPLATE), "the jumper protects more than a leather tunic");
        h.assertTrue(ModItems.WOOLLEN_BOOTS.get().getDefaultInstance().getMaxDamage() > Items.LEATHER_BOOTS.getDefaultInstance().getMaxDamage(), "and it lasts longer");
        h.succeed();
    }

    /**
     * Woollen Boots give Muffled: a husk wearing them makes no sounds (positioned or from the entity) and no
     * vibrations, while one without them does; take them off and Muffled wears off.
     */
    private static void woollenBootsMuffle(GameTestHelper h) {
        var quiet = h.spawn(Vanilla.HUSK, new BlockPos(1, 1, 1));
        var loud = h.spawn(Vanilla.HUSK, new BlockPos(5, 1, 5));
        quiet.setNoAi(true);
        loud.setNoAi(true);
        quiet.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.WOOLLEN_BOOTS.get()));
        var step = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.HUSK_STEP);
        java.util.function.Function<net.minecraft.world.entity.LivingEntity, Boolean> silencedAtFeet = e -> net.neoforged.neoforge.event.EventHooks.onPlaySoundAtPosition(
                h.getLevel(), e.getX(), e.getY(), e.getZ(), step, net.minecraft.sounds.SoundSource.HOSTILE, 1, 1).isCanceled();
        java.util.function.Function<net.minecraft.world.entity.LivingEntity, Boolean> silencedFromIt = e -> net.neoforged.neoforge.event.EventHooks.onPlaySoundAtEntity(
                e, step, net.minecraft.sounds.SoundSource.HOSTILE, 1, 1).isCanceled();
        java.util.function.Function<net.minecraft.world.entity.LivingEntity, Boolean> vibrates = e -> net.neoforged.neoforge.common.CommonHooks.onVanillaGameEvent(
                h.getLevel(), net.minecraft.world.level.gameevent.GameEvent.STEP, e.position(), net.minecraft.world.level.gameevent.GameEvent.Context.of(e));
        h.runAfterDelay(15, () -> {
            h.assertTrue(quiet.hasEffect(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModEffects.MUFFLED), "the boots make it Muffled");
            h.assertFalse(loud.hasEffect(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModEffects.MUFFLED), "the other one isn't");
            h.assertTrue(silencedAtFeet.apply(quiet) && silencedFromIt.apply(quiet), "its sounds are cancelled");
            h.assertFalse(vibrates.apply(quiet), "and its steps don't reach sculk");
            h.assertFalse(silencedAtFeet.apply(loud) || silencedFromIt.apply(loud), "the other one's sounds play");
            h.assertTrue(vibrates.apply(loud), "and it sets off sculk");
            var away = h.absolutePos(new BlockPos(3, 1, 3));
            h.assertFalse(net.neoforged.neoforge.event.EventHooks.onPlaySoundAtPosition(h.getLevel(), away.getX() + 0.5, away.getY(), away.getZ() + 0.5,
                    step, net.minecraft.sounds.SoundSource.BLOCKS, 1, 1).isCanceled(), "sounds elsewhere still play");
            quiet.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, ItemStack.EMPTY);
        });
        h.runAfterDelay(70, () -> {
            h.assertFalse(quiet.hasEffect(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModEffects.MUFFLED), "without the boots Muffled wears off");
            h.assertFalse(silencedAtFeet.apply(quiet), "and it makes sounds again");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Thermal Reaction Chamber

    private static final BlockPos TRC = new BlockPos(3, 1, 3);

    /**
     * A Thermal Reaction Chamber facing north in the middle of the platform with water, and Blaze sheep on its hot
     * (east) side and Snow sheep on its cold (west) side, one and two blocks out.
     */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity chamber(GameTestHelper h, int blaze, int snow, int buckets) {
        h.setBlock(TRC, ModBlocks.THERMAL_REACTION_CHAMBER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ThermalReactionChamberBlock.FACING, Direction.NORTH));
        var be = h.getBlockEntity(TRC, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.class);
        for (int i = 0; i < buckets; i++) be.fillFromBucket();
        for (int i = 0; i < blaze; i++) sheepAt(h, new BlockPos(4 + i % 2, 1, 1 + i / 2 * 2 + i % 2), ResourcefulSheep.id("blaze"));
        for (int i = 0; i < snow; i++) sheepAt(h, new BlockPos(2 - i % 2, 1, 1 + i / 2 * 2 + i % 2), ResourcefulSheep.id("snow"));
        return be;
    }

    /** Keeps its tank topped up through its fluid capability, as a Fluid Reservoir would. */
    private static void keepWatered(GameTestHelper h) {
        h.onEachTick(() -> {
            var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, h.absolutePos(TRC), Direction.SOUTH);
            if (handler == null) return;
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                handler.insert(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.WATER, 1000, tx);
                tx.commit();
            }
        });
    }

    /**
     * Two balanced pairs make the falloff's power for two pairs and use water; a Blaze Rod and Blue Ice burning
     * together double it; the power can be taken out of any face and water can't.
     */
    private static void thermalPower(GameTestHelper h) {
        var type = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.class;
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.powerFrom(1) == 2500, "one pair makes 2,500 FE/t");
        int two = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.powerFrom(2);
        int six = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.powerFrom(6);
        h.assertTrue(two > 2500 && two < 5000 && six >= 9000 && six <= 11000, "more pairs add less each: two " + two + ", six " + six);
        var be = chamber(h, 2, 2, 16);
        h.runAfterDelay(30, () -> {
            h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State.REACTING, "two pairs react, state " + be.state());
            h.assertTrue(be.pairs() == 2 && be.balance() == 0, "it counts two balanced pairs, got " + be.pairs() + " / " + be.balance());
            h.assertTrue(be.generating() == two, "two pairs make " + two + " FE/t, got " + be.generating());
            h.assertTrue(be.getTank().getAmount() < 16_000, "it uses water");
            h.assertTrue(be.getEnergyHandler().getAmountAsInt() > 0, "and stores power");
            h.assertTrue(h.getBlockState(TRC).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ThermalReactionChamberBlock.ACTIVE), "its vents glow");
            var energy = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, h.absolutePos(TRC), Direction.UP);
            var fluid = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, h.absolutePos(TRC), Direction.SOUTH);
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                h.assertTrue(energy.extract(1000, tx) == 1000, "power comes out");
                h.assertTrue(energy.insert(1000, tx) == 0, "and none goes in");
                h.assertTrue(fluid.extract(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.WATER, 1000, tx) == 0, "water doesn't come out");
            }
            be.setItem(0, new ItemStack(Items.BLAZE_ROD));
            be.setItem(1, new ItemStack(Items.BLUE_ICE));
            h.assertTrue(!be.canPlaceItem(0, new ItemStack(Items.BLUE_ICE)) && !be.canPlaceItem(1, new ItemStack(Items.BLAZE_ROD)), "each boost has its own slot");
        });
        h.runAfterDelay(40, () -> {
            h.assertTrue(be.getItem(0).isEmpty() && be.getItem(1).isEmpty(), "both boosts are lit");
            h.assertTrue(be.balance() == 0, "burning together they keep the balance");
            h.assertTrue(be.generating() == two * 2, "and double the power: " + be.generating());
            h.succeed();
        });
    }

    /** One pair with a bucket of water reacts until it runs dry, then stops and scalds a sheep standing in front. */
    private static void thermalDry(GameTestHelper h) {
        var be = chamber(h, 1, 1, 1);
        Sheep bystander = sheepAt(h, new BlockPos(3, 1, 2), null);
        float health = bystander.getHealth();
        h.runAfterDelay(60, () -> {
            h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State.NO_WATER, "it stops without water, state " + be.state());
            h.assertTrue(be.generating() == 0, "and makes nothing");
            h.assertTrue(bystander.getHealth() < health, "its steam scalds whatever is in front of it");
            h.succeed();
        });
    }

    /**
     * A Snow sheep whose fleece is spent doesn't count, so the chamber waits for a partner for its Blaze sheep; infusing
     * it again renews the fleece and it reacts.
     */
    private static void thermalFatigue(GameTestHelper h) {
        var be = chamber(h, 1, 0, 16);
        Sheep snow = sheepAt(h, new BlockPos(2, 1, 3), ResourcefulSheep.id("snow"));
        snow.setData(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.THERMAL_FATIGUE,
                new com.jax3.barelyfunctionalcontraptions.sheep.entity.ThermalFatigue(ModConfigs.getInt(ModConfigs.SERVER.thermalFatigueTicks)));
        h.runAfterDelay(25, () -> {
            h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State.NEEDS_HERDS,
                    "a spent sheep doesn't count, state " + be.state());
            h.assertTrue(be.spent() == 1, "it notices the spent sheep");
            h.assertTrue(be.instability() == 0, "and a lone Blaze sheep can't overheat it");
            var resource = SheepResourceRegistry.server().byId(ResourcefulSheep.id("snow")).orElseThrow();
            h.assertTrue(SheepInfusionEvents.checkSheep(snow, resource, false) == null, "a spent sheep can be infused again");
            SheepInfusionEvents.infuseFromAltar(h.getLevel(), snow, resource);
            h.assertTrue(!com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.isSpent(snow), "which renews its fleece");
        });
        h.runAfterDelay(50, () -> {
            h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State.REACTING, "then it reacts, state " + be.state());
            h.assertTrue(snow.getData(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.THERMAL_FATIGUE).ticks() > 0, "and the fleece wears again");
            h.succeed();
        });
    }

    /** The wrench is in c:tools, c:tools/wrench (so other mods treat it as a wrench) and c:storable_item. */
    private static void wrenchTags(GameTestHelper h) {
        ItemStack wrench = new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get());
        for (String tag : new String[]{"tools", "tools/wrench", "storable_item"}) {
            var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath("c", tag));
            h.assertTrue(wrench.is(key), "the wrench is in #c:" + tag);
        }
        h.succeed();
    }

    /** A Snow sheep a few blocks outside a chiller's area walks back into it and is counted. */
    private static void chillerAttracts(GameTestHelper h) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.STONE);
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.CHILLER.get());
        var be = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity.class);
        Sheep stray = sheepAt(h, new BlockPos(5, 1, 5), ResourcefulSheep.id("snow"));
        stray.setNoAi(false);
        stray.goalSelector.removeAllGoals(goal -> true); // no wandering off the platform: only the chiller moves it
        var area = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity.chillingArea(h.absolutePos(pos));
        h.assertFalse(area.contains(stray.position()), "the sheep starts outside the area");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepAttraction.reach(area).contains(stray.position()), "but within the attraction area");
        h.succeedWhen(() -> h.assertTrue(be.coldSourceCount() == 1, "it walks back in and is counted, got " + be.coldSourceCount()));
    }

    /**
     * Two Blaze sheep against one Snow sheep: the chamber calls the Snow sheep standing just outside its cold pen in,
     * evening the herds.
     */
    private static void thermalAttracts(GameTestHelper h) {
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(6, 0, 6))) h.setBlock(p, Blocks.STONE);
        var be = chamber(h, 2, 1, 16);
        Sheep stray = sheepAt(h, new BlockPos(1, 1, 0), ResourcefulSheep.id("snow"));
        stray.setNoAi(false);
        stray.goalSelector.removeAllGoals(goal -> true); // only the chamber moves it
        h.succeedWhen(() -> {
            h.assertTrue(be.coldCount() == 2 && be.hotCount() == 2, "the stray Snow sheep joins its pen, got " + be.hotCount() + " / " + be.coldCount());
            h.assertTrue(be.balance() == 0 || be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State.NO_WATER,
                    "and the herds are even, balance " + be.balance());
        });
    }

    /**
     * With its buffer full and nothing taking its power, a chamber makes nothing and uses no water, but still follows
     * its herds: two Blaze sheep against one Snow sheep move the needle and fill the danger bar.
     */
    private static void thermalFull(GameTestHelper h) {
        var be = chamber(h, 2, 1, 16);
        be.fillBuffer();
        int water = be.getTank().getAmount();
        h.runAfterDelay(60, () -> {
            h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State.FULL, "its buffer is full, state " + be.state());
            h.assertTrue(be.generating() == 0 && be.getTank().getAmount() == water, "so it makes nothing and uses no water");
            h.assertTrue(be.balance() == 1, "but the needle follows the herds: one step hot, got " + be.balance());
            h.assertTrue(be.instability() > 0 && be.comparatorSignal() > 0, "and the danger still builds, " + be.instability());
            h.succeed();
        });
    }

    /** Two Blaze sheep against one Snow sheep run it hot: power drops, a comparator reads the danger, and it melts down. */
    private static void thermalMeltdown(GameTestHelper h) {
        var be = chamber(h, 2, 1, 16);
        keepWatered(h);
        Sheep blaze = h.getLevel().getEntitiesOfClass(Sheep.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(4, 1, 1))).inflate(0.5)).getFirst();
        h.runAfterDelay(100, () -> {
            h.assertTrue(be.state() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.State.RUNNING_HOT, "it runs hot, state " + be.state());
            h.assertTrue(be.balance() == 1, "one step hot, got " + be.balance());
            h.assertTrue(be.generating() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.power(1, false, 1)
                    && be.generating() < 2500, "and loses a quarter of its power: " + be.generating());
            h.assertTrue(be.comparatorSignal() > 0, "a comparator reads the danger");
        });
        h.succeedWhen(() -> {
            h.assertBlockNotPresent(ModBlocks.THERMAL_REACTION_CHAMBER.get(), TRC);
            h.assertTrue(blaze.isSheared(), "the hot side's sheep are singed bare");
        });
    }

    /** One Blaze sheep against two Snow sheep run it cold until it freezes solid; mining the ice away thaws it. */
    private static void thermalFreeze(GameTestHelper h) {
        var be = chamber(h, 1, 2, 16);
        keepWatered(h);
        boolean[] thawed = {false};
        h.onEachTick(() -> {
            if (be.frozen() && !thawed[0]) {
                h.assertTrue(h.getBlockState(TRC.above()).is(Blocks.PACKED_ICE), "it encases itself in Packed Ice");
                h.assertTrue(h.getBlockState(TRC).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ThermalReactionChamberBlock.FROZEN), "and frosts over");
                h.assertTrue(be.generating() == 0, "frozen, it makes nothing");
                thawed[0] = true;
                for (Direction dir : Direction.values()) {
                    if (h.getBlockState(TRC.relative(dir)).is(Blocks.PACKED_ICE)) h.setBlock(TRC.relative(dir), Blocks.AIR);
                }
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(thawed[0], "it froze");
            h.assertTrue(!be.frozen(), "with the ice gone it thaws");
        });
    }

    /**
     * The Snow Sheep Chiller freezes water into Ice with the Snow sheep around it: two of them make Ice from its water,
     * a bucket each; one with only a sheared one stays idle; pipes fill it with water and can't empty it.
     */
    private static void chiller(GameTestHelper h) {
        Identifier snowId = ResourcefulSheep.id("snow");
        var chillerBlock = ModBlocks.CHILLER.get();
        // A chiller with two Snow sheep and three buckets of water.
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, chillerBlock);
        var be = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity.class);
        for (int i = 0; i < 3; i++) h.assertTrue(be.fillFromBucket(), "a bucket goes in");
        sheepAt(h, new BlockPos(1, 1, 2), snowId);
        sheepAt(h, new BlockPos(3, 1, 2), snowId);
        // One with only a sheared Snow sheep and water: too warm.
        BlockPos warm = new BlockPos(6, 1, 6);
        h.setBlock(warm, chillerBlock);
        var warmBe = h.getBlockEntity(warm, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity.class);
        warmBe.fillFromBucket();
        Sheep sheared = sheepAt(h, new BlockPos(6, 1, 5), snowId);
        sheared.setSheared(true);
        // Pipes and a Fluid Reservoir fill it through its fluid capability: water goes in, nothing comes out, lava doesn't fit.
        BlockPos fed = new BlockPos(6, 1, 1);
        h.setBlock(fed, chillerBlock);
        var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, h.absolutePos(fed), net.minecraft.core.Direction.UP);
        h.assertTrue(handler != null, "it has a fluid handler");
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            h.assertTrue(handler.insert(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity.WATER, 2000, tx) == 2000, "water goes in");
            h.assertTrue(handler.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA), 1000, tx) == 0, "lava doesn't");
            h.assertTrue(handler.extract(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity.WATER, 1000, tx) == 0, "and water doesn't come out");
            tx.commit();
        }
        var fedBe = h.getBlockEntity(fed, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity.class);
        h.runAfterDelay(260, () -> {
            int ice = be.output().is(Items.ICE) ? be.output().getCount() : 0;
            h.assertTrue(ice >= 2, "two Snow sheep freeze Ice, got " + be.output());
            h.assertTrue(be.getTank().getAmount() == 3000 - ice * 1000, "a bucket of water each, left " + be.getTank().getAmount());
            h.assertTrue(be.coldSourceCount() == 2, "it counts the two Snow sheep, got " + be.coldSourceCount());
            h.assertTrue(warmBe.output().isEmpty() && warmBe.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus.IDLE,
                    "a sheared Snow sheep gives no cold, status " + warmBe.status());
            h.assertTrue(fedBe.getTank().getAmount() == 2000, "the piped water stays, with no sheep to freeze it: " + fedBe.getTank().getAmount());
            h.assertTrue(be.comparatorSignal() > 0, "a comparator reads the Ice");
            h.succeed();
        });
    }

    /**
     * A Snow sheep is infused at the altar with a Snow Block (2 more and a Wheat round it), and leaves snow where it
     * walks while its fleece is on; a sheared one, or a plain sheep, leaves none.
     */
    private static void snowSheep(GameTestHelper h) {
        Identifier snowId = ResourcefulSheep.id("snow");
        var snow = SheepResourceRegistry.server().byId(snowId).orElseThrow();
        h.assertTrue(snow.trailBlock().map(Blocks.SNOW::equals).orElse(false), "the Snow sheep's trail is snow");
        h.assertTrue(snow.feedingItem().test(new ItemStack(Items.SNOW_BLOCK)) && snow.altar().catalysts().size() == 2, "fed Snow Blocks at the altar");
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(8, 0, 6))) h.setBlock(p, Blocks.STONE);
        Sheep walker = sheepAt(h, new BlockPos(1, 1, 1), snowId);
        walker.setNoAi(false);
        Sheep sheared = sheepAt(h, new BlockPos(1, 1, 3), snowId);
        sheared.setNoAi(false);
        sheared.setSheared(true);
        Sheep plain = sheepAt(h, new BlockPos(1, 1, 5), null);
        plain.setNoAi(false);
        h.onEachTick(() -> {
            walker.getNavigation().moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(7.5, 1, 1.5)).x, h.absolutePos(new BlockPos(0, 1, 0)).getY(), h.absoluteVec(new net.minecraft.world.phys.Vec3(7.5, 1, 1.5)).z, 1.0);
            sheared.getNavigation().moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(7.5, 1, 3.5)).x, h.absolutePos(new BlockPos(0, 1, 0)).getY(), h.absoluteVec(new net.minecraft.world.phys.Vec3(7.5, 1, 3.5)).z, 1.0);
            plain.getNavigation().moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(7.5, 1, 5.5)).x, h.absolutePos(new BlockPos(0, 1, 0)).getY(), h.absoluteVec(new net.minecraft.world.phys.Vec3(7.5, 1, 5.5)).z, 1.0);
        });
        h.runAfterDelay(100, () -> {
            int[] rows = new int[7];
            for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(8, 1, 6))) {
                if (h.getBlockState(p).is(Blocks.SNOW)) rows[p.getZ()]++;
            }
            ResourcefulSheep.LOGGER.info("[snow test] snow per row {} walker at {}", java.util.Arrays.toString(rows), h.relativePos(walker.blockPosition()));
            h.assertTrue(rows[0] + rows[1] + rows[2] >= 3, "the Snow sheep leaves a trail of snow, rows " + java.util.Arrays.toString(rows));
            h.assertTrue(rows[3] + rows[4] + rows[5] + rows[6] == 0, "a sheared Snow sheep and a plain sheep leave none, rows " + java.util.Arrays.toString(rows));
            h.succeed();
        });
    }

    /** {@code /barelyfunctionalcontraptions list} prints a header and one line per loaded sheep. */
    private static void listCommand(GameTestHelper h) {
        var server = h.getLevel().getServer();
        List<String> lines = new java.util.ArrayList<>();
        net.minecraft.commands.CommandSource collector = new net.minecraft.commands.CommandSource() {
            @Override public void sendSystemMessage(net.minecraft.network.chat.Component message) { lines.add(message.getString()); }
            @Override public boolean acceptsSuccess() { return true; }
            @Override public boolean acceptsFailure() { return true; }
            @Override public boolean shouldInformAdmins() { return false; }
        };
        var source = server.createCommandSourceStack().withSource(collector);
        server.getCommands().performPrefixedCommand(source, "barelyfunctionalcontraptions list");
        ResourcefulSheep.LOGGER.info("[list test] {}", lines);
        int loaded = SheepResourceRegistry.server().all().size();
        h.assertTrue(lines.size() == loaded + 1, "a header and " + loaded + " sheep, got " + lines);
        h.succeed();
    }

    /**
     * The altar gives one redstone-tick pulse (2 game ticks) when its items become complete: nothing while a catalyst
     * is missing, one pulse when the last one goes in, and no more while it stays complete.
     */
    private static void altarPulse(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        var be = altar(h, pos, false, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_BLOCK, 2));
        int[] poweredTicks = {0};
        boolean[] lampLit = {false};
        h.setBlock(pos.east(), Blocks.REDSTONE_LAMP);
        h.onEachTick(() -> {
            if (h.getBlockState(pos).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.InfusionAltarBlock.POWERED)) {
                poweredTicks[0]++;
                if (h.getLevel().hasNeighborSignal(h.absolutePos(pos.east()))) lampLit[0] = true;
            }
        });
        h.runAfterDelay(10, () -> {
            h.assertTrue(poweredTicks[0] == 0, "no pulse while the Golden Carrot is missing, got " + poweredTicks[0]);
            be.setItem(2, new ItemStack(Items.GOLDEN_CARROT));
        });
        h.runAfterDelay(40, () -> {
            h.assertTrue(poweredTicks[0] == 2, "one redstone tick (2 game ticks) of power once it's complete, got " + poweredTicks[0]);
            h.assertTrue(lampLit[0], "the pulse reaches the block beside it");
            h.assertFalse(h.getBlockState(pos).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.InfusionAltarBlock.POWERED), "and then it's off");
            h.succeed();
        });
    }

    /**
     * It waits for catalysts, a sheep, power and enough items; tier 1 Iron takes 2 Blocks of Iron and a Golden Carrot
     * round the ingot; Endurance still counts. By default no sheep can be fed by hand, Iron included.
     */
    private static void altarWaits(GameTestHelper h) {
        // Missing a catalyst.
        var noRose = altar(h, new BlockPos(1, 1, 1), true, new ItemStack(Items.NETHER_STAR), new ItemStack(Items.SOUL_SAND, 4), new ItemStack(Items.BONE_BLOCK, 2));
        sheepAt(h, new BlockPos(1, 1, 2), null);
        // No sheep.
        var alone = altar(h, new BlockPos(5, 1, 1), true, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_BLOCK, 2), new ItemStack(Items.GOLDEN_CARROT));
        // No power.
        var unpowered = altar(h, new BlockPos(1, 1, 5), false, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_BLOCK, 2), new ItemStack(Items.GOLDEN_CARROT));
        sheepAt(h, new BlockPos(1, 1, 6), null);
        // Tier 1 Iron at a powered altar, an already-Gold sheep next to it is skipped for a plain Endurance-10 one.
        var iron = altar(h, new BlockPos(5, 1, 5), true, new ItemStack(Items.IRON_INGOT, 3), new ItemStack(Items.IRON_BLOCK, 2), new ItemStack(Items.GOLDEN_CARROT));
        // Without its catalysts, Iron waits too.
        var noBlocks = altar(h, new BlockPos(3, 1, 3), true, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLDEN_CARROT));
        // And it can't be fed by hand any more.
        Sheep handFed = sheepAt(h, new BlockPos(7, 1, 1), null);
        Player feeder = playerHolding(h, new ItemStack(Items.IRON_INGOT));
        feeder.interactOn(handFed, InteractionHand.MAIN_HAND, handFed.position());
        h.assertFalse(SheepInfusionEvents.getInfusion(handFed).isInfused(), "an Iron Ingot can't be fed by hand");
        h.assertTrue(feeder.getMainHandItem().getCount() == 1, "and isn't used");
        sheepAt(h, new BlockPos(5, 1, 6), ResourcefulSheep.id("gold"));
        Sheep tough = sheepAt(h, new BlockPos(6, 1, 5), null);
        SheepInfusionEvents.setStats(tough, new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(1, 1, 10));
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity.isCatalyst(h.getLevel(), new ItemStack(Items.SOUL_SAND)), "soul sand is a catalyst");
        // Its area outline: off at first, toggled like the shears' and furnace's, and the 5x5x3 box the sheep search uses.
        h.assertFalse(iron.showsArea(), "the area outline starts hidden");
        h.assertTrue(iron.toggleArea() && iron.showsArea(), "and toggles on");
        var box = iron.displayedArea();
        h.assertTrue(box.getXsize() == 5 && box.getZsize() == 5 && box.getYsize() == 3 && box.contains(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(6, 1, 5)))),
                "it covers 5x5 columns, 3 high, round the altar, was " + box);
        h.assertFalse(iron.canPlaceItem(0, new ItemStack(Items.DIRT)), "dirt isn't a feeding item");
        h.runAfterDelay(130, () -> {
            h.assertTrue(noRose.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AltarStatus.MISSING_CATALYSTS, "waits for the Wither Rose, was " + noRose.status());
            h.assertTrue(alone.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AltarStatus.NO_SHEEP, "waits for a sheep, was " + alone.status());
            h.assertTrue(unpowered.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AltarStatus.NO_ENERGY, "waits for power, was " + unpowered.status());
            h.assertTrue(noRose.getItem(0).getCount() == 1 && noRose.getItem(1).getCount() == 4, "nothing is used while it waits");
            h.assertTrue(SheepInfusionEvents.getInfusion(tough).resource().map(IRON::equals).orElse(false), "tier 1 Iron is infused with its catalysts");
            h.assertTrue(iron.getItem(1).isEmpty() && iron.getItem(2).isEmpty(), "the 2 Blocks of Iron and the Golden Carrot are used");
            h.assertTrue(noBlocks.status() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AltarStatus.MISSING_CATALYSTS, "Iron waits for its blocks, was " + noBlocks.status());
            int base = SheepResourceRegistry.server().byId(IRON).orElseThrow().effectiveInfusionUses();
            h.assertTrue(SheepInfusionEvents.getInfusion(tough).usesRemaining() == Math.round(base * 1.9), "its Endurance still counts");
            h.assertTrue(iron.getItem(0).getCount() == 2, "one ingot used, then it waits: no other sheep can take Iron");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ fluid tanks

    // Made when a test runs: fluid resources can't be created while the mod is still loading.
    private static net.neoforged.neoforge.transfer.fluid.FluidResource water() {
        return net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
    }

    private static net.neoforged.neoforge.transfer.fluid.FluidResource lava() {
        return net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA);
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankBlockEntity tank(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.FLUID_TANK.get());
        return h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankBlockEntity.class);
    }

    /** Pours {@code amount} mB in through the capability, in as many transfers as the rate needs. */
    private static int pour(net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> handler,
                            net.neoforged.neoforge.transfer.fluid.FluidResource fluid, int amount) {
        int total = 0;
        while (total < amount) {
            try (Transaction tx = Transaction.openRoot()) {
                int in = handler.insert(fluid, amount - total, tx);
                tx.commit();
                if (in <= 0) break;
                total += in;
            }
        }
        return total;
    }

    private static int drain(net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> handler,
                             net.neoforged.neoforge.transfer.fluid.FluidResource fluid, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            int out = handler.extract(fluid, amount, tx);
            tx.commit();
            return out;
        }
    }

    /** One tank: 16,000 mB of one fluid, 1,000 mB per transfer, in and out on any face; other fluids are refused. */
    private static void tankSingle(GameTestHelper h) {
        var t = tank(h, new BlockPos(2, 1, 2));
        var face = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, h.absolutePos(new BlockPos(2, 1, 2)), Direction.NORTH);
        h.assertTrue(face != null, "fluid capability on every face");
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(face.insert(water(), 5_000, tx) == 1_000, "one transfer moves at most 1,000 mB");
        }
        h.assertTrue(pour(face, water(), 20_000) == 16_000, "a tank holds 16,000 mB");
        h.assertTrue(face.getAmountAsLong(0) == 16_000 && face.getResource(0).equals(water()), "full of water");
        h.assertTrue(pour(face, lava(), 1_000) == 0, "lava is refused while it holds water");
        h.assertTrue(drain(face, water(), 600) == 600 && t.getFluid().getAmount() == 15_400, "water comes back out");
        h.assertTrue(drain(face, lava(), 600) == 0, "nothing comes out for the wrong fluid");
        h.succeed();
    }

    /**
     * Two tanks side by side with a third on top: one group of 3 (48,000 mB, 3,000 mB/t). 20,000 mB settles into the
     * bottom layer, split evenly; 40,000 fills the bottom layer and puts the rest on top.
     */
    private static void tankGroup(GameTestHelper h) {
        var a = tank(h, new BlockPos(1, 1, 2));
        var b = tank(h, new BlockPos(2, 1, 2));
        var top = tank(h, new BlockPos(2, 2, 2));
        var group = top.group();
        h.assertTrue(group == a.group() && group.size() == 3, "3 tanks form one group, got " + group.size());
        h.assertTrue(group.capacity() == 48_000 && group.rate() == 3_000, "48,000 mB at 3,000 mB/t");
        h.assertTrue(pour(top.getFluidHandler(), water(), 20_000) == 20_000, "poured in through the top tank");
        h.runAfterDelay(2, () -> {
            h.assertTrue(a.getFluid().getAmount() == 10_000 && b.getFluid().getAmount() == 10_000 && top.getFluid().isEmpty(),
                    "settles into the bottom layer, evenly: " + a.getFluid().getAmount() + "/" + b.getFluid().getAmount() + "/" + top.getFluid().getAmount());
            pour(a.getFluidHandler(), water(), 20_000);
        });
        h.runAfterDelay(4, () -> {
            h.assertTrue(a.getFluid().getAmount() == 16_000 && b.getFluid().getAmount() == 16_000 && top.getFluid().getAmount() == 8_000,
                    "bottom layer full, the rest on top: " + a.getFluid().getAmount() + "/" + b.getFluid().getAmount() + "/" + top.getFluid().getAmount());
            h.assertTrue(group.amount() == 40_000, "nothing made or lost");
            h.succeed();
        });
    }

    /** Touching tanks of water and lava stay separate groups; an empty tank joins whichever it touches. */
    private static void tankFluidsApart(GameTestHelper h) {
        var water = tank(h, new BlockPos(1, 1, 2));
        pour(water.getFluidHandler(), water(), 5_000);
        var lava = tank(h, new BlockPos(3, 1, 2));
        pour(lava.getFluidHandler(), lava(), 5_000);
        var middle = tank(h, new BlockPos(2, 1, 2)); // touches both
        for (var t : List.of(water, lava, middle)) t.invalidateGroup();
        int together = (water.group() == middle.group() ? 1 : 0) + (lava.group() == middle.group() ? 1 : 0);
        h.assertTrue(water.group() != lava.group(), "water and lava never share a group");
        h.assertTrue(together == 1, "the empty tank joins exactly one of them");
        h.succeed();
    }

    /** A broken tank drops itself with its own share of fluid, and placing it again puts the fluid back. */
    private static void tankBreak(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        var t = tank(h, pos);
        pour(t.getFluidHandler(), lava(), 7_000);
        h.getLevel().destroyBlock(h.absolutePos(pos), true);
        var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(2), e -> e.getItem().is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_TANK.get()));
        h.assertTrue(drops.size() == 1, "the tank drops itself");
        ItemStack item = drops.getFirst().getItem();
        var contents = item.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.FLUID_CONTENTS.get());
        h.assertTrue(contents != null && contents.is(net.minecraft.world.level.material.Fluids.LAVA) && contents.copy().getAmount() == 7_000,
                "the item keeps 7,000 mB of lava");
        var placed = tank(h, pos);
        placed.applyComponentsFromItemStack(item);
        h.assertTrue(placed.getFluid().getAmount() == 7_000, "placed again, it holds the lava");
        h.succeed();
    }

    /** The dropped item of {@code item} lying around {@code pos}, or empty. */
    private static ItemStack droppedNear(GameTestHelper h, BlockPos pos, net.minecraft.world.item.Item item) {
        return h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(2), e -> e.getItem().is(item))
                .stream().findFirst().map(e -> e.getItem().copy()).orElse(ItemStack.EMPTY);
    }

    /**
     * Breaks the block at {@code pos} the way a player's game mode does: the block's own playerWillDestroy, then, unless
     * the player is in creative, drops if the held tool can harvest it.
     */
    private static void breakLikeAPlayer(GameTestHelper h, Player player, BlockPos pos) {
        var level = h.getLevel();
        BlockPos abs = h.absolutePos(pos);
        var state = level.getBlockState(abs);
        var be = level.getBlockEntity(abs);
        var adjusted = state.getBlock().playerWillDestroy(level, abs, state, player);
        boolean drops = !player.preventsBlockDrops() && adjusted.canHarvestBlock(level, abs, player);
        level.removeBlock(abs, false);
        if (drops) net.minecraft.world.level.block.Block.dropResources(adjusted, level, abs, be, player, player.getMainHandItem().copy()); // what playerDestroy does
    }

    /** Places {@code stack} the way a player does, by clicking the east face of the block west of {@code pos}. */
    private static void placeLikeAPlayer(GameTestHelper h, Player player, BlockPos pos, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos west = h.absolutePos(pos.west());
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(west).add(0.5, 0, 0), Direction.EAST, west, false);
        stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    /**
     * Broken by a player (survival or creative), a charged cube and a filled tank drop themselves with what they held;
     * placed back by a player next to another one, they hold it again and the group adds it up.
     */
    private static void keepContentsThroughPlayer(GameTestHelper h) {
        Player survival = playerHolding(h, new ItemStack(Items.IRON_PICKAXE));
        // Survival: a lone cube with 300,000 FE and a lone tank with 5,000 mB of lava.
        BlockPos cubePos = new BlockPos(1, 1, 1), tankPos = new BlockPos(1, 1, 4);
        cube(h, cubePos).storage().set(300_000);
        pour(tank(h, tankPos).getFluidHandler(), lava(), 5_000);
        breakLikeAPlayer(h, survival, cubePos);
        breakLikeAPlayer(h, survival, tankPos);
        ItemStack cubeItem = droppedNear(h, cubePos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE.get());
        ItemStack tankItem = droppedNear(h, tankPos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_TANK.get());
        var energy = cubeItem.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.MACHINE_CONTENTS.get());
        var fluid = tankItem.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.FLUID_CONTENTS.get());
        h.assertTrue(energy != null && energy.energy() == 300_000, "a survival-broken cube's item keeps 300,000 FE, got " + energy);
        h.assertTrue(fluid != null && fluid.copy().getAmount() == 5_000, "a survival-broken tank's item keeps 5,000 mB, got " + fluid);
        // Placed back by the player beside an empty cube / tank: the group holds it all.
        BlockPos cube2 = new BlockPos(4, 1, 1), tank2 = new BlockPos(4, 1, 4);
        var emptyCube = cube(h, cube2);
        var emptyTank = tank(h, tank2);
        placeLikeAPlayer(h, survival, cube2.east(), cubeItem);
        placeLikeAPlayer(h, survival, tank2.east(), tankItem);
        h.assertTrue(h.getBlockState(cube2.east()).is(ModBlocks.HUME_ENERGY_CUBE.get()), "the player placed the cube, found " + h.getBlockState(cube2.east())
                + " below " + h.getBlockState(cube2.east().below()));
        var placedCube = h.getBlockEntity(cube2.east(), com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity.class);
        h.assertTrue(placedCube.storage().getAmountAsInt() == 300_000, "the placed cube holds its 300,000 FE itself, has " + placedCube.storage().getAmountAsInt()
                + ", group " + placedCube.group().amount() + " of " + placedCube.group().size());
        h.assertTrue(emptyCube.group().amount() == 300_000 && emptyCube.group().size() == 2,
                "placed next to an empty cube, the group has 300,000 FE, got " + emptyCube.group().amount() + " in " + emptyCube.group().size());
        h.assertTrue(emptyTank.getFluidHandler().getAmountAsInt(0) == 5_000,
                "placed next to an empty tank, the group has 5,000 mB, got " + emptyTank.getFluidHandler().getAmountAsInt(0));
        // Creative: breaking a charged cube or a filled tank still drops it with its contents (an empty one doesn't drop).
        Player creative = h.makeMockPlayer(GameType.CREATIVE);
        GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities()); // what the game does for a real creative player
        BlockPos cCube = new BlockPos(1, 1, 7), cTank = new BlockPos(4, 1, 7), cEmpty = new BlockPos(7, 1, 7);
        cube(h, cCube).storage().set(50_000);
        pour(tank(h, cTank).getFluidHandler(), lava(), 2_000);
        cube(h, cEmpty);
        breakLikeAPlayer(h, creative, cCube);
        breakLikeAPlayer(h, creative, cTank);
        breakLikeAPlayer(h, creative, cEmpty);
        var cEnergy = droppedNear(h, cCube, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE.get())
                .get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.MACHINE_CONTENTS.get());
        var cFluid = droppedNear(h, cTank, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FLUID_TANK.get())
                .get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.FLUID_CONTENTS.get());
        h.assertTrue(cEnergy != null && cEnergy.energy() == 50_000, "a creative-broken charged cube drops with its 50,000 FE, got " + cEnergy);
        h.assertTrue(cFluid != null && cFluid.copy().getAmount() == 2_000, "a creative-broken tank drops with its 2,000 mB, got " + cFluid);
        h.assertTrue(droppedNear(h, cEmpty, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.HUME_ENERGY_CUBE.get()).isEmpty(),
                "an empty cube broken in creative drops nothing");
        h.succeed();
    }

    /** Right-clicking with a water bucket pours it in; with an empty bucket, takes it back out. */
    private static void tankBuckets(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        var t = tank(h, pos);
        Player player = playerHolding(h, new ItemStack(Items.WATER_BUCKET));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
        h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(t.getFluid().getAmount() == 1_000 && player.getMainHandItem().is(Items.BUCKET), "the bucket emptied into the tank");
        h.getLevel().getBlockState(h.absolutePos(pos)).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(t.getFluid().isEmpty() && player.getMainHandItem().is(Items.WATER_BUCKET), "an empty bucket takes it back");
        h.succeed();
    }

    // ------------------------------------------------------------------ helpers

    private static final Identifier IRON = ResourcefulSheep.id("iron");

    private static Sheep sheep(GameTestHelper h) {
        Sheep sheep = h.spawn(Vanilla.SHEEP, new BlockPos(3, 1, 3));
        sheep.setNoAi(true);
        sheep.setColor(DyeColor.WHITE);
        return sheep;
    }

    private static Player playerHolding(GameTestHelper h, ItemStack stack) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /**
     * Feeds the sheep by hand, the quick way most tests infuse a sheep. Every sheep needs the altar by default now, so
     * this allows hand feeding for tier 1 (as it used to be) for the one interaction; tier 2 still can't be fed by hand.
     */
    private static void feed(Player player, Sheep sheep) {
        var setting = com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.handFeedingMaxTier;
        int old = setting.get();
        setting.set(Math.max(old, 1));
        try {
            player.interactOn(sheep, InteractionHand.MAIN_HAND, sheep.position());
        } finally {
            setting.set(old);
        }
    }

    private static List<ItemStack> shear(GameTestHelper h, Sheep sheep) {
        IShearable shearable = sheep;
        return shearable.onSheared(null, new ItemStack(Items.SHEARS), h.getLevel(), sheep.blockPosition());
    }

    private static MechanicalSeparatorBlockEntity separator(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState());
        return h.getBlockEntity(pos, MechanicalSeparatorBlockEntity.class);
    }

    private static int insertEnergy(EnergyHandler handler, int amount) {
        int total = 0;
        while (total < amount) {
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = handler.insert(amount - total, tx);
                tx.commit();
                if (inserted <= 0) break;
                total += inserted;
            }
        }
        return total;
    }

    private static int insertItem(ResourceHandler<ItemResource> handler, ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            int n = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return n;
        }
    }

    // ------------------------------------------------------------------ sheep tests

    private static void definitionsLoaded(GameTestHelper h) {
        SheepResourceRegistry reg = SheepResourceRegistry.server();
        for (String name : List.of("iron", "gold", "copper", "diamond", "emerald", "redstone", "lapis", "coal", "netherite", "stone", "wood", "quartz")) {
            h.assertTrue(reg.byId(ResourcefulSheep.id(name)).isPresent(), "missing built-in resource " + name);
        }
        h.assertTrue(reg.byFeedingItem(new ItemStack(Items.IRON_INGOT)).map(SheepResource::id).orElse(null) != null
                && reg.byFeedingItem(new ItemStack(Items.IRON_INGOT)).get().id().equals(IRON), "iron ingot should map to iron");
        h.assertTrue(reg.byFeedingItem(new ItemStack(Items.IRON_BLOCK)).isEmpty(), "iron block is no longer a feeding item (ingots are)");
        Separation iron = reg.byId(IRON).get().separation();
        h.assertTrue(iron.primary().create().is(Items.RAW_IRON) && iron.primaryRange().equals(new com.jax3.barelyfunctionalcontraptions.sheep.resource.CountRange(2, 4)),
                "iron separates to 2-4 raw iron, got " + iron.primaryRange());
        h.assertTrue(iron.secondary().isPresent() && iron.secondary().get().create().is(Items.STRING) && iron.secondary().get().count() == 2, "iron gives 2 string");
        h.assertTrue(iron.effectiveEnergy() == 20000 && iron.effectiveTicks() == 75, "iron costs 20,000 FE over 75 ticks (100 at the x1.35 speed), got " + iron.effectiveEnergy() + "/" + iron.effectiveTicks());
        h.succeed();
    }

    private static void feedIronBlock(GameTestHelper h) {
        Sheep sheep = sheep(h);
        Player player = playerHolding(h, new ItemStack(Items.IRON_INGOT, 3));
        feed(player, sheep);
        SheepInfusion infusion = SheepInfusionEvents.getInfusion(sheep);
        h.assertTrue(infusion.resource().map(IRON::equals).orElse(false), "sheep should be infused with iron, was " + infusion);
        h.assertTrue(infusion.usesRemaining() == 100, "infusions last 100 shearings by default (infusionUsesOverride), was " + infusion.usesRemaining());
        h.assertTrue(player.getMainHandItem().getCount() == 2, "one iron ingot should be consumed");
        h.assertTrue(sheep.getColor() == DyeColor.WHITE, "dye colour must be preserved");
        // Feeding again at full uses is refused and does not consume.
        feed(player, sheep);
        h.assertTrue(player.getMainHandItem().getCount() == 2, "a fully infused sheep must not consume more items");
        h.succeed();
    }

    private static void unrecognisedItemIgnored(GameTestHelper h) {
        Sheep sheep = sheep(h);
        Player player = playerHolding(h, new ItemStack(Items.DIRT, 5));
        feed(player, sheep);
        h.assertFalse(SheepInfusionEvents.getInfusion(sheep).isInfused(), "dirt must not infuse");
        h.assertTrue(player.getMainHandItem().getCount() == 5, "dirt must not be consumed");
        h.succeed();
    }

    /** Runs the sheep's own death loot table as if a player killed it, collecting the drops. */
    private static List<ItemStack> killDrops(GameTestHelper h, Sheep sheep) {
        List<ItemStack> drops = new java.util.ArrayList<>();
        Player killer = h.makeMockPlayer(GameType.SURVIVAL);
        sheep.dropFromLootTable(h.getLevel(), h.getLevel().damageSources().playerAttack(killer), true,
                sheep.getLootTable().orElseThrow(), drops::add);
        return drops;
    }

    private static int count(List<ItemStack> drops, java.util.function.Predicate<ItemStack> filter) {
        return drops.stream().filter(filter).mapToInt(ItemStack::getCount).sum();
    }

    /** Every block in the mod places, breaks and steps with the vanilla Iron Block's sounds, except the wool bale and the crate. */
    private static void ironSounds(GameTestHelper h) {
        var ironPlace = net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState().getSoundType().getPlaceSound();
        int checked = 0;
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(ResourcefulSheep.MOD_ID)) continue;
            if (block == com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.SHEEP_GLOW.get()) continue; // invisible light, never placed by hand
            if (block == com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.WOOL_BALE.get()) continue; // wool, so it sounds like wool
            if (block == com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.CRATE.get() || block == com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.CRATE_CONTROLLER.get()) continue; // wooden crates, so they sound like wood
            var sound = block.defaultBlockState().getSoundType();
            h.assertTrue(sound == net.minecraft.world.level.block.SoundType.IRON && sound.getPlaceSound() == ironPlace,
                    id + " should use the iron block sound type");
            checked++;
        }
        h.assertTrue(checked >= 7, "expected all 7 blocks, checked " + checked);
        h.succeed();
    }

    private static void machineBlockRecipes(GameTestHelper h) {
        var recipes = h.getLevel().getServer().getRecipeManager();
        var casing = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.FUNCTIONAL_MACHINE_BLOCK.get();
        var own = recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                ResourcefulSheep.id("functional_machine_block"))).orElse(null);
        h.assertTrue(own != null && own.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe,
                "the Functional Machine Block has a shaped recipe");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.FUNCTIONAL_MACHINE_BLOCK.get().defaultBlockState()
                .is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), "mined with a pickaxe");
        for (String machine : List.of("mycelial_generator", "mechanical_separator", "automatic_shears", "fluid_reservoir", "hume_energy_cube", "fluid_tank", "zero_point_energy_module", "blaze_furnace", "wool_burner", "wool_packer", "infusion_altar")) {
            var holder = recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    ResourcefulSheep.id(machine))).orElse(null);
            h.assertTrue(holder != null && holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe, "missing recipe " + machine);
            var shaped = (net.minecraft.world.item.crafting.ShapedRecipe) holder.value();
            @SuppressWarnings("deprecation")
            long uses = shaped.getIngredients().stream()
                    .filter(i -> i.isPresent() && i.get().items().anyMatch(it -> it.value() == casing)).count();
            h.assertTrue(uses == 1, machine + " should take exactly one Functional Machine Block, takes " + uses);
        }
        h.succeed();
    }

    private static void killInfused(GameTestHelper h) {
        java.util.function.Predicate<ItemStack> ironWool = st -> IRON.equals(ResourceWoolItem.getResourceId(st));
        // Infused with its wool: shearing's 1 Iron Wool + 1, plus one separator roll of Raw Iron (2-4), no white wool.
        Sheep infused = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), infused);
        List<ItemStack> drops = killDrops(h, infused);
        h.assertTrue(count(drops, ironWool) == 2, "killed iron sheep drops 2 iron wool (1 more than shearing): " + drops);
        int raw = count(drops, st -> st.is(Items.RAW_IRON));
        h.assertTrue(raw >= 2 && raw <= 4, "killed iron sheep drops 2-4 raw iron: " + drops);
        h.assertTrue(count(drops, st -> st.is(Vanilla.wool(DyeColor.WHITE))) == 0, "no plain wool from an infused sheep: " + drops);
        h.assertTrue(count(drops, st -> st.is(Items.MUTTON) || st.is(Items.COOKED_MUTTON)) > 0, "mutton still drops: " + drops);
        h.assertFalse(SheepInfusionEvents.getInfusion(infused).isInfused(), "the infusion is used up by the kill");

        // Already sheared: nothing extra, so shearing and then killing does not pay twice.
        Sheep sheared = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), sheared);
        shear(h, sheared);
        List<ItemStack> shearedDrops = killDrops(h, sheared);
        h.assertTrue(count(shearedDrops, ironWool) == 0 && count(shearedDrops, st -> st.is(Items.RAW_IRON)) == 0,
                "a sheared infused sheep drops only its normal loot: " + shearedDrops);

        // A plain sheep keeps its vanilla wool.
        List<ItemStack> plain = killDrops(h, sheep(h));
        h.assertTrue(count(plain, st -> st.is(Vanilla.wool(DyeColor.WHITE))) == 1 && count(plain, ironWool) == 0, "plain sheep drops white wool: " + plain);
        h.succeed();
    }

    private static void shearInfused(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), sheep);
        List<ItemStack> drops = shear(h, sheep);
        h.assertTrue(sheep.isSheared(), "sheep should be sheared");
        h.assertTrue(drops.stream().noneMatch(s -> s.is(Vanilla.wool(DyeColor.WHITE))), "infused sheep must not drop normal wool: " + drops);
        long ironWool = drops.stream().filter(s -> IRON.equals(ResourceWoolItem.getResourceId(s))).mapToInt(ItemStack::getCount).sum();
        h.assertTrue(ironWool == 1, "expected exactly 1 iron wool, got " + drops);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).usesRemaining() == 99, "one use consumed");
        // Already sheared: a second shear attempt does nothing (no duplication).
        h.assertFalse(((IShearable) sheep).isShearable(null, new ItemStack(Items.SHEARS), h.getLevel(), sheep.blockPosition()), "sheared sheep not shearable");
        h.succeed();
    }

    /**
     * Regrowing wool by eating grass keeps the infusion: shear an iron sheep, let vanilla's own grass-eating goal
     * (EatBlockGoal) eat a real grass block, then shear again and get Iron Wool again.
     */
    private static void infusionSurvivesEatingGrass(GameTestHelper h) {
        Sheep sheep = sheep(h);
        BlockPos below = new BlockPos(3, 0, 3);
        h.setBlock(below, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), sheep);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).usesRemaining() == 100, "100 uses by default");

        List<ItemStack> first = shear(h, sheep);
        h.assertTrue(sheep.isSheared(), "sheep should be sheared");
        h.assertTrue(first.stream().anyMatch(s -> IRON.equals(ResourceWoolItem.getResourceId(s))), "first shearing drops iron wool: " + first);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).usesRemaining() == 99, "one use consumed");

        // Run the sheep's real grass-eating goal to completion (the same code that runs when a sheep grazes).
        var eat = sheep.goalSelector.getAvailableGoals().stream()
                .map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                .filter(g -> g instanceof net.minecraft.world.entity.ai.goal.EatBlockGoal)
                .findFirst().orElseThrow(() -> h.assertionException("sheep has no EatBlockGoal"));
        eat.start();
        for (int i = 0; i < 60 && sheep.isSheared(); i++) eat.tick();
        eat.stop();
        h.assertFalse(sheep.isSheared(), "eating grass should regrow the wool");
        h.assertBlockPresent(net.minecraft.world.level.block.Blocks.DIRT, below);

        // Still the same infusion with the same uses left, and it still renders as infused.
        h.assertTrue(SheepInfusionEvents.getInfusedResource(sheep).map(r -> r.id().equals(IRON)).orElse(false),
                "still infused with iron after eating grass");
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).usesRemaining() == 99, "eating grass must not change the uses left");

        List<ItemStack> second = shear(h, sheep);
        h.assertTrue(second.stream().noneMatch(s -> s.is(Vanilla.wool(DyeColor.WHITE))), "regrown wool must not be normal wool: " + second);
        h.assertTrue(second.stream().anyMatch(s -> IRON.equals(ResourceWoolItem.getResourceId(s))), "regrown wool shears as iron wool: " + second);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).usesRemaining() == 98, "second shearing uses one more");
        h.succeed();
    }

    // ------------------------------------------------------------------ Automatic Shears

    /** Automatic Shears on the north edge facing south: its 3x3 area is x 2-4, z 1-3 (relative), the middle of the platform. */
    private static final BlockPos SHEARS_POS = new BlockPos(3, 1, 0);

    /** Places the shears with a full energy buffer. */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity autoShears(GameTestHelper h) {
        var be = unpoweredAutoShears(h);
        be.getEnergy().set(be.getEnergy().getCapacityAsInt());
        return be;
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity unpoweredAutoShears(GameTestHelper h) {
        h.setBlock(SHEARS_POS, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, net.minecraft.core.Direction.SOUTH));
        return h.getBlockEntity(SHEARS_POS, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.class);
    }

    private static Sheep sheepAt(GameTestHelper h, int x, int z) {
        Sheep sheep = h.spawn(Vanilla.SHEEP, new BlockPos(x, 1, z));
        sheep.setNoAi(true);
        sheep.setColor(DyeColor.WHITE);
        return sheep;
    }

    private static int countIn(net.minecraft.world.Container c, java.util.function.Predicate<ItemStack> match) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) if (match.test(c.getItem(i))) n += c.getItem(i).getCount();
        return n;
    }

    /** Shears every sheep in the 3x3 in front (infused ones give resource wool), and none outside it. */
    private static void autoShearsShearsArea(GameTestHelper h) {
        var shears = autoShears(h);
        Sheep iron = sheepAt(h, 3, 2);
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), iron);
        Sheep plain = sheepAt(h, 2, 3);
        Sheep cornerIn = sheepAt(h, 4, 1);
        Sheep beyond = sheepAt(h, 3, 5);   // 5 blocks out: past the 3x3
        Sheep beside = sheepAt(h, 5, 2);   // one column too far to the side
        h.runAfterDelay(5, () -> {
            h.assertTrue(iron.isSheared() && plain.isSheared() && cornerIn.isSheared(), "all three sheep in the 3x3 should be sheared");
            h.assertFalse(beyond.isSheared(), "sheep beyond the area must not be sheared");
            h.assertFalse(beside.isSheared(), "sheep beside the area must not be sheared");
            h.assertTrue(countIn(shears, s -> IRON.equals(ResourceWoolItem.getResourceId(s))) == 1, "1 iron wool stored");
            h.assertTrue(countIn(shears, s -> s.is(Vanilla.wool(DyeColor.WHITE))) >= 2, "plain sheep's white wool stored");
            h.assertTrue(SheepInfusionEvents.getInfusion(iron).usesRemaining() == 99, "infusion uses one shearing as usual");
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, h.getBounds()).isEmpty(),
                    "everything went into the inventory, nothing on the ground");
            h.assertTrue(shears.getBlockState().getAnalogOutputSignal(h.getLevel(), h.absolutePos(SHEARS_POS), net.minecraft.core.Direction.NORTH) > 0,
                    "comparator reads the stored wool");
            h.succeed();
        });
    }

    /** A full inventory stops it shearing; emptying a slot starts it again. */
    private static void autoShearsStopWhenFull(GameTestHelper h) {
        var shears = autoShears(h);
        for (int i = 0; i < shears.getContainerSize(); i++) shears.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        Sheep sheep = sheepAt(h, 3, 2);
        h.startSequence()
                .thenExecuteAfter(25, () -> h.assertFalse(sheep.isSheared(), "a full machine must not shear (and must not drop wool on the ground)"))
                .thenExecute(() -> shears.setItem(4, ItemStack.EMPTY))
                .thenWaitUntil(() -> h.assertTrue(sheep.isSheared(), "shears again once there is room"))
                .thenExecute(() -> h.assertTrue(countIn(shears, s -> s.is(Vanilla.wool(DyeColor.WHITE))) >= 1, "wool stored in the freed slot"))
                .thenSucceed();
    }

    /**
     * Redstone modes work like the separator's: "Active without Signal" pauses while powered and resumes when the signal
     * goes; "Disabled" never runs.
     */
    private static void autoShearsRedstone(GameTestHelper h) {
        var shears = autoShears(h);
        shears.setRedstoneMode(RedstoneMode.LOW);
        BlockPos power = new BlockPos(4, 1, 0);
        h.setBlock(power, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        Sheep sheep = sheepAt(h, 3, 2);
        h.startSequence()
                .thenExecuteAfter(25, () -> {
                    h.assertFalse(sheep.isSheared(), "active-without-signal + powered: paused");
                    h.assertTrue(shears.getStatus() == MachineStatus.REDSTONE_DISABLED, "status shows redstone, was " + shears.getStatus());
                })
                .thenExecute(() -> h.setBlock(power, net.minecraft.world.level.block.Blocks.AIR))
                .thenWaitUntil(() -> h.assertTrue(sheep.isSheared(), "signal removed: shears"))
                .thenExecute(() -> {
                    shears.setRedstoneMode(RedstoneMode.DISABLED);
                    sheep.setSheared(false); // wool back
                })
                .thenExecuteAfter(25, () -> h.assertFalse(sheep.isSheared(), "disabled: never shears"))
                .thenSucceed();
    }

    /** No FE, no shearing; power fed in from the back is used at energyPerShear per sheep; the front takes no power. */
    private static void autoShearsNeedPower(GameTestHelper h) {
        var shears = unpoweredAutoShears(h);
        Sheep a = sheepAt(h, 3, 2);
        Sheep b = sheepAt(h, 2, 2);
        int cost = shears.energyPerShear();
        h.assertTrue(cost > 0, "default config needs power");
        var back = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                h.absolutePos(SHEARS_POS), net.minecraft.core.Direction.NORTH);
        var front = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                h.absolutePos(SHEARS_POS), net.minecraft.core.Direction.SOUTH);
        h.assertTrue(back != null, "accepts FE from the back");
        h.assertTrue(front == null, "the front (the blades) takes no FE");
        h.startSequence()
                .thenExecuteAfter(25, () -> {
                    h.assertFalse(a.isSheared() || b.isSheared(), "no power: no shearing");
                    h.assertTrue(shears.getStatus() == MachineStatus.NO_ENERGY, "status No Energy, was " + shears.getStatus());
                    h.assertBlockProperty(SHEARS_POS, com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.ACTIVE, false);
                })
                // Exactly enough for one sheep: only one is sheared.
                .thenExecute(() -> h.assertTrue(insertEnergy(back, cost) == cost, "back face accepts FE"))
                .thenExecuteAfter(1, () -> h.assertBlockProperty(SHEARS_POS,
                        com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.ACTIVE, true))
                .thenWaitUntil(() -> h.assertTrue(a.isSheared() || b.isSheared(), "one sheep sheared with one shear's worth of FE"))
                .thenExecuteAfter(25, () -> {
                    h.assertFalse(a.isSheared() && b.isSheared(), "the second sheep must wait for more power");
                    h.assertTrue(shears.getEnergy().getEnergy() == 0, "used exactly " + cost + " FE, left " + shears.getEnergy().getEnergy());
                })
                .thenExecute(() -> insertEnergy(back, cost))
                .thenWaitUntil(() -> h.assertTrue(a.isSheared() && b.isSheared(), "second sheep sheared once powered again"))
                .thenSucceed();
    }

    /** A hopper below takes the wool out; nothing can be put in; mobs outside the tag (a mooshroom) are left alone. */
    private static void autoShearsHopperAndTag(GameTestHelper h) {
        var shears = autoShears(h);
        BlockPos hopper = new BlockPos(3, 0, 0);
        h.setBlock(hopper, net.minecraft.world.level.block.Blocks.HOPPER);
        var mooshroom = h.spawn(Vanilla.MOOSHROOM, new BlockPos(2, 1, 2));
        mooshroom.setNoAi(true);
        Sheep sheep = sheepAt(h, 4, 2);

        var handler = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, h.absolutePos(SHEARS_POS), net.minecraft.core.Direction.UP);
        h.assertTrue(handler != null, "exposes an item handler for pipes");
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.DIRT), 1, tx);
            h.assertTrue(inserted == 0, "output only: nothing can be inserted, got " + inserted);
        }
        h.assertFalse(shears.canPlaceItem(0, new ItemStack(Items.DIRT)), "hoppers above can't insert either");

        h.succeedWhen(() -> {
            h.assertTrue(sheep.isSheared(), "sheep sheared");
            var hopperBe = h.getBlockEntity(hopper, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
            h.assertTrue(countIn(hopperBe, s -> s.is(Vanilla.wool(DyeColor.WHITE))) >= 1, "hopper below pulls the wool out");
            h.assertTrue(mooshroom.isAlive() && mooshroom.getType() == Vanilla.MOOSHROOM, "mooshroom is not auto-sheared into a cow");
        });
    }

    /**
     * A wool type made by {@code /barelyfunctionalcontraptions generate} keeps the sheep's infusion through everything a normal
     * one does: feeding, saving and loading the sheep, shearing, regrowing, and another data pack reload.
     */
    private static final Identifier AMETHYST_GEN = Identifier.fromNamespaceAndPath(
            com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.NAMESPACE, "amethyst");
    private static final SheepResourceRegistry[] REGISTRY_BEFORE = new SheepResourceRegistry[1];

    private static void generatedResourceKeepsInfusion(GameTestHelper h) {
        var server = h.getLevel().getServer();
        Identifier slime = Identifier.fromNamespaceAndPath(com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.NAMESPACE, "slime");
        Sheep[] sheep = new Sheep[1];
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "barelyfunctionalcontraptions generate material slime");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(SheepResourceRegistry.server().byId(slime).isPresent(), "generated slime resource loaded"))
                .thenExecute(() -> {
                    sheep[0] = sheep(h);
                    feed(playerHolding(h, new ItemStack(Items.SLIME_BLOCK, 2)), sheep[0]); // generated resources take 2 blocks
                    var infusion = SheepInfusionEvents.getInfusion(sheep[0]);
                    ResourcefulSheep.LOGGER.info("[generated test] after feeding: {}", infusion);
                    h.assertTrue(infusion.resource().map(slime::equals).orElse(false), "fed a slime block: infused with generated slime, got " + infusion);
                    h.assertTrue(SheepInfusionEvents.getInfusedResource(sheep[0]).isPresent(), "infusion resolves to the loaded resource");

                    TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
                    sheep[0].saveWithoutId(out);
                    Sheep loaded = (Sheep) EntityType.create(Vanilla.SHEEP, TagValueInput.create(ProblemReporter.DISCARDING,
                            h.getLevel().registryAccess(), out.buildResult()), h.getLevel(), EntitySpawnReason.LOAD).orElseThrow();
                    ResourcefulSheep.LOGGER.info("[generated test] after save/load: {}", SheepInfusionEvents.getInfusion(loaded));
                    h.assertTrue(SheepInfusionEvents.getInfusion(loaded).equals(infusion), "save/load kept " + SheepInfusionEvents.getInfusion(loaded));

                    List<ItemStack> drops = shear(h, sheep[0]);
                    ResourcefulSheep.LOGGER.info("[generated test] first shear drops {}, infusion {}", drops, SheepInfusionEvents.getInfusion(sheep[0]));
                    h.assertTrue(drops.stream().anyMatch(st -> slime.equals(ResourceWoolItem.getResourceId(st))), "slime wool dropped: " + drops);
                    h.assertTrue(SheepInfusionEvents.getInfusion(sheep[0]).resource().isPresent(), "still infused after one shearing");
                    sheep[0].ate(); // regrow (the same call grass-eating makes)
                    h.assertTrue(SheepInfusionEvents.getInfusion(sheep[0]).resource().isPresent(), "still infused after regrowing");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
                })
                .thenExecuteAfter(40, () -> {
                    ResourcefulSheep.LOGGER.info("[generated test] after /reload: {} (resource loaded: {})",
                            SheepInfusionEvents.getInfusion(sheep[0]), SheepResourceRegistry.server().byId(slime).isPresent());
                    h.assertTrue(SheepResourceRegistry.server().byId(slime).isPresent(), "generated resource still loaded after /reload");
                    h.assertTrue(SheepInfusionEvents.getInfusedResource(sheep[0]).isPresent(), "still infused after /reload");
                    List<ItemStack> drops = shear(h, sheep[0]);
                    ResourcefulSheep.LOGGER.info("[generated test] second shear drops {}, infusion {}", drops, SheepInfusionEvents.getInfusion(sheep[0]));
                    h.assertTrue(drops.stream().anyMatch(st -> slime.equals(ResourceWoolItem.getResourceId(st))), "slime wool again: " + drops);
                    // Now the case that used to break: a wool type added on its own with "generate item"...
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "barelyfunctionalcontraptions generate item minecraft:amethyst_block");
                })
                .thenWaitUntil(() -> h.assertTrue(SheepResourceRegistry.server().byId(AMETHYST_GEN).isPresent(), "generated amethyst loaded"))
                .thenExecute(() -> {
                    sheep[0] = sheepAt(h, 1, 5);
                    feed(playerHolding(h, new ItemStack(Items.AMETHYST_BLOCK, 2)), sheep[0]);
                    h.assertTrue(SheepInfusionEvents.getInfusion(sheep[0]).resource().map(AMETHYST_GEN::equals).orElse(false),
                            "infused with generated amethyst: " + SheepInfusionEvents.getInfusion(sheep[0]));
                    // ...followed by a plain "generate", which used to delete every generated file it didn't plan itself.
                    REGISTRY_BEFORE[0] = SheepResourceRegistry.server();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "barelyfunctionalcontraptions generate");
                })
                .thenWaitUntil(() -> h.assertTrue(SheepResourceRegistry.server() != REGISTRY_BEFORE[0], "generate reloaded"))
                .thenExecute(() -> {
                    ResourcefulSheep.LOGGER.info("[generated test] after plain generate: amethyst loaded {}, infusion {}",
                            SheepResourceRegistry.server().byId(AMETHYST_GEN).isPresent(), SheepInfusionEvents.getInfusion(sheep[0]));
                    h.assertTrue(SheepResourceRegistry.server().byId(AMETHYST_GEN).isPresent(), "running generate again must keep the amethyst wool type");
                    h.assertTrue(SheepResourceRegistry.server().byId(slime).isPresent(), "and slime");
                    h.assertTrue(SheepInfusionEvents.getInfusedResource(sheep[0]).isPresent(), "the amethyst sheep is still infused");
                    List<ItemStack> drops = shear(h, sheep[0]);
                    h.assertTrue(drops.stream().anyMatch(st -> AMETHYST_GEN.equals(ResourceWoolItem.getResourceId(st))), "amethyst wool: " + drops);
                    // Tidy up so later runs in the same world start clean.
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "barelyfunctionalcontraptions generate remove amethyst");
                })
                .thenSucceed();
    }

    /**
     * A sheep infused with a wool type that isn't loaded (pack removed, mod missing) keeps that infusion through
     * shearing (it drops normal wool meanwhile), and can still be fed a different resource.
     */
    private static void unloadedInfusionKept(GameTestHelper h) {
        Sheep sheep = sheep(h);
        Identifier missing = Identifier.fromNamespaceAndPath("some_removed_pack", "tin");
        SheepInfusionEvents.setInfusion(sheep, new SheepInfusion(Optional.of(missing), 5, 0));
        List<ItemStack> drops = shear(h, sheep);
        h.assertTrue(drops.stream().anyMatch(st -> st.is(Vanilla.wool(DyeColor.WHITE))), "unloaded wool type shears as normal wool: " + drops);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).equals(new SheepInfusion(Optional.of(missing), 5, 0)),
                "infusion kept untouched for when the wool type returns: " + SheepInfusionEvents.getInfusion(sheep));
        sheep.ate();
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), sheep);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).resource().map(IRON::equals).orElse(false),
                "a stale infusion doesn't block feeding a new resource: " + SheepInfusionEvents.getInfusion(sheep));
        h.succeed();
    }

    private static void shearPlain(GameTestHelper h) {
        Sheep sheep = sheep(h);
        List<ItemStack> drops = shear(h, sheep);
        h.assertTrue(!drops.isEmpty() && drops.stream().allMatch(s -> s.is(Vanilla.wool(DyeColor.WHITE))), "plain sheep drops white wool, got " + drops);
        h.succeed();
    }

    /** With infusionUsesOverride = -1, each wool type's own uses apply: a diamond (1 use) runs out after one shearing. */
    private static void infusionRunsOut(GameTestHelper h) {
        int old = ModConfigs.SERVER.infusionUsesOverride.get();
        ModConfigs.SERVER.infusionUsesOverride.set(-1);
        try {
            Sheep iron = sheep(h);
            feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), iron);
            h.assertTrue(SheepInfusionEvents.getInfusion(iron).usesRemaining() == 9, "override off: iron's own 9 uses");
            Sheep sheep = sheep(h);
            feed(playerHolding(h, new ItemStack(Items.DIAMOND)), sheep); // diamond: 1 use
            List<ItemStack> first = shear(h, sheep);
            h.assertTrue(first.stream().anyMatch(s -> ResourcefulSheep.id("diamond").equals(ResourceWoolItem.getResourceId(s))), "diamond wool dropped");
            h.assertFalse(SheepInfusionEvents.getInfusion(sheep).isInfused(), "single-use infusion must be cleared after shearing");
            sheep.setSheared(false); // regrow
            List<ItemStack> second = shear(h, sheep);
            h.assertTrue(second.stream().allMatch(s -> s.is(Vanilla.wool(DyeColor.WHITE))), "sheep returns to normal wool, got " + second);
        } finally {
            ModConfigs.SERVER.infusionUsesOverride.set(old);
        }
        h.succeed();
    }

    private static void dispenserShears(GameTestHelper h) {
        Sheep sheep = h.spawn(Vanilla.SHEEP, new BlockPos(3, 1, 3));
        sheep.setNoAi(true);
        feed(playerHolding(h, new ItemStack(Items.GOLD_INGOT)), sheep);
        BlockPos dispenserPos = new BlockPos(3, 1, 2);
        h.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
        DispenserBlockEntity dispenser = h.getBlockEntity(dispenserPos, DispenserBlockEntity.class);
        dispenser.setItem(0, new ItemStack(Items.SHEARS));
        h.setBlock(new BlockPos(2, 1, 2), Blocks.REDSTONE_BLOCK);
        h.succeedWhen(() -> {
            h.assertTrue(sheep.isSheared(), "dispenser should shear the sheep");
            AABB box = new AABB(h.absolutePos(new BlockPos(3, 1, 3))).inflate(3);
            List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, box);
            h.assertTrue(items.stream().anyMatch(e -> ResourcefulSheep.id("gold").equals(ResourceWoolItem.getResourceId(e.getItem()))), "gold wool item entity expected");
            h.assertTrue(items.stream().noneMatch(e -> e.getItem().is(Vanilla.wool(DyeColor.WHITE))), "no plain wool expected");
        });
    }

    private static void infusionPersists(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.EMERALD)), sheep);
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        sheep.saveWithoutId(out);
        CompoundTag tag = out.buildResult();
        Sheep loaded = (Sheep) EntityType.create(Vanilla.SHEEP, TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag),
                h.getLevel(), EntitySpawnReason.LOAD).orElseThrow();
        h.assertTrue(SheepInfusionEvents.getInfusion(loaded).equals(SheepInfusionEvents.getInfusion(sheep)),
                "infusion lost on reload: " + SheepInfusionEvents.getInfusion(loaded));
        h.succeed();
    }

    private static void replaceRefused(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), sheep);
        Player player = playerHolding(h, new ItemStack(Items.GOLD_INGOT));
        feed(player, sheep);
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).resource().map(IRON::equals).orElse(false), "iron infusion must not be replaced");
        h.assertTrue(player.getMainHandItem().getCount() == 1, "refused feeding must not consume");
        h.succeed();
    }

    // ------------------------------------------------------------------ separator tests

    private static void separatorProcesses(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        h.assertTrue(insertEnergy(be.getEnergy(), 100_000) == 100_000, "machine should accept 100,000 FE");
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        h.succeedWhen(() -> {
            ItemStack raw = be.getInventory().getStack(SeparatorInventory.PRIMARY);
            h.assertTrue(raw.is(Items.RAW_IRON) && raw.getCount() >= 2 && raw.getCount() <= 4, "2-4 raw iron expected, got " + raw);
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.SECONDARY).is(Items.STRING)
                    && be.getInventory().getStack(SeparatorInventory.SECONDARY).getCount() == 2, "2 string expected");
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.INPUT).isEmpty(), "input consumed");
            h.assertTrue(be.getEnergy().getEnergy() == 80_000, "exactly 20,000 FE used, have " + be.getEnergy().getEnergy());
        });
    }

    private static void separatorNoEnergy(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        insertEnergy(be.getEnergy(), 5_000); // 20,000 FE over 75 ticks is 267 FE for the first 50: enough for 18 of them
        h.runAfterDelay(60, () -> {
            h.assertTrue(be.getStatus() == MachineStatus.NO_ENERGY, "status should be NO_ENERGY, was " + be.getStatus());
            h.assertTrue(be.getProgress() == 18, "progress should pause at 18, was " + be.getProgress());
            h.assertTrue(!be.getInventory().getStack(SeparatorInventory.INPUT).isEmpty(), "input must remain");
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.PRIMARY).isEmpty(), "no output without energy");
            h.assertFalse(h.getBlockState(pos).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.MechanicalSeparatorBlock.ACTIVE), "inactive when unpowered");
            insertEnergy(be.getEnergy(), 100_000);
        });
        h.runAfterDelay(160, () -> {
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON), "resumes and finishes once powered");
            h.succeed();
        });
    }

    private static void separatorAutomationSides(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos); // facing north
        var level = h.getLevel();
        BlockPos abs = h.absolutePos(pos);
        ResourceHandler<ItemResource> top = level.getCapability(Capabilities.Item.BLOCK, abs, Direction.UP);
        ResourceHandler<ItemResource> bottom = level.getCapability(Capabilities.Item.BLOCK, abs, Direction.DOWN);
        ResourceHandler<ItemResource> front = level.getCapability(Capabilities.Item.BLOCK, abs, Direction.NORTH);
        h.assertTrue(top != null && bottom != null, "top and bottom expose item handlers");
        h.assertTrue(front == null, "front exposes no item handler by default");
        h.assertTrue(insertItem(top, new ItemStack(Items.DIRT)) == 0, "invalid input rejected");
        h.assertTrue(insertItem(top, ResourceWoolItem.create(IRON, 4)) == 4, "wool accepted from top");
        h.assertTrue(insertItem(bottom, ResourceWoolItem.create(IRON, 1)) == 0, "bottom is output-only");
        be.getInventory().setStack(SeparatorInventory.PRIMARY, new ItemStack(Items.RAW_IRON, 5));
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(top.extract(ItemResource.of(Items.RAW_IRON), 5, tx) == 0, "top cannot extract outputs");
            h.assertTrue(bottom.extract(ItemResource.of(Items.RAW_IRON), 5, tx) == 5, "bottom extracts outputs");
            h.assertTrue(bottom.extract(ResourceWoolItem.getResource(ResourceWoolItem.create(IRON, 1), false).map(r -> ItemResource.of(ResourceWoolItem.create(IRON, 1))).orElseThrow(), 1, tx) == 0,
                    "input cannot be extracted");
        }
        EnergyHandler frontEnergy = level.getCapability(Capabilities.Energy.BLOCK, abs, Direction.NORTH);
        EnergyHandler backEnergy = level.getCapability(Capabilities.Energy.BLOCK, abs, Direction.SOUTH);
        h.assertTrue(frontEnergy == null && backEnergy != null, "energy accepted on back, not front");
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(backEnergy.insert(5_000, tx) == ModConfigs.getInt(ModConfigs.SERVER.maxEnergyInput), "insert rate limited");
            h.assertTrue(backEnergy.extract(100, tx) == 0, "machine never outputs energy");
        }
        h.succeed();
    }

    private static void separatorHopperChain(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 2, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        insertEnergy(be.getEnergy(), 100_000);
        h.setBlock(new BlockPos(3, 3, 3), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.setBlock(new BlockPos(3, 1, 3), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        h.setBlock(new BlockPos(3, 1, 2), Blocks.CHEST);
        HopperBlockEntity input = h.getBlockEntity(new BlockPos(3, 3, 3), HopperBlockEntity.class);
        input.setItem(0, ResourceWoolItem.create(ResourcefulSheep.id("coal"), 2));
        h.succeedWhen(() -> {
            BlockEntity chest = h.getBlockEntity(new BlockPos(3, 1, 2), BlockEntity.class);
            int coal = 0, string = 0;
            var container = (net.minecraft.world.Container) chest;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack s = container.getItem(i);
                if (s.is(Items.COAL)) coal += s.getCount();
                if (s.is(Items.STRING)) string += s.getCount();
            }
            h.assertTrue(coal >= 4 && coal <= 8 && string == 4, "chest should receive 4-8 coal (2-4 per wool) and 4 string, has " + coal + "/" + string);
        });
    }

    private static void separatorOutputFull(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        insertEnergy(be.getEnergy(), 100_000);
        be.getInventory().setStack(SeparatorInventory.SECONDARY, new ItemStack(Items.STRING, 63));
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        h.runAfterDelay(120, () -> {
            h.assertTrue(be.getStatus() == MachineStatus.OUTPUT_FULL, "should report OUTPUT_FULL, was " + be.getStatus());
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.INPUT).getCount() == 1, "input untouched");
            h.assertTrue(be.getEnergy().getEnergy() == 100_000, "no energy used while blocked");
            h.succeed();
        });
    }

    private static void separatorPersists(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos);
        insertEnergy(be.getEnergy(), 50_000);
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 3));
        h.runAfterDelay(10, () -> {
            CompoundTag tag = be.saveWithFullMetadata(h.getLevel().registryAccess());
            BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), tag, h.getLevel().registryAccess());
            h.assertTrue(loaded instanceof MechanicalSeparatorBlockEntity, "reload produced " + loaded);
            MechanicalSeparatorBlockEntity copy = (MechanicalSeparatorBlockEntity) loaded;
            h.assertTrue(copy.getEnergy().getEnergy() == be.getEnergy().getEnergy(), "energy persisted");
            h.assertTrue(copy.getProgress() == be.getProgress() && copy.getProgress() > 0, "progress persisted: " + copy.getProgress());
            h.assertTrue(ItemStack.matches(copy.getInventory().getStack(SeparatorInventory.INPUT), be.getInventory().getStack(SeparatorInventory.INPUT)), "inventory persisted");
            h.succeed();
        });
    }

    private static void separatingRecipe(GameTestHelper h) {
        var sep = SeparationLookup.find(h.getLevel(), new ItemStack(Vanilla.wool(DyeColor.RED)));
        h.assertTrue(sep.isPresent() && sep.get().primary().create().is(Items.STRING) && sep.get().primary().count() == 3,
                "separating recipe should turn wool into 3 string: " + sep);
        h.assertTrue(SeparationLookup.find(h.getLevel(), new ItemStack(Items.DIRT)).isEmpty(), "dirt is not separable");
        h.succeed();
    }

    private static void configMultipliers(GameTestHelper h) {
        Separation iron = SheepResourceRegistry.server().byId(IRON).orElseThrow().separation();
        double oldEnergy = ModConfigs.SERVER.energyCostMultiplier.get();
        double oldSpeed = ModConfigs.SERVER.processingSpeedMultiplier.get();
        try {
            ModConfigs.SERVER.energyCostMultiplier.set(0.5);
            ModConfigs.SERVER.processingSpeedMultiplier.set(2.0);
            h.assertTrue(iron.effectiveTicks() == 50, "speed x2 halves ticks, got " + iron.effectiveTicks());
            h.assertTrue(iron.effectiveEnergy() == 10_000, "energy x0.5 halves cost, got " + iron.effectiveEnergy());
        } finally {
            ModConfigs.SERVER.energyCostMultiplier.set(oldEnergy);
            ModConfigs.SERVER.processingSpeedMultiplier.set(oldSpeed);
        }
        h.assertTrue(iron.effectiveEnergy() == 20_000, "restored");
        h.succeed();
    }

    private static void customDefinitionParses(GameTestHelper h) {
        String json = """
                {
                  "feeding_item": "#c:storage_blocks/iron",
                  "feeding_amount": 2,
                  "display_name": "Test Metal",
                  "wool_color": "#123456",
                  "accent_color": [255, 0, 0],
                  "category": "ore",
                  "separator": { "primary_output": { "id": "minecraft:iron_nugget", "count": 4 }, "secondary_count": 0 }
                }""";
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        SheepResource.Definition def = SheepResource.DEFINITION_CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        SheepResource r = SheepResource.withId(ResourcefulSheep.id("test_metal"), def);
        h.assertTrue(r.feedingItem().test(new ItemStack(Items.IRON_BLOCK)), "tag-based feeding item should accept iron blocks");
        h.assertTrue(r.woolColor() == 0x123456 && r.accentColor() == 0xFF0000, "colours parsed");
        h.assertTrue(r.effectiveFeedingAmount() == 2, "feeding amount");
        Separation s = r.separation();
        h.assertTrue(s.primary().create().is(Items.IRON_NUGGET) && s.primary().count() == 4 && s.secondary().isEmpty(), "separator block parsed: " + s);
        h.assertTrue(r.name().getString().equals("Test Metal"), "display name");

        String bad = "{ \"feeding_item\": \"notamod:missing_item\" }";
        h.assertTrue(SheepResource.DEFINITION_CODEC.parse(ops, JsonParser.parseString(bad)).error().isPresent(), "missing items are reported as errors");
        h.assertTrue(ModItems.RESOURCE_WOOL.get() != null, "items registered");
        h.succeed();
    }

    private static net.minecraft.world.level.block.state.BlockState zpe(Direction facing) {
        return ModBlocks.ZERO_POINT_ENERGY_MODULE.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock.FACING, facing);
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity zpeAt(GameTestHelper h, BlockPos pos) {
        var be = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.class);
        be.updateLink(h.getLevel(), h.absolutePos(pos), h.getLevel().getBlockState(h.absolutePos(pos)));
        return be;
    }

    /**
     * Zero Point Energy Modules: a floor module (up) and a ceiling module (down) in one column link across open space
     * or glass and make energyPerBlock FE/t per block of gap, split between them, which they push into neighbours.
     * A solid block in the way, two modules facing the same way, or modules with no gap don't link.
     */
    private static void zeroPointModules(GameTestHelper h) {
        var linkedProp = com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock.LINKED;
        int perBlock = ModConfigs.getInt(ModConfigs.SERVER.zpeEnergyPerBlock);
        BlockPos floor = new BlockPos(1, 1, 1), ceiling = new BlockPos(1, 4, 1);
        h.setBlock(floor, zpe(Direction.UP));
        h.setBlock(ceiling, zpe(Direction.DOWN));
        var low = zpeAt(h, floor);
        var high = zpeAt(h, ceiling);
        h.assertTrue(low.getLinkDistance() == 3 && high.getLinkDistance() == 3, "pair 3 apart links, got " + low.getLinkDistance() + "/" + high.getLinkDistance());
        h.assertTrue(low.getPairOutput() == 2 * perBlock, "2-block gap makes " + 2 * perBlock + " FE/t, got " + low.getPairOutput());
        h.assertTrue(low.getGenerating() + high.getGenerating() == low.getPairOutput(), "the two halves add up to the pair's output");
        h.assertTrue(low.getLink() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink.LINKED, "linked reason");
        // Output limit: maxOutput, raised to the pair's whole output when that is bigger.
        int maxOutput = ModConfigs.getInt(ModConfigs.SERVER.zpeMaxOutput);
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.outputLimit(0) == maxOutput,
                "an unlinked module gives out up to maxOutput");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.outputLimit(maxOutput + 12_345) == maxOutput + 12_345,
                "a pair making more than maxOutput can give all of it out of one module");
        h.assertTrue(low.getOutputLimit() >= low.getPairOutput(), "the limit always covers the pair's output");

        // The GUI menu receives the module's numbers.
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = (com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu) low.createMenu(0, player.getInventory(), player);
        h.assertTrue(menu.getLink() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink.LINKED && menu.getLinkDistance() == 3 && menu.facing() == Direction.UP, "menu shows the link");
        h.assertTrue(menu.getGenerating() == low.getGenerating() && menu.getPairOutput() == low.getPairOutput(), "menu shows generation");
        h.assertTrue(menu.getCapacity() == 2 * ModConfigs.getInt(ModConfigs.SERVER.zpeCapacity), "menu shows the pair's pooled buffer");
        var highMenu = (com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu) high.createMenu(1, player.getInventory(), player);
        h.assertTrue(highMenu.facing() == Direction.DOWN && highMenu.towards().equals("below"), "ceiling module's menu knows it points down");
        h.assertBlockProperty(floor, linkedProp, true);
        h.assertBlockProperty(ceiling, linkedProp, true);

        // Glass doesn't block the beam; stone does.
        h.setBlock(floor.above(2), Blocks.GLASS);
        h.assertTrue(zpeAt(h, floor).getLinkDistance() == 3, "glass in the gap keeps the link");
        // Stained glass colours the beam like a beacon's: the first sets the colour, a different one mixes in.
        int red = net.minecraft.world.item.DyeColor.RED.getTextureDiffuseColor();
        int yellow = net.minecraft.world.item.DyeColor.YELLOW.getTextureDiffuseColor();
        h.setBlock(floor.above(1), Vanilla.stainedGlass(DyeColor.RED));
        h.setBlock(floor.above(2), Vanilla.stainedGlassPane(DyeColor.YELLOW));
        int[] mixed = zpeAt(h, floor).getBeamSections();
        int[] expected = {0, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_COLOR, 1, red, 2, net.minecraft.util.ARGB.average(red, yellow)};
        h.assertTrue(java.util.Arrays.equals(mixed, expected), "red then yellow glass: " + java.util.Arrays.toString(mixed));
        h.assertTrue(zpeAt(h, floor).getBeamColor() == net.minecraft.util.ARGB.average(red, yellow), "the whole beam is drawn in the mixed colour");
        h.setBlock(floor.above(2), Vanilla.stainedGlassPane(DyeColor.RED));
        int[] same = zpeAt(h, floor).getBeamSections();
        h.assertTrue(java.util.Arrays.equals(same, new int[] {0, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_COLOR, 1, red}), "the same colour twice is one section: " + java.util.Arrays.toString(same));
        h.assertTrue(zpeAt(h, floor).getPairOutput() == 2 * perBlock, "coloured beams make the same power");
        h.assertTrue(zpeAt(h, ceiling).getBeamSections().length == 0, "only the floor module keeps the colours");
        h.setBlock(floor.above(1), Blocks.AIR);
        h.assertTrue(java.util.Arrays.equals(zpeAt(h, floor).getBeamSections(), new int[] {0, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_COLOR, 2, red}), "a lone pane higher up starts there");
        h.assertTrue(zpeAt(h, floor).getBeamColor() == red, "a lone pane high up still colours the whole beam");
        h.setBlock(floor.above(2), Blocks.AIR);
        h.assertTrue(java.util.Arrays.equals(zpeAt(h, floor).getBeamSections(), new int[] {0, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_COLOR}), "no glass: all blue");
        h.assertTrue(zpeAt(h, floor).getBeamColor() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_COLOR, "no glass: blue beam");
        h.setBlock(floor.above(2), Blocks.STONE);
        h.assertTrue(zpeAt(h, floor).getLinkDistance() == 0 && zpeAt(h, ceiling).getLinkDistance() == 0, "stone in the gap breaks the link");
        h.assertTrue(zpeAt(h, floor).getLink() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink.BLOCKED, "the floor module reports the beam is blocked");
        var blocked = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.scan(h.getLevel(), h.absolutePos(floor), Direction.UP);
        h.assertTrue(blocked.stoppedAt() == 2, "the block is 2 above, got " + blocked.stoppedAt());
        h.assertBlockProperty(floor, linkedProp, false);
        h.assertTrue(zpeAt(h, floor).getGenerating() == 0, "unlinked modules make nothing");
        h.setBlock(floor.above(2), Blocks.AIR);

        // Both pointing up: no link.
        BlockPos sameA = new BlockPos(5, 1, 1), sameB = new BlockPos(5, 4, 1);
        h.setBlock(sameA, zpe(Direction.UP));
        h.setBlock(sameB, zpe(Direction.UP));
        h.assertTrue(zpeAt(h, sameA).getLinkDistance() == 0, "modules must face each other");
        h.assertTrue(zpeAt(h, sameA).getLink() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink.WRONG_FACING, "reported as facing the wrong way");
        h.assertTrue(zpeAt(h, sameB).getLink() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink.NO_MODULE, "nothing above the top one");
        // Touching (no gap): no link.
        BlockPos touchA = new BlockPos(5, 1, 5), touchB = new BlockPos(5, 2, 5);
        h.setBlock(touchA, zpe(Direction.UP));
        h.setBlock(touchB, zpe(Direction.DOWN));
        h.assertTrue(zpeAt(h, touchA).getLinkDistance() == 0, "modules need open space between them");
        h.assertTrue(zpeAt(h, touchA).getLink() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink.TOO_CLOSE, "reported as too close");

        // A second pair with nothing attached, to check the pool: either module reaches both buffers.
        BlockPos poolLow = new BlockPos(3, 1, 5), poolHigh = new BlockPos(3, 4, 5);
        h.setBlock(poolLow, zpe(Direction.UP));
        h.setBlock(poolHigh, zpe(Direction.DOWN));
        var pLow = zpeAt(h, poolLow);
        var pHigh = zpeAt(h, poolHigh);

        // A Hume Energy Cube touching only the floor module gets the pair's whole output, drawing the ceiling
        // module's half through the pool, so the ceiling module's buffer never builds up.
        BlockPos cubePos = new BlockPos(1, 1, 2);
        h.setBlock(cubePos, ModBlocks.HUME_ENERGY_CUBE.get());
        h.succeedWhen(() -> {
            var cube = h.getBlockEntity(cubePos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity.class);
            int stored = cube.storage().getAmountAsInt();
            h.assertTrue(stored >= 20 * low.getPairOutput(), "cube should receive the pair's full output, has " + stored);
            h.assertTrue(high.getEnergyStored() <= 2 * high.getGenerating(),
                    "the ceiling module's half is drawn through the floor module, still holds " + high.getEnergyStored());

            // The unattached pair: the ceiling module's capability holds and gives out both buffers.
            int own = pHigh.getEnergyStored(), other = pLow.getEnergyStored();
            h.assertTrue(own > 0 && other > 0, "both buffers of the idle pair charge");
            EnergyHandler pool = h.getLevel().getCapability(Capabilities.Energy.BLOCK, h.absolutePos(poolHigh), Direction.NORTH);
            h.assertTrue(pool != null && pool.getAmountAsLong() == own + other, "the ceiling module shows the whole pool");
            try (Transaction tx = Transaction.openRoot()) {
                int taken = pool.extract(Integer.MAX_VALUE, tx);
                h.assertTrue(taken == Math.min(own + other, pHigh.getOutputLimit()),
                        "all of the pool can be taken from one module, took " + taken + " of " + (own + other));
            } // not committed
            // The module only gives energy; nothing can be pushed into it.
            EnergyHandler handler = h.getLevel().getCapability(Capabilities.Energy.BLOCK, h.absolutePos(floor), Direction.NORTH);
            h.assertTrue(handler != null, "module exposes FE");
            try (Transaction tx = Transaction.openRoot()) {
                h.assertTrue(handler.insert(1000, tx) == 0, "modules don't accept FE");
            }
        });
    }

    /**
     * The beam hurts and ignites living things in it, with its own damage type, and leaves things beside it alone.
     * An iron golem (lots of health, no AI) stands on the floor module in the beam; a pig stands two blocks away.
     */
    /**
     * Two modules on facing walls link across the room just like a floor and ceiling pair: the east-pointing one leads
     * (keeps the odd FE and colours the beam), stained glass in the gap tints it, and the beam burns a pig standing in
     * it. A module placed against any face points away from that face.
     */
    private static void zeroPointSideways(GameTestHelper h) {
        int perBlock = ModConfigs.getInt(ModConfigs.SERVER.zpeEnergyPerBlock);
        var block = ModBlocks.ZERO_POINT_ENERGY_MODULE.get();
        var facingProp = com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock.FACING;
        // Placement: clicking each face of a block makes the module point out of that face.
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (Direction face : Direction.values()) {
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 5, 5))),
                    face, h.absolutePos(new BlockPos(2, 5, 5)), false);
            var context = new net.minecraft.world.item.context.BlockPlaceContext(h.getLevel(), player, InteractionHand.MAIN_HAND,
                    new ItemStack(block), hit);
            var placed = block.getStateForPlacement(context);
            h.assertTrue(placed != null && placed.getValue(facingProp) == face, "placed against the " + face + " face it points " + face);
        }

        BlockPos west = new BlockPos(0, 2, 2), east = new BlockPos(5, 2, 2);
        h.setBlock(west, zpe(Direction.EAST));
        h.setBlock(east, zpe(Direction.WEST));
        h.setBlock(new BlockPos(2, 2, 2), Vanilla.stainedGlass(DyeColor.RED));
        h.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
        var lead = zpeAt(h, west);
        var other = zpeAt(h, east);
        h.assertTrue(lead.getLinkDistance() == 5 && other.getLinkDistance() == 5, "a sideways pair 5 apart links, got "
                + lead.getLinkDistance() + "/" + other.getLinkDistance());
        h.assertTrue(lead.getPairOutput() == 4 * perBlock && lead.getGenerating() + other.getGenerating() == lead.getPairOutput(),
                "a 4-block gap makes " + 4 * perBlock + " FE/t, shared by the pair");
        h.assertTrue(lead.getGenerating() >= other.getGenerating(), "the east-pointing module leads");
        h.assertTrue(lead.getBeamColor() == net.minecraft.world.item.DyeColor.RED.getTextureDiffuseColor(), "red glass in the gap tints the beam");
        var player2 = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = (com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu) lead.createMenu(0, player2.getInventory(), player2);
        h.assertTrue(menu.facing() == Direction.EAST && menu.towards().equals("ahead"), "the GUI says the partner is ahead");
        // Facing the same way never links, sideways too.
        h.setBlock(east, zpe(Direction.EAST));
        h.assertTrue(zpeAt(h, west).getLink() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink.WRONG_FACING,
                "a partner pointing the same way is reported");
        h.setBlock(east, zpe(Direction.WEST));
        zpeAt(h, west);
        zpeAt(h, east);
        var pig = h.spawnWithNoFreeWill(Vanilla.PIG, new BlockPos(3, 2, 2));
        float health = pig.getHealth();
        h.succeedWhen(() -> {
            h.assertTrue(pig.getHealth() < health || !pig.isAlive(), "the sideways beam hurts the pig standing in it");
            h.assertTrue(pig.isOnFire() || !pig.isAlive(), "and sets it on fire");
        });
    }

    private static void zeroPointBeamBurns(GameTestHelper h) {
        BlockPos floor = new BlockPos(3, 1, 3), ceiling = new BlockPos(3, 4, 3);
        h.setBlock(floor, zpe(Direction.UP));
        h.setBlock(ceiling, zpe(Direction.DOWN));
        zpeAt(h, floor);
        zpeAt(h, ceiling);
        var golem = h.spawnWithNoFreeWill(Vanilla.IRON_GOLEM, new BlockPos(3, 2, 3));
        var pig = h.spawnWithNoFreeWill(Vanilla.PIG, new BlockPos(5, 1, 3));
        var beamKey = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_DAMAGE;
        String beamPlayer = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_PLAYER_NAME;

        // A second beam with a cow in it: the cow burns to death and, as a player kill, drops experience.
        BlockPos cowFloor = new BlockPos(1, 1, 1), cowCeiling = new BlockPos(1, 4, 1);
        h.setBlock(cowFloor, zpe(Direction.UP));
        h.setBlock(cowCeiling, zpe(Direction.DOWN));
        zpeAt(h, cowFloor);
        zpeAt(h, cowCeiling);
        var cow = h.spawnWithNoFreeWill(Vanilla.COW, new BlockPos(1, 2, 1));
        var cowArea = new net.minecraft.world.phys.AABB(h.absolutePos(cowFloor)).inflate(2, 3, 2);

        // Player-only loot: blaze rods only drop for player kills. Credited to the beam's fake player, they do.
        var blaze = h.spawnWithNoFreeWill(Vanilla.BLAZE, new BlockPos(5, 2, 5));
        var beamSource = h.getLevel().damageSources().source(beamKey);
        blaze.setLastHurtByPlayer(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity
                .beamPlayer(h.getLevel(), h.absolutePos(cowFloor)), 100);
        int rods = 0, rodsWithoutPlayer = 0;
        for (int i = 0; i < 40; i++) {
            List<ItemStack> asPlayerKill = new java.util.ArrayList<>(), notPlayerKill = new java.util.ArrayList<>();
            blaze.dropFromLootTable(h.getLevel(), beamSource, true, blaze.getLootTable().orElseThrow(), asPlayerKill::add);
            blaze.dropFromLootTable(h.getLevel(), beamSource, false, blaze.getLootTable().orElseThrow(), notPlayerKill::add);
            rods += count(asPlayerKill, st -> st.is(Items.BLAZE_ROD));
            rodsWithoutPlayer += count(notPlayerKill, st -> st.is(Items.BLAZE_ROD));
        }
        blaze.discard();
        h.assertTrue(rods > 0 && rodsWithoutPlayer == 0, "blaze rods drop only as a player kill: " + rods + " vs " + rodsWithoutPlayer);

        h.succeedWhen(() -> {
            var lastPlayer = golem.getLastHurtByPlayer();
            h.assertTrue(lastPlayer != null && lastPlayer.getGameProfile().name().equals(beamPlayer),
                    "the golem was last hurt by the beam's fake player, got " + lastPlayer);
            h.assertFalse(cow.isAlive(), "the cow in the beam dies");
            h.assertFalse(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, cowArea).isEmpty(),
                    "the beam kill drops experience");
            h.assertTrue(golem.getHealth() < golem.getMaxHealth(), "the golem in the beam is hurt, health " + golem.getHealth());
            h.assertTrue(golem.getLastDamageSource() != null && golem.getLastDamageSource().is(beamKey),
                    "hurt by the beam's own damage type, got " + golem.getLastDamageSource());
            h.assertTrue(golem.getRemainingFireTicks() > 0, "the golem is set on fire");
            h.assertTrue(pig.getHealth() == pig.getMaxHealth() && pig.getRemainingFireTicks() <= 0, "the pig beside the beam is untouched");
        });
    }

    /**
     * Mobs the beam kills drop as if killed with Looting {@code beamLooting}. A pig's death loot (1-3 porkchops) is
     * rolled 300 times with the beam as the cause and 300 times with plain fire: with the beam the most porkchops go up
     * by the looting level and the total goes up; with fire it stays at most 3.
     */
    private static void zeroPointBeamLooting(GameTestHelper h) {
        int looting = ModConfigs.getInt(ModConfigs.SERVER.zpeBeamLooting);
        h.assertTrue(looting >= 1, "default beamLooting is at least 1, got " + looting);
        var pig = h.spawnWithNoFreeWill(Vanilla.PIG, new BlockPos(3, 1, 3));
        var level = h.getLevel();
        var beam = level.damageSources().source(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_DAMAGE);
        var fire = level.damageSources().inFire();
        int beamTotal = 0, fireTotal = 0, beamMax = 0, fireMax = 0;
        for (int i = 0; i < 300; i++) {
            List<ItemStack> withBeam = new java.util.ArrayList<>(), withFire = new java.util.ArrayList<>();
            pig.dropFromLootTable(level, beam, false, pig.getLootTable().orElseThrow(), withBeam::add);
            pig.dropFromLootTable(level, fire, false, pig.getLootTable().orElseThrow(), withFire::add);
            int b = count(withBeam, st -> st.is(Items.PORKCHOP)), f = count(withFire, st -> st.is(Items.PORKCHOP));
            beamTotal += b;
            fireTotal += f;
            beamMax = Math.max(beamMax, b);
            fireMax = Math.max(fireMax, f);
        }
        h.assertTrue(fireMax <= 3, "normal pig drops are at most 3 porkchops, got " + fireMax);
        h.assertTrue(beamMax == 3 + looting, "beam kills drop up to " + (3 + looting) + " porkchops, got " + beamMax);
        h.assertTrue(beamTotal > fireTotal, "beam kills drop more in total: " + beamTotal + " vs " + fireTotal);

        // The looting helper itself: level 0 changes nothing, and overflowing stacks are split.
        var random = net.minecraft.util.RandomSource.create(1);
        var one = it.unimi.dsi.fastutil.objects.ObjectArrayList.of(new ItemStack(Items.BEEF, 2));
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.entity.ZeroPointBeamLootingModifier.applyLooting(one, 0, random)
                .getFirst().getCount() == 2, "looting 0 adds nothing");
        var full = it.unimi.dsi.fastutil.objects.ObjectArrayList.of(new ItemStack(Items.BEEF, 64));
        var split = com.jax3.barelyfunctionalcontraptions.sheep.entity.ZeroPointBeamLootingModifier.applyLooting(full, 10, random);
        h.assertTrue(split.stream().mapToInt(ItemStack::getCount).sum() >= 64 && split.stream().allMatch(st -> st.getCount() <= 64),
                "extra items over a full stack go into a new stack");
        pig.discard();
        h.succeed();
    }

    private static void creativeGeneratorPowers(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        MechanicalSeparatorBlockEntity be = separator(h, pos); // facing north: back (south) accepts FE
        h.setBlock(new BlockPos(3, 1, 4), ModBlocks.CREATIVE_GENERATOR.get());
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        // Pull side: any face of the generator offers unlimited energy.
        EnergyHandler source = h.getLevel().getCapability(Capabilities.Energy.BLOCK, h.absolutePos(new BlockPos(3, 1, 4)), Direction.UP);
        h.assertTrue(source != null, "generator exposes an energy handler");
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(source.extract(5_000_000, tx) == 5_000_000, "unlimited extraction");
        }
        h.succeedWhen(() -> {
            h.assertTrue(be.getEnergy().getEnergy() > 0, "generator should push FE into the separator");
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON), "separator runs on generator power");
        });
    }

    /** The default [generator] filter: outputs from Building Blocks / Ingredients / Food & Drinks, never a non-stacking item. */
    private static void generatorFilter(GameTestHelper h) {
        var server = h.getLevel().getServer();
        var registries = server.registryAccess();
        var filter = com.jax3.barelyfunctionalcontraptions.sheep.command.GeneratorItemFilter.fromConfig(server);
        h.assertTrue(filter.requireStackable() && filter.tabs().size() == 3, "default filter: 3 tabs + stackable only, got " + filter);

        // Tabs: one item from each allowed tab passes; a redstone-tab item does not.
        h.assertTrue(filter.allowsOutput(Items.STONE_BRICKS), "building blocks allowed");
        h.assertTrue(filter.allowsOutput(Items.RAW_IRON), "ingredients allowed");
        h.assertTrue(filter.allowsOutput(Items.BREAD), "food allowed");
        h.assertFalse(filter.allowsOutput(Items.REDSTONE_TORCH), "redstone tab is not allowed");
        // Non-stacking items are blacklisted even inside an allowed tab (stew is food) and as feeding items.
        h.assertFalse(filter.allowsOutput(Items.MUSHROOM_STEW), "stew doesn't stack");
        h.assertTrue(filter.feedingRejection(Items.DIAMOND_SWORD).isPresent(), "swords can't be fed");
        h.assertTrue(filter.feedingRejection(Items.WATER_BUCKET).isPresent(), "buckets can't be fed");
        h.assertTrue(filter.feedingRejection(Items.IRON_BLOCK).isEmpty(), "iron blocks can be fed");

        // generate / generate all: everything planned passes the filter.
        var plan = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.plan(registries, SheepResourceRegistry.server(), true, filter);
        h.assertFalse(plan.isEmpty(), "filtered plan should still find storage-block materials");
        var unfiltered = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.plan(registries, SheepResourceRegistry.server(), true);
        ResourcefulSheep.LOGGER.info("Generator filter: kept {}, dropped {}",
                plan.stream().map(g -> g.name() + "->" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(g.output()).getPath()).toList(),
                unfiltered.stream().map(g -> g.name()).filter(n -> plan.stream().noneMatch(g -> g.name().equals(n))).toList());
        for (var g : plan) {
            h.assertTrue(filter.allowsOutput(g.output()), g.name() + " output " + g.output() + " should pass the filter");
            var parsed = SheepResource.DEFINITION_CODEC.parse(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), g.json());
            h.assertTrue(parsed.result().isPresent(), g.name() + " should parse: " + parsed.error().map(Object::toString).orElse(""));
            for (var item : SheepResource.withId(com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions.id(g.name()), parsed.result().get()).feedingItems()) {
                h.assertTrue(filter.allowsFeeding(item.value()), g.name() + " feeds with non-stacking " + item.value());
            }
        }
        h.assertTrue(plan.stream().anyMatch(g -> g.name().equals("slime") && g.output() == Items.SLIME_BALL), "slime -> slime ball (ingredient) kept");
        var materials = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.storageMaterials(registries, filter);
        h.assertTrue(materials.contains("iron") && materials.contains("slime"), "suggestions keep allowed materials: " + materials);

        // generate item: food is fine, a redstone component is rejected for its output, a sword for feeding.
        var bread = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.forItem(registries, Items.BREAD, filter);
        h.assertTrue(bread.output() == Items.BREAD && filter.outputRejection(bread.output()).isEmpty(), "bread wool gives bread");
        var torch = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.forItem(registries, Items.REDSTONE_TORCH, filter);
        h.assertTrue(filter.outputRejection(torch.output()).isPresent(), "redstone torch rejected (not in an allowed tab)");

        // An empty tab list allows any tab, but the stack rule still applies.
        var anyTab = com.jax3.barelyfunctionalcontraptions.sheep.command.GeneratorItemFilter.create(registries, server.getWorldData().enabledFeatures(), List.of(), true);
        h.assertTrue(anyTab.allowsOutput(Items.REDSTONE_TORCH) && !anyTab.allowsOutput(Items.DIAMOND_SWORD), "empty tab list = any stackable item");
        h.succeed();
    }

    private static void generatorCreatesDefinitions(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var plan = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.plan(registries, SheepResourceRegistry.server(), false);
        java.util.Set<String> names = new java.util.HashSet<>();
        plan.forEach(g -> names.add(g.name()));
        h.assertTrue(names.contains("slime") && names.contains("dried_kelp"), "vanilla storage-block materials discovered: " + names);
        // Hay Bales and Bone Blocks already feed the built-in Hay and Skeleton sheep.
        h.assertFalse(names.contains("wheat") || names.contains("bone_meal"), "materials the themed sheep cover are skipped: " + names);
        h.assertFalse(names.contains("iron") || names.contains("diamond") || names.contains("netherite"), "already-defined materials skipped: " + names);
        h.assertFalse(names.stream().anyMatch(n -> n.startsWith("raw_")), "raw ore blocks skipped: " + names);
        var slime = plan.stream().filter(g -> g.name().equals("slime")).findFirst().orElseThrow();
        h.assertTrue(slime.output() == Items.SLIME_BALL && slime.uses() == 9, "slime block -> 9 slime balls, got " + slime.output() + " x" + slime.uses());

        // Every generated file must load through the real definition codec, and actually accept its block.
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        for (var g : plan) {
            var parsed = SheepResource.DEFINITION_CODEC.parse(ops, g.json());
            h.assertTrue(parsed.error().isEmpty(), g.name() + " does not parse: " + parsed.error().map(Object::toString).orElse(""));
            SheepResource r = SheepResource.withId(ResourcefulSheep.id(g.name()), parsed.getOrThrow());
            h.assertTrue(r.feedingItem().items().findAny().isPresent(), g.name() + " feeds nothing");
        }
        // Writing produces one file per definition plus pack.mcmeta.
        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("rs-gen");
            java.nio.file.Path pack = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.write(tmp, plan);
            h.assertTrue(java.nio.file.Files.exists(pack.resolve("pack.mcmeta")), "pack.mcmeta written");
            try (var files = java.nio.file.Files.list(pack.resolve("data/barelyfunctionalcontraptions_generated/barelyfunctionalcontraptions/sheep_resources"))) {
                h.assertTrue(files.count() == plan.size(), "one JSON per resource");
            }
        } catch (java.io.IOException e) {
            h.fail("write failed: " + e);
        }
        h.succeed();
    }

    private static void generatorDedupe(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        // "generate all" on purpose: produces copies of built-ins (iron, gold, diamond...) alongside new materials.
        var plan = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.plan(registries, SheepResourceRegistry.server(), true);
        java.util.Map<Identifier, SheepResource> combined = new java.util.HashMap<>(SheepResourceRegistry.server().asMap());
        for (var g : plan) {
            Identifier id = Identifier.fromNamespaceAndPath(com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.NAMESPACE, g.name());
            combined.put(id, SheepResource.withId(id, SheepResource.DEFINITION_CODEC.parse(ops, g.json()).getOrThrow()));
        }
        var dups = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.findDuplicates(new SheepResourceRegistry(combined));
        java.util.Set<String> dupNames = new java.util.HashSet<>();
        dups.forEach(d -> dupNames.add(d.resource().id().getPath()));
        for (String n : List.of("iron", "gold", "copper", "redstone", "lapis", "coal", "diamond", "emerald", "netherite", "wheat", "bone_meal")) {
            h.assertTrue(dupNames.contains(n), "generated " + n + " should be flagged as a duplicate: " + dupNames);
        }
        for (String n : List.of("slime", "dried_kelp")) {
            h.assertFalse(dupNames.contains(n), n + " is unique and must be kept: " + dupNames);
        }
        h.assertTrue(dups.stream().noneMatch(d -> !com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.isGenerated(d.resource())),
                "built-in definitions are never flagged");
        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("rs-dedupe");
            java.nio.file.Path pack = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.write(tmp, plan);
            int removed = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.delete(tmp, dups.stream().map(d -> d.resource()).toList());
            h.assertTrue(removed == dups.size(), "every duplicate file deleted: " + removed + "/" + dups.size());
            try (var files = java.nio.file.Files.list(pack.resolve("data/barelyfunctionalcontraptions_generated/barelyfunctionalcontraptions/sheep_resources"))) {
                h.assertTrue(files.count() == plan.size() - dups.size(), "unique definitions remain on disk");
            }
        } catch (java.io.IOException e) {
            h.fail("file handling failed: " + e);
        }
        h.succeed();
    }

    private static void generatorPickAndChoose(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var gen = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.class;
        var materials = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.storageMaterials(registries);
        h.assertTrue(materials.contains("slime") && !materials.contains("raw_iron"), "material suggestions: " + materials);
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.forMaterial(registries, "not_a_material").isEmpty(), "unknown material rejected");

        var slime = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.forMaterial(registries, "slime").orElseThrow();
        h.assertTrue(slime.feeding().equals("#c:storage_blocks/slime") && slime.uses() == 9, "slime material: " + slime.feeding() + " x" + slime.uses());

        // Any item: amethyst block is not a c:storage_blocks entry, so it is fed directly and defaults to 1 use.
        var amethyst = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.forItem(registries, Items.AMETHYST_BLOCK);
        h.assertTrue(amethyst.name().equals("amethyst") && amethyst.feeding().equals("minecraft:amethyst_block"), "amethyst: " + amethyst.name() + " " + amethyst.feeding());
        h.assertTrue(amethyst.output() == Items.AMETHYST_SHARD && amethyst.uses() == 1, "amethyst -> shard x1, got " + amethyst.output() + " x" + amethyst.uses());
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.coveredBy(registries, SheepResourceRegistry.server(), amethyst).isEmpty(), "amethyst is new");

        // A storage block picked by item uses the whole tag; iron is already covered by the built-in definition.
        var iron = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.forItem(registries, Items.IRON_BLOCK);
        h.assertTrue(iron.feeding().equals("#c:storage_blocks/iron"), "iron block uses the storage tag: " + iron.feeding());
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.coveredBy(registries, SheepResourceRegistry.server(), iron)
                .map(r -> r.id().equals(IRON)).orElse(false), "iron already covered by the built-in");

        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        h.assertTrue(SheepResource.DEFINITION_CODEC.parse(ops, amethyst.json()).error().isEmpty(), "amethyst JSON parses");
        // writeOne adds a file without removing the others.
        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("rs-pick");
            java.nio.file.Path pack = com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.write(tmp, List.of(slime));
            com.jax3.barelyfunctionalcontraptions.sheep.command.ResourceGenerator.writeOne(tmp, amethyst);
            try (var files = java.nio.file.Files.list(pack.resolve("data/barelyfunctionalcontraptions_generated/barelyfunctionalcontraptions/sheep_resources"))) {
                h.assertTrue(files.count() == 2, "writeOne keeps existing files");
            }
        } catch (java.io.IOException e) {
            h.fail("write failed: " + e);
        }
        h.succeed();
    }

    private static void outputRange(GameTestHelper h) {
        java.util.Set<Integer> seen = new java.util.TreeSet<>();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(42);
        for (SheepResource r : SheepResourceRegistry.server().all()) {
            Separation s = r.separation();
            h.assertTrue(s.primaryLabel().equals("2-4"), r.id() + " should output 2-4, label " + s.primaryLabel());
            h.assertTrue(s.maxPrimaryCount() == 4, r.id() + " machine reserves room for 4");
            for (int i = 0; i < 1000; i++) {
                int n = s.rollPrimaryCount(random);
                h.assertTrue(n >= 2 && n <= 4, r.id() + " rolled " + n);
                seen.add(n);
            }
        }
        h.assertTrue(seen.equals(java.util.Set.of(2, 3, 4)), "all of 2, 3 and 4 occur: " + seen);
        // Plain numbers still work and mean a fixed count.
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var fixed = com.jax3.barelyfunctionalcontraptions.sheep.resource.CountRange.CODEC.parse(ops, new com.google.gson.JsonPrimitive(3)).getOrThrow();
        h.assertTrue(fixed.isFixed() && fixed.min() == 3, "plain number = fixed count");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.resource.CountRange.CODEC.parse(ops, JsonParser.parseString("{\"min\":5,\"max\":2}")).error().isPresent(),
                "min > max rejected");
        h.succeed();
    }

    // ------------------------------------------------------------------ Fluid Reservoir

    private static final BlockPos RESERVOIR_POS = new BlockPos(1, 2, 1);

    /** A 2x2 pool of water sources at y=1 (x,z 1..2) inside a stone rim, on the stone floor: an infinite source. */
    private static void infinitePool(GameTestHelper h) {
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 3; z++) {
                boolean inside = x >= 1 && x <= 2 && z >= 1 && z <= 2;
                h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
                h.setBlock(new BlockPos(x, 1, z), inside ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.STONE);
            }
        }
    }

    private static String debugPool(GameTestHelper h) {
        BlockPos src = h.absolutePos(RESERVOIR_POS.below());
        StringBuilder sb = new StringBuilder();
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) sb.append(d).append('=').append(h.getLevel().getFluidState(src.relative(d))).append(' ');
        sb.append("under=").append(h.getLevel().getBlockState(src.below()));
        return sb.toString();
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity reservoir(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.FLUID_RESERVOIR.get());
        return h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity.class);
    }

    /** Over a 2x2 pool it makes 1 bucket a second until its 10 buckets are full, without using up the source. */
    private static void reservoirFills(GameTestHelper h) {
        infinitePool(h);
        var be = reservoir(h, RESERVOIR_POS);
        var tank = be.getTank();
        h.assertTrue(tank.getCapacity() == 10_000, "holds 10 buckets, was " + tank.getCapacity());
        h.startSequence()
                .thenExecuteAfter(40, () -> {
                    h.assertTrue(tank.getAmount() >= 1_500 && tank.getAmount() <= 2_500, "about 2 buckets after 2 seconds, was " + tank.getAmount() + " status " + be.getStatus() + " debug " + debugPool(h));
                    h.assertTrue(be.getStatus() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ReservoirStatus.FILLING, "status Filling, was " + be.getStatus());
                })
                .thenWaitUntil(() -> h.assertTrue(tank.getAmount() >= 10_000, "fills up, at " + tank.getAmount()))
                .thenExecuteAfter(25, () -> {
                    h.assertTrue(tank.getAmount() == 10_000, "never goes over 10,000 mB, was " + tank.getAmount());
                    h.assertTrue(be.getStatus() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ReservoirStatus.FULL, "status Full, was " + be.getStatus());
                    h.assertBlockProperty(RESERVOIR_POS, com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidReservoirBlock.FILL, 4);
                    h.assertTrue(h.getLevel().getFluidState(h.absolutePos(RESERVOIR_POS.below())).isSource(), "the water source below is not used up");
                })
                .thenSucceed();
    }

    /** A lone water source (no neighbouring sources) or no water at all: nothing is made. */
    private static void reservoirNeedsInfiniteSource(GameTestHelper h) {
        for (int x = 4; x <= 6; x++) {
            for (int z = 4; z <= 6; z++) {
                h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
                h.setBlock(new BlockPos(x, 1, z), x == 5 && z == 5 ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        var lone = reservoir(h, new BlockPos(5, 2, 5));
        var dry = reservoir(h, new BlockPos(1, 1, 1));
        // Two sources in a line: each has only one source neighbour, so neither refills itself.
        h.setBlock(new BlockPos(1, 1, 4), net.minecraft.world.level.block.Blocks.STONE);
        for (int x = 0; x <= 3; x++) {
            for (int z = 3; z <= 5; z++) h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(new BlockPos(x, 1, 3), net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(new BlockPos(x, 1, 5), net.minecraft.world.level.block.Blocks.STONE);
            h.setBlock(new BlockPos(x, 1, 4), x == 1 || x == 2 ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.STONE);
        }
        var pair = reservoir(h, new BlockPos(1, 2, 4));
        h.startSequence()
                .thenExecuteAfter(30, () -> {
                    h.assertTrue(lone.getTank().getAmount() == 0 && dry.getTank().getAmount() == 0 && pair.getTank().getAmount() == 0,
                            "no water without an infinite source: " + lone.getTank().getAmount() + ", " + dry.getTank().getAmount() + ", " + pair.getTank().getAmount());
                    h.assertTrue(lone.getStatus() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ReservoirStatus.NOT_INFINITE, "lone source: " + lone.getStatus());
                    h.assertTrue(pair.getStatus() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ReservoirStatus.NOT_INFINITE, "two in a line: " + pair.getStatus());
                    h.assertTrue(dry.getStatus() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ReservoirStatus.NO_SOURCE, "on stone: " + dry.getStatus());
                    h.assertBlockProperty(new BlockPos(5, 2, 5), com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidReservoirBlock.FILL, 0);
                })
                .thenSucceed();
    }

    /** Empty buckets in its slot are filled, pipes can take water but not put any in, and right-clicking with a bucket fills it. */
    private static void reservoirBucketsAndPipes(GameTestHelper h) {
        var be = reservoir(h, new BlockPos(3, 1, 3)); // on stone: makes nothing, so the amounts below are exact
        var tank = be.getTank();
        tank.generate(3_000);
        be.setItem(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity.SLOT_INPUT, new ItemStack(Items.BUCKET, 2));
        h.assertFalse(be.canPlaceItem(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity.SLOT_INPUT, new ItemStack(Items.DIRT)), "only buckets go in");
        h.startSequence()
                .thenExecuteAfter(2, () -> {
                    h.assertTrue(be.getItem(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity.SLOT_OUTPUT).is(Items.WATER_BUCKET), "a water bucket comes out");
                    h.assertTrue(be.getItem(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity.SLOT_INPUT).getCount() == 1, "one empty bucket used");
                    h.assertTrue(tank.getAmount() == 2_000, "1,000 mB per bucket, left " + tank.getAmount());

                    var fluid = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, h.absolutePos(new BlockPos(3, 1, 3)), net.minecraft.core.Direction.EAST);
                    h.assertTrue(fluid != null, "exposes a fluid handler for pipes");
                    var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
                    try (Transaction tx = Transaction.openRoot()) {
                        h.assertTrue(fluid.insert(water, 1_000, tx) == 0, "nothing can be poured in");
                        h.assertTrue(fluid.extract(water, 500, tx) == 500, "pipes can take water out");
                        tx.commit();
                    }
                    h.assertTrue(tank.getAmount() == 1_500, "500 mB taken, left " + tank.getAmount());

                    Player player = playerHolding(h, new ItemStack(Items.BUCKET));
                    h.useBlock(new BlockPos(3, 1, 3), player);
                    h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "right-click with a bucket fills it, holding " + player.getMainHandItem());
                    h.assertTrue(tank.getAmount() == 500, "another 1,000 mB taken, left " + tank.getAmount());
                })
                .thenSucceed();
    }

    /**
     * waterPerSecond and capacity apply straight away: 4,000 mB/s into a 2,000 mB tank, then the tank shrunk to 1,000.
     * Ticks the reservoir by hand so the changed config never leaks into other tests running at the same time.
     */
    private static void reservoirConfig(GameTestHelper h) {
        infinitePool(h);
        var be = reservoir(h, RESERVOIR_POS);
        var tank = be.getTank();
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(RESERVOIR_POS);
        int oldRate = ModConfigs.SERVER.reservoirWaterPerSecond.get();
        int oldCapacity = ModConfigs.SERVER.reservoirCapacity.get();
        Runnable tick = () -> com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity.serverTick(level, pos, level.getBlockState(pos), be);
        try {
            ModConfigs.SERVER.reservoirWaterPerSecond.set(4_000);
            ModConfigs.SERVER.reservoirCapacity.set(2_000);
            for (int i = 0; i < 5; i++) tick.run();
            h.assertTrue(tank.getCapacity() == 2_000, "capacity follows the config, was " + tank.getCapacity());
            h.assertTrue(tank.getAmount() == 1_000, "4,000 mB/s makes 1,000 mB in 5 ticks, made " + tank.getAmount());
            for (int i = 0; i < 20; i++) tick.run();
            h.assertTrue(tank.getAmount() == 2_000, "stops at the configured 2,000 mB, has " + tank.getAmount());
            h.assertTrue(be.getStatus() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ReservoirStatus.FULL, "status Full, was " + be.getStatus());
            ModConfigs.SERVER.reservoirCapacity.set(1_000);
            tick.run();
            h.assertTrue(tank.getCapacity() == 1_000 && tank.getAmount() == 1_000, "a smaller tank drops the extra water: "
                    + tank.getAmount() + " of " + tank.getCapacity());
        } finally {
            ModConfigs.SERVER.reservoirWaterPerSecond.set(oldRate);
            ModConfigs.SERVER.reservoirCapacity.set(oldCapacity);
        }
        h.succeed();
    }

    /** Infused sheep show blocks of their resource in the fleece, not flat items. */
    private static void growthBlocks(GameTestHelper h) {
        var registry = SheepResourceRegistry.server();
        java.util.Map<String, net.minecraft.world.item.Item> expected = java.util.Map.ofEntries(
                java.util.Map.<String, net.minecraft.world.item.Item>entry("iron", Items.IRON_BLOCK), java.util.Map.<String, net.minecraft.world.item.Item>entry("gold", Items.GOLD_BLOCK),
                java.util.Map.<String, net.minecraft.world.item.Item>entry("copper", net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("copper_block"))), java.util.Map.<String, net.minecraft.world.item.Item>entry("diamond", Items.DIAMOND_BLOCK),
                java.util.Map.<String, net.minecraft.world.item.Item>entry("emerald", Items.EMERALD_BLOCK), java.util.Map.<String, net.minecraft.world.item.Item>entry("redstone", Items.REDSTONE_BLOCK),
                java.util.Map.<String, net.minecraft.world.item.Item>entry("lapis", Items.LAPIS_BLOCK), java.util.Map.<String, net.minecraft.world.item.Item>entry("coal", Items.COAL_BLOCK),
                java.util.Map.<String, net.minecraft.world.item.Item>entry("netherite", Items.ANCIENT_DEBRIS), java.util.Map.<String, net.minecraft.world.item.Item>entry("stone", Items.COBBLESTONE),
                java.util.Map.<String, net.minecraft.world.item.Item>entry("wood", Items.OAK_LOG), java.util.Map.<String, net.minecraft.world.item.Item>entry("quartz", Items.QUARTZ_BLOCK),
                java.util.Map.<String, net.minecraft.world.item.Item>entry("red_mushroom", Items.RED_MUSHROOM_BLOCK), java.util.Map.<String, net.minecraft.world.item.Item>entry("brown_mushroom", Items.BROWN_MUSHROOM_BLOCK));
        expected.forEach((name, item) -> {
            ItemStack growth = registry.byId(ResourcefulSheep.id(name)).orElseThrow().growthStack();
            h.assertTrue(growth.is(item), name + " should grow " + item + ", grows " + growth);
        });
        for (SheepResource resource : registry.all()) {
            ItemStack growth = resource.growthStack();
            if (!growth.isEmpty() && !(growth.getItem() instanceof net.minecraft.world.item.BlockItem)) {
                // Only allowed when the resource's item has no block form at all.
                h.assertTrue(SheepResource.blockForm(growth.getItem()).isEmpty(), resource.id() + " grows a flat item " + growth);
            }
        }
        h.succeed();
    }

    // ------------------------------------------------------------------ recipe support for other mods

    private static void recipeSupport(GameTestHelper h) {
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        java.util.function.Function<String, net.minecraft.world.item.crafting.Ingredient> parse = json ->
                net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        var ironWool = com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(IRON, 1);
        var diamondWool = com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("diamond"), 1);
        var iron = parse.apply("{\"neoforge:ingredient_type\": \"barelyfunctionalcontraptions:resource_wool\", \"resource\": \"barelyfunctionalcontraptions:iron\"}");
        h.assertTrue(iron.test(ironWool) && !iron.test(diamondWool) && !iron.test(new ItemStack(Items.IRON_INGOT)), "the resource wool ingredient matches only Iron Wool");
        var gems = parse.apply("{\"neoforge:ingredient_type\": \"barelyfunctionalcontraptions:resource_wool\", \"category\": \"gem\"}");
        h.assertTrue(gems.test(diamondWool) && !gems.test(ironWool), "a category matches the wool of every resource in it");
        var any = parse.apply("{\"neoforge:ingredient_type\": \"barelyfunctionalcontraptions:resource_wool\"}");
        h.assertTrue(any.test(ironWool) && any.test(diamondWool) && !any.test(new ItemStack(Vanilla.wool(DyeColor.WHITE))), "no filter matches any resource wool, not plain wool");
        // It works inside the mod's own recipe type too.
        var recipe = com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparatingRecipe.MAP_CODEC.codec().parse(ops, JsonParser.parseString(
                "{\"ingredient\": {\"neoforge:ingredient_type\": \"barelyfunctionalcontraptions:resource_wool\", \"resource\": \"barelyfunctionalcontraptions:iron\"},"
                        + " \"primary_output\": \"minecraft:iron_ingot\"}")).getOrThrow();
        h.assertTrue(recipe.ingredient().test(ironWool), "a separating recipe can use it");
        // Generator fuels from the barelyfunctionalcontraptions:mycelial_fuel data map, after the config's.
        var crimson = com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelRegistry.get(new ItemStack(Items.CRIMSON_FUNGUS));
        h.assertTrue(crimson != null && crimson.burnTicks() == 300 && Math.abs(crimson.energyMultiplier() - 1.1) < 1e-9, "Crimson Fungus is a fuel from the data map: " + crimson);
        var warped = com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelRegistry.get(new ItemStack(Items.WARPED_FUNGUS));
        h.assertTrue(warped != null && Math.abs(warped.waterMultiplier() - 0.8) < 1e-9, "Warped Fungus uses less water: " + warped);
        var red = com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelRegistry.get(new ItemStack(Items.RED_MUSHROOM));
        h.assertTrue(red != null && red.burnTicks() == 600, "the config's Red Mushroom is unchanged: " + red);
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelRegistry.definitions().stream().anyMatch(d -> d.id().equals("minecraft:crimson_fungus")),
                "data map fuels are listed (for JEI)");
        h.assertFalse(com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelRegistry.isFuel(new ItemStack(Items.DIRT)), "dirt still isn't fuel");
        h.succeed();
    }

    // ------------------------------------------------------------------ Glow Panel

    private static void glowPanel(GameTestHelper h) {
        // Under a Glow Lamp with a roof over it (no sky, so only the lamp counts): the full light rate.
        BlockPos lit = new BlockPos(1, 1, 1);
        h.setBlock(lit, ModBlocks.GLOW_PANEL.get());
        h.setBlock(lit.above(), ModBlocks.GLOW_LAMP.get());
        h.setBlock(lit.above(2), Blocks.STONE);
        var cube = cube(h, lit.east());
        // Covered by stone: no light at all.
        BlockPos dark = new BlockPos(4, 1, 1);
        h.setBlock(dark, ModBlocks.GLOW_PANEL.get());
        h.setBlock(dark.above(), Blocks.STONE);
        h.runAfterDelay(10, () -> {
            var panel = h.getBlockEntity(lit, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GlowPanelBlockEntity.class);
            int rate = com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.glowPanelLightRate);
            h.assertTrue(panel.generating() == rate, "a Glow Lamp gives the full " + rate + " FE/t, got " + panel.generating());
            h.assertTrue(panel.source() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GlowPanelBlockEntity.Source.GLOW, "from glow light, got " + panel.source());
            var GLOW = com.jax3.barelyfunctionalcontraptions.sheep.blocks.GlowPanelBlock.GLOW;
            h.assertTrue(h.getBlockState(lit).getValue(GLOW) == 2, "a lamp's 40 of the best 60 FE/t lights the cells half way, got " + h.getBlockState(lit).getValue(GLOW));
            h.assertTrue(h.getBlockState(dark).getValue(GLOW) == 0, "a covered panel stays dark");
            java.util.function.IntUnaryOperator glowFor = com.jax3.barelyfunctionalcontraptions.sheep.blocks.GlowPanelBlock::glowFor;
            h.assertTrue(glowFor.applyAsInt(60) == 3 && glowFor.applyAsInt(40) == 2 && glowFor.applyAsInt(20) == 1
                    && glowFor.applyAsInt(1) == 1 && glowFor.applyAsInt(0) == 0, "full sun is brightest, dusk dim, nothing dark");
            h.assertTrue(h.getBlockState(lit).getLightEmission() == 2, "and it gives off light to match");
            h.assertTrue(cube.getEnergyHandler().getAmountAsLong() > 0, "it charges the cube beside it");
            var covered = h.getBlockEntity(dark, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GlowPanelBlockEntity.class);
            h.assertTrue(covered.generating() == 0 && covered.getEnergyHandler().getAmountAsInt() == 0, "a covered panel makes nothing");
            h.assertTrue(cubeInsert(cubeFace(h, dark, net.minecraft.core.Direction.UP), 100) == 0, "nothing can be put into a panel");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Energy Meter

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity meter(GameTestHelper h, BlockPos pos, net.minecraft.core.Direction out) {
        h.setBlock(pos, ModBlocks.ENERGY_METER.get().defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING, out));
        return h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity.class);
    }

    /** However the meter is placed, it faces from the source to the receiver; the wrench flips it. */
    private static void energyMeterPlacement(GameTestHelper h) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack meterItem = new ItemStack(ModItems.ENERGY_METER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, meterItem);
        var block = ModBlocks.ENERGY_METER.get();
        var facing = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING;
        java.util.function.BiFunction<BlockPos, Direction, Direction> place = (against, face) -> {
            BlockPos abs = h.absolutePos(against);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
            var context = new net.minecraft.world.item.context.BlockPlaceContext(h.getLevel(), player, InteractionHand.MAIN_HAND, meterItem, hit);
            return block.getStateForPlacement(context).getValue(facing);
        };
        // Clicking the floor between a Creative Generator (west) and a cube (east), looking any way: in from the generator.
        h.setBlock(new BlockPos(1, 1, 2), ModBlocks.CREATIVE_GENERATOR.get());
        cube(h, new BlockPos(3, 1, 2));
        h.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        h.setBlock(new BlockPos(2, 0, 5), Blocks.STONE);
        Direction floorFacing = place.apply(new BlockPos(2, 0, 2), Direction.UP);
        h.assertTrue(floorFacing == Direction.EAST, "between a generator and a cube it points at the cube, got " + floorFacing);
        // The screen faces the player, on a side that isn't a port.
        var screenProp = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN;
        java.util.function.Function<net.minecraft.world.phys.Vec3, Direction> screenFor = at -> {
            player.setPos(at);
            BlockPos floor = h.absolutePos(new BlockPos(2, 0, 2));
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
            return block.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(h.getLevel(), player, InteractionHand.MAIN_HAND, meterItem, hit)).getValue(screenProp);
        };
        var centre = net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 1, 2)));
        h.assertTrue(screenFor.apply(centre.add(0, 0, 3)) == Direction.SOUTH, "a player to the south sees the screen on the south side");
        h.assertTrue(screenFor.apply(centre.add(0, 0, -3)) == Direction.NORTH, "a player to the north sees it on the north side");
        Direction fromEnd = screenFor.apply(centre.add(-3, 0, 0.4));
        h.assertTrue(fromEnd == Direction.SOUTH, "from the generator's end (west, slightly south) it goes on the nearer side, not on a port: " + fromEnd);
        // A charged cube to the east and an empty one to the west: in from the charged one.
        var charged = cube(h, new BlockPos(3, 1, 5));
        cube(h, new BlockPos(1, 1, 5));
        cubeInsert(cubeFace(h, new BlockPos(3, 1, 5), Direction.UP), 5_000);
        h.assertTrue(place.apply(new BlockPos(2, 0, 5), Direction.UP) == Direction.WEST, "between a charged and an empty cube it takes from the charged one");
        // Placed against a cube's side: in from that cube.
        h.assertTrue(place.apply(new BlockPos(3, 1, 2), Direction.SOUTH) == Direction.SOUTH, "placed against a cube, it takes from the cube");
        // The wrench flips it; sneaking turns it to the next direction.
        h.setBlock(new BlockPos(5, 1, 2), block.defaultBlockState().setValue(facing, Direction.EAST));
        com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.turnWithWrench(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 2)), player);
        h.assertTrue(h.getBlockState(new BlockPos(5, 1, 2)).getValue(facing) == Direction.WEST, "the wrench flips it end to end");
        h.assertTrue(h.getBlockState(new BlockPos(5, 1, 2)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN) == Direction.NORTH,
                "flipping keeps the screen where it was");
        player.setShiftKeyDown(true);
        for (int i = 0; i < 6; i++) {
            com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.turnWithWrench(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 2)), player);
            var turned = h.getBlockState(new BlockPos(5, 1, 2));
            h.assertTrue(turned.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN).getAxis() != turned.getValue(facing).getAxis(),
                    "turning never leaves the screen on a port: " + turned);
        }
        h.succeed();
    }

    private static void energyMeter(GameTestHelper h) {
        var east = net.minecraft.core.Direction.EAST;
        var west = net.minecraft.core.Direction.WEST;
        // Pushing: Creative Generator -> meter (in at the west, out at the east) -> Hume Energy Cube.
        h.setBlock(new BlockPos(1, 1, 1), ModBlocks.CREATIVE_GENERATOR.get());
        var pushed = meter(h, new BlockPos(2, 1, 1), east);
        var cube = cube(h, new BlockPos(3, 1, 1));
        // Pulling: a charged cube behind a meter, pulled from the meter's front.
        cube(h, new BlockPos(1, 1, 4));
        var pulled = meter(h, new BlockPos(2, 1, 4), east);
        h.assertTrue(cubeInsert(cubeFace(h, new BlockPos(1, 1, 4), net.minecraft.core.Direction.UP), 10_000) == 10_000, "charge the source cube");
        EnergyHandler front = cubeFace(h, new BlockPos(2, 1, 4), west.getOpposite());
        EnergyHandler back = cubeFace(h, new BlockPos(2, 1, 4), west);
        h.assertTrue(cubeFace(h, new BlockPos(2, 1, 4), net.minecraft.core.Direction.UP) == null, "the meter only connects at the back and the front");
        int got;
        try (Transaction tx = Transaction.openRoot()) {
            got = front.extract(5_000, tx);
            tx.commit();
        }
        h.assertTrue(got == 5_000, "pulling from the front draws from the block behind, got " + got);
        h.assertTrue(cubeInsert(front, 1_000) == 0, "energy can't be pushed in at the front");
        h.assertTrue(pulled.total() == 5_000, "the pulled energy is counted, total " + pulled.total());
        // A rolled-back transfer isn't counted.
        try (Transaction tx = Transaction.openRoot()) {
            front.extract(3_000, tx);
        }
        h.assertTrue(pulled.total() == 5_000, "a cancelled transfer isn't counted, total " + pulled.total());
        h.runAfterDelay(45, () -> {
            int perTick = com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.energyCubeTransferPerCube);
            h.assertTrue(cube.getEnergyHandler().getAmountAsLong() > 0, "the cube in front is charged through the meter");
            h.assertTrue(pushed.rate() == perTick, "the meter reads the cube's " + perTick + " FE/t, got " + pushed.rate());
            h.assertTrue(pushed.total() >= cube.getEnergyHandler().getAmountAsLong(), "the total covers what reached the cube: " + pushed.total() + " vs " + cube.getEnergyHandler().getAmountAsLong());
            int signal = h.getBlockState(new BlockPos(2, 1, 1)).getAnalogOutputSignal(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 1)), east);
            h.assertTrue(signal == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity.signalFor(perTick) && signal == 10,
                    "10,000 FE/t gives comparator signal 10, got " + signal);
            h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity.signalFor(0) == 0
                    && com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity.signalFor(1) == 1
                    && com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity.signalFor(2_000_000) == 15, "signal scale");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Hume Energy Cube

    // ------------------------------------------------------------------ Blaze Sheep Furnace

    private static final BlockPos FURNACE_POS = new BlockPos(3, 1, 3);

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity furnace(GameTestHelper h) {
        h.getLevel().resetWeatherCycle(); // rain halves a sheep's heat
        h.setBlock(FURNACE_POS, ModBlocks.BLAZE_FURNACE.get());
        return h.getBlockEntity(FURNACE_POS, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.class);
    }

    private static net.minecraft.world.phys.BlockHitResult hitTop(GameTestHelper h, BlockPos rel) {
        BlockPos abs = h.absolutePos(rel);
        return new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
    }

    private static void blazeFurnaceHeat(GameTestHelper h) {
        var furnace = furnace(h);
        var cube = cube(h, FURNACE_POS.below());
        Identifier blaze = ResourcefulSheep.id("blaze");
        sheepAt(h, new BlockPos(2, 1, 3), blaze);
        Sheep plain = sheepAt(h, new BlockPos(4, 1, 3), null);
        h.runAfterDelay(25, () -> {
            h.assertTrue(furnace.generating() == 80, "one Blaze sheep beside it gives 80 FE/t, got " + furnace.generating());
            h.assertTrue(furnace.heaterCount() == 1, "the plain sheep beside it doesn't count, counted " + furnace.heaterCount());
            h.assertTrue(h.getBlockState(FURNACE_POS).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock.LIT), "it lights up");
            h.assertTrue(furnace.comparatorSignal() == 4, "a comparator reads one level per 20 FE/t, got " + furnace.comparatorSignal());
            h.assertTrue(cube.getEnergyHandler().getAmountAsLong() > 0, "it charges the cube under it");
            // Three more around it and one standing on top: only the hottest four count, each adding less.
            Sheep onTop = sheepAt(h, FURNACE_POS.above(), blaze);
            Sheep east = sheepAt(h, new BlockPos(4, 1, 2), blaze);
            sheepAt(h, new BlockPos(2, 1, 4), blaze);
            sheepAt(h, new BlockPos(4, 1, 4), blaze);
            sheepAt(h, new BlockPos(3, 1, 6), blaze); // two blocks past the 3x3: out of range
            Sheep lamb = sheepAt(h, new BlockPos(2, 1, 2), blaze);
            lamb.setBaby(true);
            h.runAfterDelay(25, () -> {
                h.assertTrue(furnace.heaterCount() == 4, "it counts four of the five in range, counted " + furnace.heaterCount());
                h.assertTrue(furnace.generating() == 260, "four Blaze sheep give 80 + 70 + 60 + 50 = 260 FE/t, got " + furnace.generating());
                // Sheared sheep have no flames: down to three.
                onTop.setSheared(true);
                east.setSheared(true);
                plain.discard();
                h.runAfterDelay(25, () -> {
                    h.assertTrue(furnace.generating() == 210, "three Blaze sheep give 210 FE/t, got " + furnace.generating());
                    h.succeed();
                });
            });
        });
    }

    private static void blazeFurnaceBoost(GameTestHelper h) {
        var furnace = furnace(h);
        var state = h.getBlockState(FURNACE_POS);
        // Right-click with Blaze Rods loads them; anything else doesn't go in.
        Player player = playerHolding(h, new ItemStack(Items.BLAZE_ROD, 3));
        state.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hitTop(h, FURNACE_POS));
        h.assertTrue(furnace.boostItems().is(Items.BLAZE_ROD) && furnace.boostItems().getCount() == 3, "three Blaze Rods go in, got " + furnace.boostItems());
        h.assertTrue(player.getMainHandItem().isEmpty(), "and leave the player's hand");
        Player cobble = playerHolding(h, new ItemStack(Items.COBBLESTONE, 5));
        var result = state.useItemOn(cobble.getMainHandItem(), h.getLevel(), cobble, InteractionHand.MAIN_HAND, hitTop(h, FURNACE_POS));
        h.assertTrue(result instanceof net.minecraft.world.InteractionResult.TryEmptyHandInteraction && cobble.getMainHandItem().getCount() == 5,
                "cobblestone isn't a boost item");
        // A hopper pointing into it adds rods and keeps everything else; a hopper below can't take them out.
        h.setBlock(new BlockPos(3, 1, 2), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.SOUTH));
        var feeder = h.getBlockEntity(new BlockPos(3, 1, 2), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        feeder.setItem(0, new ItemStack(Items.BLAZE_ROD, 2));
        feeder.setItem(1, new ItemStack(Items.DIRT, 1));
        h.setBlock(FURNACE_POS.below(), Blocks.HOPPER);
        h.runAfterDelay(30, () -> {
            h.assertTrue(furnace.boostItems().getCount() == 5, "the hopper adds its two rods and none are burnt without heat, got " + furnace.boostItems());
            h.assertTrue(furnace.boostTicks() == 0, "nothing burns while there's no heat to boost");
            h.assertTrue(feeder.getItem(1).is(Items.DIRT), "dirt stays in the hopper");
            sheepAt(h, new BlockPos(2, 1, 3), ResourcefulSheep.id("blaze"));
            h.runAfterDelay(25, () -> {
                h.assertTrue(furnace.boostTicks() > 0 && furnace.boostItems().getCount() == 4, "a sheep's heat burns one rod, left " + furnace.boostItems());
                h.assertTrue(furnace.generating() == 160, "a Blaze Rod doubles 80 FE/t to 160, got " + furnace.generating());
                h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock.statusMessage(furnace).getString().contains("x2"),
                        "right-click says it's boosted: " + com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock.statusMessage(furnace).getString());
                h.assertTrue(h.getBlockEntity(FURNACE_POS.below(), net.minecraft.world.level.block.entity.HopperBlockEntity.class).isEmpty(),
                        "the hopper under it can't pull rods out");
                h.succeed();
            });
        });
    }

    private static void blazeFurnaceData(GameTestHelper h) {
        h.getLevel().resetWeatherCycle();
        var sources = com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeatManager.sources();
        h.assertTrue(sources.stream().anyMatch(e -> e.getKey().equals(ResourcefulSheep.id("blaze_sheep")))
                && sources.stream().anyMatch(e -> e.getKey().equals(ResourcefulSheep.id("fiery_sheep"))), "the built-in heat sources load: " + sources);
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, h.getLevel().registryAccess());
        var codec = com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeat.CODEC;
        java.util.function.Function<String, com.mojang.serialization.DataResult<com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeat>> parse =
                json -> codec.parse(ops, com.google.gson.JsonParser.parseString(json));
        var cowHeat = parse.apply("{\"entity\": \"minecraft:cow\", \"power\": 30}").getOrThrow();
        h.assertTrue(parse.apply("{\"entity\": \"#minecraft:skeletons\", \"power\": 5}").isSuccess(), "an entity tag parses");
        h.assertTrue(parse.apply("{\"sheep\": \"a:b\", \"feature\": \"on_fire\", \"power\": 5}").isError(), "two kinds at once is an error");
        h.assertTrue(parse.apply("{\"power\": 5}").isError(), "no kind is an error");
        var glowing = parse.apply("{\"feature\": \"glowing\", \"power\": 10}").getOrThrow();
        var cow = h.spawn(Vanilla.COW, new BlockPos(1, 1, 1));
        cow.setNoAi(true);
        Sheep blaze = sheepAt(h, new BlockPos(3, 1, 3), ResourcefulSheep.id("blaze"));
        Sheep glowInk = sheepAt(h, new BlockPos(5, 1, 1), ResourcefulSheep.id("glow_ink"));
        h.assertTrue(cowHeat.matches(cow) && !cowHeat.matches(blaze), "an entity source matches that entity only");
        h.assertTrue(glowing.matches(glowInk) && !glowing.matches(blaze), "a feature source matches sheep showing that look");
        var heat = com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeatManager.heatOf(blaze).orElseThrow();
        h.assertTrue(heat.power() == 80 && heat.sheep().isPresent(), "the Blaze sheep's own source (80) wins over the on_fire one (60), got " + heat);
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeatManager.heatOf(cow).isEmpty(), "a cow isn't a heat source by default");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.combine(List.of(80.0, 80.0, 80.0, 80.0, 80.0), 4, 0.125) == 260,
                "four of five count, with falloff");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.combine(List.of(10.0, 30.0), 4, 0.0) == 40, "no falloff just adds");
        // Standing in water: half its heat.
        Sheep wet = sheepAt(h, new BlockPos(5, 1, 5), ResourcefulSheep.id("blaze"));
        h.setBlock(new BlockPos(5, 1, 5), Blocks.WATER);
        h.runAfterDelay(3, () -> {
            h.assertTrue(heat.powerFor(wet) == 40 && heat.powerFor(blaze) == 80, "in water it gives half: " + heat.powerFor(wet));
            h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceBoostDataMap.of(new ItemStack(Items.BLAZE_ROD)) != null
                    && com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceBoostDataMap.of(new ItemStack(Items.BLAZE_ROD)).duration() == 600,
                    "the Blaze Rod boost comes from the data map");
            h.succeed();
        });
    }

    private static void machineAreaToggle(GameTestHelper h) {
        var shears = autoShears(h);
        var furnace = furnace(h);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertFalse(shears.showsArea() || furnace.showsArea(), "areas start hidden");
        // Sneak-right-click with an empty hand toggles it.
        player.setShiftKeyDown(true);
        h.getBlockState(FURNACE_POS).useWithoutItem(h.getLevel(), player, hitTop(h, FURNACE_POS));
        h.getBlockState(SHEARS_POS).useWithoutItem(h.getLevel(), player, hitTop(h, SHEARS_POS));
        h.assertTrue(furnace.showsArea() && shears.showsArea(), "sneak-clicking shows the areas");
        h.assertTrue(furnace.displayedArea().equals(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.heatingArea(h.absolutePos(FURNACE_POS))),
                "the furnace shows its heating area");
        // The area follows [blaze_furnace] radius: 1 is 3x3, 2 (the default) is 5x5, always two blocks tall.
        BlockPos abs = h.absolutePos(FURNACE_POS);
        h.assertTrue(furnace.displayedArea().equals(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.heatingArea(abs,
                com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.blazeFurnaceRadius))),
                "the area uses the configured radius");
        var r1 = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.heatingArea(abs, 1);
        var r2 = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.heatingArea(abs, 2);
        var r0 = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.heatingArea(abs, 0);
        h.assertTrue(r1.getXsize() == 3 && r1.getZsize() == 3 && r1.getYsize() == 2 && r1.minY == abs.getY(), "radius 1 is 3x3, two tall: " + r1);
        h.assertTrue(r2.getXsize() == 5 && r2.getZsize() == 5 && r2.getYsize() == 2 && r2.contains(net.minecraft.world.phys.Vec3.atBottomCenterOf(abs.offset(2, 0, -2))),
                "radius 2 is 5x5 and reaches two blocks out: " + r2);
        h.assertTrue(r0.getXsize() == 1 && r0.contains(net.minecraft.world.phys.Vec3.atBottomCenterOf(abs.above())), "radius 0 is only the furnace's column: " + r0);
        h.assertTrue(shears.displayedArea().equals(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.shearingArea(h.absolutePos(SHEARS_POS), Direction.SOUTH)),
                "the shears show their shearing area");
        h.assertTrue(furnace.getUpdateTag(h.getLevel().registryAccess()).getBooleanOr("show_area", false)
                && shears.getUpdateTag(h.getLevel().registryAccess()).getBooleanOr("show_area", false), "and tell clients");
        // The shears' window button toggles it too, and shows it in the menu.
        var menu = new com.jax3.barelyfunctionalcontraptions.sheep.menu.AutomaticShearsMenu(0, player.getInventory(), shears,
                new net.minecraft.world.inventory.SimpleContainerData(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.DATA_COUNT) {
                    @Override
                    public int get(int index) {
                        return index == 11 ? (shears.showsArea() ? 1 : 0) : 0;
                    }
                });
        menu.clickMenuButton(player, com.jax3.barelyfunctionalcontraptions.sheep.menu.AutomaticShearsMenu.BUTTON_AREA);
        h.assertFalse(shears.showsArea() || menu.isShowingArea(), "the window button hides it");
        // Without sneaking, a click opens the furnace's window and the setting stays.
        player.setShiftKeyDown(false);
        h.getBlockState(FURNACE_POS).useWithoutItem(h.getLevel(), player, hitTop(h, FURNACE_POS));
        h.assertTrue(furnace.showsArea(), "a plain click doesn't change it");
        // The furnace's window: its area button toggles it, and its slot only takes boost items.
        var furnaceMenu = (com.jax3.barelyfunctionalcontraptions.sheep.menu.BlazeFurnaceMenu) furnace.createMenu(1, player.getInventory(), player);
        furnaceMenu.clickMenuButton(player, com.jax3.barelyfunctionalcontraptions.sheep.menu.BlazeFurnaceMenu.BUTTON_AREA);
        h.assertFalse(furnace.showsArea() || furnaceMenu.isShowingArea(), "the furnace window's button hides it");
        furnaceMenu.clickMenuButton(player, com.jax3.barelyfunctionalcontraptions.sheep.menu.BlazeFurnaceMenu.BUTTON_AREA);
        h.assertTrue(furnace.showsArea() && furnaceMenu.isShowingArea(), "and shows it again");
        h.assertTrue(furnaceMenu.getSlot(0).mayPlace(new ItemStack(Items.BLAZE_ROD)) && !furnaceMenu.getSlot(0).mayPlace(new ItemStack(Items.COBBLESTONE)),
                "the boost slot takes Blaze Rods only");
        h.assertTrue(furnaceMenu.getMaxHeat() == 260 && furnaceMenu.getBestPower() == 80 && furnaceMenu.getMaxHeaters() == 4,
                "the heat column tops out at four Blaze sheep: " + furnaceMenu.getMaxHeat());
        player.getInventory().setItem(0, new ItemStack(Items.BLAZE_ROD, 3));
        furnaceMenu.quickMoveStack(player, 1 + 27); // hotbar slot 0
        h.assertTrue(furnace.boostItems().getCount() == 3, "shift-clicking Blaze Rods puts them in the furnace, got " + furnace.boostItems());
        h.succeed();
    }

    // ------------------------------------------------------------------ Wool Burner

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity burner(GameTestHelper h, BlockPos pos, ItemStack fuel) {
        h.setBlock(pos, ModBlocks.WOOL_BURNER.get());
        var be = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity.class);
        be.setItem(0, fuel);
        return be;
    }

    private static void woolBurnerFuels(GameTestHelper h) {
        var wool = burner(h, new BlockPos(1, 1, 1), new ItemStack(Vanilla.wool(DyeColor.RED), 2));
        var string = burner(h, new BlockPos(3, 1, 1), new ItemStack(Items.STRING, 4));
        var coal = burner(h, new BlockPos(5, 1, 1), com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("coal"), 1));
        var iron = burner(h, new BlockPos(1, 1, 4), com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(IRON, 1));
        var cube = cube(h, new BlockPos(1, 1, 2));
        h.runAfterDelay(5, () -> {
            h.assertTrue(wool.generating() == 30 && wool.burnTotal() == 200 && wool.fuel().getCount() == 1,
                    "plain wool: 30 FE/t for 200 ticks, one used, got " + wool.generating() + " / " + wool.burnTotal() + " / " + wool.fuel());
            h.assertTrue(string.generating() == 30 && string.burnTotal() == 50 && string.fuel().getCount() == 3,
                    "string: 30 FE/t for 50 ticks, got " + string.generating() + " / " + string.burnTotal());
            h.assertTrue(coal.generating() == 120 && coal.burnTotal() == 400 && coal.fuel().isEmpty(),
                    "Coal Wool: 120 FE/t for 400 ticks, got " + coal.generating() + " / " + coal.burnTotal());
            h.assertTrue(iron.generating() == 0 && iron.fuel().getCount() == 1, "Iron Wool isn't a fuel");
            h.assertTrue(h.getBlockState(new BlockPos(1, 1, 1)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolBurnerBlock.LIT), "it lights up");
            h.assertTrue(cube.getEnergyHandler().getAmountAsLong() > 0, "it charges the cube beside it");
            // String burns out after 50 ticks and the next one is lit straight away.
            h.runAfterDelay(50, () -> {
                h.assertTrue(string.fuel().getCount() == 2 && string.generating() == 30, "the next string is lit, left " + string.fuel());
                h.succeed();
            });
        });
    }

    private static void woolBurnerFullAndHoppers(GameTestHelper h) {
        // Full buffer, nothing taking power: the wool waits.
        var full = burner(h, new BlockPos(1, 1, 1), new ItemStack(Vanilla.wool(DyeColor.WHITE), 3));
        var buffer = (net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler) full.getEnergyHandler();
        buffer.set(buffer.getCapacityAsInt());
        // Right-click with wool loads it; cobblestone doesn't go in.
        BlockPos pos = new BlockPos(4, 1, 4);
        var burner = burner(h, pos, ItemStack.EMPTY);
        Player player = playerHolding(h, new ItemStack(Vanilla.wool(DyeColor.BLUE), 5));
        h.getBlockState(pos).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hitTop(h, pos));
        h.assertTrue(burner.fuel().is(Vanilla.wool(DyeColor.BLUE)) && burner.fuel().getCount() == 5 && player.getMainHandItem().isEmpty(), "right-clicking with wool loads it");
        h.assertFalse(burner.canPlaceItem(0, new ItemStack(Items.COBBLESTONE)), "cobblestone isn't fuel");
        // A hopper pointing in adds string and keeps dirt; a hopper under it can't pull fuel out.
        BlockPos fedPos = new BlockPos(1, 1, 4);
        var fed = burner(h, fedPos, ItemStack.EMPTY);
        h.setBlock(fedPos.north(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.SOUTH));
        var hopper = h.getBlockEntity(fedPos.north(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.STRING, 2));
        hopper.setItem(1, new ItemStack(Items.DIRT, 1));
        h.setBlock(new BlockPos(4, 0, 4), Blocks.HOPPER);
        h.runAfterDelay(30, () -> {
            h.assertTrue(full.burnTime() == 0 && full.fuel().getCount() == 3, "with a full buffer it doesn't light more wool, left " + full.fuel());
            h.assertTrue(hopper.getItem(1).is(Items.DIRT) && hopper.getItem(0).isEmpty(), "the hopper puts string in and keeps the dirt");
            h.assertTrue(fed.burnTime() > 0 || fed.generating() > 0 || fed.fuel().getCount() < 2, "the string burns");
            h.assertTrue(h.getBlockEntity(new BlockPos(4, 0, 4), net.minecraft.world.level.block.entity.HopperBlockEntity.class).isEmpty(),
                    "the hopper under it can't pull wool out");
            // Emptying the buffer lets it light again.
            buffer.set(0);
            h.runAfterDelay(3, () -> {
                h.assertTrue(full.burnTime() > 0 && full.fuel().getCount() == 2, "once there's room it lights the next wool");
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ Wool Packer and Wool Bales

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity packer(GameTestHelper h, BlockPos pos, ItemStack wool, boolean powered) {
        h.setBlock(pos, ModBlocks.WOOL_PACKER.get());
        var be = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity.class);
        be.setItem(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity.INPUT, wool);
        if (powered) {
            var energy = (net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler) be.getEnergyHandler();
            energy.set(energy.getCapacityAsInt());
        }
        return be;
    }

    private static boolean isBaleOf(ItemStack stack, Identifier resource, int count) {
        return stack.is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOL_BALE.get()) && stack.getCount() == count
                && resource.equals(stack.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.SHEEP_RESOURCE.get()));
    }

    /** 20 Iron Wool: two bales, 1,000 FE each over 30 ticks, and 2 wool left waiting. */
    private static void woolPackerPresses(GameTestHelper h) {
        var be = packer(h, new BlockPos(1, 1, 1), ResourceWoolItem.create(IRON, 20), true);
        var gold = packer(h, new BlockPos(4, 1, 1), ResourceWoolItem.create(ResourcefulSheep.id("gold"), 8), true);
        h.assertFalse(be.canPlaceItem(0, new ItemStack(Vanilla.wool(DyeColor.WHITE))), "plain wool can't be baled");
        h.assertTrue(be.canPlaceItem(0, ResourceWoolItem.create(IRON, 1)), "resource wool can");
        h.assertFalse(be.canPlaceItem(1, com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(IRON, 1)), "nothing goes into the bale slot");
        h.runAfterDelay(5, () -> h.assertTrue(h.getBlockState(new BlockPos(1, 1, 1)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolPackerBlock.ACTIVE),
                "the press runs"));
        h.runAfterDelay(41, () -> {
            h.assertTrue(isBaleOf(be.output(), IRON, 1) && be.input().getCount() == 11, "one bale after 30 ticks, got " + be.output() + " / " + be.input());
        });
        h.runAfterDelay(100, () -> {
            h.assertTrue(isBaleOf(be.output(), IRON, 2) && be.input().getCount() == 2, "two bales, 2 wool left, got " + be.output() + " / " + be.input());
            h.assertTrue(be.status() == MachineStatus.IDLE, "waits for more wool, was " + be.status());
            h.assertTrue(be.getEnergyHandler().getAmountAsInt() == 18_000, "1,000 FE a bale, have " + be.getEnergyHandler().getAmountAsInt());
            h.assertTrue(gold.output().isEmpty() && gold.input().getCount() == 8, "8 wool isn't a bale");
            h.assertFalse(h.getBlockState(new BlockPos(1, 1, 1)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolPackerBlock.ACTIVE), "the press stops");
            h.succeed();
        });
    }

    /** No power pauses it, a full bale slot stops it, hoppers feed wool in and take only bales out. */
    private static void woolPackerPausesAndHoppers(GameTestHelper h) {
        var unpowered = packer(h, new BlockPos(1, 1, 1), ResourceWoolItem.create(IRON, 9), false);
        var full = packer(h, new BlockPos(3, 1, 1), ResourceWoolItem.create(IRON, 9), true);
        full.setItem(1, com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(IRON, 64));
        var mixed = packer(h, new BlockPos(5, 1, 1), ResourceWoolItem.create(IRON, 9), true);
        mixed.setItem(1, com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(ResourcefulSheep.id("gold"), 1));
        // Hopper above with 12 Iron Wool and a dirt; hopper below pulls.
        BlockPos fedPos = new BlockPos(3, 2, 4);
        var fed = packer(h, fedPos, ItemStack.EMPTY, true);
        h.setBlock(fedPos.above(), Blocks.HOPPER);
        var top = h.getBlockEntity(fedPos.above(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        top.setItem(0, ResourceWoolItem.create(IRON, 12));
        top.setItem(1, new ItemStack(Items.DIRT));
        h.setBlock(fedPos.below(), Blocks.HOPPER);
        var bottom = h.getBlockEntity(fedPos.below(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        h.runAfterDelay(60, () -> {
            h.assertTrue(unpowered.status() == MachineStatus.NO_ENERGY && unpowered.output().isEmpty(), "no power, no bale: " + unpowered.status());
            h.assertTrue(full.status() == MachineStatus.OUTPUT_FULL && full.input().getCount() == 9, "a full bale slot stops it: " + full.status());
            h.assertTrue(mixed.status() == MachineStatus.OUTPUT_FULL && mixed.input().getCount() == 9, "a different bale in the slot stops it");
        });
        h.succeedWhen(() -> {
            h.assertTrue(top.getItem(1).is(Items.DIRT), "the hopper keeps the dirt");
            int bales = 0, wool = 0;
            for (int i = 0; i < bottom.getContainerSize(); i++) {
                ItemStack st = bottom.getItem(i);
                if (st.is(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOL_BALE.get())) bales += st.getCount();
                if (st.getItem() instanceof ResourceWoolItem) wool += st.getCount();
            }
            h.assertTrue(wool == 0, "the hopper below never takes the wool");
            h.assertTrue(bales == 1, "the hopper below takes the bale, has " + bales);
            h.assertTrue(fed.input().getCount() == 3 && top.getItem(0).isEmpty(), "the 3 left over wait in the packer, has " + fed.input());
        });
    }

    /** A bale gives 12 wool's worth for 15 wool's energy over 9 wool's time; counts never pass a stack. */
    private static void woolBaleSeparationNumbers(GameTestHelper h) {
        var iron = com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup.find(h.getLevel(),
                com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(IRON, 1)).orElseThrow();
        h.assertTrue(iron.primary().item().value() == Items.RAW_IRON && iron.primaryRange().min() == 24 && iron.primaryRange().max() == 48,
                "an Iron Wool Bale gives 24-48 raw iron, got " + iron.primaryRange());
        h.assertTrue(iron.secondary().orElseThrow().count() == 24, "and 24 string, got " + iron.secondary());
        h.assertTrue(iron.bonus().orElseThrow().item().count() == 36 && Math.abs(iron.bonus().get().chance() - 0.1F) < 1e-6,
                "and a 10% chance of 36 iron nuggets, got " + iron.bonus());
        // 300,000 FE over 900 ticks of wool's time, which the x1.35 speed makes 667.
        h.assertTrue(iron.effectiveEnergy() == 300_000 && iron.effectiveTicks() == 667,
                "for 300,000 FE over 667 ticks, got " + iron.effectiveEnergy() + " / " + iron.effectiveTicks());
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup.find(h.getLevel(),
                new ItemStack(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOL_BALE.get())).isEmpty(), "a blank bale doesn't separate");
        // Ender pearls stack to 16: a bale of pearl wool gives at most a stack.
        var pearls = new com.jax3.barelyfunctionalcontraptions.sheep.resource.Separation(new net.minecraft.world.item.ItemStackTemplate(Items.ENDER_PEARL, 2), 4,
                java.util.Optional.empty(), java.util.Optional.of(new com.jax3.barelyfunctionalcontraptions.sheep.resource.ChanceOutput(
                        new net.minecraft.world.item.ItemStackTemplate(Items.ENDER_PEARL, 3), 0.5F)), java.util.Optional.empty(), java.util.Optional.empty()).baled();
        h.assertTrue(pearls.maxPrimaryCount() <= 16 && pearls.bonus().orElseThrow().item().count() <= 16,
                "bale outputs fit in a stack, got " + pearls.maxPrimaryCount() + " / " + pearls.bonus());
        h.succeed();
    }

    /** A placed bale keeps its wool: it drops the same bale, and pick-block gives it too. */
    private static void woolBaleBlock(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModBlocks.WOOL_BALE.get());
        var be = h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBaleBlockEntity.class);
        be.applyComponentsFromItemStack(com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(IRON, 1));
        h.assertTrue(IRON.equals(be.resource()), "placing a bale stores its wool, got " + be.resource());
        var drops = net.minecraft.world.level.block.Block.getDrops(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos), be);
        h.assertTrue(drops.size() == 1 && isBaleOf(drops.getFirst(), IRON, 1), "breaking it gives the Iron Wool Bale back, got " + drops);
        ItemStack picked = h.getBlockState(pos).getCloneItemStack(h.getLevel(), h.absolutePos(pos), false);
        h.assertTrue(isBaleOf(picked, IRON, 1), "pick-block gives the same bale, got " + picked);
        h.assertTrue(h.getBlockState(pos).is(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE), "a hoe breaks it fastest");
        h.succeed();
    }

    /** A Wool Bale alone in a crafting grid gives back its 9 wool, of the same resource; anything else doesn't match. */
    private static void woolBaleUnpacks(GameTestHelper h) {
        var level = h.getLevel();
        java.util.function.Function<List<ItemStack>, ItemStack> craft = items -> {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, items);
            return level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                    .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
        };
        java.util.function.Function<ItemStack, List<ItemStack>> grid = stack -> {
            List<ItemStack> items = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
            items.set(4, stack);
            return items;
        };
        var bale = com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(IRON, 1);
        ItemStack out = craft.apply(grid.apply(bale));
        h.assertTrue(out.getCount() == 9 && out.getItem() instanceof ResourceWoolItem && IRON.equals(ResourceWoolItem.getResourceId(out)),
                "an Iron Wool Bale unpacks into 9 Iron Wool, got " + out);
        var gold = com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(ResourcefulSheep.id("gold"), 1);
        List<ItemStack> corner = new java.util.ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        corner.set(0, gold);
        ItemStack goldOut = craft.apply(corner);
        h.assertTrue(goldOut.getCount() == 9 && ResourcefulSheep.id("gold").equals(ResourceWoolItem.getResourceId(goldOut)),
                "anywhere in the grid, and it keeps the wool: " + goldOut);
        List<ItemStack> two = grid.apply(bale);
        two.set(0, bale.copy());
        h.assertTrue(craft.apply(two).isEmpty() || !(craft.apply(two).getItem() instanceof ResourceWoolItem), "two bales at once don't unpack");
        h.assertTrue(!(craft.apply(grid.apply(new ItemStack(ModItems.WOOL_BALE.get()))).getItem() instanceof ResourceWoolItem),
                "a bale with no wool gives nothing");
        h.succeed();
    }

    /** A whole Iron Wool Bale through the separator: 24-48 raw iron and 24 string, the bale used up. */
    private static void separatorSeparatesBale(GameTestHelper h) {
        MechanicalSeparatorBlockEntity be = separator(h, new BlockPos(3, 1, 3));
        be.getInventory().setStack(SeparatorInventory.INPUT, com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(IRON, 1));
        h.onEachTick(() -> be.getEnergy().set(be.getEnergy().getCapacityAsInt()));
        h.succeedWhen(() -> {
            ItemStack raw = be.getInventory().getStack(SeparatorInventory.PRIMARY);
            h.assertTrue(raw.is(Items.RAW_IRON) && raw.getCount() >= 24 && raw.getCount() <= 48, "24-48 raw iron expected, got " + raw);
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.SECONDARY).getCount() == 24, "24 string expected");
            h.assertTrue(be.getInventory().getStack(SeparatorInventory.INPUT).isEmpty(), "bale used up");
        });
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity cube(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.HUME_ENERGY_CUBE.get());
        return h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity.class);
    }

    private static EnergyHandler cubeFace(GameTestHelper h, BlockPos pos, net.minecraft.core.Direction side) {
        return h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, h.absolutePos(pos), side);
    }

    private static int cubeInsert(EnergyHandler handler, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = handler.insert(amount, tx);
            tx.commit();
            return moved;
        }
    }

    private static int cubeExtract(EnergyHandler handler, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            int moved = handler.extract(amount, tx);
            tx.commit();
            return moved;
        }
    }

    /** One cube: 1,000,000 FE buffer, 10,000 FE/t in and out, on every side. */
    private static void cubeSingle(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        cube(h, pos);
        for (var side : net.minecraft.core.Direction.values()) {
            h.assertTrue(cubeFace(h, pos, side) != null, "energy on the " + side + " face");
        }
        EnergyHandler top = cubeFace(h, pos, net.minecraft.core.Direction.UP);
        h.assertTrue(top.getCapacityAsLong() == 1_000_000, "1,000,000 FE buffer, was " + top.getCapacityAsLong());
        h.assertTrue(cubeInsert(top, 50_000) == 10_000, "takes 10,000 FE per transfer");
        h.assertTrue(cubeExtract(cubeFace(h, pos, net.minecraft.core.Direction.WEST), 50_000) == 10_000, "gives 10,000 FE per transfer, from another side");
        h.assertTrue(top.getAmountAsLong() == 0, "empty again");
        h.succeed();
    }

    /** Three cubes in a row are one battery with 3x the buffer and rate; a fourth touching one joins; a separate cube doesn't. */
    private static void cubeGroupScales(GameTestHelper h) {
        BlockPos a = new BlockPos(1, 1, 3), b = new BlockPos(2, 1, 3), c = new BlockPos(3, 1, 3), lone = new BlockPos(5, 1, 3);
        cube(h, a);
        cube(h, b);
        var last = cube(h, c);
        var separate = cube(h, lone);
        h.assertTrue(last.group().size() == 3 && separate.group().size() == 1, "groups of 3 and 1, got " + last.group().size() + " and " + separate.group().size());
        EnergyHandler first = cubeFace(h, a, net.minecraft.core.Direction.WEST);
        h.assertTrue(first.getCapacityAsLong() == 3_000_000, "3x buffer, was " + first.getCapacityAsLong());
        h.assertTrue(cubeInsert(first, 100_000) == 30_000, "3x transfer rate in");
        h.assertTrue(cubeFace(h, c, net.minecraft.core.Direction.EAST).getAmountAsLong() == 30_000, "the energy is seen from the far end");
        h.assertTrue(cubeExtract(cubeFace(h, c, net.minecraft.core.Direction.UP), 100_000) == 30_000, "and taken out there, at 3x the rate");

        cube(h, new BlockPos(2, 2, 3)); // on top of the middle one
        h.assertTrue(last.group().size() == 4, "a fourth cube joins, size " + last.group().size());
        h.assertTrue(cubeFace(h, a, net.minecraft.core.Direction.WEST).getCapacityAsLong() == 4_000_000, "4x buffer");
        h.assertTrue(separate.group().size() == 1, "the cube two blocks away stays separate");
        h.succeed();
    }

    /** Breaking the middle cube splits the group; the ends keep their energy and the broken cube's item keeps its share. */
    private static void cubeBreakSplits(GameTestHelper h) {
        BlockPos a = new BlockPos(1, 1, 3), b = new BlockPos(2, 1, 3), c = new BlockPos(3, 1, 3);
        var ca = cube(h, a);
        var cb = cube(h, b);
        var cc = cube(h, c);
        ca.storage().set(100_000);
        cb.storage().set(200_000);
        cc.storage().set(300_000);
        h.assertTrue(ca.group().amount() == 600_000, "600,000 FE in the group");
        var components = cb.collectComponents();
        var contents = components.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.MACHINE_CONTENTS.get());
        h.assertTrue(contents != null && contents.energy() == 200_000, "the middle cube's item would keep its 200,000 FE");
        h.setBlock(b, net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(ca.group().size() == 1 && cc.group().size() == 1, "split into two single cubes");
        h.assertTrue(ca.group().amount() == 100_000 && cc.group().amount() == 300_000, "each end keeps its own energy");
        h.succeed();
    }

    /** A charged cube pushes FE into a machine touching it, at the group's rate. */
    private static void cubePushes(GameTestHelper h) {
        BlockPos cubePos = new BlockPos(2, 1, 3);
        var energyCube = cube(h, cubePos);
        cube(h, new BlockPos(1, 1, 3)); // a second cube: its neighbours are not push targets
        energyCube.storage().set(500_000);
        BlockPos machinePos = new BlockPos(3, 1, 3);
        h.setBlock(machinePos, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, net.minecraft.core.Direction.EAST));
        var shears = h.getBlockEntity(machinePos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.class);
        h.succeedWhen(() -> {
            h.assertTrue(shears.getEnergy().getEnergy() >= 5_000, "shears charged by the cube, has " + shears.getEnergy().getEnergy());
            h.assertTrue(energyCube.group().amount() == 500_000 - shears.getEnergy().getEnergy(), "energy only moved, none made or lost");
        });
    }

    /** With maxGroupSize = 2, three cubes in a row form a group of 2 and a group of 1. */
    private static void cubeMaxGroup(GameTestHelper h) {
        int old = ModConfigs.SERVER.energyCubeMaxGroupSize.get();
        try {
            ModConfigs.SERVER.energyCubeMaxGroupSize.set(2);
            var ca = cube(h, new BlockPos(1, 1, 3));
            var cb = cube(h, new BlockPos(2, 1, 3));
            var cc = cube(h, new BlockPos(3, 1, 3));
            for (var cube : List.of(ca, cb, cc)) cube.invalidateGroup();
            int sizeA = ca.group().size();
            int sizeC = cc.group().size();
            h.assertTrue(sizeA == 2 && sizeC == 1, "groups of 2 and 1, got " + sizeA + " and " + sizeC);
            h.assertTrue(ca.group().rate() == 20_000, "2 cubes, 20,000 FE/t, was " + ca.group().rate());
        } finally {
            ModConfigs.SERVER.energyCubeMaxGroupSize.set(old);
        }
        h.succeed();
    }

    /** The side windows follow the group's fill in eighths: empty 0, a quarter 2, half 4, nearly full 7, full 8. */
    private static void cubeWindow(GameTestHelper h) {
        var charge = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock.CHARGE;
        int[][] cases = {{0, 0}, {250_000, 2}, {500_000, 4}, {990_000, 7}, {1_000_000, 8}, {1_000, 1}};
        BlockPos[] spots = new BlockPos[cases.length];
        for (int i = 0; i < cases.length; i++) {
            spots[i] = new BlockPos(i, 1, i % 2 == 0 ? 1 : 4); // apart, so each is its own group
            cube(h, spots[i]).storage().set(cases[i][0]);
        }
        h.succeedWhen(() -> {
            for (int i = 0; i < cases.length; i++) {
                int expected = cases[i][1];
                h.assertBlockProperty(spots[i], charge, expected);
            }
        });
    }

    /**
     * The GUI's numbers, read through a real menu: two cubes' buffer and rate, the stored energy, and the measured
     * flow in (5,000 FE every tick) and its Charging status, then Discharging once energy is only taken out.
     */
    private static void cubeGui(GameTestHelper h) {
        BlockPos a = new BlockPos(2, 1, 3), b = new BlockPos(3, 1, 3);
        var cubeA = cube(h, a);
        cube(h, b);
        Player player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = (com.jax3.barelyfunctionalcontraptions.sheep.menu.EnergyCubeMenu) cubeA.createMenu(0, player.getInventory(), player);
        EnergyHandler face = cubeFace(h, a, net.minecraft.core.Direction.WEST);
        h.startSequence()
                .thenExecuteFor(45, () -> cubeInsert(face, 5_000))
                .thenExecute(() -> {
                    h.assertTrue(menu.getCubes() == 2, "2 cubes, got " + menu.getCubes());
                    h.assertTrue(menu.getCapacity() == 2_000_000L, "2,000,000 FE buffer, got " + menu.getCapacity());
                    h.assertTrue(menu.getRate() == 20_000, "20,000 FE/t, got " + menu.getRate());
                    h.assertTrue(menu.getAmount() == cubeA.group().amount() && menu.getAmount() >= 200_000, "stored energy, got " + menu.getAmount());
                    h.assertTrue(menu.getInRate() == 5_000 && menu.getOutRate() == 0, "5,000 FE/t in, 0 out; got " + menu.getInRate() + " / " + menu.getOutRate());
                    h.assertTrue(menu.getStatus() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CubeStatus.CHARGING, "Charging, got " + menu.getStatus());
                })
                .thenExecuteFor(45, () -> cubeExtract(face, 3_000))
                .thenExecute(() -> {
                    h.assertTrue(menu.getInRate() == 0 && menu.getOutRate() == 3_000, "0 in, 3,000 FE/t out; got " + menu.getInRate() + " / " + menu.getOutRate());
                    h.assertTrue(menu.getStatus() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CubeStatus.DISCHARGING, "Discharging, got " + menu.getStatus());
                })
                .thenSucceed();
    }

    /**
     * Touching cubes connect (their shared faces lose the frame), and a stack fills like one tank: two layers at 75%
     * show a full bottom layer and a half-full top layer; at 25% a half-full bottom and an empty top.
     */
    private static void cubeStack(GameTestHelper h) {
        var charge = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock.CHARGE;
        var connected = com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyCubeBlock.CONNECTED;
        BlockPos bottomL = new BlockPos(2, 1, 3), bottomR = new BlockPos(3, 1, 3), topL = new BlockPos(2, 2, 3), topR = new BlockPos(3, 2, 3);
        var a = cube(h, bottomL);
        var b = cube(h, bottomR);
        var c = cube(h, topL);
        var d = cube(h, topR);
        h.assertBlockProperty(bottomL, connected.get(net.minecraft.core.Direction.UP), true);
        h.assertBlockProperty(bottomL, connected.get(net.minecraft.core.Direction.EAST), true);
        h.assertBlockProperty(bottomL, connected.get(net.minecraft.core.Direction.WEST), false);
        h.assertBlockProperty(topR, connected.get(net.minecraft.core.Direction.DOWN), true);
        h.assertBlockProperty(topR, connected.get(net.minecraft.core.Direction.UP), false);
        for (var cube : List.of(a, b, c, d)) cube.storage().set(750_000); // 75% of 4,000,000
        h.startSequence()
                .thenWaitUntil(() -> {
                    h.assertBlockProperty(bottomL, charge, 8);
                    h.assertBlockProperty(bottomR, charge, 8);
                    h.assertBlockProperty(topL, charge, 4);
                    h.assertBlockProperty(topR, charge, 4);
                })
                .thenExecute(() -> { for (var cube : List.of(a, b, c, d)) cube.storage().set(250_000); }) // 25%
                .thenWaitUntil(() -> {
                    h.assertBlockProperty(bottomL, charge, 4);
                    h.assertBlockProperty(topR, charge, 0);
                })
                .thenExecute(() -> h.setBlock(topL, net.minecraft.world.level.block.Blocks.AIR))
                .thenExecute(() -> h.assertBlockProperty(bottomL, connected.get(net.minecraft.core.Direction.UP), false))
                .thenSucceed();
    }

    /** The main tab holds the machines and tools only; every resource wool, and a bale of each, is in the Resource Wool tab. */
    private static void creativeTabs(GameTestHelper h) {
        var level = h.getLevel();
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
        var main = com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.MAIN.get().getDisplayItems();
        // Leave out the section banners and blanks (an invisible placeholder item), and check each tab has them.
        var allWool = com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.RESOURCE_WOOL.get().getDisplayItems();
        var wool = allWool.stream().filter(s -> !s.is(ModItems.TAB_SECTION.get())).toList();
        java.util.function.Function<java.util.Collection<ItemStack>, java.util.List<String>> banners = stacks -> stacks.stream()
                .map(com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab::section)
                .filter(s -> s != null && s.isBanner() && s.index() == 0).map(com.jax3.barelyfunctionalcontraptions.registry.TabSection::section).toList();
        h.assertTrue(banners.apply(main).equals(java.util.List.of("power", "logistics", "production", "utility")), "the main tab's sections, got " + banners.apply(main));
        h.assertTrue(banners.apply(allWool).equals(java.util.List.of("wool", "bales", "eggs")), "the wool tab's sections, got " + banners.apply(allWool));
        h.assertTrue(main.size() % 9 == 0 && allWool.size() % 9 == 0, "every section fills whole rows");
        h.assertTrue(main.stream().noneMatch(s -> s.is(ModItems.RESOURCE_WOOL.get())), "no resource wool in the main tab");
        h.assertTrue(main.stream().anyMatch(s -> s.is(ModItems.MECHANICAL_SEPARATOR.get()))
                && main.stream().anyMatch(s -> s.is(ModItems.HUME_ENERGY_CUBE.get())), "machines stay in the main tab");
        h.assertTrue(main.stream().noneMatch(s -> s.is(ModItems.WOOL_BALE.get())), "no bales in the main tab");
        h.assertTrue(!wool.isEmpty() && wool.stream().allMatch(s -> s.is(ModItems.RESOURCE_WOOL.get()) || s.is(ModItems.WOOL_BALE.get())
                        || s.is(ModItems.RESOURCE_SHEEP_SPAWN_EGG.get())), "the wool tab holds only resource wool, bales and spawn eggs");
        h.assertTrue(main.stream().noneMatch(s -> s.is(ModItems.RESOURCE_SHEEP_SPAWN_EGG.get())), "no spawn eggs in the main tab");
        long eggs = wool.stream().filter(s -> s.is(ModItems.RESOURCE_SHEEP_SPAWN_EGG.get())).count();
        long pieces = wool.stream().filter(s -> s.is(ModItems.RESOURCE_WOOL.get())).count();
        long bales = wool.stream().filter(s -> s.is(ModItems.WOOL_BALE.get())).count();
        h.assertTrue(pieces >= com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.BUILT_IN_WOOL.size(),
                "every built-in wool type, got " + pieces);
        h.assertTrue(bales == pieces, "a bale of every wool, got " + bales + " for " + pieces);
        h.assertTrue(eggs == pieces, "a spawn egg of every sheep, got " + eggs + " for " + pieces);
        h.succeed();
    }

    /** A resource egg on the ground spawns a sheep already infused for its full shearings; on a sheep, an infused lamb. */
    private static void spawnEggs(GameTestHelper h) {
        var egg = com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem.create(ResourcefulSheep.id("diamond"), 2);
        Player player = playerHolding(h, egg);
        h.assertTrue(egg.getHoverName().getString().equals("Diamond Sheep Spawn Egg"), "named for its sheep, was " + egg.getHoverName().getString());
        BlockPos floor = h.absolutePos(new BlockPos(2, 0, 2));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor), net.minecraft.core.Direction.UP, floor, false);
        var result = egg.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(result.consumesAction(), "the egg is used, got " + result);
        var spawned = h.getLevel().getEntitiesOfClass(Sheep.class, new net.minecraft.world.phys.AABB(floor.above()).inflate(1));
        h.assertTrue(spawned.size() == 1, "one sheep spawns, got " + spawned.size());
        Sheep sheep = spawned.get(0);
        int uses = SheepResourceRegistry.server().byId(ResourcefulSheep.id("diamond")).orElseThrow().effectiveInfusionUses();
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).resource().map(ResourcefulSheep.id("diamond")::equals).orElse(false), "it comes infused with Diamond");
        h.assertTrue(SheepInfusionEvents.getInfusion(sheep).usesRemaining() == uses, "for its full shearings, has " + SheepInfusionEvents.getInfusion(sheep).usesRemaining());
        h.assertTrue(egg.getCount() == 1, "one egg used");

        var lamb = com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem.spawnLamb(h.getLevel(), player, sheep, egg);
        h.assertTrue(lamb.isPresent() && lamb.get().isBaby(), "used on a sheep it gives a lamb");
        h.assertTrue(SheepInfusionEvents.getInfusion(lamb.get()).resource().map(ResourcefulSheep.id("diamond")::equals).orElse(false), "and the lamb is infused too");
        h.succeed();
    }
}
