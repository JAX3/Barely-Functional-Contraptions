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

/**
 * In-world integration tests, run with {@code ./gradlew runGameTestServer} (a real dedicated-server environment,
 * which also proves no client classes are loaded on the server).
 */
public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ResourcefulSheep.MOD_ID);
    private static final Identifier STRUCTURE = ResourcefulSheep.id("test_platform");
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, Consumer<GameTestHelper>> RELOAD_TESTS = new java.util.LinkedHashMap<>();

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
        RELOAD_TESTS.put("generated_resource_keeps_infusion", ModGameTests::generatedResourceKeepsInfusion);
        TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
        RELOAD_TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
    }

    private ModGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(ResourcefulSheep.id("sheep_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, ResourcefulSheep.id(name));
            event.registerTest(ResourcefulSheep.id(name), new FunctionGameTestInstance(fn, new TestData<>(env, STRUCTURE, 400, 0, true)));
        }
        // Tests that run /barelyfunctionalcontraptions generate (which reloads data packs) get their own batch, so the reload
        // never happens in the middle of the other tests.
        Holder<TestEnvironmentDefinition<?>> reloadEnv = event.registerEnvironment(ResourcefulSheep.id("sheep_reload"), new TestEnvironmentDefinition.AllOf());
        for (String name : RELOAD_TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, ResourcefulSheep.id(name));
            event.registerTest(ResourcefulSheep.id(name), new FunctionGameTestInstance(fn, new TestData<>(reloadEnv, STRUCTURE, 1200, 0, true)));
        }
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
        for (String machine : List.of("mycelial_generator", "mechanical_separator", "automatic_shears", "fluid_reservoir", "hume_energy_cube", "zero_point_energy_module")) {
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
        h.assertTrue(menu.getLink() == com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink.LINKED && menu.getLinkDistance() == 3 && menu.facesUp(), "menu shows the link");
        h.assertTrue(menu.getGenerating() == low.getGenerating() && menu.getPairOutput() == low.getPairOutput(), "menu shows generation");
        h.assertTrue(menu.getCapacity() == 2 * ModConfigs.getInt(ModConfigs.SERVER.zpeCapacity), "menu shows the pair's pooled buffer");
        var highMenu = (com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu) high.createMenu(1, player.getInventory(), player);
        h.assertFalse(highMenu.facesUp(), "ceiling module's menu knows it points down");
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
        h.assertTrue(names.contains("slime") && names.contains("wheat") && names.contains("bone_meal"), "vanilla storage-block materials discovered: " + names);
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
        for (String n : List.of("iron", "gold", "copper", "redstone", "lapis", "coal", "diamond", "emerald", "netherite")) {
            h.assertTrue(dupNames.contains(n), "generated " + n + " should be flagged as a duplicate: " + dupNames);
        }
        for (String n : List.of("slime", "wheat", "bone_meal")) {
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

    // ------------------------------------------------------------------ Hume Energy Cube

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

    /** The main tab holds the machines and tools only; every resource wool is in the Resource Wool tab. */
    private static void creativeTabs(GameTestHelper h) {
        var level = h.getLevel();
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
        var main = com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.MAIN.get().getDisplayItems();
        var wool = com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.RESOURCE_WOOL.get().getDisplayItems();
        h.assertTrue(main.stream().noneMatch(s -> s.is(ModItems.RESOURCE_WOOL.get())), "no resource wool in the main tab");
        h.assertTrue(main.stream().anyMatch(s -> s.is(ModItems.MECHANICAL_SEPARATOR.get()))
                && main.stream().anyMatch(s -> s.is(ModItems.HUME_ENERGY_CUBE.get())), "machines stay in the main tab");
        h.assertTrue(!wool.isEmpty() && wool.stream().allMatch(s -> s.is(ModItems.RESOURCE_WOOL.get())), "the wool tab holds only resource wool");
        h.assertTrue(wool.size() >= com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.BUILT_IN_WOOL.size(),
                "every built-in wool type, got " + wool.size());
        h.succeed();
    }
}
