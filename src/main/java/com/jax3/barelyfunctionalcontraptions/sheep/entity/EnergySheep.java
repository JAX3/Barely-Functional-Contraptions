package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCableBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Energy sheep (any sheep whose resource has the {@link Feature#CHARGED charged} feature): a wireless charger on
 * legs.
 *
 * <ul>
 *     <li><b>Drains:</b> every {@link #INTERVAL} ticks it draws up to {@code drainPerTick} FE a tick from the Hume
 *     Energy Cubes within {@code drainRadius} (3) blocks of it into its fleece, which holds {@code capacity} (100,000) FE.</li>
 *     <li><b>Charges:</b> it shares up to {@code chargePerTick} FE a tick out between the machines (any block that
 *     takes FE, but not Hume Energy Cubes or cables) and the players' powered items (tools, armour, anything that holds
 *     FE, in any slot) within {@code chargeRadius} (8) blocks, a fair share each. Sparks arc to whatever it charges.</li>
 *     <li>Only a grown sheep with its fleece on does either; a sheared one keeps its charge until its wool grows back.</li>
 *     <li><b>The wrench:</b> right-click it to pause it (it keeps its charge, and neither drains nor charges) or start
 *     it again; sneak-right-click to outline its reach for {@link #REACH_TICKS} ticks: purple round the cubes it drains,
 *     yellow round what it charges.</li>
 * </ul>
 * What's round it is looked up once every {@link #CACHE_TICKS} ticks (or when it moves), not every time.
 */
public final class EnergySheep {
    private EnergySheep() {}

    /** Ticks between its drains and charges. */
    public static final int INTERVAL = 10;
    /** How long the wrench outlines its reach, in ticks. */
    public static final int REACH_TICKS = 200;
    /** How long it trusts its list of what's round it, in ticks, while it stays put. */
    public static final int CACHE_TICKS = 100;

    /** What's round a sheep: the cubes it can drain and the blocks it can charge, found at {@code at} at {@code time}. */
    private record Nearby(BlockPos at, long time, List<BlockPos> cubes, List<BlockPos> targets) {}

    private static final java.util.Map<Sheep, Nearby> NEARBY = new java.util.WeakHashMap<>();
    /** Sheep showing their reach, and the game time to stop. */
    private static final java.util.Map<Sheep, Long> SHOWING = new java.util.WeakHashMap<>();

    public static boolean isPaused(Entity sheep) {
        return sheep.hasData(ModAttachments.SHEEP_PAUSED) && sheep.getData(ModAttachments.SHEEP_PAUSED);
    }

    public static void setPaused(Entity sheep, boolean paused) {
        if (paused != isPaused(sheep)) sheep.setData(ModAttachments.SHEEP_PAUSED, paused);
    }

    public static boolean isEnergySheep(Entity entity) {
        return SheepFeatureEvents.has(entity, Feature.CHARGED);
    }

    public static int charge(Entity sheep) {
        return sheep.hasData(ModAttachments.SHEEP_CHARGE) ? sheep.getData(ModAttachments.SHEEP_CHARGE) : 0;
    }

    public static void setCharge(Entity sheep, int charge) {
        if (charge != charge(sheep)) sheep.setData(ModAttachments.SHEEP_CHARGE, Math.max(0, charge));
    }

    public static int capacity() {
        return ModConfigs.getInt(ModConfigs.SERVER.energySheepCapacity);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Sheep sheep)) return;
        if (sheep.level().isClientSide()) {
            // It crackles while it holds charge: hardly at all nearly empty, every tick or two when full.
            int charge = charge(sheep);
            if (charge > 0 && !isPaused(sheep) && isEnergySheep(sheep) && sheep.getRandom().nextFloat() < 0.05F + 0.55F * Math.min(1F, charge / (float) capacity())) {
                sheep.level().addParticle(ParticleTypes.ELECTRIC_SPARK, sheep.getRandomX(0.6),
                        sheep.getY() + sheep.getBbHeight() * (0.5 + sheep.getRandom().nextDouble() * 0.6), sheep.getRandomZ(0.6), 0, 0.05, 0);
            }
            return;
        }
        if (!(sheep.level() instanceof ServerLevel level)) return;
        Long until = SHOWING.get(sheep);
        if (until != null) {
            if (level.getGameTime() >= until || !isEnergySheep(sheep)) SHOWING.remove(sheep);
            else if (level.getGameTime() % 10 == 0) showReach(level, sheep);
        }
        if ((sheep.tickCount + sheep.getId()) % INTERVAL != 0 || !isEnergySheep(sheep) || isPaused(sheep)) return;
        drain(level, sheep);
        share(level, sheep);
    }

    /** What's round it, looked up again if it has moved or the list is old. */
    private static Nearby nearby(ServerLevel level, Sheep sheep) {
        Nearby known = NEARBY.get(sheep);
        BlockPos at = sheep.blockPosition();
        if (known != null && known.at().equals(at) && level.getGameTime() - known.time() < CACHE_TICKS) return known;
        int drainRadius = ModConfigs.getInt(ModConfigs.SERVER.energySheepDrainRadius);
        int chargeRadius = ModConfigs.getInt(ModConfigs.SERVER.energySheepChargeRadius);
        List<BlockPos> cubes = new ArrayList<>(), targets = new ArrayList<>();
        for (BlockEntity be : blockEntitiesNear(level, sheep.position(), Math.max(drainRadius, chargeRadius))) {
            BlockPos p = be.getBlockPos();
            int d = Math.max(Math.abs(p.getX() - at.getX()), Math.max(Math.abs(p.getY() - at.getY()), Math.abs(p.getZ() - at.getZ())));
            if (be instanceof EnergyCubeBlockEntity) {
                if (d <= drainRadius) cubes.add(p.immutable());
            } else if (!(be instanceof EnergyCableBlockEntity) && d <= chargeRadius && blockHandler(level, p) != null) {
                targets.add(p.immutable());
            }
        }
        Nearby found = new Nearby(at.immutable(), level.getGameTime(), cubes, targets);
        NEARBY.put(sheep, found);
        return found;
    }

    /** The wrench: right-click pauses it or starts it again; sneak-right-click shows its reach for a while. */
    @SubscribeEvent
    public static void onInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Sheep sheep) || !event.getItemStack().is(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get())) return;
        if (!sheep.hasData(ModAttachments.SHEEP_INFUSION) || SheepInfusionEvents.getInfusedResource(sheep)
                .map(r -> !r.appearance().has(Feature.CHARGED)).orElse(true)) return;
        Player player = event.getEntity();
        if (sheep.level() instanceof ServerLevel level) {
            if (player.isSecondaryUseActive()) {
                boolean show = !SHOWING.containsKey(sheep);
                if (show) {
                    SHOWING.put(sheep, level.getGameTime() + REACH_TICKS);
                    showReach(level, sheep);
                } else {
                    SHOWING.remove(sheep);
                }
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(show
                        ? "message.barelyfunctionalcontraptions.energy_sheep.reach" : "message.barelyfunctionalcontraptions.energy_sheep.reach_hidden",
                        ModConfigs.getInt(ModConfigs.SERVER.energySheepDrainRadius), ModConfigs.getInt(ModConfigs.SERVER.energySheepChargeRadius))
                        .withColor(0xFFE14A));
            } else {
                boolean paused = !isPaused(sheep);
                setPaused(sheep, paused);
                level.playSound(null, sheep, net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, paused ? 0.6F : 0.9F);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(paused
                        ? "message.barelyfunctionalcontraptions.energy_sheep.paused" : "message.barelyfunctionalcontraptions.energy_sheep.running")
                        .withColor(paused ? 0xAAAAAA : 0xFFE14A));
            }
        }
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Shows its reach for {@link #REACH_TICKS} ticks, as a sneak-right-click with the wrench does. */
    public static void showReachFor(ServerLevel level, Sheep sheep) {
        SHOWING.put(sheep, level.getGameTime() + REACH_TICKS);
        showReach(level, sheep);
    }

    /** Dust along the edges of its two reaches: purple, the cubes it drains; yellow, what it charges. */
    private static void showReach(ServerLevel level, Sheep sheep) {
        BlockPos at = sheep.blockPosition();
        outline(level, at, ModConfigs.getInt(ModConfigs.SERVER.energySheepDrainRadius), new net.minecraft.core.particles.DustParticleOptions(0x9A52F0, 1.2F));
        outline(level, at, ModConfigs.getInt(ModConfigs.SERVER.energySheepChargeRadius), new net.minecraft.core.particles.DustParticleOptions(0xFFE14A, 1.4F));
    }

    private static void outline(ServerLevel level, BlockPos at, int radius, net.minecraft.core.particles.ParticleOptions dust) {
        double x0 = at.getX() - radius, y0 = at.getY() - radius, z0 = at.getZ() - radius;
        double size = radius * 2 + 1;
        double[][] corners = {{0, 0, 0}, {1, 0, 0}, {0, 0, 1}, {1, 0, 1}, {0, 1, 0}, {1, 1, 0}, {0, 1, 1}, {1, 1, 1}};
        int[][] edges = {{0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            double[] a = corners[e[0]], b = corners[e[1]];
            int steps = (int) size;
            for (int k = 0; k <= steps; k++) {
                double t = k / (double) steps;
                level.sendParticles(dust, x0 + size * (a[0] + (b[0] - a[0]) * t), y0 + size * (a[1] + (b[1] - a[1]) * t),
                        z0 + size * (a[2] + (b[2] - a[2]) * t), 1, 0, 0, 0, 0);
            }
        }
    }

    /** Draws FE from the Hume Energy Cubes near it, up to {@code drainPerTick} a tick and what its fleece has room for. */
    public static int drain(ServerLevel level, Sheep sheep) {
        int room = capacity() - charge(sheep);
        int budget = Math.min(room, ModConfigs.getInt(ModConfigs.SERVER.energySheepDrainPerTick) * INTERVAL);
        if (budget <= 0) return 0;
        int drawn = 0;
        for (BlockPos p : nearby(level, sheep).cubes()) {
            if (!(level.getBlockEntity(p) instanceof EnergyCubeBlockEntity cube)) continue;
            EnergyHandler handler = cube.getEnergyHandler();
            try (Transaction tx = Transaction.openRoot()) {
                int got = handler.extract(budget - drawn, tx);
                tx.commit();
                drawn += got;
            }
            if (drawn >= budget) break;
        }
        if (drawn > 0) setCharge(sheep, charge(sheep) + drawn);
        return drawn;
    }

    /** Shares its charge out to the machines and players' powered items round it. Returns the FE it gave. */
    public static int share(ServerLevel level, Sheep sheep) {
        int budget = Math.min(charge(sheep), ModConfigs.getInt(ModConfigs.SERVER.energySheepChargePerTick) * INTERVAL);
        if (budget <= 0) return 0;
        int radius = ModConfigs.getInt(ModConfigs.SERVER.energySheepChargeRadius);
        List<EnergyHandler> targets = new ArrayList<>();
        List<Vec3> where = new ArrayList<>();
        for (BlockPos p : nearby(level, sheep).targets()) {
            EnergyHandler handler = level.isLoaded(p) ? blockHandler(level, p) : null; // gone since it looked: skip it
            if (handler == null) continue;
            targets.add(handler);
            where.add(Vec3.atCenterOf(p));
        }
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(sheep.blockPosition()).inflate(radius), p -> !p.isSpectator())) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot).isEmpty()) continue;
                EnergyHandler handler = ItemAccess.forPlayerSlot(player, slot).getCapability(Capabilities.Energy.ITEM);
                if (handler == null) continue;
                targets.add(handler);
                where.add(player.position().add(0, 1, 0));
            }
        }
        if (targets.isEmpty()) return 0;
        // A fair share each; whatever some can't take goes round again to the ones that still can.
        int given = 0;
        boolean[] full = new boolean[targets.size()];
        boolean[] reached = new boolean[targets.size()];
        for (int round = 0; round < 3 && given < budget; round++) {
            int open = 0;
            for (boolean f : full) if (!f) open++;
            if (open == 0) break;
            int share = Math.max(1, (budget - given) / open);
            for (int i = 0; i < targets.size() && given < budget; i++) {
                if (full[i]) continue;
                int offer = Math.min(share, budget - given);
                try (Transaction tx = Transaction.openRoot()) {
                    int took = targets.get(i).insert(offer, tx);
                    tx.commit();
                    given += took;
                    if (took > 0) reached[i] = true;
                    if (took < offer) full[i] = true;
                }
            }
        }
        if (given > 0) {
            setCharge(sheep, charge(sheep) - given);
            // Sparks arc to each thing it charged, now and then.
            if (level.getRandom().nextInt(2) == 0) {
                Vec3 from = sheep.position().add(0, sheep.getBbHeight() * 0.8, 0);
                for (int i = 0; i < where.size(); i++) if (reached[i]) arc(level, from, where.get(i));
            }
        }
        return given;
    }

    /** A line of sparks from {@code from} to {@code to}. */
    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int points = Math.max(2, (int) (step.length() * 2));
        for (int k = 1; k < points; k++) {
            Vec3 p = from.add(step.scale(k / (double) points));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    /** A block's energy handler: from no side in particular if it offers one, else from any side. */
    private static EnergyHandler blockHandler(ServerLevel level, BlockPos pos) {
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, null);
        if (handler != null) return handler;
        for (Direction side : Direction.values()) {
            handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (handler != null) return handler;
        }
        return null;
    }

    /** The block entities within {@code radius} blocks (a cube round it) of {@code centre}, chunk by chunk. */
    private static List<BlockEntity> blockEntitiesNear(ServerLevel level, Vec3 centre, int radius) {
        List<BlockEntity> found = new ArrayList<>();
        BlockPos c = BlockPos.containing(centre);
        for (int cx = (c.getX() - radius) >> 4; cx <= (c.getX() + radius) >> 4; cx++) {
            for (int cz = (c.getZ() - radius) >> 4; cz <= (c.getZ() + radius) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (Math.abs(p.getX() - c.getX()) <= radius && Math.abs(p.getY() - c.getY()) <= radius && Math.abs(p.getZ() - c.getZ()) <= radius) {
                        found.add(be);
                    }
                }
            }
        }
        return found;
    }
}
