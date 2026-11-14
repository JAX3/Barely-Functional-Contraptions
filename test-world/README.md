# Test world

`BFC-Test-World.zip` is a creative superflat world with every contraption in the mod set up and running. It's the
world the screenshot harness (`./gradlew runClient -PscreenshotHarness`) builds and saves, so it matches the
screenshots in [`docs/screenshots`](../docs/screenshots).

## Opening it

1. Unzip it into your Minecraft instance's `saves` folder, so you have `saves/BFC Test World/`.
2. Start Minecraft 26.3 with NeoForge and the mod (JEI and Jade optional), and open
   **Barely Functional Contraptions Test World** from the singleplayer list.

Cheats are on, so you can use `/tp` to jump between the scenes below. You start beside the Fluid Tanks.

## What's where

Every scene except the first two stands on its own stone platform floating at y = 34, in a row running east along
z = -9.

| Scene | Teleport |
|---|---|
| Infused sheep, Mechanical Separators and Automatic Shears (on the ground) | `/tp @s -10 4 0` |
| Fluid Reservoir on its pool | `/tp @s -11 26 -5` |
| Hume Energy Cubes: groups, towers, an L, a ring, steps | `/tp @s -19 36 1` |
| Mycelial Generator powering a separator and shears wirelessly (Barely Functional Wrench) | `/tp @s 6 36 -2` |
| Creative Generator | `/tp @s 22 36 -2` |
| Zero Point Energy Modules (floor and ceiling pair, coloured beam) | `/tp @s 38 35 0` |
| Energy Cables: a generator feeding a cube, a separator and shears | `/tp @s 55 36 -2` |
| Fluid Pipes: a reservoir watering two generators | `/tp @s 71 36 -1` |
| Item Pipes: shears to separator to chest | `/tp @s 88 36 -1` |
| Zero Point modules on walls (sideways beams) | `/tp @s 106 39 1` |
| Bred lambs that kept their parents' infusion | `/tp @s 122 37 -3` |
| Fluid Tanks: water, lava, a single tank and an L | `/tp @s 139 38 0` |
| New blocks: Glow Panels and a Glow Lamp, an Energy Meter, Item and Fluid Pipes with Pull and Push sides | `/tp @s 167 38 -0.5` |
| Themed sheep: hay, wither, dragon, glow ink, skeleton, wither skeleton, spider, sculk, blaze, ghast | `/tp @s 202.5 37 0.5` |
| Blaze Sheep Furnace with four Blaze sheep, and Automatic Shears, both with their working area shown | `/tp @s 237.5 40 -3.5` |

The last three scenes were added to the existing world with
`./gradlew runClient -PscreenshotHarness -PharnessScene=extend_world "-PharnessWorld=BFC Test World"` (unzip this
world into `run/saves` first). This file is updated whenever a new contraption is added.

When you first open the world, Minecraft warns that it uses experimental settings, because it's a superflat test world.
Choose **I Know What I'm Doing!**. The warning won't show again for this world.
