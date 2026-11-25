# The machine frame (for developers)

Every machine and generator is built the same way: a **base machine frame** (`MachineFrame`) with separate
**modules** switched on for what that machine actually does. Upgrade slots, energy, fluid and an internal
inventory are each one module. A machine installs only the modules it needs, and the frame handles everything they
have in common. The machine's own class only has to do its job.

All of it is in `com.jax3.barelyfunctionalcontraptions.sheep.machine`, except the upgrade module, which lives with
the cards in `sheep.upgrade`.

## The frame

`MachineFrame` extends `BlockEntity`. For each module a machine has installed, it:

- **saves and loads** it with the machine (`saveAdditional` / `loadAdditional`);
- **keeps it on the machine's item** when the machine is broken or picked up, and gives it back when the machine is
  placed (`collectImplicitComponents`, `applyImplicitComponents`, `removeComponentsFromTag`);
- **adds what clients need** to the machine's update packet and tag. The machine adds its own part with
  `writeClientData(CompoundTag)` instead of overriding `getUpdateTag`;
- **drops what it holds** when the machine is broken (`preRemoveSideEffects`);
- **offers its energy, fluid and items** to cables, pipes and hoppers. `MachineFrame.registerCapabilities(event, types...)`
  registers all three capabilities for each block entity type through `energyFor(side)`, `fluidFor(side)` and
  `itemsFor(side)`, and a machine without a module offers nothing there.

A machine still overrides `loadAdditional` / `saveAdditional` for its own fields (progress, its area setting...), and
calls `super` first so the frame handles the modules.

## The modules

| Module | What it is | Saved as |
|---|---|---|
| `EnergyModule.of(handler)` | An FE buffer: a `MachineEnergy` for a machine that uses power, a `GeneratorEnergy` for one that makes it. `.onSides((state, side) -> …)` limits which faces connect to cables. | `energy` |
| `FluidModule.of(tank)` | A fluid tank (any `FluidStacksResourceHandler`: `WaterTank`, `ReservoirTank`…), with `.onSides` as for energy. | `tank` |
| `MachineUpgrades` | The two upgrade-card slots and what the cards do. Kept on the item as `machine_upgrades`; syncs the Range and Muffler cards to clients. | `upgrades` |
| `MachineInventory.builder()…build()` | Internal item slots, each with its own rule: `.input(name, accepts)` (things go in, hoppers and pipes don't take them out), `.output(name)` (only the machine fills it, hoppers and pipes take from it), `.slot(name, accepts, extractable)`. `.savedAsList()` saves them as a vanilla item list; `.onReplaced(…)` hears when a slot's stack is swapped. A machine using it implements `InventoryHolder`, so hoppers see it as a container. | each slot by name, or `Items` |
| `NameModule` | The name an anvil gave the machine: its window's title, kept when it's broken. | `CustomName` |

Each module saves under the key the machine always used, so worlds from before the frame load unchanged.

Every module implements `MachineModule`, whose hooks (`save`, `load`, `collectComponents`, `applyComponents`,
`removeComponents`, `writeSync`, `readSync`, `dropContents`) all do nothing by default. A machine with unusual needs
can install a module of its own. Two do:

- the Mechanical Separator's `SeparatorInventory`, whose sides come from the config;
- the Mycelial Generator's modules, which keep its own save format (`Inventory`, `Tank`, `Energy`).

A machine whose sides work differently overrides `energyFor`, `fluidFor` or `itemsFor`. The Separator does this for
its configured item sides, the Zero Point Module for its pair's pooled buffer, and the Mycelial Generator for its sided
views.

## A new machine

```java
public class ExampleMachineBlockEntity extends MachineFrame implements Upgradeable, InventoryHolder, MenuProvider {
    // Its modules: power in on every side, two upgrade slots, an input and an output.
    private final MachineEnergy energy = install(EnergyModule.of(new MachineEnergy(
            () -> 20_000, () -> 200, this::setChanged))).handler();
    private final MachineUpgrades upgrades = install(new MachineUpgrades(this));
    private final MachineInventory inventory = install(MachineInventory.builder()
            .input("input", stack -> stack.is(Items.COBBLESTONE))
            .output("output")
            .build());
    private int progress;

    // ... the machine's own job, in its server tick ...

    @Override public MachineInventory inventory() { return inventory; }
    @Override public MachineUpgrades upgrades() { return upgrades; }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); // its modules
        progress = in.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); // its modules
        out.putInt("progress", progress);
    }
}
```

Then add its block entity type to the `MachineFrame.registerCapabilities(...)` call in `ModCapabilities`. A machine
that spans several blocks (the Sheep Treadmill, the Echo Chamber) registers per block instead and calls the main block
entity's `energyFor` / `itemsFor`.

## Which machine has which modules

| Machine | Energy | Fluid | Upgrades | Inventory | Name |
|---|---|---|---|---|---|
| Mechanical Separator | ✓ (configured sides) | | ✓ | ✓ (its own, configured sides) | |
| Automatic Shears | ✓ (not the front) | | ✓ | ✓ (9 outputs) | ✓ |
| Powered Furnace | ✓ | | ✓ | ✓ | |
| Wool Packer | ✓ | | ✓ | ✓ | |
| Infusion Altar | ✓ | | ✓ | ✓ (resource + 4 catalysts) | |
| Snow Sheep Chiller | | ✓ | ✓ | ✓ | |
| Grass Seeder | ✓ | ✓ | | ✓ (bone meal) | |
| Fleece Trawler | | | | ✓ (lure + 9 outputs) | |
| Fluid Reservoir | | ✓ | | ✓ (buckets) | ✓ |
| Glow Panel, Static Generator, Echo Chamber, Zero Point Module | ✓ | | | | |
| Wool Burner, Blaze Furnace, Sheep Treadmill | ✓ | | | ✓ (fuel / boost / food) | |
| Thermal Reaction Chamber | ✓ | ✓ | | ✓ (hot and cold boosts) | |
| Mycelial Generator | ✓ | ✓ | | ✓ | |

## Tests

The GameTests `machine_frame_saves_like_before_and_offers_its_modules` and
`generators_on_the_machine_frame_save_like_before` check that machines on the frame save under their old keys, load
back, keep names and cards on their items, and offer exactly the capabilities of the modules they have.
