package com.jax3.functionalresources.gametest;

import com.jax3.functionalresources.FunctionalResources;
import com.jax3.functionalresources.mycelial.blockentity.GeneratorState;
import com.jax3.functionalresources.mycelial.blockentity.MycelialGeneratorBlockEntity;
import com.jax3.functionalresources.mycelial.capability.GeneratorItemHandler;
import com.jax3.functionalresources.mycelial.fuel.FuelRegistry;
import com.jax3.functionalresources.sheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.functionalresources.sheep.blockentity.SeparatorInventory;
import com.jax3.functionalresources.sheep.entity.SheepInfusionEvents;
import com.jax3.functionalresources.sheep.items.ResourceWoolItem;
import com.jax3.functionalresources.sheep.resource.SheepResource;
import com.jax3.functionalresources.sheep.resource.SheepResourceRegistry;
import java.util.LinkedHashMap;
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
            DeferredRegister.create(Registries.TEST_FUNCTION, FunctionalResources.MOD_ID);

    private static final Identifier STRUCTURE = FunctionalResources.id("test_platform");
    private static final Identifier IRON = FunctionalResources.id("iron");
    private static final Identifier RED_MUSHROOM = FunctionalResources.id("red_mushroom");
    private static final Identifier BROWN_MUSHROOM = FunctionalResources.id("brown_mushroom");

    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("integration_mycelial_generator_powers_separator", IntegrationGameTests::mycelialGeneratorPowersSeparator);
        TESTS.put("integration_mushroom_block_infuses_sheep", IntegrationGameTests::mushroomBlockInfusesSheep);
        TESTS.put("integration_mushroom_wool_fuels_generator", IntegrationGameTests::mushroomWoolFuelsGenerator);
        TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> fn));
    }

    private IntegrationGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(IntegrationGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(FunctionalResources.id("integration"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, FunctionalResources.id(name));
            event.registerTest(FunctionalResources.id(name), new FunctionGameTestInstance(fn, new TestData<>(env, STRUCTURE, 600, 0, true)));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static MycelialGeneratorBlockEntity generator(GameTestHelper h, BlockPos pos, ItemStack fuel, int water) {
        h.setBlock(pos, com.jax3.functionalresources.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get());
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
        h.setBlock(pos, com.jax3.functionalresources.sheep.registry.ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState());
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

    /** Mushroom blocks (which Mycelial Generators grow as huge mushrooms) are a built-in sheep resource. */
    private static void mushroomBlockInfusesSheep(GameTestHelper h) {
        SheepResourceRegistry registry = SheepResourceRegistry.server();
        h.assertTrue(registry.byId(RED_MUSHROOM).isPresent(), "red mushroom resource should be loaded");
        h.assertTrue(registry.byId(BROWN_MUSHROOM).isPresent(), "brown mushroom resource should be loaded");

        Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 1, 3));
        sheep.setNoAi(true);
        sheep.setColor(DyeColor.WHITE);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_MUSHROOM_BLOCK, 2));
        player.interactOn(sheep, InteractionHand.MAIN_HAND, sheep.position());

        h.assertTrue(SheepInfusionEvents.getInfusedResource(sheep).map(SheepResource::id).map(RED_MUSHROOM::equals).orElse(false),
                "feeding a red mushroom block should infuse the sheep with red mushroom");
        h.assertTrue(player.getMainHandItem().getCount() == 1, "one mushroom block should be consumed");
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
}
