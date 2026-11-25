package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.compat.McTest;
import com.jax3.barelyfunctionalcontraptions.compat.McClient;
import com.jax3.barelyfunctionalcontraptions.compat.Vanilla;
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
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FileUtil;
import net.minecraft.world.entity.EntitySpawnReason;
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
    private static boolean confirmedExperimental;

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
        String existing = System.getProperty("barelyfunctionalcontraptions.screenshotHarness.world");
        if (existing != null && !existing.isBlank()) {
            // Add to a world that's already there (the test world) instead of starting a fresh one.
            mc.execute(() -> mc.createWorldOpenFlows().openWorld(existing, () -> ResourcefulSheep.LOGGER.error("Harness could not open world {}", existing)));
            return;
        }
        mc.execute(() -> {
            try {
                LevelSettings settings = new LevelSettings("FR Harness", GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                String folder = FileUtil.findAvailableName(mc.getLevelSource().getBaseDir(), "FR Harness", "");
                mc.createWorldOpenFlows().createFreshLevel(folder, settings, new WorldOptions(42L, false, false),
                        McTest::testWorldDimensions, new net.minecraft.client.gui.screens.TitleScreen());
            } catch (IOException e) {
                ResourcefulSheep.LOGGER.error("Harness could not create world", e);
            }
        });
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        // Opening an existing test world (superflat test dimensions) first asks to confirm its experimental settings.
        if (mc.player == null && McClient.screen(mc) != null && System.getProperty("barelyfunctionalcontraptions.screenshotHarness.world") != null
                && !confirmedExperimental) {
            for (var child : McClient.screen(mc).children()) {
                if (child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().equals("I Know What I'm Doing!")) {
                    confirmedExperimental = true;
                    button.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                    ResourcefulSheep.LOGGER.info("Harness: confirmed the world's experimental settings");
                    break;
                }
            }
        }
        if (mc.player == null || server == null || mc.level == null) return;
        tick++;
        McClient.clearToasts(mc);
        if ("meter_setups".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            meterSetupsTick(mc, server);
            return;
        }
        if ("interactions".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            interactionsTick(mc, server);
            return;
        }
        if ("new_blocks".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            newBlocksTick(mc, server);
            return;
        }
        if ("keep_contents".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            keepContentsTick(mc, server);
            return;
        }
        if ("showcase".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            showcaseTick(mc, server);
            return;
        }
        if ("altar".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            altarTick(mc, server);
            return;
        }
        if ("wool_packer".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            woolPackerTick(mc, server);
            return;
        }
        if ("wool_burner".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            woolBurnerTick(mc, server);
            return;
        }
        if ("world_tour".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            worldTourTick(mc, server);
            return;
        }
        if ("powered_furnace".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            poweredFurnaceTick(mc, server);
            return;
        }
        if ("wrench_dismantle".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            // A survival player with the wrench hits a Powered Furnace (with Raw Iron in it) once, through the real
            // attack key: it should break at once and both go into the inventory.
            switch (tick) {
                case 60 -> server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    origin = player.blockPosition();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode survival @a");
                    player.getInventory().clearContent();
                    player.getInventory().setItem(0, new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()));
                    player.getInventory().setSelectedSlot(0);
                    BlockPos at = origin.offset(0, 0, -2);
                    player.level().setBlock(at, ModBlocks.POWERED_FURNACE.get().defaultBlockState(), 3);
                    if (player.level().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity be) {
                        be.setItem(0, new ItemStack(Items.RAW_IRON, 7));
                    }
                });
                case 70 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 35", origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5));
                case 90 -> net.minecraft.client.KeyMapping.click(mc.options.keyAttack.getKey());
                case 110 -> server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    ResourcefulSheep.LOGGER.warn("DISMANTLE block now {}, furnaces {}, raw iron {}",
                            player.level().getBlockState(origin.offset(0, 0, -2)).getBlock(),
                            player.getInventory().countItem(ModItems.POWERED_FURNACE.get()), player.getInventory().countItem(Items.RAW_IRON));
                });
                case 115 -> mc.execute(() -> McClient.setScreen(mc, new InventoryScreen(mc.player)));
                case 135 -> shot(mc, "wrench_dismantle");
                case 140 -> mc.stop();
                default -> {}
            }
            return;
        }
        if ("creative_sections".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            switch (tick) {
                case 60 -> command(server, "gamemode creative @a");
                case 80 -> openCreativeTab(mc, com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.MAIN.get());
                case 110 -> shot(mc, "28_creative_tab_main");
                case 112 -> clickCreativeRow(mc, 0); // a real click on the Power banner
                case 130 -> shot(mc, "28_creative_tab_main_folded");
                case 132 -> clickCreativeRow(mc, 0);
                case 140 -> openCreativeTab(mc, com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.RESOURCE_WOOL.get());
                case 170 -> shot(mc, "27_creative_tab_wool");
                case 172 -> clickCreativeRow(mc, 0); // Resource Wool
                case 190 -> shot(mc, "27_creative_tab_wool_folded");
                case 192 -> clickCreativeRow(mc, 1); // then Wool Bales, now on the second row: the eggs show
                case 210 -> shot(mc, "27_creative_tab_eggs");
                case 212 -> clickCreativeRow(mc, 0);
                case 214 -> clickCreativeRow(mc, 1);
                case 215 -> mc.stop();
                default -> {}
            }
            return;
        }
        if ("woollen_armor".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            woollenArmorTick(mc, server);
            return;
        }
        if ("chiller".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            chillerTick(mc, server);
            return;
        }
        if ("snow_sheep".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            snowSheepTick(mc, server);
            return;
        }
        if ("ore_line".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            oreLineTick(mc, server);
            return;
        }
        if ("zero_point_spin".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            zeroPointSpinTick(mc, server);
            return;
        }
        if ("thermal_reaction_chamber".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            thermalTick(mc, server);
            return;
        }
        if ("sheep_treadmill".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            sheepTreadmillTick(mc, server);
            return;
        }
        if ("hume_field".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            humeFieldTick(mc, server);
            return;
        }
        if ("echo_chamber".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            echoChamberTick(mc, server);
            return;
        }
        if ("crate_controller".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            crateControllerTick(mc, server);
            return;
        }
        if ("crate".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            crateTick(mc, server);
            return;
        }
        if ("status_panel".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            statusPanelTick(mc, server);
            return;
        }
        if ("herd_counter".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            herdCounterTick(mc, server);
            return;
        }
        if ("grass_seeder".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            grassSeederTick(mc, server);
            return;
        }
        if ("static_generator".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            staticGeneratorTick(mc, server);
            return;
        }
        if ("list_command".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            listCommandTick(mc);
            return;
        }
        if ("square_world".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            squareWorldTick(mc, server);
            return;
        }
        if ("survey_world".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            if (tick == 40) {
                command(server, "forceload add -96 -96 200 64");
                command(server, "forceload add 201 -96 520 64");
            }
            if (tick == 300) server.execute(() -> surveyWorld(server));
            if (tick == 302) server.execute(() -> {
                ServerLevel level = server.overworld();
                StringBuilder col = new StringBuilder();
                for (int y = -64; y < 40; y++) {
                    var st = level.getBlockState(new BlockPos(-10, y, 0));
                    if (!st.isAir()) col.append(y).append('=').append(st.getBlock().getDescriptionId()).append(' ');
                }
                ResourcefulSheep.LOGGER.warn("SURVEY column -10,0: {}", col);
                for (var e : level.getAllEntities()) {
                    if (!(e instanceof ServerPlayer)) ResourcefulSheep.LOGGER.warn("SURVEY entity {} at {}", net.minecraft.world.entity.EntityType.getKey(e.getType()).getPath(), e.blockPosition());
                }
            });
            if (tick == 320) mc.stop();
            return;
        }
        if ("extend_world".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            extendWorldTick(mc, server);
            return;
        }
        if ("blaze_furnace".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            blazeFurnaceTick(mc, server);
            return;
        }
        if ("power_blocks".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            powerBlocksTick(mc, server);
            return;
        }
        if ("upgrades".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            upgradesTick(mc, server);
            return;
        }
        if ("fleece_trawler".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            fleeceTrawlerTick(mc, server);
            return;
        }
        if ("blue_steel".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            blueSteelTick(mc, server);
            return;
        }
        if ("block_wool".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            blockWoolTick(mc, server);
            return;
        }
        if ("themed_sheep".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            themedSheepTick(mc, server);
            return;
        }
        switch (tick) {
            case 60 -> server.execute(() -> setupScene(server));
            case 90 -> command(server, "bfc generate material slime");
            case 110 -> command(server, "bfc generate item minecraft:amethyst_block");
            case 130 -> command(server, "bfc generate item minecraft:iron_block");
            case 140 -> command(server, "bfc generate item minecraft:emerald_block");
            case 150 -> server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HONEY_BLOCK));
                server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "bfc generate hand");
            });
            case 175 -> command(server, "bfc generate remove slime");
            case 190 -> setHudHidden(mc, true);
            case 200 -> shot(mc, "01_world_sheep");
            case 210 -> server.execute(() -> look(server, 180, 28));
            case 240 -> shot(mc, "02_separators");
            case 250 -> {
                setHudHidden(mc, false);
                command(server, "gamemode survival @a");
                mc.execute(() -> McClient.setScreen(mc, new InventoryScreen(mc.player)));
            }
            case 290 -> shot(mc, "03_inventory_jei");
            case 300 -> {
                McClient.setScreen(mc, null);
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
                McClient.setScreen(mc, null);
                if (ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showCategory(false);
                }
            }
            case 460 -> {
                shot(mc, "06_jei_separation");
                if (ModList.get().isLoaded("jei")) {
                    ResourcefulSheep.LOGGER.warn("Harness JEI report: {}", com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.separatorReport());
                    ResourcefulSheep.LOGGER.warn("Harness JEI tag report: {}", com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.tagReport());
                }
            }
            case 470 -> {
                McClient.setScreen(mc, null);
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
                McClient.setScreen(mc, McTest.modListScreen(null));
            }
            case 553 -> {
                // Click the first entry (this mod, sorted A-Z) so the info panel shows its banner.
                var handle = mc.getWindow().handle();
                double scale = mc.getWindow().getGuiScale();
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, handle, 70 * scale, 45 * scale);
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseButton(mc, handle, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), 1);
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseButton(mc, handle, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), 0);
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, handle, 2, 2);
            }
            case 560 -> shot(mc, "08_mod_list");
            case 565 -> {
                McClient.setScreen(mc, null);
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
                setHudHidden(mc, true);
            }
            case 1040 -> {
                mc.player.closeContainer();
                server.execute(() -> cubeScene(server));
            }
            case 1075 -> server.execute(() -> cubeMessage(server));
            case 1080 -> { setHudHidden(mc, false); }
            case 1085 -> shot(mc, "21_energy_cubes");
            case 1090 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos pos = cubeBase.offset(1, 0, 0);
                // Stand next to the cube first: menus close when the player is too far away to use the block.
                player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 3.5);
                if (player.level().getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity be) {
                    player.openMenu(be, pos);
                }
            });
            case 1125 -> shot(mc, "22_energy_cube_gui");
            case 1127 -> {
                mc.player.closeContainer();
                setHudHidden(mc, true);
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 160 -55", cubeBase.getX() + 14.5, cubeBase.getY() + 1, cubeBase.getZ() + 3.5)));
            }
            case 1145 -> shot(mc, "23_energy_cubes_below");
            case 1147 -> server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 75", cubeBase.getX() + 5.5, cubeBase.getY() + 7, cubeBase.getZ() + 9.5)));
            case 1165 -> shot(mc, "24_energy_cube_corners");
            case 1167 -> server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 150 35", cubeBase.getX() + 7.0, cubeBase.getY() + 3, cubeBase.getZ() + 17.0)));
            case 1185 -> shot(mc, "25_energy_cube_folds");
            // Crafting recipes, shown in a real crafting table: one every 25 ticks.
            case 1190, 1215, 1240, 1265, 1290, 1315, 1340, 1365 -> {
                int index = (tick - 1190) / 25;
                if (index > 0) mc.player.closeContainer();
                setHudHidden(mc, false);
                server.execute(() -> showRecipe(server, RECIPES[index]));
            }
            case 1210, 1235, 1260, 1285, 1310, 1335, 1360, 1385 -> shot(mc, "recipe_" + RECIPES[(tick - 1210) / 25]);
            case 1390 -> {
                mc.player.closeContainer();
                setHudHidden(mc, true);
                server.execute(() -> creativeScene(server));
            }
            case 1435 -> shot(mc, "26_creative_generator");
            case 1438 -> command(server, "gamemode creative @a");
            case 1445 -> openCreativeTab(mc, com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.RESOURCE_WOOL.get());
            case 1460 -> shot(mc, "27_creative_tab_wool");
            case 1465 -> openCreativeTab(mc, com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.MAIN.get());
            case 1480 -> shot(mc, "28_creative_tab_main");
            case 1485 -> {
                McClient.setScreen(mc, null);
                server.execute(() -> zeroPointScene(server));
            }
            case 1530 -> shot(mc, "29_zero_point_modules");
            case 1532 -> {
                setHudHidden(mc, false);
                command(server, "clear @a");
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 27", zeroPointBase.getX() + 0.5, zeroPointBase.getY(), zeroPointBase.getZ() + 3.0)));
            }
            case 1555 -> shot(mc, "30_zero_point_jade");
            case 1558 -> server.execute(() -> openZeroPointGui(server));
            case 1580 -> shot(mc, "31_zero_point_gui");
            case 1582 -> {
                mc.player.closeContainer();
                server.execute(() -> server.overworld().setBlock(zeroPointBase.above(3), Blocks.STONE.defaultBlockState(), 3));
            }
            case 1595 -> server.execute(() -> openZeroPointGui(server));
            case 1615 -> shot(mc, "32_zero_point_gui_blocked");
            case 1617 -> {
                // Leave the saved test world with a working pair.
                mc.player.closeContainer();
                server.execute(() -> server.overworld().setBlock(zeroPointBase.above(3), Blocks.AIR.defaultBlockState(), 3));
            }
            case 1619 -> {
                // Looking up at the ceiling module, to check the beam reaches its emitter.
                setHudHidden(mc, true);
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 -50", zeroPointBase.getX() + 0.5, zeroPointBase.getY() + 2, zeroPointBase.getZ() + 3.5)));
            }
            case 1640 -> shot(mc, "33_zero_point_beam_top");
            case 1642 -> server.execute(() -> {
                // Stained glass recolours the beam like a beacon's: red, then yellow mixing in to make orange.
                ServerLevel level = server.overworld();
                level.setBlock(zeroPointBase.above(2), Vanilla.stainedGlass(DyeColor.RED).defaultBlockState(), 3);
                level.setBlock(zeroPointBase.above(4), Vanilla.stainedGlass(DyeColor.YELLOW).defaultBlockState(), 3);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), String.format(java.util.Locale.ROOT,
                        "tp @a %.1f %d %.1f 180 -12", zeroPointBase.getX() + 0.5, zeroPointBase.getY() + 1, zeroPointBase.getZ() + 8.5));
            });
            case 1670 -> shot(mc, "34_zero_point_coloured_beam");
            case 1672 -> server.execute(() -> {
                // A pig wanders into the beam: it is hurt and set on fire.
                var pig = Vanilla.PIG.create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (pig != null) {
                    pig.setNoAi(true);
                    pig.snapTo(zeroPointBase.getX() + 0.5, zeroPointBase.getY() + 0.5, zeroPointBase.getZ() + 0.5, 90, 0);
                    server.overworld().addFreshEntity(pig);
                }
            });
            case 1679 -> shot(mc, "35_zero_point_beam_burns");
            case 1681 -> {
                // The advancement tab: everything earned except the hidden "Don't Cross the Streams".
                command(server, "advancement grant @a from barelyfunctionalcontraptions:contraptions/root");
                command(server, "advancement revoke @a only barelyfunctionalcontraptions:contraptions/zero_point_death");
            }
            case 1695 -> {
                var advancements = mc.player.connection.getAdvancements();
                McClient.setScreen(mc, new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(advancements));
                advancements.setSelectedTab(advancements.get(ResourcefulSheep.id("contraptions/root")), true);
            }
            case 1712 -> McClient.clearToasts(mc);
            case 1715 -> shot(mc, "36_advancements");
            case 1720 -> {
                McClient.setScreen(mc, null);
                server.execute(() -> cableScene(server));
            }
            case 1765 -> shot(mc, "37_energy_cables");
            case 1767 -> {
                setHudHidden(mc, false);
                McClient.clearChat(mc); // the advancement announcements from earlier
                command(server, "clear @a");
                // Standing in front of the first cable, looking down at it, so Jade shows the network.
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 29", cableBase.getX() + 1.5, cableBase.getY(), cableBase.getZ() + 2.5)));
            }
            case 1790 -> shot(mc, "38_energy_cable_jade");
            case 1792 -> server.execute(() -> showRecipe(server, "energy_cable"));
            case 1812 -> shot(mc, "recipe_energy_cable");
            case 1815 -> {
                mc.player.closeContainer();
                setHudHidden(mc, true);
                server.execute(() -> pipeScene(server));
            }
            case 1860 -> shot(mc, "39_fluid_pipes");
            case 1862 -> {
                setHudHidden(mc, false);
                command(server, "clear @a");
                // In front of the pipe beside the reservoir, looking down at it, so Jade shows the network.
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 29", pipeBase.getX() + 0.5, pipeBase.getY(), pipeBase.getZ() + 3.0)));
            }
            case 1885 -> shot(mc, "40_fluid_pipe_jade");
            case 1887 -> server.execute(() -> showRecipe(server, "fluid_pipe"));
            case 1907 -> shot(mc, "recipe_fluid_pipe");
            case 1910 -> {
                mc.player.closeContainer();
                setHudHidden(mc, true);
                server.execute(() -> itemPipeScene(server));
            }
            case 1955 -> shot(mc, "41_item_pipes");
            case 1957 -> {
                setHudHidden(mc, false);
                command(server, "clear @a");
                // In front of the pipe beside the shears, looking down at it, so Jade shows the network.
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 29", itemPipeBase.getX() + 1.5, itemPipeBase.getY(), itemPipeBase.getZ() + 2.5)));
            }
            case 1980 -> shot(mc, "42_item_pipe_jade");
            case 1982 -> server.execute(() -> showRecipe(server, "item_pipe"));
            case 2002 -> shot(mc, "recipe_item_pipe");
            case 2005 -> {
                mc.player.closeContainer();
                openConfig(mc, "barelyfunctionalcontraptions-sheep-synced.toml");
            }
            case 2025 -> shot(mc, "43_config_sections");
            case 2027 -> pressButton(mc, "Zero Point Energy Module");
            case 2045 -> shot(mc, "44_config_zero_point");
            case 2047 -> {
                openConfig(mc, "barelyfunctionalcontraptions-mycelial-synced.toml");
                pressButton(mc, "Mycelium Spreading");
            }
            case 2065 -> shot(mc, "45_config_mycelium");
            case 2067 -> {
                McClient.setScreen(mc, null);
                setHudHidden(mc, true);
                server.execute(() -> sidewaysScene(server));
            }
            case 2110 -> shot(mc, "46_zero_point_sideways");
            case 2111 -> {
                setHudHidden(mc, false);
                command(server, "clear @a");
                // Close enough for the module's menu to stay open.
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 10", sidewaysBase.getX() + 2.5, sidewaysBase.getY(), sidewaysBase.getZ() + 3.5)));
            }
            case 2112 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                if (server.overworld().getBlockEntity(sidewaysBase.above()) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity be) {
                    player.openMenu(be, sidewaysBase.above());
                }
            });
            case 2130 -> shot(mc, "47_zero_point_sideways_gui");
            case 2132 -> {
                mc.player.closeContainer();
                setHudHidden(mc, true);
                server.execute(() -> breedingScene(server));
            }
            case 2175 -> shot(mc, "48_bred_lambs");
            case 2177 -> server.execute(() -> tankScene(server));
            case 2230 -> shot(mc, "49_fluid_tanks");
            case 2232 -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 200 12", tankBase.getX() + 5.0, tankBase.getY(), tankBase.getZ() + 4.5));
            case 2255 -> shot(mc, "50_fluid_tanks_close");
            case 2257 -> {
                setHudHidden(mc, false);
                command(server, "clear @a");
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 20", tankBase.getX() + 1.0, tankBase.getY(), tankBase.getZ() + 4.0)));
            }
            case 2262 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                if (server.overworld().getBlockEntity(tankBase) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankBlockEntity be) {
                    player.openMenu(be, tankBase);
                }
            });
            case 2280 -> shot(mc, "51_fluid_tank_gui");
            case 2282 -> {
                mc.player.closeContainer();
                server.execute(() -> showRecipe(server, "fluid_tank"));
            }
            case 2302 -> shot(mc, "recipe_fluid_tank");
            case 2305 -> {
                mc.player.closeContainer();
                McClient.setScreen(mc, null);
                setHudHidden(mc, false);
                ResourcefulSheep.LOGGER.warn("Screenshot harness finished; client definitions loaded: {}", SheepResourceRegistry.client().size());
                mc.stop();
            }
            default -> {}
        }
    }

    /** Opens the in-game config screen for one of the mod's config files, the way Mods > Config > (file) does. */
    private static void openConfig(Minecraft mc, String fileName) {
        var config = net.neoforged.fml.config.ModConfigs.getFileMap().get(fileName);
        if (config == null) {
            ResourcefulSheep.LOGGER.warn("Harness: no config file {}", fileName);
            return;
        }
        var parent = new net.neoforged.neoforge.client.gui.ConfigurationScreen(
                ModList.get().getModContainerById(ResourcefulSheep.MOD_ID).orElseThrow(), null);
        McClient.setScreen(mc, new net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen(parent, config.getType(), config,
                net.minecraft.network.chat.Component.translatable("barelyfunctionalcontraptions.configuration.section." + fileName.replace("barelyfunctionalcontraptions-", "barelyfunctionalcontraptions.").replace("-", ".") + ".title")));
    }

    /** Presses the button labelled {@code label} on the open screen (searching nested lists), like a click. */
    private static void pressButton(Minecraft mc, String label) {
        if (McClient.screen(mc) == null || !pressIn(McClient.screen(mc).children(), label)) {
            ResourcefulSheep.LOGGER.warn("Harness: no button '{}' on {}", label, McClient.screen(mc));
        }
    }

    private static boolean pressIn(java.util.List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children, String label) {
        // A config row is a container holding a label widget ("Zero Point Energy Module...") and its "Edit..." button.
        boolean labelHere = children.stream().anyMatch(c -> c instanceof net.minecraft.client.gui.components.AbstractWidget w
                && w.getMessage().getString().startsWith(label));
        for (var child : children) {
            if (child instanceof net.minecraft.client.gui.components.Button button
                    && (labelHere || button.getMessage().getString().equals(label))) {
                button.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                return true;
            }
        }
        for (var child : children) {
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container && pressIn(container.children(), label)) {
                return true;
            }
        }
        return false;
    }

    /** Opens the creative inventory on {@code tab} (set before opening, so the screen also turns to that tab's page). */
    private static void openCreativeTab(Minecraft mc, net.minecraft.world.item.CreativeModeTab tab) {
        try {
            var field = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
            field.setAccessible(true);
            field.set(null, tab);
        } catch (ReflectiveOperationException e) {
            ResourcefulSheep.LOGGER.warn("Harness: could not preselect the creative tab", e);
        }
        McClient.setScreen(mc, new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                mc.player, mc.player.connection.enabledFeatures(), true));
    }

    private static void setHudHidden(Minecraft mc, boolean hidden) {
        McClient.setHudHidden(mc, hidden);
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

    /** The crafted contraptions, in the order docs/REFERENCE.md shows them. */
    private static final String[] RECIPES = {"functional_machine_block", "mycelial_generator", "mycelium_wrench", "mechanical_separator",
            "automatic_shears", "fluid_reservoir", "hume_energy_cube", "zero_point_energy_module"};

    /**
     * Opens a crafting table and lays a recipe's ingredients out in its grid from the recipe's own pattern, so the
     * result slot shows what it makes (exactly what a player would see).
     */
    private static void showRecipe(MinecraftServer server, String name) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        player.getInventory().clearContent();
        BlockPos table = player.blockPosition().above(2);
        level.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, ResourcefulSheep.id(name));
        var holder = server.getRecipeManager().byKey(key).orElse(null);
        if (holder == null || !(holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe recipe)) {
            ResourcefulSheep.LOGGER.warn("Harness: no shaped recipe {}", key);
            return;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new net.minecraft.world.inventory.CraftingMenu(
                id, inventory, net.minecraft.world.inventory.ContainerLevelAccess.create(level, table)),
                net.minecraft.network.chat.Component.translatable("container.crafting")));
        if (!(player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu)) return;
        var ingredients = recipe.getIngredients();
        for (int row = 0; row < recipe.getHeight(); row++) {
            for (int col = 0; col < recipe.getWidth(); col++) {
                var ingredient = ingredients.get(row * recipe.getWidth() + col);
                if (ingredient.isEmpty()) continue;
                @SuppressWarnings("deprecation")
                var item = ingredient.get().items().findFirst();
                int slot = 1 + row * 3 + col; // slot 0 is the result
                item.ifPresent(holderItem -> menu.getSlot(slot).set(new ItemStack(holderItem)));
            }
        }
        menu.broadcastChanges();
    }

    /**
     * The Creative Generator powering three contraptions at once: a separator with wool on one side, Automatic Shears
     * on the other and two Hume Energy Cubes stacked on top (charging).
     */
    private static void creativeScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(30, 30, -4);
        for (int x = -2; x <= 2; x++) for (int z = -1; z <= 1; z++) level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        level.setBlock(base.west(), ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(base.west()) instanceof MechanicalSeparatorBlockEntity separator) {
            separator.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(ResourcefulSheep.id("gold"), 16));
        }
        level.setBlock(base.east(), ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(base, ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        placeCube(level, base.above(), 0);
        placeCube(level, base.above(2), 0);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 20", base.getX() + 0.5, base.getY() + 2, base.getZ() + 5.0));
    }

    private static BlockPos zeroPointBase;

    /**
     * A linked pair of Zero Point Energy Modules: one on a stone floor pointing up, one under a stone overhang six
     * blocks above pointing down, with the beam between them, charging a Hume Energy Cube beside the floor module.
     */
    private static void zeroPointScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(48, 30, -4);
        zeroPointBase = base;
        for (int x = -3; x <= 3; x++) for (int z = -2; z <= 2; z++) level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        // A pillar behind and an overhang over the floor module.
        for (int y = 0; y <= 6; y++) level.setBlock(base.offset(0, y, -2), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        for (int x = -1; x <= 1; x++) for (int z = -2; z <= 0; z++) level.setBlock(base.offset(x, 7, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        var module = ModBlocks.ZERO_POINT_ENERGY_MODULE.get().defaultBlockState();
        var facing = com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock.FACING;
        level.setBlock(base, module.setValue(facing, Direction.UP), 3);
        level.setBlock(base.above(6), module.setValue(facing, Direction.DOWN), 3);
        placeCube(level, base.east(), 0);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 -12", base.getX() + 0.5, base.getY() + 1, base.getZ() + 8.5));
    }

    private static BlockPos cableBase;

    /**
     * A running Mycelial Generator feeding a run of Energy Cables: straight on into a Hume Energy Cube, branching off
     * to a Mechanical Separator, and up a riser into Automatic Shears.
     */
    private static void cableScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(64, 30, -4);
        cableBase = base;
        for (int x = -2; x <= 7; x++) for (int z = -3; z <= 4; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int y = 0; y <= 4; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(base, com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(base) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity generator) {
            generator.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT, new ItemStack(Items.RED_MUSHROOM, 32));
            generator.getTank().fillInternal(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 8000);
        }
        placeCube(level, base.east(5), 0);
        level.setBlock(base.offset(3, 0, 3), ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(base.offset(3, 0, 3)) instanceof MechanicalSeparatorBlockEntity be) {
            be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(ResourcefulSheep.id("iron"), 16));
        }
        level.setBlock(base.offset(1, 3, 0), ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH), 3);
        var cable = ModBlocks.ENERGY_CABLE.get();
        for (BlockPos pos : java.util.List.of(base.east(1), base.east(2), base.east(3), base.east(4), base.offset(3, 0, 1), base.offset(3, 0, 2),
                base.offset(1, 1, 0), base.offset(1, 2, 0))) {
            level.setBlock(pos, cable.withConnections(cable.defaultBlockState(), level, pos), 3);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 200 25", base.getX() + 0.5, base.getY() + 2, base.getZ() + 7.0));
    }

    private static BlockPos pipeBase;

    /**
     * A Fluid Reservoir on a 2x2 pool, piping water to two Mycelial Generators: one straight ahead and one up a riser
     * on a stone ledge.
     */
    private static void pipeScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(80, 30, -4);
        pipeBase = base.south();
        for (int x = -3; x <= 7; x++) for (int z = -3; z <= 5; z++) {
            level.setBlock(base.offset(x, -2, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int y = 0; y <= 4; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) level.setBlock(base.offset(x, -1, z), Blocks.WATER.defaultBlockState(), 3);
        level.setBlock(base, ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
        var generator = com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH);
        level.setBlock(base.offset(5, 0, 1), generator, 3);
        for (int y = 0; y <= 1; y++) level.setBlock(base.offset(3, y, -2), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(base.offset(3, 2, -2), generator, 3);
        var pipe = ModBlocks.FLUID_PIPE.get();
        for (BlockPos pos : java.util.List.of(base.south(), base.offset(1, 0, 1), base.offset(2, 0, 1), base.offset(3, 0, 1), base.offset(4, 0, 1),
                base.offset(3, 0, 0), base.offset(3, 0, -1), base.offset(3, 1, -1), base.offset(3, 2, -1))) {
            level.setBlock(pos, pipe.withConnections(pipe.defaultBlockState(), level, pos), 3);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 200 25", base.getX() + 0.5, base.getY() + 2, base.getZ() + 8.0));
    }

    private static BlockPos itemPipeBase;

    /**
     * Automatic Shears full of Iron Wool, piped into the side of a powered Mechanical Separator, whose bottom output is
     * piped on into a chest.
     */
    private static void itemPipeScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(96, 30, -4);
        itemPipeBase = base;
        for (int x = -3; x <= 7; x++) for (int z = -3; z <= 5; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int y = 0; y <= 4; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(base, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(base) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity shears) {
            shears.setItem(0, ResourceWoolItem.create(ResourcefulSheep.id("iron"), 64));
            shears.setItem(1, ResourceWoolItem.create(ResourcefulSheep.id("iron"), 64));
        }
        BlockPos separatorPos = base.offset(3, 1, 0);
        level.setBlock(separatorPos, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(separatorPos.north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        level.setBlock(base.offset(6, 0, 0), Blocks.CHEST.defaultBlockState(), 3);
        var pipe = ModBlocks.ITEM_PIPE.get();
        for (BlockPos pos : java.util.List.of(base.east(1), base.offset(1, 1, 0), base.offset(2, 1, 0), base.east(3), base.east(4), base.east(5))) {
            level.setBlock(pos, pipe.withConnections(pipe.defaultBlockState(), level, pos), 3);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 200 25", base.getX() + 2.0, base.getY() + 2, base.getZ() + 7.5));
    }

    private static BlockPos sidewaysBase;

    /**
     * Zero Point modules on walls: an east-west pair across a 6-block room with red and yellow glass in the gap, and a
     * north-south pair crossing above it, plus a floor-to-ceiling pair in the corner for comparison.
     */
    private static void sidewaysScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(112, 30, -4);
        sidewaysBase = base;
        for (int x = -2; x <= 8; x++) for (int z = -4; z <= 4; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int y = 0; y <= 6; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        // Two walls, 7 apart, east and west.
        for (int y = 0; y <= 4; y++) for (int z = -3; z <= 3; z++) {
            level.setBlock(base.offset(-1, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            level.setBlock(base.offset(7, y, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        var module = ModBlocks.ZERO_POINT_ENERGY_MODULE.get().defaultBlockState();
        var facing = com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock.FACING;
        level.setBlock(base.above(), module.setValue(facing, Direction.EAST), 3);
        level.setBlock(base.offset(6, 1, 0), module.setValue(facing, Direction.WEST), 3);
        level.setBlock(base.offset(2, 1, 0), Vanilla.stainedGlass(DyeColor.RED).defaultBlockState(), 3);
        level.setBlock(base.offset(4, 1, 0), Vanilla.stainedGlassPane(DyeColor.YELLOW).defaultBlockState(), 3);
        // A north-south pair higher up, hanging off two pillars.
        for (int y = 0; y <= 4; y++) {
            level.setBlock(base.offset(3, y, -4), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        level.setBlock(base.offset(3, 3, -3), module.setValue(facing, Direction.SOUTH), 3);
        level.setBlock(base.offset(3, 3, 3), module.setValue(facing, Direction.NORTH), 3);
        level.setBlock(base.offset(3, 3, 4), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 25", base.getX() + 3.5, base.getY() + 5, base.getZ() + 10.0));
    }

    /**
     * Three pairs of sheep bred the vanilla way on a grass pad: Iron + Iron, Gold + plain white, Diamond + Emerald.
     * Each lamb is born infused with one of its parents' resources.
     */
    private static void breedingScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(128, 30, -4);
        for (int x = -2; x <= 8; x++) for (int z = -3; z <= 3; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (int y = 0; y <= 3; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        String[][] pairs = {{"iron", "iron"}, {"gold", null}, {"diamond", "emerald"}};
        for (int i = 0; i < pairs.length; i++) {
            Sheep a = null, b = null;
            for (int k = 0; k < 2; k++) {
                Sheep sh = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (sh == null) return;
                sh.setNoAi(true);
                sh.snapTo(base.getX() + i * 3 + 0.5 + k * 1.2, base.getY(), base.getZ() - 0.5, 200, 0);
                if (pairs[i][k] != null) {
                    SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id(pairs[i][k])), 9, 0));
                }
                level.addFreshEntity(sh);
                if (k == 0) a = sh; else b = sh;
            }
            var before = new java.util.HashSet<>(level.getEntitiesOfClass(Sheep.class, a.getBoundingBox().inflate(3), Sheep::isBaby));
            a.spawnChildFromBreeding(level, b);
            // Move the new lamb to the front so it isn't hidden behind its parents.
            for (Sheep lamb : level.getEntitiesOfClass(Sheep.class, a.getBoundingBox().inflate(3), l -> l.isBaby() && !before.contains(l))) {
                lamb.setNoAi(true);
                lamb.snapTo(base.getX() + i * 3 + 1.1, base.getY(), base.getZ() + 1.3, 200, 0);
            }
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", base.getX() + 4.0, base.getY() + 3, base.getZ() + 6.0));
    }

    private static BlockPos tankBase;

    /**
     * Fluid Tanks: a 2x2x2 group a little over half full of water, a 3-high tower of lava two thirds full, a single
     * tank a quarter full, and an L of three water tanks, on a stone pad.
     */
    private static void tankScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos base = origin.offset(144, 30, -4);
        tankBase = base;
        for (int x = -3; x <= 10; x++) for (int z = -4; z <= 4; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int y = 0; y <= 5; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        var tank = ModBlocks.FLUID_TANK.get().defaultBlockState();
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        var lava = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA);
        java.util.function.BiConsumer<BlockPos, Object[]> fill = (pos, f) -> {
            if (level.getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankBlockEntity be) {
                be.storage().set(0, (net.neoforged.neoforge.transfer.fluid.FluidResource) f[0], (Integer) f[1]);
            }
        };
        // 2x2x2 of water: 70,000 of 128,000 mB (the group settles it: bottom full, top a little over a third).
        for (int x = 0; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = 0; z <= 1; z++) level.setBlock(base.offset(x, y, z), tank, 3);
        for (int x = 0; x <= 1; x++) for (int z = 0; z <= 1; z++) fill.accept(base.offset(x, 0, z), new Object[]{water, 16_000});
        fill.accept(base.offset(0, 1, 0), new Object[]{water, 6_000});
        // A 3-high tower of lava, two thirds full.
        for (int y = 0; y <= 2; y++) level.setBlock(base.offset(4, y, 0), tank, 3);
        fill.accept(base.offset(4, 0, 0), new Object[]{lava, 16_000});
        fill.accept(base.offset(4, 1, 0), new Object[]{lava, 16_000});
        // A single tank, a quarter full of water.
        level.setBlock(base.offset(6, 0, 1), tank, 3);
        fill.accept(base.offset(6, 0, 1), new Object[]{water, 4_000});
        // An L of three, half full.
        level.setBlock(base.offset(8, 0, 0), tank, 3);
        level.setBlock(base.offset(8, 0, 1), tank, 3);
        level.setBlock(base.offset(9, 0, 1), tank, 3);
        fill.accept(base.offset(8, 0, 0), new Object[]{water, 24_000 > 16_000 ? 16_000 : 0});
        fill.accept(base.offset(8, 0, 1), new Object[]{water, 8_000});
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 28", base.getX() + 4.5, base.getY() + 4, base.getZ() + 8.5));
    }

    /** Opens the floor module's GUI for the player. */
    private static void openZeroPointGui(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        if (server.overworld().getBlockEntity(zeroPointBase) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity be) {
            player.openMenu(be, zeroPointBase);
        }
    }

    private static BlockPos cubeBase;

    /**
     * Hume Energy Cubes: a 2x2x2 group charged by a running generator, a single empty cube, and a full single one,
     * so the window levels can be compared.
     */
    private static void cubeScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        cubeBase = origin.offset(-14, 30, -4);
        for (int x = -1; x <= 9; x++) {
            for (int z = -1; z <= 2; z++) level.setBlock(cubeBase.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        }
        level.setBlock(cubeBase, com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(cubeBase) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity generator) {
            generator.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT, new ItemStack(Items.RED_MUSHROOM, 32));
            generator.getTank().fillInternal(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 8000);
        }
        for (int x = 1; x <= 2; x++) for (int y = 0; y <= 1; y++) for (int z = 0; z <= 1; z++) {
            placeCube(level, cubeBase.offset(x, y, z), 0);
        }
        if (level.getBlockEntity(cubeBase.offset(1, 0, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity be) {
            be.storage().set(1_000_000);
            if (level.getBlockEntity(cubeBase.offset(2, 0, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity b2) b2.storage().set(1_000_000);
            if (level.getBlockEntity(cubeBase.offset(1, 1, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity b3) b3.storage().set(1_000_000);
            if (level.getBlockEntity(cubeBase.offset(2, 1, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity b4) b4.storage().set(1_000_000);
        }
        // A 3-high tower at 60%: the energy level runs on through all three windows.
        for (int y = 0; y < 3; y++) placeCube(level, cubeBase.offset(5, y, 0), 600_000);
        // A single empty cube and a single full one.
        placeCube(level, cubeBase.offset(7, 0, 0), 0);
        placeCube(level, cubeBase.offset(9, 0, 0), 1_000_000);
        // An L (like a corner of a wall) and a 3x3 ring with no middle: their inside corners need the diagonal check.
        for (int i = 0; i < 4; i++) placeCube(level, cubeBase.offset(1 + i, -1, 5), 500_000);
        for (int i = 1; i < 4; i++) placeCube(level, cubeBase.offset(1, -1, 5 + i), 500_000);
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            if (x != 1 || z != 1) placeCube(level, cubeBase.offset(7 + x, -1, 5 + z), 500_000);
        }
        // Odd shapes with inward folds: a 2x2 base with a wall on its back row, and a three-step staircase.
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) placeCube(level, cubeBase.offset(1 + x, -1, 11 + z), 500_000);
        for (int x = 0; x < 2; x++) placeCube(level, cubeBase.offset(1 + x, 0, 11), 500_000);
        for (int step = 0; step < 3; step++) for (int h = 0; h <= step; h++) placeCube(level, cubeBase.offset(6 + step, -1 + h, 11), 500_000);
        // A floating 3x2 slab (to see the connected bottom faces from below).
        for (int x = 0; x < 3; x++) for (int z = 0; z < 2; z++) placeCube(level, cubeBase.offset(12 + x, 4, z), 500_000);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 18", cubeBase.getX() + 5.5, cubeBase.getY() + 3, cubeBase.getZ() + 9.5));
    }

    private static void placeCube(ServerLevel level, BlockPos pos, int energy) {
        level.setBlock(pos, ModBlocks.HUME_ENERGY_CUBE.get().getStateForPlacement(
                new net.minecraft.world.item.context.DirectionalPlaceContext(level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)), 3);
        if (level.getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity be) {
            be.storage().set(energy);
        }
    }

    /** The same readout a right-click on the 2x2x2 group gives. */
    private static void cubeMessage(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        if (player.level().getBlockEntity(cubeBase.offset(1, 0, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity be) {
            var group = be.group();
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.barelyfunctionalcontraptions.energy_cube.info",
                    group.size(), String.format(java.util.Locale.ROOT, "%,d", group.amount()), String.format(java.util.Locale.ROOT, "%,d", group.capacity()),
                    String.format(java.util.Locale.ROOT, "%,d", group.rate())).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
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
            Sheep sh = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
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
            Sheep sh = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
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

    /** Rows of common Energy Meter set-ups, each meter placed by a real floor click ({@code -PharnessScene=meter_setups}). */
    private static final String[] METER_ROWS = {"generator > meter > separator", "generator > cable > meter > cable > separator",
            "cube > meter > separator", "glow panel > meter > cable > cube", "cable > meter > cable (creative generator > ... > cube)",
            "generator > meter > (nothing)"};

    private static void meterSetupsTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> meterSetupsScene(server));
            case 76, 86, 96, 106, 116, 126 -> {
                BlockPos spot = meterSpot((tick - 76) / 10);
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 60", spot.getX() + 0.5, spot.getY(), spot.getZ() + 1.8));
            }
            case 80, 90, 100, 110, 120, 130 -> {
                int row = (tick - 80) / 10;
                mc.player.getInventory().setSelectedSlot(0);
                BlockPos floor = meterSpot(row).below();
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
                mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            }
            case 135 -> setHudHidden(mc, true);
            case 220 -> server.execute(() -> {
                var level = server.overworld();
                for (int row = 0; row < METER_ROWS.length; row++) {
                    BlockPos meter = meterSpot(row);
                    var state = level.getBlockState(meter);
                    String reading = level.getBlockEntity(meter) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity be
                            ? be.rate() + " FE/t, total " + be.total() : "no meter";
                    BlockPos end = meterEnd(row);
                    var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, end, null);
                    ResourcefulSheep.LOGGER.warn("METER row {} ({}): {} {} -> end block {} holds {}", row, METER_ROWS[row],
                            state.hasProperty(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING) ? state.getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING) : "-",
                            reading, level.getBlockState(end).getBlock(), handler == null ? "?" : handler.getAmountAsLong());
                }
            });
            case 225 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.8));
            case 260 -> shot(mc, "67_meter_setups");
            case 262 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 80", origin.getX() + 0.5, origin.getY() + 2, origin.getZ() - 1.0));
            case 290 -> shot(mc, "68_meter_top");
            case 292 -> {
                BlockPos lone = meterSpot(5);
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", lone.getX() + 0.5, lone.getY(), lone.getZ() + 2.3));
            }
            case 320 -> shot(mc, "69_meter_no_output");
            case 330 -> mc.stop();
            default -> {}
        }
    }

    /** Where the meter of {@code row} goes (rows run west to east along z = -2 - 2*row). */
    private static BlockPos meterSpot(int row) {
        return origin.offset(0, 0, -2 - 2 * row);
    }

    private static BlockPos meterEnd(int row) {
        return switch (row) {
            case 1 -> meterSpot(row).east(2);
            case 3 -> meterSpot(row).east(2);
            case 4 -> meterSpot(row).east(3);
            default -> meterSpot(row).east();
        };
    }

    private static void meterSetupsScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-6, 0, -16), o.offset(6, 4, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-6, -1, -16), o.offset(6, -1, 4))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
        var genState = com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH);
        // The separator faces south, so its west (left) side takes energy.
        var sepState = ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH);
        var cable = ModBlocks.ENERGY_CABLE.get();
        java.util.function.Consumer<BlockPos> generator = pos -> {
            level.setBlock(pos, genState, 3);
            if (level.getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity g) {
                g.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT, new ItemStack(Items.RED_MUSHROOM, 32));
                g.getTank().fillInternal(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 8000);
            }
        };
        java.util.function.Consumer<BlockPos> separator = pos -> {
            level.setBlock(pos, sepState, 3);
            if (level.getBlockEntity(pos) instanceof MechanicalSeparatorBlockEntity be) {
                be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(ResourcefulSheep.id("iron"), 64));
            }
        };
        java.util.function.Consumer<BlockPos> cableAt = pos -> level.setBlock(pos, cable.withConnections(cable.defaultBlockState(), level, pos), 3);
        // Row 0: generator > meter > separator.
        BlockPos m = meterSpot(0);
        generator.accept(m.west());
        separator.accept(m.east());
        // Row 1: generator > cable > meter > cable > separator.
        m = meterSpot(1);
        generator.accept(m.west(2));
        separator.accept(m.east(2));
        cableAt.accept(m.west());
        cableAt.accept(m.east());
        // Row 2: charged cube > meter > separator.
        m = meterSpot(2);
        level.setBlock(m.west(), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        level.setBlock(m.west(2), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        separator.accept(m.east());
        // Row 3: glow panel > meter > cable > cube.
        m = meterSpot(3);
        level.setBlock(m.west(), ModBlocks.GLOW_PANEL.get().defaultBlockState(), 3);
        cableAt.accept(m.east());
        level.setBlock(m.east(2), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        // Row 4: creative generator > cable > meter > cable > cable > cube.
        m = meterSpot(4);
        level.setBlock(m.west(2), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        cableAt.accept(m.west());
        cableAt.accept(m.east());
        cableAt.accept(m.east(2));
        level.setBlock(m.east(3), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        // Row 5: a generator and nothing in front: the meter should say NO OUTPUT.
        generator.accept(meterSpot(5).west());
        for (BlockPos p : new BlockPos[]{meterSpot(1).west(), meterSpot(1).east(), meterSpot(3).east(), meterSpot(4).west(), meterSpot(4).east(), meterSpot(4).east(2)}) {
            level.setBlock(p, cable.withConnections(level.getBlockState(p), level, p), 3);
        }
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModItems.ENERGY_METER.get(), 16));
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 60", o.getX() + 0.5, o.getY() + 1, o.getZ() + 1.5));
    }

    /**
     * Checks real player interactions end to end ({@code -PharnessScene=interactions}): placing an Energy Meter
     * against a running Mycelial Generator with a right-click, and sneak-right-clicking an Item Pipe's plug with the
     * wrench. Results are written to the log as "INTERACTION ...".
     */
    private static void interactionsTick(Minecraft mc, MinecraftServer server) {
        BlockPos o = origin;
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                BlockPos b = origin;
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                for (BlockPos p : BlockPos.betweenClosed(b.offset(-6, 0, -6), b.offset(6, 4, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(b.offset(-6, -1, -6), b.offset(6, -1, 2))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
                // A running generator at (-2, 0, -3), a Hume Energy Cube two blocks east of it; the meter goes in between.
                BlockPos gen = b.offset(-2, 0, -3);
                level.setBlock(gen, com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                        .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH), 3);
                if (level.getBlockEntity(gen) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity generator) {
                    generator.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT, new ItemStack(Items.RED_MUSHROOM, 32));
                    generator.getTank().fillInternal(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 8000);
                }
                level.setBlock(gen.east(2), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
                // A chest with an Item Pipe on its east side, at (2, 0, -3).
                level.setBlock(b.offset(2, 0, -3), Blocks.CHEST.defaultBlockState(), 3);
                var pipeBlock = ModBlocks.ITEM_PIPE.get();
                level.setBlock(b.offset(3, 0, -3), pipeBlock.withConnections(pipeBlock.defaultBlockState(), level, b.offset(3, 0, -3)), 3);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.getInventory().setItem(0, new ItemStack(ModItems.ENERGY_METER.get(), 4));
                player.getInventory().setItem(1, new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()));
                // Stand south of the gap, looking north at it: how a player would usually place the meter.
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", gen.getX() + 1.5, gen.getY(), gen.getZ() + 2.5));
            });
            case 80 -> {
                // Right-click the floor in the gap between the generator and the cube, holding the meter.
                mc.player.getInventory().setSelectedSlot(0);
                BlockPos floor = origin.offset(-1, -1, -3);
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
                var result = mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                ResourcefulSheep.LOGGER.warn("INTERACTION meter place result {}", result);
            }
            case 90 -> server.execute(() -> {
                BlockPos meter = origin.offset(-1, 0, -3);
                var state = server.overworld().getBlockState(meter);
                ResourcefulSheep.LOGGER.warn("INTERACTION meter state {}", state);
            });
            case 160 -> server.execute(() -> {
                BlockPos meter = origin.offset(-1, 0, -3);
                var level = server.overworld();
                String rate = level.getBlockEntity(meter) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity be ? be.rate() + " FE/t, total " + be.total() : "no meter";
                long cube = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, origin.offset(0, 0, -3), Direction.UP) instanceof net.neoforged.neoforge.transfer.energy.EnergyHandler h ? h.getAmountAsLong() : -1;
                ResourcefulSheep.LOGGER.warn("INTERACTION meter reads {}; cube holds {}", rate, cube);
            });
            case 170 -> {
                // Sneak + right-click the pipe's west plug with the wrench.
                McClient.setScreen(mc, null);
                mc.player.getInventory().setSelectedSlot(1);
                mc.options.keyShift.setDown(true);
            }
            case 175 -> {
                BlockPos pipe = origin.offset(3, 0, -3);
                // On the top of the pipe's middle, not on the plug: the easy click to make.
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pipe).add(0, 0.19, 0), Direction.UP, pipe, false);
                var result = mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                ResourcefulSheep.LOGGER.warn("INTERACTION wrench sneak-click result {} (client sneaking {})", result, mc.player.isShiftKeyDown());
            }
            case 195 -> {
                mc.options.keyShift.setDown(false);
                ResourcefulSheep.LOGGER.warn("INTERACTION screen after sneak-click: {}", McClient.screen(mc));
                shot(mc, "66_interaction_filter");
            }
            case 200 -> mc.stop();
            default -> {}
        }
    }

    /** Every block added in the latest update, labelled: {@code -PharnessScene=new_blocks}. */
    private static void newBlocksTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> newBlocksScene(server));
            case 70 -> setHudHidden(mc, true);
            case 160 -> shot(mc, "62_new_blocks");
            case 162 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 22", origin.getX() - 4.0, origin.getY() + 2, origin.getZ() + 0.5));
            case 200 -> shot(mc, "63_new_blocks_power");
            case 202 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 22", origin.getX() + 6.0, origin.getY() + 2, origin.getZ() + 0.5));
            case 240 -> shot(mc, "64_new_blocks_pipes");
            case 242 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos pipePos = origin.offset(3, 0, -4);
                if (!(player.level().getBlockEntity(pipePos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeBlockEntity<?> pipe)) return;
                var side = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.DEFAULT.withMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PULL);
                for (String id : new String[]{"iron_ingot", "gold_ingot", "copper_ingot", "diamond", "redstone"}) side = side.toggle(net.minecraft.resources.Identifier.withDefaultNamespace(id));
                pipe.setSide(Direction.WEST, side);
                player.getInventory().setItem(0, new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()));
                player.getInventory().setItem(1, new ItemStack(Items.EMERALD, 16));
                player.getInventory().setItem(2, new ItemStack(Items.LAPIS_LAZULI, 32));
                player.getInventory().setItem(3, new ItemStack(Items.COAL, 64));
                net.minecraft.network.chat.Component title = net.minecraft.network.chat.Component.translatable("gui.barelyfunctionalcontraptions.pipe_filter.title", ModBlocks.ITEM_PIPE.get().getName(),
                        com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock.sideNameOf(Direction.WEST));
                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, p) -> new com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu(id, inventory, pipe, Direction.WEST), title),
                        buf -> {
                            buf.writeBlockPos(pipePos);
                            buf.writeEnum(Direction.WEST);
                        });
            });
            case 246 -> setHudHidden(mc, false);
            case 248 -> com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 265 -> shot(mc, "65_pipe_filter");
            case 275 -> mc.stop();
            default -> {}
        }
    }

    private static void label(MinecraftServer server, BlockPos at, String text, String colour) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), String.format(java.util.Locale.ROOT,
                "summon text_display %.2f %.2f %.2f {billboard:\"center\",text:{text:\"%s\",color:\"%s\"},background:1342177280,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1.3f,1.3f,1.3f]}}",
                at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, text, colour));
    }

    private static void newBlocksScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-12, 0, -10), o.offset(12, 6, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-12, -1, -10), o.offset(12, -1, 6))) level.setBlock(p, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        int z = -4;
        var panel = ModBlocks.GLOW_PANEL.get().defaultBlockState();
        // Glow Panel in the sun; a Glow Lamp on a panel; a panel shaded by stone, with the Contraptions Manual framed on the stone.
        level.setBlock(o.offset(-8, 0, z), panel, 3);
        label(server, o.offset(-8, 1, z), "Glow Panel", "aqua");
        level.setBlock(o.offset(-6, 0, z), panel, 3);
        level.setBlock(o.offset(-6, 1, z), ModBlocks.GLOW_LAMP.get().defaultBlockState(), 3);
        label(server, o.offset(-6, 2, z), "Glow Lamp", "aqua");
        level.setBlock(o.offset(-4, 0, z), panel, 3);
        level.setBlock(o.offset(-4, 1, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), String.format(java.util.Locale.ROOT,
                "summon item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"barelyfunctionalcontraptions:contraptions_manual\",count:1}}",
                o.getX() - 4, o.getY() + 1, o.getZ() + z + 1));
        label(server, o.offset(-4, 3, z), "Contraptions Manual", "yellow");
        // Energy Meter: a Glow Panel in the sun -> meter -> Hume Energy Cube (a steady 60 FE/t).
        level.setBlock(o.offset(-2, 0, z), panel, 3);
        level.setBlock(o.offset(-1, 0, z), ModBlocks.ENERGY_METER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING, Direction.EAST)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN, Direction.SOUTH), 3);
        level.setBlock(o.offset(0, 0, z), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        label(server, o.offset(-1, 1, z), "Energy Meter", "light_purple");
        // Item Pipes: chest -> Pull (orange) ... Push (green) -> chest.
        level.setBlock(o.offset(2, 0, z), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(o.offset(2, 0, z)) instanceof net.minecraft.world.Container chest) chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        level.setBlock(o.offset(5, 0, z), Blocks.CHEST.defaultBlockState(), 3);
        var itemPipe = ModBlocks.ITEM_PIPE.get();
        for (int x = 3; x <= 4; x++) level.setBlock(o.offset(x, 0, z), itemPipe.withConnections(itemPipe.defaultBlockState(), level, o.offset(x, 0, z)), 3);
        setSide(level, o.offset(3, 0, z), Direction.WEST, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PULL);
        setSide(level, o.offset(4, 0, z), Direction.EAST, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PUSH);
        label(server, o.offset(3, 1, z), "Item Pipe: Pull / Push", "gold");
        // Fluid Pipes: Fluid Tank -> Pull ... Push -> Fluid Tank.
        level.setBlock(o.offset(7, 0, z), ModBlocks.FLUID_TANK.get().defaultBlockState(), 3);
        level.setBlock(o.offset(10, 0, z), ModBlocks.FLUID_TANK.get().defaultBlockState(), 3);
        var fluidPipe = ModBlocks.FLUID_PIPE.get();
        for (int x = 8; x <= 9; x++) level.setBlock(o.offset(x, 0, z), fluidPipe.withConnections(fluidPipe.defaultBlockState(), level, o.offset(x, 0, z)), 3);
        setSide(level, o.offset(8, 0, z), Direction.WEST, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PULL);
        setSide(level, o.offset(9, 0, z), Direction.EAST, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PUSH);
        label(server, o.offset(8, 2, z), "Fluid Pipe: Pull / Push", "aqua");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 15", o.getX() + 1.0, o.getY() + 3, o.getZ() + 8.5));
    }

    /** Glow Panels, Glow Lamps and Energy Meters: {@code -PharnessScene=power_blocks}. */
    private static void powerBlocksTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> powerBlocksScene(server));
            case 70 -> setHudHidden(mc, true);
            case 75 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 35", origin.getX() + 0.5, origin.getY() + 2, origin.getZ() + 1.5));
            case 140 -> shot(mc, "56_glow_panels");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 160 25", origin.getX() + 2.5, origin.getY() + 1, origin.getZ() - 0.5));
            case 170 -> shot(mc, "57_energy_meter");
            case 171 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 90", origin.getX() - 2.5, origin.getY() + 3, origin.getZ() - 0.5));
            case 200 -> shot(mc, "59_pipe_modes");
            case 202 -> {
                command(server, "time set midnight");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 35", origin.getX() + 0.5, origin.getY() + 2, origin.getZ() + 1.5));
            }
            case 138, 258, 318 -> logPanels(server);
            case 260 -> shot(mc, "58_glow_panels_night");
            // Sunset: the open-sky panels make less, so their cells glow dimmer.
            case 262 -> command(server, "time set 12300");
            case 320 -> shot(mc, "58_glow_panels_dusk");
            case 322 -> McClient.setScreen(mc, new GuideScreen());
            case 335 -> shot(mc, "60_guide");
            case 337 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Glow Panels"); }
            case 350 -> shot(mc, "61_guide_glow_panels");
            case 360 -> mc.stop();
            default -> {}
        }
    }

    /** Logs every Glow Panel near the scene: what it makes and how brightly its cells glow. */
    private static void logPanels(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-10, -2, -10), origin.offset(10, 4, 10))) {
                if (level.getBlockEntity(p) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GlowPanelBlockEntity panel) {
                    ResourcefulSheep.LOGGER.info("PANEL at {} generating={} source={} glow={}", p.immutable(),
                            panel.generating(), panel.source(), panel.getBlockState().getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.GlowPanelBlock.GLOW));
                }
            }
        });
    }

    /**
     * Adds new scenes to the test world, each in a free cell of its square platform (see {@link #SQUARE_SCENES}; name,
     * column, row), then puts the player back where they were, drains the generators that should be saved empty, and
     * saves: {@code -PharnessScene=extend_world -PharnessWorld="BFC Test World"}. With no scenes listed it only drains.
     */
    private static final Object[][] EXTRA_SCENES = {
            // Rows 0 to 5 are full: up to (1, 5) by the moved scenes, (2, 5) by the Grass Seeder, (3, 5) by the Herd
            // Counter and (4, 5) by the Machine Status Panel. Row 6, added by extendPlatform: (0, 6) the Wool-Lined Crates
            // (built by an earlier run, as "crates"), (1, 6) the Fleece Trawler (as "fleece trawler"), (2, 6) the upgrade
            // cards (as "upgrades"), (3, 6) the Hume Field Generator (as "hume field"); (4, 6) is free. An earlier run also
            // cabled the old wireless-power scene ("cable the wireless scene"). Now: the Hume Field Generators onto the
            // Machine Status Panel.
            {"hume field on the panel", 3, 6},
    };
    /** Rows of cells on the platform now: the seventh was added south of the first six, for the crates. */
    private static final int PLATFORM_ROWS = 7;

    /**
     * Grows the square platform south by a row of cells (from {@link #SQUARE_SIDE} deep to {@link #PLATFORM_ROWS} rows):
     * the old south rim becomes floor, the cell lines carry on, a new rim goes round, and the spawn moves to the new south
     * edge. Run again it changes nothing.
     */
    private static void extendPlatform(MinecraftServer server) {
        ServerLevel level = server.overworld();
        int south = SQUARE_Z + GRID_Z + PLATFORM_ROWS * CELL_D + 3; // the new rim: three blocks past the last cell line, as before
        for (int x = SQUARE_X; x < SQUARE_X + SQUARE_SIDE; x++) {
            for (int z = SQUARE_Z + SQUARE_SIDE - 4; z <= south; z++) {
                boolean rim = x == SQUARE_X || x == SQUARE_X + SQUARE_SIDE - 1 || z == south;
                int gx = x - SQUARE_X - GRID_X, gz = z - SQUARE_Z - GRID_Z;
                boolean line = (gx >= 0 && gx <= 5 * CELL_W && gx % CELL_W == 0 && gz >= 0 && gz <= PLATFORM_ROWS * CELL_D)
                        || (gz >= 0 && gz <= PLATFORM_ROWS * CELL_D && gz % CELL_D == 0 && gx >= 0 && gx <= 5 * CELL_W);
                level.setBlock(new BlockPos(x, 34, z), (rim ? Blocks.POLISHED_DEEPSLATE : line ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE).defaultBlockState(), 2);
            }
        }
        int sx = SQUARE_X + SQUARE_SIDE / 2;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), String.format(java.util.Locale.ROOT, "setworldspawn %d 35 %d", sx, south - 2));
        extendReturn = new net.minecraft.world.phys.Vec3(sx + 0.5, 35, south - 1.5);
        extendYaw = 180;
        extendPitch = 10;
        ResourcefulSheep.LOGGER.info("EXTEND platform to z {}; spawn {} 35 {}", south, sx, south - 2);
    }

    /**
     * The Wool-Lined Crates' cell (0, 6): a storeroom wall of fourteen crates in two rows, each lined its own colour
     * (cobblestone grey, wood brown, wheat yellow, iron locked, two packed with bales, one bulging); in front, a chest
     * and hopper overfilling a full crate of redstone, so it bursts now and then, beside one strapped shut that never does.
     * The Machine Status Panel (cell 4, 5) is linked to the overfilled one.
     */
    private static void crateWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin, g = o.offset(0, 0, -3);
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(10, 6, 9),
                e -> e instanceof net.minecraft.world.entity.Display.TextDisplay || e instanceof net.minecraft.world.entity.item.ItemEntity)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-9, 0, -7), g.offset(9, 6, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        // (An earlier build's chests and hoppers spill what they held as they go.)
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(12, 8, 12),
                e -> e instanceof net.minecraft.world.entity.item.ItemEntity)) e.discard();
        // A spruce floor for the storeroom.
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-6, -1, -5), g.offset(6, -1, 3))) level.setBlock(p, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
        Object[][] wall = {
                {Items.COBBLESTONE, "GRAY", 1287, false, 0}, {Items.OAK_LOG, "BROWN", 1950, false, 0}, {Items.WHEAT, "YELLOW", 640, false, 0},
                {Items.IRON_INGOT, "WHITE", 412, true, 0}, {Items.REDSTONE, "RED", 3000, false, 2}, {Items.LAPIS_LAZULI, "BLUE", 255, false, 0},
                {Items.EMERALD, "LIME", 37, true, 0},
                {Items.COAL, "BLACK", 1024, false, 0}, {Items.COPPER_INGOT, "ORANGE", 733, false, 0}, {Items.GOLD_INGOT, "YELLOW", 96, true, 0},
                {Items.DIAMOND, "CYAN", 18, true, 0}, {Items.STRING, "WHITE", 5120, false, 4}, {Items.BONE_MEAL, "LIGHT_GRAY", 820, false, 0},
                {Items.GLASS, "LIGHT_BLUE", 300, false, 0},
        };
        for (int i = 0; i < wall.length; i++) {
            BlockPos at = g.offset(-3 + i % 7, i / 7, -4);
            placeCrate(level, at, null, false,
                    new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack((net.minecraft.world.item.Item) wall[i][0]),
                            (Integer) wall[i][2], (Boolean) wall[i][3], (Integer) wall[i][4]));
        }
        // In front: a chest and hopper pushing redstone at a full crate (it bursts now and then), and the same at a
        // strapped crate of cobblestone (it never does).
        BlockPos burst = g.offset(-4, 0, 1), strapped = g.offset(4, 0, 1);
        placeCrate(level, burst, null, false,
                new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.REDSTONE), 2048, false, 0));
        placeCrate(level, strapped, null, true,
                new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.COBBLESTONE), 2048, true, 0));
        for (BlockPos crate : new BlockPos[]{burst, strapped}) {
            net.minecraft.world.item.Item fill = crate == burst ? Items.REDSTONE : Items.COBBLESTONE;
            level.setBlock(crate.above(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN), 3);
            level.setBlock(crate.above(2), Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(crate.above(2)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                for (int s = 0; s < chest.getContainerSize(); s++) chest.setItem(s, new ItemStack(fill, 64));
            }
        }
        // On top of the wall: a Crate Controller (the wall's crates join it by touch) beside an empty crate, fed by a
        // hopper under a chest of things that go in the wall's crates (and bread, which goes in the empty one).
        BlockPos controller = g.offset(0, 2, -4);
        level.setBlock(controller, ModBlocks.CRATE_CONTROLLER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateControllerBlock.FACING, Direction.SOUTH), 3);
        placeCrate(level, controller.east(), null, false, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents.EMPTY);
        level.setBlock(controller.above(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN), 3);
        level.setBlock(controller.above(2), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(controller.above(2)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity feed) {
            net.minecraft.world.item.Item[] fill = {Items.COBBLESTONE, Items.BREAD, Items.COAL, Items.WHEAT, Items.BREAD, Items.COBBLESTONE, Items.COAL,
                    Items.WHEAT, Items.GLASS, Items.COBBLESTONE};
            for (int s = 0; s < fill.length; s++) feed.setItem(s, new ItemStack(fill[s], s == 0 ? 16 : 64));
        }
        if (level.getBlockEntity(controller) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity c) {
            c.refresh(level);
            ResourcefulSheep.LOGGER.info("EXTEND crates: the controller has {} crates", c.crateCount());
        }
        label(server, g.above(6).relative(Direction.NORTH, 4), "Wool-Lined Crates and a Crate Controller", "gold");
        label(server, burst.above(3), "Overfilled: bursts", "red");
        label(server, strapped.above(3), "Strapped: never bursts", "gray");
        // The status panel shows the overfilled one.
        BlockPos panelMaster = new BlockPos(128, 37, 152);
        if (level.getBlockEntity(panelMaster) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity panel) {
            var result = panel.links().contains(burst) ? null : panel.toggleLink(level, burst);
            var store = panel.links().contains(controller) ? null : panel.toggleLink(level, controller);
            ResourcefulSheep.LOGGER.info("EXTEND crates: linked the overfilled crate to the status panel: {}, and the controller: {}", result, store);
        }
        ResourcefulSheep.LOGGER.info("EXTEND crates built round {}", g);
    }
    /**
     * The Fleece Trawler's cell (1, 6): a full pool (7 by 7, 4 deep, its surface level with the platform) sunk through
     * the platform in a dark prismarine basin, fenced so its three Tide sheep stay in; the trawler on the basin's south
     * rim facing it, with a stack of Iron Wool as its lure, and a hopper under it emptying the catch into a chest set in
     * the floor. The Machine Status Panel (cell 4, 5) is linked to it.
     */
    private static void trawlerWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos g = origin.offset(0, 0, -3); // the trawler
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(10, 8, 10),
                e -> e instanceof net.minecraft.world.entity.Display.TextDisplay || e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof Sheep)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-8, 0, -10), g.offset(8, 6, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-8, -1, -10), g.offset(8, -1, 4))) level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        // The basin: dark prismarine from a block under the platform up to its top, round a pool four deep.
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-4, -4, -8), g.offset(4, 0, 0))) {
            boolean inside = p.getX() > g.getX() - 4 && p.getX() < g.getX() + 4 && p.getZ() > g.getZ() - 8 && p.getZ() < g.getZ() && p.getY() > g.getY() - 4;
            level.setBlock(p, (inside ? Blocks.WATER : Blocks.DARK_PRISMARINE).defaultBlockState(), 2);
        }
        level.setBlock(g, ModBlocks.FLEECE_TRAWLER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.FleeceTrawlerBlock.FACING, Direction.NORTH), 3);
        if (level.getBlockEntity(g) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity t) {
            t.addLure(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("iron"), 16));
        }
        // Under it, a hopper into a chest in the floor.
        level.setBlock(g.below(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(g.below().south(), Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH), 3);
        // A fence round the rim (over the trawler too), so the sheep can't climb out.
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-4, 1, -8), g.offset(4, 1, 0))) {
            if (p.getX() == g.getX() - 4 || p.getX() == g.getX() + 4 || p.getZ() == g.getZ() - 8 || p.getZ() == g.getZ()) {
                level.setBlock(p, Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-4, 1, -8), g.offset(4, 1, 0))) {
            net.minecraft.world.level.block.state.BlockState fence = level.getBlockState(p);
            if (fence.is(Blocks.SPRUCE_FENCE)) level.setBlock(p, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(fence, level, p), 2);
        }
        for (int i = 0; i < 3; i++) {
            Sheep sh = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sh.snapTo(g.getX() - 1.5 + i * 2, g.getY() - 1.5, g.getZ() - 3.5 - (i == 1 ? 2 : 0), 90, 0);
            SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id("tide")), 9, 0));
            sh.setPersistenceRequired();
            level.addFreshEntity(sh);
        }
        label(server, g.above(4).relative(Direction.NORTH, 4), "Fleece Trawler and three Tide sheep", "aqua");
        label(server, g.below().south().above(2), "Its catch", "gray");
        BlockPos panelMaster = new BlockPos(128, 37, 152);
        if (level.getBlockEntity(panelMaster) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity panel) {
            var result = panel.links().contains(g) ? null : panel.toggleLink(level, g);
            ResourcefulSheep.LOGGER.info("EXTEND trawler: linked to the status panel: {}", result);
        }
        ResourcefulSheep.LOGGER.info("EXTEND trawler built at {}", g);
    }

    /**
     * The scene that showed a Mycelial Generator powering a Mechanical Separator (4 blocks east of it) and Automatic
     * Shears (8 east) wirelessly, from before wireless power was removed: lays Energy Cable from the generator to both
     * along the row, and renames its label. Found by that layout anywhere on the platform; run again it changes nothing.
     */
    private static void cableWirelessScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var cable = ModBlocks.ENERGY_CABLE.get();
        int found = 0;
        for (BlockPos p : BlockPos.betweenClosed(0, 35, 24, 145, 40, 192)) {
            if (!level.getBlockState(p).is(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get())) continue;
            BlockPos gen = p.immutable();
            if (!(level.getBlockEntity(gen.east(4)) instanceof MechanicalSeparatorBlockEntity)
                    || !(level.getBlockEntity(gen.east(8)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity)) continue;
            // Only that scene: nothing but air (or cable, from an earlier run) between them, so rows of machines
            // standing side by side elsewhere are left alone.
            boolean open = true;
            for (int dx : new int[]{1, 2, 3, 5, 6, 7}) {
                var between = level.getBlockState(gen.east(dx));
                if (!between.isAir() && !between.is(cable)) open = false;
            }
            if (!open) continue;
            found++;
            for (int dx : new int[]{1, 2, 3, 5, 6, 7}) {
                BlockPos at = gen.east(dx);
                if (!level.getBlockState(at).isAir() && !level.getBlockState(at).is(cable)) {
                    ResourcefulSheep.LOGGER.warn("EXTEND cable: {} is {}, left alone", at, level.getBlockState(at));
                    continue;
                }
                // (Bridging the gaps between the pads.)
                if (level.getBlockState(at.below()).isAir()) level.setBlock(at.below(), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                level.setBlock(at, cable.withConnections(cable.defaultBlockState(), level, at), 3);
            }
            for (int dx = 1; dx <= 7; dx++) {
                BlockPos at = gen.east(dx);
                if (level.getBlockState(at).is(cable)) level.setBlock(at, cable.withConnections(level.getBlockState(at), level, at), 3);
            }
            var box = new net.minecraft.world.phys.AABB(gen).inflate(16, 10, 12);
            for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, box, e -> true)) {
                String text;
                try { // (private; this is a dev tool)
                    var getText = net.minecraft.world.entity.Display.TextDisplay.class.getDeclaredMethod("getText");
                    getText.setAccessible(true);
                    text = ((net.minecraft.network.chat.Component) getText.invoke(e)).getString();
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
                ResourcefulSheep.LOGGER.warn("EXTEND cable: label '{}' at {}", text, e.blockPosition());
                if (text.toLowerCase(java.util.Locale.ROOT).contains("wireless")) {
                    BlockPos at = BlockPos.containing(e.getX() - 0.5, e.getY() - 0.2, e.getZ() - 0.5);
                    e.discard();
                    label(server, at, text.replace("Wireless power", "A Mycelial Generator cabled to machines")
                            .replace("wireless power", "a Mycelial Generator cabled to machines"), "white");
                }
            }
            ResourcefulSheep.LOGGER.info("EXTEND cable: cabled the generator at {} to its separator and shears", gen);
        }
        ResourcefulSheep.LOGGER.info("EXTEND cable: {} wireless scene(s) found", found);
    }

    /**
     * The upgrade cards' cell (2, 6): three Mechanical Separators side by side, facing the spawn, each with a Creative
     * Generator behind it, a chest of Iron Wool feeding it through a hopper and a chest set in the floor under it taking
     * its outputs: one plain, one with a Speed card, one with an Efficiency card. The Speed one gets through its wool
     * first, the Efficiency one last (but on less power).
     */
    private static void upgradeWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        BlockPos g = origin.offset(0, 0, -3);
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(10, 8, 10),
                e -> e instanceof net.minecraft.world.entity.Display.TextDisplay || e instanceof net.minecraft.world.entity.item.ItemEntity)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-8, 0, -6), g.offset(8, 6, 5))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-8, -1, -6), g.offset(8, -1, 5))) level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(12, 8, 12),
                e -> e instanceof net.minecraft.world.entity.item.ItemEntity)) e.discard();
        Object[][] row = {{-4, null, "No cards"}, {0, ModItems.SPEED_UPGRADE.get(), "Speed Upgrade"}, {4, ModItems.EFFICIENCY_UPGRADE.get(), "Efficiency Upgrade"}};
        for (Object[] r : row) {
            BlockPos at = g.offset((Integer) r[0], 0, 0);
            level.setBlock(at, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
            level.setBlock(at.north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
            level.setBlock(at.above(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN), 3);
            level.setBlock(at.above(2), Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(at.below(), Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH), 3);
            if (level.getBlockEntity(at.above(2)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity feed) {
                for (int i = 0; i < 9; i++) feed.setItem(i, ResourceWoolItem.create(ResourcefulSheep.id("iron"), 64));
            }
            if (r[1] != null && level.getBlockEntity(at) instanceof MechanicalSeparatorBlockEntity be) {
                be.upgrades().setItem(0, new ItemStack((net.minecraft.world.item.Item) r[1]));
            }
            label(server, at.above(4), (String) r[2], r[1] == null ? "gray" : "aqua");
        }
        label(server, g.above(6).north(2), "Upgrade cards: the same Iron Wool, three ways", "gold");
        ResourcefulSheep.LOGGER.info("EXTEND upgrades built round {}", g);
    }

    /** The test world's offsets of its three Hume Field Generators' cores from the cell's middle (see {@link #humeFieldWorldScene}). */
    private static final int[] HUME_WORLD_X = {-6, -1, 5};

    /**
     * Three Hume Field Generators showing their three states: one with two Hume Energy Cubes against it (delivering all
     * it makes), one with only a Powered Furnace against it (wasting what the furnace can't take, and everything once
     * it's full), and one on its own (all of it lost). Its pulse nauseates players within 8 blocks, hence the warning.
     */
    private static void humeFieldWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        BlockPos g = origin.offset(0, 0, -3);
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(12, 8, 12),
                e -> e instanceof net.minecraft.world.entity.Display.TextDisplay || e instanceof net.minecraft.world.entity.item.ItemEntity)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-9, 0, -6), g.offset(9, 6, 5))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-9, -1, -6), g.offset(9, -1, 5))) level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        var block = ModBlocks.HUME_FIELD_GENERATOR.get();
        String[][] labels = {{"Delivering: cubes catch it all", "green"}, {"Wasting: the furnace uses a little", "gold"},
                {"Alone: all of it lost", "red"}};
        for (int k = 0; k < HUME_WORLD_X.length; k++) {
            BlockPos core = g.offset(HUME_WORLD_X[k], 0, -1);
            for (int part = 0; part < 4; part++) {
                for (int layer = 0; layer < 3; layer++) {
                    level.setBlock(core.offset(part & 1, layer, part >> 1), block.defaultBlockState()
                            .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock.PART, part)
                            .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock.LAYER, layer), 3);
                }
            }
            label(server, core.offset(1, k == 1 ? 4 : 3, 2), labels[k][0], labels[k][1]); // the middle one higher, clear of its neighbours
        }
        // Delivering: two Hume Energy Cubes, joined, against the first one's west side.
        BlockPos first = g.offset(HUME_WORLD_X[0], 0, -1);
        level.setBlock(first.west(), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        level.setBlock(first.west().above(), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        // Wasting: a Powered Furnace against the second one's east side, smelting cobblestone from a chest above into a
        // chest under the floor. It takes only the little it uses each tick; the rest is lost.
        BlockPos furnace = g.offset(HUME_WORLD_X[1], 0, -1).east(2);
        level.setBlock(furnace, ModBlocks.POWERED_FURNACE.get().defaultBlockState(), 3);
        level.setBlock(furnace.above(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN), 3);
        level.setBlock(furnace.above(2), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(furnace.below(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN), 3);
        level.setBlock(furnace.below(2), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(furnace.above(2)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity feed) {
            for (int i = 0; i < feed.getContainerSize(); i++) feed.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        label(server, g.above(7).north(3), "Hume Field Generator: random power, no buffer", "light_purple");
        label(server, g.above(6).north(3), "Its pulse gives Nausea within 8 blocks", "yellow");
        linkHumeFieldToPanel(server);
        ResourcefulSheep.LOGGER.info("EXTEND hume field built round {}", g);
    }

    /** Links the test world's three Hume Field Generators (cell (3, 6)) to its Machine Status Panel, if they aren't yet. */
    private static void linkHumeFieldToPanel(MinecraftServer server) {
        ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
        BlockPos panelMaster = new BlockPos(128, 37, 152);
        if (!(level.getBlockEntity(panelMaster) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity panel)) return;
        for (int dx : HUME_WORLD_X) {
            BlockPos core = new BlockPos(101 + dx, 35, 176);
            var result = panel.links().contains(core) ? null : panel.toggleLink(level, core);
            ResourcefulSheep.LOGGER.info("EXTEND hume field at {} linked to the status panel: {}", core, result);
        }
    }

    /** Two Powered Furnaces, one alone and one with two Blaze sheep, both powered and fed by hoppers. */
    private static void poweredFurnaceScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(8, 6, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(8, -1, 4))) level.setBlock(p, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        var blaze = com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.server().byId(ResourcefulSheep.id("blaze")).orElseThrow();
        String[] names = {"Powered Furnace", "With 2 Blaze sheep"};
        for (int i = 0; i < 2; i++) {
            BlockPos at = o.offset(-3 + i * 6, 0, -4);
            level.setBlock(at, ModBlocks.POWERED_FURNACE.get().defaultBlockState()
                    .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.PoweredFurnaceBlock.FACING, Direction.SOUTH), 3);
            level.setBlock(at.north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
            level.setBlock(at.above(), Blocks.HOPPER.defaultBlockState(), 3);
            level.setBlock(at.above(2), Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(at.above(2)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                chest.setItem(0, new ItemStack(Items.RAW_IRON, 64));
                chest.setItem(1, new ItemStack(Items.RAW_GOLD, 32));
            }
            label(server, at.above(3), names[i], i == 0 ? "white" : "gold");
            if (i == 1) {
                if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity be) be.setShowsArea(true);
                for (int side : new int[]{-1, 1}) {
                    var sheep = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    sheep.snapTo(at.getX() + 0.5 + side, at.getY(), at.getZ() + 0.5, 180, 0);
                    sheep.setNoAi(true);
                    level.addFreshEntity(sheep);
                    com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.setInfusion(sheep,
                            com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion.NONE.withResource(blaze.id(), blaze.effectiveInfusionUses()));
                }
            }
        }
    }

    /** The Powered Furnace scene, then the window of the one with Blaze sheep: {@code -PharnessScene=powered_furnace}. */
    private static void poweredFurnaceTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                poweredFurnaceScene(server);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %.1f %.1f 180 25", origin.getX() + 0.5, origin.getY() + 1.5, origin.getZ() + 1.5));
            case 200 -> shot(mc, "101_powered_furnace");
            case 202 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 3.5, origin.getY(), origin.getZ() - 1.5));
            }
            case 210 -> openAt(server, origin.offset(3, 0, -4));
            case 235 -> shot(mc, "101_powered_furnace_gui");
            case 240 -> mc.stop();
            default -> {}
        }
    }

    /** A Woollen Armour set in the given wool colours (helmet, jumper, leggings, boots), coloured as crafting would. */
    private static ItemStack[] woollenSet(net.minecraft.world.item.DyeColor... colours) {
        var items = new net.minecraft.world.item.Item[]{com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_HELMET.get(),
                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_CHESTPLATE.get(),
                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_LEGGINGS.get(),
                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.WOOLLEN_BOOTS.get()};
        ItemStack[] set = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            set[i] = new ItemStack(items[i]);
            set[i].set(net.minecraft.core.component.DataComponents.DYED_COLOR, net.minecraft.world.item.component.DyedItemColor.applyDyes(
                    (net.minecraft.world.item.component.DyedItemColor) null, java.util.List.of(colours[Math.min(i, colours.length - 1)])));
        }
        return set;
    }

    private static final net.minecraft.world.entity.EquipmentSlot[] ARMOUR_SLOTS = {net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};

    /** Four armour stands in Woollen Armour of different wool, on a meadow. */
    private static void woollenArmorScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(8, 6, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(8, -1, 4))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        ItemStack[][] sets = {
                woollenSet(net.minecraft.world.item.DyeColor.WHITE),
                woollenSet(net.minecraft.world.item.DyeColor.RED),
                woollenSet(net.minecraft.world.item.DyeColor.LIGHT_BLUE),
                woollenSet(net.minecraft.world.item.DyeColor.YELLOW, net.minecraft.world.item.DyeColor.GREEN, net.minecraft.world.item.DyeColor.BLUE, net.minecraft.world.item.DyeColor.BROWN)};
        for (int i = 0; i < sets.length; i++) {
            var stand = Vanilla.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            stand.snapTo(o.getX() - 3.75 + i * 2.5, o.getY(), o.getZ() - 4 + 0.5, 180, 0);
            stand.setYHeadRot(180);
            stand.setYBodyRot(180);
            stand.setShowArms(true);
            for (int s = 0; s < 4; s++) stand.setItemSlot(ARMOUR_SLOTS[s], sets[i][s]);
            level.addFreshEntity(stand);
        }
        label(server, o.offset(0, 2, -5), "Woollen Armour", "white");
    }

    /** Woollen Armour on stands, a red jumper being crafted, and a player in a set with the Muffled icon: {@code -PharnessScene=woollen_armor}. */
    private static void woollenArmorTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                woollenArmorScene(server);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %.1f %.1f 180 12", origin.getX() + 0.5, origin.getY() + 0.6, origin.getZ() + 1.0));
            case 200 -> shot(mc, "102_woollen_armor");
            case 202 -> {
                setHudHidden(mc, false);
                command(server, "gamemode survival @a");
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    player.getInventory().clearContent();
                    ItemStack[] set = woollenSet(net.minecraft.world.item.DyeColor.PURPLE);
                    for (int s = 0; s < 4; s++) player.setItemSlot(ARMOUR_SLOTS[s], set[s]);
                    player.getInventory().add(new ItemStack(Vanilla.wool(net.minecraft.world.item.DyeColor.RED), 16));
                    player.getInventory().add(new ItemStack(Vanilla.wool(net.minecraft.world.item.DyeColor.BLUE), 16));
                });
            }
            case 230 -> mc.execute(() -> McClient.setScreen(mc, new InventoryScreen(mc.player)));
            case 260 -> shot(mc, "102_muffled");
            case 262 -> mc.execute(() -> McClient.setScreen(mc, null));
            case 266 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos table = player.blockPosition().above(2);
                player.level().setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new net.minecraft.world.inventory.CraftingMenu(
                        id, inventory, net.minecraft.world.inventory.ContainerLevelAccess.create(player.level(), table)),
                        net.minecraft.network.chat.Component.translatable("container.crafting")));
                if (!(player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu)) return;
                ItemStack red = new ItemStack(Vanilla.wool(net.minecraft.world.item.DyeColor.RED));
                ItemStack blue = new ItemStack(Vanilla.wool(net.minecraft.world.item.DyeColor.BLUE));
                int[] slots = {1, 3, 4, 5, 6, 7, 8, 9};
                for (int i = 0; i < slots.length; i++) menu.getSlot(slots[i]).set((i % 2 == 0 ? red : blue).copy());
                menu.slotsChanged(menu.getSlot(1).container);
                menu.broadcastChanges();
            });
            case 300 -> shot(mc, "102_woollen_armor_crafting");
            case 305 -> mc.stop();
            default -> {}
        }
    }

    /** A Snow Sheep Chiller with three Snow sheep round it, water in its tank, a hopper and chest under it, and its area shown. */
    private static void chillerScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos at = origin.offset(0, 0, -4);
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, 0, -10), origin.offset(6, 4, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, -1, -10), origin.offset(6, -1, 2))) level.setBlock(p, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
        level.setBlock(at, ModBlocks.CHILLER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ChillerBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        level.setBlock(at.below(), Blocks.HOPPER.defaultBlockState(), 3);
        level.setBlock(at.below().south(), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity be) {
            for (int i = 0; i < 8; i++) be.fillFromBucket();
            be.setShowsArea(true);
        }
        var snow = com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.server().byId(ResourcefulSheep.id("snow")).orElseThrow();
        double[][] spots = {{-1.5, -0.5}, {1.5, 0.5}, {0.5, -2.0}};
        for (double[] s : spots) {
            var sheep = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            sheep.snapTo(at.getX() + 0.5 + s[0], at.getY(), at.getZ() + 0.5 + s[1], 200, 0);
            sheep.setNoAi(true);
            level.addFreshEntity(sheep);
            com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.setInfusion(sheep,
                    com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion.NONE.withResource(snow.id(), snow.effectiveInfusionUses()));
        }
        label(server, at.above(), "Snow Sheep Chiller", "aqua");
    }

    /** A Snow Sheep Chiller with three Snow sheep, its area shown, and its window: {@code -PharnessScene=chiller}. */
    private static void chillerTick(Minecraft mc, MinecraftServer server) {
        BlockPos chiller = origin == null ? null : origin.offset(0, 0, -4);
        switch (tick) {
            case 60 -> server.execute(() -> {
                chillerScene(server);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %.1f %.1f 180 28", origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 1.0));
            case 180 -> shot(mc, "100_chiller");
            case 182 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 0.5, origin.getY(), origin.getZ() - 1.5));
            }
            case 190 -> openAt(server, chiller);
            case 215 -> shot(mc, "100_chiller_gui");
            case 220 -> mc.stop();
            default -> {}
        }
    }

    /** Snow sheep walking a grass field and leaving snow behind them: {@code -PharnessScene=snow_sheep}. */
    private static void snowSheepTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-8, 0, -14), origin.offset(8, 4, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-8, -1, -14), origin.offset(8, -1, 2))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                var snow = com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.server().byId(ResourcefulSheep.id("snow")).orElseThrow();
                for (int i = 0; i < 3; i++) {
                    var sheep = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    sheep.snapTo(origin.getX() - 5.5 + i * 4, origin.getY(), origin.getZ() - 10.5 + i * 2, 0, 0);
                    level.addFreshEntity(sheep);
                    com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.setInfusion(sheep,
                            com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion.NONE.withResource(snow.id(), snow.effectiveInfusionUses()));
                    snowWalkers.add(sheep);
                }
                label(server, origin.offset(0, 1, -12), "Snow Sheep", "aqua");
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %.1f %.1f 180 30", origin.getX() + 0.5, origin.getY() + 2.0, origin.getZ() + 1.5));
            default -> {}
        }
        // Walk them back and forth across the field for the trail.
        if (tick >= 80 && tick < 400 && tick % 20 == 0) {
            server.execute(() -> {
                for (int i = 0; i < snowWalkers.size(); i++) {
                    var sheep = snowWalkers.get(i);
                    double tx = origin.getX() + ((tick / 100 + i) % 2 == 0 ? 6.5 : -6.5);
                    double tz = origin.getZ() - 12.5 + ((tick / 60 + i) % 3) * 4;
                    sheep.getNavigation().moveTo(tx, origin.getY(), tz, 1.1);
                }
            });
        }
        if (tick == 420) shot(mc, "99_snow_sheep");
        if (tick == 426) mc.stop();
    }

    private static final java.util.List<net.minecraft.world.entity.animal.sheep.Sheep> snowWalkers = new java.util.ArrayList<>();

    /**
     * A basic ore processing line, as planned: six Iron sheep in a pen that is exactly the Automatic Shears' 3x3 area;
     * pipes over the top carry their wool to a Mechanical Separator; a pipe trench under the row takes its Raw Iron to
     * three Powered Furnaces, its String to a Wool Burner, and the ingots, nuggets and spare String to a chest (the only
     * filter, on the chest's side). A Mycelial Generator watered by a Fluid Reservoir powers it all through a cable
     * behind the row. Machines at the player's feet level; the trench is in the floor.
     */
    private static void oreLineScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-4, 0, -6), o.offset(10, 4, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-4, -1, -6), o.offset(10, -1, 4))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        // The pen: a fence round exactly the shears' 3x3 area (x -1..1, z -3..-1), grass inside and all round it.
        for (int x = -2; x <= 2; x++) for (int z = -4; z <= 0; z++) {
            boolean edge = x == -2 || x == 2 || z == -4 || z == 0;
            if (edge && !(z == 0 && (x == 0 || x == 2))) level.setBlock(o.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
        // The row: shears and separator facing into the pen, three Powered Furnaces, a Wool Burner, a chest.
        level.setBlock(o, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.NORTH), 3);
        level.setBlock(o.east(2), ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.NORTH), 3);
        for (int x = 3; x <= 5; x++) {
            level.setBlock(o.east(x), ModBlocks.POWERED_FURNACE.get().defaultBlockState()
                    .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.PoweredFurnaceBlock.FACING, Direction.SOUTH), 3);
        }
        level.setBlock(o.east(6), ModBlocks.WOOL_BURNER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolBurnerBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(o.east(7), Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH), 3);
        // Power: a Mycelial Generator watered by a Fluid Reservoir on a 2x2 pool, and a cable along the back of the row.
        for (int x = 8; x <= 9; x++) for (int z = 1; z <= 2; z++) {
            level.setBlock(o.offset(x, -2, z), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(o.offset(x, -1, z), Blocks.WATER.defaultBlockState(), 3);
        }
        level.setBlock(o.offset(8, 0, 1), ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
        level.setBlock(o.offset(7, 0, 1), com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(o.offset(7, 0, 1)) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity generator) {
            generator.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT, new ItemStack(Items.RED_MUSHROOM, 64));
        }
        var cable = ModBlocks.ENERGY_CABLE.get();
        for (int x = 0; x <= 6; x++) {
            BlockPos pos = o.offset(x, 0, 1);
            level.setBlock(pos, cable.withConnections(cable.defaultBlockState(), level, pos), 3);
        }
        // Items: network A over the top (shears to the separator's top), network B in a trench under the row.
        var pipe = ModBlocks.ITEM_PIPE.get();
        java.util.List<BlockPos> pipes = new java.util.ArrayList<>();
        for (int x = 0; x <= 2; x++) pipes.add(o.offset(x, 1, 0));
        for (int x = 2; x <= 7; x++) pipes.add(o.offset(x, -1, 0));
        for (BlockPos pos : pipes) level.setBlock(pos, pipe.withConnections(pipe.defaultBlockState(), level, pos), 3);
        // The one filter: the chest only takes ingots, nuggets and String, so Raw Iron goes to the furnaces.
        if (level.getBlockEntity(o.offset(7, -1, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeBlockEntity<?> chestPipe) {
            var side = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.DEFAULT;
            for (String id : new String[]{"iron_ingot", "iron_nugget", "string"}) side = side.toggle(net.minecraft.resources.Identifier.withDefaultNamespace(id));
            chestPipe.setSide(Direction.UP, side);
        }
        // Six Iron sheep, infused for 100 shearings, in the pen.
        int[][] spots = {{-1, -1}, {0, -1}, {1, -1}, {-1, -3}, {0, -3}, {1, -3}};
        for (int[] spot : spots) {
            var sheep = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            sheep.snapTo(o.getX() + spot[0] + 0.5, o.getY(), o.getZ() + spot[1] + 0.5, 180, 0);
            level.addFreshEntity(sheep);
            SheepInfusionEvents.setInfusion(sheep, new SheepInfusion(Optional.of(ResourcefulSheep.id("iron")), 100, 0));
        }
        label(server, o.offset(3, 2, -2), "Ore processing line", "white");
    }

    /**
     * The Thermal Reaction Chamber for the test world, built to run safely for as long as the world is open: one
     * balanced chamber facing south with three Blaze sheep in a nether-brick pen on its hot (west) side and three Snow
     * sheep in a spruce pen on its cold (east) side, standing still so the balance never tips, 16 Blaze Rods and Blue
     * Ice to burn together, its pens outlined, and six Fluid Reservoirs on a 3x3 pool behind it piping in 300 mB a tick
     * (it drinks 250).
     */
    private static void thermalWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, 0, -12), o.offset(7, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, -1, -12), o.offset(7, -1, 3))) {
            level.setBlock(p, Math.floorMod(p.getX() + p.getZ(), 2) == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        }
        BlockPos at = o.offset(0, 0, -4);
        level.setBlock(at, ModBlocks.THERMAL_REACTION_CHAMBER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ThermalReactionChamberBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity be) {
            be.setItem(0, new ItemStack(Items.BLAZE_ROD, 16));
            be.setItem(1, new ItemStack(Items.BLUE_ICE, 16));
            be.setShowsArea(true);
            for (int i = 0; i < 16; i++) be.fillFromBucket(); // a full tank to start
        }
        // Water: a 3x3 pool sunk into the platform behind it (x -1..1, z -10..-8) with six reservoirs on it, either side of a
        // fluid pipe that runs from the chamber's back to the end of the pool.
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, -2, -10), o.offset(1, -2, -8))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, -1, -10), o.offset(1, -1, -8))) level.setBlock(p, Blocks.WATER.defaultBlockState(), 3);
        for (int z = -10; z <= -8; z++) {
            level.setBlock(o.offset(-1, 0, z), ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
            level.setBlock(o.offset(1, 0, z), ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
        }
        var pipe = ModBlocks.FLUID_PIPE.get();
        java.util.List<BlockPos> pipes = new java.util.ArrayList<>();
        for (int z = -10; z <= -5; z++) pipes.add(o.offset(0, 0, z));
        for (BlockPos pos : pipes) level.setBlock(pos, pipe.defaultBlockState(), 3);
        for (BlockPos pos : pipes) level.setBlock(pos, pipe.withConnections(pipe.defaultBlockState(), level, pos), 3);
        // The pens: 5x5 on each side (x -5..-1 and 1..5, z -6..-2), fenced round their outer edges.
        for (int side = -1; side <= 1; side += 2) {
            var fence = (side < 0 ? Blocks.NETHER_BRICK_FENCE : Blocks.SPRUCE_FENCE).defaultBlockState();
            for (int x = 1; x <= 6; x++) for (int z = -7; z <= -1; z++) {
                boolean edge = x == 6 || z == -7 || z == -1;
                if (edge) level.setBlock(o.offset(side * x, 0, z), fence, 3);
            }
            for (int i = 0; i < 3; i++) {
                Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                sheep.snapTo(at.getX() + 0.5 + side * (1.6 + (i % 2) * 2.2), at.getY(), at.getZ() + 0.5 + (i - 1) * 1.6, side > 0 ? 90 : 270, 0);
                sheep.setNoAi(true); // they stand still, so the herds stay even
                sheep.setPersistenceRequired();
                SheepInfusionEvents.setInfusion(sheep, new SheepInfusion(Optional.of(ResourcefulSheep.id(side < 0 ? "blaze" : "snow")), 100, 0));
                level.addFreshEntity(sheep);
            }
        }
        // Something to take its power, so it keeps reacting for a while: an Energy Meter on its front reading the flow into
        // a bank of eight Hume Energy Cubes (8,000,000 FE, about ten minutes at full boost) between the pens' fences.
        level.setBlock(at.south(), ModBlocks.ENERGY_METER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING, Direction.SOUTH)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN, Direction.EAST), 3);
        for (int z = -2; z <= 1; z++) for (int y = 0; y <= 1; y++) level.setBlock(o.offset(0, y, z), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        label(server, at.above(2), "Thermal Reaction Chamber", "gold");
    }

    /** Where {@link #echoWorldScene} put the redstone block that starts its clock, to be swapped back for stone a tick later. */
    private static @org.jspecify.annotations.Nullable BlockPos echoKick;

    /**
     * The Echo Chamber for the test world, built to run for as long as the world is open: one chamber facing south on a
     * sculk floor, four Warden sheep standing still in its fenced listening pen, its pen outlined, and a repeater clock
     * beside it striking two note blocks about 1.25 times a second (noise level 6, in the sweet spot). Its power goes
     * through an Energy Meter behind its power port into a bank of eight Hume Energy Cubes. No sculk sensors: they'd
     * click back at the note blocks and make the noise less steady. Call {@link #echoWorldKick} a tick later.
     */
    private static void echoWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, 0, -12), o.offset(7, 6, 5))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, -1, -12), o.offset(7, -1, 5))) {
            level.setBlock(p, Math.floorMod(p.getX() + p.getZ(), 3) == 0 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.SCULK.defaultBlockState(), 2);
        }
        // The chamber: its front middle block here, facing south; it fills the 3x3x3 space north of and above it.
        BlockPos front = o.offset(0, 0, -2);
        com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.fill(level, front, ModBlocks.ECHO_CHAMBER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(front.north()) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity be) be.setShowsArea(true);
        // The pen (x -2..2, z -1..3 from here) fenced round its sides and front, and four Warden sheep in it facing the dish.
        var fence = Blocks.DARK_OAK_FENCE.defaultBlockState();
        for (int z = -1; z <= 4; z++) {
            level.setBlock(o.offset(-3, 0, z), fence, 3);
            level.setBlock(o.offset(3, 0, z), fence, 3);
        }
        for (int x = -2; x <= 2; x++) level.setBlock(o.offset(x, 0, 4), fence, 3);
        for (double[] p : new double[][]{{-1.2, 0.5}, {1.2, 0.5}, {-0.8, 2.4}, {1.0, 2.6}}) {
            Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sheep.snapTo(o.getX() + 0.5 + p[0], o.getY(), o.getZ() + 0.5 + p[1], 180, 0);
            sheep.setNoAi(true);
            sheep.setPersistenceRequired();
            SheepInfusionEvents.setInfusion(sheep, new SheepInfusion(Optional.of(ResourcefulSheep.id("warden")), 100, 0));
            level.addFreshEntity(sheep);
        }
        // The noise: a 3x3 ring east of the chamber (x 4..6, z -4..-2). Repeaters on its edges, at their longest delay, carry
        // one pulse round it every 1.6 seconds; its corners are two note blocks (struck as the pulse passes) and two stone.
        BlockPos c = o.offset(4, 0, -4);
        var stone = Blocks.STONE.defaultBlockState();
        level.setBlock(c, stone, 3);
        level.setBlock(c.offset(2, 0, 2), stone, 3);
        level.setBlock(c.offset(2, 0, 0), Blocks.NOTE_BLOCK.defaultBlockState(), 3);
        level.setBlock(c.offset(0, 0, 2), Blocks.NOTE_BLOCK.defaultBlockState(), 3);
        var repeater = Blocks.REPEATER.defaultBlockState().setValue(net.minecraft.world.level.block.RepeaterBlock.DELAY, 4);
        var facing = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING; // a repeater takes its input from the way it faces
        level.setBlock(c.offset(1, 0, 0), repeater.setValue(facing, Direction.WEST), 3);
        level.setBlock(c.offset(2, 0, 1), repeater.setValue(facing, Direction.NORTH), 3);
        level.setBlock(c.offset(1, 0, 2), repeater.setValue(facing, Direction.EAST), 3);
        level.setBlock(c.offset(0, 0, 1), repeater.setValue(facing, Direction.SOUTH), 3);
        level.setBlock(c, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3); // the kick that starts the pulse
        echoKick = c;
        // Its power: an Energy Meter behind its power port (the middle of its back) into a bank of eight Hume Energy Cubes.
        level.setBlock(o.offset(0, 0, -5), ModBlocks.ENERGY_METER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING, Direction.NORTH)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN, Direction.EAST), 3);
        for (int z = -9; z <= -6; z++) for (int y = 0; y <= 1; y++) level.setBlock(o.offset(0, y, z), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        label(server, o.offset(0, 5, -3), "Echo Chamber", "aqua");
    }

    /**
     * The Static Generator for the test world: one facing south on grass, its 5x5 pen fenced (with a gate at the front)
     * and outlined, four fresh Copper Combs in it, and six plain sheep standing still at them, which make 600 FE/t. Its
     * power goes through an Energy Meter behind it into a bank of eight Hume Energy Cubes, so it runs for about 12
     * minutes before its cubes fill and its dome overcharges.
     */
    private static void staticWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, 0, -12), o.offset(7, 6, 5))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, -1, -12), o.offset(7, -1, 5))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        BlockPos g = o.offset(0, 0, -3);
        var gen = ModBlocks.STATIC_GENERATOR.get().defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock.FACING, Direction.SOUTH);
        level.setBlock(g, gen, 3);
        level.setBlock(g.above(), gen.setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
        if (level.getBlockEntity(g) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity be) be.setShowsArea(true);
        var comb = ModBlocks.COPPER_COMB.get().defaultBlockState();
        for (int[] c : new int[][]{{-2, 1}, {2, 1}, {-2, -1}, {2, -1}}) level.setBlock(g.offset(c[0], 0, c[1]), comb, 3);
        // The fence round the pen (x -2..2, z -2..2 from the generator), a gate in the front, a gap at the back for the cubes.
        var fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int d = -3; d <= 3; d++) {
            level.setBlock(g.offset(-3, 0, d), fence, 3);
            level.setBlock(g.offset(3, 0, d), fence, 3);
            if (d != 0) level.setBlock(g.offset(d, 0, -3), fence, 3);
            level.setBlock(g.offset(d, 0, 3), d == 0 ? Blocks.SPRUCE_FENCE_GATE.defaultBlockState() : fence, 3);
        }
        // Six plain sheep, each within reach of a comb, standing still.
        for (double[] p : new double[][]{{-0.6, 2.3, 270}, {1.6, 2.3, 90}, {-0.6, -0.3, 270}, {1.6, -0.3, 90}, {-1.3, 2.7, 180}, {2.3, 2.7, 180}}) {
            Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sheep.snapTo(g.getX() + p[0], g.getY(), g.getZ() + p[1], (float) p[2], 0);
            sheep.setNoAi(true);
            sheep.setPersistenceRequired();
            level.addFreshEntity(sheep);
        }
        // Its power: an Energy Meter behind its base into a bank of eight Hume Energy Cubes.
        level.setBlock(g.north(), ModBlocks.ENERGY_METER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING, Direction.NORTH)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN, Direction.EAST), 3);
        for (int z = -5; z <= -2; z++) for (int y = 0; y <= 1; y++) level.setBlock(g.offset(0, y, z), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        label(server, g.above(3), "Static Generator", "gold");
    }

    /**
     * The Grass Seeder for the test world: one in the middle of a fenced 5x5 pen of grass eaten into patches of dirt,
     * with a gate at the front, 64 Bone Meal, Trim on, its area outlined, and four plain sheep free to wander and graze. Automatic
     * Shears in the back fence (powered by a Creative Generator behind them) shear the sheep that wander in front of
     * them, and the Seeder gets them grazing and regrowing their wool: the pen runs itself.
     */
    private static void seederWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin, g = o.offset(0, 0, -3);
        // (Built again over an earlier copy: its sheep and label go first.)
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(7, 6, 7),
                e -> e instanceof Sheep || e instanceof net.minecraft.world.entity.Display.TextDisplay)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-6, 0, -6), g.offset(6, 6, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        var rnd = new java.util.Random(8);
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-6, -1, -6), g.offset(6, -1, 6))) {
            boolean inPen = Math.abs(p.getX() - g.getX()) <= 2 && Math.abs(p.getZ() - g.getZ()) <= 2;
            level.setBlock(p, (inPen && rnd.nextInt(5) < 2 ? Blocks.DIRT : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
        }
        level.setBlock(g, ModBlocks.GRASS_SEEDER.get().defaultBlockState(), 3);
        if (level.getBlockEntity(g) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity be) {
            be.setShowsArea(true);
            be.setTrim(true); // so it never overgrows and hides its sheep from the shears
            be.insertBoneMeal(new ItemStack(Items.BONE_MEAL, 64));
        }
        // The fence round the pen, a gate at the front, and the shears in the middle of the back.
        var fence = Blocks.OAK_FENCE.defaultBlockState();
        for (int d = -3; d <= 3; d++) {
            level.setBlock(g.offset(-3, 0, d), fence, 3);
            level.setBlock(g.offset(3, 0, d), fence, 3);
            if (d != 0) level.setBlock(g.offset(d, 0, -3), fence, 3);
            level.setBlock(g.offset(d, 0, 3), d == 0 ? Blocks.OAK_FENCE_GATE.defaultBlockState() : fence, 3);
        }
        level.setBlock(g.offset(0, 0, -3), ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(g.offset(0, 0, -4), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        for (double[] p : new double[][]{{-1.5, -1.2}, {1.6, -0.9}, {-1.2, 1.7}, {1.4, 1.5}}) {
            Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sheep.snapTo(g.getX() + 0.5 + p[0], g.getY(), g.getZ() + 0.5 + p[1], (float) (p[0] * 60), 0);
            sheep.setPersistenceRequired();
            level.addFreshEntity(sheep);
        }
        label(server, g.above(3), "Grass Seeder", "green");
    }

    /**
     * The Herd Counter for the test world: a fenced 5x5 pen (a gate at the front) of eight sheep free to wander, Iron,
     * Gold, Copper and plain, one a lamb, watched by two counters. The one on the west counts every sheep in Full mode at
     * 8, wired through a comparator to a lamp that's lit while the whole herd is in; the one on the north counts the Iron
     * sheep ready to shear, Steady, through a comparator to a lamp of its own. Both areas are outlined.
     */
    private static void herdWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin, g = o.offset(0, 0, -3);
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(8, 6, 8),
                e -> e instanceof Sheep || e instanceof net.minecraft.world.entity.Display.TextDisplay)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-7, 0, -7), g.offset(6, 6, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-7, -1, -7), g.offset(6, -1, 6))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        var fence = Blocks.OAK_FENCE.defaultBlockState();
        for (int d = -3; d <= 3; d++) {
            level.setBlock(g.offset(-3, 0, d), fence, 3);
            level.setBlock(g.offset(3, 0, d), fence, 3);
            level.setBlock(g.offset(d, 0, -3), fence, 3);
            level.setBlock(g.offset(d, 0, 3), d == 0 ? Blocks.OAK_FENCE_GATE.defaultBlockState() : fence, 3);
        }
        var facing = com.jax3.barelyfunctionalcontraptions.sheep.blocks.HerdCounterBlock.FACING;
        var diode = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
        // West: every sheep, Full at 8.
        BlockPos west = g.offset(-4, 0, 0);
        level.setBlock(west, ModBlocks.HERD_COUNTER.get().defaultBlockState().setValue(facing, Direction.EAST), 3);
        level.setBlock(west.west(), Blocks.COMPARATOR.defaultBlockState().setValue(diode, Direction.EAST), 3);
        level.setBlock(west.west(2), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        if (level.getBlockEntity(west) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity be) {
            be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.FULL);
            be.setTarget(8);
            be.setShowsArea(true);
        }
        // North: the Iron sheep ready to shear, Steady.
        BlockPos north = g.offset(0, 0, -4);
        level.setBlock(north, ModBlocks.HERD_COUNTER.get().defaultBlockState().setValue(facing, Direction.SOUTH), 3);
        level.setBlock(north.north(), Blocks.COMPARATOR.defaultBlockState().setValue(diode, Direction.SOUTH), 3);
        level.setBlock(north.north(2), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        if (level.getBlockEntity(north) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity be) {
            be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.READY);
            be.setFilter(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("iron"), 1));
            be.setSteady(true);
        }
        String[] kinds = {"iron", "iron", "iron", "gold", "gold", "copper", "", ""};
        double[][] spots = {{-1.5, -1.5}, {0.5, -1.8}, {1.6, 0.2}, {-1.7, 0.6}, {-0.2, 1.6}, {1.5, 1.7}, {0.2, -0.1}, {-1.4, 2.2}};
        for (int i = 0; i < kinds.length; i++) {
            Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sheep.snapTo(g.getX() + 0.5 + spots[i][0], g.getY(), g.getZ() + 0.5 + spots[i][1], i * 45, 0);
            sheep.setPersistenceRequired();
            if (!kinds[i].isEmpty()) {
                var resource = com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.server().byId(ResourcefulSheep.id(kinds[i])).orElseThrow();
                SheepInfusionEvents.setInfusion(sheep, SheepInfusion.NONE.withResource(resource.id(), resource.effectiveInfusionUses()));
            }
            if (i == 4) sheep.setSheared(true);
            if (i == 7) sheep.setAge(-24000);
            level.addFreshEntity(sheep);
        }
        label(server, g.above(3), "Herd Counter", "aqua");
    }

    /**
     * The Machine Status Panel for the test world: nine panels joined into a 3x3 screen on a stone-brick wall, linked to
     * the real machines of the scenes round it on the platform (the nearest of each kind within its range: the Echo
     * Chamber, the Thermal Reaction Chamber, the Grass Seeder, the Herd Counters, the Powered Furnaces and so on), with a
     * comparator in front of it lighting a lamp while any of them has a problem.
     */
    private static void panelWorldScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin, g = o.offset(0, 0, -3);
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(g).inflate(8, 6, 8),
                e -> e instanceof net.minecraft.world.entity.Display.TextDisplay)) e.discard();
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-7, 0, -7), g.offset(6, 6, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-4, -1, -5), g.offset(4, -1, 3))) level.setBlock(p, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(g.offset(-2, 0, -3), g.offset(2, 3, -3))) level.setBlock(p, Blocks.STONE_BRICKS.defaultBlockState(), 2);
        var panelState = ModBlocks.STATUS_PANEL.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.FACING, Direction.SOUTH);
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) level.setBlock(g.offset(x, y, -2), panelState, 3);
        BlockPos topLeft = g.offset(-1, 2, -2);
        level.setBlock(g.offset(0, 0, -1), Blocks.COMPARATOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
        level.setBlock(g, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        if (!(level.getBlockEntity(topLeft) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity panel)) return;
        // The nearest machine of each kind within its range, nearest kinds first.
        int range = com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity.range() - 2;
        java.util.Map<net.minecraft.world.level.block.Block, BlockPos> nearest = new java.util.HashMap<>();
        for (int cx = (topLeft.getX() - range) >> 4; cx <= (topLeft.getX() + range) >> 4; cx++) {
            for (int cz = (topLeft.getZ() - range) >> 4; cz <= (topLeft.getZ() + range) >> 4; cz++) {
                for (var be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (be instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity
                            || !p.closerThan(topLeft, range) || !com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.supports(be)) continue;
                    var block = be.getBlockState().getBlock();
                    BlockPos best = nearest.get(block);
                    if (best == null || p.distSqr(topLeft) < best.distSqr(topLeft)) nearest.put(block, p.immutable());
                }
            }
        }
        nearest.values().stream().sorted(java.util.Comparator.comparingDouble(p -> p.distSqr(topLeft))).limit(20)
                .forEach(p -> panel.toggleLink(level, p));
        label(server, g.above(4).relative(Direction.NORTH, 2), "Machine Status Panel", "aqua");
        ResourcefulSheep.LOGGER.info("EXTEND status panel linked {} machines: {}", panel.links().size(), panel.rows().stream()
                .map(r -> r.name().replace("block.barelyfunctionalcontraptions.", "")).toList());
    }

    /** Swaps the redstone block that started the Echo Chamber's clock back for stone, leaving the pulse going round. */
    private static void echoWorldKick(MinecraftServer server) {
        if (echoKick == null) return;
        server.getPlayerList().getPlayers().getFirst().level().setBlock(echoKick, Blocks.STONE.defaultBlockState(), 3);
        echoKick = null;
    }

    /**
     * A floor and ceiling pair of Zero Point Energy Modules three blocks apart, close up from the side, shot a few ticks
     * apart to show the emitters turning against the beam: {@code -PharnessScene=zero_point_spin}.
     */
    private static void zeroPointSpinTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos o = origin.offset(0, 0, -3);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, 0, -2), o.offset(2, 5, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                var module = ModBlocks.ZERO_POINT_ENERGY_MODULE.get().defaultBlockState();
                var facing = com.jax3.barelyfunctionalcontraptions.sheep.blocks.ZeroPointEnergyModuleBlock.FACING;
                level.setBlock(o, module.setValue(facing, Direction.UP), 3);
                level.setBlock(o.above(4), module.setValue(facing, Direction.DOWN), 3);
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 28", origin.getX() + 0.5, origin.getY() + 1.6, origin.getZ() - 1.2));
            case 120 -> shot(mc, "zp_spin_0");
            case 124 -> shot(mc, "zp_spin_1");
            case 128 -> shot(mc, "zp_spin_2");
            case 130 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 -28", origin.getX() + 0.5, origin.getY() + 2.6, origin.getZ() - 1.2));
            case 160 -> shot(mc, "zp_spin_top_0");
            case 164 -> shot(mc, "zp_spin_top_1");
            case 170 -> mc.stop();
            default -> {}
        }
    }

    /** The ore line, from above the front, a while after it starts, then the chest it fills: {@code -PharnessScene=ore_line}. */
    private static void oreLineTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                oreLineScene(server);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 34", origin.getX() + 3.5, origin.getY() + 5.0, origin.getZ() + 8.0));
            case 600 -> shot(mc, "104_ore_line");
            case 602 -> server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                StringBuilder report = new StringBuilder("ORELINE");
                if (level.getBlockEntity(origin.east(7)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    for (int i = 0; i < chest.getContainerSize(); i++) if (!chest.getItem(i).isEmpty()) report.append(" chest:").append(chest.getItem(i));
                }
                for (int x = 3; x <= 5; x++) {
                    if (level.getBlockEntity(origin.east(x)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity f) {
                        report.append(" furnace").append(x).append(':').append(f.getItem(0)).append('/').append(f.output()).append('/').append(f.status());
                    }
                }
                if (level.getBlockEntity(origin.east(2)) instanceof MechanicalSeparatorBlockEntity sep) report.append(" separatorIn:").append(sep.getInventory().getStack(SeparatorInventory.INPUT));
                if (level.getBlockEntity(origin.east(6)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity burner) report.append(" burner:").append(burner.fuel()).append(" gen ").append(burner.generating());
                ResourcefulSheep.LOGGER.warn(report.toString());
            });
            case 604 -> {
                setHudHidden(mc, false);
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 7.5, origin.getY(), origin.getZ() + 2.5));
            }
            case 612 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                if (player.level().getBlockEntity(origin.east(7)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) player.openMenu(chest);
            });
            case 635 -> shot(mc, "104_ore_line_chest");
            case 640 -> mc.stop();
            default -> {}
        }
    }

    /**
     * Three Sheep Treadmills fed Hay Bales in a fenced pen, a sheep walking the left one and a sheared sheep the middle
     * one, the right one empty, and one turned east.
     */
    private static void sheepTreadmillScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, 0, -8), origin.offset(6, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, -1, -8), origin.offset(6, -1, 3))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        var block = ModBlocks.SHEEP_TREADMILL.get();
        var facing = com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.FACING;
        var part = com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.PART;
        for (int i = -1; i <= 1; i++) {
            BlockPos back = origin.offset(i * 2, 0, -3);
            level.setBlock(back, block.defaultBlockState().setValue(facing, Direction.NORTH).setValue(part, 0), 3);
            level.setBlock(back.north(), block.defaultBlockState().setValue(facing, Direction.NORTH).setValue(part, 1), 3);
            if (level.getBlockEntity(back) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity be) {
                be.setItem(0, new ItemStack(Items.HAY_BLOCK, 16)); // over an hour of walking
            }
            if (i == 1) continue; // the right one waits for a sheep
            var sheep = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            sheep.snapTo(back.getX() + 0.5, back.getY() + 0.4, back.getZ() + 0.1, 180, 0);
            if (i == 0) sheep.setSheared(true);
            level.addFreshEntity(sheep);
        }
        // A fence round the pen, so tired sheep wandering off to rest stay on the platform.
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, 0, -8), origin.offset(6, 0, 3))) {
            if (p.getX() == origin.getX() - 6 || p.getX() == origin.getX() + 6 || p.getZ() == origin.getZ() - 8 || p.getZ() == origin.getZ() + 3) {
                level.setBlock(p, Blocks.OAK_FENCE.defaultBlockState(), 3);
            }
        }
        // One turned east, to show it faces the way it's placed.
        BlockPos turned = origin.offset(4, 0, -6);
        level.setBlock(turned, block.defaultBlockState().setValue(facing, Direction.EAST).setValue(part, 0), 3);
        level.setBlock(turned.east(), block.defaultBlockState().setValue(facing, Direction.EAST).setValue(part, 1), 3);
    }

    /**
     * Three Sheep Treadmills with wheat, a sheep walking the left one and a sheared sheep the middle one, the right one
     * empty, and one turned east; then the middle one's window: {@code -PharnessScene=sheep_treadmill}.
     */
    private static void sheepTreadmillTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                sheepTreadmillScene(server);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 205 28", origin.getX() - 1.5, origin.getY() + 1.2, origin.getZ() + 1.5));
            case 120 -> shot(mc, "103_sheep_treadmill");
            case 122 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 270 20", origin.getX() - 4.2, origin.getY() + 0.4, origin.getZ() - 3.5));
            case 150 -> shot(mc, "103_sheep_treadmill_side");
            case 153 -> shot(mc, "103_sheep_treadmill_side_b"); // a moment later: the legs and belt have moved
            case 156 -> {
                setHudHidden(mc, false);
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 0.5, origin.getY(), origin.getZ() - 0.5));
            }
            case 165 -> openAt(server, origin.offset(0, 0, -3));
            case 190 -> shot(mc, "103_sheep_treadmill_gui");
            case 192 -> {
                mc.execute(() -> McClient.setScreen(mc, null));
                // A loose sheep a few blocks from the empty treadmill: it should be called over and hop on.
                server.execute(() -> {
                    ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                    var sheep = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    sheep.snapTo(origin.getX() + 5.5, origin.getY(), origin.getZ() + 1.5, 90, 0);
                    sheep.setColor(net.minecraft.world.item.DyeColor.PINK);
                    level.addFreshEntity(sheep);
                });
                setHudHidden(mc, true);
                // Creative, not spectator, so the player nearby earns "Running Ewe Ragged" when the sheep starts walking.
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 205 28", origin.getX() - 1.5, origin.getY() + 1.2, origin.getZ() + 1.5));
            }
            case 205 -> shot(mc, "103_sheep_treadmill_called_0");
            case 400 -> {
                shot(mc, "103_sheep_treadmill_called_1");
                server.execute(() -> {
                    var level = server.getPlayerList().getPlayers().getFirst().level();
                    var be = level.getBlockEntity(origin.offset(2, 0, -3));
                    ResourcefulSheep.LOGGER.warn("CALLED state {}, advancement {}", be instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity t ? t.state() : null,
                            com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(server.getPlayerList().getPlayers().getFirst(),
                                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.SHEEP_TREADMILL));
                });
            }
            case 405 -> mc.stop();
            default -> {}
        }
    }

    /** The chambers of the Thermal Reaction Chamber scene, kept topped up with water: balanced, running cold, running hot. */
    private static final java.util.List<BlockPos> THERMAL_CHAMBERS = new java.util.ArrayList<>();

    /**
     * Three Thermal Reaction Chambers facing the camera on a stone floor, Blaze sheep in the pen on each one's hot (left)
     * side and Snow sheep on its cold (right) side, a Fluid Reservoir behind each: the middle one balanced with three
     * pairs and both boosts, its pens outlined; the right one with a Snow sheep too many (it freezes); the left one with a
     * Blaze sheep too many (it runs hot).
     */
    private static void thermalScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-22, 0, -12), origin.offset(22, 6, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-22, -1, -12), origin.offset(22, -1, 4))) {
            level.setBlock(p, Math.floorMod(p.getX() + p.getZ(), 2) == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        }
        THERMAL_CHAMBERS.clear();
        int[][] herds = {{3, 3}, {1, 2}, {2, 1}}; // middle, right, left
        int[] offsets = {0, 15, -15};
        for (int c = 0; c < 3; c++) {
            BlockPos at = origin.offset(offsets[c], 0, -5);
            level.setBlock(at, ModBlocks.THERMAL_REACTION_CHAMBER.get().defaultBlockState()
                    .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.ThermalReactionChamberBlock.FACING, Direction.SOUTH), 3);
            level.setBlock(at.north(), ModBlocks.FLUID_RESERVOIR.get().defaultBlockState(), 3);
            THERMAL_CHAMBERS.add(at);
            if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity be) {
                if (c == 0) {
                    be.setItem(0, new ItemStack(Items.BLAZE_ROD, 16));
                    be.setItem(1, new ItemStack(Items.BLUE_ICE, 16));
                    be.setShowsArea(true);
                }
            }
            // Facing south, the hot side is west (the camera's left) and the cold side east.
            for (int side = 0; side < 2; side++) {
                int dir = side == 0 ? -1 : 1;
                // A low fence round the pen, on its outside edge.
                for (BlockPos p : BlockPos.betweenClosed(at.offset(dir > 0 ? 1 : -6, 0, -3), at.offset(dir > 0 ? 6 : -1, 0, 3))) {
                    boolean edge = p.getX() == at.getX() + dir * 6 || p.getZ() == at.getZ() - 3 || p.getZ() == at.getZ() + 3;
                    if (edge) level.setBlock(p, (side == 0 ? Blocks.NETHER_BRICK_FENCE : Blocks.SPRUCE_FENCE).defaultBlockState(), 3);
                }
                for (int i = 0; i < herds[c][side]; i++) {
                    Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    sheep.snapTo(at.getX() + 0.5 + dir * (1.6 + (i % 2) * 2.2), at.getY(), at.getZ() + 0.5 + (i - 1) * 1.6, dir > 0 ? 90 : 270, 0);
                    sheep.setNoAi(true);
                    SheepInfusionEvents.setInfusion(sheep, new SheepInfusion(Optional.of(ResourcefulSheep.id(side == 0 ? "blaze" : "snow")), 9, 0));
                    level.addFreshEntity(sheep);
                }
            }
        }
    }

    /** Tops up each chamber's water, as the reservoir behind it would. */
    private static void waterThermalChambers(MinecraftServer server) {
        server.execute(() -> {
            ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
            for (BlockPos at : THERMAL_CHAMBERS) {
                var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, Direction.NORTH);
                if (handler == null) continue;
                try (Transaction tx = Transaction.openRoot()) {
                    handler.insert(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity.WATER, 1000, tx);
                    tx.commit();
                }
                // And something using its power, so its buffer never fills and it keeps reacting.
                var energy = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, at, Direction.UP);
                if (energy != null) {
                    try (Transaction tx = Transaction.openRoot()) {
                        energy.extract(Integer.MAX_VALUE, tx);
                        tx.commit();
                    }
                }
            }
        });
    }

    /**
     * The Thermal Reaction Chamber: three chambers reacting, a close-up, the middle one's window, the left one running
     * hot and the right one frozen solid: {@code -PharnessScene=thermal_reaction_chamber}.
     */
    private static void thermalTick(Minecraft mc, MinecraftServer server) {
        if (tick > 60 && !THERMAL_CHAMBERS.isEmpty()) waterThermalChambers(server);
        switch (tick) {
            case 60 -> server.execute(() -> {
                thermalScene(server);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 32", origin.getX() + 0.5, origin.getY() + 6.5, origin.getZ() + 5.5));
            case 130 -> shot(mc, "105_thermal_reaction_chamber");
            case 132 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 205 22", origin.getX() - 1.2, origin.getY() + 1.6, origin.getZ() - 2.0));
            case 160 -> shot(mc, "105_thermal_reaction_chamber_close");
            case 162 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 150 22", origin.getX() + 2.2, origin.getY() + 1.6, origin.getZ() - 2.0));
            case 190 -> shot(mc, "105_thermal_reaction_chamber_cold_side");
            case 192 -> {
                setHudHidden(mc, false);
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 0.5, origin.getY(), origin.getZ() - 2.5));
            }
            case 200 -> openAt(server, origin.offset(0, 0, -5));
            case 230 -> shot(mc, "105_thermal_reaction_chamber_gui");
            case 232 -> mc.execute(() -> McClient.setScreen(mc, null));
            case 236 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() - 14.5, origin.getY(), origin.getZ() - 2.5));
            case 250 -> openAt(server, origin.offset(-15, 0, -5));
            case 300 -> shot(mc, "105_thermal_reaction_chamber_gui_hot");
            case 302 -> {
                mc.execute(() -> McClient.setScreen(mc, null));
                command(server, "gamemode spectator @a");
                setHudHidden(mc, true);
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 25", origin.getX() - 14.5, origin.getY() + 2.4, origin.getZ() - 1.5));
            }
            case 340 -> shot(mc, "105_thermal_reaction_chamber_running_hot");
            case 342 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 25", origin.getX() + 15.5, origin.getY() + 2.4, origin.getZ() - 1.5));
            case 760 -> shot(mc, "105_thermal_reaction_chamber_frozen");
            case 762 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 32", origin.getX() - 14.5, origin.getY() + 6.5, origin.getZ() + 5.5));
            case 790 -> shot(mc, "105_thermal_reaction_chamber_meltdown");
            // Jade on a Blaze sheep (its fleece wear) and on the balanced chamber, then the JEI pages.
            case 792 -> {
                setHudHidden(mc, false);
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 40", origin.getX() - 1.1, origin.getY(), origin.getZ() - 2.6));
            }
            case 815 -> shot(mc, "105_thermal_jade_sheep");
            case 817 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 35", origin.getX() + 0.5, origin.getY(), origin.getZ() - 2.6));
            case 840 -> shot(mc, "105_thermal_jade_chamber");
            case 842 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showThermal();
                }
            }
            case 865 -> shot(mc, "105_thermal_jei");
            case 867 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showThermalInfo();
                }
            }
            case 890 -> shot(mc, "105_thermal_jei_info");
            case 895 -> mc.stop();
            default -> {}
        }
    }

    /** The work-in-progress Hume Field Generator model, from the front and from above: {@code -PharnessScene=hume_field}. */
    private static void humeFieldTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-4, 0, -6), origin.offset(4, 5, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-4, -1, -6), origin.offset(4, -1, 2))) level.setBlock(p, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
                // The 2x2 machine: corner 0 at the north-west, the other three corners east and south of it.
                BlockPos at = origin.offset(-1, 0, -4);
                var block = ModBlocks.HUME_FIELD_GENERATOR.get();
                for (int part = 0; part < 4; part++) {
                    for (int layer = 0; layer < 3; layer++) {
                        level.setBlock(at.offset(part & 1, layer, part >> 1), block.defaultBlockState()
                                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock.PART, part)
                                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock.LAYER, layer), 3);
                    }
                }
                level.setBlock(origin.offset(2, 0, -3), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
                // A cube against its east side, in its middle layer, so it delivers its power.
                level.setBlock(origin.offset(1, 1, -3), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
            });
            case 70 -> setHudHidden(mc, true);
            // Eye height is 1.62 above these, so the camera sits about level with the cap.
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 15", origin.getX() + 0.5, origin.getY() + 0.4, origin.getZ() + 1.8));
            case 110 -> shot(mc, "wip_hume_field_front");
            case 112 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 145 38", origin.getX() + 2.5, origin.getY() + 1.4, origin.getZ() + 0.5));
            case 140 -> shot(mc, "wip_hume_field_above");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 2", origin.getX() + 0.0, origin.getY() - 0.4, origin.getZ() - 0.2));
            case 170 -> shot(mc, "wip_hume_field_close");
            // Jade on it, from in front, in creative with the HUD back and the chat cleared.
            case 172 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 20", origin.getX() + 0.0, origin.getY() + 0.0, origin.getZ() - 0.6));
            }
            case 176 -> { setHudHidden(mc, false); McClient.clearChat(mc); }
            case 196 -> shot(mc, "hume_field_jade");
            case 198 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos core = origin.offset(-1, 0, -4);
                if (player.level().getBlockEntity(core) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity be) {
                    ResourcefulSheep.LOGGER.warn("HUME {}: made {}, taken {} by {} blocks, average {}", be.status(), be.generated(), be.delivered(), be.receivers(), be.average());
                    player.openMenu(be, core);
                }
            });
            case 206 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 226 -> shot(mc, "hume_field_gui");
            case 228 -> {
                McClient.setScreen(mc, null);
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showHumeField();
            }
            case 236 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 246 -> shot(mc, "hume_field_jei");
            case 248 -> {
                McClient.setScreen(mc, null);
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showHumeFieldCrafting();
            }
            case 256 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 266 -> shot(mc, "hume_field_jei_recipe");
            // Its chapter in the Contraptions Manual, in the Power tab now.
            case 268 -> McClient.setScreen(mc, new GuideScreen());
            case 272 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Hume Field Generator"); }
            case 276 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 286 -> shot(mc, "hume_field_guide");
            // A pulse, from a few blocks back in survival: its burst of sparks, then the Nausea warping the view.
            case 288 -> {
                McClient.setScreen(mc, null);
                command(server, "gamemode survival @a");
                command(server, "effect clear @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 160 12", origin.getX() + 1.2, origin.getY() + 0.0, origin.getZ() + 1.5));
            }
            case 292 -> { setHudHidden(mc, true); McClient.clearChat(mc); }
            case 300 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos core = origin.offset(-1, 0, -4);
                if (player.level().getBlockEntity(core) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity be) {
                    int reached = be.pulse(player.level(), core);
                    ResourcefulSheep.LOGGER.warn("HUME pulse reached {} players; nausea {}", reached, player.getEffect(net.minecraft.world.effect.MobEffects.NAUSEA));
                }
            });
            case 304 -> shot(mc, "hume_field_pulse");
            case 390 -> shot(mc, "hume_field_nausea");
            case 396 -> mc.stop();
            default -> {}
        }
    }

    /**
     * The work-in-progress 3x3x3 Echo Chamber model on a sculk floor with a Warden sheep beside it, from the front, from
     * above at an angle, from the back and close up on the back, then running, listening to a note block, and
     * shrieking: {@code -PharnessScene=echo_chamber}.
     */
    private static void echoChamberTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-7, 0, -13), origin.offset(7, 7, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-7, -1, -13), origin.offset(7, -1, 4))) {
                    level.setBlock(p, (p.getX() + p.getZ()) % 3 == 0 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.SCULK.defaultBlockState(), 2);
                }
                // The 3x3x3 chamber faces the camera (south): its front middle block here, the rest behind and above.
                BlockPos at = origin.offset(0, 0, -4);
                com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.fill(level, at, ModBlocks.ECHO_CHAMBER.get().defaultBlockState()
                        .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.FACING, Direction.SOUTH));
                level.setBlock(at.offset(-3, 0, -1), Blocks.SCULK_SENSOR.defaultBlockState(), 3);
                level.setBlock(at.offset(3, 0, -2), Blocks.SCULK_SHRIEKER.defaultBlockState(), 3);
                level.setBlock(at.offset(-3, 0, 1), Blocks.SCULK_CATALYST.defaultBlockState(), 3);
                // The noise: a note block, struck by a redstone block beside it going on and off (see below).
                level.setBlock(at.offset(-3, 0, 2), Blocks.NOTE_BLOCK.defaultBlockState(), 3);
                // Two more Warden sheep, in the listening pen in front of the dish.
                for (double[] p : new double[][]{{-1.0, 2.5}, {0.6, 3.6}}) {
                    Sheep listener = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    listener.snapTo(at.getX() + 0.5 + p[0], at.getY(), at.getZ() + p[1], 180, 0);
                    listener.setNoAi(true);
                    SheepInfusionEvents.setInfusion(listener, new SheepInfusion(Optional.of(ResourcefulSheep.id("warden")), 9, 0));
                    level.addFreshEntity(listener);
                }
                Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                sheep.snapTo(at.getX() + 2.9, at.getY(), at.getZ() + 1.0, 150, 0);
                sheep.setYHeadRot(150);
                sheep.setYBodyRot(150);
                sheep.setNoAi(true);
                SheepInfusionEvents.setInfusion(sheep, new SheepInfusion(Optional.of(ResourcefulSheep.id("warden")), 9, 0));
                level.addFreshEntity(sheep);
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 8", origin.getX() + 0.5, origin.getY() + 0.8, origin.getZ() + 1.0));
            case 110 -> shot(mc, "wip_echo_chamber_front");
            case 112 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 147 29", origin.getX() + 5.0, origin.getY() + 4.5, origin.getZ() + 2.0));
            case 140 -> shot(mc, "wip_echo_chamber_above");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f -33 17", origin.getX() - 4.0, origin.getY() + 2.5, origin.getZ() - 12.0));
            case 170 -> shot(mc, "wip_echo_chamber_back");
            case 172 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 0 5", origin.getX() + 0.5, origin.getY() - 0.2, origin.getZ() - 9.0));
            case 200 -> shot(mc, "wip_echo_chamber_back_close");
            // Then it runs: a note block beside the pen, struck once a second by a redstone block going on and off.
            case 210 -> {
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 150 30", origin.getX() + 4.5, origin.getY() + 2.5, origin.getZ() + 3.5));
                server.execute(() -> {
                    var be = echoChamber(server);
                    if (be != null) be.setShowsArea(true);
                });
            }
            case 215 -> setHudHidden(mc, false);
            // Nothing takes its power here, so its buffer is long full: empty it to see it listening.
            case 460 -> server.execute(() -> {
                var be = echoChamber(server);
                if (be != null) be.emptyBuffer();
            });
            case 450 -> command(server, "gamemode creative @a"); // spectators can't open machine windows
            case 464 -> shot(mc, "wip_echo_chamber_listening");
            case 466 -> openEchoWindow(server);
            case 474 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); // park the cursor in a corner
            case 478 -> shot(mc, "wip_echo_chamber_gui");
            case 481 -> McClient.setScreen(mc, null);
            // Then far too loud for too long: it shrieks, and the player (in survival, so it counts) gets Darkness.
            case 484 -> command(server, "gamemode survival @a");
            case 488 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 155 12", origin.getX() + 3.5, origin.getY() + 0.0, origin.getZ() + 2.5));
            case 490 -> server.execute(() -> {
                var be = echoChamber(server);
                if (be != null) {
                    be.hearSteadily(7);
                    be.setMeters(0, com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(
                            com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.echoShriekTicks) - 2);
                }
            });
            case 530 -> McClient.clearChat(mc);
            case 546 -> shot(mc, "wip_echo_chamber_shriek");
            case 548 -> openEchoWindow(server);
            case 554 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 560 -> shot(mc, "wip_echo_chamber_gui_shriek");
            // Jade on its dish once the Darkness has worn off, then its JEI page and its recipe.
            case 562 -> McClient.setScreen(mc, null);
            case 570 -> command(server, "gamemode creative @a");
            case 575 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 -4", origin.getX() + 0.5, origin.getY() + 0.0, origin.getZ() - 1.0));
            case 720 -> shot(mc, "wip_echo_chamber_jade");
            case 722 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showEcho();
            }
            case 730 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 745 -> shot(mc, "wip_echo_chamber_jei");
            case 747 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.show(new ItemStack(ModItems.ECHO_CHAMBER.get()), true);
                }
            }
            case 755 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 770 -> shot(mc, "wip_echo_chamber_jei_recipe");
            // Its chapter in the Contraptions Manual (Power, Late Game), and the WIP tab without it.
            case 772 -> McClient.setScreen(mc, new GuideScreen());
            case 776 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Echo Chamber"); }
            case 780 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 790 -> shot(mc, "106_guide_echo_chamber");
            case 792 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Work in Progress"); }
            case 804 -> shot(mc, "106_guide_wip");
            case 810 -> mc.stop();
            default -> {}
        }
        if (tick >= 80 && tick < 480 && tick % 10 == 0) {
            server.execute(() -> {
                ServerLevel level = server.overworld();
                BlockPos switchAt = origin.offset(-4, 0, -2);
                boolean on = level.getBlockState(switchAt).is(Blocks.REDSTONE_BLOCK);
                level.setBlockAndUpdate(switchAt, on ? Blocks.AIR.defaultBlockState() : Blocks.REDSTONE_BLOCK.defaultBlockState());
            });
        }
    }

    /**
     * A Static Generator on grass with four Copper Combs round it (fresh, exposed, weathered and waxed, oxidised) and five
     * sheep at them, two of them Copper sheep: from the front at an angle, its dome close up, its window charging, and
     * then overcharged (its buffer full and its dome sparking): {@code -PharnessScene=static_generator}.
     */
    private static void staticGeneratorTick(Minecraft mc, MinecraftServer server) {
        BlockPos at = origin == null ? null : origin.offset(0, 0, -4);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos g = origin.offset(0, 0, -4);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, 0, -10), origin.offset(6, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, -1, -10), origin.offset(6, -1, 3))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                var gen = ModBlocks.STATIC_GENERATOR.get().defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock.FACING, Direction.SOUTH);
                level.setBlock(g, gen, 3);
                level.setBlock(g.above(), gen.setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
                var comb = ModBlocks.COPPER_COMB.get().defaultBlockState();
                var age = com.jax3.barelyfunctionalcontraptions.sheep.blocks.CopperCombBlock.AGE;
                level.setBlock(g.offset(-2, 0, 1), comb, 3);
                level.setBlock(g.offset(2, 0, 1), comb.setValue(age, 1), 3);
                level.setBlock(g.offset(-2, 0, -1), comb.setValue(age, 2).setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CopperCombBlock.WAXED, true), 3);
                level.setBlock(g.offset(2, 0, -1), comb.setValue(age, 3), 3);
                String[] kinds = {"copper", "iron", "copper", "iron", "redstone"};
                double[][] spots = {{-1.2, 1.9}, {1.2, 1.9}, {-1.3, -0.4}, {1.3, -0.4}, {-1.7, 2.4}};
                for (int i = 0; i < kinds.length; i++) {
                    Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    sheep.snapTo(g.getX() + 0.5 + spots[i][0], g.getY(), g.getZ() + 0.5 + spots[i][1], i % 2 == 0 ? 90 : 270, 0);
                    sheep.setNoAi(true);
                    SheepInfusionEvents.setInfusion(sheep, new SheepInfusion(Optional.of(ResourcefulSheep.id(kinds[i])), 9, 0));
                    level.addFreshEntity(sheep);
                }
                if (level.getBlockEntity(g) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity be) be.setShowsArea(true);
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 150 30", origin.getX() + 3.5, origin.getY() + 2.4, origin.getZ() + 0.5));
            case 140 -> shot(mc, "107_static_generator");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 10", origin.getX() + 0.5, origin.getY() + 0.6, origin.getZ() - 1.6));
            case 170 -> shot(mc, "107_static_generator_close");
            case 172 -> command(server, "gamemode creative @a"); // spectators can't open machine windows
            case 182 -> openStaticWindow(server, at);
            case 190 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 196 -> shot(mc, "107_static_generator_gui");
            case 198 -> McClient.setScreen(mc, null);
            // Overcharged: its buffer full and its dome brimming, so it sparks and would shock anyone stepping in.
            case 200 -> server.execute(() -> {
                if (server.overworld().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity be) {
                    be.fillBuffer();
                    be.setCharge(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity.domeCapacity());
                }
            });
            case 201 -> command(server, "gamemode spectator @a");
            case 202 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 150 30", origin.getX() + 3.5, origin.getY() + 2.4, origin.getZ() + 0.5));
            case 228 -> command(server, "gamemode creative @a");
            case 226 -> shot(mc, "107_static_generator_overcharged");
            case 232 -> openStaticWindow(server, at);
            case 240 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 246 -> shot(mc, "107_static_generator_gui_overcharged");
            // Its chapter in the Contraptions Manual (Power, Mid Game).
            case 248 -> McClient.setScreen(mc, null);
            case 250 -> McClient.setScreen(mc, new GuideScreen());
            case 254 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Static Generator"); }
            case 258 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 266 -> shot(mc, "107_guide_static_generator");
            // Jade on its dome from just outside the pen, its JEI page and recipe, then a shock for the advancement.
            case 268 -> { McClient.setScreen(mc, null); setHudHidden(mc, false); }
            case 270 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 8", origin.getX() + 0.5, origin.getY() + 0.0, origin.getZ() - 0.4));
            case 290 -> shot(mc, "107_static_generator_jade");
            case 292 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showStatic();
            }
            case 300 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 312 -> shot(mc, "107_static_generator_jei");
            case 314 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.show(new ItemStack(ModItems.STATIC_GENERATOR.get()), true);
                }
            }
            case 322 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 332 -> shot(mc, "107_static_generator_jei_recipe");
            case 334 -> McClient.setScreen(mc, null);
            case 336 -> command(server, "gamemode survival @a");
            // Into the overcharged pen: the next arc throws the player back out, which earns "Shocking!".
            case 340 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 0", origin.getX() + 0.5, origin.getY() + 0.0, origin.getZ() - 2.5));
            case 410 -> shot(mc, "107_static_generator_shock");
            case 412 -> server.execute(() -> ResourcefulSheep.LOGGER.warn("SHOCK player at {}, Shocking! {}", server.getPlayerList().getPlayers().getFirst().position(),
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(server.getPlayerList().getPlayers().getFirst(),
                            com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.STATIC_SHOCK)));
            case 418 -> mc.stop();
            default -> {}
        }
    }

    /**
     * A Grass Seeder in a fenced pen whose grass four sheared sheep have eaten into patches of dirt, loaded with Bone
     * Meal: the pen bare, then green again with short grass growing, a close-up, and its readout:
     * {@code -PharnessScene=grass_seeder}.
     */
    private static void grassSeederTick(Minecraft mc, MinecraftServer server) {
        BlockPos at = origin == null ? null : origin.offset(0, 0, -4);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos g = origin.offset(0, 0, -4);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, 0, -10), origin.offset(6, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                var rnd = new java.util.Random(4);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, -1, -10), origin.offset(6, -1, 3))) {
                    boolean inPen = Math.abs(p.getX() - g.getX()) <= 2 && Math.abs(p.getZ() - g.getZ()) <= 2;
                    level.setBlock(p, (inPen && rnd.nextInt(5) < 3 ? Blocks.DIRT : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
                }
                var fence = Blocks.OAK_FENCE.defaultBlockState();
                for (int d = -3; d <= 3; d++) {
                    level.setBlock(g.offset(-3, 0, d), fence, 3);
                    level.setBlock(g.offset(3, 0, d), fence, 3);
                    level.setBlock(g.offset(d, 0, -3), fence, 3);
                    level.setBlock(g.offset(d, 0, 3), fence, 3);
                }
                level.setBlock(g, ModBlocks.GRASS_SEEDER.get().defaultBlockState(), 3);
                for (double[] p : new double[][]{{-1.5, -1.2}, {1.6, -0.9}, {-1.2, 1.7}, {1.4, 1.5}}) {
                    Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    sheep.snapTo(g.getX() + 0.5 + p[0], g.getY(), g.getZ() + 0.5 + p[1], (float) (p[0] * 60), 0);
                    sheep.setNoAi(true);
                    sheep.setSheared(true);
                    level.addFreshEntity(sheep);
                }
                if (level.getBlockEntity(g) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity be) {
                    be.setShowsArea(true);
                    be.insertBoneMeal(new ItemStack(Items.BONE_MEAL, 32));
                }
            });
            case 62 -> setHudHidden(mc, true);
            case 64 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 160 40", origin.getX() + 2.5, origin.getY() + 3.5, origin.getZ() + 1.5));
            case 90 -> shot(mc, "108_grass_seeder_bare");
            // Its window on the bare pen, with some power and water in as well as its Bone Meal.
            case 92 -> {
                command(server, "gamemode creative @a");
                server.execute(() -> {
                    if (server.overworld().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity be) {
                        be.fillEnergy();
                        be.getTank().fill(2_500);
                        be.work(server.overworld(), at);
                    }
                });
            }
            case 98 -> openSeederWindow(server, at);
            case 104 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 110 -> shot(mc, "108_grass_seeder_gui_bare");
            case 112 -> McClient.setScreen(mc, null);
            // Hurry it along: tend a block every few ticks (the sheep graze as they would).
            case 114 -> {
                command(server, "gamemode spectator @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 160 40", origin.getX() + 2.5, origin.getY() + 3.5, origin.getZ() + 1.5));
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity be) {
                        for (int i = 0; i < 26; i++) be.work(level, at);
                    }
                });
            }
            case 140 -> shot(mc, "108_grass_seeder");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 160 30", origin.getX() + 1.0, origin.getY() + 0.9, origin.getZ() - 1.6));
            case 170 -> shot(mc, "108_grass_seeder_close");
            case 172 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity be) {
                    ResourcefulSheep.LOGGER.warn("SEEDER {} grass {} dirt {} short {} tended {} bone {}+{} grazing {} sheared sheep left {}", be.status(), be.grass(), be.dirt(),
                            be.shortGrass(), be.tended(), be.boneMeal().getCount(), be.boneCharges(), be.grazing(),
                            level.getEntitiesOfClass(Sheep.class, new net.minecraft.world.phys.AABB(at).inflate(4), Sheep::isSheared).size());
                }
            });
            // Its window again, green, then Jade on it, then its JEI page and recipe.
            case 174 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 35", origin.getX() + 0.5, origin.getY() + 0.0, origin.getZ() - 1.5));
            }
            case 180 -> openSeederWindow(server, at);
            case 186 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 192 -> shot(mc, "108_grass_seeder_gui");
            case 194 -> { McClient.setScreen(mc, null); setHudHidden(mc, false); McClient.clearChat(mc); }
            case 210 -> shot(mc, "108_grass_seeder_jade");
            case 212 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showGrassSeeder();
            }
            case 220 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 230 -> shot(mc, "108_grass_seeder_jei");
            case 232 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.show(new ItemStack(ModItems.GRASS_SEEDER.get()), true);
                }
            }
            case 240 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 250 -> shot(mc, "108_grass_seeder_jei_recipe");
            // Left with nothing to do and Trim off, it overgrows; past 100 blocks tended, the player nearby earns Keep Off the Grass.
            case 252 -> {
                McClient.setScreen(mc, null);
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity be) {
                        for (int i = 0; i < 160; i++) be.work(level, at);
                        ResourcefulSheep.LOGGER.warn("SEEDER overgrown: {} tall {} short {} tended {}", be.status(), be.tallGrass(), be.shortGrass(), be.tended());
                    }
                });
            }
            case 262 -> openSeederWindow(server, at);
            case 268 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 276 -> shot(mc, "108_grass_seeder_gui_overgrown");
            case 278 -> McClient.setScreen(mc, null);
            case 280 -> {
                setHudHidden(mc, true);
                command(server, "gamemode spectator @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 160 40", origin.getX() + 2.5, origin.getY() + 3.5, origin.getZ() + 1.5));
            }
            case 300 -> shot(mc, "108_grass_seeder_overgrown");
            // Trim on, and some ground eaten bare again: it cuts the tall grass back and regrasses, past 100 blocks.
            case 302 -> {
                command(server, "gamemode creative @a");
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity be) {
                        be.setTrim(true);
                        int k = 0;
                        for (BlockPos p : com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.ground(at)) {
                            if (k++ % 6 == 0) continue; // a few left overgrown, for Trim to cut back
                            level.setBlock(p.above(2), Blocks.AIR.defaultBlockState(), 2);
                            level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), 2);
                            level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                        }
                        for (int i = 0; i < 60; i++) be.work(level, at);
                        // And a second time the sheep eat it bare.
                        for (BlockPos p : com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.ground(at)) {
                            if (level.getBlockState(p.above()).is(Blocks.SHORT_GRASS)) level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), 2);
                            level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                        }
                        for (int i = 0; i < 40; i++) be.work(level, at);
                        ResourcefulSheep.LOGGER.warn("SEEDER trimmed: {} tall {} short {} tended {}", be.status(), be.tallGrass(), be.shortGrass(), be.tended());
                    }
                });
            }
            case 320 -> shot(mc, "108_grass_seeder_trimmed");
            case 322 -> server.execute(() -> ResourcefulSheep.LOGGER.warn("SEEDER Keep Off the Grass: {}",
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(server.getPlayerList().getPlayers().getFirst(),
                            com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.KEEP_OFF_THE_GRASS)));
            case 326 -> mc.stop();
            default -> {}
        }
    }

    /**
     * A Herd Counter on the west side of a fenced pen of mixed sheep, its screen facing them, wired through a comparator
     * to a lamp: the pen, its screen close up, then Ready and Full modes (the lamp lighting): {@code -PharnessScene=herd_counter}.
     */
    private static void herdCounterTick(Minecraft mc, MinecraftServer server) {
        BlockPos at = origin == null ? null : origin.offset(-4, 0, -5);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos c = origin.offset(-4, 0, -5);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-8, 0, -11), origin.offset(6, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-8, -1, -11), origin.offset(6, -1, 3))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                level.setBlock(c, ModBlocks.HERD_COUNTER.get().defaultBlockState()
                        .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.HerdCounterBlock.FACING, Direction.EAST), 3);
                level.setBlock(c.west(), Blocks.COMPARATOR.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST), 3);
                level.setBlock(c.west(2), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
                var fence = Blocks.OAK_FENCE.defaultBlockState();
                BlockPos pen = c.east(4);
                for (int d = -3; d <= 3; d++) {
                    level.setBlock(pen.offset(3, 0, d), fence, 3);
                    level.setBlock(pen.offset(d, 0, -3), fence, 3);
                    level.setBlock(pen.offset(d, 0, 3), fence, 3);
                    if (d != 0) level.setBlock(pen.offset(-3, 0, d), fence, 3);
                }
                String[] kinds = {"iron", "iron", "iron", "gold", "gold", "copper", "", ""};
                double[][] spots = {{-1.5, -1.5}, {0.5, -1.8}, {1.6, 0.2}, {-1.7, 0.6}, {-0.2, 1.6}, {1.5, 1.7}, {0.2, -0.1}, {-1.4, 2.2}};
                for (int i = 0; i < kinds.length; i++) {
                    Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    sheep.snapTo(pen.getX() + 0.5 + spots[i][0], pen.getY(), pen.getZ() + 0.5 + spots[i][1], i * 45, 0);
                    sheep.setNoAi(true);
                    if (!kinds[i].isEmpty()) {
                        var resource = com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.server().byId(ResourcefulSheep.id(kinds[i])).orElseThrow();
                        SheepInfusionEvents.setInfusion(sheep, SheepInfusion.NONE.withResource(resource.id(), resource.effectiveInfusionUses()));
                    }
                    if (i == 4) sheep.setSheared(true);
                    if (i == 7) sheep.setAge(-24000);
                    level.addFreshEntity(sheep);
                }
                if (level.getBlockEntity(c) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity be) be.setShowsArea(true);
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 135 32", origin.getX() + 4.5, origin.getY() + 4.0, origin.getZ() + 2.5));
            case 140 -> shot(mc, "109_herd_counter");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 90 12", at.getX() + 2.1, at.getY() - 0.9, at.getZ() + 0.5));
            case 170 -> shot(mc, "109_herd_counter_close");
            case 172 -> server.execute(() -> {
                if (server.overworld().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity be) {
                    be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.READY);
                    be.setFilter(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("iron"), 1));
                }
            });
            case 190 -> shot(mc, "109_herd_counter_ready");
            // Full at 7: the lamp behind it lights.
            case 192 -> {
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 150 22", at.getX() + 2.0, at.getY() + 0.6, at.getZ() + 3.0));
                server.execute(() -> {
                    if (server.overworld().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity be) {
                        be.setFilter(ItemStack.EMPTY);
                        be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.FULL);
                        be.setTarget(7);
                    }
                });
            }
            case 220 -> shot(mc, "109_herd_counter_full");
            case 222 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity be) {
                    ResourcefulSheep.LOGGER.warn("HERD total {} lambs {} ready {} sheared {} low {} by {} value {} signal {} lamp {}", be.total(), be.lambs(), be.ready(),
                            be.sheared(), be.lowInfusion(), be.byResource(), be.value(), be.comparatorSignal(),
                            level.getBlockState(at.west(2)).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT));
                }
            });
            // Its window (Ready, filtered to Iron), then Jade on it, then its JEI page.
            case 224 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 90 28", at.getX() + 2.5, at.getY() + 0.0, at.getZ() + 0.5));
                server.execute(() -> {
                    if (server.overworld().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity be) {
                        be.setMode(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode.READY);
                        be.setFilter(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("iron"), 1));
                    }
                });
            }
            case 232 -> server.execute(() -> {
                if (server.overworld().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity be) {
                    server.getPlayerList().getPlayers().getFirst().openMenu(be, at);
                }
            });
            case 238 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 246 -> shot(mc, "109_herd_counter_gui");
            case 248 -> { McClient.setScreen(mc, null); setHudHidden(mc, false); McClient.clearChat(mc); }
            case 264 -> shot(mc, "109_herd_counter_jade");
            case 266 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showHerdCounter();
            }
            case 274 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 284 -> shot(mc, "109_herd_counter_jei");
            case 290 -> mc.stop();
            default -> {}
        }
    }

    /**
     * A Machine Status Panel on a stone wall, linked to a running Powered Furnace, a Grass Seeder out of fuel, a half-full
     * Hume Energy Cube and a Herd Counter (that one through the real wrench clicks), with a comparator lighting a lamp
     * while anything has a problem: {@code -PharnessScene=status_panel}.
     */
    /** A crate with {@code contents}, front to the south: dyed {@code lining}, or (null) lined to match its item. */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity placeCrate(ServerLevel level, BlockPos at,
            net.minecraft.world.item.@org.jspecify.annotations.Nullable DyeColor lining, boolean strapped, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents contents) {
        level.setBlock(at, ModBlocks.CRATE.get().defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.FACING, Direction.SOUTH)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.LINING, lining == null ? net.minecraft.world.item.DyeColor.WHITE : lining)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.DYED, lining != null)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock.STRAPPED, strapped), 3);
        var be = (com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity) level.getBlockEntity(at);
        be.setContents(contents);
        return be;
    }

    private static String crateLine(ServerLevel level, BlockPos at) {
        return level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity be
                ? be.contents() + " " + level.getBlockState(at) : "no crate";
    }

    /**
     * A Crate Controller in a wall of five crates, and two crates across the room linked with real wrench clicks
     * (controller, crate, controller); then 200 wheat put in through it, which goes to the far empty crate:
     * {@code -PharnessScene=crate_controller}.
     */
    private static void crateControllerTick(Minecraft mc, MinecraftServer server) {
        BlockPos wall = origin == null ? null : origin.offset(0, 0, -4);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode survival @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos o = origin, w = o.offset(0, 0, -4);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, 0, -8), o.offset(7, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, -1, -8), o.offset(7, -1, 3))) level.setBlock(p, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                net.minecraft.world.item.Item[] items = {Items.COBBLESTONE, Items.REDSTONE, Items.IRON_INGOT, Items.COAL, Items.LAPIS_LAZULI};
                int[] counts = {1287, 2048, 412, 900, 64};
                int k = 0;
                for (int y = 0; y <= 1; y++) for (int x = -1; x <= 1; x++) {
                    BlockPos p = w.offset(x, y, 0);
                    if (x == 0 && y == 0) continue; // the controller
                    placeCrate(level, p, null, false, new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(items[k]), counts[k], false, 0));
                    k++;
                }
                level.setBlock(w, ModBlocks.CRATE_CONTROLLER.get().defaultBlockState()
                        .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateControllerBlock.FACING, Direction.SOUTH), 3);
                // Across the room: two crates touching each other, not touching the wall.
                placeCrate(level, w.offset(-5, 0, 2), null, false, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents.EMPTY);
                placeCrate(level, w.offset(-5, 1, 2), null, false,
                        new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.EMERALD), 37, true, 0));
                player.getInventory().setItem(0, new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get()));
                player.getInventory().setSelectedSlot(0);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 200 15", o.getX() + 0.5, o.getY(), o.getZ() + 0.5));
            });
            case 80 -> setHudHidden(mc, true);
            case 90, 100, 110 -> {
                // The wrench: the controller, the crate across the room, the controller again.
                BlockPos target = tick == 100 ? wall.offset(-5, 0, 2) : wall;
                Direction face = tick == 100 ? Direction.EAST : Direction.SOUTH;
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(target).add(face.getStepX() * 0.5, 0, face.getStepZ() * 0.5), face, target, false);
                ResourcefulSheep.LOGGER.warn("CONTROLLER wrench click {} on {}", mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit), target);
            }
            case 140 -> server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                if (level.getBlockEntity(wall) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity c) {
                    c.refresh(level);
                    ResourcefulSheep.LOGGER.warn("CONTROLLER {} crates, {} / {} ({}%), {} empty, {} could burst, links {}, report {}", c.crateCount(), c.stored(), c.capacity(),
                            c.percent(), c.empties(), c.risky(), c.links(), com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.report(c));
                    var handler = c.getItemHandler();
                    try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                        int in = handler.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.WHEAT), 200, tx);
                        tx.commit();
                        ResourcefulSheep.LOGGER.warn("CONTROLLER took {} wheat; the far empty crate now {}", in,
                                level.getBlockEntity(wall.offset(-5, 0, 2)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity e ? e.contents() : null);
                    }
                }
            });
            case 150 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 12", wall.getX() + 0.5, wall.getY(), wall.getZ() + 3.6));
            case 190 -> shot(mc, "112_crate_controller");
            case 192 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.1f %.2f 180 6", wall.getX() + 0.5, wall.getY() - 0.4, wall.getZ() + 2.4));
            case 230 -> shot(mc, "112_crate_controller_close");
            case 232 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.1f %.2f 153 18", wall.getX() + 1.5, wall.getY() + 1.0, wall.getZ() + 8.5));
            case 270 -> shot(mc, "112_crate_controller_linked");
            case 272 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 38", wall.getX() + 0.5, wall.getY(), wall.getZ() + 2.4));
            }
            case 276 -> { setHudHidden(mc, false); McClient.clearChat(mc); }
            case 296 -> shot(mc, "112_crate_controller_jade");
            case 298 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showCrateController();
            }
            case 306 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 316 -> shot(mc, "112_crate_controller_jei");
            case 320 -> mc.stop();
            default -> {}
        }
    }

    /**
     * Five Wool-Lined Crates in a row, each lined to match its item: cobblestone, redstone full to bursting (its lid bulging),
     * lime wool locked, a few swords, and an empty strapped one. Then real clicks: cobblestone in, a stack out, a
     * sneak-click lining one with blue wool; and the full one bursts: {@code -PharnessScene=crate}.
     */
    private static void crateTick(Minecraft mc, MinecraftServer server) {
        BlockPos row = origin == null ? null : origin.offset(0, 0, -4);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode survival @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos o = origin, r = o.offset(0, 0, -4);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, 0, -8), o.offset(7, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, -1, -8), o.offset(7, -1, 3))) level.setBlock(p, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                placeCrate(level, r.offset(-4, 0, 0), null, false,
                        new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.COBBLESTONE), 1287, false, 0));
                placeCrate(level, r.offset(-2, 0, 0), null, false,
                        new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.REDSTONE), 2048, false, 0));
                placeCrate(level, r, null, false,
                        new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(com.jax3.barelyfunctionalcontraptions.compat.Vanilla.wool(net.minecraft.world.item.DyeColor.LIME)), 900, true, 2));
                placeCrate(level, r.offset(2, 0, 0), null, false,
                        new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.DIAMOND_SWORD), 12, false, 0));
                placeCrate(level, r.offset(4, 0, 0), net.minecraft.world.item.DyeColor.ORANGE, true,
                        com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents.EMPTY);
                // A second row stacked on top, so a wall of them shows.
                placeCrate(level, r.offset(-3, 1, 0), null, false,
                        new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.WHEAT), 340, false, 0));
                placeCrate(level, r.offset(3, 1, 0), null, false,
                        new com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents(new ItemStack(Items.OAK_LOG), 12_480, false, 4));
                player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
                player.getInventory().setItem(1, new ItemStack(com.jax3.barelyfunctionalcontraptions.compat.Vanilla.wool(net.minecraft.world.item.DyeColor.BLUE), 3));
                player.getInventory().setItem(2, new ItemStack(ModItems.CRATE.get()));
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 18", o.getX() + 0.5, o.getY(), o.getZ() + 0.5));
            });
            case 70 -> setHudHidden(mc, true);
            case 120 -> shot(mc, "111_crate");
            case 122 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 22", row.getX() - 3.5, row.getY(), row.getZ() + 2.3));
            case 150 -> shot(mc, "111_crate_close");
            case 155 -> {
                // Right-click the cobblestone crate's front with a stack of cobblestone.
                mc.player.getInventory().setSelectedSlot(0);
                BlockPos crate = row.offset(-4, 0, 0);
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(crate).add(0, 0, 0.5), Direction.SOUTH, crate, false);
                ResourcefulSheep.LOGGER.warn("CRATE store click {}", mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            }
            case 165 -> {
                // Left-click it: a stack back.
                BlockPos crate = row.offset(-4, 0, 0);
                mc.gameMode.startDestroyBlock(crate, Direction.SOUTH);
            }
            case 170 -> mc.gameMode.stopDestroyBlock();
            case 175 -> {
                // Sneak-right-click the lime one with blue wool: relined.
                mc.player.getInventory().setSelectedSlot(1);
                mc.options.keyShift.setDown(true);
            }
            case 180 -> {
                BlockPos crate = row;
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(crate).add(0.5, 0, 0), Direction.EAST, crate, false);
                ResourcefulSheep.LOGGER.warn("CRATE sneak lining click {}", mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            }
            case 185 -> mc.options.keyShift.setDown(false);
            case 195 -> server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                ResourcefulSheep.LOGGER.warn("CRATE cobblestone {} ; lime {} ; player slot0 {} slot1 {} slot3 {}", crateLine(level, row.offset(-4, 0, 0)),
                        crateLine(level, row), server.getPlayerList().getPlayers().getFirst().getInventory().getItem(0),
                        server.getPlayerList().getPlayers().getFirst().getInventory().getItem(1),
                        server.getPlayerList().getPlayers().getFirst().getInventory().getItem(3));
            });
            case 200 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 30", row.getX() - 1.5, row.getY() + 1, row.getZ() + 3.2));
            case 230 -> server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                if (level.getBlockEntity(row.offset(-2, 0, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity be) be.burst(level);
            });
            case 233 -> shot(mc, "111_crate_burst");
            case 240 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ResourcefulSheep.LOGGER.warn("CRATE burst: redstone {} ; Bursting at the Seams {}", crateLine(player.level(), row.offset(-2, 0, 0)),
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player,
                                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.BURSTING_AT_THE_SEAMS));
            });
            case 245 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 22", row.getX() + 0.5, row.getY(), row.getZ() + 2.6));
            case 270 -> shot(mc, "111_crate_relined");
            case 272 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 25", row.getX() + 0.5, row.getY(), row.getZ() + 2.6));
            }
            case 276 -> { setHudHidden(mc, false); McClient.clearChat(mc); }
            case 296 -> shot(mc, "111_crate_jade");
            case 298 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showCrate();
            }
            case 306 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 316 -> shot(mc, "111_crate_jei");
            case 320 -> mc.stop();
            default -> {}
        }
    }

    private static void statusPanelTick(Minecraft mc, MinecraftServer server) {
        BlockPos at = origin == null ? null : origin.offset(0, 0, -5);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos o = origin;
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, 0, -9), o.offset(7, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, -1, -9), o.offset(7, -1, 3))) level.setBlock(p, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, 0, -6), o.offset(2, 3, -6))) level.setBlock(p, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                // A 3x3 screen of nine panels; the top-left one (as you look at it: west, top) holds the links.
                for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) {
                    level.setBlock(o.offset(x, y, -5), ModBlocks.STATUS_PANEL.get().defaultBlockState()
                            .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.StatusPanelBlock.FACING, Direction.SOUTH), 3);
                }
                BlockPos panel = o.offset(0, 0, -5), topLeft = o.offset(-1, 2, -5);
                // Its alarm: a comparator reading it, to a lamp that's lit while anything has a problem.
                level.setBlock(panel.south(), Blocks.COMPARATOR.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
                level.setBlock(panel.south(2), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
                // The machines.
                BlockPos furnace = o.offset(-4, 0, -3), seeder = o.offset(4, 0, -3), cube = o.offset(-4, 0, 0), counter = o.offset(4, 0, 0);
                level.setBlock(furnace, ModBlocks.POWERED_FURNACE.get().defaultBlockState(), 3);
                level.setBlock(furnace.north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
                if (level.getBlockEntity(furnace) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity f) f.setItem(0, new ItemStack(Items.RAW_IRON, 64));
                for (BlockPos p : BlockPos.betweenClosed(seeder.offset(-1, -1, -1), seeder.offset(1, -1, 1))) level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                level.setBlock(seeder, ModBlocks.GRASS_SEEDER.get().defaultBlockState(), 3);
                level.setBlock(cube, ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
                if (level.getBlockEntity(cube) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity c) {
                    var handler = c.getEnergyHandler();
                    for (int k = 0; k < 400 && handler.getAmountAsLong() < handler.getCapacityAsLong() / 2; k++) {
                        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                            handler.insert((int) Math.min(Integer.MAX_VALUE, handler.getCapacityAsLong() / 2 - handler.getAmountAsLong()), tx);
                            tx.commit();
                        }
                    }
                }
                level.setBlock(counter, ModBlocks.HERD_COUNTER.get().defaultBlockState()
                        .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.HerdCounterBlock.FACING, Direction.EAST), 3);
                for (int i = 0; i < 3; i++) {
                    Sheep sheep = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    sheep.snapTo(counter.getX() + 2.5 + i, counter.getY(), counter.getZ() + 0.5, 90, 0);
                    sheep.setNoAi(true);
                    level.addFreshEntity(sheep);
                }
                // More machines: a second furnace and Automatic Shears with no power, a Mechanical Separator, a Wool Burner,
                // more cubes and an Energy Meter.
                BlockPos[] more = {o.offset(-6, 0, -1), o.offset(-6, 0, 1), o.offset(6, 0, -1), o.offset(6, 0, 1), o.offset(-3, 0, 2), o.offset(3, 0, 2)};
                level.setBlock(more[0], ModBlocks.POWERED_FURNACE.get().defaultBlockState(), 3);
                level.setBlock(more[1], ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState(), 3);
                level.setBlock(more[2], ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState(), 3);
                level.setBlock(more[3], ModBlocks.WOOL_BURNER.get().defaultBlockState(), 3);
                level.setBlock(more[4], ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
                level.setBlock(more[5], ModBlocks.ENERGY_METER.get().defaultBlockState(), 3);
                if (level.getBlockEntity(topLeft) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity be) {
                    be.toggleLink(level, furnace);
                    be.toggleLink(level, seeder);
                    be.toggleLink(level, cube);
                    for (BlockPos m : more) be.toggleLink(level, m);
                }
                // The last one through the wrench, as a player does it: the panel, the machine, the panel.
                ItemStack wrench = new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wrench);
                for (BlockPos click : new BlockPos[]{panel, counter, panel}) {
                    var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(click), Direction.UP, click, false);
                    var result = wrench.getItem().onItemUseFirst(wrench, new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
                    ResourcefulSheep.LOGGER.warn("PANEL wrench click on {}: {}", level.getBlockState(click).getBlock(), result);
                }
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 15", origin.getX() + 0.5, origin.getY() + 2.0, origin.getZ() + 3.5));
            case 140 -> shot(mc, "110_status_panel");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 0", at.getX() + 0.5, at.getY() - 0.1, at.getZ() + 3.2));
            case 170 -> shot(mc, "110_status_panel_close");
            case 172 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                if (level.getBlockEntity(at.offset(-1, 2, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity be) {
                    for (var row : be.rows()) ResourcefulSheep.LOGGER.warn("PANEL row {} {} {} {}", row.name(), row.report().status(), row.report().level(), row.report().number());
                    ResourcefulSheep.LOGGER.warn("PANEL signal {} lamp {}", be.comparatorSignal(), level.getBlockState(at.south(2)));
                    ResourcefulSheep.LOGGER.warn("PANEL Mission Control: {}", com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(
                            server.getPlayerList().getPlayers().getFirst(), com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.MISSION_CONTROL));
                }
            });
            // Its window, with a machine named and the green theme; then the screen in green, Jade, and its JEI page.
            case 174 -> {
                command(server, "gamemode creative @a");
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    if (level.getBlockEntity(at.offset(-1, 2, 0)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity be) {
                        be.rename(level, 0, "Iron line furnace");
                        be.rename(level, 1, "Pen seeder");
                        be.cycleTheme(level);
                        server.getPlayerList().getPlayers().getFirst().openMenu(be, at.offset(-1, 2, 0));
                    }
                });
            }
            case 182 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 192 -> shot(mc, "110_status_panel_gui");
            case 194 -> McClient.setScreen(mc, null);
            case 196 -> command(server, "gamemode spectator @a");
            case 198 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 0", at.getX() + 0.5, at.getY() - 0.1, at.getZ() + 3.2));
            case 214 -> shot(mc, "110_status_panel_green");
            case 216 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 180 8", at.getX() + 0.5, at.getY() + 0.0, at.getZ() + 2.5));
            }
            case 220 -> { setHudHidden(mc, false); McClient.clearChat(mc); }
            case 236 -> shot(mc, "110_status_panel_jade");
            case 238 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showStatusPanel();
            }
            case 246 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 256 -> shot(mc, "110_status_panel_jei");
            case 262 -> mc.stop();
            default -> {}
        }
    }

    private static void openSeederWindow(MinecraftServer server, BlockPos at) {
        server.execute(() -> {
            if (server.overworld().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity be) {
                server.getPlayerList().getPlayers().getFirst().openMenu(be, be.getBlockPos());
            }
        });
    }

    private static void openStaticWindow(MinecraftServer server, BlockPos at) {
        server.execute(() -> {
            if (server.overworld().getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity be) {
                server.getPlayerList().getPlayers().getFirst().openMenu(be, be.getBlockPos());
            }
        });
    }

    /** Opens the scene's Echo Chamber's window for the player, as a right-click does. */
    private static void openEchoWindow(MinecraftServer server) {
        server.execute(() -> {
            var be = echoChamber(server);
            if (be != null) server.getPlayerList().getPlayers().getFirst().openMenu(be, be.getBlockPos());
        });
    }

    /** The Echo Chamber the scene built: its middle block is one behind the front, which faces south. */
    private static com.jax3.barelyfunctionalcontraptions.sheep.blockentity.@org.jspecify.annotations.Nullable EchoChamberBlockEntity echoChamber(MinecraftServer server) {
        return server.overworld().getBlockEntity(origin.offset(0, 0, -5)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity be ? be : null;
    }

    /**
     * Runs {@code /bfc list} as the player, the way it's typed in chat, and photographs the
     * chat: {@code -PharnessScene=list_command} (with {@code -PharnessWorld="BFC Test World"} to check that world).
     */
    private static void listCommandTick(Minecraft mc) {
        switch (tick) {
            case 60 -> { if (mc.player != null) mc.player.connection.sendCommand(ResourcefulSheep.MOD_ID + " list"); }
            case 70 -> McClient.setScreen(mc, new net.minecraft.client.gui.screens.ChatScreen("", false));
            case 90 -> shot(mc, "list_command");
            case 96 -> mc.stop();
            default -> {}
        }
    }

    /** Photographs each scene {@code extend_world} added (open a copy of the world: it moves the player). */
    private static void worldTourTick(Minecraft mc, MinecraftServer server) {
        // The whole square platform from above its south edge and from straight overhead, then the latest scene.
        String[] tps = {"tp @a 73.5 95 218.5 180 40", "tp @a 73.5 185 108 180 90", "tp @a 129.5 38 162.5 180 12", "tp @a 45.5 40 184.5 180 32",
                "tp @a 101.5 39 186.5 180 22"};
        if (tick == 30) {
            setHudHidden(mc, true);
            command(server, "gamemode spectator @a");
        }
        for (int i = 0; i < tps.length; i++) {
            if (tick == 40 + i * 60) command(server, tps[i]);
            if (tick == 90 + i * 60) shot(mc, "tour_" + i);
        }
        // After the shots, out of spectator, so the scene's advancement (players nearby, not spectating) can be checked.
        if (tick == 40 + tps.length * 60 + 5) command(server, "gamemode creative @a");
        // Let the latest scene run a while (it stays loaded round the player), then report how it is doing.
        int report = 40 + tps.length * 60 + 600;
        if (tick == report) {
            server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                // The upgrade cards' separators: their output chests in the floor.
                for (int dx : new int[]{-4, 0, 4}) {
                    BlockPos sepPos = new BlockPos(73 + dx, 35, 177);
                    if (level.getBlockEntity(sepPos) instanceof MechanicalSeparatorBlockEntity sep
                            && level.getBlockEntity(sepPos.below()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity out) {
                        int iron = 0;
                        for (int i = 0; i < out.getContainerSize(); i++) if (out.getItem(i).is(Items.RAW_IRON)) iron += out.getItem(i).getCount();
                        ResourcefulSheep.LOGGER.warn("TOUR upgrades separator at {} with {}: {} Raw Iron out, status {}", sepPos, sep.upgrades().cards(), iron, sep.getStatus());
                    }
                }
                // The Hume Field Generators in cell (3, 6): delivering, wasting and alone.
                for (int dx : HUME_WORLD_X) {
                    BlockPos core = new BlockPos(101 + dx, 35, 176);
                    if (level.getBlockEntity(core) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity hume) {
                        ResourcefulSheep.LOGGER.warn("TOUR hume field at {}: {}, made {} taken {} by {} blocks, average {}, {} delivered in all, {} pulses",
                                core, hume.status(), hume.generated(), hume.delivered(), hume.receivers(), hume.average(), hume.totalDelivered(), hume.pulses());
                    }
                }
                // The generator cabled to a separator and shears, once wireless.
                if (level.getBlockEntity(new BlockPos(91, 35, 38)) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity gen
                        && level.getBlockEntity(new BlockPos(95, 35, 38)) instanceof MechanicalSeparatorBlockEntity sep
                        && level.getBlockEntity(new BlockPos(99, 35, 38)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity shears) {
                    ResourcefulSheep.LOGGER.warn("TOUR cabled generator {} {} FE; separator {} FE, {} {}; shears {} FE, {}", gen.getState(), gen.createStatus().energy(),
                            sep.getEnergy().getEnergy(), sep.getInventory().getStack(SeparatorInventory.INPUT), sep.getInventory().getStack(SeparatorInventory.PRIMARY),
                            shears.getEnergy().getEnergy(), shears.getStatus());
                }
                if (level.getBlockEntity(new BlockPos(45, 35, 177)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity t) {
                    StringBuilder chest = new StringBuilder();
                    if (level.getBlockEntity(new BlockPos(45, 34, 178)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c) {
                        for (int s = 0; s < c.getContainerSize(); s++) if (!c.getItem(s).isEmpty()) chest.append(' ').append(c.getItem(s));
                    }
                    ResourcefulSheep.LOGGER.warn("TOUR trawler {}: pool {}, sheep {}, {}%, hauls {}, lure {}; chest{}", t.status(), t.pool(), t.sheepCount(),
                            t.progressPercent(), t.hauls(), t.getItem(0), chest);
                }
                if (level.getBlockEntity(new BlockPos(128, 37, 152)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity panel) {
                    if (level.getBlockEntity(new BlockPos(17, 37, 173)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity cc) {
                        ResourcefulSheep.LOGGER.warn("TOUR crate controller: {} crates, {} / {} ({}%), {} empty, report {}", cc.crateCount(), cc.stored(), cc.capacity(),
                                cc.percent(), cc.empties(), com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.report(cc));
                    }
                    StringBuilder wallReport = new StringBuilder();
                    for (int y = 35; y <= 36; y++) for (int x = 14; x <= 20; x++) {
                        if (level.getBlockEntity(new BlockPos(x, y, 173)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity w) {
                            wallReport.append(' ').append(w.item().getItem()).append('=').append(w.count());
                        }
                    }
                    ResourcefulSheep.LOGGER.warn("TOUR crate wall:{}", wallReport);
                    if (level.getBlockEntity(new BlockPos(18, 37, 173)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity bread) {
                        ResourcefulSheep.LOGGER.warn("TOUR the controller's empty crate now {}", bread.contents());
                    }
                    for (BlockPos p : new BlockPos[]{new BlockPos(13, 35, 178), new BlockPos(21, 35, 178), new BlockPos(16, 35, 173)}) {
                        if (level.getBlockEntity(p) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity crate) {
                            ResourcefulSheep.LOGGER.warn("TOUR crate at {}: {} {}", p, crate.contents(), level.getBlockState(p));
                        }
                    }
                    ResourcefulSheep.LOGGER.warn("TOUR status panel: {} machines, signal {}, lamp {}", panel.rows().size(), panel.comparatorSignal(),
                            level.getBlockState(new BlockPos(129, 35, 154)));
                    for (var row : panel.rows()) ResourcefulSheep.LOGGER.warn("TOUR   {} {} {} at {}", row.name().replace("block.barelyfunctionalcontraptions.", ""),
                            row.report().status(), row.report().number(), row.pos());
                }
                for (BlockPos p : new BlockPos[]{new BlockPos(97, 35, 154), new BlockPos(101, 35, 150)}) {
                    if (level.getBlockEntity(p) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity hc) {
                        ResourcefulSheep.LOGGER.warn("TOUR herd counter at {}: {} {} (target {}), total {}, signal {}, steady {}, lamp {}", p, hc.mode(), hc.value(), hc.target(),
                                hc.total(), hc.comparatorSignal(), hc.steady(), level.getBlockState(p.relative(level.getBlockState(p)
                                        .getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.HerdCounterBlock.FACING).getOpposite(), 2)));
                    }
                }
                if (level.getBlockEntity(new BlockPos(73, 35, 154)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity seeder) {
                    ResourcefulSheep.LOGGER.warn("TOUR seeder {} grass {}/{} short {} tended {} grazing {} bone {}+{}", seeder.status(), seeder.grass(),
                            seeder.grass() + seeder.dirt(), seeder.shortGrass(), seeder.tended(), seeder.grazing(), seeder.boneMeal().getCount(), seeder.boneCharges());
                }
                if (level.getBlockEntity(new BlockPos(73, 35, 151)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity shears) {
                    int wool = 0;
                    for (int i = 0; i < shears.getContainerSize(); i++) wool += shears.getItem(i).getCount();
                    ResourcefulSheep.LOGGER.warn("TOUR shears hold {} wool", wool);
                }
                if (level.getBlockEntity(new BlockPos(17, 35, 154)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity be) {
                    ResourcefulSheep.LOGGER.warn("TOUR static {} {} FE/t, {} sheep, {} combs, comb wear {}", be.state(), be.generating(), be.herd(),
                            be.combs().size(), be.combs().isEmpty() ? 0 : be.wear(be.combs().getFirst()));
                }
                if (level.getBlockEntity(new BlockPos(129, 35, 131)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity echo) {
                    ResourcefulSheep.LOGGER.warn("TOUR echo {} {} FE/t, {} sheep, noise {}", echo.state(), echo.generating(), echo.herd(), echo.noise());
                }
                for (BlockPos p : new BlockPos[]{new BlockPos(17, 35, 153), new BlockPos(17, 35, 152)}) {
                    ResourcefulSheep.LOGGER.warn("TOUR at {}: {} / {}", p, level.getBlockState(p), level.getBlockEntity(p) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyMeterBlockEntity m
                            ? "meter rate " + m.rate() : String.valueOf(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, p, Direction.NORTH) instanceof net.neoforged.neoforge.transfer.energy.EnergyHandler h ? h.getAmountAsLong() : -1));
                }
            });
        }
        if (tick == report + 10) shot(mc, "tour_last");
        // The Machine Status Panel's window: every machine it shows, the Hume Field Generators at the end.
        if (tick == report + 11) command(server, "tp @a 128.5 36 155.5 180 10"); // within reach of the panel, or its window closes
        if (tick == report + 14) {
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos master = new BlockPos(128, 37, 152);
                if (player.level().getBlockEntity(master) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity panel) {
                    player.openMenu(panel, master);
                }
            });
        }
        if (tick == report + 18) { setHudHidden(mc, false); McClient.clearChat(mc); McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); }
        if (tick == report + 30) shot(mc, "tour_panel");
        if (tick == report + 32 && McClient.screen(mc) != null) {
            for (int i = 0; i < 60; i++) McClient.screen(mc).mouseScrolled(mc.getWindow().getGuiScaledWidth() / 2.0, mc.getWindow().getGuiScaledHeight() / 2.0, 0, -1);
        }
        if (tick == report + 34) McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
        if (tick == report + 40) shot(mc, "tour_panel_end");
        if (tick == report + 46) mc.stop();
    }

    /**
     * The test world's scenes, as they stood in a row of separate platforms: name, the blocks' bounds (x0, x1, z0, z1,
     * y0, y1), how far up to lift them (so every scene's floor ends up at y = 34), and the grid cell (column, row; the
     * Themed sheep take two cells). {@link #squareWorldTick} moves them all onto one square platform.
     */
    private static final Object[][] SQUARE_SCENES = {
            {"Infused sheep, Mechanical Separators and Automatic Shears", new int[]{-14, -4, -13, -4, 4, 4}, 31, 0, 0},
            {"Hume Energy Cubes", new int[]{-25, -10, -10, 3, 33, 38}, 1, 1, 0},
            {"Fluid Reservoir", new int[]{-12, -7, -12, -8, 24, 26}, 9, 2, 0},
            {"Wireless power and Creative Generator", new int[]{1, 22, -10, -8, 33, 36}, 1, 3, 0},
            {"Zero Point Energy Modules", new int[]{35, 41, -11, -7, 33, 41}, 1, 4, 0},
            {"Energy Cables", new int[]{52, 61, -12, -5, 33, 37}, 1, 0, 1},
            {"Fluid Pipes", new int[]{67, 77, -12, -4, 32, 36}, 1, 1, 1},
            {"Item Pipes", new int[]{83, 93, -12, -4, 33, 35}, 1, 2, 1},
            {"Zero Point modules on walls", new int[]{100, 110, -13, -5, 33, 38}, 1, 3, 1},
            {"Bred lambs", new int[]{116, 126, -12, -6, 33, 33}, 1, 4, 1},
            {"Fluid Tanks", new int[]{131, 144, -13, -5, 33, 36}, 1, 0, 2},
            {"New blocks", new int[]{154, 178, -19, -3, 34, 36}, 0, 1, 2},
            {"Themed sheep", new int[]{186, 218, -19, -2, 34, 35}, 0, 2, 2},
            {"Blaze Sheep Furnace", new int[]{226, 244, -19, -1, 34, 35}, 0, 4, 2},
            {"Wool Burners", new int[]{254, 270, -19, -3, 34, 37}, 0, 0, 3},
            {"Wool Packers", new int[]{274, 290, -19, -3, 34, 37}, 0, 1, 3},
            {"Infusion Altar", new int[]{294, 312, -19, -3, 34, 35}, 0, 2, 3},
            {"Snow Sheep Chiller", new int[]{316, 328, -19, -7, 34, 35}, 0, 3, 3},
            {"Powered Furnaces", new int[]{334, 350, -19, -5, 34, 37}, 0, 4, 3},
            {"Woollen Armour", new int[]{354, 370, -19, -5, 34, 34}, 0, 0, 4},
            {"Sheep Treadmills", new int[]{376, 388, -17, -6, 34, 35}, 0, 1, 4},
            {"Ore processing line", new int[]{398, 412, -15, -5, 33, 36}, 0, 2, 4},
            {"Thermal Reaction Chamber", new int[]{415, 429, -21, -6, 33, 36}, 0, 3, 4},
            {"Echo Chamber", new int[]{435, 449, -21, -4, 34, 37}, 0, 4, 4},
            {"Static Generator", new int[]{455, 469, -21, -4, 34, 36}, 0, 0, 5},
            {"Mycelial Generators, Mechanical Separators and Automatic Shears", new int[]{-15, -6, -11, -10, 16, 16}, 19, 1, 5},
    };
    /** The square platform: its north-west corner, its side, and the grid of cells on it. */
    private static final int SQUARE_X = 0, SQUARE_Z = 24, SQUARE_SIDE = 146, CELL_W = 28, CELL_D = 23, GRID_X = 3, GRID_Z = 4;

    /** Where a cell's middle is (x, z), for a scene {@code span} cells wide. */
    private static int[] cellCentre(int column, int row, int span) {
        return new int[]{SQUARE_X + GRID_X + column * CELL_W + span * CELL_W / 2, SQUARE_Z + GRID_Z + row * CELL_D + CELL_D / 2};
    }

    /**
     * Moves every scene of the test world off its own platform onto one square platform at y = 34, in a grid, with its
     * sheep, labels and armour stands, and re-points the Barely Functional Wrench's generator links; then sets the
     * spawn at the platform's south edge: {@code -PharnessScene=square_world -PharnessWorld="BFC Test World"}.
     */
    private static void squareWorldTick(Minecraft mc, MinecraftServer server) {
        if (tick == 40) {
            command(server, "forceload add -96 -96 200 64");
            command(server, "forceload add 201 -96 520 64");
            command(server, String.format(java.util.Locale.ROOT, "forceload add %d %d %d %d", SQUARE_X, SQUARE_Z, SQUARE_X + SQUARE_SIDE, SQUARE_Z + SQUARE_SIDE));
        }
        if (tick == 300) server.execute(() -> squareWorld(server));
        if (tick == 340) server.execute(() -> {
            command(server, "forceload remove all");
            command(server, "gamerule send_command_feedback true");
        });
        if (tick == 360) mc.stop();
    }

    private static void squareWorld(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var cmds = server.getCommands();
        java.util.function.Consumer<String> run = c -> cmds.performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
        run.accept("gamerule send_command_feedback false");
        // Each scene's offset, worked out from its cell.
        int n = SQUARE_SCENES.length;
        int[][] offsets = new int[n][];
        for (int i = 0; i < n; i++) {
            int[] b = (int[]) SQUARE_SCENES[i][1];
            int span = "Themed sheep".equals(SQUARE_SCENES[i][0]) ? 2 : 1;
            int[] centre = cellCentre((Integer) SQUARE_SCENES[i][3], (Integer) SQUARE_SCENES[i][4], span);
            offsets[i] = new int[]{centre[0] - (b[0] + b[1] + 1) / 2, (Integer) SQUARE_SCENES[i][2], centre[1] - (b[2] + b[3] + 1) / 2};
        }
        java.util.function.Function<BlockPos, BlockPos> moved = pos -> {
            for (int i = 0; i < n; i++) {
                int[] b = (int[]) SQUARE_SCENES[i][1];
                if (pos.getX() >= b[0] - 1 && pos.getX() <= b[1] + 1 && pos.getZ() >= b[2] - 1 && pos.getZ() <= b[3] + 1 && pos.getY() >= b[4] && pos.getY() <= b[5] + 6) {
                    return pos.offset(offsets[i][0], offsets[i][1], offsets[i][2]);
                }
            }
            return null;
        };
        // The wrench's generator-to-generator links whose ends both move, before moving.
        var manualLinks = com.jax3.barelyfunctionalcontraptions.mycelial.network.ManualLinkData.get(server);
        java.util.List<GlobalPos[]> manual = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            int[] b = (int[]) SQUARE_SCENES[i][1];
            for (BlockPos p : BlockPos.betweenClosed(b[0] - 1, b[4], b[2] - 1, b[1] + 1, b[5] + 6, b[3] + 1)) {
                GlobalPos gp = GlobalPos.of(level.dimension(), p.immutable());
                for (GlobalPos m : manualLinks.linksOf(gp)) if (gp.pos().asLong() < m.pos().asLong()) manual.add(new GlobalPos[]{gp, m});
            }
        }
        // The entities of each scene, before moving (the clone would leave them floating).
        java.util.List<java.util.List<net.minecraft.world.entity.Entity>> entities = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            int[] b = (int[]) SQUARE_SCENES[i][1];
            var box = new net.minecraft.world.phys.AABB(b[0] - 1, b[4], b[2] - 1, b[1] + 2, b[5] + 10, b[3] + 2);
            entities.add(level.getEntities((net.minecraft.world.entity.Entity) null, box,
                    e -> !(e instanceof ServerPlayer) && !(e instanceof net.minecraft.world.entity.monster.Enemy)
                            && !(e instanceof net.minecraft.world.entity.ExperienceOrb) && !(e instanceof net.minecraft.world.entity.item.ItemEntity)));
        }
        // The platform: smooth stone, a stone-brick line between the cells, a polished-deepslate rim.
        for (int x = SQUARE_X; x < SQUARE_X + SQUARE_SIDE; x++) {
            for (int z = SQUARE_Z; z < SQUARE_Z + SQUARE_SIDE; z++) {
                boolean rim = x == SQUARE_X || z == SQUARE_Z || x == SQUARE_X + SQUARE_SIDE - 1 || z == SQUARE_Z + SQUARE_SIDE - 1;
                int gx = x - SQUARE_X - GRID_X, gz = z - SQUARE_Z - GRID_Z;
                boolean line = (gx >= 0 && gx <= 5 * CELL_W && gx % CELL_W == 0 && gz >= 0 && gz <= 6 * CELL_D)
                        || (gz >= 0 && gz <= 6 * CELL_D && gz % CELL_D == 0 && gx >= 0 && gx <= 5 * CELL_W);
                level.setBlock(new BlockPos(x, 34, z), (rim ? Blocks.POLISHED_DEEPSLATE : line ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE).defaultBlockState(), 2);
            }
        }
        // Move each scene's blocks (only its blocks, so the platform stays round them), then its entities.
        for (int i = 0; i < n; i++) {
            int[] b = (int[]) SQUARE_SCENES[i][1], o = offsets[i];
            {
                int y0 = b[4], y1 = b[5] + 6;
                run.accept(String.format(java.util.Locale.ROOT, "clone %d %d %d %d %d %d %d %d %d masked move",
                        b[0] - 1, y0, b[2] - 1, b[1] + 1, y1, b[3] + 1, b[0] - 1 + o[0], y0 + o[1], b[2] - 1 + o[2]));
            }
            for (var e : entities.get(i)) e.teleportTo(e.getX() + o[0], e.getY() + o[1], e.getZ() + o[2]);
            boolean labelled = entities.get(i).stream().anyMatch(e -> e instanceof net.minecraft.world.entity.Display.TextDisplay);
            int[] centre = new int[]{(b[0] + b[1] + 1) / 2 + o[0], (b[2] + b[3] + 1) / 2 + o[2]};
            if (!labelled) label(server, new BlockPos(centre[0], 41, (b[2] + o[2]) - 1), (String) SQUARE_SCENES[i][0], "white");
            ResourcefulSheep.LOGGER.warn("SQUARE '{}' moved by {} {} {} ({} entities), centre now {} {}", SQUARE_SCENES[i][0], o[0], o[1], o[2],
                    entities.get(i).size(), centre[0], centre[1]);
        }
        // Re-point the links that moved.
        for (GlobalPos[] link : manual) {
            BlockPos a = moved.apply(link[0].pos()), c = moved.apply(link[1].pos());
            if (a == null || c == null) continue;
            manualLinks.remove(link[0], link[1]);
            manualLinks.add(GlobalPos.of(level.dimension(), a), GlobalPos.of(level.dimension(), c));
        }
        ResourcefulSheep.LOGGER.warn("SQUARE links moved: {} manual", manual.size());
        // A crafting table left over from an old screenshot.
        level.setBlock(new BlockPos(-17, 39, 8), Blocks.AIR.defaultBlockState(), 3);
        // Clear out wild mobs and leftovers, and stop them spawning in the platform's shadow on the sand below.
        run.accept("gamerule spawn_mobs false");
        // (Discarded, not killed: a killed slime splits into smaller ones.)
        for (var e : java.util.List.copyOf((java.util.Collection<net.minecraft.world.entity.Entity>) com.google.common.collect.Lists.newArrayList(level.getAllEntities()))) {
            if (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof net.minecraft.world.entity.item.ItemEntity
                    || e instanceof net.minecraft.world.entity.ExperienceOrb) e.discard();
        }
        int sx = SQUARE_X + SQUARE_SIDE / 2;
        run.accept(String.format(java.util.Locale.ROOT, "setworldspawn %d 35 %d", sx, SQUARE_Z + SQUARE_SIDE - 3));
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        player.teleportTo(level, sx + 0.5, 35, SQUARE_Z + SQUARE_SIDE - 2.5, java.util.Set.of(), 180, 10, true);
        ResourcefulSheep.LOGGER.warn("SQUARE done; spawn {} 35 {}", sx, SQUARE_Z + SQUARE_SIDE - 3);
    }

    /** Logs each separate group of blocks above the superflat ground (the scenes), with its bounds and entities. */
    private static void surveyWorld(MinecraftServer server) {
        ServerLevel level = server.overworld();
        int x0 = -96, x1 = 520, z0 = -96, z1 = 200, y0 = 4, y1 = 90;
        int w = x1 - x0 + 1, d = z1 - z0 + 1, hgt = y1 - y0 + 1;
        boolean[] solid = new boolean[w * d * hgt];
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) level.getChunk(cx, cz);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) {
            if (!level.getBlockState(m.set(x, y, z)).isAir()) solid[((x - x0) * d + (z - z0)) * hgt + (y - y0)] = true;
        }
        boolean[] seen = new boolean[solid.length];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        for (int i = 0; i < solid.length; i++) {
            if (!solid[i] || seen[i]) continue;
            int minX = Integer.MAX_VALUE, minY = minX, minZ = minX, maxX = Integer.MIN_VALUE, maxY = maxX, maxZ = maxX, count = 0;
            seen[i] = true; queue.add(i);
            while (!queue.isEmpty()) {
                int k = queue.poll(); count++;
                int y = k % hgt, z = (k / hgt) % d, x = k / hgt / d;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y); minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) {
                    int nx = x + dx, ny = y + dy, nz = z + dz;
                    if (nx < 0 || ny < 0 || nz < 0 || nx >= w || ny >= hgt || nz >= d) continue;
                    int n = (nx * d + nz) * hgt + ny;
                    if (solid[n] && !seen[n]) { seen[n] = true; queue.add(n); }
                }
            }
            if (count < 1 || minZ + z0 > 23 && count > 100) continue;
            if (minZ + z0 <= 23 || count < 40) {
                StringBuilder what = new StringBuilder();
                java.util.Set<String> names = new java.util.TreeSet<>();
                for (int xx = minX + x0; xx <= maxX + x0; xx++) for (int zz = minZ + z0; zz <= maxZ + z0; zz++) for (int yy = minY + y0; yy <= maxY + y0; yy++) {
                    var st = level.getBlockState(new BlockPos(xx, yy, zz));
                    if (!st.isAir()) names.add(st.getBlock().getDescriptionId().replace("block.minecraft.", "").replace("block.barelyfunctionalcontraptions.", "bfc:"));
                }
                ResourcefulSheep.LOGGER.warn("SURVEY blocks {}", names);
            }
            var box = new net.minecraft.world.phys.AABB(minX + x0, minY + y0, minZ + z0, maxX + x0 + 1, maxY + y0 + 1, maxZ + z0 + 1).inflate(1, 4, 1);
            var ents = level.getEntities((net.minecraft.world.entity.Entity) null, box, e -> !(e instanceof ServerPlayer));
            java.util.Map<String, Integer> kinds = new java.util.TreeMap<>();
            for (var e : ents) kinds.merge(net.minecraft.world.entity.EntityType.getKey(e.getType()).getPath(), 1, Integer::sum);
            ResourcefulSheep.LOGGER.warn("SURVEY x {}..{} y {}..{} z {}..{} blocks {} entities {}", minX + x0, maxX + x0, minY + y0, maxY + y0, minZ + z0, maxZ + z0, count, kinds);
        }
        ResourcefulSheep.LOGGER.warn("SURVEY links {}", com.jax3.barelyfunctionalcontraptions.mycelial.network.ManualLinkData.get(server));
    }

    private static net.minecraft.world.phys.Vec3 extendReturn = net.minecraft.world.phys.Vec3.ZERO;
    private static float extendYaw, extendPitch;

    private static void extendWorldTick(Minecraft mc, MinecraftServer server) {
        int start = 60, per = 80;
        int end = start + EXTRA_SCENES.length * per;
        if (tick == start) {
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                extendReturn = player.position();
                extendYaw = player.getYRot();
                extendPitch = player.getXRot();
                ResourcefulSheep.LOGGER.info("EXTEND player starts at {}", extendReturn);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "forceload add 0 160 145 200");
                extendPlatform(server); // (moves the return spot to the new spawn)
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "forceload remove 0 160 145 200");
            });
        }
        if (tick >= start && tick < end) {
            int i = (tick - start) / per, step = (tick - start) % per;
            int[] cell = cellCentre((Integer) EXTRA_SCENES[i][1], (Integer) EXTRA_SCENES[i][2], 1);
            // The scenes build round the player, from 12 blocks north of them to 5 south: stand so that's the cell's middle.
            int x = cell[0], z = cell[1] + 3;
            if (step == 0) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(x + 0.5, 35, z + 0.5);
                });
            } else if (step == 40) {
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    player.teleportTo(x + 0.5, 35, z + 0.5); // exactly on the spot, in case it drifted
                    // Earlier runs built woolBurnerScene, woolPackerScene, altarScene (with bone blocks and a
                    // wither rose in its catalyst slots 2 and 3) and chillerScene here.
                    // poweredFurnaceScene, woollenArmorScene, sheepTreadmillScene, oreLineScene, thermalWorldScene,
                    // echoWorldScene (then echoWorldKick, two ticks later) and staticWorldScene, all since moved onto
                    // the square platform by squareWorld, and seederWorldScene.
                    switch ((String) EXTRA_SCENES[i][0]) {
                        case "grass seeder" -> seederWorldScene(server);
                        case "herd counter" -> herdWorldScene(server);
                        case "status panel" -> panelWorldScene(server);
                        case "crates" -> crateWorldScene(server);
                        case "fleece trawler" -> trawlerWorldScene(server);
                        case "cable the wireless scene" -> cableWirelessScene(server);
                        case "upgrades" -> upgradeWorldScene(server);
                        case "hume field" -> humeFieldWorldScene(server);
                        case "hume field on the panel" -> linkHumeFieldToPanel(server);
                        default -> throw new IllegalStateException("no builder for " + EXTRA_SCENES[i][0]);
                    }
                    ResourcefulSheep.LOGGER.info("EXTEND scene '{}' built at origin {}", EXTRA_SCENES[i][0], origin);
                });
            }
        }
        if (tick == end + 20) {
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                player.teleportTo(player.level(), extendReturn.x, extendReturn.y, extendReturn.z, java.util.Set.of(), extendYaw, extendPitch, true);
                // The scenes turn command feedback off for clean shots; the shared world must keep it on, or commands
                // such as /bfc list show nothing.
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule send_command_feedback true");
                // Save the Thermal Reaction Chamber, the Echo Chamber, the Static Generator and their cube banks empty, so they
                // run for a while when someone visits.
                ServerLevel level = player.level();
                // The Echo Chamber (its port, from the back) and its cube bank, and the Static Generator and its cubes, from
                // the north; the Thermal Reaction Chamber and its cubes (x = 101) from above.
                BlockPos[] drain = {new BlockPos(129, 35, 130), new BlockPos(129, 35, 128), new BlockPos(17, 35, 154), new BlockPos(17, 35, 152),
                        new BlockPos(101, 35, 131), new BlockPos(101, 35, 133)};
                for (BlockPos p : drain) {
                    Direction side = p.getX() == 101 ? Direction.UP : Direction.NORTH;
                    var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, p, side);
                    if (handler == null) continue;
                    for (int k = 0; k < 10_000 && handler.getAmountAsLong() > 0; k++) { // a cube bank only gives so much at a time
                        try (Transaction tx = Transaction.openRoot()) {
                            handler.extract(Integer.MAX_VALUE, tx);
                            tx.commit();
                        }
                    }
                    ResourcefulSheep.LOGGER.info("EXTEND drained {} to {}", p, handler.getAmountAsLong());
                }
                ResourcefulSheep.LOGGER.info("EXTEND player back at {}", player.position());
            });
        }
        if (tick == end + 60) mc.stop();
    }

    /**
     * A charged Hume Energy Cube and a filled Fluid Tank broken with real creative clicks drop themselves with what they
     * held; the items' tooltips show it: {@code -PharnessScene=keep_contents}.
     */
    private static void keepContentsTick(Minecraft mc, MinecraftServer server) {
        BlockPos cubePos = origin == null ? null : origin.offset(0, 0, -2);
        BlockPos tankPos = origin == null ? null : origin.offset(1, 0, -2);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                command(server, "gamemode creative @a");
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-3, 0, -5), origin.offset(3, 3, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                BlockPos c = origin.offset(0, 0, -2), t = origin.offset(1, 0, -2);
                level.setBlock(c, ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
                level.setBlock(t, ModBlocks.FLUID_TANK.get().defaultBlockState(), 3);
                if (level.getBlockEntity(c) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity cube) cube.storage().set(750_000);
                if (level.getBlockEntity(t) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankBlockEntity tank) {
                    try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                        tank.getFluidHandler().insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA), 12_000, tx);
                        tx.commit();
                    }
                }
                player.getInventory().clearContent();
            });
            // Real creative left clicks on each block.
            case 80 -> { if (mc.gameMode != null) mc.gameMode.startDestroyBlock(cubePos, Direction.UP); }
            case 90 -> { if (mc.gameMode != null) mc.gameMode.startDestroyBlock(tankPos, Direction.UP); }
            case 110 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                for (var e : player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(origin).inflate(6))) {
                    ItemStack item = e.getItem();
                    ResourcefulSheep.LOGGER.info("DROPPED {} energy={} fluid={}", item,
                            item.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.MACHINE_CONTENTS.get()),
                            java.util.Optional.ofNullable(item.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.FLUID_CONTENTS.get())).map(f -> f.copy().getAmount() + " " + f.copy().getHoverName().getString()).orElse("none"));
                    player.getInventory().add(item.copy());
                    e.discard();
                }
                ResourcefulSheep.LOGGER.info("BLOCKS cube={} tank={}", player.level().getBlockState(origin.offset(0, 0, -2)), player.level().getBlockState(origin.offset(1, 0, -2)));
            });
            case 115 -> command(server, "gamemode survival @a");
            case 125 -> { if (mc.player != null) McClient.setScreen(mc, new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); }
            case 135 -> hover(mc, 176, 166, 16, 150); // hotbar slot 1: the cube
            case 150 -> shot(mc, "97_cube_keeps_energy");
            case 152 -> hover(mc, 176, 166, 34, 150); // hotbar slot 2: the tank
            case 167 -> shot(mc, "97_tank_keeps_fluid");
            case 175 -> mc.stop();
            default -> {}
        }
    }

    /** For the README: every block on stepped rows, the creative tabs, and every built-in sheep: {@code -PharnessScene=showcase}. */
    private static void showcaseTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> showcaseScene(server));
            case 70 -> {
                setHudHidden(mc, true);
                command(server, "gamemode spectator @a"); // floats where it is put, so the camera can sit up high
            }
            case 75 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 28", origin.getX() + 0.5, origin.getY() + 5, origin.getZ() + 2.5));
            case 158, 228 -> {
                if (mc.player != null) ResourcefulSheep.LOGGER.info("CAMERA at {} yaw={} pitch={} origin={}", mc.player.position(), mc.player.getYRot(), mc.player.getXRot(), origin);
            }
            case 160 -> shot(mc, "showcase_blocks");
            case 162 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 0 33", origin.getX() + 0.5, origin.getY() + 7, origin.getZ() + 11.0));
            case 230 -> shot(mc, "showcase_sheep");
            case 232 -> {
                command(server, "gamemode creative @a");
                openCreativeTab(mc, com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.MAIN.get());
            }
            case 250 -> shot(mc, "showcase_tab_main");
            case 252 -> openCreativeTab(mc, com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.RESOURCE_WOOL.get());
            case 256, 236 -> com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); // no tooltip in the shot
            case 270 -> shot(mc, "showcase_tab_wool");
            case 272 -> {
                // Scroll to the bottom of the tab, where the spawn eggs are, and point at one for its tooltip.
                if (McClient.screen(mc) != null) McClient.screen(mc).mouseScrolled(213, 120, 0, -20);
            }
            case 276 -> com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), EGG_MOUSE_X, EGG_MOUSE_Y);
            case 290 -> shot(mc, "showcase_tab_eggs");
            case 294 -> {
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
                McClient.setScreen(mc, new GuideScreen());
            }
            case 300 -> {
                if (McClient.screen(mc) instanceof GuideScreen guide) guide.openTab(GuideBook.Tab.RECIPES);
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); // no tooltip in the shots
            }
            case 310 -> shot(mc, "99_guide_recipes_machines");
            case 312 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Mechanical Separator"); }
            case 322 -> shot(mc, "99_guide_recipes_separator");
            case 324 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Infusion Altar"); }
            case 336 -> shot(mc, "99_guide_recipes_altar");
            case 338 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Wool Bales"); }
            case 348 -> shot(mc, "99_guide_recipes_bales");
            case 350 -> { if (McClient.screen(mc) instanceof GuideScreen guide) { guide.openTab(GuideBook.Tab.WIP); guide.openChapter("Quantum Sheep"); } }
            case 360 -> shot(mc, "99_guide_wip");
            case 362 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Wool Burner"); }
            case 372 -> shot(mc, "83_guide_power_tab");
            case 376 -> mc.stop();
            default -> {}
        }
    }

    static final int EGG_MOUSE_X = 455, EGG_MOUSE_Y = 455;

    private static final String[][] SHOWCASE_ROWS = {
            {"mechanical_separator", "automatic_shears", "wool_packer", "infusion_altar", "wool_bale"},
            {"mycelial_generator", "zero_point_energy_module", "glow_panel", "glow_lamp", "blaze_furnace"},
            {"wool_burner", "hume_energy_cube", "fluid_tank", "fluid_reservoir", "creative_generator"},
            {"energy_cable", "fluid_pipe", "item_pipe", "energy_meter", "functional_machine_block"},
    };

    private static void showcaseScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false",
                "gamerule advance_weather false", "gamerule spawn_mobs false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-14, 0, -14), o.offset(14, 10, 40))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-14, -1, -14), o.offset(14, -1, 40))) level.setBlock(p, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        // Stepped rows: the front row on the floor, each further row a block higher, so every label shows.
        for (int row = 0; row < SHOWCASE_ROWS.length; row++) {
            int z = -3 - row * 3, y = row;
            if (y > 0) {
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-9, 0, z - 1), o.offset(9, y - 1, z + 1))) level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            }
            String[] names = SHOWCASE_ROWS[row];
            for (int i = 0; i < names.length; i++) {
                BlockPos at = o.offset((int) Math.round((i - (names.length - 1) / 2.0) * 4), y, z);
                var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(ResourcefulSheep.id(names[i]));
                net.minecraft.world.level.block.state.BlockState state = block.defaultBlockState();
                for (var prop : state.getProperties()) {
                    if (prop.getName().equals("facing") && prop.getPossibleValues().contains(Direction.SOUTH)) {
                        state = setUnchecked(state, prop, Direction.SOUTH);
                    }
                }
                level.setBlock(at, state, 3);
                if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBaleBlockEntity bale) {
                    bale.setResource(ResourcefulSheep.id("gold"));
                    level.sendBlockUpdated(at, bale.getBlockState(), bale.getBlockState(), 3);
                }
                net.minecraft.network.chat.Component name = new ItemStack(block).getHoverName();
                if (names[i].equals("wool_bale")) name = net.minecraft.network.chat.Component.literal("Wool Bale");
                String[] colours = {"light_purple", "gold", "yellow", "aqua"};
                label(server, at.above(), name.getString(), colours[row]);
            }
        }
        // Every built-in sheep in a pen further back, each with its name.
        var resources = com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.server().all().stream()
                .filter(r -> r.id().getNamespace().equals(ResourcefulSheep.MOD_ID)).toList();
        // As square a grid as the count allows (5 x 5 for the 25 built-in sheep), so no row is left nearly empty.
        int perRow = (int) Math.ceil(Math.sqrt(resources.size()));
        for (int i = 0; i < resources.size(); i++) {
            var r = resources.get(i);
            int col = i % perRow, row = i / perRow;
            double x = o.getX() + 0.5 + (col - (perRow - 1) / 2.0) * 3.8;
            double z = o.getZ() + 16.5 + row * 4.0;
            Sheep sh = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            sh.snapTo(x, o.getY(), z, 160, 0); // facing the camera, which looks south from in front of the pen
            sh.setYHeadRot(160);
            sh.setYBodyRot(160);
            sh.yBodyRotO = 160;
            sh.setNoAi(true);
            com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.setInfusion(sh,
                    new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion(java.util.Optional.of(r.id()), 9, 0));
            level.addFreshEntity(sh);
            double scale = r.appearance().scale();
            label(server, BlockPos.containing(x, o.getY() + 1.1 + (scale > 1 ? 1.1 : 0), z), r.name().getString(), "white");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static net.minecraft.world.level.block.state.BlockState setUnchecked(net.minecraft.world.level.block.state.BlockState state,
                                                                                net.minecraft.world.level.block.state.properties.Property prop, Comparable value) {
        return state.setValue(prop, value);
    }

    /** The Infusion Altar making a Wither sheep, and a bred sheep's stats: {@code -PharnessScene=altar}. */
    private static void altarTick(Minecraft mc, MinecraftServer server) {
        BlockPos altar = origin == null ? null : origin.offset(0, 0, -3);
        switch (tick) {
            case 60 -> server.execute(() -> altarScene(server));
            case 70 -> setHudHidden(mc, true);
            // First only the star and the soul sand: the window shows the missing catalysts faded.
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 0.5, origin.getY(), origin.getZ() - 0.5));
            case 86 -> openAt(server, altar);
            case 100 -> shot(mc, "92_altar_missing_catalysts");
            case 102 -> hover(mc, 176, 193, 100, 47); // an empty side slot: the full list, missing ones in red
            case 118 -> shot(mc, "92_altar_catalyst_tooltip");
            case 120 -> {
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
                server.execute(() -> {
                    if (server.overworld().getBlockEntity(altar) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity be) {
                        be.setItem(2, new ItemStack(Items.BONE_BLOCK, 2));
                        be.setItem(3, new ItemStack(Items.WITHER_ROSE, 1));
                    }
                });
            }
            // Show the area a sheep must stand in, with the window's spyglass button, as a player would.
            case 124 -> {
                if (mc.player != null && mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,
                        com.jax3.barelyfunctionalcontraptions.sheep.menu.InfusionAltarMenu.BUTTON_AREA);
            }
            case 150 -> shot(mc, "93_altar_infusing_gui");
            case 152 -> {
                if (mc.player != null) mc.player.closeContainer();
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 204 30", origin.getX() - 0.5, origin.getY() + 1, origin.getZ() + 1.5));
            }
            case 175 -> shot(mc, "94_altar_infusing");
            case 176, 260 -> server.execute(() -> {
                if (server.overworld().getBlockEntity(altar) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity be) {
                    ResourcefulSheep.LOGGER.info("ALTAR status={} progress={}/{} energy={}", be.status(), be.progress(), be.maxProgress(), be.getEnergyHandler().getAmountAsInt());
                }
                for (var sheep : server.overworld().getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class, new net.minecraft.world.phys.AABB(origin).inflate(12))) {
                    ResourcefulSheep.LOGGER.info("SHEEP {} infusion={} stats={}", sheep.getId(),
                            com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.getInfusion(sheep),
                            com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.getStats(sheep));
                }
            });
            // The bred sheep: look at it with the HUD on (Jade), and show the sneak-inspect line.
            case 262 -> {
                setHudHidden(mc, false);
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f -90 20", origin.getX() + 7.0, origin.getY(), origin.getZ() - 2.5));
            }
            case 272 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                for (var sheep : server.overworld().getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class, new net.minecraft.world.phys.AABB(origin).inflate(12))) {
                    if (!com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.getStats(sheep).isWild()) {
                        player.sendOverlayMessage(com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.describe(sheep));
                    }
                }
            });
            case 285 -> shot(mc, "95_sheep_stats");
            case 287 -> McClient.setScreen(mc, new GuideScreen());
            case 297 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Infusion Altar"); }
            case 310 -> shot(mc, "96_guide_altar");
            case 312 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Breeding Sheep"); }
            case 325 -> shot(mc, "96_guide_breeding");
            // JEI: the Infusion Altar page, and what Soul Sand is used for.
            case 327 -> {
                McClient.setScreen(mc, null);
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    ResourcefulSheep.LOGGER.info("JEI {}", com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showAltar());
                }
            }
            case 345 -> shot(mc, "98_jei_altar");
            case 347 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.show(new ItemStack(Items.SOUL_SAND), false);
                }
            }
            case 365 -> shot(mc, "98_jei_soul_sand_uses");
            case 375 -> mc.stop();
            default -> {}
        }
    }

    private static void altarScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(10, 6, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(10, -1, 6))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        BlockPos altar = o.offset(0, 0, -3);
        level.setBlock(altar, ModBlocks.INFUSION_ALTAR.get().defaultBlockState(), 3);
        level.setBlock(altar.north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(altar) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity be) {
            be.setItem(0, new ItemStack(Items.NETHER_STAR, 1));
            be.setItem(1, new ItemStack(Items.SOUL_SAND, 4));
        }
        var sheep = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        sheep.snapTo(altar.getX() + 1.5, altar.getY(), altar.getZ() + 0.5, 90, 0);
        sheep.setNoAi(true);
        level.addFreshEntity(sheep);
        // A well-bred sheep further along.
        var bred = Vanilla.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        bred.snapTo(o.getX() + 9.5, o.getY(), o.getZ() - 2.5, 90, 0);
        bred.setNoAi(true);
        level.addFreshEntity(bred);
        com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.setStats(bred, new com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats(7, 4, 9));
        int uses = com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.infusionUses(bred,
                com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry.server().byId(ResourcefulSheep.id("gold")).orElseThrow());
        com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.setInfusion(bred,
                com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion.NONE.withResource(ResourcefulSheep.id("gold"), uses));
        label(server, altar.above(), "Infusion Altar", "light_purple");
    }

    /** Wool Packers fed by hoppers, a separator working a bale, and placed bales of every colour: {@code -PharnessScene=wool_packer}. */
    private static void woolPackerTick(Minecraft mc, MinecraftServer server) {
        BlockPos packer = origin == null ? null : origin.offset(-3, 0, -4);
        switch (tick) {
            case 60 -> server.execute(() -> woolPackerScene(server));
            case 70 -> setHudHidden(mc, true);
            case 150 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                if (level.getBlockEntity(packer) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity be) {
                    ResourcefulSheep.LOGGER.info("PACKER status={} progress={}/{} input={} output={} energy={} active={}", be.status(), be.progress(), be.maxProgress(),
                            be.input(), be.output(), be.getEnergyHandler().getAmountAsInt(),
                            be.getBlockState().getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolPackerBlock.ACTIVE));
                }
                if (level.getBlockEntity(origin.offset(3, 0, -4)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity sep) {
                    ResourcefulSheep.LOGGER.info("SEPARATOR status={} progress={}/{} energyPerTick={} input={}", sep.getStatus(), sep.getProgress(), sep.getMaxProgress(),
                            sep.getEnergyPerTick(), sep.getInventory().getStack(0));
                }
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(-8, 0, 1), origin.offset(8, 0, 3))) {
                    if (level.getBlockEntity(p) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBaleBlockEntity bale) {
                        ResourcefulSheep.LOGGER.info("BALE at {} resource={}", p.immutable(), bale.resource());
                    }
                }
            });
            case 160 -> shot(mc, "85_wool_packers");
            case 162 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 200 30", origin.getX() - 2.5, origin.getY() + 2, origin.getZ() + 6.5));
            case 190 -> {
                ClientLevelLog.bales(mc, origin);
                shot(mc, "86_wool_bales");
            }
            case 192 -> {
                setHudHidden(mc, false);
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() - 2.5, origin.getY(), origin.getZ() - 1.5));
            }
            case 196 -> openAt(server, packer);
            case 206 -> shot(mc, "87_wool_packer_gui");
            // Hover over the pips.
            case 208 -> hover(mc, 176, 193, 68, 60);
            case 222 -> shot(mc, "87_wool_packer_gui_pips");
            case 224 -> {
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
                if (mc.player != null) mc.player.closeContainer();
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 3.5, origin.getY(), origin.getZ() - 1.5));
            }
            case 230 -> openAt(server, origin.offset(3, 0, -4));
            case 240 -> hover(mc, 176, 193, 52, 64); // the bale in the separator's input slot
            case 256 -> shot(mc, "88_separator_bale_gui");
            case 258 -> {
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
                if (mc.player != null) mc.player.closeContainer();
                McClient.setScreen(mc, new GuideScreen());
            }
            case 268 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Wool Packer"); }
            case 280 -> shot(mc, "89_guide_wool_packer");
            // Unpacking: a Diamond Wool Bale in the inventory's 2x2 crafting grid.
            case 282 -> {
                McClient.setScreen(mc, null);
                command(server, "gamemode survival @a"); // the survival inventory, with its crafting grid
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    player.inventoryMenu.getSlot(1).set(com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(ResourcefulSheep.id("diamond"), 3));
                    player.inventoryMenu.slotsChanged(player.inventoryMenu.getCraftSlots());
                    player.inventoryMenu.broadcastChanges();
                });
            }
            case 288 -> { if (mc.player != null) McClient.setScreen(mc, new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); }
            case 296 -> {
                if (mc.player != null) ResourcefulSheep.LOGGER.info("UNPACK result={}", mc.player.inventoryMenu.getSlot(0).getItem());
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); // no tooltip over the grid
            }
            case 310 -> shot(mc, "90_unpack_wool_bale");
            case 316 -> mc.stop();
            default -> {}
        }
    }

    /** Moves the mouse to ({@code x}, {@code y}) inside a centred window of the given size. */
    private static void hover(Minecraft mc, int width, int height, int x, int y) {
        var window = mc.getWindow();
        double scale = window.getGuiScale();
        int left = (window.getGuiScaledWidth() - width) / 2, top = (window.getGuiScaledHeight() - height) / 2;
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, window.handle(), (left + x) * scale, (top + y) * scale);
    }

    /** Client-side check that placed bales got their wool (and so their colour). */
    private static final class ClientLevelLog {
        static void bales(Minecraft mc, BlockPos origin) {
            if (mc.level == null) return;
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-8, 0, 1), origin.offset(8, 0, 3))) {
                if (mc.level.getBlockEntity(p) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBaleBlockEntity bale) {
                    ResourcefulSheep.LOGGER.info("CLIENT BALE at {} resource={}", p.immutable(), bale.resource());
                }
            }
        }
    }

    private static void woolPackerScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(8, 6, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(8, -1, 6))) level.setBlock(p, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        var south = Direction.SOUTH;
        // Two packers fed from chests through hoppers, powered by a creative generator behind each.
        net.minecraft.resources.Identifier[] wools = {ResourcefulSheep.id("iron"), ResourcefulSheep.id("gold")};
        for (int i = 0; i < 2; i++) {
            BlockPos at = o.offset(-3 - i * 3, 0, -4);
            level.setBlock(at, ModBlocks.WOOL_PACKER.get().defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolPackerBlock.FACING, south), 3);
            level.setBlock(at.north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
            level.setBlock(at.above(), Blocks.HOPPER.defaultBlockState(), 3);
            level.setBlock(at.above(2), Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(at.above(2)) instanceof net.minecraft.world.Container chest) {
                for (int k = 0; k < 4; k++) chest.setItem(k, com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(wools[i], 64));
            }
            if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity be) {
                be.setItem(0, com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(wools[i], 64));
                be.setItem(1, com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(wools[i], 5));
            }
        }
        label(server, o.offset(-3, 3, -4), "Wool Packer", "white");
        // A separator working an Iron Wool Bale.
        BlockPos sep = o.offset(3, 0, -4);
        level.setBlock(sep, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.MechanicalSeparatorBlock.FACING, south), 3);
        level.setBlock(sep.east(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(sep) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity be) {
            be.getInventory().setStack(0, com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(ResourcefulSheep.id("iron"), 3));
        }
        label(server, sep.above(), "Separating a bale", "gold");
        // A row of placed bales, every built-in wool, some lying down.
        var ids = com.jax3.barelyfunctionalcontraptions.registry.ContraptionsCreativeTab.BUILT_IN_WOOL;
        for (int i = 0; i < ids.size(); i++) {
            BlockPos at = o.offset(-7 + i % 7 * 2 + i / 7, 0, 1 + i / 7 * 2);
            Direction.Axis axis = i % 3 == 1 ? Direction.Axis.X : i % 3 == 2 ? Direction.Axis.Z : Direction.Axis.Y;
            level.setBlock(at, ModBlocks.WOOL_BALE.get().defaultBlockState().setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolBaleBlock.AXIS, axis), 3);
            if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBaleBlockEntity bale) {
                bale.setResource(ResourcefulSheep.id(ids.get(i)));
                level.sendBlockUpdated(at, bale.getBlockState(), bale.getBlockState(), 3);
            }
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 24", o.getX() + 0.0, o.getY() + 4, o.getZ() + 6.5));
    }

    /** Three Wool Burners (wool, string, Coal Wool) charging cubes, and one fed by a hopper: {@code -PharnessScene=wool_burner}. */
    private static void woolBurnerTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> woolBurnerScene(server));
            case 70 -> setHudHidden(mc, true);
            case 120 -> server.execute(() -> {
                for (int i = 0; i < 4; i++) {
                    if (server.overworld().getBlockEntity(origin.offset(-5 + i * 3, 0, -4)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity be) {
                        ResourcefulSheep.LOGGER.info("BURNER {} generating={} burn={}/{} fuel={} lit={}", i, be.generating(), be.burnTime(), be.burnTotal(), be.fuel(),
                                be.getBlockState().getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolBurnerBlock.LIT));
                    }
                }
            });
            case 140 -> shot(mc, "76_wool_burners");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "time set midnight"));
            case 190 -> shot(mc, "77_wool_burners_night");
            case 192 -> {
                setHudHidden(mc, false);
                command(server, "time set noon");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 1.5, origin.getY(), origin.getZ() - 1.5));
            }
            case 196 -> openAt(server, origin.offset(1, 0, -4)); // the Coal Wool burner
            case 206 -> shot(mc, "78_wool_burner_gui");
            // Hover over the fuel box for its list of fuels.
            case 208 -> {
                var window = mc.getWindow();
                double scale = window.getGuiScale();
                int left = (window.getGuiScaledWidth() - 176) / 2, top = (window.getGuiScaledHeight() - 193) / 2;
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, window.handle(), (left + 67) * scale, (top + 30) * scale);
            }
            case 225 -> shot(mc, "78_wool_burner_gui_fuels");
            case 227 -> {
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
                if (mc.player != null) mc.player.closeContainer();
                McClient.setScreen(mc, new GuideScreen());
            }
            case 237 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Wool Burner"); }
            case 250 -> shot(mc, "79_guide_wool_burner");
            // The other tabs, opened with real clicks on their buttons.
            case 252, 266, 280 -> {
                int tabIndex = (tick - 252) / 14 + 1; // Logistics, Production, Utility
                var window = mc.getWindow();
                double scale = window.getGuiScale();
                int left = (window.getGuiScaledWidth() - 330) / 2, top = (window.getGuiScaledHeight() - 214) / 2;
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, window.handle(), (left + 6 + tabIndex * 28 + 13) * scale, (top + 21 + 10) * scale);
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseButton(mc, window.handle(), new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), 1);
                com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseButton(mc, window.handle(), new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), 0);
            }
            case 258, 272, 286 -> com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); // no tooltip in the shot
            case 262 -> shot(mc, "79_guide_tab_logistics");
            case 276 -> shot(mc, "79_guide_tab_production");
            case 290 -> shot(mc, "79_guide_tab_utility");
            case 300 -> mc.stop();
            default -> {}
        }
    }

    private static void woolBurnerScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(8, 6, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(8, -1, 6))) level.setBlock(p, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
        // Plain wool, string, Coal Wool, and one fed by a hopper from a chest of mixed wool. Each charges a cube behind it.
        ItemStack[] fuels = {new ItemStack(Vanilla.wool(DyeColor.WHITE), 32), new ItemStack(Items.STRING, 64),
                com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("coal"), 16), ItemStack.EMPTY};
        String[] names = {"Wool: 30 FE/t", "String: 30 FE/t", "Coal Wool: 120 FE/t", "Fed by a hopper"};
        for (int i = 0; i < 4; i++) {
            BlockPos at = o.offset(-5 + i * 3, 0, -4);
            level.setBlock(at, ModBlocks.WOOL_BURNER.get().defaultBlockState()
                    .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolBurnerBlock.FACING, Direction.SOUTH), 3);
            if (level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity be) be.setItem(0, fuels[i]);
            level.setBlock(at.north(), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
            label(server, at.above(i == 3 ? 3 : 1), names[i], i == 2 ? "gold" : "white");
        }
        BlockPos fed = o.offset(4, 0, -4);
        level.setBlock(fed.above(), Blocks.HOPPER.defaultBlockState(), 3);
        level.setBlock(fed.above(2), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(fed.above(2)) instanceof net.minecraft.world.Container chest) {
            chest.setItem(0, new ItemStack(Vanilla.wool(DyeColor.RED), 64));
            chest.setItem(1, new ItemStack(Vanilla.wool(DyeColor.BLUE), 64));
            chest.setItem(2, new ItemStack(Items.STRING, 64));
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 20", o.getX() + 0.0, o.getY() + 2, o.getZ() + 4.5));
    }

    /** The Blaze Sheep Furnace with four Blaze sheep, and both machines' working areas shown: {@code -PharnessScene=blaze_furnace}. */
    private static void blazeFurnaceTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> blazeFurnaceScene(server));
            case 70 -> setHudHidden(mc, true);
            case 75 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 170 30", origin.getX() + 3.5, origin.getY() + 5, origin.getZ() + 5.5));
            case 130 -> server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                if (level.getBlockEntity(origin.offset(0, 0, -4)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity be) {
                    ResourcefulSheep.LOGGER.info("FURNACE generating={} heat={} heaters={} boost={}t x{} waiting={} lit={} message='{}'", be.generating(), be.heat(),
                            be.heaterCount(), be.boostTicks(), be.boostMultiplier(), be.boostItems(), be.getBlockState().getValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock.LIT),
                            com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock.statusMessage(be).getString());
                }
            });
            case 140 -> shot(mc, "62_blaze_furnace");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 0.5, origin.getY() + 2, origin.getZ() - 1.2));
            case 170 -> shot(mc, "63_blaze_furnace_close");
            case 172 -> {
                command(server, "time set midnight");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 170 30", origin.getX() + 3.5, origin.getY() + 5, origin.getZ() + 5.5));
            }
            case 230 -> shot(mc, "64_blaze_furnace_night");
            case 232 -> {
                setHudHidden(mc, false);
                command(server, "time set noon");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 0.5, origin.getY(), origin.getZ() - 1.5));
            }
            case 236 -> openAt(server, origin.offset(0, 0, -4));
            case 250 -> shot(mc, "67_blaze_furnace_gui");
            // Real clicks on the furnace window's area button: off, then on again.
            case 252, 264 -> clickGui(mc, 154 + 8, 4 + 8);
            case 262, 274 -> logArea(server, origin.offset(0, 0, -4), "furnace window button");
            // Shear two of the sheep: the heat column drops to two marks.
            case 276 -> server.execute(() -> server.overworld().getEntitiesOfClass(Sheep.class,
                    com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity.heatingArea(origin.offset(0, 0, -4)),
                    sh -> !sh.isSheared() && sh.getX() > origin.getX() + 0.6).forEach(sh -> sh.setSheared(true)));
            case 300 -> shot(mc, "68_blaze_furnace_gui_two_sheep");
            case 302 -> {
                if (mc.player != null) mc.player.closeContainer();
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 5.5, origin.getY(), origin.getZ() - 0.5));
            }
            case 306 -> openAt(server, origin.offset(5, 0, -4));
            case 320 -> shot(mc, "65_shears_area_button");
            // And on the shears' window button.
            case 322, 334 -> clickGui(mc, 136 + 8, 4 + 8);
            case 332, 344 -> logArea(server, origin.offset(5, 0, -4), "shears window button");
            // The redstone button beside it, with a real left click too.
            case 336 -> clickGui(mc, 154 + 8, 4 + 8);
            case 340 -> server.execute(() -> {
                if (server.overworld().getBlockEntity(origin.offset(5, 0, -4)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity be) {
                    ResourcefulSheep.LOGGER.info("REDSTONE after a click on the shears' redstone button: {}", be.getRedstoneMode());
                }
            });
            // Hide the furnace's area with its window button, close the window and look: only the shears' box is left.
            case 346 -> {
                if (mc.player != null) mc.player.closeContainer();
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", origin.getX() + 0.5, origin.getY(), origin.getZ() - 1.5));
            }
            case 350 -> openAt(server, origin.offset(0, 0, -4));
            case 362 -> clickGui(mc, 154 + 8, 4 + 8);
            case 368 -> {
                logArea(server, origin.offset(0, 0, -4), "furnace window button (then closed)");
                if (mc.player != null) mc.player.closeContainer();
                setHudHidden(mc, true);
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 170 30", origin.getX() + 3.5, origin.getY() + 5, origin.getZ() + 5.5));
            }
            case 400 -> shot(mc, "69_furnace_area_hidden_by_button");
            case 402 -> {
                setHudHidden(mc, false);
                McClient.setScreen(mc, new GuideScreen());
            }
            case 412 -> { if (McClient.screen(mc) instanceof GuideScreen guide) guide.openChapter("Blaze Furnace"); }
            case 426 -> shot(mc, "66_guide_blaze_furnace");
            case 436 -> mc.stop();
            default -> {}
        }
    }

    /** Opens the window of the block at {@code pos}, as a right-click does. */
    private static void openAt(MinecraftServer server, BlockPos pos) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            if (player.level().getBlockEntity(pos) instanceof net.minecraft.world.MenuProvider provider) player.openMenu(provider, pos);
        });
    }

    /** A real left click on the middle of row {@code row} of the open creative screen's item grid, on its banner's title. */
    private static void clickCreativeRow(Minecraft mc, int row) {
        if (!(McClient.screen(mc) instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen screen)) return;
        var window = mc.getWindow();
        double scale = window.getGuiScale();
        long handle = window.handle();
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, handle, (screen.getLeftPos() + 60) * scale, (screen.getTopPos() + 26 + row * 18) * scale);
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseButton(mc, handle, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), 1);
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseButton(mc, handle, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), 0);
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, handle, 4, 4);
    }

    /** A real left click at ({@code gx}, {@code gy}) inside a centred 176x193 container window, then the cursor parked. */
    private static void clickGui(Minecraft mc, int gx, int gy) {
        var window = mc.getWindow();
        double scale = window.getGuiScale();
        int left = (window.getGuiScaledWidth() - 176) / 2, top = (window.getGuiScaledHeight() - 193) / 2;
        long handle = window.handle();
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, handle, (left + gx) * scale, (top + gy) * scale);
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseButton(mc, handle, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), 1);
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseButton(mc, handle, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0), 0);
        com.jax3.barelyfunctionalcontraptions.compat.McClient.mouseMove(mc, handle, 4, 4);
    }

    private static void logArea(MinecraftServer server, BlockPos pos, String what) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AreaDisplay client) {
            ResourcefulSheep.LOGGER.info("AREA (client) after a click on the {}: shown={}", what, client.showsArea());
        }
        server.execute(() -> {
            if (server.overworld().getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AreaDisplay area) {
                ResourcefulSheep.LOGGER.info("AREA after a click on the {}: shown={}", what, area.showsArea());
            }
        });
    }

    private static void blazeFurnaceScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(10, 6, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(10, -1, 8))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        // The furnace, front to the camera, with a Hume Energy Cube under it and Blaze Rods loaded.
        BlockPos furnacePos = o.offset(0, 0, -4);
        level.setBlock(furnacePos, ModBlocks.BLAZE_FURNACE.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(furnacePos.below(), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(furnacePos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity be) {
            be.insertBoost(new ItemStack(Items.BLAZE_ROD, 5), null);
            be.setShowsArea(true);
        }
        // Four Blaze sheep: one on top, one either side and one behind. A plain sheep in front doesn't count.
        double[][] spots = {{0.5, 1, -3.5, 160}, {-0.5, 0, -3.5, 120}, {1.5, 0, -3.5, -120}, {0.5, 0, -4.5, 0}, {-0.5, 0, -2.5, 200}};
        for (int i = 0; i < spots.length; i++) {
            double[] at = spots[i];
            Sheep sh = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
            float yaw = (float) at[3];
            sh.snapTo(o.getX() + at[0], o.getY() + at[1], o.getZ() + at[2], yaw, 0);
            sh.setYHeadRot(yaw);
            sh.yHeadRotO = yaw;
            sh.setYBodyRot(yaw);
            sh.yBodyRotO = yaw;
            sh.setNoAi(true);
            if (i < 4) SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id("blaze")), 9, 0));
            level.addFreshEntity(sh);
        }
        // Automatic Shears to the right, facing the camera, with their shearing area shown.
        BlockPos shearsPos = o.offset(5, 0, -4);
        level.setBlock(shearsPos, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(shearsPos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity be) be.setShowsArea(true);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 165 32", o.getX() + 1.5, o.getY() + 3, o.getZ() + 2.5));
    }

    private static void powerBlocksScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(8, 6, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(8, -1, 4))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        var panel = ModBlocks.GLOW_PANEL.get().defaultBlockState();
        // Left: a 3x2 field of panels in the open. Right: three panels under Glow Lamps in a stone shelter.
        for (int x = -6; x <= -4; x++) for (int z = -6; z <= -5; z++) level.setBlock(o.offset(x, 0, z), panel, 3);
        for (int x = 3; x <= 5; x++) {
            level.setBlock(o.offset(x, 0, -6), panel, 3);
            level.setBlock(o.offset(x, 1, -6), ModBlocks.GLOW_LAMP.get().defaultBlockState(), 3);
            level.setBlock(o.offset(x, 2, -6), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
        }
        // Middle: Glow Panel -> Energy Meter -> Hume Energy Cube, and a Creative Generator -> meter -> cube for a big number.
        level.setBlock(o.offset(-1, 0, -3), panel, 3);
        level.setBlock(o.offset(0, 0, -3), ModBlocks.ENERGY_METER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING, Direction.EAST)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN, Direction.SOUTH), 3);
        level.setBlock(o.offset(1, 0, -3), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        level.setBlock(o.offset(-1, 0, -5), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        level.setBlock(o.offset(0, 0, -5), ModBlocks.ENERGY_METER.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.FACING, Direction.EAST)
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.EnergyMeterBlock.SCREEN, Direction.SOUTH), 3);
        level.setBlock(o.offset(1, 0, -5), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        level.setBlock(o.offset(2, 0, -5), ModBlocks.HUME_ENERGY_CUBE.get().defaultBlockState(), 3);
        // Front left: a chest -> Item Pipes -> a chest, the first side set to Pull (orange plug), the last to Push (green).
        level.setBlock(o.offset(-5, 0, -1), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(o.offset(-5, 0, -1)) instanceof net.minecraft.world.Container chest) chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        level.setBlock(o.offset(-1, 0, -1), Blocks.CHEST.defaultBlockState(), 3);
        var pipeBlock = ModBlocks.ITEM_PIPE.get();
        for (int x = -4; x <= -2; x++) {
            level.setBlock(o.offset(x, 0, -1), pipeBlock.withConnections(pipeBlock.defaultBlockState(), level, o.offset(x, 0, -1)), 3);
        }
        setSide(level, o.offset(-4, 0, -1), Direction.WEST, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PULL);
        setSide(level, o.offset(-2, 0, -1), Direction.EAST, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode.PUSH);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", o.getX() + 0.5, o.getY() + 2, o.getZ() + 3.5));
    }

    private static void setSide(ServerLevel level, BlockPos pos, Direction dir, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.Mode mode) {
        if (!(level.getBlockEntity(pos) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeBlockEntity<?> pipe)) return;
        pipe.setSide(dir, com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide.DEFAULT.withMode(mode));
        var block = (com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock) level.getBlockState(pos).getBlock();
        level.setBlock(pos, block.withConnections(level.getBlockState(pos), level, pos), 3);
    }

    /** The themed sheep (wings, extra heads, legs...) only: {@code -PharnessScene=themed_sheep}. */
    private static final String[] THEMED = {"hay", "snow", "wither", "dragon", "glow_ink", "skeleton", "wither_skeleton", "spider", "warden", "blaze", "ghast"};

    private static void themedSheepTick(Minecraft mc, MinecraftServer server) {
        // 60: stage; 120: overview; then one close-up per sheep every 25 ticks; then the overview again at night.
        int closeStart = 130, perSheep = 25, night = closeStart + THEMED.length * perSheep + 125;
        if (tick == 60) server.execute(() -> themedSheepScene(server));
        else if (tick == 70) setHudHidden(mc, true);
        else if (tick == 120) shot(mc, "52_themed_sheep");
        else if (tick >= closeStart && tick < closeStart + THEMED.length * perSheep) {
            int i = (tick - closeStart) / perSheep, step = (tick - closeStart) % perSheep;
            if (step == 0) {
                // Slightly in front of the sheep's left side, looking back at it.
                // The Ghast sheep is twice the size, so the camera stands further back for it.
                double back = THEMED[i].equals("ghast") ? 6.5 : 3.6;
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 14", themedX(i), origin.getY(), origin.getZ() - 5 + back));
            } else if (step == perSheep - 1) {
                shot(mc, "53_themed_" + THEMED[i]);
            }
        } else if (tick == night - 120 || tick == night - 90) {
            // The Skeleton and Wither Skeleton sheep from the front, to show their skull faces.
            int i = tick == night - 120 ? 4 : 5;
            command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 250 18", themedX(i) - 2.6, origin.getY(), origin.getZ() - 5 + 1.6));
        } else if (tick == night - 92) {
            shot(mc, "53_themed_skeleton_front");
        } else if (tick == night - 62) {
            shot(mc, "53_themed_wither_skeleton_front");
        } else if (tick == night - 60) {
            command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 160 16", origin.getX() + 1.5, origin.getY(), origin.getZ() + 5.8));
        } else if (tick == night - 32) {
            shot(mc, "55_player_skin_sheep");
        } else if (tick == night - 30) {
            // The Wither sheep from the front, to show the three heads side by side.
            command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 250 18", themedX(1) - 2.6, origin.getY(), origin.getZ() - 5 + 1.6));
        } else if (tick == night - 2) {
            shot(mc, "53_themed_wither_front");
        } else if (tick == night) {
            command(server, "time set midnight");
            command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 18", origin.getX() + 0.5, origin.getY() + 2, origin.getZ() + 5.5));
        } else if (tick == night + 50) shot(mc, "54_themed_sheep_night");
        else if (tick == night + 60) mc.stop();
    }

    /**
     * Blue Steel: a few Blocks of it and Blue Steel in an item frame; then JEI's recipes for it and for the
     * Functional Machine Block (which takes it now): {@code -PharnessScene=blue_steel}.
     */
    private static void blueSteelTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode creative @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos o = origin, w = o.offset(0, 0, -3);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-4, 0, -6), o.offset(4, 4, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-4, -1, -6), o.offset(4, -1, 2))) level.setBlock(p, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                for (BlockPos p : new BlockPos[]{w.west(), w, w.east(), w.above()}) level.setBlock(p, ModBlocks.BLUE_STEEL_BLOCK.get().defaultBlockState(), 3);
                level.setBlock(w.north().above(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                level.setBlock(w.east().north().above(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                var frame2 = new net.minecraft.world.entity.decoration.ItemFrame(level, w.east().above(), Direction.SOUTH);
                frame2.setItem(new ItemStack(ModItems.BLUE_STEEL.get()));
                level.addFreshEntity(frame2);
                player.getInventory().setItem(0, new ItemStack(ModItems.BLUE_STEEL.get(), 32));
                player.getInventory().setItem(1, new ItemStack(ModItems.BLUE_STEEL_BLOCK.get(), 8));
                player.getInventory().setSelectedSlot(0);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 20", o.getX() + 0.5, o.getY(), o.getZ() - 0.5));
            });
            case 100 -> { McClient.clearChat(mc); shot(mc, "114_blue_steel"); }
            case 104 -> {
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.show(new ItemStack(ModItems.BLUE_STEEL.get()), true);
                }
            }
            case 112 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 124 -> shot(mc, "114_blue_steel_jei");
            case 126 -> {
                McClient.setScreen(mc, null);
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) {
                    com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.show(new ItemStack(ModItems.FUNCTIONAL_MACHINE_BLOCK.get()), true);
                }
            }
            case 134 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 146 -> shot(mc, "114_blue_steel_machine_block");
            case 150 -> mc.stop();
            default -> {}
        }
    }

    /**
     * A Fleece Trawler on a stone dock at the edge of a 7x7 pool, three Tide sheep swimming in it (one close up, to show
     * its fins and gills), Iron Wool on its lure; then its hauls, and shears cutting a tangle free:
     * {@code -PharnessScene=fleece_trawler}.
     */
    /**
     * Upgrade cards: a Mechanical Separator with a Speed card in, its window open (the upgrade panel on its right, JEI's
     * list beside it), the player holding the cards; then a card's tooltip, and the window of a furnace with none.
     * {@code -PharnessScene=upgrades}.
     */
    private static void upgradesTick(Minecraft mc, MinecraftServer server) {
        BlockPos at = origin == null ? null : origin.offset(0, 0, -3);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode creative @a"}) command(server, c);
                BlockPos o = origin, s = o.offset(0, 0, -3);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-4, 0, -6), o.offset(4, 4, 2))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-4, -1, -6), o.offset(4, -1, 2))) level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                level.setBlock(s, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
                if (level.getBlockEntity(s) instanceof MechanicalSeparatorBlockEntity be) {
                    be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(ResourcefulSheep.id("iron"), 16));
                    be.upgrades().setItem(0, new ItemStack(ModItems.SPEED_UPGRADE.get()));
                }
                level.setBlock(s.north(), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
                level.setBlock(s.east(2), ModBlocks.POWERED_FURNACE.get().defaultBlockState(), 3);
                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(ModItems.SPEED_UPGRADE.get(), 3));
                player.getInventory().setItem(1, new ItemStack(ModItems.EFFICIENCY_UPGRADE.get(), 2));
                player.getInventory().setItem(2, new ItemStack(ModItems.UPGRADE_BASE.get(), 4));
                player.getInventory().setItem(3, new ItemStack(ModItems.CAPACITY_UPGRADE.get(), 2));
                player.getInventory().setItem(4, new ItemStack(ModItems.RANGE_UPGRADE.get(), 2));
                player.getInventory().setItem(5, new ItemStack(ModItems.YIELD_UPGRADE.get(), 2));
                player.getInventory().setItem(6, new ItemStack(ModItems.MUFFLER_UPGRADE.get(), 2));
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", s.getX() + 0.5, s.getY(), s.getZ() + 2.5));
            });
            case 92 -> McClient.clearChat(mc);
            case 98 -> shot(mc, "116_upgrades_jade");
            case 100 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                if (player.level().getBlockEntity(at) instanceof MechanicalSeparatorBlockEntity be) player.openMenu(be, at);
            });
            case 105 -> server.execute(() -> {
                if (server.overworld().getBlockEntity(at) instanceof MechanicalSeparatorBlockEntity be) be.upgrades().setItem(1, new ItemStack(ModItems.MUFFLER_UPGRADE.get()));
                // Two Speed cards in the furnace: Maximum Overdrive for the player standing by.
                if (server.overworld().getBlockEntity(at.east(2)) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity f) {
                    f.upgrades().setItem(0, new ItemStack(ModItems.SPEED_UPGRADE.get()));
                    f.upgrades().setItem(1, new ItemStack(ModItems.SPEED_UPGRADE.get()));
                }
            });
            case 110 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ResourcefulSheep.LOGGER.warn("UPGRADES advancements: Some Assembly Required {}, Maximum Overdrive {}",
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.SOME_ASSEMBLY_REQUIRED),
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(player, com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.MAXIMUM_OVERDRIVE));
            });
            case 120 -> { McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); McClient.clearChat(mc); } // (no player names in the shots)
            case 130 -> ResourcefulSheep.LOGGER.warn("UPGRADES client sees the separator muffled: {}",
                    mc.level.getBlockEntity(at) instanceof com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable u && u.muffled());
            case 135 -> shot(mc, "116_upgrades_gui");
            case 140 -> {
                // Hover the Speed card in the hotbar row of the window for its tooltip.
                if (McClient.screen(mc) instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
                    var window = mc.getWindow();
                    double scale = window.getGuiScale();
                    int left = (window.getGuiScaledWidth() - 176) / 2, top = (window.getGuiScaledHeight() - 193) / 2; // the separator's window
                    for (var slot : screen.getMenu().slots) {
                        if (slot.container == mc.player.getInventory() && slot.getContainerSlot() == 0) {
                            McClient.mouseMove(mc, window.handle(), (left + slot.x + 8) * scale, (top + slot.y + 8) * scale);
                        }
                    }
                }
            }
            case 155 -> shot(mc, "116_upgrade_tooltip");
            case 160 -> {
                McClient.setScreen(mc, null);
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    BlockPos f = at.east(2);
                    if (player.level().getBlockEntity(f) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity be) player.openMenu(be, f);
                });
            }
            case 175 -> { McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); McClient.clearChat(mc); }
            case 190 -> shot(mc, "116_upgrades_furnace");
            case 191 -> {
                McClient.setScreen(mc, null);
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.show(new ItemStack(ModItems.SPEED_UPGRADE.get()), false);
            }
            case 194 -> { McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4); McClient.clearChat(mc); }
            case 199 -> shot(mc, "116_upgrades_jei");
            case 201 -> {
                McClient.setScreen(mc, null);
                server.execute(() -> {
                    // Two Automatic Shears, their areas shown: one plain, one with a Range card (bigger, and wobbling).
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    ServerLevel level = player.level();
                    BlockPos plain = at.offset(-6, 0, 6), ranged = at.offset(1, 0, 6);
                    for (BlockPos p : BlockPos.betweenClosed(at.offset(-9, -1, 4), at.offset(6, -1, 14))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (BlockPos p : new BlockPos[]{plain, ranged}) {
                        level.setBlock(p, ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH), 3);
                        if (level.getBlockEntity(p) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity sh) {
                            if (p == ranged) sh.upgrades().setItem(0, new ItemStack(ModItems.RANGE_UPGRADE.get()));
                            sh.setShowsArea(true);
                        }
                    }
                    level.setBlock(at.offset(5, 0, 3), ModBlocks.CHILLER.get().defaultBlockState(), 3);
                    command(server, "gamemode spectator @a"); // hover above them
                    command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 50", at.getX() - 1.5, at.getY() + 7, at.getZ() + 15.5));
                });
            }
            case 205 -> { setHudHidden(mc, true); McClient.clearChat(mc); }
            case 230 -> shot(mc, "116_range_upgrade_areas");
            case 240 -> shot(mc, "116_range_upgrade_areas_2");
            case 246 -> setHudHidden(mc, false);
            case 247 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 30", at.getX() + 5.5, at.getY(), at.getZ() + 5.5));
            }
            case 250 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos c = at.offset(5, 0, 3);
                if (player.level().getBlockEntity(c) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity be) {
                    be.upgrades().setItem(0, new ItemStack(ModItems.RANGE_UPGRADE.get()));
                    player.openMenu(be, c);
                }
            });
            case 265 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 280 -> shot(mc, "116_upgrades_chiller");
            case 290 -> mc.stop();
            default -> {}
        }
    }

    private static void fleeceTrawlerTick(Minecraft mc, MinecraftServer server) {
        BlockPos dock = origin == null ? null : origin.offset(0, 0, -3);
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamemode spectator @a", "gamerule advance_time false"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos o = origin, d = o.offset(0, 0, -3);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-6, -5, -13), o.offset(6, 5, 3))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-6, -5, -13), o.offset(6, -1, 3))) level.setBlock(p, Blocks.SAND.defaultBlockState(), 2);
                // The pool: 7 wide, 7 out from the dock, 4 deep, its surface a block below the trawler (level with the sand).
                for (BlockPos p : BlockPos.betweenClosed(d.offset(-3, -4, -7), d.offset(3, -1, -1))) level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(d.offset(-4, -1, 0), d.offset(4, -1, 2))) level.setBlock(p, Blocks.STONE_BRICKS.defaultBlockState(), 2);
                level.setBlock(d, ModBlocks.FLEECE_TRAWLER.get().defaultBlockState()
                        .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.FleeceTrawlerBlock.FACING, Direction.NORTH), 3);
                if (level.getBlockEntity(d) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity t) {
                    t.addLure(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("iron"), 4));
                }
                for (int i = 0; i < 3; i++) {
                    Sheep sh = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    sh.snapTo(d.getX() - 1.5 + i * 2, d.getY() - 1.4, d.getZ() - 3.5 + (i == 1 ? -1.5 : 0), 90, 0);
                    SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id("tide")), 9, 0));
                    if (i == 0) sh.setNoAi(true);
                    level.addFreshEntity(sh);
                }
                label(server, d.above(3), "Fleece Trawler", "aqua");
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.1f %.2f 200 32", dock.getX() + 2.5, dock.getY() + 3.5, dock.getZ() + 3.0));
            case 140 -> shot(mc, "115_fleece_trawler");
            case 142 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.1f %.2f -60 18", dock.getX() - 4.2, dock.getY() + 0.3, dock.getZ() - 5.0));
            case 180 -> shot(mc, "115_tide_sheep");
            case 182 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %.1f %.2f 180 25", dock.getX() + 0.5, dock.getY() + 1.6, dock.getZ() + 2.3));
            case 220 -> shot(mc, "115_fleece_trawler_close");
            case 230 -> server.execute(() -> {
                ServerLevel level = server.getPlayerList().getPlayers().getFirst().level();
                if (level.getBlockEntity(dock) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity t) {
                    t.look(level);
                    ResourcefulSheep.LOGGER.warn("TRAWLER {}: pool {}, sheep {}, {}%", t.status(), t.pool(), t.sheepCount(), t.progressPercent());
                    for (int i = 0; i < 6; i++) t.haul(level, level.getRandom(), false, false, i % 2 == 0);
                    StringBuilder caught = new StringBuilder();
                    for (int i = 1; i < t.getContainerSize(); i++) if (!t.getItem(i).isEmpty()) caught.append(' ').append(t.getItem(i));
                    ResourcefulSheep.LOGGER.warn("TRAWLER after 6 hauls ({} total): lure {}, caught{}; Reel Ewe In {}", t.hauls(), t.getItem(0), caught,
                            com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.isDone(server.getPlayerList().getPlayers().getFirst(),
                                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.REEL_EWE_IN));
                }
            });
            case 232 -> {
                command(server, "gamemode creative @a");
                command(server, String.format(java.util.Locale.ROOT, "tp @a %.2f %d %.2f 180 40", dock.getX() + 0.5, dock.getY(), dock.getZ() + 2.2));
            }
            case 236 -> { setHudHidden(mc, false); McClient.clearChat(mc); }
            case 256 -> shot(mc, "115_fleece_trawler_jade");
            case 258 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                if (player.level().getBlockEntity(dock) instanceof com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity t) {
                    player.openMenu(t, dock);
                }
            });
            case 266 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 274 -> shot(mc, "115_fleece_trawler_gui");
            case 276 -> {
                McClient.setScreen(mc, null);
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.showFleeceTrawler();
            }
            case 284 -> McClient.mouseMove(mc, mc.getWindow().handle(), 4, 4);
            case 294 -> shot(mc, "115_fleece_trawler_jei");
            case 300 -> mc.stop();
            default -> {}
        }
    }

    /** Every built-in resource, for {@link #blockWoolTick}. */
    private static final String[] ALL_RESOURCES = {"stone", "coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald", "quartz",
            "netherite", "wood", "hay", "brown_mushroom", "red_mushroom", "snow", "glow_ink", "blaze", "ghast", "spider", "skeleton", "wither_skeleton",
            "warden", "wither", "dragon", "tide"};

    /**
     * One sheep of every built-in resource, side on in five rows of five, each with its name over it, wearing the fleece
     * made of its block; then each row close up: {@code -PharnessScene=block_wool}.
     */
    private static void blockWoolTick(Minecraft mc, MinecraftServer server) {
        switch (tick) {
            case 60 -> server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = player.level();
                origin = player.blockPosition();
                for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamemode spectator @a"}) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
                }
                BlockPos o = origin;
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-14, 0, -26), o.offset(14, 6, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos p : BlockPos.betweenClosed(o.offset(-14, -1, -26), o.offset(14, -1, 4))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int i = 0; i < ALL_RESOURCES.length; i++) {
                    int col = i % 5, row = i / 5;
                    double x = o.getX() - 9.5 + col * 4.5, z = o.getZ() - 4.5 - row * 4.5;
                    Sheep sh = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    sh.snapTo(x, o.getY(), z, 90, 0);
                    sh.setYHeadRot(90);
                    sh.yHeadRotO = 90;
                    sh.setYBodyRot(90);
                    sh.yBodyRotO = 90;
                    sh.setNoAi(true);
                    SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id(ALL_RESOURCES[i])), 9, 0));
                    level.addFreshEntity(sh);
                    label(server, BlockPos.containing(x, o.getY() + 2.2, z), ALL_RESOURCES[i].replace('_', ' '), "white");
                }
            });
            case 70 -> setHudHidden(mc, true);
            case 80 -> command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 40", origin.getX() + 0.5, origin.getY() + 8, origin.getZ() + 0.5));
            case 130 -> shot(mc, "113_block_wool");
            default -> {
                // Each row close up, from in front of it.
                int start = 140, per = 40;
                if (tick >= start && tick < start + 5 * per) {
                    int row = (tick - start) / per, step = (tick - start) % per;
                    if (step == 0) {
                        command(server, String.format(java.util.Locale.ROOT, "tp @a %.1f %.1f %.1f 180 16", origin.getX() + 0.5, origin.getY() + 1.6,
                                origin.getZ() - 4.5 - row * 4.5 + 5.2));
                    } else if (step == per - 1) {
                        shot(mc, "113_block_wool_row_" + (row + 1));
                    }
                }
                if (tick == start + 5 * per + 10) mc.stop();
            }
        }
    }

    private static double themedX(int i) {
        return origin.getX() + (i - THEMED.length / 2) * 2.6 + 0.5;
    }

    /** The ten themed sheep in a row, all facing west so the camera (to the south) sees their left sides. */
    private static void themedSheepScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-16, 0, -10), o.offset(16, 5, 7))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-16, -1, -10), o.offset(16, -1, 7))) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (int i = 0; i < THEMED.length; i++) {
            Sheep sh = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sh.snapTo(themedX(i), o.getY(), o.getZ() - 5 + 0.5, 90, 0);
            sh.setYHeadRot(90);
            sh.yHeadRotO = 90;
            sh.setYBodyRot(90);
            sh.yBodyRotO = 90;
            sh.setNoAi(true);
            SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id(THEMED[i])), 9, 0));
            level.addFreshEntity(sh);
        }
        // Sheep fed Melon Slices, facing the camera at an angle: plain, sheared to show the body, and infused with iron.
        String melon = SheepInfusionEvents.MELON_SKIN_PLAYER.toString();
        String[][] skinned = {{melon, "", "false"}, {melon, "", "true"}, {melon, "iron", "false"}};
        for (int i = 0; i < skinned.length; i++) {
            Sheep sh = Vanilla.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sh.snapTo(o.getX() - 1.5 + i * 2.0, o.getY(), o.getZ() + 2.5, -20, 0);
            sh.setYHeadRot(-20);
            sh.yHeadRotO = -20;
            sh.setYBodyRot(-20);
            sh.yBodyRotO = -20;
            sh.setNoAi(true);
            sh.setSheared(Boolean.parseBoolean(skinned[i][2]));
            var profile = net.minecraft.world.item.component.ResolvableProfile.createUnresolved(java.util.UUID.fromString(skinned[i][0]));
            sh.setData(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.SHEEP_SKIN, com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepSkin.of(profile));
            if (!skinned[i][1].isEmpty()) {
                SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id(skinned[i][1])), 9, 0));
            }
            level.addFreshEntity(sh);
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 18", o.getX() + 0.5, o.getY() + 2, o.getZ() + 9.5));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", McClient.mainTarget(mc), 1,
                msg -> ResourcefulSheep.LOGGER.warn("Harness screenshot {}: {}", name, msg.getString()));
    }
}
