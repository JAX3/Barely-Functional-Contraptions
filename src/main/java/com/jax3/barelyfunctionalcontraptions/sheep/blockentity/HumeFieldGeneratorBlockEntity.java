package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Hume Field Generator's workings, held by its bottom north-west block.
 *
 * <ul>
 *     <li>Every tick its field makes a <b>random</b> amount of FE between {@code [hume_field_generator] minPerTick} and
 *     {@code maxPerTick}.</li>
 *     <li>It has <b>no energy buffer</b>: that tick's FE goes straight into whatever touches any of its twelve blocks
 *     (cables, cubes, machines), and whatever they don't take is lost. Nothing can be pulled from it.</li>
 *     <li>It offers the FE to its neighbours in turn, starting with a different one each tick, so no one of them always
 *     gets first pick.</li>
 *     <li>Every {@code pulseInterval} ticks its field <b>pulses</b> (a burst of purple particles and a deep hum), giving
 *     every player within {@code nauseaRadius} blocks of its middle {@code nauseaSeconds} of Nausea. Spectators are
 *     spared.</li>
 * </ul>
 *
 * It's on the machine frame with no modules switched on: nothing to save, store or connect.
 */
public class HumeFieldGeneratorBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame
        implements net.minecraft.world.MenuProvider {
    /** How many ticks its window's trace shows, and how many its average is over. */
    public static final int HISTORY = 28, AVERAGE_OVER = 20;
    /** Values synced to the window: generated, delivered, average, min and max (two shorts each), status, receivers, the trace. */
    public static final int DATA_COUNT = 13 + HISTORY;
    /** Where the trace starts in its synced values. */
    public static final int TRACE_DATA = 13;

    /** How its power is going, for the window, Jade and the status lamp. */
    public enum Status {
        /** Nothing touching it takes power: all it makes is lost. */
        NOTHING_CONNECTED(0xFFE35D5D),
        /** Its neighbours took some, the rest was lost (they're full, or can't take that much a tick). */
        WASTING(0xFFE8B23A),
        /** Its neighbours took everything it made. */
        DELIVERING(0xFF55D66B),
        /** Its field makes nothing (both ends of its range are 0). */
        IDLE(0xFFAEB4C6);

        public final int color;

        Status(int color) {
            this.color = color;
        }

        public static Status byId(int id) {
            Status[] all = values();
            return id >= 0 && id < all.length ? all[id] : NOTHING_CONNECTED;
        }
    }

    /** Each block touching the machine, relative to this block, and the face of it the machine touches. */
    private static final List<Neighbour> NEIGHBOURS = neighbours();

    private record Neighbour(BlockPos offset, Direction side) {}

    private int generated;
    private int delivered;
    private long totalDelivered;
    private int start;
    private int pulses;
    private int receivers;
    private Status status = Status.NOTHING_CONNECTED;
    /** The last {@link #HISTORY} ticks, oldest first from {@link #head}: what it made and what was taken. */
    private final int[] madeHistory = new int[HISTORY], takenHistory = new int[HISTORY];
    private int head;
    /** Ticks until its field next pulses. */
    private int untilPulse = -1;

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            if (index >= TRACE_DATA) return sample(index - TRACE_DATA);
            return switch (index) {
                case 0 -> generated & 0xFFFF;
                case 1 -> generated >>> 16;
                case 2 -> delivered & 0xFFFF;
                case 3 -> delivered >>> 16;
                case 4 -> average() & 0xFFFF;
                case 5 -> average() >>> 16;
                case 6 -> rangeMin() & 0xFFFF;
                case 7 -> rangeMin() >>> 16;
                case 8 -> rangeMax() & 0xFFFF;
                case 9 -> rangeMax() >>> 16;
                case 10 -> status.ordinal();
                case 11 -> receivers;
                case 12 -> Math.min(Short.MAX_VALUE, Math.max(0, untilPulse));
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative; the client-side menu keeps its own SimpleContainerData copy.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public HumeFieldGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HUME_FIELD_GENERATOR.get(), pos, state);
    }

    private static List<Neighbour> neighbours() {
        List<Neighbour> list = new ArrayList<>();
        for (int part = 0; part < 4; part++) {
            for (int layer = 0; layer < HumeFieldGeneratorBlock.LAYERS; layer++) {
                BlockPos inside = new BlockPos(part & 1, layer, part >> 1);
                for (Direction dir : Direction.values()) {
                    BlockPos out = inside.relative(dir);
                    boolean ownBlock = out.getX() >= 0 && out.getX() <= 1 && out.getZ() >= 0 && out.getZ() <= 1
                            && out.getY() >= 0 && out.getY() < HumeFieldGeneratorBlock.LAYERS;
                    if (!ownBlock) list.add(new Neighbour(out, dir.getOpposite()));
                }
            }
        }
        return List.copyOf(list);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, HumeFieldGeneratorBlockEntity be) {
        be.generated = roll(level);
        be.receivers = 0;
        be.delivered = be.generated > 0 ? be.offer(level, pos, be.generated) : 0;
        if (be.generated <= 0) be.receivers = be.countReceivers(level, pos);
        be.totalDelivered += be.delivered;
        be.status = be.generated <= 0 ? Status.IDLE : be.receivers == 0 ? Status.NOTHING_CONNECTED
                : be.delivered < be.generated ? Status.WASTING : Status.DELIVERING;
        be.tickPulse(level, pos);
        be.madeHistory[be.head] = be.generated;
        be.takenHistory[be.head] = be.delivered;
        be.head = (be.head + 1) % HISTORY;
    }

    /** Counts down to its field's next pulse, and pulses when it gets there (never, with {@code pulseInterval} 0). */
    private void tickPulse(ServerLevel level, BlockPos origin) {
        int interval = ModConfigs.getInt(ModConfigs.SERVER.humeFieldPulseInterval);
        if (interval <= 0) {
            untilPulse = 0;
            return;
        }
        if (untilPulse < 0 || untilPulse > interval) untilPulse = interval;
        if (--untilPulse <= 0) {
            pulse(level, origin);
            untilPulse = interval;
        }
    }

    /** The middle of the machine, where its pulse comes from. */
    public static net.minecraft.world.phys.Vec3 centre(BlockPos origin) {
        return new net.minecraft.world.phys.Vec3(origin.getX() + 1.0, origin.getY() + 1.4, origin.getZ() + 1.0);
    }

    /**
     * Its field pulses: purple particles burst from its core, it hums, and every player (but spectators) within
     * {@code nauseaRadius} blocks of its middle gets {@code nauseaSeconds} of Nausea. Returns how many players it reached.
     */
    public int pulse(ServerLevel level, BlockPos origin) {
        var centre = centre(origin);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, centre.x, centre.y, centre.z, 60, 0.9, 1.1, 0.9, 0.08);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, centre.x, centre.y + 1.2, centre.z, 16, 0.6, 0.3, 0.6, 0.02);
        level.playSound(null, origin.offset(1, 1, 1), net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.BLOCKS,
                1.2F, 0.55F + level.getRandom().nextFloat() * 0.1F);
        pulses++;
        return nauseate(level.players(), centre);
    }

    /**
     * Gives {@code nauseaSeconds} of Nausea to each of {@code players} (but spectators) within {@code nauseaRadius} blocks
     * of {@code centre}, one pulse's worth. Returns how many it reached.
     */
    public static int nauseate(Iterable<? extends net.minecraft.world.entity.player.Player> players, net.minecraft.world.phys.Vec3 centre) {
        int radius = ModConfigs.getInt(ModConfigs.SERVER.humeFieldNauseaRadius);
        if (radius <= 0) return 0;
        int ticks = ModConfigs.getInt(ModConfigs.SERVER.humeFieldNauseaSeconds) * 20;
        int reached = 0;
        for (var player : players) {
            if (player.isSpectator() || player.distanceToSqr(centre) > (double) radius * radius) continue;
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NAUSEA, ticks, 0, false, true, true));
            reached++;
        }
        return reached;
    }

    /** The configured range's ends, the smaller first. */
    public static int rangeMin() {
        return Math.min(ModConfigs.getInt(ModConfigs.SERVER.humeFieldMinPerTick), ModConfigs.getInt(ModConfigs.SERVER.humeFieldMaxPerTick));
    }

    public static int rangeMax() {
        return Math.max(ModConfigs.getInt(ModConfigs.SERVER.humeFieldMinPerTick), ModConfigs.getInt(ModConfigs.SERVER.humeFieldMaxPerTick));
    }

    /** How many blocks touching it take power, without offering any. */
    private int countReceivers(ServerLevel level, BlockPos origin) {
        int found = 0;
        for (Neighbour n : NEIGHBOURS) {
            BlockPos at = origin.offset(n.offset());
            if (level.isLoaded(at) && level.getCapability(Capabilities.Energy.BLOCK, at, n.side()) != null) found++;
        }
        return found;
    }

    /**
     * Tick {@code i} of the trace (0 the oldest) packed in 16 bits for the window: the high byte what it made as a
     * share of the range's top (0-255), the low byte the share of that its neighbours took (0-255).
     */
    private int sample(int i) {
        int at = (head + i) % HISTORY;
        int made = madeHistory[at], taken = takenHistory[at];
        int top = Math.max(1, rangeMax());
        int height = Math.min(255, (int) Math.round(255.0 * made / top));
        int share = made <= 0 ? 0 : Math.min(255, (int) Math.round(255.0 * taken / made));
        return (height << 8) | share;
    }

    /** FE/t it made on average over the last {@link #AVERAGE_OVER} ticks. */
    public int average() {
        long sum = 0;
        for (int i = 0; i < AVERAGE_OVER; i++) sum += madeHistory[Math.floorMod(head - 1 - i, HISTORY)];
        return (int) (sum / AVERAGE_OVER);
    }

    /** This tick's FE: a random amount in the configured range (either way round). */
    private static int roll(ServerLevel level) {
        int a = ModConfigs.getInt(ModConfigs.SERVER.humeFieldMinPerTick), b = ModConfigs.getInt(ModConfigs.SERVER.humeFieldMaxPerTick);
        return level.getRandom().nextIntBetweenInclusive(Math.min(a, b), Math.max(a, b));
    }

    /** Offers {@code amount} FE to the blocks touching the machine in turn; how much they took (the rest is lost). */
    private int offer(ServerLevel level, BlockPos origin, int amount) {
        int left = amount;
        int count = NEIGHBOURS.size();
        start = (start + 1) % count;
        for (int i = 0; i < count; i++) {
            Neighbour n = NEIGHBOURS.get((start + i) % count);
            BlockPos at = origin.offset(n.offset());
            if (!level.isLoaded(at)) continue;
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, at, n.side());
            if (target == null) continue;
            receivers++; // counted even once it's all gone, for the window
            if (left <= 0) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int taken = target.insert(left, tx);
                tx.commit();
                left -= Math.max(0, taken);
            }
        }
        return amount - left;
    }

    // ------------------------------------------------------------------ readouts

    /** FE its field made on the last tick. */
    public int generated() {
        return generated;
    }

    /** FE its neighbours took on the last tick (the rest of {@link #generated} was lost). */
    public int delivered() {
        return delivered;
    }

    /** FE lost on the last tick: made, but nothing took it. */
    public int lost() {
        return generated - delivered;
    }

    /** How many of the blocks touching it take power. */
    public int receivers() {
        return receivers;
    }

    public Status status() {
        return status;
    }

    /** Ticks until its field next pulses (0 if it never does). */
    public int untilPulse() {
        return Math.max(0, untilPulse);
    }

    /** How many times its field has pulsed since it was loaded. */
    public int pulses() {
        return pulses;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.hume_field_generator");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
                                                                         net.minecraft.world.entity.player.Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.HumeFieldGeneratorMenu(containerId, inventory, this, data);
    }

    /** FE its neighbours have taken since it was loaded. */
    public long totalDelivered() {
        return totalDelivered;
    }
}
