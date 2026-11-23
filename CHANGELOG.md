# Changelog

## 1.5.7: storage, the Static Generator and the Machine Status Panel (20–23 November 2026)

New contraptions for Minecraft 26.3, 26.2 and 26.1 on NeoForge: the mod's first item storage (crates and a controller that links them into one store), a mid-game
generator that runs on static, and a wall screen that keeps an eye on all your machines. They come after 1.5.5,
in 1.5.7. Numbers are the defaults; almost all of them can be changed in the synced config.

### Storage: the Wool-Lined Crate
A wooden crate lined with wool that holds **lots of one kind of item** and shows it on its front, like a drawer. It
needs no power.
- **Holds** 32 stacks of one item: 2,048 of most things, 512 Ender Pearls, 32 swords. Once something's in, it only takes
  that item until it's empty again.
- **Its front** shows the item and how many, lit by the light in front of it.
- **Putting items in:** right-click its front with the item; right-click twice quickly to put in every one you carry.
- **Taking items out:** left-click its front for a stack, sneak-left-click for one. Its other sides break it as usual,
  and so does its front once it's empty.
- **Lock:** sneak-right-click it with an empty hand to lock it to its item. A little gold padlock shows, its count
  turns gold, and it keeps that item even when it's emptied, so nothing else gets in.
- **A colour-coded storeroom for free:** its wool lining takes the **colour of the item it holds** (red for redstone,
  cyan for diamonds, grey for cobblestone, white while it's empty). It's worked out from the item's own texture, so
  items from other mods work too. Sneak-right-click it with dyed wool to line it a colour of your own; again with the
  same colour to go back to matching its item.
- **Packing:** sneak-right-click it with a Wool Bale to pack it in. Each bale adds 32 stacks, up to 4 bales: 160 stacks,
  10,240 items.
- **Bursting at the seams:** past 90% full its lid bulges, wool squeezing out under it. Once it's full, pushing more at
  it makes it creak, and each push has a 2% chance to **burst** it: 8 of its items fly out and its lock breaks. A pipe
  or hopper pushing at a full crate gets one roll a second (a full crate tells them it has room for one more, so they
  keep straining it).
- **Crate Strap:** sneak-right-click a crate with one to strap its lid down. A strapped crate never bursts.
- **Automation:** pipes and hoppers put items in and take them out on every side; a comparator reads how full it is.
- **Breaking it** keeps everything: its items, lock, bales, lining and strap, shown in its tooltip. A crate with
  something in it (or a shulker box) can't go in another crate.
- **JEI:** a Wool-Lined Crate page, and information pages for the crate and the strap. **Jade:** its item, how many out
  of how many, how full, its lock, bales, lining, and whether it's strapped, bulging or about to burst.
- **Advancement:** *Bursting at the Seams*, for being near a crate when it bursts.
- **Recipes:** Wool-Lined Crate: planks, wool, Iron Nuggets and a Chest. Crate Strap: three Iron Ingots over an Iron
  Nugget.
- New config section `[crate]`: `stacks`, `stacksPerBale`, `maxBales`, `bursting`, `burstChance`, `burstItems`.

### Storage: the Crate Controller
Links Wool-Lined Crates into one store. It needs no power.
- **Joining:** crates touching it, or touching a crate already in its store, join by themselves. Link crates elsewhere
  with the Barely Functional Wrench (the controller, a crate, the controller again): every crate touching the one you
  click comes too. Up to 64 crates, up to 200 blocks away. Crates in a store wear a brass tag, and a crate is only
  ever in one store. Only Wool-Lined Crates join.
- **Pipes and hoppers** on it reach every crate: items go to a crate already holding them, then to an empty unlocked
  crate, and only then into bulging ones; anything stored can be piped out. If nothing fits, the fullest unstrapped
  crate strains, and may burst.
- **Its front** shows how many crates and how full it all is, in green, amber or red. A comparator reads how full.
- **Machine Status Panel:** one line for the whole store (*Stored*, *Nearly full*, *No empty crates*, *Crate missing*,
  *Could burst*), and its problems set off the panel's alarm.
- **Advancement:** *Dewey Decimal Sheep* (goal), for 16 crates in one store.
- **Recipe:** a Wool-Lined Crate, a Hopper, two Comparators, two Iron Ingots and a Functional Machine Block.
- New config section `[crate_controller]`: `range`, `maxCrates`; a chapter in the Contraptions Manual (Logistics).

### Power: the Static Generator and Copper Comb (mid-game)
Two blocks tall, like a Van de Graaff generator. Sheep rubbing against **Copper Combs** in its 5x5 pen build up static,
and its copper dome arcs the charge into power.
- **Power:** each grown sheep with its fleece on, standing at a comb, gives **100 FE/t**; Copper sheep give 150 and
  Redstone sheep 125. Up to six count, each in full: **600 FE/t** from six plain sheep, 900 from six Copper sheep.
- **Its dome** arcs the charge into its buffer every 3 seconds, with a crackle of sparks. It stores 500,000 FE and
  pushes up to 2,000 FE/t out of every face of its base but the top.
- **Copper Combs:** up to 4 in its pen. They oxidise as they're worked (passing on 100, 75, 50 and 25% of a sheep's
  charge), a stage about every 20 minutes. An axe scrapes one back a stage; Honeycomb waxes it so it stops ageing, at
  85%. A broken comb keeps its stage and wax.
- **Shocks:** with nothing taking its power, its buffer fills and the dome keeps charging. Past half full it's
  **Overcharged** and shocks players and mobs in its pen, knocking them back. Woollen Armour insulates you, a quarter
  per piece.
- **Window:** energy, herd and output, the dome's charge with the shock line marked, and a chip for each comb.
- Sneak-right-click to outline its pen; a comparator reads the dome's charge.
- **JEI:** a Static Generator page (the power for one to six plain or Copper sheep, the comb stages, the shock), and
  information pages for the generator and the comb. **Jade:** status, output, herd, dome charge, combs and energy.
- **Advancement:** *Shocking!* (goal), for being knocked back by an overcharged Static Generator.
- **Recipes:** a Lightning Rod, two Copper Blocks, Redstone, two wool and a Functional Machine Block; a Copper Comb from
  three Copper Ingots and a Brush.
- New config section `[static_generator]`, and a chapter in the Contraptions Manual (Power → Mid Game).

### Utility: the Machine Status Panel
A thin wall screen that shows how your machines are doing, all in one place. It needs no power.
- **Linking:** with the Barely Functional Wrench, click the panel (the wrench glows), then each machine within
  **200 blocks** (again to unlink one), then the panel again to finish. Any block of a bigger machine links it: the
  Static Generator's dome, any block of the Echo Chamber, either end of the Sheep Treadmill.
- **Its screen:** two lines for each machine: its name with a status lamp, then its status and one key number (FE/t for
  generators; progress, stored energy, Ice made, wool held, grass or items stored for the rest). **Green** working,
  **amber** waiting, **red** a problem, **grey** idle. A broken machine reads *Missing*.
- **Bigger screens:** panels facing the same way in a rectangle up to **3x3** join into one seamless screen, 4 machines
  a panel (36 on a 3x3), its rows sized to fill it. Break one and the rest regroup.
- **Comparator:** reads how many of its machines have a problem, up to 15, for an alarm lamp or bell (or, if you
  choose, how many are working).
- **Window:** right-click it to name machines, move them up and down, unlink them, choose which ones' problems set off
  the alarm, sort the screen (your order, problems first, by machine), pick its colours (classic, green, amber), and
  set what its comparator counts.
- **Machines it shows:** every generator, the Powered Furnace, Snow Sheep Chiller, Mechanical Separator, Automatic
  Shears, Wool Packer, Infusion Altar, Fluid Reservoir, Grass Seeder, Herd Counter, Wool-Lined Crates, Crate
  Controllers, Hume Energy
  Cubes, Energy Meter and Zero Point Energy Module.
- **JEI:** a Machine Status Panel page and an information page. **Jade:** machines linked, screen size, how many have a
  problem and the first two, and its signal.
- **Advancement:** *Mission Control* (goal), for linking 8 machines to one screen.
- **Recipe:** four Glass Panes, two Redstone, a Functional Machine Block, a Comparator and an Iron Ingot.
- New config section `[status_panel]`, and a chapter in the Contraptions Manual (Utility).

### Fixes
- **Static Generator window:** the dome's charge bar no longer flashes while it's overcharged.
- **Machine Status Panel:** clicking a Static Generator's dome with the wrench now links it (only its base did), and the
  panel reaches 200 blocks (it was 64, too short to reach across a base). A config file from before keeps its old
  `range`: set it to 200, or delete the line.
- **Snow Sheep Chiller on the panel:** out of water, it reads *No water* (it read *No power*).

### Test world
- The platform grew a **seventh row** of cells, and you now start at its new south edge.
- **Static Generator:** six sheep at four fresh combs, making 600 FE/t into a bank of Hume Energy Cubes.
- **Machine Status Panel:** a 3x3 screen showing 21 machines from across the platform, with an alarm lamp.
- **Wool-Lined Crates:** a storeroom wall of fourteen crates, each lined the colour of what it holds; and a hopper
  overfilling a crate of redstone (it bursts now and then) beside a strapped one that never does.

## 1.5.5 (17–19 November 2026)

Everything added or changed in Barely Functional Contraptions from 17 November 2026 to now, for Minecraft 26.3, 26.2
and 26.1 on NeoForge. Numbers are the defaults; almost all of them can be changed in the synced config.

### Minecraft 26.2 and 26.1 support
- The mod now also runs on **Minecraft 26.2** (NeoForge 26.2.x) and **Minecraft 26.1.2** (NeoForge 26.1.2.x), with
  every feature. Download the jar for your version: `barelyfunctionalcontraptions-1.5.5-mc26.3.jar`, `-mc26.2.jar` or
  `-mc26.1.jar`.
- On 26.2 and 26.1 the synced config is a per-world server config, kept in `<world>/serverconfig/`.
- JEI 30.x and Jade 26.2.x work on 26.2; JEI 29.x and Jade 26.1.x work on 26.1.

### New machines

#### Herd Counter (utility)
- A little screen that counts the sheep in the 7x7 pen in front of it every second, and puts the count on a comparator
  (1 a sheep, up to 15). It needs no power.
- **Window:** its screen and signal, a button for each mode, the herd (a row for each resource with how many are ready),
  a ghost filter slot and the Full target (− and +). **JEI:** a Herd Counter page and an information page. **Jade:**
  what it's counting, its signal and filter, the herd and its biggest resources.
- **Modes:** every sheep, ready to shear, sheared, lambs, low on infusion, or Full (15 once there are as many as its
  target). **Filter:** a resource wool or spawn egg in its filter slot counts only that resource.
- **Recount glitch and Steady:** a sheep dashing past its screen is now and then counted twice for a moment; Steady
  (a clock button in its window) averages its counts over 5 seconds to smooth that out. Config `doubleCount`,
  `steadySeconds`.
- **Advancement:** *Count Your Sheep* (goal), for being near one counting 20 sheep.
- **Test world:** a Herd Counter pen with two counters, one in Full lighting a lamp, one counting ready Iron sheep.
- **Recipe:** two Iron Ingots, a Glass Pane, an Observer, a Functional Machine Block and a Comparator.
- New config section `[herd_counter]`, and a chapter in the Contraptions Manual (Utility).

#### Grass Seeder (production)
- Keeps a sheep pen green: every 2 seconds it turns a block of eaten dirt in the 5x5 around it back to grass, then
  grows short grass on up to 40% of it. While it has fuel, sheared sheep in its area graze four times as often, so
  their wool grows back much sooner.
- **Fuel:** Bone Meal (8 blocks each), or 800 FE and 10 mB of water a block, which it uses first.
- **Window:** energy and water gauges, a map of its ground, how green it is, its Bone Meal and the sheep grazing.
  **JEI:** a Grass Seeder page and an information page. **Jade:** its status, grass, sheep grazing and open Bone Meal.
- Sneak-right-click to outline its area; a comparator reads how green it is.
- **Overgrowth:** with nothing to tend and Trim off, its short grass shoots up into tall grass, which sheep can't eat and
  Automatic Shears can't see them in. **Trim** (a button in its window) cuts it back. Config `overgrowth`.
- **Advancement:** *Keep Off the Grass* (goal), for being near a Grass Seeder that has tended 100 blocks.
- **Test world:** a Grass Seeder pen (Trim on) with Automatic Shears, which runs itself.
- **Recipe:** Wheat Seeds, a Composter, two Bone Blocks, a Functional Machine Block, two Iron Ingots and a Hopper.
- New config section `[grass_seeder]`, and a chapter in the Contraptions Manual (Production).

#### Echo Chamber (late-game generator)
- A 3x3x3 machine that turns noise into power. Place it facing you: it fills the space behind and above, and
  breaking any part takes it all down (it drops one, with a pickaxe).
- **Herd:** grown Warden sheep in a 5x5 pen in front of its dish. One makes 8,000 FE/t, up to 20,000 FE/t from four.
- **Hearing:** it hears vibrations like a sculk sensor, within 8 blocks of its dish. Wool blocks noise, Woollen Boots
  make none, and its own Warden sheep don't count.
- **Noise level and sweet spot:** vibrations per 5 seconds over the last 10 seconds (0-15). Levels 5-10 give full
  power; quieter or louder gives less. A steady noise gets full power and an erratic one as little as half.
- **Sleep and shriek:** too quiet for 30 seconds and its sheep fall asleep until the noise is back; too loud for 10
  and the room shrieks, giving players nearby Darkness, and stops for a minute.
- Power comes out of the port in the middle of its back (up to 40,000 FE/t). A comparator reads the noise level.
- **Window:** energy gauge, the herd and output, a noise meter showing the sweet spot, steadiness, and the
  drowsiness and shriek bars.
- **Model:** blue-steel casing lined with sculk, a big resonator dish that pulses, foam walls, louvred vents, and
  a crown of sculk fronds on its roof like the Warden sheep's ears.
- **Recipe:** Iron Block, Echo Shard and Iron Block on top; Echo Shards either side of a Warden Wool Bale in the
  middle; Blackstone, a Functional Machine Block and Blackstone on the bottom.
- **JEI:** an *Echo Chamber* page and an information page. **Jade:** status, output, herd, noise, steadiness and
  energy, from any of its blocks.
- New recipe ingredient `barelyfunctionalcontraptions:wool_bale` for data packs, like `resource_wool`.
- **Advancement:** ⭐ *Good Vibrations* (challenge), for being nearby when one starts listening.
- New config section `[echo_chamber]`.

#### Thermal Reaction Chamber (late-game generator)
- Makes power from the difference between hot and cold. Blaze sheep stand in a 5x5 pen against its hot side (on the
  left as you face it), Snow sheep in a pen against its cold side, and water runs through the middle.
- **Power:** each Blaze sheep matched by a Snow sheep is a pair. One pair makes 2,500 FE/t, and each extra pair adds a
  little less, up to about 10,100 FE/t for six pairs.
- **Boosts:** a Blaze Rod and Blue Ice burning together double the output for 30 seconds.
- **Water:** it uses 250 mB a tick. If it runs dry, it stops and blasts scalding steam out of its front.
- **Balance:**
  - Every unpaired sheep costs a quarter of the output.
  - Running hot for 30 seconds causes a meltdown: the Blaze sheep are singed bare and it explodes like TNT. The
    explosion can be turned off in the config.
  - Running cold for 30 seconds freezes it solid in Packed Ice, which has to be mined away before it restarts.
  - A comparator reads how close it is to either, for safety circuits.
  - With a pen empty it just waits, so a pen of Blaze sheep on their own can't melt it down.
- **Fatigue:** after an hour of reacting a sheep's fleece is spent, until it's infused again at the Infusion Altar or
  by hand.
- **Window:** energy and water gauges, a thermometer for each side, a balance needle, a danger bar, two boost slots and
  a status line.
- **Model:** the machines' blue-steel casing with glowing hot vents, frosted cold vents, cooling fins, and a window on top
  where flame and snow chase each other. It frosts over when it freezes.
- **Recipe:** Magma Block, Nether Star and Blue Ice on top; Blaze Sheep Furnace, Hume Energy Cube and Snow Sheep Chiller
  in the middle; Copper Block, Fluid Tank and Copper Block on the bottom.
- **Advancement:** ⭐ *Thermal Shock* (challenge).
- **JEI:** a *Thermal Reaction* page shows both herds, both boosts, the water, and a table of the power for each number
  of pairs, with and without boosts. The chamber also has an information page.
- **Jade:** shows its status, output, herds, balance, how close it is to a meltdown or a freeze, the boosts, and any
  spent sheep. On a Blaze or Snow sheep, Jade shows how much of its thermal fleece is left.

#### Sheep Treadmill (early-game generator)
- A belt two blocks long. A sheep walks it towards the wheat hanging at the front and makes 20 FE/t.
- It makes more with Speed, and a quarter more while the sheep is sheared.
- Feed it Wheat (30 seconds each) or Hay Bales, by hand or with hoppers and pipes.
- **Stamina:** after 2 minutes a sheep is worn out and rests for 30 seconds. Sheep bred for Endurance walk longer.
- While it has food and no sheep, it calls the nearest sheep within 8 blocks, which hops onto the belt.
- Its model has the machines' blue-steel casing, a metal gantry and an animated belt, and the sheep's legs move as it
  walks.
- **Recipe:** two Iron Ingots, three wool, two Copper Ingots and a Functional Machine Block.
- **Advancement:** ⭐ *Running Ewe Ragged* (goal).

### New items

#### Woollen Armour and the Muffled effect
- Woollen Beanie, Jumper, Leggings and Boots, made from wool and coloured like the wool you craft them from. Mixed
  wool blends the colour, and they can be dyed afterwards.
- Slightly better than leather: armour 1 / 4 / 3 / 1, and a little tougher (durability multiplier 7, against leather's 5).
- They use the standard armour shapes with a subtle knitted texture.
- Wearing the **Woollen Boots** gives *Muffled*: you make no sound, and sculk sensors, shriekers and Wardens can't hear
  you.

### Changes to existing features

#### Machines work about a quarter faster
Each job still costs the same total FE.

| Machine | Before | Now |
|---|---|---|
| Mechanical Separator | Iron wool: 5 s | 3.75 s (every wool and bale ×1.35 speed) |
| Wool Packer | 2 s a bale | 1.5 s |
| Infusion Altar | 5 s | 3.75 s |
| Powered Furnace | 0.75 of a Furnace's time | 0.55 (Raw Iron: 5.5 s) |
| Snow Sheep Chiller | 10 s an Ice with one sheep | 7.5 s |
| Automatic Shears | sweeps every 1 s | every 0.75 s |

Older config files are updated once, the first time they load. Settings still at their old defaults move to the new
ones, and anything you changed is kept.

#### Machines keep their sheep close
- The Blaze Sheep Furnace, Powered Furnace, Snow Sheep Chiller, Static Generator, Thermal Reaction Chamber and Echo Chamber call their sheep back when
  one wanders up to 4 blocks out of the working area.
- A machine only calls sheep while it has room for more.
- The chamber only calls sheep in a way that keeps its herds even, so the attraction can never set off a meltdown or a
  freeze.
- Sheep in one machine's area aren't pulled away by another. Sheep on a lead, riding something, and lambs are left alone.
- With a machine's area shown, the attraction area appears as a faint dashed box.
- New config section `[sheep_attraction]`.

#### Barely Functional Wrench dismantles machines
Mining one of the mod's blocks with the wrench breaks it instantly and puts it, with its contents, straight into your
inventory.

The wrench is now in the common tags `#c:tools`, `#c:tools/wrench` and `#c:storable_item`, so other mods that accept
any wrench recognise it.

#### Sectioned creative tabs
- The main tab is split into the manual's sections: Power, Logistics, Production and Utility.
- The Resource Wool tab is split into Wool, Bales and Spawn Eggs.
- Click a section's banner to fold it away or open it again.

#### Zero Point Energy Module
While a pair is linked, each module's emitter slowly turns the opposite way to the beam.

### Fixes
- **Sheep Treadmill:** breaking one dropped two treadmills on Minecraft 26.3, one for each half (its loot table was in an
  older format that 26.3 ignored). It drops one again.
- **Mechanical Separator:** it couldn't take in power fast enough for the costliest bales, such as the Wither Wool Bale.
  It now accepts at least twice what the wool or bale inside it uses, so every bale runs at full speed when the power is
  there.
- **Exact energy costs:** the separator, Infusion Altar and Wool Packer spread a job's power exactly over its ticks.
  Totals that don't divide evenly no longer round up (a bale costs 1,000 FE, not 1,020).
- **Thermal Reaction Chamber with a full buffer:** while full, it stopped following its herds, so the needle froze and
  adding or removing sheep did nothing. It still makes no power and uses no water while full, but its balance stays
  live, and uneven herds still lead to a meltdown or a freeze.
- **Creative tab sections:** clicking a banner didn't fold the section.

### Manual, docs and test world
- **Contraptions Manual:** new chapters for the Sheep Treadmill (Power → Early Game), the Thermal Reaction Chamber
  (Power → Late Game) and Woollen Armour (Utility), and updated times and notes throughout.
- **Test world:** new scenes for the Powered Furnace, Woollen Armour, the Sheep Treadmill, an ore processing line, the
  Thermal Reaction Chamber, the Echo Chamber and the Static Generator. Teleports are in `test-world/README.md`.
- **Test world on one square platform:** every scene now stands on a single 146x146 platform in a grid of cells,
  instead of a long row of separate platforms, and you start at its south edge. Hostile mobs no longer spawn there.
- **Smaller jar:** the GameTests and the screenshot tools are no longer shipped in the released jar.
- **Docs:** the README, the CurseForge page and the reference doc cover all of the above. There's a new *Sheep
  attraction* section, and config tables for `[thermal_reaction_chamber]`, `[sheep_treadmill]`, `[woollen_armor]` and
  `[sheep_attraction]`.
