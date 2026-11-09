# Functional Resources: test report

Environment for the results below: Minecraft 26.3, NeoForge 26.3.0.101, JEI 31.17.0.109, Jade 26.3.5, OpenJDK 25 on Linux. The client ran on Xvfb with Mesa lavapipe (software Vulkan).

## Results for the merged mod

| Check | Command | Result |
|---|---|---|
| Build | `./gradlew build` | ✅ BUILD SUCCESSFUL, produces `build/libs/functionalresources-1.0.0.jar` |
| Unit tests | `./gradlew test` | ✅ 36/36 passed (31 Mycelial Power + 5 Resourceful Sheep) |
| In-world GameTests | `./gradlew runGameTestServer` | ✅ **All 42 required tests passed** on a dedicated-server GameTest server: 15 Mycelial Power, 23 Resourceful Sheep, 3 integration, 1 NeoForge built-in |
| Real client | `./gradlew runClient -PscreenshotHarness` | ✅ Loads with JEI and Jade, with no errors or warnings from the mod in the log. The 11 screenshots are in [`docs/screenshots/`](docs/screenshots) |

### Integration GameTests (`com.jax3.functionalresources.gametest.IntegrationGameTests`)

| Test | Verifies |
|---|---|
| `integration_mycelial_generator_powers_separator` | A Mycelial Generator placed against a Mechanical Separator's energy face, with no cables, powers it. The separator turns Iron Wool into Raw Iron + String on mycelial power alone. |
| `integration_mushroom_block_infuses_sheep` | The built-in `red_mushroom` and `brown_mushroom` sheep resources load. Feeding a Red Mushroom Block infuses a sheep with Red Mushroom and consumes exactly one block. |
| `integration_mushroom_wool_fuels_generator` | The whole loop: Red Mushroom Wool goes through a separator powered by a Mycelial Generator, the 2–4 Red Mushrooms that come out are valid generator fuel, and a second generator fed with them starts running. |

### What the real client run showed
* One mod entry, **Functional Resources 1.0.0**, with one config button that lists all four config files.
* One creative tab that holds the generator, wrench, separator, creative generator and every wool type, including the two mushroom wools.
* A red-mushroom-infused sheep renders red fleece with mushroom blocks growing out of it (`07_sheep_closeup.png`).
* The Mycelial Generator GUI shows **+250 FE/t generated, 200 FE/t out**: it is feeding the separator next to it (`11_generator_gui.png`).
* JEI shows the generator fuel category plus the infusion and separation categories, which now include the mushroom resources. Jade shows sheep and separator info (`09`, `10`).

## Not verified here
* Dedicated multiplayer with two real clients. The networking code is unchanged from the standalone mods, but only the payload ids were renamed.
* Running without JEI (`-Puse_jei_runtime=false`). Both modules load JEI code only through `@JeiPlugin`, as before.
* The manual Mycelium Wrench checklist below, which is interactive.

---

# Mycelial Power module: manual test plan


There are three layers of tests. Run them in this order after any change.

### 1. Unit tests (no Minecraft needed)

```bash
./gradlew test
```

These cover the pure logic: the network formula and its overflow guards, energy rounding, the mycelium chance table, the fuel config parser, number formatting, and the Mycelium Wrench link rules (distance, dimension rules, link limits, durability). That's 31 tests in `src/test/java`.

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

#### Mycelium Wrench
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

> Recorded before the merge, when the mod id was `resourcefulsheep`. Commands are now `/functionalresources …` and the jar is `functionalresources-1.0.0.jar`. The test counts above are the current ones.


Environment:
- Minecraft 26.3 + NeoForge 26.3.0.101, JEI 31.17.0.109
- OpenJDK 25 on Linux
- Client rendering through Mesa lavapipe (software Vulkan) on Xvfb

### Build ✅
- `./gradlew build`: **BUILD SUCCESSFUL**.
  - All dependencies resolve: NeoForge, JEI API, plus JEI and its config library at runtime.
  - Produces `build/libs/functionalresources-1.0.0.jar`.

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
| `generator_creates_valid_definitions` | `/functionalresources generate` logic: discovers slime, wheat and bone meal; skips already-defined iron, diamond and netherite and the raw ore blocks; slime block → 9 slime balls. Every generated JSON parses through the real codec, and writing produces one file per resource plus `pack.mcmeta`. |
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
The harness ran `/functionalresources generate` in a live world:
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
