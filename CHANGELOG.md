# Changelog

## 1.5.7: storage, the Fleece Trawler, upgrade cards, the Static Generator, the Hume Field Generator, the Machine Status Panel and a purple Echo Chamber (20–25 November 2026)

New contraptions for Minecraft 26.3, 26.2 and 26.1 on NeoForge: the mod's first item storage (crates and a controller that links them into one store), a fishing
machine driven by a new sea-going sheep, upgrade cards for the processing machines, a mid-game generator that runs on static, a late-game
generator with a random output and no buffer, and a wall screen that keeps an eye on all your machines. The Echo Chamber is recoloured to match the other machines, and machine recipes take Copper Wool from your
sheep where they took Copper Ingots. They come after 1.5.5,
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
- **JEI:** a Crate Controller page and an information page. **Jade:** crates, items stored and how full, empty crates,
  problems and its signal, with every item in the store; on a crate, whether it's in a store.
- **Advancement:** *Dewey Decimal Sheep* (goal), for 16 crates in one store.
- **Recipe:** a Wool-Lined Crate, a Hopper, two Comparators, two Iron Ingots and a Functional Machine Block.
- New config section `[crate_controller]`: `range`, `maxCrates`; a chapter in the Contraptions Manual (Logistics).

### The Explosive sheep
A new themed sheep, infused with **TNT** at the altar (with 2 Gunpowder and a Fire Charge).
- **Looks:** a TNT fleece, laid out face by face so the red stripes run along its body and the white band reads
  "TNT" down each flank, little TNT blocks on its back, and a fuse sticking up out of them.
- **Short fuse:** fire, lava, lightning, or flint and steel (or a Fire Charge) on it lights the fuse. It hisses like
  a creeper for a second and a half, then **pops**: a small blast that **breaks no blocks and hurts no one**, but
  shoves things nearby and **blows its fleece off as Explosive Wool**, exactly as shearing would (one infusion use).
  The pop puts out any fire on it. Only a sheep with its fleece on has a fuse, so it won't go off again until its
  wool grows back.
- **Its wool** separates into **Gunpowder** (2–4), with a small chance of a block of TNT.
- **Advancement:** *Short Fuse*, and Explosive Wool joins the *Fleece of Every Colour* challenge (now 27 wools).
- For data packs: a new `fuse` appearance feature, and `"block_wool": true`, which lets a themed sheep still wear the
  fleece made of its resource's block.

### Production: the Fleece Trawler and the Tide sheep
A dockside winch that **fishes the pool in front of it**. It needs no power: **Tide sheep swimming in the pool drive
it**.
- **Tide sheep:** a new themed sheep, infused at the altar with Prismarine Crystals (2 Prismarine and a Nautilus Shell
  round it). A fin along its back and gills on its neck; it breathes underwater, bubbles as it swims and calls like a
  dolphin. Its wool separates into Prismarine Shards and Crystals. Advancement *Making Waves*; it's in the
  collect-every-wool challenge too.
- **Placing it:** its net faces away from you, so place it on the bank looking out over the water.
- **Its pool:** the water in the 7x7 in front of it, from its own level down to 4 deep. It needs at least 18 water (a
  3x3 two deep) and fishes faster the bigger the pool, at full speed with all 196.
- **Its sheep:** each grown Tide sheep swimming in the pool adds speed, up to 4. A haul takes 30 seconds with one sheep
  in a full pool, and a quarter of that with four. Ocean or river biomes and rain each make it a quarter faster again.
- **Hauls** are the vanilla fishing loot: fish, and now and then junk.
- **Lures:** put resource wool in its lure slot (right-click it with the wool, or pipe it in) and each haul has a 30%
  chance to bring up 1–2 of that resource instead: Raw Iron on Iron Wool, Diamonds on Diamond Wool. One wool lasts 8
  resource hauls, then it's used up.
- **Barely functional:** now and then (4% of hauls) its net **tangles** and it stops until you cut it free with
  shears, which gives back a little string. Very rarely a haul drags up a confused, dripping sheep onto the bank.
- **Its window:** right-click it with an empty hand. Its lure slot and how many hauls the lure has left, a pip for each
  Tide sheep, a bar for its pool, the winch winding in each haul, its 9 catch slots and its status; hover over any of
  them for more. A button shows its pool's outline in the world, and so does sneak-right-clicking it.
- **Automation:** hoppers and pipes take the catch out of every side and put lures in; a comparator reads how many of
  its catch slots are in use. Broken, it drops its lures and catch.
- **Machine Status Panel:** *Hauling* (and how far through the haul), *No water*, *No sheep*, *Tangled* or *Output
  full*.
- **JEI:** a page for its fishing (a Tide sheep in, fish and junk out) and one for each resource wool as a lure, plus an
  information page. **Jade:** its status, sheep, pool, winch, lure and how many hauls it's made.
- **Advancement:** *Reel Ewe In*, for being near one when it hauls up a resource on a lure.
- **Recipe:** Blue Steel, a Fishing Rod, two Tide Wool Bales, a Functional Machine Block and a Hopper.
- New config section `[fleece_trawler]`: `haulTicks`, `maxSheep`, `minPool`, `lureChance`, `lureUses`,
  `tangleChance`, `straySheepChance`, `waterBonus`, `rainBonus`; a chapter in the Contraptions Manual (Production).

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
  three Copper Wool and a Brush.
- New config section `[static_generator]`, and a chapter in the Contraptions Manual (Power → Mid Game).

### Power: the Hume Field Generator (late game)
A new late-game generator, 2x2 and three blocks tall, whose field can't make up its mind. It began in the manual's
Work in Progress tab and is now finished: in released jars, in the creative tab's Power section, and in the manual's
**Power** tab (Late Game).
- **Built like the other big machines,** the Echo Chamber above all:
  - the Echo Chamber's own casing: the same plates, the same dark bolted band round its plinth and lid, and the same
    steel corner pillars;
  - a plate wall on each side with a window sunk in a trim bezel, as the Echo Chamber's dish is, its glass tinted the
    field's purple so its core, flickering between purple and magenta, and its three coils glow through;
  - a power port under each window in a trim housing, like the one on the Echo Chamber's back, since it pushes power
    out of every side;
  - an emitter on the lid, among four corner bolts, its lens the same purple as the Echo Chamber's dome.

  It fills the space east, south and above where it's placed, and breaking any of its twelve blocks drops one
  generator.
- **Random power, no buffer:** every second its field rolls a **random amount of FE per tick** between `minPerTick`
  and `maxPerTick` (200 to 1,200 FE/t by default, so 700 on average), and makes that much each tick until the next
  roll (`ticksPerRoll`, 20). It has **no energy buffer**: each tick's power goes
  straight into whatever touches any of its blocks (cables, Hume Energy Cubes, machines), and whatever they don't take
  is **lost**. Nothing can pull power out of it, and on its own it wastes everything it makes. It offers each tick's
  power to a different neighbour first, so no one of them always gets first pick.
- **The pulse:** every 5 seconds its field **pulses** with a burst of purple sparks and a deep hum. It's just for show.
- **Window** (right-click any of its blocks). It has no buffer, so in place of an energy gauge:
  - a **field gauge** showing what it makes now, against its range (shaded) and its average (yellow marks);
  - this tick's output, how much was **taken** and how much was **lost**;
  - a **trace** of its last rolls, one bar a second with the roll now on the right, each bar purple where its
    neighbours took the power and red where it was lost, with the ends of its range dashed across it;
  - its average over its last 10 rolls and its range;
  - a **countdown to the next pulse** by its title, flashing in the last second;
  - a status lamp: *Delivering all it makes* (green), *Wasting power* (amber: what touches it was full or couldn't take
    that much a tick, so some or all was lost), *Nothing to take it: all lost* (red), or *Field off* (its range is 0).

  Its core flickers slowly, and the gauge's shimmer is gentle.

  Every part has a tooltip, and the status's says how many blocks touching it take power.
- **Jade:** its status, this tick's output and its range, what was taken and lost, its average, the next pulse, and that
  it has no buffer, whichever of its twelve blocks you look at.
- **JEI:** a *Hume Field Generator* page (its range, its average, how often it rolls, what it powers and that it has no buffer,
  read from the config), and an information page.
- **Recipe:** Blocks of Blue Steel in the corners, Tinted Glass at the top, two Hume Energy Cubes either side of a
  Nether Star, and a Functional Machine Block at the bottom. It's unlocked by picking up a Nether Star. A pickaxe breaks
  it quickly.
- New config section `[hume_field_generator]`: `minPerTick`, `maxPerTick` (swapped if the minimum is the larger),
  `ticksPerRoll` (how long each amount lasts; 1 rolls every tick) and `pulseInterval` (ticks between pulses; 0 stops
  them).
- **Contraptions Manual:** its chapter moved from Work in Progress to Power. To keep the Power tab to ten chapters,
  *Energy Storage* (the Hume Energy Cube) moved to **Logistics**, next to cables and tanks.
- **Test world:** cell (3, 6) shows it delivering, wasting and alone (see Test world below).
- **Machine Status Panel:** link it with the wrench from any of its twelve blocks. It shows *Delivering* (green),
  *Wasting power* (amber) or *Not connected* (red, which the panel's alarm comparator counts), with its average FE/t
  over its last 10 rolls. The test world's panel now shows its three as well.
- **Not yet:** it takes no upgrade cards, and its planned upkeep (re-infusing sheep) isn't in.

### Utility: the Machine Status Panel
A thin wall screen that shows how your machines are doing, all in one place. It needs no power.
- **Linking:** with the Barely Functional Wrench, click the panel (the wrench glows), then each machine within
  **200 blocks** (again to unlink one), then the panel again to finish. Any block of a bigger machine links it: the
  Static Generator's dome, any block of the Echo Chamber or Hume Field Generator, either end of the Sheep Treadmill.
- **Its screen:** two lines for each machine: its name with a status lamp, then its status and one key number (FE/t for
  generators; progress, stored energy, Ice made, wool held, grass or items stored for the rest). **Green** working,
  **amber** waiting, **red** a problem, **grey** idle. A broken machine reads *Missing*. The text sizes itself so every
  machine's whole name, status and number fit.
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

### Upgrade cards
Cards that change how a machine works: faster, cheaper, holding more power, reaching further, getting more out of each
wool, or just quieter.
- **Two upgrade slots** in the Mechanical Separator, Wool Packer, Powered Furnace, Infusion Altar, Automatic Shears and
  Snow Sheep Chiller, in a small panel on the right of their window. Drag a card in or shift-click it: one card a
  slot, and only cards that machine takes.
- **Upgrade Base:** Blue Steel, Redstone and any wool make two blank cards. Each card is a base plus a few items:

| Card | Crafted with | What it does | Goes in |
|---|---|---|---|
| **Speed** | Block of Redstone, Sugar, Red Wool | Jobs take 60% of the time, for 150% of the power | Separator, packer, furnace, altar, shears |
| **Efficiency** | Block of Copper, Lime Wool | Jobs cost 75% of the power, taking 115% of the time | Separator, packer, furnace, altar, shears |
| **Capacity** | Wool Bale, Light Blue Wool | Four times the energy buffer and the power taken in a tick | Separator, packer, furnace, altar, shears |
| **Range** | Ender Pearl, Purple Wool | One more block of working area on every side: shears 3x3 to 5x5, altar and chiller 7x7 | Shears, altar, chiller |
| **Yield** | Diamond, Yellow Wool | One more of each separation's resource and string (Iron Wool: 3–5 Raw Iron, 3 String) | Separator |
| **Muffler** | Woollen Helmet, Black Wool | Silences the machine's own sounds; it works just the same | Separator, packer, furnace, altar, chiller |

- **Stacking:** a second card of the same kind does half as much (two Speed cards: 48% of the time for 187.5% of the
  power). Different kinds multiply: Speed and Efficiency together make jobs 69% as long for 112.5% of the power.
- **Barely functional:** Speed costs power and Efficiency costs time. A Range card's area outline wobbles between
  colours, and a chiller with one calls its sheep back only every other second, so they wander a bit more. Yield has no
  catch: the separator waits for room for the extra, so it never jams.
- **Kept:** cards stay with a machine that's broken or picked up with the wrench (its item lists them), and come back
  when it's placed. The separator and shears, which keep their power when picked up, keep all of it with a Capacity
  card. Hoppers and pipes never touch the upgrade slots.
- **JEI:** an information page for each card and for each machine that takes them, and its item list keeps clear of
  the panel. **Jade:** a machine's cards and what they add up to (*Time 60%, Power 150%*).
- **Advancements:** *Some Assembly Required* for putting a card in a machine; the hidden *Maximum Overdrive* for two
  Speed cards in one.
- **Data packs:** the block tags `#barelyfunctionalcontraptions:accepts_upgrade/<card>` decide which machines take which
  cards.
- New config section `[upgrades]`: `speedTime`, `speedEnergy`, `efficiencyTime`, `efficiencyEnergy`,
  `capacityMultiplier`, `rangeBlocks`, `yieldBonus`, `secondCardStrength`; an *Upgrade Cards* chapter in the
  Contraptions Manual (Utility).

### Blue Steel
- A new item, **Blue Steel**: an Iron Ingot and any wool, crafted together. Nine make a **Block of Blue Steel**
  (and it gives the nine back).
- **Every crafting recipe in the mod now takes Blue Steel where it took an Iron Ingot, and a Block of Blue Steel
  where it took a Block of Iron**: the Functional Machine Block, every machine, the wrench, cables, tanks, meters,
  crates, the status panel and the rest (22 recipes). Iron Nuggets stay, and the Infusion Altar still takes a real
  Iron Ingot for an Iron sheep.
- Blue Steel is in `#c:ingots`, and its block in `#c:storage_blocks`.

### Copper Wool in place of Copper Ingots
Recipes that took a **Copper Ingot** now take **Copper Wool** from a Copper sheep, and the mid- and late-game machines
take a **Copper Wool Bale** instead:
- **Copper Wool:** the Mechanical Separator, Automatic Shears, Powered Furnace, Sheep Treadmill, Fluid Pipe, Item Pipe
  and Copper Comb.
- **Copper Wool Bale:** the Mycelial Generator and the Zero Point Energy Module.
- **Still a Copper Ingot**, so a new world can get started: every sheep is infused at the Infusion Altar, which is built
  on a Functional Machine Block and needs power to run. So the **Functional Machine Block**, the **Energy Cable** and
  the **Wool Burner** keep their Copper Ingots, and the **Infusion Altar** keeps its Gold Ingots. Once the altar is
  running, a Copper sheep gives you everything else.
- Copper Wool is any wool from a Copper sheep, and a bale is nine of it pressed in a Wool Packer. Blue Steel and
  copper blocks are unchanged.
- In JEI, the manual and `docs/REFERENCE.md`, the recipes show the Copper Wool and bales. A new GameTest checks them:
  Copper Wool makes Item Pipes and a Copper Ingot or another resource's wool doesn't, the Zero Point Energy Module
  needs bales rather than single wool, and the Functional Machine Block still takes Copper Ingots.

### The Echo Chamber: the machines' colours and a sculk recipe
The Echo Chamber was the only machine in sculk's cyan. It now wears the blue steel and purple of every other machine,
so it sits with the Hume Field Generator and the rest instead of standing out from them.
- **Its glow is purple:** the resonator dish and the heart that breathes in its middle, the crown of fronds on its
  roof, the sculk creeping over its side walls and the light inside its back vents. It's the purple of the machines'
  ports and the Hume blocks.
- **Its own roof and dome:** they were vanilla sculk and a sculk catalyst. The roof is now its own purple sculk, and the
  dome is a blue-steel drum with a purple band, topped with a glowing well like the Hume Field Generator's emitter.
- **Blue-steel foam:** the egg-crate foam on its side walls was dark navy. It's now blue steel, a shade darker than the
  casing.
- **The same purple everywhere else:** its window's text and strip, its Jade tooltip, its JEI page and its
  working-area outline.
- **A new recipe, in sculk and blue steel:**
  - the Echo Shards and the Sculk Wool swap places: Sculk Wool Bales (from Warden sheep) now sit where the three Echo
    Shards were, and a single Echo Shard sits in the middle where the bale was;
  - the two Blackstone at the bottom are now Blocks of Blue Steel, so there's one in each corner.

  ```
  I W I      I = Block of Blue Steel, W = Sculk Wool Bale
  W E W      E = Echo Shard
  I M I      M = Functional Machine Block
  ```
- Otherwise it works exactly as before, Echo Chambers already built just change colour, and its Warden
  sheep and their sculk stay cyan.

### Sheep wear their blocks
- An infused sheep's fleece is now **its resource's block**: Block of Redstone on a Redstone sheep, Block of Iron on an
  Iron sheep, oak bark on a Wood sheep, mushroom blocks on the mushroom sheep. It's the block that grows out of it,
  shaded by the wool's fluff, and without the glinting overlay (the blocks have their own pattern).
- It's made on your client from the block's own texture, so data-pack resources, modded blocks and resource packs
  all work.
- The **themed sheep** keep their own designs: the Ghast, Spider, Wither, Dragon, Blaze, Warden, Glow Ink, Skeleton
  and Wither Skeleton sheep (anything with extra parts, a skull, a glow, flames or sculk). Hay and Snow keep their
  hand-drawn fleeces; lambs keep a plain one in the resource's colour.
- Client config `blockTexturedWool` (on) brings back the old plain fleece when off.

### Processing machines about half as fast again
Each job still costs the same total FE; it just finishes sooner.

| Machine | Before | Now |
|---|---|---|
| Mechanical Separator | Iron wool: 3.75 s (×1.35 speed) | 2.5 s (×2 speed); a Wither Wool Bale 22.5 s for its 9 wool's worth |
| Wool Packer | 1.5 s a bale | 1 s |
| Infusion Altar | 3.75 s | 2.5 s (a tier 2 infusion draws 4,000 FE/t) |
| Powered Furnace | 0.55 of a Furnace's time (Raw Iron: 5.5 s), 20 FE/t | 0.35 (3.5 s), 31 FE/t, so a smelt costs about the same |
| Snow Sheep Chiller | 7.5 s an Ice with one sheep, about 2.3 s with four | 5 s, about 1.5 s with four |
| Automatic Shears | sweeps every 0.75 s | every 0.5 s |

Older config files are updated once, the first time they load: settings still at their old defaults (from either
speed-up) move to the new ones, and anything you changed is kept.

### Shorter command: `/bfc`
The mod's command is now **`/bfc`** (short for Barely Functional Contraptions) instead of
`/barelyfunctionalcontraptions`. It works the same, for operators (permission level 2):

| Command | What it does |
|---|---|
| `/bfc preview` | Lists the wool types `generate` would make, without changing anything |
| `/bfc generate` | Makes a wool type for every material in the modpack that doesn't have one yet, saves them in the world's data pack `barelyfunctionalcontraptions_generated`, and reloads |
| `/bfc generate all` | The same, also remaking materials that already have one |
| `/bfc generate material <material>` | Makes one material from the storage-block tags (Tab lists them) |
| `/bfc generate item <item>` | Makes a wool type fed with any item you choose |
| `/bfc generate hand` | The same, with the item in your main hand |
| `/bfc generate remove <name>` | Deletes one generated wool type (Tab lists them) |
| `/bfc generate dedupe` | Deletes generated wool types that duplicate another, then reloads |
| `/bfc list` | Lists every loaded wool type |

- Its messages, the Contraptions Manual, the config comments and the docs all use `/bfc` now.
- **Updating:** command blocks, functions and scripts that call `/barelyfunctionalcontraptions` need changing to
  `/bfc`; the long name no longer works.

### Under the hood: one machine frame
Every machine and generator is now built from a shared **machine frame** with separate **modules** switched on for
what it does: **energy**, **fluid**, **upgrade cards** and an **internal inventory** (and an anvil name for the
Automatic Shears and Fluid Reservoir). Each machine has only the modules it needs, and the frame saves them, keeps them
on the machine's item, syncs them, drops their contents and connects them to cables, pipes and hoppers. That's one
shared code path where there used to be eighteen copies, so a new machine is mostly its own job.
- **Nothing changes in game:** every machine saves under the same keys as before, so existing worlds, broken machines
  carrying their energy or cards, and renamed machines all load as they were. Machines connect on the same sides as
  before.
- **For developers:** how the frame and modules work, how to build a new machine on them, and which machine has which
  module are in [docs/MACHINE-FRAME.md](docs/MACHINE-FRAME.md). Two new GameTests check the old save keys and the
  connections.

### Removed: wireless power to machines
- The Barely Functional Wrench no longer links a Mycelial Generator to a Mechanical Separator, Automatic Shears or Hume
  Energy Cube to power it wirelessly. Run **Energy Cables** from the generator instead, or put the machine against it.
- Gone with it: the generator window's wireless button, the `max_power_links_per_generator` config setting and the
  `#barelyfunctionalcontraptions:wireless_power_receivers` block tag. Right-clicking a machine with the wrench now
  opens it as usual.
- **Existing worlds:** wireless links already made are ignored (machines that ran on them stop until cabled). The wrench
  still links generators to generators, machines to a Machine Status Panel and crates to a Crate Controller.

### Fixes
- **Item Pipe on a Mechanical Separator's top:** it never took the separator's outputs, even set to pull: outputs only
  came out of the bottom, so the separator filled up and stopped (*Output Full*). Pipes now take its outputs from any
  side that has one, as they do from the Automatic Shears; it still pushes outputs only out of its bottom, so a hopper
  feeding it from above gets nothing back. `pipesTakeOutputsFromAnySide = false` in `[separator]` brings back the old
  way.
- **Static Generator window:** the dome's charge bar no longer flashes while it's overcharged.
- **Machine Status Panel:**
  - **Long names fit:** its screen sized its text for a fixed width that suited short names, so with only a machine
    or two linked it drew them up to three times as large and cut long names off: a lone Hume Field Generator read
    *Hume Field Generat*, *Wasting po*. It now works out how wide each column must be for every machine's whole
    name, status and number, and sizes the text to that. A screen of short names looks as before; one with a long
    name draws its text a little smaller rather than cutting it.
  - **In its window too:** names and statuses were cut at a fixed width (*Mechanical Separat*, *Hume Field Generat*).
    They now get all the room left of the number and distance, and one that's still a little too long is drawn
    slightly smaller (down to three-quarters size) instead; only a very long custom name is still shortened.
  - Clicking a Static Generator's dome with the wrench now links it (only its base did), and the panel reaches 200
    blocks (it was 64, too short to reach across a base). A config file from before keeps its old `range`: set it to
    200, or delete the line.
  - A Snow Sheep Chiller out of water reads *No water* (it read *No power*).

### Test world
- The platform grew a **seventh row** of cells, and you now start at its new south edge.
- **Static Generator:** six sheep at four fresh combs, making 600 FE/t into a bank of Hume Energy Cubes.
- **Machine Status Panel:** a 3x3 screen showing 26 machines from across the platform (the crate store, the Fleece Trawler and the three Hume Field Generators among them), with an alarm lamp.
- **Wireless power scene:** its generator is now cabled to the separator and shears it used to power wirelessly.
- **Hume Field Generator:** cell (3, 6) has three, one for each state: two Hume Energy Cubes against the first catch
  everything it makes (*Delivering*), a Powered Furnace smelting cobblestone against the second takes only the little
  it uses (*Wasting*), and the third stands alone (*Nothing to take it*). All three are on the status panel.
- **Upgrade cards:** cell (2, 6) has three separators on the same Iron Wool: plain, with a Speed card and with an
  Efficiency card.
- **Wool-Lined Crates:** a storeroom wall of fourteen crates, each lined the colour of what it holds; and a hopper
  overfilling a crate of redstone (it bursts now and then) beside a strapped one that never does.
- **Crate Controller:** on top of the storeroom wall (all its crates join by touch) beside an empty crate, fed by a
  hopper and a chest: cobblestone, coal and wheat go to their crates, bread to the empty one. The status panel shows
  the whole store as one line.
- **Fleece Trawler:** a full 7x7 pool, four deep, sunk into a fenced dark prismarine basin, with three Tide sheep
  swimming in it; the trawler on its rim with an Iron Wool lure, and a hopper under it into a chest in the floor.

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
  a crown of sculk fronds on its roof like the Warden sheep's ears. (Recoloured purple in 1.5.7.)
- **Recipe:** Iron Block, Echo Shard and Iron Block on top; Echo Shards either side of a Warden Wool Bale in the
  middle; Blackstone, a Functional Machine Block and Blackstone on the bottom. (Changed in 1.5.7.)
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
- **Recipe:** two Iron Ingots, three wool, two Copper Ingots and a Functional Machine Block. (Changed in 1.5.7: Blue
  Steel and Copper Wool.)
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
