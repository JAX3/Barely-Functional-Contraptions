# Design Rules

These rules decide what goes into Barely Functional Contraptions and what stays out. Check every new idea against
them before it is built. The aim is a mod where every feature matters, not a mod with the most features.

## The identity

> **Sheep are the power source and the factory.** Every machine needs sheep doing something sheep-like, and every
> machine works, but only barely: it has a quirk, a cost or a way to go wrong.

If an idea doesn't fit that sentence, it doesn't belong in this mod, however good it is.

### Support blocks are exempt
Storage, transport and display blocks support the sheep machines rather than being machines themselves: the Hume
Energy Cube, Fluid Tank, Fluid Reservoir, Wool-Lined Crate, Crate Controller, Machine Status Panel, Energy Cable,
Energy Meter, and Item and Fluid Pipes. They don't need sheep (rule 1) or a quirk (rule 3). Instead they must pass one
test: **does it make a sheep machine easier to run?** A support block that only helps other mods' machines doesn't
belong. Rules 2 and 4 to 10 still apply to them.

## The rules

### 1. It must pass the identity test
A new machine needs sheep to work, or works on what sheep make. A new sheep has to *do* something, not just drop a
different item. Ideas that only copy another mod's feature (a capacitor, pipe tiers, a generic battery) aren't enough
on their own: they need a sheep-shaped reason to exist.

### 2. Name two things it improves
Before building anything, name two features already in the mod that it connects to or makes better. Good examples:
- the Implosion Chamber uses the Explosive sheep and feeds sand and gravel back into recipes;
- the Energy sheep drains Hume Energy Cubes and powers every machine;
- the Grass Seeder speeds up every sheep machine.

If you can't name two, the feature only adds itself. Don't build it.

### 3. It must have a quirk
Every machine has one way it barely works: Static Generator combs oxidise and shock things, the Hume Field
Generator's output is random with no buffer, the Fleece Trawler's net tangles, the Crate bursts when overfilled. A
feature with no quirk is a plain utility, so it should be a card or a config option, not a machine.

### 4. Count the full cost
A machine is never just a block. It needs all of this, and every later rework touches all of it again:

| Part | Part |
|---|---|
| Block, model, textures | Window and screen |
| JEI page | Jade lines |
| Manual chapter | Status panel line |
| Config section | Recipe and advancement |
| GameTests | Test-world cell |
| README and REFERENCE | Changelog entry |

If the idea isn't worth all of that, make it smaller: an upgrade card, a new resource, a config option, or an extra on
an existing machine.

### 5. Deepen before you widen
Before adding something new, look at the "Extras for finished machines" list below. Deepening a machine already in
the mod costs a fraction of a new one and makes the mod feel more finished, not bigger.

### 6. Respect the built-in limits
These limits exist on purpose. When one is reached, merge or remove something rather than finding a way around it:
- **10 chapters per manual tab.** A full tab means the tab has enough.
- **One machine per test-world cell**, and the platform is full.
- **One line in the README** to explain a feature. If it needs a paragraph, it's too fiddly to play.
- **Two upgrade slots** per machine.
- **No new mod-wide systems** (a new energy type, a new kind of storage network) without removing or replacing one.

### 7. No carve-outs
If a feature needs exceptions in other features to work (special cases in recipes, configs or the machine frame), it
isn't settled. Rethink it before building it.

### 8. Cut something on purpose
Every release, ask "what would nobody miss?" and remove, merge or simplify at least one thing. Removing the wireless
power links was the right call; do it again when something stops earning its place.

### 9. Ideas wait two weeks
New ideas go in **Parked ideas** below, with the date. Nothing is started until it has waited at least two weeks and
still passes rules 1 to 3. Most ideas look weaker after a cool-off. The ones that don't are worth building.

### 10. Every release has a scope
Decide what a release contains before starting it, and write it in **Current release** below. Once it's set, new
ideas go to Parked ideas, not into the release. A release has a short theme ("the logistics release") and at most
three new features. Bug fixes and polish don't count towards the three.

## Warning signs

Stop adding features and polish what's there when you notice any of these:
- Polish and bug reports pile up faster than features land.
- A decision is reversed soon after it was made.
- A feature needs its own config section *and* exceptions elsewhere.
- The changelog for a release needs more than a screen to read.
- You're adding something because another mod has it.

## Current release: 1.5.8

**Theme:** finishing and polish.
- Crate Controller window (started).
- Polish and fixes only otherwise.

## Parked ideas

Not to be started until they have waited two weeks and pass rules 1 to 3. Ideas that fail are deleted, not kept.

| Idea | Parked | Notes |
|---|---|---|
| Fluid sheep | 2026-11-27 | Buckets of lava, water or honey for the Fluid Tank. |
| Lightning Array | 2026-11-27 | A copper mast that catches lightning into Hume Energy Cubes; oxidises. Needs a sheep to pass rule 1. |
| Fleece Fusion | 2026-11-27 | Fuses two resource wools into power; clear the lint with shears. |
| Quantum Sheep | 2026-11-27 | Blinks in and out, changes wool type, drops random resource wool. |
| Backpack Sheep | 2026-11-27 | No design yet. |
| Pipe tiers | 2026-11-27 | Likely fails rule 1. |
| Sheep transport | 2026-11-27 | A Sheep Crate or net that keeps a sheep's infusion, skin and stats. |
| Capacitor item | 2026-11-27 | Likely fails rule 1; the Energy sheep already charges items. |
| Resource mutations | 2026-11-27 | Breeding two resources makes a new one. |
| Sheep Feeder | 2026-11-27 | Infuses and re-feeds sheep in range so a pen runs on its own. |
| Auto-breeder and lamb sorter | 2026-11-27 | Similar to Industrial Foregoing's; check rule 1. |

## Extras for finished machines

Small ways to deepen what's already there (rule 5). These don't need the two-week wait.

### Needs a quirk (rule 3)
These machines work, but nothing about them is barely functional yet. Each needs one small quirk, not a rework.
- **Wool Burner:** no downside yet.
- **Blaze Sheep Furnace:** no downside yet.
- **Snow Sheep Chiller:** no downside yet.
- **Powered Furnace:** a plain fast furnace; Blaze sheep are only a bonus.
- **Mechanical Separator:** the core machine, with no quirk.
- **Automatic Shears:** no quirk.
- **Infusion Altar:** no quirk.
- **Wool Packer:** no quirk.
- **Glow Panel:** no quirk (making nothing at night is how all solar power works). Its sheep are optional too (rule 1).

### Other extras
- **Echo Chamber:** catalysts, an Echo Shard boost, fatigue.
- **Upgrade cards:** coloured plates on a machine for its cards; its cards on the Machine Status Panel; a better
  processing step (wool into fibre, giving 4–6 resource instead of 2–4).
- **Wool-Lined Crate:** its window; the Wool Index terminal.
- **Machine Status Panel:** burn-in and a Degauss button.
- **Herd Counter:** falling asleep in an empty pen.
