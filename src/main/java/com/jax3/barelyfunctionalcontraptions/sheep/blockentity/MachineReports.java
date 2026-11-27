package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import java.util.Locale;
import net.minecraft.world.item.ItemStack;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * What a Machine Status Panel shows for each machine linked to it: a status word, how it's doing (running, waiting, a
 * problem, or idle, which sets its colour and whether it counts for the panel's comparator), and one key number that
 * suits the machine (FE/t for a generator, progress or stored energy for the rest). Read from the machines' own status,
 * the same one their windows and Jade show, so nothing is worked out twice.
 */
public final class MachineReports {
    private MachineReports() {}

    /** How a machine is doing. */
    public enum Level {
        RUNNING(0xFF55D66B), WAITING(0xFFE8B23A), PROBLEM(0xFFE35D5D), IDLE(0xFF9AA0A6);

        public final int color;

        Level(int color) {
            this.color = color;
        }

        public static Level byId(int id) {
            Level[] all = values();
            return all[Math.floorMod(id, all.length)];
        }
    }

    /** One machine's line: its status word (a translation key), how it's doing, and its key number. */
    public record Report(String status, Level level, String number) {
        public Component statusText() {
            return Component.translatable("status_panel.barelyfunctionalcontraptions.state." + status);
        }
    }

    /** Status names that mean a problem someone should look at. */
    private static final Set<String> PROBLEMS = Set.of("no_energy", "output_full", "invalid_input", "missing_catalysts", "no_sheep_altar",
            "not_enough_items", "no_source", "no_water", "overheated", "frozen", "too_loud", "shrieked", "needs_combs", "overcharged", "hungry",
            "no_fuel", "no_ground", "overgrown", "missing", "crate_full", "crate_missing", "could_burst", "no_sheep", "tangled", "nothing_connected");
    /** ... that mean it's working. */
    private static final Set<String> RUNNING = Set.of("processing", "infusing", "reacting", "listening", "charging", "walking", "tending",
            "filling", "burning", "boosted", "running", "counting", "hauling", "delivering", "cracking");
    /** ... that mean it's fine but waiting on something. */
    private static final Set<String> WAITING = Set.of("full", "energy_full", "redstone_disabled", "too_quiet", "asleep", "waking", "resting",
            "all_green", "running_hot", "running_cold", "not_infinite", "cold", "nearly_full", "no_empty_crates", "no_crates", "wasting", "regrowing", "jammed", "frosted");

    private static Report of(Enum<?> status, String number) {
        return of(status.name().toLowerCase(Locale.ROOT), number);
    }

    private static Report of(String status, String number) {
        Level level = PROBLEMS.contains(status) ? Level.PROBLEM : RUNNING.contains(status) ? Level.RUNNING
                : WAITING.contains(status) ? Level.WAITING : Level.IDLE;
        return new Report(status, level, number);
    }

    private static String fe(int perTick) {
        return perTick >= 10_000 ? String.format(Locale.ROOT, "%.1fk FE/t", perTick / 1000.0) : perTick + " FE/t";
    }

    private static String percent(long amount, long capacity) {
        return (capacity <= 0 ? 0 : amount * 100 / capacity) + "%";
    }

    private static String stored(EnergyHandler energy) {
        return percent(energy.getAmountAsLong(), energy.getCapacityAsLong());
    }

    /**
     * The block that holds a machine's block entity, from any of its blocks: a Static Generator's base from its dome, an
     * Echo Chamber's core from any of its 27 blocks, a Hume Field Generator's bottom north-west block from any of its 12, a
     * Sheep Treadmill's back from its front. Anything else: itself.
     */
    public static net.minecraft.core.BlockPos machinePos(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        var state = level.getBlockState(pos);
        var block = state.getBlock();
        if (block instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock) {
            return com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock.base(pos, state);
        }
        if (block instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock) {
            return com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock.core(pos, state);
        }
        if (block instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock) {
            return com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock.origin(pos, state);
        }
        if (block instanceof com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock) {
            return com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock.back(pos, state);
        }
        return pos;
    }

    /** Whether a panel can show {@code be}. */
    public static boolean supports(@Nullable BlockEntity be) {
        return report(be) != null;
    }

    /** {@code be}'s line, or null if a panel can't show it. */
    public static @Nullable Report report(@Nullable BlockEntity be) {
        if (be == null) return null;
        // Generators: their status, and the FE/t they make.
        if (be instanceof BlazeFurnaceBlockEntity b) {
            String status = b.generating() <= 0 ? "cold" : b.getEnergyHandler().getAmountAsLong() >= b.getEnergyHandler().getCapacityAsLong() ? "full" : "burning";
            return of(status, fe(b.generating()));
        }
        if (be instanceof WoolBurnerBlockEntity b) return of(b.generating() > 0 ? "burning" : "idle", fe(b.generating()));
        if (be instanceof ThermalReactionChamberBlockEntity b) return of(b.state(), fe(b.generating()));
        if (be instanceof EchoChamberBlockEntity b) return of(b.state(), fe(b.generating()));
        // Its output is random from tick to tick, so the panel shows its average over the last second.
        if (be instanceof HumeFieldGeneratorBlockEntity b) return of(b.status(), fe(b.average()));
        if (be instanceof StaticGeneratorBlockEntity b) return of(b.state(), fe(b.generating()));
        if (be instanceof SheepTreadmillBlockEntity b) return of(b.state(), fe(b.generating()));
        if (be instanceof GlowPanelBlockEntity b) return of(b.generating() > 0 ? "running" : "idle", fe(b.generating()));
        if (be instanceof com.jax3.barelyfunctionalcontraptions.mycelial.blockentity.MycelialGeneratorBlockEntity b) {
            var energy = b.getEnergyStorage();
            return of(b.getState(), percent(energy.getAmountAsLong(), energy.getCapacityAsLong()));
        }
        // Machines: their status, and their progress or stored energy.
        if (be instanceof PoweredFurnaceBlockEntity b) return of(b.status(), stored(b.getEnergyHandler()));
        if (be instanceof ChillerBlockEntity b) {
            // (The chiller has no power: its "no energy" means it's out of water.)
            return of(b.status() == MachineStatus.NO_ENERGY ? "no_water" : b.status().name().toLowerCase(Locale.ROOT), b.output().getCount() + " Ice");
        }
        if (be instanceof MechanicalSeparatorBlockEntity b) return of(b.getStatus(), percent(b.getEnergy().getEnergy(), b.getEnergy().getCapacityAsInt()));
        if (be instanceof AutomaticShearsBlockEntity b) {
            int wool = 0;
            for (int i = 0; i < b.getContainerSize(); i++) wool += b.getItem(i).getCount();
            return of(b.getStatus(), wool + " wool");
        }
        if (be instanceof WoolPackerBlockEntity b) return of(b.status(), b.progress() + "%");
        if (be instanceof InfusionAltarBlockEntity b) {
            String status = b.status() == AltarStatus.NO_SHEEP ? "no_sheep_altar" : b.status().name().toLowerCase(Locale.ROOT);
            return of(status, b.progress() + "%");
        }
        if (be instanceof FluidReservoirBlockEntity b) return of(b.getStatus(), b.getTank().getAmountAsInt(0) + " mB");
        if (be instanceof GrassSeederBlockEntity b) return of(b.status(), b.grass() + "/" + (b.grass() + b.dirt()));
        if (be instanceof ImplosionChamberBlockEntity b) {
            String status = switch (b.status()) {
                case CRACKING -> "cracking";
                case WAITING -> "regrowing";
                case NO_SHEEP -> "no_sheep";
                case IDLE -> "idle";
                case FULL -> "output_full";
                case NO_POWER -> "no_energy";
            };
            long n = b.crackedTotal();
            return of(status, (n >= 10_000 ? String.format(Locale.ROOT, "%.1fk", n / 1000.0) : Long.toString(n)) + " cracked");
        }
        if (be instanceof HerdCounterBlockEntity b) {
            return of("counting", b.mode() == HerdCounterBlockEntity.Mode.FULL ? b.value() + "/" + b.target() : Integer.toString(b.value()));
        }
        // Storage and meters: how full, or the FE/t passing.
        if (be instanceof FleeceTrawlerBlockEntity b) return of(b.statusKey(), b.progressPercent() + "%");
        if (be instanceof CrateControllerBlockEntity b) {
            String status = b.missing() > 0 ? "crate_missing" : b.risky() > 0 ? "could_burst" : b.crateCount() == 0 ? "no_crates"
                    : b.percent() >= 90 ? "nearly_full" : b.empties() == 0 ? "no_empty_crates" : "stored";
            return of(status, b.percent() + "%");
        }
        if (be instanceof CrateBlockEntity b) {
            String status = b.count() <= 0 ? "empty" : b.isFull() ? "crate_full" : "stored";
            return of(status, b.count() >= 10_000 ? String.format(Locale.ROOT, "%.1fk", b.count() / 1000.0) : Integer.toString(b.count()));
        }
        if (be instanceof EnergyCubeBlockEntity b) return of("stored", stored(b.getEnergyHandler()));
        if (be instanceof EnergyMeterBlockEntity b) return of(b.rate() > 0 ? "running" : "idle", fe(b.rate()));
        if (be instanceof ZeroPointEnergyModuleBlockEntity b) return of("stored", stored(b.getEnergyHandler()));
        return null;
    }
}
