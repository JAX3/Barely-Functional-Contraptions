# Test world

`BFC-Test-World.zip` is a creative superflat world with every contraption in the mod set up and running. It's the
world the screenshot harness (`./gradlew runClient -PscreenshotHarness`) builds and saves, so it matches the
screenshots in [`docs/screenshots`](../docs/screenshots).

## Opening it

1. Unzip it into your Minecraft instance's `saves` folder, so you have `saves/BFC Test World/`.
2. Start Minecraft 26.3 with NeoForge and the mod's 26.3 jar (JEI and Jade optional), and open
   **Barely Functional Contraptions Test World** from the singleplayer list. It's a 26.3 world, so it can't be opened in
   Minecraft 26.2 or 26.1.

Cheats are on, so you can use `/tp` to jump between the scenes below. You start at the south edge of the platform,
facing it.

## What's where

Every scene stands on one square platform of smooth stone, 146 blocks a side, floating at y = 34 above the sand. It's
split into a grid of cells by stone-brick lines, five across and six deep, and the scenes fill it in the order below,
from the north-west corner: west to east, then the next row south. The Themed sheep take two cells; the last three
cells were left free for new contraptions (the Grass Seeder now has the first of them). Each scene keeps its own floor (grass, sculk, andesite and so on), and the ones
without a label of their own have their name floating over their north edge. Hostile mobs don't spawn (the
`spawn_mobs` game rule is off), so nothing gathers in the platform's shadow.

| Scene | Teleport |
|---|---|
| Infused sheep, Mechanical Separators and Automatic Shears | `/tp @s 15 35 47` |
| Fluid Reservoir on its pool | `/tp @s 71 35 43` |
| Hume Energy Cubes: groups, towers, an L, a ring, steps | `/tp @s 43 37 43` |
| Mycelial Generator powering a separator and shears wirelessly (Barely Functional Wrench) | `/tp @s 95 37 45` |
| Creative Generator | `/tp @s 111 37 45` |
| Zero Point Energy Modules (floor and ceiling pair, coloured beam) | `/tp @s 129 36 47` |
| Energy Cables: a generator feeding a cube, a separator and shears | `/tp @s 15 37 68` |
| Fluid Pipes: a reservoir watering two generators | `/tp @s 44 37 68` |
| Item Pipes: shears to separator to chest | `/tp @s 73 37 68` |
| Zero Point modules on walls (sideways beams) | `/tp @s 102 40 71` |
| Bred lambs that kept their parents' infusion | `/tp @s 130 38 67` |
| Fluid Tanks: water, lava, a single tank and an L | `/tp @s 18 39 93` |
| New blocks: Glow Panels and a Glow Lamp, an Energy Meter, Item and Fluid Pipes with Pull and Push sides | `/tp @s 46 38 94.5` |
| Themed sheep: hay, wither, dragon, glow ink, skeleton, wither skeleton, spider, sculk, blaze, ghast | `/tp @s 87.5 37 95.5` |
| Blaze Sheep Furnace with four Blaze sheep, and Automatic Shears, both with their working area shown | `/tp @s 131.5 40 90.5` |
| Wool Burners on wool, string and Coal Wool, and one fed by a hopper | `/tp @s 17.5 37 113.5 180 25` |
| Wool Packers fed by hoppers, a separator working a bale, and placed Wool Bales of every wool | `/tp @s 45.5 37 115.5 180 30` |
| Infusion Altar that has made a Wither sheep (powered by a Creative Generator), and a bred sheep with high stats | `/tp @s 72.5 37 112.5 180 25` |
| Snow Sheep Chiller with three Snow sheep, a hopper and chest under it, and its area shown | `/tp @s 101.5 37 114.5 180 30` |
| Powered Furnaces fed by hoppers, one alone and one sped up by two Blaze sheep (its area shown) | `/tp @s 129.5 37 112.5 180 25` |
| Woollen Armour on armour stands: white, red, light blue and a mixed set | `/tp @s 17.5 35.6 134 180 12` |
| Sheep Treadmills in a fenced pen: a sheep walking one, a sheared sheep another, one empty and one turned east, all fed Hay Bales | `/tp @s 43.5 36.2 134.5 205 28` |
| Ore processing line: six Iron sheep in the Automatic Shears' pen, pipes over the top to a Mechanical Separator, a pipe trench under the row to three Powered Furnaces, a Wool Burner and a chest (filtered to ingots, nuggets and String), powered by a Mycelial Generator on a Fluid Reservoir | `/tp @s 73.5 40 139 180 34` |
| Thermal Reaction Chamber: three Blaze sheep in its hot pen and three Snow sheep in its cold pen (standing still, so it stays balanced), 16 Blaze Rods and Blue Ice to burn, pens outlined, six Fluid Reservoirs on a pool piping in its water, and an Energy Meter into a bank of eight Hume Energy Cubes | `/tp @s 101.5 41.5 140.5 180 32` |
| Echo Chamber on a sculk floor: four Warden sheep standing still in its fenced listening pen (outlined), a repeater clock beside it striking two note blocks for a steady noise in its sweet spot, and an Energy Meter behind its power port into a bank of eight Hume Energy Cubes | `/tp @s 129.5 40.5 143.5 180 28` |
| Static Generator on grass: six sheep standing still at four fresh Copper Combs in its fenced pen (outlined, a gate at the front), making 600 FE/t through an Energy Meter into a bank of eight Hume Energy Cubes | `/tp @s 17.5 40 163.5 180 32` |
| Grass Seeder in a fenced pen of grass eaten into patches of dirt (outlined, a gate at the front), with 64 Bone Meal; four sheep free to graze, and Automatic Shears in the back fence (powered by a Creative Generator) shearing the ones that wander in front of them: the pen runs itself | `/tp @s 73.5 41 163.5 180 38` |
| A row of running machines: two Mycelial Generators (one fuelled and watered), two Mechanical Separators (one fed wool and powered by a Creative Generator) and two Automatic Shears (one charged) | `/tp @s 45.5 38 161 180 25` |

The scenes used to stand in a long row of separate platforms; `-PharnessScene=square_world` moved them all onto the
square platform, with their sheep, labels and the wrench's wireless links. New scenes go into the free cells with
`./gradlew runClient -PscreenshotHarness -PharnessScene=extend_world "-PharnessWorld=BFC Test World"` (unzip this
world into `run/saves` first). This file is updated whenever a new contraption is added.

The Thermal Reaction Chamber is saved with its buffer and cube bank empty, and it only runs while you're near it, so
it reacts for about 45 seconds after you arrive (the meter shows about 13,000 FE/t) and then waits as *Buffer full*.
Take power out of the cubes, with cables to machines, to keep it making power. Even while it's full, its needle and danger bar keep following the herds, so adding or removing a sheep still runs it hot or cold. Its sheep can't wander, so it never runs hot or
cold on its own; to see a meltdown or a freeze, add or remove a sheep yourself (back up the world first).

The Echo Chamber is saved the same way, empty, and only runs while you're near it. Its clock keeps the noise at about
level 6, so after about 10 seconds it starts listening (about 18,000 FE/t, and players nearby get *Good Vibrations*);
its buffer and cube bank fill in about half a minute, and then it waits as *Buffer full*. Walking around near it adds
noise of your own, unless you're wearing Woollen Boots. To see it shriek, put another note block on the clock; to see
its sheep fall asleep, break a repeater.

The Static Generator is saved empty too, and runs while you're near it: 600 FE/t from its six sheep, in bursts as its
dome arcs every 3 seconds. Its combs oxidise a stage for every 20 minutes they're worked, so scrape them with an axe or
wax them with Honeycomb. After about 12 minutes its cube bank and buffer are full, its dome overcharges, and stepping
through the gate into its pen in survival throws you back out (and gets you *Shocking!*). Take power out of the cubes
to keep it charging safely.

When you first open the world, Minecraft warns that it uses experimental settings, because it's a superflat test world.
Choose **I Know What I'm Doing!**. The warning won't show again for this world.
