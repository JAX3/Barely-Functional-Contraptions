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
 * </ul>
 */
public final class EnergySheep {
    private EnergySheep() {}

    /** Ticks between its drains and charges. */
    public static final int INTERVAL = 10;

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
            // It crackles while it holds charge.
            if (charge(sheep) > 0 && isEnergySheep(sheep) && sheep.getRandom().nextInt(4) == 0) {
                sheep.level().addParticle(ParticleTypes.ELECTRIC_SPARK, sheep.getRandomX(0.6),
                        sheep.getY() + sheep.getBbHeight() * (0.5 + sheep.getRandom().nextDouble() * 0.6), sheep.getRandomZ(0.6), 0, 0.05, 0);
            }
            return;
        }
        if ((sheep.tickCount + sheep.getId()) % INTERVAL != 0 || !(sheep.level() instanceof ServerLevel level) || !isEnergySheep(sheep)) return;
        drain(level, sheep);
        share(level, sheep);
    }

    /** Draws FE from the Hume Energy Cubes near it, up to {@code drainPerTick} a tick and what its fleece has room for. */
    public static int drain(ServerLevel level, Sheep sheep) {
        int room = capacity() - charge(sheep);
        int budget = Math.min(room, ModConfigs.getInt(ModConfigs.SERVER.energySheepDrainPerTick) * INTERVAL);
        if (budget <= 0) return 0;
        int drawn = 0;
        for (BlockEntity be : blockEntitiesNear(level, sheep.position(), ModConfigs.getInt(ModConfigs.SERVER.energySheepDrainRadius))) {
            if (!(be instanceof EnergyCubeBlockEntity cube)) continue;
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
        for (BlockEntity be : blockEntitiesNear(level, sheep.position(), radius)) {
            if (be instanceof EnergyCubeBlockEntity || be instanceof EnergyCableBlockEntity) continue;
            EnergyHandler handler = blockHandler(level, be.getBlockPos());
            if (handler == null) continue;
            targets.add(handler);
            where.add(Vec3.atCenterOf(be.getBlockPos()));
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
