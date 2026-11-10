# Barely Functional Contraptions

A NeoForge mod for **Minecraft 26.3** (Java 25) that combines two systems, **Mycelial Power** and **Resourceful Sheep**, into one mod. They share one mod id (`barelyfunctionalcontraptions`), one jar, one creative tab and one config screen, and they are built to work together:

* **Mycelial Power:** the **Mycelial Generator** burns mushrooms and water to make **Forge Energy (FE)**. Generators that touch, or are linked with the **Mycelium Wrench**, form networks whose output grows **exponentially**. Running generators slowly spread **mycelium** and sprout **huge mushrooms**.
* **Resourceful Sheep:** feed sheep blocks, ingots and gems to **infuse** their wool, shear them for **resource wool**, and turn the wool back into resources in the FE-powered **Mechanical Separator**. The **Fluid Reservoir** draws water from an infinite source to keep generators supplied.

![Infused sheep](docs/screenshots/01_world_sheep.png)

| Mycelial Generator | Mechanical Separator | Mushroom sheep |
|---|---|---|
| ![Generator GUI](docs/screenshots/11_generator_gui.png) | ![Separator GUI](docs/screenshots/04_separator_gui.png) | ![Mushroom sheep](docs/screenshots/07_sheep_closeup.png) |

*Real screenshots, captured in-game with `./gradlew runClient -PscreenshotHarness`.*

| | |
|---|---|
| Minecraft | 26.3 |
| NeoForge | 26.3.x (built against 26.3.0.101) |
| Java | 25 |
| JEI (optional) | 31.x for 26.3 (built against 31.17.0.109) |
| Jade (optional) | 26.3.x for NeoForge (built against 26.3.5) |

---

## How the two halves work together

```
 mushrooms ──────► feed a sheep ──► shear ──► Mushroom Wool (×100 shearings)
      ▲                                                                │
      │ mycelium spread                                                ▼
 Mycelial Generator ───────── FE (no cables needed) ──────────► Mechanical Separator
      ▲                                                                │
      └──────────────── Red / Brown Mushrooms (fuel) ◄─────────────────┘
```

1. **Generators power separators directly.** Both use NeoForge's standard energy capability, so a Mycelial Generator placed against any energy face of a Mechanical Separator (back, left, right, top or bottom) pushes FE into it. One generator makes 200–250 FE/t, which is exactly what a separator uses for most wool. FE cables from other mods work too.
2. **Mushroom sheep.** Two new built-in sheep resources, **Red Mushroom** and **Brown Mushroom**, are infused with a **Red/Brown Mushroom** (100 shearings per feeding by default). The separator turns each Mushroom Wool into **2–4 mushrooms** + 2 String, with a 5% chance of a **Mycelium** block. Those mushrooms are the Mycelial Generator's fuel.
3. **The loop closes.** Running generators spread mycelium and grow huge mushrooms, which give you mushrooms for the sheep. The sheep give you mushrooms for the generators, and the generators power the separators that process all your other resource wool.
4. **One mod.** One creative tab (*Barely Functional Contraptions*), one entry in the mod list, one config button listing all four config files, and one JEI and Jade plugin set.

The integration is covered by GameTests, which run in [`IntegrationGameTests`](src/main/java/com/jax3/barelyfunctionalcontraptions/gametest/IntegrationGameTests.java). See [TESTING.md](TESTING.md).

## Installation (players and servers)

1. Install NeoForge 26.3.x for Minecraft 26.3.
2. Put `barelyfunctionalcontraptions-1.0.0.jar` in the `mods/` folder of the client **and** the server.
3. Optional: install JEI 31.x and/or Jade 26.3.x. The mod runs normally without them.

**Machine fronts:** all three machines share one look. Each has a purple-rimmed glass window on its front: a red mushroom in the generator, a wool block in the separator and a pair of shears in the Automatic Shears. The window is dark while the machine is idle and glows with a pulsing halo and sparkles while it runs. The separator and shears also give off a little light while running.

![Machine fronts, idle and running](docs/screenshots/14_machine_fronts.png)

**Placing machines:** the Mycelial Generator, Mechanical Separator and Automatic Shears place with their front facing you. **Hold Shift while placing** to make the front face away from you instead (handy for pointing the shears at a pen from outside it, or the separator's back at a cable).

> **Renamed from "Functional Resources".** The mod ID changed from `functionalresources` to `barelyfunctionalcontraptions`. Blocks, items, wool and infused sheep from worlds made with the old builds don't carry over. Config files are now `config/barelyfunctionalcontraptions-*.toml`, and the command is `/barelyfunctionalcontraptions`. Wool types made with the old generate command live in the old `functionalresources_generated` pack; run `/barelyfunctionalcontraptions generate` again to recreate them.

> Coming from the separate mods? Remove `mycelialpower-*.jar` and `resourcefulsheep-*.jar` first. Everything now lives in the `barelyfunctionalcontraptions` namespace (for example `barelyfunctionalcontraptions:mycelial_generator` and `barelyfunctionalcontraptions:mechanical_separator`), so blocks and items placed with the old standalone mods do not carry over into existing worlds. Data packs that add sheep resources must move from `data/<ns>/resourcefulsheep/sheep_resources/` to `data/<ns>/barelyfunctionalcontraptions/sheep_resources/`. Separator recipes now use the type `barelyfunctionalcontraptions:separating`.

---

## Mycelial Power

### Crafting

```
 I R I      I = Iron Ingot      R = Red Mushroom
 B C B      B = Brown Mushroom  C = Cauldron
 I X I      X = Redstone
```

The generator is mined with any pickaxe and drops itself. Its inventory is dropped when it is broken.

### Running a generator

1. Place it. The front window, with a red mushroom behind the glass, faces you. The mushroom lights up and pulses while the generator runs, and stays lit while it is only paused by a full energy buffer.
2. **Water:** right-click with a water bucket, put a bucket in the bucket slot (the empty bucket comes out below), or pipe water in.
3. **Fuel:** put red or brown mushrooms (or any configured fuel) in the top slot. Hoppers and pipes can insert from any side.
4. Energy is pushed into adjacent cables and machines (any block exposing NeoForge `IEnergyStorage`). Pipes can also pull it out.

| Fuel (default) | Burn time | Energy | FE/t alone | FE per item |
|---|---|---|---|---|
| Brown Mushroom | 400 ticks (20 s) | ×1.0 | 200 | 80,000 |
| Red Mushroom | 600 ticks (30 s) | ×1.25 | 250 | 150,000 |

A generator stops when it runs out of fuel or water. When its buffer is full it pauses and keeps its fuel progress (configurable).

### Networks

Generators connect automatically in all six directions. By default only **running** generators count towards `N`, so a network grows as more members are fed. Networks update instantly when generators are placed, broken, loaded or unloaded. Generators in unloaded chunks simply stop counting, and **no chunk is ever loaded by the mod**. Networks are capped at 64 members by default.

### Mycelium Wrench (wireless links)

```
 . I A      I = Iron Ingot     A = Amethyst Shard
 . X I      X = Redstone Dust  R = Red Mushroom
 R . .
```

* **Right-click** a generator to select it. It gets a purple outline while you hold the wrench, and the wrench glints.
* **Right-click another generator** to link them. Red and white particles run between the two, the action bar confirms the link, and the wrench flashes with the enchantment glint for 2 seconds.
* **Shift + right-click** a generator to remove all of its manual links. Shift + right-click the air to clear the selection.
* Linked generators form **one network**, exactly like touching ones, and every running member counts towards the exponential boost. Touching and linked generators can be mixed freely. Each generator keeps its own mushrooms, water and energy (unless the server enables resource sharing).
* By default, links reach **64 blocks**, each generator can have **8** links, links stay in one dimension, and wrenches never wear out. All of this is configurable in `[network.wrench]`.
* Links are saved with the world. Breaking a generator removes its links. A link to a generator in an unloaded chunk is kept, and only counts while both chunks are loaded; the mod never loads chunks to keep a link working.
* The generator GUI's status line shows how it is connected (*Physical*, *Manual*, *Physical + manual* or *Standalone*) and its link count.

#### Wireless power to machines
The wrench can also power a **Mechanical Separator** or **Automatic Shears** from a generator that isn't touching it.

* **Right-click the generator, then the machine** (or the machine first, then the generator). The same red and white particles run between them.
* The generator sends the machine FE as if it were touching it. This comes out of the generator's normal output (`outputTransferRate` per tick), after any machines it actually touches. When it powers several machines, it shares the energy evenly between them.
* One generator can power up to **8** machines (`max_power_links_per_generator`), within the usual **64 block** limit (`max_link_distance`), in the same dimension. A machine can be powered by several generators.
* Machines don't join the generator's network, so they don't count towards its boost.
* **Shift + right-click a machine** to remove its wireless links. Shift + right-clicking a generator removes all its links, including wireless ones.
* Right-clicking a machine with the wrench selects it instead of opening its GUI.
* Breaking the machine or the generator removes the link. Links are saved with the world, and they only carry power while both blocks are loaded.
* Which machines can be powered is the block tag `#barelyfunctionalcontraptions:wireless_power_receivers`. A data pack can add other mods' FE machines to it.
* **Generator screen:** a small **wireless button** (signal bars) under the *Running* lamp. The bars are purple while linked machines are getting power, yellow when every link is idle, and grey with no links. Next to it, a count shows how many machines are powered out of how many are linked, e.g. *2/2*. **Hover over the button** for the details: the limit, the FE/t being sent, and each machine with its position. Machines in unloaded chunks are marked *(not loaded)*, and links a lowered range limit no longer allows are marked *(inactive)*.

| Linking a generator to two machines | The separator running on wireless power only |
|---|---|
| ![Wireless power](docs/screenshots/17_wireless_power.png) | ![Separator on wireless power](docs/screenshots/18_wireless_separator_gui.png) |

| The wireless button under the lamp | Hovering it shows the machines |
|---|---|
| ![Generator status](docs/screenshots/19_generator_wireless_status.png) | ![Wireless tooltip](docs/screenshots/20_generator_wireless_tooltip.png) |

### Mycelium

Converted blocks at least 3 blocks from the generator have a 5% chance to sprout a **huge red or brown mushroom**, if there's room. It only grows into empty space and never loads chunks.

Every 100 running ticks a generator tries to convert a dirt block with air above it, within 4 blocks, into mycelium. The first attempt has a 1% chance. Each failed attempt adds 0.5 percentage points, so attempt 100 has 50.5% and attempt 199 is guaranteed. After a success the chance resets. Attempts with no valid dirt nearby do not change the chance. Conversions fire a block place event so claim and protection mods can block them, and they respect spawn protection and the world border.

### GUI

* Bars: **energy** (left) and **water tank** (right). The orange box is the remaining burn time of the current mushroom.
* The info panel shows fuel and time left, water and consumption rate, stored, generated and extracted FE, network size, active count, multiplier, total FE/t and status, and the mycelium chance, increase, next-attempt progress, conversions and huge mushrooms grown this session.
* Hover over any bar or line for exact numbers and explanations. The lamp shows the machine state.
* All values are computed on the server and sent to the viewing player.

---

### Jade (optional)
With [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) installed (26.3.x for NeoForge), looking at a Mycelial Generator shows:
- **State** (Running, No fuel, No water, Energy full) and the **FE/t** it is generating.
- The **mushroom burning** and how long it has left, with a fuel progress bar.
- **Network** size, active generators and scaling, and the network's total FE/t when several generators are linked.
- Stored **energy**, the **water tank** and the **fuel slots**. These come from Jade's built-in support for NeoForge's energy, fluid and item capabilities.

![Jade on a Mycelial Generator](docs/screenshots/jade_generator.png)

Each part can be switched off in Jade's settings under *Plugins → Barely Functional Contraptions*. Jade is optional: the mod runs fine without it.

Full configuration reference: [docs/CONFIGURATION-MYCELIAL.md](docs/CONFIGURATION-MYCELIAL.md). Defaults: [docs/barelyfunctionalcontraptions-mycelial-synced.toml](docs/barelyfunctionalcontraptions-mycelial-synced.toml).

Adding a modded fuel takes one line in `config/barelyfunctionalcontraptions-mycelial-synced.toml`:

[fuel]
	fuels = ["minecraft:brown_mushroom;burn=400", "minecraft:red_mushroom;burn=600;energy=1.25", "#c:mushrooms;burn=400"]
```


---

## Resourceful Sheep

### 1. Infuse a sheep
Right-click an **adult** sheep while holding a resource, for example an **Iron Ingot**.

- One item is consumed (this is configurable). You'll see coloured particles and hear a chime.
- The sheep's fleece takes on the resource colour with glinting strands, and **small blocks of the resource grow out of it**: Blocks of Iron on an iron sheep, Blocks of Diamond on a diamond sheep, Red Mushroom Blocks on a red mushroom sheep, Cobblestone on a stone sheep. The cluster shrinks as the infusion is sheared away. The block is the resource's storage block (`<name>_block`, found from the feeding item or the separator output), or the item itself when it is already a block (Oak Log). A data pack can choose another with `growth_item`.
- Its dye colour still shows through slightly, so a blue diamond sheep looks different from a white one.
- **Each infusion lasts 100 shearings by default**, whatever the resource (`infusionUsesOverride` in the config). Set that to `-1` to use each resource's own count instead: iron, gold, copper, redstone, lapis and coal last **9** shearings, and diamond, emerald, netherite scrap, cobblestone, oak log, quartz and the mushrooms last **1** or **3**.
- Feeding an item that isn't a resource does nothing special, so wheat breeding, dyes and so on work as normal.
- Sneak + right-click a sheep with an empty hand to see its infusion and how many shearings it has left.

### 2. Shear it
Shears and dispensers both work. An infused sheep drops **resource wool** (e.g. *Iron Wool*) **instead of** its normal wool. When the infusion runs out the sheep goes back to normal.

### 3. Separate the wool
Put the wool into a **Mechanical Separator** and supply Forge Energy (FE).

| Example | Value |
|---|---|
| Input | 1 Iron Wool |
| Energy | 20,000 FE (200 FE/t) |
| Time | 100 ticks (5 s) |
| Output | **2–4** Raw Iron (random each time) + 2 String (+10% chance of 3 Iron Nuggets) |

**Balance:** every wool separates into **2–4** of its resource (3 on average). With the default of 100 shearings per feeding, one Iron Ingot yields about 300 Raw Iron and one Diamond about 300 Diamonds, so infused sheep **multiply** resources a lot. (With `infusionUsesOverride = -1`, one Iron Ingot gives about 27 Raw Iron and one Diamond about 3 Diamonds.) The cost is the shearing time and the machine's energy. To tone it down, lower `primaryOutputMultiplier` (e.g. `0.34` brings it back to about 1 per wool), lower `infusionUsesOverride` (or set it to `-1`) or `infusionUsesMultiplier`, or set `primary_count` per resource.

### Crafting the separator
```
I H I      I = Iron Ingot     H = Hopper
P S P      P = Piston         S = Shears
C R C      C = Copper Ingot   R = Redstone
```
- Requires a stone pickaxe or better.
- Hardness 3.5, blast resistance 6.
- When broken, it keeps its stored energy and redstone mode.

### Automatic Shears
An FE-powered machine that shears sheep for you and keeps the wool.

![Automatic Shears](docs/screenshots/13_automatic_shears.png)

- **Where it shears:** the **3×3 area in front of its face** (the face with the crossed blades, which points at you when you place it). That's the three block columns 1–3 blocks out, one column either side, from one block below the machine to one block above it. A sheep counts when its feet are in that area.
- **Power:** it runs on Forge Energy and uses **2,000 FE per sheep sheared** (`energyPerShear`). It holds 50,000 FE and accepts up to 1,000 FE/t on every face except the front, from any FE cable or generator. A Mycelial Generator placed against its back powers it directly. Without enough FE it waits, showing *No Energy*.
- **How often:** every second by default (`shearInterval` in `[automatic_shears]`), shearing every ready sheep in the area each time. Once a sheep eats grass and regrows its wool, it gets sheared again.
- **Infused sheep:** these work exactly like hand shears. They drop their resource wool and use up one infusion shearing.
- **GUI:** right-click to open it. It's in the same style as the Mechanical Separator: an energy gauge, a status lamp and line (*Waiting for Sheep*, *Shearing*, *No Energy*, *Storage Full*, *Disabled by Redstone*), the redstone button, a shears icon that snips while it works, a bar counting down to the next sweep, and its 9 storage slots. Hover over things for details.
- **Inventory:** 9 slots. Take the wool out by hand (shift-click works), or put a **hopper underneath** (or any pipe) to pull it out automatically. Nothing can be put *into* it. A comparator reads how full it is.
- **When it's full:** it stops shearing until there is room again, so wool is never thrown away. If one sheep's drops only partly fit, the rest drop on the ground at the sheep.
- **Redstone:** the same four modes as the separator, cycled with the button in the top-right corner: *Ignored* (default), *Active with Signal*, *Active without Signal*, *Disabled*.
- **Which mobs:** only those in the entity tag `#barelyfunctionalcontraptions:auto_shearable`, which is sheep by default. It never turns a mooshroom into a cow or takes a snow golem's pumpkin.
- Its shears never wear out. Mine it with a stone pickaxe or better. When broken it keeps its stored energy and redstone mode, like the separator, and its wool drops on the ground.

```
I S I      I = Iron Ingot
S R S      S = Shears
I C I      R = Redstone Dust   C = Chest
```

| GUI |
|---|
| ![Automatic Shears GUI](docs/screenshots/12_automatic_shears_gui.png) |

### Fluid Reservoir
A water source for your machines. Put it **directly on top of an infinite water source** and it fills its **10-bucket tank** with water.

| In the world | GUI |
|---|---|
| ![Fluid Reservoir](docs/screenshots/15_fluid_reservoir.png) | ![Fluid Reservoir GUI](docs/screenshots/16_fluid_reservoir_gui.png) |

- **Infinite source:** the water block under it must refill itself, by vanilla's rule: it needs **at least two neighbouring water sources** and something solid (or more water) under it. A 2×2 pool, or the middle of a 3-long line, works. A lone source, or two sources side by side, does not. The source is never used up. Set `requireInfiniteSource = false` to allow any water source.
- **Speed:** 1 bucket (1,000 mB) a second by default (`waterPerSecond`), so it fills in 10 seconds. It then stops until water is taken out.
- **Getting water out:**
  - Right-click it with an empty bucket (or any other fluid container) to fill it.
  - Put empty buckets in the top slot of its GUI; it fills them into the bottom slot. Hoppers and pipes can also feed in empty buckets and take water buckets out.
  - Pipes can pull water from any side.
  - It **pushes water into tanks beside it or on top** (up to 1,000 mB/t, `outputPerTick`). A **Mycelial Generator placed next to it never runs dry**.
- Nothing can be poured *into* it. It's a water source, not a storage tank.
- **Windows:** the glass on its sides shows the water level. A comparator reads how full it is.
- **GUI:** right-click with an empty hand. It shows a water gauge with a mark per bucket, the stored amount, the bucket slots, and a status lamp: *Filling*, *Tank Full*, *Source Not Infinite* or *No Water Source*. Hover over things for details.
- Mine it with a stone pickaxe or better. The water in it is lost when it's broken.

```
I G I      I = Iron Ingot     G = Glass (any)
G B G      B = Bucket
I C I      C = Cauldron
```

### Hume Energy Cube
A Forge Energy battery. FE goes **in and out on every side**, and **cubes that touch join into one bigger battery**: every cube adds to the group's buffer and its transfer rate.

| Groups connect into one casing | From below |
|---|---|
| ![Hume Energy Cubes](docs/screenshots/21_energy_cubes.png) | ![Hume Energy Cubes from below](docs/screenshots/23_energy_cubes_below.png) |

- **One cube:** 1,000,000 FE buffer, 10,000 FE/t transfer rate (`capacityPerCube`, `transferPerCube`).
- **A group of N touching cubes** (faces touching, in any shape) has **N x** the buffer and **N x** the transfer rate. A 2x2x2 block of 8 cubes holds 8,000,000 FE and moves 80,000 FE/t. Every cube in the group shares the same energy, so you can feed one end and take power out of the other.
- **Output:** the group **pushes energy into any machine or cable touching it** (`autoOutput`), up to its transfer rate each tick. Machines and cables can also pull from it, and any generator or cable can charge it. It never pushes into other cubes.
- **Breaking a cube** splits the group if it was holding it together. Each cube keeps its own share of the energy, and a broken cube keeps its share in the item (shown in its tooltip), so no energy is lost.
- **Looks like one big block:** touching cubes connect. Their frames only run around the outside of the group, so a 2x2x2 group looks like a single large cube with one big window, and a tower looks like one tall tank. The windows fill with swirling purple energy **from the bottom layer up**, like a tank of liquid: a 2-high stack at 75% shows a full bottom layer and a half-full top layer. The top and bottom join into one plain metal plate (only a cube with nothing beside it, like a lone cube or the top of a one-wide tower, shows the purple bolt port), and the cubes glow brighter as they charge. A comparator reads how full the group is.
- **GUI:** right-click a cube to open it. It's in the same slate style as the other machines:
  - an energy gauge with a mark for every eighth;
  - the stored energy and buffer (*4,020,500 FE of 8,000,000 FE*);
  - the group's cube count and transfer rate (*x8 · 80,000 FE/t*);
  - the energy actually flowing **in** and **out**, averaged over the last second;
  - a status lamp: *Charging*, *Discharging*, *Full*, *Empty* or *Idle*.
  It shows the whole group, whichever cube you open. Hover over things for details.

  ![Hume Energy Cube GUI](docs/screenshots/22_energy_cube_gui.png)
- **Wireless:** a Mycelial Generator can charge a cube from a distance with the Mycelium Wrench, like the other machines.
- Groups are limited to 64 cubes (`maxGroupSize`); extra cubes form another group. Mine it with a stone pickaxe or better.

```
I A I      I = Iron Ingot
A R A      A = Amethyst Shard
I A I      R = Block of Redstone
```

### Creative Generator (testing)
A creative-only block, found in the Resourceful Sheep creative tab. It has no recipe and can't be broken in survival.
- Produces **unlimited Forge Energy**.
- Every tick it pushes FE into all adjacent energy receivers. The rate is `creativeGeneratorOutput` (1,000,000 FE/t by default); receivers still apply their own input limits.
- Cables and machines can also **pull** unlimited FE from any side.
- Place it against any energy face of a Mechanical Separator to test without building a power setup.

### Built-in resources

| Fed item | Wool | Separator output (per wool) | Own uses (`-1`) | FE / time |
|---|---|---|---|---|
| Iron Ingot | Iron Wool | 2–4 Raw Iron | 9 | 20,000 / 5 s |
| Gold Ingot | Gold Wool | 2–4 Raw Gold | 9 | 20,000 / 5 s |
| Copper Ingot | Copper Wool | 2–4 Raw Copper | 9 | 16,000 / 5 s |
| Diamond | Diamond Wool | 2–4 Diamond | 1 | 40,000 / 8 s |
| Emerald | Emerald Wool | 2–4 Emerald | 1 | 40,000 / 8 s |
| Redstone Dust | Redstone Wool | 2–4 Redstone Dust | 9 | 12,000 / 4 s |
| Lapis Lazuli | Lapis Wool | 2–4 Lapis Lazuli | 9 | 12,000 / 4 s |
| Coal | Coal Wool | 2–4 Coal | 9 | 8,000 / 4 s |
| Netherite Scrap | Netherite Wool | 2–4 Netherite Scrap | 1 | 80,000 / 10 s |
| Cobblestone | Stone Wool | 2–4 Cobblestone | 1 | 4,000 / 2 s |
| Oak Log | Wood Wool | 2–4 Oak Log | 1 | 4,000 / 2 s |
| Quartz | Quartz Wool | 2–4 Nether Quartz | 1 | 16,000 / 5 s |
| Red Mushroom | Red Mushroom Wool | 2–4 Red Mushroom (+5% Mycelium) | 3 | 8,000 / 4 s |
| Brown Mushroom | Brown Mushroom Wool | 2–4 Brown Mushroom (+5% Mycelium) | 3 | 8,000 / 4 s |

Every resource also gives 2 String. Plain wool (`#minecraft:wool`) can be separated into 3 string through the bundled `barelyfunctionalcontraptions:separating` recipe.

### JEI: every wool type the separator handles
With JEI installed, click the Mechanical Separator in JEI (or press **U** over it) to open **Mechanical Separation**. It lists **every wool type that's loaded**: built-in, mushroom, command-generated and data-pack ones, sorted by name. Plain wool → string and other explicit recipes come after them. Each wool type is one compact row: wool → resource (cycling through 2–4) + string + bonus chance, with energy and time. Like the Resource Infusion page and the Mycelial Generator's fuel page, it uses the same dark slate style as the machine screens: slate panels, recessed slots, and the separator's purple arrow filling over the real processing time. Hover over things for the details. JEI updates by itself after `/reload` or `/barelyfunctionalcontraptions generate`. Press **U** on any resource wool to jump to its row (**R** shows how to make it: the infusion page).

![JEI Mechanical Separation](docs/screenshots/06_jei_separation.png)

### The Mechanical Separator GUI
- **Energy bar:** shows stored / max FE. Hover for the input rate and the current usage per tick.
- **Progress arrow and spinning gear:** hover for time remaining and the cost of one operation.
- **Status lamp and text:** Idle, Processing %, No Energy, Output Full, Invalid Input or Disabled by Redstone. Hover for an explanation.
- **Redstone button** (top right): cycles Ignored → Active with Signal → Active without Signal → Disabled.
- **Slots:** wool input, resource output, string output and bonus output.

### Automation
- **Items:** by default, hoppers and pipes insert wool through the top, back, left and right faces, and extract outputs from the bottom. Outputs are also auto-pushed into any inventory below the machine. Only items the machine can process are accepted.
- **Energy:** FE is accepted on every face except the front, through NeoForge's `EnergyHandler` capability (`Capabilities.Energy.BLOCK`). Any FE cable or generator works with it.
- **No voiding:** the machine never starts or finishes an operation unless all outputs are guaranteed to fit.
- **No power:** when energy runs out, progress pauses and resumes later.
- **Comparator:** reads how full the input slot is.

---

### Jade (optional)
With [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) installed (26.3.x for NeoForge), looking at things shows:
- **Infused sheep:** the resource (in its colour) and how many shearings are left. A permanent infusion or a resting cooldown is shown instead when that applies.
- **Mechanical Separator:** its status (Processing, No Energy, Output Full...), the FE/t it is using, a non-default redstone mode, and a live progress bar. The energy bar and slot contents come from Jade's built-in support.

Each part can be switched off in Jade's settings under *Plugins → Barely Functional Contraptions*. Jade is optional: the mod runs fine without it.

| Sheep | Mechanical Separator |
|---|---|
| ![Jade sheep](docs/screenshots/09_jade_sheep.png) | ![Jade separator](docs/screenshots/10_jade_separator.png) |

<a id="resourceful-sheep-config"></a>

### Configuration

#### Global config (per world, synced to clients)
`config/barelyfunctionalcontraptions-sheep-synced.toml`, synced to every client. A copy placed in `<world>/syncedconfig/` overrides it for that world only. You can also edit it in-game from *Mods → Barely Functional Contraptions → Config*.

**`[sheep]`**

| Key | Default | Meaning |
|---|---|---|
| `feedingEnabled` | true | Master switch for feeding |
| `feedingAmountOverride` | -1 | Items consumed per feeding (-1 = use each definition's value) |
| `allowBabies` | false | Allow lambs to be infused |
| `allowFeedingShearedSheep` | true | Infuse a sheep that has no wool yet |
| `allowReplacingInfusion` | false | Feed a different resource to replace an infusion |
| `sameResourceRefreshesUses` | true | Re-feeding the same resource tops the uses back up |
| `infusionConsumedByShearing` | true | `false` makes infusions **permanent** |
| `infusionUsesOverride` | 100 | Shearings every infusion lasts, for every wool type. `-1` = each resource's own `infusion_uses` |
| `infusionUsesMultiplier` | 1.0 | Scales every resource's `infusion_uses` |
| `woolDropCountOverride` | -1 | Wool dropped per shearing |
| `shearingBonusChance` / `shearingBonusAmount` | 0.0 / 1 | Chance of extra wool per shearing |
| `preserveWoolColour` | true | Blend the dye colour into infused wool |
| `feedingParticles` | true | Particles and sound when feeding |
| `infusionCooldownTicks` | 0 | Rest time after an infusion runs out |
| `creativeConsumesItems` | false | Whether feeding uses up items in creative |
| `creativeIgnoresCooldown` | true | Creative players bypass the cooldown and the replacement lock |
| `sneakToInspect` | true | Sneak-right-click inspection |

**`[separator]`**

| Key | Default | Meaning |
|---|---|---|
| `energyCapacity` | 100000 | FE buffer |
| `maxEnergyInput` | 1000 | FE accepted per tick |
| `defaultEnergyCost` | 20000 | FE per operation when a resource doesn't set one |
| `energyPerTickOverride` | 0 | Fixed FE/t for every recipe (0 = derive from cost ÷ time) |
| `defaultProcessingTicks` | 100 | Time when a resource doesn't set one |
| `defaultStringOutput` | 2 | String when a resource doesn't set `secondary_count` |
| `primaryOutputMultiplier` | 1.0 | Scales the resource output; fractional parts become a chance |
| `bonusPrimaryChance` | 0.0 | Global chance of +1 resource output |
| `energyCostMultiplier` | 1.0 | Efficiency multiplier for energy |
| `processingSpeedMultiplier` | 1.0 | Speed multiplier (total energy is unchanged) |
| `resetProgressWhenUnpowered` | false | Reset progress instead of pausing it |
| `energyInputSides` | BACK, LEFT, RIGHT, TOP, BOTTOM | Faces that accept FE |
| `itemInputSides` | TOP, BACK, LEFT, RIGHT | Faces that accept wool |
| `itemOutputSides` | BOTTOM | Faces to extract from and auto-push into |
| `autoPushOutputs` / `autoPushInterval` | true / 10 | Automatic output |
| `redstoneControlEnabled` / `defaultRedstoneMode` | true / IGNORED | Redstone behaviour |

**`[automatic_shears]`**

| Key | Default | Meaning |
|---|---|---|
| `shearInterval` | 20 | Ticks between shearing sweeps (20 = once a second) |
| `energyPerShear` | 2000 | FE used per sheep sheared (0 = runs without power) |
| `energyCapacity` | 50000 | FE buffer |
| `maxEnergyInput` | 1000 | FE accepted per tick (every face except the front) |

**`[fluid_reservoir]`**

| Key | Default | Meaning |
|---|---|---|
| `waterPerSecond` | 1000 | mB of water made per second |
| `capacity` | 10000 | Tank size in mB (10 buckets) |
| `requireInfiniteSource` | true | Only work over a self-refilling water source (false = any water source) |
| `outputPerTick` | 1000 | mB pushed into neighbouring tanks (sides and top) per tick; 0 = no pushing |

**`[energy_cube]`**

| Key | Default | Meaning |
|---|---|---|
| `capacityPerCube` | 1000000 | FE each cube adds to its group's buffer |
| `transferPerCube` | 10000 | FE/t each cube adds to its group's transfer rate (in, out and pushing) |
| `maxGroupSize` | 64 | Most cubes in one group; extra cubes form another group |
| `autoOutput` | true | Push energy into touching machines and cables every tick |

Changes to `waterPerSecond` and `capacity` apply to reservoirs that are already placed, straight away (the config screen works in-game). Shrinking the tank drops any water above the new size. The GUI gauge always has 10 marks, so with a different size each mark is a tenth of the tank instead of a bucket.

**`[creative_generator]`**

| Key | Default | Meaning |
|---|---|---|
| `creativeGeneratorOutput` | 1000000 | FE pushed into each adjacent receiver per tick (0 = only allow pulling) |

**`[generator]`** (limits for `/barelyfunctionalcontraptions generate`; see [What the generator may use](#what-the-generator-may-use))

| Key | Default | Meaning |
|---|---|---|
| `allowedCreativeTabs` | `minecraft:building_blocks`, `minecraft:ingredients`, `minecraft:food_and_drinks` | A wool type's resource (its separator output) must be in one of these creative tabs. Empty list = any tab. |
| `requireStackable` | true | Blacklist every item that doesn't stack (max stack size 1), both as a feeding item and as an output |

#### Client config
`config/barelyfunctionalcontraptions-sheep-client.toml`:

| Key | What it controls |
|---|---|
| `ambientParticles` | Particles around infused sheep |
| `dyeTintStrength` | How strongly the dye colour shows through infused wool |
| `showResourceGrowths` | Small blocks of the resource growing out of infused fleece |
| `showSheepSparkleOverlay` | The glinting overlay on infused fleece |
| `detailedWoolTooltips` | Always show separator details in wool tooltips |
| `detailedMachineTooltips` | Extra detail in the separator GUI tooltips |

---

### Resource definitions (data packs)

Definitions live in `data/<namespace>/barelyfunctionalcontraptions/sheep_resources/<name>.json`.
- The resource id is `<namespace>:<name>`.
- They reload with `/reload` and are synced to clients automatically, so JEI and tooltips update too.
- Feeding, shearing, the separator and JEI all read the same definition.

```jsonc
{
  // Optional NeoForge load conditions: skip the file when a mod isn't installed.
  "neoforge:conditions": [ { "type": "neoforge:mod_loaded", "modid": "mekanism" } ],

  "feeding_item": "#c:storage_blocks/tin",   // item id, #tag, or a list
  "feeding_amount": 1,                       // default 1
  "display_name": "Tin",                     // optional text component; default: translation key resource.<ns>.<name>
  "wool_name": "Tin Wool",                   // optional; default "<display_name> Wool"
  "wool_color": "#B8C4CC",                   // "#RRGGBB", integer, or [r, g, b] (0-255)
  "accent_color": "#E6EEF2",                 // colour of the glinting strands
  "category": "ore",                         // ore | gem | dust | material | organic
  "wool_drop_count": 1,                      // wool per shearing
  "infusion_uses": 9,                        // shearings per feeding (used when infusionUsesOverride = -1)
  "separator": {
    "primary_output": "mekanism:raw_tin",    // item id or {"id": ..., "count": ..., "components": {...}}; default: the fed item
    "primary_count": { "min": 2, "max": 4 }, // random 2-4 per wool; a plain number means a fixed count
    "secondary_output": "minecraft:string",  // default string
    "secondary_count": 2,                    // default: config defaultStringOutput (0 = none)
    "bonus_output": { "item": "mekanism:nugget_tin", "chance": 0.1 },
    "energy_cost": 20000,                    // total FE; default: config
    "processing_ticks": 100                  // default: config
  },
  "growth_item": "mekanism:raw_tin"          // optional: block shown growing out of the fleece; default: the storage block of the fed item or output
}
```

**Ores vs other materials.** For an ore, set `primary_output` to the raw material (as the built-ins do with Raw Iron). If you leave `primary_output` out, the separator gives back the fed item, which suits gems, logs and stone. A block input never implies 9 outputs automatically; you choose the numbers.

**Missing items.** A definition that references an item from a mod that isn't installed is skipped. The log gets one warning saying which file and which item; the rest of the reload is unaffected. Empty tags are skipped the same way.

**Sheep keep their infusion even when its wool type is missing.** If a sheep's wool type isn't loaded (its data pack was removed or disabled, or its mod uninstalled), the sheep keeps the infusion stored. Until the wool type comes back it looks and shears like a normal sheep, without using up shearings. You can also feed it a new resource, which replaces the stale infusion.

**Examples.** Ready-made definitions for **Tin, Lead, Silver, Nickel, Uranium, Osmium, Aluminium** and **Amethyst** are in `examples/resource_definitions/`. `examples/datapack/` is the same set packaged as a drop-in data pack, plus an example cobweb separating recipe. Copy the folder into `<world>/datapacks/`.

#### Auto-generating resources for every material
You don't have to write a definition for every metal by hand. In a world with cheats on (operator level 2), run:

| Command | What it does |
|---|---|
| `/barelyfunctionalcontraptions preview` | Lists what would be generated, without changing anything |
| `/barelyfunctionalcontraptions generate` | Creates a resource for every material that doesn't have one yet, saves them into the world data pack `barelyfunctionalcontraptions_generated`, and reloads |
| `/barelyfunctionalcontraptions generate all` | Same, but also regenerates materials that already have a definition |
| `/barelyfunctionalcontraptions generate material <material>` | Adds just one material from the storage-block tags. Press Tab to pick one (`slime`, `tin`, `osmium`...) |
| `/barelyfunctionalcontraptions generate item <item>` | Adds a wool type fed with **any** item you choose, e.g. `minecraft:amethyst_block`. If the item is a storage block, its whole `c:storage_blocks` tag is used. |
| `/barelyfunctionalcontraptions generate hand` | Same as `item`, using the item in your main hand |
| `/barelyfunctionalcontraptions generate remove <name>` | Deletes one generated wool type (press Tab to list them) |
| `/barelyfunctionalcontraptions generate dedupe` | Deletes generated wool types that duplicate another one, then reloads. A duplicate has the same feeding block or the same name as another type, for example a generated Iron next to the built-in Iron after `generate all`. Built-in and hand-written definitions are never removed. Of two generated duplicates, the first by id is kept. |
| `/barelyfunctionalcontraptions list` | Lists every loaded resource |

#### What the generator may use
By default the generator only makes wool types whose resource is something from the **Building Blocks**, **Ingredients** or **Food & Drinks** creative tabs, and it **blacklists every item that doesn't stack** (tools, weapons, armour, buckets, potions, stews and so on). This applies to `generate`, `generate all`, `preview`, `material`, `item` and `hand`, and to tab-completion:
- The **output** (the resource the separator gives back) must be in one of the allowed tabs and must stack. When a material's first choice isn't allowed, the generator tries the next candidate (raw material → ingot → gem → dust → same-named item → the block itself). If none is allowed, the material is skipped.
- The **feeding item** must stack. Members of a storage-block tag that don't stack are left out; the definition then lists the allowed items instead of using the whole tag.
- Modded items count when their mod adds them to one of the allowed tabs.
- `generate item`/`hand` explain why an item is refused, for example *"Diamond Sword doesn't stack…"* or *"Redstone Torch isn't in the Building Blocks, Ingredients, Food & Drinks creative tabs…"*.
- Definitions you already have, and hand-written or built-in ones, are never touched.
- Change the tabs or switch the stack rule off in the `[generator]` section of `config/barelyfunctionalcontraptions-sheep-synced.toml`.

How the generator chooses each value:
- **Materials:** it scans the `c:storage_blocks/<material>` item tags that vanilla, NeoForge and nearly every content mod fill. With Mekanism, Thermal and similar mods installed, that covers tin, lead, osmium, uranium, silver and so on. Raw ore blocks (`raw_*`) and anything already defined are skipped.
- **Feeding item:** the storage-block tag, so every mod's variant of the block works.
- **Output:** the first item in `c:raw_materials/`, `c:ingots/`, `c:gems/` or `c:dusts/<material>`. If none exist, it uses an item named after the material (for example `slime_ball` or `resin_clump`), and failing that, the block itself.
- **Uses:** 9 when a block is broken down into one of those units (a block holds 9), otherwise 1.
- **Colours:** taken from the block's map colour. The category comes from whichever tag supplied the output.

Picking single items or materials adds one file without touching the others. It refuses anything an existing built-in or hand-written resource already covers (same feeding item or same name), so you can't create duplicates this way. A wool type made from an arbitrary item (not a storage block) has `infusion_uses` 1, because the mod can't know how many units the item holds. That only matters with `infusionUsesOverride = -1`; by default every wool type lasts 100 shearings.

Generated files are ordinary JSON in `<world>/datapacks/barelyfunctionalcontraptions_generated/`. You can edit them freely. Re-running `generate` (or `generate all`) rewrites only the wool types it generates itself; other generated ones, such as those added with `generate item`/`hand`/`material`, stay until you delete them with `generate remove` or `generate dedupe`. Sheep infused with them keep their infusion. In plain vanilla this adds Bone Meal, Dried Kelp, Slime and Wheat (Resin is skipped because the Resin Clump is not in an allowed creative tab).

#### Explicit separator recipes
Use the recipe type `barelyfunctionalcontraptions:separating` to process any item, or to override how a particular wool is handled:
```json
{
  "type": "barelyfunctionalcontraptions:separating",
  "ingredient": "#minecraft:wool",
  "primary_output": { "id": "minecraft:string", "count": 3 },
  "secondary_output": "minecraft:white_dye",
  "bonus_output": { "item": "minecraft:string", "chance": 0.1 },
  "energy_cost": 2000,
  "processing_ticks": 40
}
```
An explicit recipe always takes priority over a resource definition. JEI shows only the recipe that actually applies.

#### Tags
| Tag | Default contents | Purpose |
|---|---|---|
| `#barelyfunctionalcontraptions:infusable` (entity type) | `minecraft:sheep` | Which mobs can be infused. Add modded sheep here. |
| `#barelyfunctionalcontraptions:replaceable_wool` (item) | `#minecraft:wool` | Which drops infused wool replaces |
| `#barelyfunctionalcontraptions:auto_shearable` (entity type) | `#barelyfunctionalcontraptions:infusable` | Which mobs the Automatic Shears shear |

Shearing drops are hooked through a global loot modifier on loot tables whose path starts with `shearing/`. That means hand shears, dispensers and modded shears are all covered, along with modded sheep that use their own `shearing/...` table. Infused-fleece rendering is applied to any entity whose renderer extends the vanilla `SheepRenderer`.

#### Custom wool textures
The 12 built-in wools have hand-made textures. Any other resource uses a greyscale wool texture tinted with its `wool_color` and `accent_color`, so it never shows a missing texture.

To give a data-pack resource its own icon, ship a resource pack that overrides `assets/barelyfunctionalcontraptions/items/resource_wool.json` and adds a `case` for your resource id. The file is a `minecraft:select` on the `barelyfunctionalcontraptions:sheep_resource` component.

---


---

## Configuration files

| File | Contents |
|---|---|
| `config/barelyfunctionalcontraptions-mycelial-synced.toml` | Mycelial Generator, networks, wrench, water, fuels and mycelium spread. Synced to clients; a copy in `<world>/syncedconfig/` overrides it per world. |
| `config/barelyfunctionalcontraptions-mycelial-client.toml` | Generator particles and sounds. |
| `config/barelyfunctionalcontraptions-sheep-synced.toml` | Sheep infusion, the Mechanical Separator and the Creative Generator. Synced to clients; a copy in `<world>/syncedconfig/` overrides it per world. |
| `config/barelyfunctionalcontraptions-sheep-client.toml` | Sheep particles, tinting, overlays and tooltips. |

All four can be edited in-game from *Mods → Barely Functional Contraptions → Config*.

## Commands

`/barelyfunctionalcontraptions preview | generate | list` (operator level 2). This is the Resourceful Sheep resource generator described above, which used to be `/resourcefulsheep`.

---

## Building from source

Requires **JDK 25**. Gradle 9 is downloaded by the wrapper.

```bash
./gradlew build                          # compiles, runs unit tests, outputs build/libs/barelyfunctionalcontraptions-1.0.0.jar
./gradlew runClient                      # dev client (with JEI and Jade)
./gradlew runServer                      # dev dedicated server
./gradlew runGameTestServer              # runs every GameTest (both modules + integration) headlessly, then exits
./gradlew test                           # unit tests
./gradlew runClient -PscreenshotHarness  # stages a test world, saves screenshots to run/screenshots/ and quits
```

To launch the dev runs **without JEI**, pass `-Puse_jei_runtime=false`.

The build downloads from `maven.neoforged.net` (NeoForge), Mojang's servers (Minecraft), `maven.blamejared.com` (JEI) and `api.modrinth.com` (Jade).

**IntelliJ IDEA:** *File → Open*, select the project folder, and set the Gradle JVM to a JDK 25 (IntelliJ 2025.2+). ModDevGradle generates the `Client`, `Server` and `GameTestServer` run configurations on sync.

### Regenerating art and data

| Command | What it does |
|---|---|
| `cd tools/mycelial-texture-generator && pip install -r requirements.txt && python generate.py -o ../../src/main/resources` | Mycelial Generator and wrench textures, models and GUI ([README](tools/mycelial-texture-generator/README.md)) |
| `python3 tools/sheep/generate_textures.py` | Sheep wool, separator and creative generator textures |
| `python3 tools/sheep/generate_data.py` | Sheep models, blockstates, built-in resources, recipes, loot and tags. It merges into the shared `en_us.json` instead of replacing it. |

## Project layout

```
src/main/java/com/jax3/barelyfunctionalcontraptions/
├── BarelyFunctionalContraptions.java        the single @Mod entry point; initialises both modules
├── client/BarelyFunctionalContraptionsClient the single client entry point (config screen + both modules' client code)
├── registry/ContraptionsCreativeTab the shared creative tab
├── gametest/IntegrationGameTests   tests for the parts where the modules meet
├── mycelial/                       Mycelial Power module (generator, wrench, networks, mycelium, JEI/Jade)
└── sheep/                          Resourceful Sheep module (infusion, wool, separator, commands, JEI/Jade)

src/main/resources/
├── assets/barelyfunctionalcontraptions/     models, textures, sounds and the merged en_us.json for both modules
└── data/barelyfunctionalcontraptions/
    ├── barelyfunctionalcontraptions/sheep_resources/   built-in sheep resources (incl. red_mushroom, brown_mushroom)
    ├── recipe/, loot_table/, advancement/, loot_modifiers/
    └── structure/, test_environment/, test_instance/   GameTest data
```

The modules keep their own registries, configs and networking. They share the mod id and namespace through `BarelyFunctionalContraptions.MOD_ID`, and each module's `init(...)` is called from the single entry point. History: [docs/PORTING-MYCELIAL.md](docs/PORTING-MYCELIAL.md) describes Mycelial Power's port from 1.21.1 to 26.3.

## License

MIT, see [LICENSE](LICENSE).
