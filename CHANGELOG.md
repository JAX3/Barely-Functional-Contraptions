# Changelog

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

#### Static Generator and Copper Comb (mid-game generator)
- Two blocks tall, like a Van de Graaff generator. Sheep rubbing against Copper Combs in its 5x5 pen build up static,
  and its copper dome arcs the charge into its buffer every 3 seconds.
- **Power:** a grown sheep with its fleece on gives 75 FE/t; Copper sheep 150, Redstone sheep 125. Up to six count,
  each after the best a little less: about 400 FE/t from plain sheep, about 800 from Copper sheep.
- **Copper Combs:** up to 4 in its pen. They oxidise as they're worked (100, 75, 50, 25%), about every 20 minutes. An
  axe scrapes one back a stage; Honeycomb waxes it so it stops ageing, at 85%. A broken comb keeps its stage and wax.
- **Shocks:** with its buffer full the dome keeps charging; past half full it shocks players and mobs in its pen,
  knocking them back. Woollen Armour insulates, a quarter per piece. A comparator reads the dome's charge.
- **Window:** energy, herd and output, the dome's charge with the shock line, and a chip for each comb.
- **JEI:** a Static Generator page (the power for one to six plain or Copper sheep, the comb stages, the shock), and
  information pages for the generator and the comb. **Jade:** status, output, herd, dome charge, combs and energy.
- **Advancement:** *Shocking!* (goal), for being knocked back by an overcharged Static Generator.
- **Recipes:** a Lightning Rod, two Copper Blocks, Redstone, two wool and a Functional Machine Block; a Copper Comb
  from three Copper Ingots and a Brush.
- New config section `[static_generator]`.

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
  Thermal Reaction Chamber and the Echo Chamber. Teleports are in `test-world/README.md`.
- **Smaller jar:** the GameTests and the screenshot tools are no longer shipped in the released jar.
- **Docs:** the README, the CurseForge page and the reference doc cover all of the above. There's a new *Sheep
  attraction* section, and config tables for `[thermal_reaction_chamber]`, `[sheep_treadmill]`, `[woollen_armor]` and
  `[sheep_attraction]`.
