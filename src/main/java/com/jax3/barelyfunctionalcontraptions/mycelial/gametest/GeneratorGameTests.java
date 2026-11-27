package com.jax3.barelyfunctionalcontraptions.mycelial.gametest;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.GeneratorState;
import com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * In-game tests. Run with {@code gradlew runGameTestServer}. They assume the default server config.
 */
public final class GeneratorGameTests {
    private static final double EPSILON = 1.0E-6;

    private GeneratorGameTests() {
    }

    /** {@code GameTestHelper#assertTrue} takes a Component message since 1.21.5. */
    private static void check(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, Component.literal(message));
    }

    private static MycelialGeneratorBlockEntity place(GameTestHelper helper, BlockPos pos, ItemStack fuel, int water) {
        helper.setBlock(pos, ModBlocks.MYCELIAL_GENERATOR.get());
        MycelialGeneratorBlockEntity generator = helper.getBlockEntity(pos, MycelialGeneratorBlockEntity.class);
        if (!fuel.isEmpty()) {
            generator.getInventory().setStackInSlot(GeneratorItemHandler.FUEL_SLOT, fuel);
        }
        if (water > 0) {
            generator.getTank().fillInternal(FluidResource.of(Fluids.WATER), water);
        }
        return generator;
    }

    /** Verification 1 + 6: one generator consumes a mushroom and water and produces FE. */
    public static void singleGeneratorProducesEnergy(GameTestHelper helper) {
        MycelialGeneratorBlockEntity generator = place(helper, new BlockPos(2, 1, 2), new ItemStack(Items.RED_MUSHROOM, 2), 1000);
        helper.runAfterDelay(41, () -> {
            check(helper, generator.getState() == GeneratorState.RUNNING, "Generator should be running, was " + generator.getState());
            check(helper, generator.getInventory().getStackInSlot(GeneratorItemHandler.FUEL_SLOT).getCount() == 1,
                    "Exactly one mushroom should have been consumed");
            double expected = ServerConfig.get(ServerConfig.BASE_GENERATION) * 1.25D;
            check(helper, Math.abs(generator.getLastExactOutput() - expected) < EPSILON,
                    "Expected " + expected + " FE/t, got " + generator.getLastExactOutput());
            check(helper, generator.getEnergyStorage().getAmountAsInt() > 0, "Energy should have been generated");
            int per = ServerConfig.get(ServerConfig.WATER_PER_INTERVAL);
            int water = generator.getTank().getFluidAmount();
            check(helper, water <= 1000 - per && water >= 1000 - 3 * per,
                    "About two water consumptions should have happened, tank has " + water);
            helper.succeed();
        });
    }

    /** Verification 5: without water the generator idles and keeps its mushroom. */
    public static void generatorWithoutWaterStops(GameTestHelper helper) {
        MycelialGeneratorBlockEntity generator = place(helper, new BlockPos(2, 1, 2), new ItemStack(Items.RED_MUSHROOM, 1), 0);
        helper.runAfterDelay(20, () -> {
            check(helper, generator.getState() == GeneratorState.NO_WATER, "Expected NO_WATER, got " + generator.getState());
            check(helper, generator.getEnergyStorage().getAmountAsInt() == 0, "No energy may be produced without water");
            check(helper, generator.getInventory().getStackInSlot(GeneratorItemHandler.FUEL_SLOT).getCount() == 1,
                    "The mushroom must not be consumed");
            helper.succeed();
        });
    }

    /** Verification 8: a guaranteed attempt converts dirt with air above into mycelium. */
    public static void myceliumConvertsDirt(GameTestHelper helper) {
        BlockPos dirt = new BlockPos(4, 1, 2);
        helper.setBlock(dirt, Blocks.DIRT);
        MycelialGeneratorBlockEntity generator = place(helper, new BlockPos(2, 1, 2), new ItemStack(Items.RED_MUSHROOM, 4), 4000);
        generator.setOwnChance(1.0D);
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.MYCELIUM, dirt));
    }

    /** A converted block far enough from the generator sprouts a huge mushroom when the chance is 100%. */
    public static void convertedBlockSproutsHugeMushroom(GameTestHelper helper) {
        double previousChance = ServerConfig.HUGE_MUSHROOM_CHANCE.get();
        ServerConfig.HUGE_MUSHROOM_CHANCE.set(1.0D);
        BlockPos dirt = new BlockPos(5, 1, 5);
        BlockPos stem = dirt.above();
        helper.setBlock(dirt, Blocks.DIRT);
        MycelialGeneratorBlockEntity generator = place(helper, new BlockPos(1, 1, 1), new ItemStack(Items.RED_MUSHROOM, 4), 4000);
        generator.setOwnChance(1.0D);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.MYCELIUM, dirt);
            helper.assertBlockPresent(Blocks.MUSHROOM_STEM, stem);
            ServerConfig.HUGE_MUSHROOM_CHANCE.set(previousChance);
        });
    }

    /** Verification 14: machine state survives a save/load round trip. */
    public static void stateSurvivesSaveAndLoad(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        MycelialGeneratorBlockEntity generator = place(helper, pos, new ItemStack(Items.BROWN_MUSHROOM, 3), 2000);
        generator.setOwnChance(0.125D);
        helper.runAfterDelay(30, () -> {
            var registries = helper.getLevel().registryAccess();
            CompoundTag saved = generator.saveWithFullMetadata(registries);
            MycelialGeneratorBlockEntity copy = new MycelialGeneratorBlockEntity(generator.getBlockPos(), generator.getBlockState());
            try (ProblemReporter.ScopedCollector problems = new ProblemReporter.ScopedCollector(copy.problemPath(), MycelialPower.LOGGER)) {
                copy.loadWithComponents(TagValueInput.create(problems, registries, saved));
            }
            check(helper, copy.getEnergyStorage().getAmountAsInt() == generator.getEnergyStorage().getAmountAsInt(), "Energy lost");
            check(helper, copy.getBurnRemaining() == generator.getBurnRemaining(), "Burn progress lost");
            check(helper, copy.getTank().getFluidAmount() == generator.getTank().getFluidAmount(), "Water lost");
            check(helper, Math.abs(copy.getOwnChance() - generator.getOwnChance()) < EPSILON, "Mycelium chance lost");
            check(helper, ItemStack.matches(copy.getInventory().getStackInSlot(0), generator.getInventory().getStackInSlot(0)),
                    "Inventory lost");
            helper.succeed();
        });
    }

    /** Generators touching each other don't boost each other: each makes the base rate times its fuel, as on its own. */
    public static void touchingGeneratorsStandAlone(GameTestHelper helper) {
        MycelialGeneratorBlockEntity a = place(helper, new BlockPos(2, 1, 2), new ItemStack(Items.BROWN_MUSHROOM, 4), 4000);
        MycelialGeneratorBlockEntity b = place(helper, new BlockPos(3, 1, 2), new ItemStack(Items.BROWN_MUSHROOM, 4), 4000);
        MycelialGeneratorBlockEntity c = place(helper, new BlockPos(4, 1, 2), new ItemStack(Items.BROWN_MUSHROOM, 4), 4000);
        helper.succeedWhen(() -> {
            double base = ServerConfig.get(ServerConfig.BASE_GENERATION);
            for (MycelialGeneratorBlockEntity generator : new MycelialGeneratorBlockEntity[]{a, b, c}) {
                check(helper, generator.getState() == GeneratorState.RUNNING, "all three run");
                check(helper, Math.abs(generator.getLastExactOutput() - base) < EPSILON,
                        "each makes the base " + base + " FE/t, got " + generator.getLastExactOutput());
            }
        });
    }
}
