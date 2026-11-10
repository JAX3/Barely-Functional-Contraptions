package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SeparatorInventory;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.MechanicalSeparatorBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FileUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Developer tool: with {@code -Dbarelyfunctionalcontraptions.screenshotHarness=true} the client creates a flat test world,
 * stages sheep/machines, captures screenshots into {@code run/screenshots/} and quits. Used to visually verify the
 * mod's rendering in CI-like environments. It does nothing unless the system property is set.
 */
public final class DevScreenshotHarness {
    private static int tick = -1;
    private static boolean worldRequested;
    private static BlockPos origin = BlockPos.ZERO;

    private DevScreenshotHarness() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean("barelyfunctionalcontraptions.screenshotHarness")) return;
        ResourcefulSheep.LOGGER.warn("Barely Functional Contraptions screenshot harness enabled");
        NeoForge.EVENT_BUS.addListener(DevScreenshotHarness::onScreenOpened);
        NeoForge.EVENT_BUS.addListener(DevScreenshotHarness::onTick);
    }

    private static void onScreenOpened(ScreenEvent.Opening event) {
        if (worldRequested || !(event.getNewScreen() instanceof net.minecraft.client.gui.screens.TitleScreen
                || event.getNewScreen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)) return;
        worldRequested = true;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                LevelSettings settings = new LevelSettings("FR Harness", GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                String folder = FileUtil.findAvailableName(mc.getLevelSource().getBaseDir(), "FR Harness", "");
                mc.createWorldOpenFlows().createFreshLevel(folder, settings, new WorldOptions(42L, false, false),
                        WorldPresets::createTestWorldDimensions, new net.minecraft.client.gui.screens.TitleScreen());
            } catch (IOException e) {
                ResourcefulSheep.LOGGER.error("Harness could not create world", e);
            }
        });
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null || mc.level == null) return;
        tick++;
        mc.gui.toastManager().clear();
        switch (tick) {
            case 60 -> server.execute(() -> setupScene(server));
            case 90 -> command(server, "barelyfunctionalcontraptions generate material slime");
            case 110 -> command(server, "barelyfunctionalcontraptions generate item minecraft:amethyst_block");
            case 130 -> command(server, "barelyfunctionalcontraptions generate item minecraft:iron_block");
            case 140 -> command(server, "barelyfunctionalcontraptions generate item minecraft:emerald_block");
            case 150 -> server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HONEY_BLOCK));
                server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "barelyfunctionalcontraptions generate hand");
            });
            case 175 -> command(server, "barelyfunctionalcontraptions generate remove slime");
            case 190 -> setHudHidden(mc, true);
            case 200 -> shot(mc, "01_world_sheep");
            case 210 -> server.execute(() -> look(server, 180, 28));
            case 240 -> shot(mc, "02_separators");
            case 250 -> {
                setHudHidden(mc, false);
                command(server, "gamemode survival @a");
                mc.execute(() -> mc.gui.setScreen(new InventoryScreen(mc.player)));
            }
            case 290 -> shot(mc, "03_inventory_jei");
            case 300 -> {
                mc.gui.setScreen(null);
                server.execute(() -> openSeparator(server));
            }
            case 360 -> shot(mc, "04_separator_gui");
            case 370 -> {
                mc.player.closeContainer();
                if (ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showCategory(true);
                }
            }
            case 410 -> shot(mc, "05_jei_infusion");
            case 420 -> {
                mc.gui.setScreen(null);
                if (ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showCategory(false);
                }
            }
            case 460 -> {
                shot(mc, "06_jei_separation");
                if (ModList.get().isLoaded("jei")) {
                    ResourcefulSheep.LOGGER.warn("Harness JEI report: {}", com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.separatorReport());
                }
            }
            case 470 -> {
                mc.gui.setScreen(null);
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %d %d %d 200 22", origin.getX() - 1, origin.getY() + 1, origin.getZ() - 2)));
            }
            case 500 -> { setHudHidden(mc, true); shot(mc, "07_sheep_closeup"); }
            case 502 -> {
                setHudHidden(mc, false);
                // Look at the diamond sheep (Jade shows its infusion).
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %d.5 %d %d.5 180 30", origin.getX(), origin.getY(), origin.getZ() - 3)));
            }
            case 520 -> shot(mc, "09_jade_sheep");
            case 525 -> server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    String.format(java.util.Locale.ROOT, "tp @a %d.5 %d %d.5 180 25", origin.getX() + 2, origin.getY(), origin.getZ() - 6)));
            case 545 -> shot(mc, "10_jade_separator");
            case 550 -> {
                setHudHidden(mc, false);
                mc.gui.setScreen(net.neoforged.neoforge.client.gui.modlist.ModListScreen.create(null));
            }
            case 560 -> shot(mc, "08_mod_list");
            case 565 -> {
                mc.gui.setScreen(null);
                server.execute(() -> openGenerator(server));
            }
            case 600 -> shot(mc, "11_generator_gui");
            case 605 -> {
                mc.player.closeContainer();
                server.execute(() -> openAutomaticShears(server));
            }
            case 630 -> shot(mc, "12_automatic_shears_gui");
            case 635 -> {
                mc.player.closeContainer();
                setHudHidden(mc, true);
                command(server, "gamemode spectator @a"); // hover above the sheep instead of falling
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %d.5 %d %d.5 -99 24", origin.getX(), origin.getY() + 3, origin.getZ() + 1)));
            }
            case 660 -> shot(mc, "13_automatic_shears");
            case 670 -> server.execute(() -> machineRow(server));
            case 700 -> shot(mc, "14_machine_fronts");
            case 705 -> server.execute(() -> reservoirScene(server));
            case 745 -> shot(mc, "15_fluid_reservoir");
            case 750 -> server.execute(() -> openReservoir(server));
            case 775 -> { setHudHidden(mc, false); }
            case 790 -> shot(mc, "16_fluid_reservoir_gui");
            case 800 -> {
                mc.player.closeContainer();
                setHudHidden(mc, false);
                ResourcefulSheep.LOGGER.warn("Screenshot harness finished; client definitions loaded: {}", SheepResourceRegistry.client().size());
                mc.stop();
            }
            default -> {}
        }
    }

    private static void setHudHidden(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
    }

    private static void command(MinecraftServer server, String cmd) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd));
    }

    private static void look(MinecraftServer server, float yaw, float pitch) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %d %d %d %.1f %.1f", origin.getX(), origin.getY(), origin.getZ(), yaw, pitch));
    }

    /** Generator, separator and shears side by side, each idle (left) and running (right), floating in the sky. */
    private static void machineRow(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos row = origin.offset(0, 12, -5);
        var sepState = ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH);
        var shearsState = ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH);
        var genState = com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH);
        BlockPos[] positions = {row.offset(-5, 0, 0), row.offset(-4, 0, 0), row.offset(-1, 0, 0), row.offset(0, 0, 0), row.offset(3, 0, 0), row.offset(4, 0, 0)};
        var states = new net.minecraft.world.level.block.state.BlockState[]{genState, genState, sepState, sepState, shearsState, shearsState};
        for (int i = 0; i < positions.length; i++) level.setBlock(positions[i], states[i], 3);
        // Running copies: fuel for the generator, wool + a creative generator behind the separator, FE for the shears.
        if (level.getBlockEntity(positions[1]) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity generator) {
            generator.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT,
                    new ItemStack(Items.RED_MUSHROOM, 32));
            generator.getTank().fillInternal(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 8000);
        }
        level.setBlock(positions[3].north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(positions[3]) instanceof MechanicalSeparatorBlockEntity be) fill(be, 64);
        if (level.getBlockEntity(positions[5]) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity shearsBe) {
            shearsBe.getEnergy().set(30_000);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 8", row.getX() + 0.0, row.getY() + 1, row.getZ() + 6.2));
    }

    private static BlockPos reservoirPos;

    /** A Fluid Reservoir on a 2x2 pool in the sky, watering a Mycelial Generator beside it, plus one on dry stone. */
    private static void reservoirScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(-1, 20, -6);
        for (int x = -1; x <= 4; x++) {
            for (int z = -1; z <= 3; z++) {
                level.setBlock(base.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
                boolean pool = x >= 0 && x <= 1 && z >= 0 && z <= 1;
                level.setBlock(base.offset(x, 1, z), pool ? Blocks.WATER.defaultBlockState()
                        : x <= 2 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            }
        }
        reservoirPos = base.offset(0, 2, 0);
        level.setBlock(reservoirPos, ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(reservoirPos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity be) {
            be.getTank().generate(5_500);
            be.setItem(0, new ItemStack(Items.BUCKET, 3));
        }
        BlockPos generatorPos = reservoirPos.west();
        level.setBlock(generatorPos, com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(generatorPos) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity generator) {
            generator.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT,
                    new ItemStack(Items.RED_MUSHROOM, 32));
            generator.getTank().fillInternal(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 100_000);
        }
        // A second reservoir on dry stone, half full, to show the water level in its windows.
        BlockPos dry = base.offset(3, 1, 1);
        level.setBlock(dry, ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(dry) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity be) {
            be.getTank().generate(5_000);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 150 32", base.getX() + 3.5, base.getY() + 4, base.getZ() + 5.5));
    }

    private static void openReservoir(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        if (player.level().getBlockEntity(reservoirPos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity be) {
            player.openMenu(be, reservoirPos);
        }
    }

    private static void setupScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        // Clear a stage in front of the player (north).
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(8, 5, 2))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(8, -1, 2))) {
            level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
        // Infused sheep in a row, side-on to the camera, plus a plain one for comparison.
        Object[][] sheep = {
                {"iron", DyeColor.WHITE, -4}, {"gold", DyeColor.WHITE, -2}, {"diamond", DyeColor.LIGHT_BLUE, 0},
                {"red_mushroom", DyeColor.WHITE, 2}, {null, DyeColor.WHITE, 4}};
        for (Object[] s : sheep) {
            Sheep sh = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sh.snapTo(o.getX() + (int) s[2] + 0.5, o.getY(), o.getZ() - 5 + 0.5, 90, 0);
            sh.setYHeadRot(90);
            sh.setYBodyRot(90);
            sh.setNoAi(true);
            sh.setColor((DyeColor) s[1]);
            if (s[0] != null) {
                SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id((String) s[0])), 9, 0));
            }
            level.addFreshEntity(sh);
        }
        // Two separators: one idle, one running (facing the camera).
        BlockPos idle = o.offset(-2, 0, -8), active = o.offset(2, 0, -8);
        level.setBlock(idle, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(active, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(o.offset(0, 0, -8), ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.WEST), 3);
        // The running separator is powered by a Mycelial Generator on its east face (Barely Functional Contraptions integration).
        BlockPos generatorPos = o.offset(3, 0, -8);
        level.setBlock(generatorPos, com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(generatorPos) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity generator) {
            generator.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT,
                    new ItemStack(Items.RED_MUSHROOM, 32));
            generator.getTank().fillInternal(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 8000);
        }
        level.setBlock(o.offset(5, 0, -3), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(active) instanceof MechanicalSeparatorBlockEntity be) {
            fill(be, 64);
        }
        // Automatic Shears beside the camera, facing west, with a gold sheep and a plain sheep in its 3x3 area.
        BlockPos shearsPos = o.offset(6, 0, 0);
        level.setBlock(shearsPos, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.WEST), 3);
        if (level.getBlockEntity(shearsPos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity shearsBe) {
            shearsBe.getEnergy().set(30_000); // it runs on FE now
        }
        Object[][] shearSheep = {{"gold", 4, 0}, {null, 4, -1}, {"diamond", 3, 1}};
        for (Object[] s : shearSheep) {
            Sheep sh = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sh.snapTo(o.getX() + (int) s[1] + 0.5, o.getY(), o.getZ() + (int) s[2] + 0.5, 90, 0);
            sh.setNoAi(true);
            sh.setColor(DyeColor.WHITE);
            if (s[0] != null) SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id((String) s[0])), 9, 0));
            level.addFreshEntity(sh);
        }
        // Inventory: every wool type plus the machine.
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.MECHANICAL_SEPARATOR.get(), 4));
        player.getInventory().add(new ItemStack(ModItems.CREATIVE_GENERATOR.get(), 1));
        player.getInventory().add(new ItemStack(ModItems.AUTOMATIC_SHEARS.get(), 2));
        player.getInventory().add(new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIAL_GENERATOR.get(), 4));
        player.getInventory().add(new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()));
        player.getInventory().add(new ItemStack(Items.RED_MUSHROOM_BLOCK, 8));
        SheepResourceRegistry.server().all().forEach(r -> player.getInventory().add(ResourceWoolItem.create(r.id(), 16)));
        player.getInventory().add(new ItemStack(Items.IRON_BLOCK, 8));
        player.getInventory().add(new ItemStack(Items.SHEARS));
        look(server, 180, 12);
    }

    private static void fill(MechanicalSeparatorBlockEntity be, int wool) {
        try (Transaction tx = Transaction.openRoot()) {
            for (int i = 0; i < 200; i++) be.getEnergy().insert(1000, tx);
            tx.commit();
        }
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(ResourcefulSheep.id("iron"), wool));
    }

    private static void openSeparator(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        BlockPos pos = origin.offset(2, 0, -8);
        if (player.level().getBlockEntity(pos) instanceof MechanicalSeparatorBlockEntity be) {
            player.openMenu(be, pos);
        }
    }

    private static void openGenerator(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        BlockPos pos = origin.offset(3, 0, -8);
        if (player.level().getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity be) {
            player.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
    }

    private static void openAutomaticShears(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        BlockPos pos = origin.offset(6, 0, 0);
        if (player.level().getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity be) {
            player.openMenu(be, pos); // the menu needs the position, exactly like a right-click on the block
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1,
                msg -> ResourcefulSheep.LOGGER.warn("Harness screenshot {}: {}", name, msg.getString()));
    }
}
