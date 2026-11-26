# Changelog

Numbers are defaults and can be changed in the synced config. Full details are in the Contraptions Manual and
`docs/REFERENCE.md`.

## 1.5.7 (20–26 November 2026)

For Minecraft 26.3, 26.2 and 26.1 on NeoForge.

### New
- **Wool-Lined Crate:** stores 2,048 of one item and shows it on its front. Its lining takes the item's colour; lock
  it, pack it with Wool Bales (up to 160 stacks), and strap it, or it may burst when overfilled.
- **Crate Controller:** links up to 64 crates into one store that pipes and hoppers can use.
- **Fleece Trawler and Tide sheep:** a winch that fishes the pool in front of it, driven by Tide sheep (Prismarine
  Crystals) swimming there. Resource wool lures haul up that resource; now and then its net tangles.
- **Explosive sheep:** infused with TNT. Light its fuse and it pops its fleece off as wool, breaking nothing. Regrows
  in about 20 s.
- **Implosion Chamber:** lights the Explosive sheep in its pen and uses their blasts to crack Cobblestone into Gravel
  and Gravel into Sand, 8 a blast.
- **Static Generator (mid game):** sheep rubbing Copper Combs make 100 FE/t each, up to 600. Combs oxidise; left full,
  it shocks things nearby.
- **Hume Field Generator (late game):** makes a random 200–1,200 FE/t each second, with no buffer: what its neighbours
  can't take is lost.
- **Energy sheep:** infused with a Hume Energy Cube. Drains cubes within 3 blocks and charges machines and players'
  powered items within 8, wirelessly. It lights up its surroundings like a Glow Ink sheep, as brightly as its charge.
- **Machine Status Panel:** a wall screen (up to 3x3) showing up to 36 linked machines' status, with a comparator for
  problems.
- **Upgrade cards:** Speed, Efficiency, Capacity, Range, Yield and Muffler, in two slots on the processing machines.
- **Blue Steel:** an Iron Ingot and wool. It replaces Iron Ingots in the mod's recipes.

### Changed
- Recipes take **Copper Wool** (or bales, for later machines) instead of Copper Ingots; the Functional Machine Block,
  Energy Cable, Wool Burner and Infusion Altar keep their ingots so a new world can start.
- The **Echo Chamber** is recoloured to the machines' blue steel and purple, with a new recipe.
- Infused sheep wear their **resource's block** as fleece (client config `blockTexturedWool`).
- Processing machines are **about half as fast again** for the same power.
- The command is now **`/bfc`**.
- **Wireless power links** from the wrench are gone: use cables, or an Energy sheep.
- Every machine now runs on one shared machine frame; worlds load as before.

### Fixed
- An Item Pipe on a Mechanical Separator's top can take its outputs.
- The Machine Status Panel fits long names, links a Static Generator's dome, reaches 200 blocks, and shows *No water*
  for a dry Chiller.
- The Static Generator's charge bar no longer flashes while overcharged.

### Test world
- A seventh row of cells with the new machines and sheep; the status panel shows 27 machines.
- Rebuilding the world no longer wipes the seventh row's floor.

## 1.5.5 (17–19 November 2026)

### New
- **Minecraft 26.2 and 26.1.2** support, a jar for each version.
- **Herd Counter:** counts the sheep in a pen onto a comparator, by mode and resource.
- **Grass Seeder:** keeps a pen green so sheared sheep regrow sooner; runs on Bone Meal or power and water.
- **Echo Chamber:** Warden sheep turn steady noise into up to 20,000 FE/t; too quiet and they sleep, too loud and it
  shrieks.
- **Thermal Reaction Chamber:** Blaze and Snow sheep in balance make up to about 10,100 FE/t; uneven herds melt it
  down or freeze it.
- **Sheep Treadmill:** a sheep walks a belt for wheat, making 20 FE/t.
- **Woollen Armour:** Woollen Boots make you silent to sculk and Wardens.

### Changed
- Machines are about a quarter faster for the same power.
- Machines call back sheep that wander out of their area.
- The wrench dismantles machines straight into your inventory.
- Creative tabs are split into folding sections.

### Fixed
- The Sheep Treadmill dropped two of itself on 26.3.
- The Mechanical Separator couldn't take power fast enough for the costliest bales.
- Job costs no longer round up.
- A full Thermal Reaction Chamber stopped tracking its herds.
