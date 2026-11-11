# Barely Functional Contraptions: test report

Environment for the results below: Minecraft 26.3, NeoForge 26.3.0.101, JEI 31.17.0.109, Jade 26.3.5, OpenJDK 25 on Linux. The client ran on Xvfb with Mesa lavapipe (software Vulkan).

## Results for the merged mod

| Check | Command | Result |
|---|---|---|
| Build | `./gradlew build` | ✅ BUILD SUCCESSFUL, produces `build/libs/barelyfunctionalcontraptions-1.5.0.jar` |
| Unit tests | `./gradlew test` | ✅ 36/36 passed (31 Mycelial Power + 5 Resourceful Sheep) |
| In-world GameTests | `./gradlew runGameTestServer` | ✅ **All 77 required tests passed** on a dedicated-server GameTest server: 15 Mycelial Power, 50 Resourceful Sheep, 11 integration, 1 NeoForge built-in |
| Real client | `./gradlew runClient -PscreenshotHarness` | ✅ Loads with JEI and Jade, with no errors or warnings from the mod in the log. The 33 screenshots and 8 crafting-table recipe shots are in [`docs/screenshots/`](docs/screenshots) |

### Integration GameTests (`com.jax3.barelyfunctionalcontraptions.gametest.IntegrationGameTests`)

| Test | Verifies |
|---|---|
| `integration_mycelial_generator_powers_separator` | A Mycelial Generator placed against a Mechanical Separator's energy face, with no cables, powers it. The separator turns Iron Wool into Raw Iron + String on mycelial power alone. |
| `integration_mushroom_infuses_sheep` | The built-in `red_mushroom` and `brown_mushroom` sheep resources load. Feeding a Red Mushroom infuses a sheep with Red Mushroom and consumes exactly one. |
| `integration_mushroom_wool_fuels_generator` | The whole loop: Red Mushroom Wool goes through a separator powered by a Mycelial Generator, the 2–4 Red Mushrooms that come out are valid generator fuel, and a second generator fed with them starts running. |

### Automatic Shears
| Test | Verifies |
|---|---|
| `automatic_shears_shear_sheep_in_front` | With the shears facing south: an iron-infused sheep, a plain sheep and a sheep in the far corner of the 3×3 are all sheared. A sheep 5 blocks out and one a column too far to the side are not. The inventory holds 1 Iron Wool plus the white wool, nothing lands on the ground, the iron sheep has 8 uses left, and a comparator reads a signal. (Checked that this test fails if the area is made one block wider.) |
| `automatic_shears_stop_when_full` | With all 9 slots full it doesn't shear. After one slot is emptied, it shears and stores the wool. |
| `automatic_shears_redstone_modes` | In *Active without Signal* mode a redstone block pauses it (status *Disabled by Redstone*), and removing the redstone block lets it shear. In *Disabled* mode it never shears. |
| `automatic_shears_need_power` | With no FE it doesn't shear (status *No Energy*). The front face takes no FE; the back face does. With exactly one shear's worth (2,000 FE) it shears one of the two sheep, ends with 0 FE, and the second sheep waits until more FE arrives. |
| `integration_mycelial_generator_powers_automatic_shears` | An unpowered Automatic Shears with a running Mycelial Generator against its back shears the sheep in front and stores the wool. |
| `automatic_shears_hopper_output_only_sheep` | A hopper below pulls the wool out. Neither the item capability (for pipes) nor hoppers can insert anything. A mooshroom in the area is not sheared into a cow. |

In the real client (`14_machine_fronts.png`): the generator, separator and shears stand side by side, each idle and running. All three have the same raised glass window. The idle separator shows a dim wool block and the idle shears show dim shears on dark glass. The running separator (wool inside, Creative Generator behind) glows violet, and the powered shears glow pink like the running generator. `automatic_shears_need_power` also checks that the shears' `active` state (which lights the window) is off without FE and turns on once it has enough to shear.

In the real client (`12_automatic_shears_gui.png`, `13_automatic_shears.png`): the block renders with the shears in its front window, and the three sheep in front of it (gold, diamond and plain) are sheared bald. Its slate-style screen shows the energy gauge and readout (24,000 of 50,000 FE: it started with 30,000 and sheared 3 sheep at 2,000 each), the *Waiting for Sheep* status, the redstone button, and the Gold Wool, Diamond Wool and 2 White Wool it collected.

### Hume Energy Cube

| Test | What it checks |
|---|---|
| `energy_cube_single` | Every face has the energy capability. One cube holds 1,000,000 FE, takes 10,000 FE per transfer and gives 10,000 FE out from another side. |
| `energy_cube_group_scales` | Three cubes in a row are one group of 3 with a 3,000,000 FE buffer. They take 30,000 FE in at one end, which can be seen and taken out at the other end at 30,000 FE. A fourth cube placed on top joins (4,000,000 FE). A cube two blocks away stays a group of 1. (Checked that this test fails if the rate does not scale.) |
| `energy_cube_break_splits_group` | With 100k/200k/300k FE in a row of three (600k total), the middle cube's item would keep its 200,000 FE. Breaking it leaves two single cubes with 100,000 and 300,000 FE. |
| `energy_cube_pushes_into_machines` | A charged cube pushes FE into Automatic Shears touching it, and the total is conserved (what left the cubes is exactly what the shears hold). |
| `energy_cube_max_group_size` | With `maxGroupSize = 2`, three cubes form groups of 2 and 1, and the group of 2 moves 20,000 FE/t. |
| `energy_cube_window_shows_charge` | Separate cubes holding 0, 1,000, 250,000, 500,000, 990,000 and 1,000,000 FE show window levels 0, 1, 2, 4, 7 and 8. Any energy shows at least one row, and only a completely full group shows all 8. |
| `energy_cube_gui_values` | Read through a real menu on two cubes: 2 cubes, a 2,000,000 FE buffer, 20,000 FE/t, and the stored amount matching the group. With 5,000 FE put in every tick it shows *In: 5,000 FE/t*, *Out: 0* and *Charging*. With 3,000 FE taken out every tick it shows *In: 0*, *Out: 3,000 FE/t* and *Discharging*. |
| `energy_cube_stack_fills_bottom_up` | In a 2x2x2 group, each cube's connection properties match its neighbours (up and east connected, west not, and so on). At 75% the bottom layer shows level 8 and the top layer 4; at 25% the bottom shows 4 and the top 0. Removing a cube clears the connection on the cube below. |
| `integration_generator_charges_energy_cube` | A running Mycelial Generator charges two cubes, and a separator on their far side finishes iron wool on that power. |

In the real client (`21_energy_cubes.png`, `23_energy_cubes_below.png`): the 2x2x2 group renders as one large cube with a single window and one plain top plate, with no repeating ports. A 3-high tower at 60% shows the energy level running on from the bottom cube into the middle one. A floating 3x2 slab seen from below is one plain plate with its frame only around the outside, so the bottom faces are oriented correctly. The group's GUI (`22_energy_cube_gui.png`) still reads *x8 · 80,000 FE/t*. No missing-model warnings in the log. `24_energy_cube_corners.png`: an L of seven cubes and a 3x3 ring with no middle, seen from above. Every inside corner is a continuous turn of the frame (outline, purple rim, steel) instead of the strips stopping short at the corner cube. The diagonal neighbour this needs isn't in the block state, so a dynamic model part looks it up when the chunk is built; this is checked visually only, since GameTests run without a renderer. `25_energy_cube_folds.png`: a 2x2 base with a wall on its back row, and a three-step staircase. The base's top plate is framed along the foot of the wall, and the wall's window has a frame along its bottom instead of running into the plate. The staircase side is one stepped window with its corners turned, and each step top is framed (and shows the port, since no cube top continues beside it).

### Wireless power with the Barely Functional Wrench

| Test | What it checks |
|---|---|
| `integration_wireless_power_to_separator_and_shears` | A running generator in one corner, linked to a separator and an Automatic Shears that it doesn't touch, powers both: the separator turns iron wool into Raw Iron and the shears shear a sheep. The links work in either order. (The first version of this test caught a bug: the first machine took the whole buffer and the shears got nothing. Energy is now shared evenly.) |
| `integration_wrench_links_machines` | With the wrench item: clicking the separator selects it (handled before its GUI would open), clicking the generator links them, the selection clears, and no generator-network link is made. Shift-clicking the separator removes the link. |
| `integration_wireless_link_rules` | Two machines can't be linked. A machine 8.5 blocks away is *too far* with `max_link_distance = 4`. Duplicate links are refused, and `max_power_links_per_generator = 1` blocks a second machine. Breaking the linked separator removes the link the next time the generator sends power. |

| `integration_generator_status_lists_wireless_links` | The generator's GUI status lists no machines at first, then both linked machines in order, with their block ids and positions, all active, max 8. With `max_link_distance = 4` the far one is reported inactive. The status (now carrying the machine list) round-trips through its network codec unchanged, with nothing left unread. |

In the real client (`19_generator_wireless_status.png`, `20_generator_wireless_tooltip.png`): the generator screen keeps its original size. Under the *Running* lamp is the wireless button with purple signal bars and *2/2* beside it. Hovering it shows *Wireless: 2/2 powered (max 8), 250 FE/t* and lists *Mechanical Separator at 6, 34, -2* and *Automatic Shears at 10, 34, -2*.

In the real client (`17_wireless_power.png`, `18_wireless_separator_gui.png`): a running generator on its own pad is linked to a separator and an Automatic Shears on other pads, and the link particles run to both. The separator started with wool and 0 FE. Five seconds later its screen shows it processing iron wool with 3 Raw Iron already made, all on wireless power.

### Fluid Reservoir

| Test | What it checks |
|---|---|
| `fluid_reservoir_fills_over_infinite_source` | On a 2x2 pool it holds 10,000 mB, has 1,500–2,500 mB after 2 seconds (status *Filling*), fills to exactly 10,000 mB and stops there (status *Full*, windows at fill 4). The water source below is still a source. |
| `fluid_reservoir_needs_infinite_source` | No water is made over a lone source or over two sources in a line (each has only one source neighbour); both report *Source Not Infinite*. One on stone reports *No Water Source*. (Checked that this test fails if one neighbouring source is accepted.) |
| `fluid_reservoir_buckets_and_pipes` | Two empty buckets in the input slot: one becomes a water bucket in the output slot, using 1,000 mB. Dirt is refused. The fluid capability refuses water poured in but lets 500 mB be taken out. Right-clicking with an empty bucket gives a water bucket and takes another 1,000 mB. |
| `fluid_reservoir_config_rate_and_size` | With `waterPerSecond = 4000` and `capacity = 2000`, a placed reservoir makes exactly 1,000 mB in 5 ticks and stops at 2,000 mB (*Full*). Lowering `capacity` to 1,000 shrinks the tank to 1,000 mB. The old values are restored afterwards. |
| `integration_fluid_reservoir_waters_generator` | A reservoir on a 2x2 pool fills a dry Mycelial Generator beside it with at least 2,000 mB of water. |

In the real client (`15_fluid_reservoir.png`, `16_fluid_reservoir_gui.png`): the reservoir on its pool shows water in its side windows next to a running generator, and a second one on dry stone is half full. The GUI shows the water gauge with bucket marks, *8,610 mB of 10,000 mB*, *8.6 buckets*, *+1,000 mB/s*, the *Filling* lamp, and an empty bucket in the top slot already turned into a water bucket in the bottom slot.

### Zero Point Energy Module (`zero_point_modules_generate_across_a_gap`)

A module pointing up on the floor and one pointing down three blocks above it link at distance 3, and report `LINKED`. Their GUI menus receive the link, distance, facing, generation, pair output and the pooled buffer size (both modules' buffers). Their output is 2 x `energyPerBlock` (a 2-block gap), and the two modules' shares add up to it. Both blockstates switch to `linked=true`. Glass in the gap keeps the link. Stone in the gap breaks it: both modules go back to `linked=false` and make nothing, and the floor module reports `BLOCKED` 2 blocks up. Two modules both pointing up don't link (`WRONG_FACING`, and the top one reports `NO_MODULE`), and nor do two touching modules (`TOO_CLOSE`). A Hume Energy Cube beside only the floor module receives the pair's full output: the ceiling module's half is drawn through the floor module, so its own buffer never builds up. In a second pair with nothing attached, the ceiling module's energy capability reports both buffers together, and one extraction from it takes the whole pool. Inserting FE into a module is refused.

In the real client (`29_zero_point_modules.png`, `30_zero_point_jade.png`, `recipe_zero_point_energy_module.png`):
- a floor module and a ceiling module under a stone overhang, 6 blocks apart, are joined by the blue beam, with both emitters lit. Looking up from underneath (`33_zero_point_beam_top.png`), the beam runs right into the ceiling module's emitter: it goes from the floor emitter's tip, half a block up, to the ceiling emitter's tip, half a block into its block, so it is exactly as long as the distance between the modules (it used to stop a block short);
- Jade on the floor module reads *Linked to the module above (5 block gap)* and *+50 FE/t, from either module*, with a 100 kFE pooled buffer that stays nearly empty because the floor module pushes the whole pool into the Hume Energy Cube next to it;
- the recipe makes 2;
- the GUI (`31_zero_point_gui.png`) shows the beam diagram with both modules lit and the beam between them, *of 100,000 FE*, *+50 FE/t*, *Shared pool*, *Out: 50 FE/t* (the full output, through the floor module alone) and a green *Linked: 5 block gap*;
- after a stone block is put in the gap (`32_zero_point_gui_blocked.png`), the GUI shows the blocking block on a dotted line above the module, *+0 FE/t* and a red *Beam blocked 3 above*.

### Block sounds (`all_blocks_use_iron_block_sounds`)

Every block in the mod (the Functional Machine Block, all six machines and the Creative Generator) uses the vanilla Iron Block's sound type, so placing, breaking, stepping on and hitting them sound like an Iron Block. The test checks every registered block in the mod's namespace against `Blocks.IRON_BLOCK`.

### Functional Machine Block (`machines_are_built_from_functional_machine_block`)

The Functional Machine Block has a shaped recipe and is mined with a pickaxe, and each of the six machine recipes (generator, separator, shears, reservoir, energy cube, Zero Point Energy Module) takes exactly one. In the real client (`recipe_functional_machine_block.png` and the other `recipe_*.png` shots) each recipe is laid out in a crafting table and its result slot shows the item, so the recipes load and match.

### Creative tabs (`creative_tabs_split_wool`)

The machines and tools are in the *Barely Functional Contraptions* tab, and every resource wool type is in its own *Resource Wool* tab right after it. The test rebuilds the creative tabs and checks that the main tab has no resource wool but does have the machines, and that the wool tab holds only resource wool, at least every built-in type. In the real client (`28_creative_tab_main.png`, `27_creative_tab_wool.png`) the main tab shows the Functional Machine Block and the eight machines and tools, and the wool tab shows all 15 wool types, including the three made by the harness's generate commands.

### JEI: no tag pages for this mod's own tags

JEI shows every tag as a page in its *Block Tags* / *Item Tags* tabs, so looking up a machine used to show this mod's internal tags, such as `#barelyfunctionalcontraptions:wireless_power_receivers`. The JEI plugin now hides the pages of every tag in this mod's namespace. In the real client the harness logs `own tag pages visible=[]`, and the Mechanical Separator's tag pages are only the vanilla mining tags: `mineable/pickaxe`, `needs_stone_tool`, `incorrect_for_gold_tool` and `incorrect_for_wooden_tool`. Those are shared by every pickaxe block, so they stay; JEI's own `showTagRecipesEnabled` setting turns all tag pages off.

### JEI: Mechanical Separation lists every wool type
In the real client, the harness compares JEI with the loaded wool types: `wool types loaded=15, resource wool in JEI list=15, separation recipes=16 (with resource wool input=15), missing from JEI list=[]`. That's the 14 built-in types plus the Amethyst type the harness generates, and the extra recipe is plain wool → string. The category now uses one compact row per wool type, sorted by name, with explicit recipes last. In the 1280×720 test client that's 4 per page (4 pages) instead of 1 per page (16 pages); see `06_jei_separation.png`.

### JEI pages in the machine style
The Mechanical Separation and Resource Infusion pages draw slate panels with recessed slots, the separator's purple arrow (filling over the processing time) and light text, matching the machine GUIs and the Mycelial Generator's JEI page. Checked in the real client (`05_jei_infusion.png`, `06_jei_separation.png`): no clipped text, and every slot and arrow is aligned.

### Feeding items (your "Infusion recipes" change)
Built-in wool types are fed single items: Iron/Gold/Copper Ingot, Coal, Redstone Dust, Lapis Lazuli and Red/Brown Mushroom. Tests that fed storage blocks now feed those items, and `definitions_loaded` checks that an Iron Ingot maps to iron and an Iron Block no longer does. `tools/sheep/generate_data.py` writes the same feeding items, so regenerating keeps them.

### Shift-placing (`shift_placing_faces_machines_away`)
The Mycelial Generator, Mechanical Separator and Automatic Shears are each placed twice, through the real block-item placement, by a player looking south. Placed normally they face north (towards the player). Placed while sneaking they face south (away). I checked that the test fails if sneaking is ignored. The "Hold Shift while placing…" tooltip line was not checked in the game client.

### 100 shearings per feeding by default (`infusionUsesOverride = 100`)
Feeding an Iron Ingot gives 100 uses, and shearing counts down from there (99, 98…), including through eating grass and the Automatic Shears. `infusion_runs_out_after_uses` switches the override to `-1` while it runs: an Iron Ingot then gives iron's own 9 uses, and a Diamond (1 use) runs out after one shearing.

### Generated wool types keep their sheep (`generated_resource_keeps_infusion`, `unloaded_infusion_is_kept_but_replaceable`)
This runs the real commands in their own test batch, because they reload data packs:
* `generate material slime` adds Slime. A sheep fed a Slime Block is infused with it (9 uses). The infusion survives saving and loading the sheep, a shearing (it drops Slime Wool and has 8 uses left), regrowing, and `/reload`. It shears Slime Wool again after that.
* `generate item minecraft:amethyst_block` adds Amethyst, and a sheep is infused with it. Then a plain `generate` runs. Amethyst is still loaded, the sheep is still infused, and it shears Amethyst Wool. **This failed before the fix:** `generate` deleted every generated file it didn't plan itself, so Amethyst vanished and the sheep lost its infusion at the next shearing. I checked that the test fails with the old behaviour restored.
* A sheep infused with a wool type that isn't loaded drops normal wool, keeps the infusion unchanged, and can still be fed Iron.

### Infusion survives eating grass (`infusion_survives_eating_grass`)
An iron-infused sheep (9 uses) is sheared: it drops Iron Wool and has 8 uses left. Then the sheep's own vanilla grass-eating goal (`EatBlockGoal`) runs on a real grass block: the grass turns to dirt and the wool regrows. The sheep is still infused with iron with 8 uses left. Shearing it again drops Iron Wool (no normal wool), and it has 7 uses left.

### Generator filter (`generator_filter_limits_tabs_and_stacking`)
With the default `[generator]` config: Stone Bricks, Raw Iron and Bread are allowed outputs, while the Redstone Torch (wrong tab) and Mushroom Stew (doesn't stack) are not. Diamond Swords and Water Buckets are refused as feeding items. Every definition from `generate all` passes the filter and parses. In vanilla it keeps 13 materials (iron, gold, copper, coal, diamond, emerald, lapis, redstone, netherite, slime → slime ball, wheat, bone meal, dried kelp) and drops resin. An empty tab list allows any tab but still blacklists non-stacking items.

### What the real client run showed
* One mod entry, **Barely Functional Contraptions 1.5.0**, with one config button that lists all four config files.
* One creative tab that holds the generator, wrench, separator, creative generator and every wool type, including the two mushroom wools.
* Infused sheep grow a cluster of small blocks of their resource in the fleece again (the mooshroom-style flat items were reverted): Blocks of Iron, Gold and Diamond, and Red Mushroom Blocks (`01_world_sheep.png`, `07_sheep_closeup.png`). `infused_sheep_grow_resource_blocks` checks the block each built-in resource grows (Iron Ingot → Block of Iron, Lapis Lazuli → Block of Lapis, Redstone → Block of Redstone rather than the wire, Netherite → Ancient Debris, Cobblestone and Oak Log as themselves), and that no loaded resource grows a flat item when a block form exists.
* The Mycelial Generator GUI shows **+250 FE/t generated, 200 FE/t out**: it is feeding the separator next to it (`11_generator_gui.png`).
* JEI shows the generator fuel category plus the infusion and separation categories, which now include the mushroom resources. Jade shows sheep and separator info (`09`, `10`).

## Not verified here
* Dedicated multiplayer with two real clients. The networking code is unchanged from the standalone mods, but only the payload ids were renamed.
* Running without JEI (`-Puse_jei_runtime=false`). Both modules load JEI code only through `@JeiPlugin`, as before.
* The manual Barely Functional Wrench checklist below, which is interactive.

---

# Mycelial Power module: manual test plan


There are three layers of tests. Run them in this order after any change.

### 1. Unit tests (no Minecraft needed)

```bash
./gradlew test
```

These cover the pure logic: the network formula and its overflow guards, energy rounding, the mycelium chance table, the fuel config parser, number formatting, and the Barely Functional Wrench link rules (distance, dimension rules, link limits, durability). That's 31 tests in `src/test/java`.

### 2. Automated in-game tests (GameTests)

```bash
./gradlew runGameTestServer
```

This starts a headless server, runs every `@GameTest` in `GeneratorGameTests`, then exits. A non-zero exit code means a failure; the log names the failing test. The tests assume the **default server config**.

| GameTest | Checks |
|---|---|
| `singleGeneratorProducesEnergy` | A generator burns one mushroom, uses water and produces 250 FE/t on red mushrooms |
| `adjacentGeneratorsFormOneNetwork` | Touching generators share a network in every direction; a detached one doesn't |
| `threeGeneratorsScaleExponentially` | 3 running generators give 288 FE/t each (864 total) |
| `networkSplitsWhenGeneratorRemoved` | Breaking the middle of a line splits the network |
| `generatorWithoutWaterStops` | No water: state NO_WATER, no FE, the mushroom is kept |
| `myceliumConvertsDirt` | A 100% chance converts dirt with air above into mycelium |
| `convertedBlockSproutsHugeMushroom` | With a 100% chance, dirt 4 blocks away turns into mycelium and grows a huge mushroom (uses the taller `tall_empty` structure) |
| `stateSurvivesSaveAndLoad` | Energy, burn progress, water, chance and inventory survive a save/load |
| `hugeMultiplierDoesNotOverflow` | Extreme values stay within int range |
| `manualLinkJoinsDistantGenerators` | Two generators 5 blocks apart, linked by wrench: one network, 240 FE/t each, connection MANUAL |
| `unlinkingSplitsNetwork` | Removing a generator's links splits a link-only network |
| `destroyedGeneratorLosesLinks` | Breaking a linked generator removes the link from its partner too |
| `duplicateLinksRejectedAndChainsMerge` | Reverse duplicates and self links are refused; A–B plus B–C makes one network of 3 |
| `physicalAndManualConnectionsCombine` | Touching plus linked generators form one network; connection types are PHYSICAL, BOTH and MANUAL |
| `linkDataSurvivesSaveAndLoad` | Saved link data, including a cross-dimension link, survives NBT save and load |

### 3. Manual checks in the dev client

Run `./gradlew runClient` and create a **creative, superflat** world with cheats on. Items are in the **Mycelial Power** creative tab.

#### Generator basics
1. Place a generator, right-click it with a water bucket, and put red mushrooms in the top slot. The lamp turns green and the GUI shows **+250 FE/t**.
2. Connect a Mekanism cable or machine. Energy flows out, and the GUI's *out* value rises to match.
3. Place 3 touching generators, all fed. Each GUI shows **3 linked, 3 active**, **x1.44** and a total of about **864 FE/t** (on brown mushrooms; red ones run 1.25× higher).
4. Fill a generator's energy buffer (no cable attached), then connect a slow consumer. The front window and light stay on steadily, with no flicker, while the lamp switches between green (running) and yellow (energy full). The glow only goes out with no fuel or no water.
5. Hover the orange burn box. You should see one clean tooltip. With JEI installed, clicking it opens the fuel list.

#### Huge mushrooms
1. Set `hugeMushroomChance = 1.0` in the server config (it applies while the world runs).
2. Clear a 9×9 area around a running generator, leaving dirt with air above at least 3 blocks away from it.
3. Each conversion now grows a huge red or brown mushroom, with spore particles. The GUI's last line counts them.
4. Dirt within 2 blocks of the generator turns into mycelium but never sprouts one.
5. Put a stone ceiling 3 blocks above some dirt. Those blocks convert, but no mushroom grows (no room).

#### Barely Functional Wrench
| # | Action | Expected |
|---|---|---|
| W1 | Right-click generator A with the wrench | Action bar: *Selected generator at …*. A has a purple outline while you hold the wrench, and the wrench glints. Its tooltip shows the selected position. |
| W2 | Right-click generator B, **20 blocks** away | Action bar: *Linked …*. Red and white particles run from A to B, with a chime. Both GUIs show *Manual, 1/8 links* and **2 linked**. The selection is cleared, and the wrench keeps glinting for about 2 seconds, then stops. |
| W3 | Feed both A and B | Each produces **240 FE/t** (200 × 1.2): they scale as one network. |
| W4 | Link A to B again | *These generators are already linked*. |
| W5 | Select A, then right-click A again | *That generator is already selected…* |
| W6 | Select A, right-click C **70 blocks** away | *Too far apart (maximum 64 blocks)*. |
| W7 | Place D touching B, then open B's GUI | B: *Physical + manual*. D: *Physical*. Network shows 3 linked. |
| W8 | Shift + right-click A | *Removed 1 manual link(s)*. A shows *Standalone*. B and D stay together. |
| W9 | Link A and B again, then break A | B shows *0/8 links*. Its network drops to the touching generators only. |
| W10 | Link 9 generators to one generator | The 9th link is refused: *…maximum of 8 links*. |
| W11 | Shift + right-click air while a generator is selected | *Selection cleared*. The glint disappears. |
| W12 | Set `wrench_durability = 3` in survival mode | A durability bar appears. After 3 links the wrench breaks with a message. |
| W13 | Set `allow_network_merging = false`. Build two separate pairs of touching generators, then link one generator from each pair | *…merging is disabled*. Linking a single, unconnected generator into a pair still works. |
| W14 | Set `enabled = false` | Wrench clicks show *disabled*. Existing links stop counting; GUIs show the networks split. Set it back to true and they rejoin. |

#### Saving, reloading and unloaded chunks
| # | Action | Expected |
|---|---|---|
| S1 | Link A and B, then save and quit to the title screen, and reload | Links remain: *Manual, 1/8 links*, same network, same FE/t. |
| S2 | Link A to B **300+ blocks** away (raise `max_link_distance` first), then fly far enough from B that its chunk unloads | A shows the link but *0 loaded and active*, and drops back to single-generator output. Nothing loads B's chunk (check with F3: its chunk is not loaded). |
| S3 | Return to B | The network rejoins automatically, with no further clicks. |
| S4 | Stand near B with A's chunk unloaded, and run `/setblock` to replace B with stone | B's links are removed from both ends immediately, without loading A's chunk. When you return to A, it shows 0 links. |

#### Multiplayer
1. Run `./gradlew runServer`, set `online-mode=false` in `run/server.properties`, and connect a client.
2. Repeat W1–W3 and S1. All messages and particles appear for the player who used the wrench, and nearby players see the particles too.
3. Open the same generator GUI on two clients. Both show the same link counts and FE/t.

#### Without JEI
Run `./gradlew runClient -Puse_jei_runtime=false`. The game loads, and the generator and wrench work. Only JEI's recipe pages are missing.

---

# Resourceful Sheep module: report from the standalone mod

> Recorded before the merge, when the mod id was `resourcefulsheep`. Commands are now `/barelyfunctionalcontraptions …` and the jar is `barelyfunctionalcontraptions-1.5.0.jar`. The test counts above are the current ones.


Environment:
- Minecraft 26.3 + NeoForge 26.3.0.101, JEI 31.17.0.109
- OpenJDK 25 on Linux
- Client rendering through Mesa lavapipe (software Vulkan) on Xvfb

### Build ✅
- `./gradlew build`: **BUILD SUCCESSFUL**.
  - All dependencies resolve: NeoForge, JEI API, plus JEI and its config library at runtime.
  - Produces `build/libs/barelyfunctionalcontraptions-1.0.0.jar`.

### Unit tests ✅ (`./gradlew test`): 5/5 passed
Relative side mapping for every facing, side-name parsing, redstone modes, infusion countdown and cooldown, and permanent infusions.

### In-world game tests ✅ (`./gradlew runGameTestServer`): all 24 required tests passed
These run on a **dedicated-server** game test server, which also confirms the mod loads with no client classes present.

| Test | Verifies |
|---|---|
| `definitions_loaded` | All 12 built-in definitions load. Iron Block → iron; Iron Ingot is not a feeding item. Iron separates to 1 Raw Iron + 2 String at 20,000 FE / 100 ticks. |
| `feed_iron_block_infuses_sheep` | Feeding an Iron Block infuses the sheep (9 uses), consumes exactly 1 block and keeps the dye colour. Re-feeding at full uses consumes nothing. |
| `unrecognised_item_is_ignored` | Dirt neither infuses nor is consumed. |
| `shearing_infused_sheep_drops_resource_wool` | Exactly 1 Iron Wool and no white wool. Uses go 9 → 8. A sheared sheep can't be sheared again (no duplication). |
| `killing_infused_sheep_drops_more_than_shearing` | Runs the sheep's real death loot table as a player kill. An iron-infused sheep with its wool drops 2 Iron Wool (one more than shearing), 2–4 Raw Iron, mutton, and no white wool; the infusion is cleared. A sheared infused sheep drops neither Iron Wool nor Raw Iron, so shearing and then killing doesn't pay twice. A plain sheep still drops its white wool. |
| `shearing_plain_sheep_drops_normal_wool` | Uninfused sheep still drop white wool. |
| `infusion_runs_out_after_uses` | A Diamond infusion (1 use) clears after shearing, and the next shearing gives normal wool. |
| `dispenser_shears_infused_sheep` | A powered dispenser with shears drops Gold Wool and no plain wool. |
| `infusion_survives_save_and_load` | The attachment round-trips through entity save/load. |
| `replacing_infusion_is_refused_by_default` | Gold can't overwrite iron, and the refused feeding consumes nothing. |
| `separator_produces_raw_iron_and_string` | Iron Wool → Raw Iron + 2 String, using exactly 20,000 FE. |
| `separator_halts_without_energy` | Progress pauses at 25/100 with 5,000 FE, status is NO_ENERGY and the block goes inactive. It resumes and finishes once powered. |
| `separator_automation_sides` | Top accepts wool and rejects dirt. Bottom is extract-only. Front has no item capability. Energy is accepted on the back but not the front. Insert rate is limited. The machine never outputs energy. |
| `separator_hopper_chain` | Hopper → separator → hopper → chest delivers 2 Coal + 4 String. |
| `separator_never_overfills_output` | With 63 string already in the slot, the machine reports OUTPUT_FULL and uses no energy or input. |
| `separator_state_survives_save_and_load` | Energy, progress and inventory round-trip through block entity save/load. |
| `separating_recipe_handles_plain_wool` | The custom recipe type matches `#minecraft:wool` → 3 String. Dirt is not separable. |
| `config_multipliers_affect_values` | Speed ×2 → 50 ticks and energy ×0.5 → 10,000 FE. Restored afterwards. |
| `creative_generator_powers_separator` | Generator placed behind a separator pushes FE in, and the separator processes Iron Wool on that power alone. Any face allows unlimited extraction (5,000,000 FE pulled in one call). |
| `generator_creates_valid_definitions` | `/barelyfunctionalcontraptions generate` logic: discovers slime, wheat and bone meal; skips already-defined iron, diamond and netherite and the raw ore blocks; slime block → 9 slime balls. Every generated JSON parses through the real codec, and writing produces one file per resource plus `pack.mcmeta`. |
| `generator_dedupe_removes_duplicate_wool` | After `generate all`: iron, gold, copper, redstone, lapis, coal, diamond, emerald and netherite are flagged as duplicates. Slime, wheat and bone meal are kept, and built-ins are never flagged. Only the duplicate files are deleted. |
| `generator_pick_and_choose` | Material suggestions list slime but not raw ore blocks. An unknown material is rejected. `material slime` uses the storage tag ×9. `item amethyst_block` is fed directly, gives amethyst shard ×1 and parses. `item iron_block` uses the iron tag and is reported as already covered by the built-in. `writeOne` keeps the existing files. |
| `separator_output_rolls_two_to_four` | Every resource's separator output is labelled 2-4 and the machine reserves room for 4. 1,000 rolls per resource stay within 2-4 and produce all of 2, 3 and 4. A plain number still means a fixed count, and min > max is rejected. |
| `custom_definition_with_tag_parses` | A definition using a `#c:storage_blocks/iron` tag, a display name, hex and `[r,g,b]` colours and a custom separator block parses and behaves correctly. Missing items are reported as errors. |

### Real client run ✅ (`./gradlew runClient -PscreenshotHarness`)
The harness creates a world, stages the scene and saves screenshots to `run/screenshots/`. It confirmed:
- Infused sheep render resource-coloured fleece with accent strands on the vanilla model, including dye blending (light-blue + diamond). Plain sheep are unchanged.
- All 12 wool item icons and the separator block/item render with no missing-texture pattern. The log has no model or texture warnings.
- The separator GUI works: energy bar and text, progress arrow, status line, redstone button, slots, and outputs including a bonus roll.
- JEI shows both categories (12 infusion and 13 separation recipes) with catalysts, energy and time.
- Client definitions are synced from the integrated server (12 received).

#### Generator, end to end in the real client
The harness ran `/barelyfunctionalcontraptions generate` in a live world:
- The command wrote 5 definitions and reloaded automatically.
- The server went from 12 to 17 loaded definitions, and all 17 synced to the client.
- JEI showed 17 infusion recipes, including the generated Bone Meal resource with auto-tinted wool.

In a live world, the pick-and-choose commands ran in sequence:
- `generate material slime` added Slime.
- `generate item minecraft:amethyst_block` added Amethyst.
- `iron_block` and `emerald_block` were refused as already covered.
- `generate hand` (holding a Honey Block) added Honey.
- `generate remove slime` removed Slime.

Each step reloaded live: 12 → 13 → 14 → 15 → 14 definitions, all synced to the client.

Earlier, also in a live world, `generate all` followed by `generate dedupe` went from 26 wool types back to 17. It removed the 9 copies of built-ins and listed the reason for each.

This run also caught and fixed a bug: tag-based definitions crashed a live `/reload`, because the empty-tag check ran before Minecraft had bound the new tags.

#### Jade integration (real client + dedicated server)
- Jade 26.3.5 loads the Resourceful Sheep plugin on both the game-test server and the client, and all 24 game tests still pass with Jade installed.
- In the client, looking at a diamond-infused sheep showed "Infused: Diamond" and "9 shearing(s) left".
- Looking at a running separator showed "Status: Processing", "Usage: 200 FE/t", its contents, Jade's energy bar and a progress bar.
- The first client run failed: Jade 26.3 rejects one provider class that does both server data and tooltip. Splitting it into separate server and client providers fixed it. The dedicated-server test alone would not have caught this.

### Not tested / known limitations
- **Real multiplayer** with two separate machines was not run. Instead:
  - Every sync path ran in the integrated server, including definitions, recipes, attachment sync and menu data.
  - The game-test server confirmed dedicated-server loading.
- **Third-party energy cables and item pipes** (Mekanism, Thermal, etc.) were not installed. Compatibility is through the standard NeoForge capabilities, which the tests exercise directly and through vanilla hoppers.
- **Modded sheep:** the infusion visuals only apply to renderers that extend the vanilla `SheepRenderer`. Shearing support requires the mod to use a `shearing/...` loot table.
- **Tag-based example definitions** (`examples/`) for tin, lead and others are guarded by `mod_loaded` and `tag_empty` conditions. With the target mods absent they are skipped, as intended. Parsing with a real tag is covered by `custom_definition_with_tag_parses`.
- **Rendering backend:** screenshots came from software Vulkan, because Xvfb offered no matching GLX visual for OpenGL. The textures and models are backend-independent.
- **Sounds:** sound playback wasn't verified, because the container has no audio device.
