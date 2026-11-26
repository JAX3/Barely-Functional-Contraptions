# Changelog

All numbers are defaults; almost all can be changed in the synced config. Every new machine has a JEI page, Jade
lines and a chapter in the Contraptions Manual.

## 1.5.7 (20–26 November 2026)

For Minecraft 26.3, 26.2 and 26.1 on NeoForge. Crate storage, a fishing machine, upgrade cards, three generators, a
machine status screen, two new themed sheep and the machine that uses one of them, a wireless-charging sheep, and a
purple Echo Chamber.

### Storage

**Wool-Lined Crate.** Holds lots of one item and shows it on its front, like a drawer. No power.
- Holds 32 stacks of one item (2,048 of most things). Right-click to put items in (twice quickly for all you carry),
  left-click for a stack, sneak-left-click for one.
- **Lock** it to its item: sneak-right-click with an empty hand.
- Its wool lining takes the **colour of what it holds**, worked out from the item's texture (modded items too).
  Sneak-right-click with dyed wool to pick your own.
- **Pack** it with Wool Bales (sneak-right-click): +32 stacks each, up to 4.
- **Bursting:** past 90% its lid bulges; pushing more into a full crate has a 2% chance to burst it (8 items fly out,
  the lock breaks). A **Crate Strap** stops that.
- Pipes and hoppers on every side; comparator reads how full. Breaking it keeps everything inside.
- Advancement *Bursting at the Seams*. Config `[crate]`.
- Recipes: planks, wool, Iron Nuggets and a Chest; the strap is three Iron Ingots over an Iron Nugget.

**Crate Controller.** Links crates into one store. No power.
- Touching crates join on their own; link others with the wrench. Up to 64 crates, 200 blocks away.
- Pipes and hoppers on it reach every crate: items go to a crate already holding them, then an empty one.
- Its front and a comparator show how full the store is. On the Machine Status Panel as one line.
- Advancement *Dewey Decimal Sheep* (16 crates in one store). Config `[crate_controller]`.
- Recipe: a Wool-Lined Crate, a Hopper, two Comparators, two Iron Ingots and a Functional Machine Block.

### Production

**Fleece Trawler and the Tide sheep.** A dockside winch that fishes the pool in front of it, driven by Tide sheep
swimming in it. No power.
- **Tide sheep:** infused with Prismarine Crystals (2 Prismarine, a Nautilus Shell). Fin and gills; breathes
  underwater. Wool gives Prismarine Shards and Crystals. Advancement *Making Waves*.
- **Pool:** the 7x7 in front of it, up to 4 deep; at least 18 water, faster up to a full 196.
- **Speed:** a haul every 30 s with one sheep in a full pool, a quarter of that with four; ocean, river and rain each
  make it a quarter faster again.
- **Hauls** are vanilla fishing loot. Put **resource wool** in as a lure for a 30% chance of 1–2 of that resource
  (Raw Iron on Iron Wool); one wool lasts 8 resource hauls.
- **Barely functional:** its net tangles now and then (4%) until you cut it free with shears; very rarely it drags up
  a sheep.
- Window with lure, sheep, pool, winch and 9 catch slots. Hoppers and pipes; comparator.
- Advancement *Reel Ewe In*. Config `[fleece_trawler]`.
- Recipe: Blue Steel, a Fishing Rod, two Tide Wool Bales, a Functional Machine Block and a Hopper.

**Explosive sheep.** Infused with **TNT** (2 Gunpowder, a Fire Charge).
- A TNT fleece (the "TNT" band down each flank), TNT blocks on its back and a fuse.
- Fire, lava, lightning, flint and steel or a Fire Charge light the fuse: it hisses, then **pops**, breaking nothing
  and hurting no one, and blows its fleece off as Explosive Wool (like shearing).
- Regrows its wool on its own in about **20 seconds**. Wool gives Gunpowder (2–4), sometimes TNT.
- Advancement *Short Fuse*.

**Implosion Chamber.** Cracks **Cobblestone into Gravel, and Gravel into Sand**, with the blasts of the Explosive
sheep in the 5x5 round it.
- Every 5 s, with stone to crack, room and 1,000 FE, it lights the nearest ready sheep's fuse.
- Each blast cracks up to 8 items a step, so Cobblestone takes two blasts to become Sand. A Gravel button in its
  window makes it stop at Gravel.
- Input and output for hoppers and pipes; power on every side; comparator reads its output.
- Window: slots in a row, a fuse to the next spark, its sheep (ready and regrowing), blasts and status. On the
  Machine Status Panel. Config `[implosion_chamber]`.
- Recipe:
  ```
  I O I      I = Blue Steel, O = Obsidian
  W M W      W = Explosive Wool, M = Functional Machine Block
  I P I      P = Piston
  ```

### Power

**Static Generator (mid game).** Two blocks tall. Sheep rubbing against **Copper Combs** in its 5x5 pen build static;
its dome arcs it into power.
- 100 FE/t a sheep at a comb (Copper 150, Redstone 125), up to six: 600 FE/t.
- 500,000 FE buffer, pushes 2,000 FE/t from its base.
- Up to 4 combs. They oxidise as they work (100% → 25%); scrape with an axe, wax with Honeycomb.
- Left full, it's **Overcharged** and shocks things in its pen. Woollen Armour insulates.
- Advancement *Shocking!*. Config `[static_generator]`.
- Recipes: a Lightning Rod, two Copper Blocks, Redstone, two wool and a Functional Machine Block; a Copper Comb is
  three Copper Wool over a Brush.

**Hume Field Generator (late game).** 2x2 and three tall, in the Echo Chamber's casing: a window on each side onto its
flickering purple core, a power port under each, an emitter on the lid.
- Every second it rolls a **random 200–1,200 FE/t** and makes that each tick. It has **no buffer**: power goes
  straight into whatever touches it, and what they can't take is lost.
- Pulses with purple sparks every 5 s (just for show).
- Window: a field gauge, what was taken and lost, a trace of its recent rolls, its average and a status lamp
  (*Delivering*, *Wasting power*, *Nothing to take it*). On the Machine Status Panel.
- Config `[hume_field_generator]`: `minPerTick`, `maxPerTick`, `ticksPerRoll`, `pulseInterval`.
- Recipe: Blocks of Blue Steel in the corners, Tinted Glass on top, two Hume Energy Cubes beside a Nether Star, a
  Functional Machine Block below.

**Energy sheep, a wireless charger.** Infused with a **Hume Energy Cube** (2 Lightning Rods).
- A Hume Energy Cube fleece, crackling with sparks while charged.
- Drains up to 2,000 FE/t from Hume Energy Cubes within **3 blocks** into its 100,000 FE fleece.
- Shares up to 2,000 FE/t out, wirelessly, to every machine (not cubes or cables) and every powered item in nearby
  players' inventories within **8 blocks**. Sparks arc to what it charges.
- Only works with its fleece on. Jade shows its charge. Wool gives Redstone and Copper.
- Advancement *Static Cling*. Config `[energy_sheep]`.

### Utility

**Machine Status Panel.** A wall screen showing how your machines are doing. No power.
- Link machines with the wrench (panel, machines, panel), up to 200 blocks away.
- Two lines a machine: name and lamp, status and a key number. Green working, amber waiting, red problem, grey idle.
  Text sizes itself to fit.
- Panels join into one screen up to **3x3** (36 machines).
- Comparator counts machines with a problem, for an alarm.
- Window: rename, reorder, unlink, choose alarms, sort, colours.
- Advancement *Mission Control* (8 machines). Config `[status_panel]`.
- Recipe: four Glass Panes, two Redstone, a Functional Machine Block, a Comparator and an Iron Ingot.

**Upgrade cards.** Two upgrade slots in the Mechanical Separator, Wool Packer, Powered Furnace, Infusion Altar,
Automatic Shears and Snow Sheep Chiller. Make blank cards from Blue Steel, Redstone and wool.

| Card | Crafted with | Effect |
|---|---|---|
| Speed | Block of Redstone, Sugar, Red Wool | 60% of the time, 150% of the power |
| Efficiency | Block of Copper, Lime Wool | 75% of the power, 115% of the time |
| Capacity | Wool Bale, Light Blue Wool | 4x the energy buffer and input |
| Range | Ender Pearl, Purple Wool | +1 block of area each side (shears, altar, chiller) |
| Yield | Diamond, Yellow Wool | +1 of each separation output (separator) |
| Muffler | Woollen Helmet, Black Wool | Silences the machine |

- A second card of the same kind does half as much; different kinds multiply. Cards stay with a broken machine.
- Advancements *Some Assembly Required* and the hidden *Maximum Overdrive*. Config `[upgrades]`; block tags
  `#barelyfunctionalcontraptions:accepts_upgrade/<card>`.

### Changes

- **Blue Steel:** an Iron Ingot and any wool. Every recipe that took an Iron Ingot now takes Blue Steel, and a Block of
  Blue Steel where it took a Block of Iron (the Infusion Altar still takes a real Iron Ingot for an Iron sheep).
- **Copper Wool for Copper Ingots:** the Mechanical Separator, Automatic Shears, Powered Furnace, Sheep Treadmill,
  Fluid and Item Pipes and Copper Comb take Copper Wool; the Mycelial Generator and Zero Point Energy Module take Copper
  Wool Bales. The Functional Machine Block, Energy Cable, Wool Burner and Infusion Altar keep real ingots, so a new
  world can reach its first Copper sheep.
- **Echo Chamber:** recoloured to the machines' blue steel and purple (dish, fronds, roof, dome, foam and its window).
  New recipe: Blocks of Blue Steel in the corners, Sculk Wool Bales round an Echo Shard, a Functional Machine Block
  below.
- **Sheep wear their blocks:** an infused sheep's fleece is now its resource's block (Block of Redstone, oak bark...),
  made from the block's own texture. Themed sheep keep their own looks. Client config `blockTexturedWool`.
- **Processing machines about half as fast again** (same total FE): Separator 2.5 s for Iron Wool, Packer 1 s, Altar
  2.5 s, Powered Furnace 0.35 of a furnace's time, Chiller 5 s an Ice, Shears every 0.5 s. Old default config values
  update themselves.
- **Command is now `/bfc`** (was `/barelyfunctionalcontraptions`): `preview`, `generate` (`all`, `material`, `item`,
  `hand`, `remove`, `dedupe`) and `list`. Update any command blocks or scripts.
- **Removed wireless power links:** the wrench no longer powers machines from a Mycelial Generator; use Energy Cables,
  or an Energy sheep. Old links are ignored.
- **Under the hood:** every machine is built on one shared machine frame with modules (energy, fluid, upgrades,
  inventory). Nothing changes in game; existing worlds load as before. See `docs/MACHINE-FRAME.md`.

### Fixes

- An Item Pipe on a Mechanical Separator's top can take its outputs (config `pipesTakeOutputsFromAnySide`).
- The Static Generator's charge bar no longer flashes while overcharged.
- Machine Status Panel: long names fit on its screen and in its window; a Static Generator's dome links; its range is
  200 (old configs keep their `range`); a Chiller out of water reads *No water*.

### Test world

- A seventh row of cells; you start at its south edge.
- New: Wool-Lined Crates and a Crate Controller, the Fleece Trawler, upgrade cards side by side, three Hume Field
  Generators (delivering, wasting, alone) with an Energy sheep powering a furnace from their cubes, the Implosion
  Chamber with its Explosive sheep, and a Static Generator.
- The Machine Status Panel shows 27 machines; the old wireless scene is cabled.
- Fixed: rebuilding the world no longer wipes the seventh row's floor (its chests, hoppers and grass).

## 1.5.5 (17–19 November 2026)

### Minecraft 26.2 and 26.1 support
- Now also runs on **Minecraft 26.2** and **26.1.2**, with every feature: download the jar for your version
  (`-mc26.3`, `-mc26.2`, `-mc26.1`). On those, the synced config is per world, in `<world>/serverconfig/`.
- JEI 30.x / Jade 26.2.x on 26.2; JEI 29.x / Jade 26.1.x on 26.1.

### New machines

**Herd Counter (utility).** Counts the sheep in the 7x7 in front of it onto a comparator (1 a sheep). No power.
- Modes: every sheep, ready to shear, sheared, lambs, low infusion, or Full (15 at its target). A resource filter.
- Now and then it counts a passing sheep twice; **Steady** averages over 5 s.
- Advancement *Count Your Sheep*. Config `[herd_counter]`.
- Recipe: two Iron Ingots, a Glass Pane, an Observer, a Functional Machine Block and a Comparator.

**Grass Seeder (production).** Keeps a 5x5 pen green: turns eaten dirt back to grass and grows short grass. Sheared
sheep there graze 4x as often, so they regrow sooner.
- Fuel: Bone Meal (8 blocks each), or 800 FE and 10 mB of water a block.
- With nothing to do its grass overgrows (sheep can't eat it, shears can't see them); **Trim** stops that.
- Advancement *Keep Off the Grass*. Config `[grass_seeder]`.
- Recipe: Wheat Seeds, a Composter, two Bone Blocks, a Functional Machine Block, two Iron Ingots and a Hopper.

**Echo Chamber (late-game generator).** 3x3x3. Warden sheep in front of its dish turn noise into power: 8,000 FE/t
for one, 20,000 for four.
- Hears vibrations like a sculk sensor within 8 blocks. Levels 5–10 (vibrations per 5 s) give full power; steady
  noise beats erratic. Wool blocks noise.
- Too quiet for 30 s and its sheep fall asleep; too loud for 10 s and it shrieks (Darkness) and stops for a minute.
- Pushes up to 40,000 FE/t from its back port. Comparator reads the noise.
- Advancement *Good Vibrations* (challenge). Config `[echo_chamber]`. New `wool_bale` recipe ingredient for data packs.

**Thermal Reaction Chamber (late-game generator).** Power from hot and cold: Blaze sheep penned at its hot side, Snow
sheep at its cold side, water through the middle.
- 2,500 FE/t a pair, up to about 10,100 for six. A Blaze Rod with Blue Ice doubles it for 30 s. Uses 250 mB a tick.
- Keep the herds even: running hot for 30 s melts it down (it explodes; can be turned off), cold for 30 s freezes it
  in Packed Ice. A comparator warns you.
- A sheep's fleece is spent after an hour of reacting, until it's infused again.
- Advancement *Thermal Shock* (challenge). Config `[thermal_reaction_chamber]`.
- Recipe: Magma Block, Nether Star, Blue Ice; Blaze Sheep Furnace, Hume Energy Cube, Snow Sheep Chiller; Copper Block,
  Fluid Tank, Copper Block.

**Sheep Treadmill (early-game generator).** A sheep walks a belt towards wheat: 20 FE/t, more with Speed and a quarter
more sheared.
- Feed it Wheat (30 s each) or Hay Bales. A sheep tires after 2 minutes and rests 30 s (Endurance helps). It calls a
  sheep within 8 blocks.
- Advancement *Running Ewe Ragged*. Config `[sheep_treadmill]`.

### New items
- **Woollen Armour:** beanie, jumper, leggings and boots, coloured like their wool; a little better than leather.
  **Woollen Boots** make you *Muffled*: silent to sculk and Wardens.

### Changes
- **Machines about a quarter faster** (same total FE), e.g. Separator 3.75 s for Iron Wool. Old default config values
  update themselves.
- **Machines keep their sheep close:** furnaces, the chiller and the generators call back sheep that wander up to 4
  blocks out (config `[sheep_attraction]`).
- **The wrench dismantles machines** instantly into your inventory, and is in `#c:tools/wrench`.
- **Sectioned creative tabs** (Power, Logistics, Production, Utility; Wool, Bales, Spawn Eggs) that fold away.
- The Zero Point Energy Module's emitters turn against the beam.

### Fixes
- The Sheep Treadmill dropped two of itself on 26.3.
- The Mechanical Separator couldn't take power fast enough for the costliest bales.
- Job costs no longer round up (a bale costs 1,000 FE, not 1,020).
- A Thermal Reaction Chamber with a full buffer stopped tracking its herds.
- Creative tab banners didn't fold.

### Docs and test world
- New manual chapters for the Treadmill, the Thermal Reaction Chamber and Woollen Armour.
- The test world is one 146x146 platform of cells, with no hostile mobs, and new scenes for the new machines.
  Teleports are in `test-world/README.md`.
- The released jar no longer ships the GameTests or screenshot tools.
