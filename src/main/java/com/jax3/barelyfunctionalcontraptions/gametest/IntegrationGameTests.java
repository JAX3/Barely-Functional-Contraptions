package com.jax3.barelyfunctionalcontraptions.gametest;

import com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions;
import com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.GeneratorState;
import com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler;
import com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelRegistry;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SeparatorInventory;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * GameTests for the parts where the two modules meet. Run with {@code ./gradlew runGameTestServer} together with
 * the module tests.
 */
public final class IntegrationGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, BarelyFunctionalContraptions.MOD_ID);

    private static final Identifier STRUCTURE = BarelyFunctionalContraptions.id("test_platform");
    private static final Identifier IRON = BarelyFunctionalContraptions.id("iron");
    private static final Identifier RED_MUSHROOM = BarelyFunctionalContraptions.id("red_mushroom");
    private static final Identifier BROWN_MUSHROOM = BarelyFunctionalContraptions.id("brown_mushroom");

    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("integration_mycelial_generator_powers_separator", IntegrationGameTests::mycelialGeneratorPowersSeparator);
        TESTS.put("integration_advancements", IntegrationGameTests::advancements);
        TESTS.put("fluid_pipe_connects_to_pipes_and_tanks", IntegrationGameTests::pipeConnects);
        TESTS.put("fluid_pipe_reservoir_waters_two_generators", IntegrationGameTests::pipeWatersGenerators);
        TESTS.put("fluid_pipe_respects_transfer_rate", IntegrationGameTests::pipeRate);
        TESTS.put("fluid_pipe_break_splits_network", IntegrationGameTests::pipeBreakSplits);
        TESTS.put("integration_mycelial_generator_powers_separator_through_cables", IntegrationGameTests::generatorThroughCables);
        TESTS.put("integration_mushroom_infuses_sheep", IntegrationGameTests::mushroomInfusesSheep);
        TESTS.put("integration_mushroom_wool_fuels_generator", IntegrationGameTests::mushroomWoolFuelsGenerator);
        TESTS.put("integration_mycelial_generator_powers_automatic_shears", IntegrationGameTests::generatorPowersAutomaticShears);
        TESTS.put("shift_placing_faces_machines_away", IntegrationGameTests::shiftPlacingFacesAway);
        TESTS.put("integration_fluid_reservoir_waters_generator", IntegrationGameTests::reservoirWatersGenerator);
        TESTS.put("integration_wireless_power_to_separator_and_shears", IntegrationGameTests::wirelessPower);
        TESTS.put("integration_wrench_links_machines", IntegrationGameTests::wrenchLinksMachines);
        TESTS.put("integration_wireless_link_rules", IntegrationGameTests::wirelessLinkRules);
        TESTS.put("integration_generator_status_lists_wireless_links", IntegrationGameTests::statusListsWirelessLinks);
        TESTS.put("integration_generator_charges_energy_cube", IntegrationGameTests::generatorChargesCube);
        TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
    }

    private IntegrationGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(IntegrationGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(BarelyFunctionalContraptions.id("integration"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, BarelyFunctionalContraptions.id(name));
            event.registerTest(BarelyFunctionalContraptions.id(name), new FunctionGameTestInstance(fn, new TestData<>(env, STRUCTURE, 600, 0, true)));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static MycelialGeneratorBlockEntity generator(GameTestHelper h, BlockPos pos, ItemStack fuel, int water) {
        h.setBlock(pos, com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get());
        MycelialGeneratorBlockEntity generator = h.getBlockEntity(pos, MycelialGeneratorBlockEntity.class);
        if (!fuel.isEmpty()) {
            generator.getInventory().setStackInSlot(GeneratorItemHandler.FUEL_SLOT, fuel);
        }
        if (water > 0) {
            generator.getTank().fillInternal(FluidResource.of(Fluids.WATER), water);
        }
        return generator;
    }

    /** A separator facing north, so its back (south) face accepts energy. */
    private static MechanicalSeparatorBlockEntity separator(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState());
        return h.getBlockEntity(pos, MechanicalSeparatorBlockEntity.class);
    }

    // ------------------------------------------------------------------ tests

    /** A Mycelial Generator placed against a Mechanical Separator powers it with no cables in between. */
    private static void mycelialGeneratorPowersSeparator(GameTestHelper h) {
        MechanicalSeparatorBlockEntity separator = separator(h, new BlockPos(3, 1, 3));
        MycelialGeneratorBlockEntity generator = generator(h, new BlockPos(3, 1, 4), new ItemStack(Items.BROWN_MUSHROOM, 8), 4000);
        separator.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        h.succeedWhen(() -> {
            h.assertTrue(generator.getState() == GeneratorState.RUNNING, "generator should be running, was " + generator.getState());
            h.assertTrue(separator.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON),
                    "separator should finish iron wool on mycelial power");
            h.assertTrue(separator.getInventory().getStack(SeparatorInventory.SECONDARY).is(Items.STRING), "string output expected");
        });
    }

    /** A Mycelial Generator runs a Mechanical Separator three Energy Cables away. */
    private static void generatorThroughCables(GameTestHelper h) {
        MycelialGeneratorBlockEntity generator = generator(h, new BlockPos(0, 1, 2), new ItemStack(Items.BROWN_MUSHROOM, 8), 4000);
        MechanicalSeparatorBlockEntity separator = separator(h, new BlockPos(4, 1, 2));
        var cableBlock = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.ENERGY_CABLE.get();
        for (int x = 1; x <= 3; x++) {
            BlockPos pos = new BlockPos(x, 1, 2);
            h.setBlock(pos, cableBlock.withConnections(cableBlock.defaultBlockState(), h.getLevel(), h.absolutePos(pos)));
        }
        separator.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        h.succeedWhen(() -> {
            h.assertTrue(generator.getState() == GeneratorState.RUNNING, "generator should be running, was " + generator.getState());
            h.assertTrue(separator.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON),
                    "separator should finish iron wool on power carried by the cables");
        });
    }

    /** A Mycelial Generator against the back of an unpowered Automatic Shears powers it, and it shears the sheep in front. */
    private static void generatorPowersAutomaticShears(GameTestHelper h) {
        BlockPos shearsPos = new BlockPos(3, 1, 1);
        h.setBlock(shearsPos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, net.minecraft.core.Direction.SOUTH));
        var shears = h.getBlockEntity(shearsPos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.class);
        MycelialGeneratorBlockEntity generator = generator(h, new BlockPos(3, 1, 0), new ItemStack(Items.BROWN_MUSHROOM, 8), 4000);
        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 3));
        sheep.setNoAi(true);
        sheep.setColor(DyeColor.WHITE);
        h.assertTrue(shears.getEnergy().getEnergy() == 0, "starts unpowered");
        h.succeedWhen(() -> {
            h.assertTrue(generator.getState() == GeneratorState.RUNNING, "generator running, was " + generator.getState());
            h.assertTrue(sheep.isSheared(), "sheep sheared on mycelial power");
            boolean stored = false;
            for (int i = 0; i < shears.getContainerSize(); i++) stored |= shears.getItem(i).is(Items.WOOL.white());
            h.assertTrue(stored, "wool stored in the shears");
        });
    }

    /**
     * Every machine with a front faces the player when placed normally, and away from them when placed while holding
     * Shift. Placement goes through the real block item, the same path as a right-click.
     */
    private static void shiftPlacingFacesAway(GameTestHelper h) {
        record Machine(net.minecraft.world.item.Item item, net.minecraft.world.level.block.Block block,
                       net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> facing) {}
        List<Machine> machines = List.of(
                new Machine(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIAL_GENERATOR.get(),
                        com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get(),
                        com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING),
                new Machine(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MECHANICAL_SEPARATOR.get(),
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.MECHANICAL_SEPARATOR.get(),
                        com.jax3.barelyfunctionalcontraptions.sheep.blocks.MechanicalSeparatorBlock.FACING),
                new Machine(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.AUTOMATIC_SHEARS.get(),
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.AUTOMATIC_SHEARS.get(),
                        com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING));
        int x = 1;
        for (Machine machine : machines) {
            for (boolean sneaking : new boolean[]{false, true}) {
                BlockPos target = new BlockPos(x++, 1, 3);
                h.setBlock(target.below(), net.minecraft.world.level.block.Blocks.STONE); // solid floor to click on
                Player player = h.makeMockPlayer(GameType.SURVIVAL);
                player.setYRot(0.0F); // looking south (+Z)
                player.setShiftKeyDown(sneaking);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(machine.item()));
                // Click the top of the block below the target, as a player placing it on the floor would.
                BlockPos floor = h.absolutePos(target.below());
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0, 0.5, 0),
                        net.minecraft.core.Direction.UP, floor, false);
                var result = player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                h.assertTrue(result.consumesAction(), machine.block() + " should be placed, got " + result);
                h.assertBlockPresent(machine.block(), target);
                var expected = sneaking ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
                var facing = h.getBlockState(target).getValue(machine.facing());
                h.assertTrue(facing == expected, machine.block() + (sneaking ? " placed while sneaking" : " placed normally")
                        + " should face " + expected + ", faces " + facing);
            }
        }
        h.succeed();
    }

    /**
     * The advancement tab: every advancement loads with the right parent, and the three special ones are granted by
     * linking two generators with the wrench, by Automatic Shears shearing a sheep near you, and by dying to a Zero
     * Point beam (and not by dying any other way).
     */
    private static void advancements(GameTestHelper h) {
        var server = h.getLevel().getServer();
        String[][] tree = {
                {"root", null}, {"mycelial_generator", "root"}, {"mycelium_wrench", "mycelial_generator"},
                {"link_generators", "mycelium_wrench"}, {"mechanical_separator", "root"}, {"automatic_shears", "root"},
                {"auto_shear", "automatic_shears"}, {"fluid_reservoir", "root"}, {"hume_energy_cube", "root"},
                {"zero_point_energy_module", "root"}, {"zero_point_death", "zero_point_energy_module"}, {"energy_cable", "root"}, {"fluid_pipe", "root"}, {"item_pipe", "root"}};
        for (String[] entry : tree) {
            var advancement = server.getAdvancements().get(com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep.id("contraptions/" + entry[0]));
            h.assertTrue(advancement != null, "advancement " + entry[0] + " is loaded");
            var parent = advancement.value().parent();
            h.assertTrue(entry[1] == null ? parent.isEmpty()
                            : parent.map(id -> id.equals(com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep.id("contraptions/" + entry[1]))).orElse(false),
                    entry[0] + " has parent " + entry[1] + ", got " + parent);
            h.assertTrue(advancement.value().display().isPresent(), entry[0] + " shows in the tab");
        }

        // A server player that never logs in (so no network traffic) and ignores the wrench's chat messages.
        var player = new net.minecraft.server.level.ServerPlayer(server, h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "adv-test"), net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override
            public void sendSystemMessage(net.minecraft.network.chat.Component message, boolean overlay) {}

            @Override
            public void sendSystemMessage(net.minecraft.network.chat.Component message) {}
        };
        BlockPos shearsPos = new BlockPos(3, 1, 0);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(shearsPos.above())));
        String link = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.LINK_GENERATORS;
        String shear = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.AUTO_SHEAR;
        String death = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.ZERO_POINT_DEATH;
        h.assertFalse(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, link), "a new player hasn't linked generators");

        // Fungal Network: link two generators with the wrench.
        BlockPos genA = new BlockPos(0, 1, 4), genB = new BlockPos(6, 1, 4);
        generator(h, genA, ItemStack.EMPTY, 0);
        generator(h, genB, ItemStack.EMPTY, 0);
        ItemStack wrench = new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        for (BlockPos pos : new BlockPos[] {genA, genB}) {
            wrench.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)),
                            net.minecraft.core.Direction.UP, h.absolutePos(pos), false)));
        }
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, link), "linking generators grants Fungal Network");

        // Don't Cross the Streams: only a Zero Point beam death counts.
        var beam = h.getLevel().damageSources().source(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity.BEAM_DAMAGE);
        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.onDeath(player, h.getLevel().damageSources().inFire());
        h.assertFalse(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, death), "an ordinary fire death doesn't count");
        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.onDeath(player, beam);
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, death), "a beam death grants Don't Cross the Streams");

        // Shear Genius: powered Automatic Shears shear a sheep while the player stands next to them.
        h.setBlock(shearsPos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, net.minecraft.core.Direction.SOUTH));
        var shears = h.getBlockEntity(shearsPos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.class);
        shears.getEnergy().set(shears.getEnergy().getCapacityAsInt());
        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 2));
        sheep.setNoAi(true);
        h.assertFalse(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, shear), "not before the shears run");
        // The shears look for players in the level's player list; add ours (without logging in) until the shears run.
        h.getLevel().players().add(player);
        try {
            shears.shearArea(h.getLevel(), h.absolutePos(shearsPos), net.minecraft.core.Direction.SOUTH);
        } finally {
            h.getLevel().players().remove(player);
        }
        h.assertTrue(sheep.isSheared(), "the shears shear the sheep");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, shear), "watching it grants Shear Genius");

        // Someone far away doesn't get it.
        var farAway = new net.minecraft.server.level.ServerPlayer(server, h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "adv-test-far"), net.minecraft.server.level.ClientInformation.createDefault());
        farAway.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(shearsPos)).add(40, 0, 0));
        Sheep sheep2 = h.spawn(EntityTypes.SHEEP, new BlockPos(4, 1, 2));
        sheep2.setNoAi(true);
        h.getLevel().players().add(farAway);
        try {
            shears.shearArea(h.getLevel(), h.absolutePos(shearsPos), net.minecraft.core.Direction.SOUTH);
        } finally {
            h.getLevel().players().remove(farAway);
        }
        h.assertTrue(sheep2.isSheared(), "the second sheep is sheared");
        h.assertFalse(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(farAway, shear), "40 blocks away is too far");
        h.succeed();
    }

    /** Mushrooms (the Mycelial Generator's fuel) are a built-in sheep resource. */
    private static void mushroomInfusesSheep(GameTestHelper h) {
        SheepResourceRegistry registry = SheepResourceRegistry.server();
        h.assertTrue(registry.byId(RED_MUSHROOM).isPresent(), "red mushroom resource should be loaded");
        h.assertTrue(registry.byId(BROWN_MUSHROOM).isPresent(), "brown mushroom resource should be loaded");

        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 3));
        sheep.setNoAi(true);
        sheep.setColor(DyeColor.WHITE);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_MUSHROOM, 2));
        player.interactOn(sheep, InteractionHand.MAIN_HAND, sheep.position());

        h.assertTrue(SheepInfusionEvents.getInfusedResource(sheep).map(SheepResource::id).map(RED_MUSHROOM::equals).orElse(false),
                "feeding a red mushroom should infuse the sheep with red mushroom");
        h.assertTrue(player.getMainHandItem().getCount() == 1, "one mushroom should be consumed");
        h.succeed();
    }

    /**
     * The full loop: mushroom wool goes through a separator powered by a Mycelial Generator, and the mushrooms that come
     * out are accepted as fuel by a second generator, which starts running on them.
     */
    private static void mushroomWoolFuelsGenerator(GameTestHelper h) {
        MechanicalSeparatorBlockEntity separator = separator(h, new BlockPos(3, 1, 3));
        generator(h, new BlockPos(3, 1, 4), new ItemStack(Items.BROWN_MUSHROOM, 8), 4000);
        // A separate generator (not touching the first) with water but no fuel yet.
        MycelialGeneratorBlockEntity consumer = generator(h, new BlockPos(1, 1, 1), ItemStack.EMPTY, 4000);
        separator.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(RED_MUSHROOM, 1));

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(separator.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RED_MUSHROOM),
                        "separator should turn red mushroom wool into red mushrooms"))
                .thenExecute(() -> {
                    ItemStack mushrooms = separator.getInventory().getStack(SeparatorInventory.PRIMARY).copy();
                    h.assertTrue(mushrooms.getCount() >= 2 && mushrooms.getCount() <= 4, "expected 2-4 mushrooms, got " + mushrooms.getCount());
                    h.assertTrue(FuelRegistry.isFuel(mushrooms), "separated mushrooms must be generator fuel");
                    separator.getInventory().setStack(SeparatorInventory.PRIMARY, ItemStack.EMPTY);
                    consumer.getInventory().setStackInSlot(GeneratorItemHandler.FUEL_SLOT, mushrooms);
                })
                .thenWaitUntil(() -> h.assertTrue(consumer.getState() == GeneratorState.RUNNING,
                        "generator fuelled with separated mushrooms should run, was " + consumer.getState()))
                .thenSucceed();
    }

    /** A Fluid Reservoir on a 2x2 pool pushes its water straight into a Mycelial Generator placed beside it. */
    private static void reservoirWatersGenerator(GameTestHelper h) {
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 3; z++) {
                boolean inside = x >= 1 && x <= 2 && z >= 1 && z <= 2;
                h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
                h.setBlock(new BlockPos(x, 1, z), inside ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        BlockPos reservoirPos = new BlockPos(1, 2, 1);
        h.setBlock(reservoirPos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.FLUID_RESERVOIR.get());
        BlockPos generatorPos = new BlockPos(1, 2, 0);
        h.setBlock(generatorPos, com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get());
        var generator = h.getBlockEntity(generatorPos, com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity.class);
        h.assertTrue(generator.getTank().getFluidAmount() == 0, "generator starts dry");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(generator.getTank().getFluidAmount() >= 2_000,
                        "the generator fills with water from the reservoir, at " + generator.getTank().getFluidAmount()))
                .thenSucceed();
    }

    // ------------------------------------------------------------------ fluid pipes

    /** Places a conduit the way a player would: every side worked out from what is around it. */
    private static void conduit(GameTestHelper h, BlockPos pos, com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock block) {
        h.setBlock(pos, block.withConnections(block.defaultBlockState(), h.getLevel(), h.absolutePos(pos)));
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidPipeBlockEntity pipe(GameTestHelper h, BlockPos pos) {
        conduit(h, pos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.FLUID_PIPE.get());
        return h.getBlockEntity(pos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidPipeBlockEntity.class);
    }

    private static com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection side(GameTestHelper h, BlockPos pos, net.minecraft.core.Direction dir) {
        return h.getBlockState(pos).getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.SIDES.get(dir));
    }

    /** A 2x2 water pool with a Fluid Reservoir on one corner of it, at (1, 2, 1). */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity reservoirOnPool(GameTestHelper h) {
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 3; z++) {
                boolean inside = x >= 1 && x <= 2 && z >= 1 && z <= 2;
                h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.STONE);
                h.setBlock(new BlockPos(x, 1, z), inside ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        BlockPos reservoirPos = new BlockPos(1, 2, 1);
        h.setBlock(reservoirPos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.FLUID_RESERVOIR.get());
        return h.getBlockEntity(reservoirPos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity.class);
    }

    /** Pipes join pipes, plug into tanks, and ignore Energy Cables, Hume Energy Cubes and stone. */
    private static void pipeConnects(GameTestHelper h) {
        var CABLE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.CABLE;
        var MACHINE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.MACHINE;
        var NONE = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CableConnection.NONE;
        var E = net.minecraft.core.Direction.EAST;
        var W = net.minecraft.core.Direction.WEST;
        var U = net.minecraft.core.Direction.UP;
        var N = net.minecraft.core.Direction.NORTH;
        h.setBlock(new BlockPos(1, 1, 2), com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.FLUID_RESERVOIR.get());
        h.setBlock(new BlockPos(2, 2, 2), com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.HUME_ENERGY_CUBE.get());
        conduit(h, new BlockPos(2, 1, 1), com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.ENERGY_CABLE.get());
        var a = pipe(h, new BlockPos(2, 1, 2));
        var b = pipe(h, new BlockPos(3, 1, 2));
        h.assertTrue(side(h, new BlockPos(2, 1, 2), W) == MACHINE, "plugs into the reservoir");
        h.assertTrue(side(h, new BlockPos(2, 1, 2), E) == CABLE && side(h, new BlockPos(3, 1, 2), W) == CABLE, "joins the next pipe");
        h.assertTrue(side(h, new BlockPos(2, 1, 2), U) == NONE, "ignores the energy cube (no fluid connection)");
        h.assertTrue(side(h, new BlockPos(2, 1, 2), N) == NONE, "doesn't join the energy cable");
        h.assertTrue(side(h, new BlockPos(2, 1, 1), net.minecraft.core.Direction.SOUTH) == NONE, "the energy cable doesn't join the pipe either");
        h.assertTrue(a.network() == b.network() && a.network().size() == 2 && a.network().endpoints().size() == 1,
                "one network of 2 pipes with the reservoir plugged in");
        h.setBlock(new BlockPos(4, 1, 2), com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get());
        h.assertTrue(side(h, new BlockPos(3, 1, 2), E) == MACHINE && a.network().endpoints().size() == 2, "plugs into a generator placed later");
        h.succeed();
    }

    /** A reservoir on a pool fills two Mycelial Generators through a branching run of pipes, sharing the water. */
    private static void pipeWatersGenerators(GameTestHelper h) {
        reservoirOnPool(h);
        var first = generator(h, new BlockPos(4, 2, 4), ItemStack.EMPTY, 0);
        var second = generator(h, new BlockPos(2, 2, 5), ItemStack.EMPTY, 0);
        for (BlockPos pos : List.of(new BlockPos(1, 2, 2), new BlockPos(1, 2, 3), new BlockPos(1, 2, 4), new BlockPos(2, 2, 4), new BlockPos(3, 2, 4))) {
            pipe(h, pos);
        }
        h.succeedWhen(() -> {
            h.assertTrue(first.getTank().getFluidAmount() >= 1_000 && second.getTank().getFluidAmount() >= 1_000,
                    "both generators get water through the pipes: " + first.getTank().getFluidAmount() + " / " + second.getTank().getFluidAmount());
        });
    }

    /** With transferRate = 100 a pipe network never moves more than 100 mB in a tick. */
    private static void pipeRate(GameTestHelper h) {
        var rateValue = com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.pipeTransferRate;
        int old = rateValue.get();
        rateValue.set(100);
        var reservoir = reservoirOnPool(h);
        reservoir.getTank().generate(8_000);
        var target = generator(h, new BlockPos(1, 2, 4), ItemStack.EMPTY, 0);
        pipe(h, new BlockPos(1, 2, 2));
        pipe(h, new BlockPos(1, 2, 3));
        int[] start = new int[1];
        h.runAfterDelay(2, () -> start[0] = target.getTank().getFluidAmount());
        h.runAfterDelay(12, () -> {
            try {
                int gained = target.getTank().getFluidAmount() - start[0];
                h.assertTrue(gained > 0 && gained <= 10 * 100, "10 ticks at 100 mB/t, gained " + gained);
            } finally {
                rateValue.set(old);
            }
            h.succeed();
        });
    }

    /** Breaking the middle pipe splits the network: the generator stops filling. */
    private static void pipeBreakSplits(GameTestHelper h) {
        reservoirOnPool(h);
        var target = generator(h, new BlockPos(1, 2, 5), ItemStack.EMPTY, 0);
        var near = pipe(h, new BlockPos(1, 2, 2));
        pipe(h, new BlockPos(1, 2, 3));
        var far = pipe(h, new BlockPos(1, 2, 4));
        int[] stopped = new int[1];
        h.runAfterDelay(30, () -> {
            h.assertTrue(target.getTank().getFluidAmount() > 0, "filling before the break");
            h.setBlock(new BlockPos(1, 2, 3), net.minecraft.world.level.block.Blocks.AIR);
            h.assertTrue(near.network() != far.network() && near.network().size() == 1, "split into two networks of 1");
            stopped[0] = target.getTank().getFluidAmount();
        });
        h.runAfterDelay(40, () -> {
            h.assertTrue(target.getTank().getFluidAmount() == stopped[0], "no water crosses the gap");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ wireless power (Barely Functional Wrench)

    private static net.minecraft.core.GlobalPos global(GameTestHelper h, BlockPos pos) {
        return net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), h.absolutePos(pos));
    }

    /**
     * A running generator in one corner, linked with the wrench to a separator and an Automatic Shears that it does
     * not touch: both get its power, the separator finishes iron wool and the shears shear the sheep.
     */
    private static void wirelessPower(GameTestHelper h) {
        MycelialGeneratorBlockEntity generator = generator(h, new BlockPos(0, 1, 6), new ItemStack(Items.BROWN_MUSHROOM, 8), 4000);
        BlockPos separatorPos = new BlockPos(6, 1, 6);
        MechanicalSeparatorBlockEntity separator = separator(h, separatorPos);
        separator.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        BlockPos shearsPos = new BlockPos(3, 1, 0);
        h.setBlock(shearsPos, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, net.minecraft.core.Direction.SOUTH));
        var shears = h.getBlockEntity(shearsPos, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity.class);
        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 2));
        sheep.setNoAi(true);
        sheep.setColor(DyeColor.WHITE);

        var server = h.getLevel().getServer();
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, global(h, new BlockPos(0, 1, 6)), global(h, separatorPos)).success(),
                "generator -> separator link");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, global(h, shearsPos), global(h, new BlockPos(0, 1, 6))).success(),
                "shears -> generator link (either order)");
        h.succeedWhen(() -> {
            h.assertTrue(generator.getState() == GeneratorState.RUNNING, "generator running, was " + generator.getState());
            h.assertTrue(separator.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON), "separator finishes iron wool on wireless power");
            h.assertTrue(sheep.isSheared(), "shears shear the sheep on wireless power, shears FE " + shears.getEnergy().getEnergy() + " status " + shears.getStatus() + " gen FE " + generator.getEnergyStorage().getAmountAsInt());
        });
    }

    /** The wrench itself: machine then generator links them; shift-clicking the machine removes the link. */
    private static void wrenchLinksMachines(GameTestHelper h) {
        BlockPos generatorPos = new BlockPos(0, 1, 0);
        generator(h, generatorPos, ItemStack.EMPTY, 0);
        BlockPos separatorPos = new BlockPos(5, 1, 5);
        separator(h, separatorPos);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wrench = new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        var item = (com.jax3.barelyfunctionalcontraptions.mycelial.item.MyceliumWrenchItem) wrench.getItem();
        java.util.function.Function<BlockPos, net.minecraft.world.item.context.UseOnContext> click = pos -> new net.minecraft.world.item.context.UseOnContext(
                player, InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos)), net.minecraft.core.Direction.UP, h.absolutePos(pos), false));
        var data = com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinkData.get(h.getLevel().getServer());

        // The machine is handled before its GUI would open; the generator passes the click to useOn.
        h.assertTrue(item.onItemUseFirst(wrench, click.apply(separatorPos)) != net.minecraft.world.InteractionResult.PASS, "wrench handles the separator click");
        h.assertTrue(global(h, separatorPos).equals(wrench.get(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModDataComponents.SELECTED_GENERATOR.get())),
                "separator selected");
        h.assertTrue(item.onItemUseFirst(wrench, click.apply(generatorPos)) == net.minecraft.world.InteractionResult.PASS, "generators go through useOn");
        item.useOn(click.apply(generatorPos));
        h.assertTrue(data.isLinked(global(h, generatorPos), global(h, separatorPos)), "linked by the wrench");
        h.assertFalse(wrench.has(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModDataComponents.SELECTED_GENERATOR.get()), "selection cleared after linking");
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.network.ManualLinkData.get(h.getLevel().getServer()).count(global(h, generatorPos)) == 0,
                "a machine is not a generator-network link");

        player.setShiftKeyDown(true);
        item.onItemUseFirst(wrench, click.apply(separatorPos));
        h.assertFalse(data.isLinked(global(h, generatorPos), global(h, separatorPos)), "shift-click on the machine removes its links");
        h.succeed();
    }

    /** Two machines can't link; distance and per-generator limits apply; breaking a machine drops its link. */
    private static void wirelessLinkRules(GameTestHelper h) {
        var server = h.getLevel().getServer();
        BlockPos generatorPos = new BlockPos(0, 1, 0);
        MycelialGeneratorBlockEntity generator = generator(h, generatorPos, ItemStack.EMPTY, 0);
        BlockPos a = new BlockPos(2, 1, 0), b = new BlockPos(6, 1, 6);
        separator(h, a);
        separator(h, b);
        h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, global(h, a), global(h, b))
                == com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.Result.NOT_LINKABLE, "two machines can't be linked");
        var cfg = com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig.MAX_LINK_DISTANCE;
        var limit = com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig.MAX_POWER_LINKS;
        int oldDistance = cfg.get(), oldLimit = limit.get();
        try {
            cfg.set(4);
            h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, global(h, generatorPos), global(h, b))
                    == com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.Result.TOO_FAR, "8.5 blocks is too far with a 4 block limit");
            limit.set(1);
            h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, global(h, generatorPos), global(h, a)).success(), "near one links");
            h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, global(h, generatorPos), global(h, a))
                    == com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.Result.ALREADY_LINKED, "no duplicate links");
            cfg.set(64);
            h.assertTrue(com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, global(h, generatorPos), global(h, b))
                    == com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.Result.AT_LIMIT, "one machine per generator with max_power_links_per_generator = 1");
        } finally {
            cfg.set(oldDistance);
            limit.set(oldLimit);
        }
        // Break the linked machine: the generator drops the link the next time it sends power.
        h.setBlock(a, net.minecraft.world.level.block.Blocks.AIR);
        generator.getEnergyStorage().setEnergy(10_000);
        var data = com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinkData.get(server);
        h.succeedWhen(() -> h.assertTrue(data.machinesOf(global(h, generatorPos)).isEmpty(), "link to the broken separator removed"));
    }

    /** The generator GUI's status lists its wireless machines, which are active, and survives the network round trip. */
    private static void statusListsWirelessLinks(GameTestHelper h) {
        var server = h.getLevel().getServer();
        BlockPos generatorPos = new BlockPos(0, 1, 0);
        MycelialGeneratorBlockEntity generator = generator(h, generatorPos, ItemStack.EMPTY, 0);
        BlockPos near = new BlockPos(2, 1, 0), far = new BlockPos(6, 1, 6);
        separator(h, near);
        h.setBlock(far, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.AUTOMATIC_SHEARS.get());
        h.assertTrue(generator.createStatus().powerLinks().isEmpty(), "no wireless links yet");
        var PL = (java.util.function.BiFunction<BlockPos, BlockPos, Boolean>) (a, b) ->
                com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, global(h, a), global(h, b)).success();
        h.assertTrue(PL.apply(generatorPos, near) && PL.apply(generatorPos, far), "both linked");

        var status = generator.createStatus();
        h.assertTrue(status.powerLinks().size() == 2 && status.activePowerLinks() == 2, "2 of 2 active, got " + status.powerLinks());
        h.assertTrue(status.maxPowerLinks() == 8, "max 8 by default, got " + status.maxPowerLinks());
        h.assertTrue(status.powerLinks().get(0).blockId().equals("barelyfunctionalcontraptions:mechanical_separator")
                && status.powerLinks().get(0).pos().equals(h.absolutePos(near)), "first is the separator: " + status.powerLinks().get(0));
        h.assertTrue(status.powerLinks().get(1).blockId().equals("barelyfunctionalcontraptions:automatic_shears"), "second is the shears");

        var cfg = com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig.MAX_LINK_DISTANCE;
        int old = cfg.get();
        try {
            cfg.set(4);
            var limited = generator.createStatus();
            h.assertTrue(limited.activePowerLinks() == 1 && limited.powerLinks().get(0).active() && !limited.powerLinks().get(1).active(),
                    "the far shears are inactive with a 4 block limit: " + limited.powerLinks());
        } finally {
            cfg.set(old);
        }

        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        com.jax3.barelyfunctionalcontraptions.mycelial.menu.GeneratorStatus.STREAM_CODEC.encode(buf, status);
        var decoded = com.jax3.barelyfunctionalcontraptions.mycelial.menu.GeneratorStatus.STREAM_CODEC.decode(buf);
        h.assertTrue(decoded.equals(status), "status survives the trip to the client");
        h.assertTrue(buf.readableBytes() == 0, "nothing left unread");
        h.succeed();
    }

    /** A running Mycelial Generator touching a Hume Energy Cube charges it, and through it a separator on the far side. */
    private static void generatorChargesCube(GameTestHelper h) {
        MycelialGeneratorBlockEntity generator = generator(h, new BlockPos(1, 1, 3), new ItemStack(Items.BROWN_MUSHROOM, 8), 4000);
        h.setBlock(new BlockPos(2, 1, 3), com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.HUME_ENERGY_CUBE.get());
        h.setBlock(new BlockPos(3, 1, 3), com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks.HUME_ENERGY_CUBE.get());
        var cubeEnd = h.getBlockEntity(new BlockPos(3, 1, 3), com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity.class);
        MechanicalSeparatorBlockEntity separator = separator(h, new BlockPos(4, 1, 3));
        separator.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(IRON, 1));
        h.succeedWhen(() -> {
            h.assertTrue(generator.getState() == GeneratorState.RUNNING, "generator running, was " + generator.getState());
            h.assertTrue(cubeEnd.group().size() == 2, "two cubes, one group");
            h.assertTrue(separator.getInventory().getStack(SeparatorInventory.PRIMARY).is(Items.RAW_IRON),
                    "the separator finishes iron wool on power passed through the cubes");
        });
    }
}
