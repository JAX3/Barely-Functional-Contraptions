package com.jax3.barelyfunctionalcontraptions.sheep.gametest;

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
import net.minecraft.world.entity.EntityTypes;
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
        TESTS.put("fluid_tank_buckets", ModGameTests::tankBuckets);
        TESTS.put("breeding_inheritance_rules", ModGameTests::breedingRules);
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
        TESTS.put(LONG_BALE_TEST, ModGameTests::separatorSeparatesBale);
        RELOAD_TESTS.put("generated_resource_keeps_infusion", ModGameTests::generatedResourceKeepsInfusion);
        TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
        RELOAD_TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
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
        shears.setItem(0, new ItemStack(Items.WOOL.white(), 8));
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
            h.assertTrue(countIn(anything, Items.WOOL.white()) == 8 && countIn(diamondsOnly, Items.WOOL.white()) == 0,
                    "all the wool went to the unfiltered chest: " + countIn(anything, Items.WOOL.white()) + " / " + countIn(diamondsOnly, Items.WOOL.white()));
            // Push: the pipe stops taking wool out of the shears.
            setPipeSide(h, new BlockPos(2, 1, 2), Direction.WEST, side(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PUSH));
            shears.setItem(0, new ItemStack(Items.WOOL.white(), 8));
            h.runAfterDelay(20, () -> {
                h.assertTrue(countIn(shears, Items.WOOL.white()) == 8, "a Push side is never pulled from");
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
        shears.setItem(0, new ItemStack(Items.WOOL.white(), 64));
        var chest = chest(h, new BlockPos(3, 1, 2));
        itemPipe(h, new BlockPos(2, 1, 2));
        int[] start = new int[1];
        h.runAfterDelay(2, () -> start[0] = countIn(chest, Items.WOOL.white()));
        h.runAfterDelay(12, () -> {
            try {
                int moved = countIn(chest, Items.WOOL.white()) - start[0];
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
            h.assertTrue(h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at) == com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepGlowBlock.LIGHT_LEVEL,
                    "the light is level 12, got " + h.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at));
            // Sheared: no more glow, and the light goes out on its next check.
            sheep.setSheared(true);
            h.runAfterDelay(com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepGlowBlock.CHECK_TICKS + 2, () -> {
                h.assertTrue(h.getLevel().getBlockState(at).isAir(), "the light goes out once the sheep is sheared, found " + h.getLevel().getBlockState(at));
                h.succeed();
            });
        });
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
        Sheep copy = EntityTypes.SHEEP.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.LOAD);
        copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved.buildResult()));
        h.assertTrue(copy.getData(skinAttachment).profile().map(p -> p.partialProfile().id().equals(id)).orElse(false), "the skin survives saving and loading");
        h.succeed();
    }

    private static void themedAdvancements(GameTestHelper h) {
        var server = h.getLevel().getServer();
        for (String name : List.of("hay", "wither", "dragon", "glow_ink", "skeleton", "wither_skeleton", "spider", "warden", "blaze", "ghast")) {
            var adv = server.getAdvancements().get(ResourcefulSheep.id("contraptions/sheep/" + name));
            h.assertTrue(adv != null && adv.value().parent().map(ResourcefulSheep.id("contraptions/infuse_sheep")::equals).orElse(false),
                    "themed advancement " + name + " is loaded under Infuse a Sheep");
        }
        var every = server.getAdvancements().get(ResourcefulSheep.id("contraptions/every_wool"));
        h.assertTrue(every != null && every.value().criteria().size() == 24, "the every-wool challenge has one criterion per built-in wool");
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
            var instance = (net.minecraft.advancements.triggers.InventoryChangeTrigger.TriggerInstance) entry.getValue().triggerInstance();
            h.assertTrue(instance.matches(player.getInventory(), wool.apply(entry.getKey()), 0, 0, 1), entry.getKey() + " wool is recognised");
            String other = entry.getKey().equals("iron") ? "gold" : "iron";
            h.assertFalse(instance.matches(player.getInventory(), wool.apply(other), 0, 0, 1), entry.getKey() + " isn't met by " + other + " wool");
        }
        h.succeed();
    }

    private static void ghastSheepScale(GameTestHelper h) {
        Sheep ghast = sheepAt(h, new BlockPos(2, 1, 2), ResourcefulSheep.id("ghast"));
        float plainWidth = EntityTypes.SHEEP.getWidth();
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
        Sheep sheep = h.spawn(EntityTypes.SHEEP, pos);
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
        int full = SheepResourceRegistry.server().byId(IRON).orElseThrow().effectiveInfusionUses();
        Sheep lamb = breed(h, sheepAt(h, new BlockPos(1, 1, 1), IRON), sheepAt(h, new BlockPos(1, 1, 2), IRON));
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
        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 3));
        sheep.setNoAi(true);
        sheep.setColor(DyeColor.WHITE);
        return sheep;
    }

    private static Player playerHolding(GameTestHelper h, ItemStack stack) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static void feed(Player player, Sheep sheep) {
        player.interactOn(sheep, InteractionHand.MAIN_HAND, sheep.position());
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
        h.assertTrue(iron.effectiveEnergy() == 20000 && iron.effectiveTicks() == 100, "iron costs 20,000 FE over 100 ticks, got " + iron.effectiveEnergy() + "/" + iron.effectiveTicks());
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

    /** Every block in the mod places, breaks and steps with the vanilla Iron Block's sounds. */
    private static void ironSounds(GameTestHelper h) {
        var ironPlace = net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState().getSoundType().getPlaceSound();
        int checked = 0;
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(ResourcefulSheep.MOD_ID)) continue;
            if (block == com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.SHEEP_GLOW.get()) continue; // invisible light, never placed by hand
            if (block == com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.WOOL_BALE.get()) continue; // wool, so it sounds like wool
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
        for (String machine : List.of("mycelial_generator", "mechanical_separator", "automatic_shears", "fluid_reservoir", "hume_energy_cube", "fluid_tank", "zero_point_energy_module", "blaze_furnace", "wool_burner", "wool_packer")) {
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
        h.assertTrue(count(drops, st -> st.is(Items.WOOL.white())) == 0, "no plain wool from an infused sheep: " + drops);
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
        h.assertTrue(count(plain, st -> st.is(Items.WOOL.white())) == 1 && count(plain, ironWool) == 0, "plain sheep drops white wool: " + plain);
        h.succeed();
    }

    private static void shearInfused(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.IRON_INGOT)), sheep);
        List<ItemStack> drops = shear(h, sheep);
        h.assertTrue(sheep.isSheared(), "sheep should be sheared");
        h.assertTrue(drops.stream().noneMatch(s -> s.is(Items.WOOL.white())), "infused sheep must not drop normal wool: " + drops);
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
        h.assertTrue(second.stream().noneMatch(s -> s.is(Items.WOOL.white())), "regrown wool must not be normal wool: " + second);
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
        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(x, 1, z));
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
            h.assertTrue(countIn(shears, s -> s.is(Items.WOOL.white())) >= 2, "plain sheep's white wool stored");
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
                .thenExecute(() -> h.assertTrue(countIn(shears, s -> s.is(Items.WOOL.white())) >= 1, "wool stored in the freed slot"))
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
        var mooshroom = h.spawn(EntityTypes.MOOSHROOM, new BlockPos(2, 1, 2));
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
            h.assertTrue(countIn(hopperBe, s -> s.is(Items.WOOL.white())) >= 1, "hopper below pulls the wool out");
            h.assertTrue(mooshroom.isAlive() && mooshroom.getType() == EntityTypes.MOOSHROOM, "mooshroom is not auto-sheared into a cow");
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
                    feed(playerHolding(h, new ItemStack(Items.SLIME_BLOCK)), sheep[0]);
                    var infusion = SheepInfusionEvents.getInfusion(sheep[0]);
                    ResourcefulSheep.LOGGER.info("[generated test] after feeding: {}", infusion);
                    h.assertTrue(infusion.resource().map(slime::equals).orElse(false), "fed a slime block: infused with generated slime, got " + infusion);
                    h.assertTrue(SheepInfusionEvents.getInfusedResource(sheep[0]).isPresent(), "infusion resolves to the loaded resource");

                    TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
                    sheep[0].saveWithoutId(out);
                    Sheep loaded = (Sheep) EntityType.create(EntityTypes.SHEEP, TagValueInput.create(ProblemReporter.DISCARDING,
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
                    feed(playerHolding(h, new ItemStack(Items.AMETHYST_BLOCK)), sheep[0]);
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
        h.assertTrue(drops.stream().anyMatch(st -> st.is(Items.WOOL.white())), "unloaded wool type shears as normal wool: " + drops);
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
        h.assertTrue(!drops.isEmpty() && drops.stream().allMatch(s -> s.is(Items.WOOL.white())), "plain sheep drops white wool, got " + drops);
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
            h.assertTrue(second.stream().allMatch(s -> s.is(Items.WOOL.white())), "sheep returns to normal wool, got " + second);
        } finally {
            ModConfigs.SERVER.infusionUsesOverride.set(old);
        }
        h.succeed();
    }

    private static void dispenserShears(GameTestHelper h) {
        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 3));
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
            h.assertTrue(items.stream().noneMatch(e -> e.getItem().is(Items.WOOL.white())), "no plain wool expected");
        });
    }

    private static void infusionPersists(GameTestHelper h) {
        Sheep sheep = sheep(h);
        feed(playerHolding(h, new ItemStack(Items.EMERALD)), sheep);
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        sheep.saveWithoutId(out);
        CompoundTag tag = out.buildResult();
        Sheep loaded = (Sheep) EntityType.create(EntityTypes.SHEEP, TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag),
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
        insertEnergy(be.getEnergy(), 5_000); // enough for 25 of 100 ticks
        h.runAfterDelay(60, () -> {
            h.assertTrue(be.getStatus() == MachineStatus.NO_ENERGY, "status should be NO_ENERGY, was " + be.getStatus());
            h.assertTrue(be.getProgress() == 25, "progress should pause at 25, was " + be.getProgress());
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
        var sep = SeparationLookup.find(h.getLevel(), new ItemStack(Items.WOOL.red()));
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
        h.setBlock(floor.above(1), Blocks.STAINED_GLASS.red());
        h.setBlock(floor.above(2), Blocks.STAINED_GLASS_PANE.yellow());
        int[] mixed = zpeAt(h, floor).getBeamSections();
        int[] expected = {0, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_COLOR, 1, red, 2, net.minecraft.util.ARGB.average(red, yellow)};
        h.assertTrue(java.util.Arrays.equals(mixed, expected), "red then yellow glass: " + java.util.Arrays.toString(mixed));
        h.assertTrue(zpeAt(h, floor).getBeamColor() == net.minecraft.util.ARGB.average(red, yellow), "the whole beam is drawn in the mixed colour");
        h.setBlock(floor.above(2), Blocks.STAINED_GLASS_PANE.red());
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
        h.setBlock(new BlockPos(2, 2, 2), Blocks.STAINED_GLASS.red());
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
        var pig = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 2, 2));
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
        var golem = h.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(3, 2, 3));
        var pig = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(5, 1, 3));
        var beamKey = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_DAMAGE;
        String beamPlayer = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_PLAYER_NAME;

        // A second beam with a cow in it: the cow burns to death and, as a player kill, drops experience.
        BlockPos cowFloor = new BlockPos(1, 1, 1), cowCeiling = new BlockPos(1, 4, 1);
        h.setBlock(cowFloor, zpe(Direction.UP));
        h.setBlock(cowCeiling, zpe(Direction.DOWN));
        zpeAt(h, cowFloor);
        zpeAt(h, cowCeiling);
        var cow = h.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 1));
        var cowArea = new net.minecraft.world.phys.AABB(h.absolutePos(cowFloor)).inflate(2, 3, 2);

        // Player-only loot: blaze rods only drop for player kills. Credited to the beam's fake player, they do.
        var blaze = h.spawnWithNoFreeWill(EntityTypes.BLAZE, new BlockPos(5, 2, 5));
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
        var pig = h.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 3));
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
        h.assertTrue(any.test(ironWool) && any.test(diamondWool) && !any.test(new ItemStack(Items.WOOL.white())), "no filter matches any resource wool, not plain wool");
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
            h.assertTrue(h.getBlockState(lit).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.GlowPanelBlock.LIT), "its cells light up");
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
        var cow = h.spawn(EntityTypes.COW, new BlockPos(1, 1, 1));
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
        var wool = burner(h, new BlockPos(1, 1, 1), new ItemStack(Items.WOOL.red(), 2));
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
        var full = burner(h, new BlockPos(1, 1, 1), new ItemStack(Items.WOOL.white(), 3));
        var buffer = (net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler) full.getEnergyHandler();
        buffer.set(buffer.getCapacityAsInt());
        // Right-click with wool loads it; cobblestone doesn't go in.
        BlockPos pos = new BlockPos(4, 1, 4);
        var burner = burner(h, pos, ItemStack.EMPTY);
        Player player = playerHolding(h, new ItemStack(Items.WOOL.blue(), 5));
        h.getBlockState(pos).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hitTop(h, pos));
        h.assertTrue(burner.fuel().is(Items.WOOL.blue()) && burner.fuel().getCount() == 5 && player.getMainHandItem().isEmpty(), "right-clicking with wool loads it");
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

    /** 20 Iron Wool: two bales, 1,000 FE each over 40 ticks, and 2 wool left waiting. */
    private static void woolPackerPresses(GameTestHelper h) {
        var be = packer(h, new BlockPos(1, 1, 1), ResourceWoolItem.create(IRON, 20), true);
        var gold = packer(h, new BlockPos(4, 1, 1), ResourceWoolItem.create(ResourcefulSheep.id("gold"), 8), true);
        h.assertFalse(be.canPlaceItem(0, new ItemStack(Items.WOOL.white())), "plain wool can't be baled");
        h.assertTrue(be.canPlaceItem(0, ResourceWoolItem.create(IRON, 1)), "resource wool can");
        h.assertFalse(be.canPlaceItem(1, com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(IRON, 1)), "nothing goes into the bale slot");
        h.runAfterDelay(5, () -> h.assertTrue(h.getBlockState(new BlockPos(1, 1, 1)).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolPackerBlock.ACTIVE),
                "the press runs"));
        h.runAfterDelay(41, () -> {
            h.assertTrue(isBaleOf(be.output(), IRON, 1) && be.input().getCount() == 11, "one bale after 40 ticks, got " + be.output() + " / " + be.input());
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
        // 300,000 FE spread over 900 ticks is 334 FE/t once rounded up, so 300,600 in all.
        h.assertTrue(iron.effectiveEnergy() >= 300_000 && iron.effectiveEnergy() < 300_900 && iron.effectiveTicks() == 900,
                "for about 300,000 FE over 900 ticks, got " + iron.effectiveEnergy() + " / " + iron.effectiveTicks());
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
        var wool = com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.RESOURCE_WOOL.get().getDisplayItems();
        h.assertTrue(main.stream().noneMatch(s -> s.is(ModItems.RESOURCE_WOOL.get())), "no resource wool in the main tab");
        h.assertTrue(main.stream().anyMatch(s -> s.is(ModItems.MECHANICAL_SEPARATOR.get()))
                && main.stream().anyMatch(s -> s.is(ModItems.HUME_ENERGY_CUBE.get())), "machines stay in the main tab");
        h.assertTrue(main.stream().noneMatch(s -> s.is(ModItems.WOOL_BALE.get())), "no bales in the main tab");
        h.assertTrue(!wool.isEmpty() && wool.stream().allMatch(s -> s.is(ModItems.RESOURCE_WOOL.get()) || s.is(ModItems.WOOL_BALE.get())),
                "the wool tab holds only resource wool and bales");
        long pieces = wool.stream().filter(s -> s.is(ModItems.RESOURCE_WOOL.get())).count();
        long bales = wool.stream().filter(s -> s.is(ModItems.WOOL_BALE.get())).count();
        h.assertTrue(pieces >= com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.BUILT_IN_WOOL.size(),
                "every built-in wool type, got " + pieces);
        h.assertTrue(bales == pieces, "a bale of every wool, got " + bales + " for " + pieces);
        h.succeed();
    }
}
