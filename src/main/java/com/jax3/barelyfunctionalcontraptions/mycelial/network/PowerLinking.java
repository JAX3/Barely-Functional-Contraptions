package com.jax3.barelyfunctionalcontraptions.mycelial.network;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorEnergyStorage;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlocks;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Wireless power with the Mycelium Wrench: a generator linked to a machine sends it FE as if it were touching it.
 * Which blocks can receive power this way is the block tag {@link #RECEIVERS} (the Mechanical Separator and the
 * Automatic Shears by default). Links use the wrench's distance limit, stay in one dimension, and are limited per
 * generator by {@code max_power_links_per_generator}.
 */
public final class PowerLinking {
    public static final TagKey<Block> RECEIVERS = TagKey.create(Registries.BLOCK, MycelialPower.id("wireless_power_receivers"));

    public enum Result {
        SUCCESS, DISABLED, NOT_LOADED, NOT_LINKABLE, ALREADY_LINKED, DIFFERENT_DIMENSION, TOO_FAR, AT_LIMIT;

        public boolean success() {
            return this == SUCCESS;
        }

        public String translationKey() {
            return "message.barelyfunctionalcontraptions.power." + name().toLowerCase(Locale.ROOT);
        }
    }

    private PowerLinking() {
    }

    public static boolean isGenerator(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.MYCELIAL_GENERATOR.get());
    }

    public static boolean isReceiver(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(RECEIVERS);
    }

    /** Links a generator and a machine, given in either order. */
    public static Result tryLink(MinecraftServer server, GlobalPos first, GlobalPos second) {
        if (!ServerConfig.get(ServerConfig.WRENCH_ENABLED)) return Result.DISABLED;
        ServerLevel firstLevel = server.getLevel(first.dimension());
        ServerLevel secondLevel = server.getLevel(second.dimension());
        if (firstLevel == null || secondLevel == null || !firstLevel.isLoaded(first.pos()) || !secondLevel.isLoaded(second.pos())) {
            return Result.NOT_LOADED;
        }
        GlobalPos generator, machine;
        if (isGenerator(firstLevel, first.pos()) && isReceiver(secondLevel, second.pos())) {
            generator = first;
            machine = second;
        } else if (isReceiver(firstLevel, first.pos()) && isGenerator(secondLevel, second.pos())) {
            generator = second;
            machine = first;
        } else {
            return Result.NOT_LINKABLE;
        }
        PowerLinkData data = PowerLinkData.get(server);
        if (data.isLinked(generator, machine)) return Result.ALREADY_LINKED;
        if (!generator.dimension().equals(machine.dimension())) return Result.DIFFERENT_DIMENSION;
        if (!withinDistance(generator, machine)) return Result.TOO_FAR;
        if (data.machinesOf(generator).size() >= ServerConfig.get(ServerConfig.MAX_POWER_LINKS)) return Result.AT_LIMIT;
        data.add(generator, machine);
        ManualLinking.showLinkEffect(firstLevel, generator.pos(), machine.pos());
        return Result.SUCCESS;
    }

    private static boolean withinDistance(GlobalPos a, GlobalPos b) {
        return LinkRules.withinDistance(a.pos().getX(), a.pos().getY(), a.pos().getZ(), b.pos().getX(), b.pos().getY(), b.pos().getZ(),
                ServerConfig.get(ServerConfig.MAX_LINK_DISTANCE));
    }

    /** Whether a link would carry power right now: wrench enabled, same dimension, loaded, still a machine, in range. */
    public static boolean isActive(ServerLevel level, GlobalPos generator, GlobalPos machine) {
        return ServerConfig.get(ServerConfig.WRENCH_ENABLED) && machine.dimension().equals(level.dimension())
                && level.isLoaded(machine.pos()) && isReceiver(level, machine.pos()) && withinDistance(generator, machine);
    }

    /** Removes every wireless power link of a generator or machine. Returns how many were removed. */
    public static int unlinkAll(MinecraftServer server, GlobalPos pos) {
        return PowerLinkData.get(server).removeAll(pos);
    }

    /**
     * Sends up to {@code budget} FE from {@code source} to the machines {@code generator} is linked to, sharing it
     * evenly between them. Links whose machine is loaded but gone are removed; unloaded machines are skipped and
     * kept. Links that a lowered distance limit or a disabled wrench no longer allows are kept but carry no power.
     * Returns the FE sent.
     */
    public static int push(ServerLevel level, BlockPos generatorPos, GeneratorEnergyStorage source, int budget) {
        if (budget <= 0 || !ServerConfig.get(ServerConfig.WRENCH_ENABLED)) return 0;
        GlobalPos generator = GlobalPos.of(level.dimension(), generatorPos);
        PowerLinkData data = PowerLinkData.get(level.getServer());
        List<GlobalPos> machines = List.copyOf(data.machinesOf(generator));
        if (machines.isEmpty()) return 0;
        // Share what can actually be sent this time (the budget, or less if the buffer holds less).
        int available = Math.min(budget, source.getAmountAsInt());
        int sent = 0;
        for (int i = 0; i < machines.size(); i++) {
            GlobalPos machine = machines.get(i);
            BlockPos pos = machine.pos();
            if (!machine.dimension().equals(level.dimension()) || !level.isLoaded(pos)) continue;
            if (!isReceiver(level, pos)) {
                data.remove(generator, machine); // broken or replaced
                continue;
            }
            if (!withinDistance(generator, machine)) continue;
            @Nullable EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, pos, null);
            if (target == null || target == source) continue;
            // An even share of what is left, so one hungry machine does not starve the rest.
            int share = (available - sent) / (machines.size() - i);
            int offer = Math.min(Math.max(share, 1), Math.min(available - sent, source.getAmountAsInt()));
            if (offer <= 0) break;
            int accepted;
            // The machine's insert is transactional; the generator's buffer is only reduced once it has committed.
            try (Transaction transaction = Transaction.openRoot()) {
                accepted = Math.max(0, Math.min(offer, target.insert(offer, transaction)));
                transaction.commit();
            }
            if (accepted > 0) sent += source.extractInternal(accepted);
        }
        return sent;
    }
}
