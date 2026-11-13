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
        if ("themed_sheep".equals(System.getProperty("barelyfunctionalcontraptions.screenshotHarness.scene"))) {
            themedSheepTick(mc, server);
            return;
        }
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
                    ResourcefulSheep.LOGGER.warn("Harness JEI tag report: {}", com.jax3.barelyfunctionalcontraptions.sheep.integration.jei.JeiDevHook.tagReport());
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
            case 553 -> {
                // Click the first entry (this mod, sorted A-Z) so the info panel shows its banner.
                var handle = mc.getWindow().handle();
                double scale = mc.getWindow().getGuiScale();
                mc.mouseHandler.onMove(handle, 70 * scale, 45 * scale, 0, 0);
                mc.mouseHandler.onButton(handle, new net.minecraft.client.input.MouseButtonInfo(0, 0), 1);
                mc.mouseHandler.onButton(handle, new net.minecraft.client.input.MouseButtonInfo(0, 0), 0);
                mc.mouseHandler.onMove(handle, 2, 2, 0, 0);
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
                setHudHidden(mc, true);
                server.execute(() -> wirelessScene(server));
            }
            case 860 -> server.execute(() -> linkWireless(server));
            case 866 -> shot(mc, "17_wireless_power");
            case 960 -> server.execute(() -> openWirelessSeparator(server));
            case 990 -> shot(mc, "18_wireless_separator_gui");
            case 995 -> {
                mc.player.closeContainer();
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    if (player.level().getBlockEntity(wirelessBase) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity be) {
                        player.openMenu(be, buf -> buf.writeBlockPos(wirelessBase));
                    }
                });
            }
            case 1005 -> mc.mouseHandler.onMove(mc.getWindow().handle(), 4, 4, 0, 0); // park the cursor in a corner
            case 1015 -> shot(mc, "19_generator_wireless_status");
            case 1020 -> {
                // Hover the wireless button so its tooltip (the list of linked machines) shows.
                var window = mc.getWindow();
                int scale = window.getGuiScale();
                int left = (window.getGuiScaledWidth() - com.jax3.barelyfunctionalcontraptions.mycelial.client.screen.MycelialGeneratorScreen.IMAGE_WIDTH) / 2;
                int top = (window.getGuiScaledHeight() - com.jax3.barelyfunctionalcontraptions.mycelial.client.screen.MycelialGeneratorScreen.IMAGE_HEIGHT) / 2;
                double mx = (left + com.jax3.barelyfunctionalcontraptions.mycelial.client.screen.MycelialGeneratorScreen.WIRELESS_X + 8) * scale;
                double my = (top + com.jax3.barelyfunctionalcontraptions.mycelial.client.screen.MycelialGeneratorScreen.WIRELESS_Y + 8) * scale;
                mc.mouseHandler.onMove(window.handle(), mx, my, 0, 0);
            }
            case 1030 -> shot(mc, "20_generator_wireless_tooltip");
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
                mc.gui.setScreen(null);
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
                level.setBlock(zeroPointBase.above(2), Blocks.STAINED_GLASS.red().defaultBlockState(), 3);
                level.setBlock(zeroPointBase.above(4), Blocks.STAINED_GLASS.yellow().defaultBlockState(), 3);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), String.format(java.util.Locale.ROOT,
                        "tp @a %.1f %d %.1f 180 -12", zeroPointBase.getX() + 0.5, zeroPointBase.getY() + 1, zeroPointBase.getZ() + 8.5));
            });
            case 1670 -> shot(mc, "34_zero_point_coloured_beam");
            case 1672 -> server.execute(() -> {
                // A pig wanders into the beam: it is hurt and set on fire.
                var pig = net.minecraft.world.entity.EntityTypes.PIG.create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
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
                mc.gui.setScreen(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(advancements));
                advancements.setSelectedTab(advancements.get(ResourcefulSheep.id("contraptions/root")), true);
            }
            case 1712 -> mc.gui.toastManager().clear();
            case 1715 -> shot(mc, "36_advancements");
            case 1720 -> {
                mc.gui.setScreen(null);
                server.execute(() -> cableScene(server));
            }
            case 1765 -> shot(mc, "37_energy_cables");
            case 1767 -> {
                setHudHidden(mc, false);
                mc.gui.hud.getChat().clearMessages(false); // the advancement announcements from earlier
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
                mc.gui.setScreen(null);
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
                mc.gui.setScreen(null);
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
        mc.gui.setScreen(new net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen(parent, config.getType(), config,
                net.minecraft.network.chat.Component.translatable("barelyfunctionalcontraptions.configuration.section." + fileName.replace("barelyfunctionalcontraptions-", "barelyfunctionalcontraptions.").replace("-", ".") + ".title")));
    }

    /** Presses the button labelled {@code label} on the open screen (searching nested lists), like a click. */
    private static void pressButton(Minecraft mc, String label) {
        if (mc.gui.screen() == null || !pressIn(mc.gui.screen().children(), label)) {
            ResourcefulSheep.LOGGER.warn("Harness: no button '{}' on {}", label, mc.gui.screen());
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
        mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                mc.player, mc.player.connection.enabledFeatures(), true));
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

    /** The crafted contraptions, in the order the README shows them. */
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
        level.setBlock(base.offset(2, 1, 0), Blocks.STAINED_GLASS.red().defaultBlockState(), 3);
        level.setBlock(base.offset(4, 1, 0), Blocks.STAINED_GLASS_PANE.yellow().defaultBlockState(), 3);
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
                Sheep sh = EntityTypes.SHEEP.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
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

    private static BlockPos wirelessBase;

    /** A running generator on one stone pad, and a separator and Automatic Shears on others a few blocks away. */
    private static void wirelessScene(MinecraftServer server) {
        ServerLevel level = server.overworld();
        wirelessBase = origin.offset(12, 30, -4);
        for (int x = -1; x <= 9; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(wirelessBase.offset(x, -1, z), (x <= 1 || x >= 7 || x == 4) ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.setBlock(wirelessBase, com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks.MYCELIAL_GENERATOR.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(wirelessBase) instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity generator) {
            generator.getInventory().setStackInSlot(com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler.FUEL_SLOT, new ItemStack(Items.RED_MUSHROOM, 32));
            generator.getTank().fillInternal(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 8000);
        }
        level.setBlock(wirelessBase.offset(4, 0, 0), ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(wirelessBase.offset(4, 0, 0)) instanceof MechanicalSeparatorBlockEntity be) {
            // Wool but no FE: everything it runs on comes over the wireless link.
            be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(ResourcefulSheep.id("iron"), 16));
        }
        level.setBlock(wirelessBase.offset(8, 0, 0), ModBlocks.AUTOMATIC_SHEARS.get().defaultBlockState()
                .setValue(com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock.FACING, Direction.SOUTH), 3);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %.1f %d %.1f 180 22", wirelessBase.getX() + 4.5, wirelessBase.getY() + 2, wirelessBase.getZ() + 7.5));
    }

    private static void linkWireless(MinecraftServer server) {
        var dim = server.overworld().dimension();
        var gen = net.minecraft.core.GlobalPos.of(dim, wirelessBase);
        com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, gen, net.minecraft.core.GlobalPos.of(dim, wirelessBase.offset(4, 0, 0)));
        com.jax3.barelyfunctionalcontraptions.mycelial.network.PowerLinking.tryLink(server, gen, net.minecraft.core.GlobalPos.of(dim, wirelessBase.offset(8, 0, 0)));
    }

    private static void openWirelessSeparator(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        BlockPos pos = wirelessBase.offset(4, 0, 0);
        if (player.level().getBlockEntity(pos) instanceof MechanicalSeparatorBlockEntity be) {
            player.openMenu(be, pos);
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

    /** The themed sheep (wings, extra heads, legs...) only: {@code -PharnessScene=themed_sheep}. */
    private static final String[] THEMED = {"hay", "wither", "dragon", "glow_ink", "skeleton", "wither_skeleton", "spider", "warden", "blaze", "ghast"};

    private static void themedSheepTick(Minecraft mc, MinecraftServer server) {
        // 60: stage; 120: overview; then one close-up per sheep every 25 ticks; then the overview again at night.
        int closeStart = 130, perSheep = 25, night = closeStart + THEMED.length * perSheep + 65;
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
            Sheep sh = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
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
            Sheep sh = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
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
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1,
                msg -> ResourcefulSheep.LOGGER.warn("Harness screenshot {}: {}", name, msg.getString()));
    }
}
