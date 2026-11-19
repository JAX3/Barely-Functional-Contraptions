package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps a machine's sheep close: the Blaze Sheep Furnace, Powered Furnace, Snow Sheep Chiller and Thermal Reaction
 * Chamber each call the sheep they use back into their working area when one wanders a little way out of it.
 *
 * <ul>
 *     <li>The <b>attraction area</b> is the working area grown by {@code [sheep_attraction] range} blocks on every
 *     side (and one up and down). A matching sheep inside it but outside the working area walks back in, to a random
 *     spot in the area, as a sheep follows wheat.</li>
 *     <li>A machine only calls as many as it still has room for, so it never steals sheep it wouldn't use.</li>
 *     <li>A sheep standing in any of these machines' working areas is <b>claimed</b> for a couple of seconds, so two
 *     machines side by side don't pull the same sheep back and forth.</li>
 *     <li>Sheep on a lead, riding something, or lambs are left alone.</li>
 * </ul>
 */
public final class SheepAttraction {
    /** How long, in ticks, a sheep seen in a machine's area stays claimed by it. */
    private static final int CLAIM_TICKS = 40;
    private static final Map<UUID, Long> CLAIMED = new HashMap<>();
    private static long lastPrune;

    private SheepAttraction() {}

    public static boolean enabled() {
        return ModConfigs.getBool(ModConfigs.SERVER.attractionEnabled) && range() > 0;
    }

    public static int range() {
        return ModConfigs.getInt(ModConfigs.SERVER.attractionRange);
    }

    /** The attraction area round a working area. */
    public static AABB reach(AABB area) {
        int r = range();
        return area.inflate(r, 1, r);
    }

    /** Marks the sheep a machine found in its working area as its own for a moment. */
    public static void claim(Collection<? extends Entity> sheep, long now) {
        for (Entity entity : sheep) CLAIMED.put(entity.getUUID(), now + CLAIM_TICKS);
    }

    private static boolean claimed(Entity entity, long now) {
        Long until = CLAIMED.get(entity.getUUID());
        return until != null && until >= now;
    }

    /**
     * Calls up to {@code wanted} sheep that match, standing in the attraction area but outside {@code area} and not
     * claimed by another machine, back into the area; the nearest first. Returns how many it called.
     */
    public static int attract(ServerLevel level, AABB area, Predicate<Sheep> matches, int wanted, long now) {
        if (wanted <= 0 || !enabled()) return 0;
        if (now - lastPrune > 200) {
            CLAIMED.values().removeIf(until -> until < now);
            lastPrune = now;
        }
        Vec3 centre = area.getCenter();
        List<Sheep> strays = level.getEntitiesOfClass(Sheep.class, reach(area), s -> s.isAlive() && !s.isBaby() && !s.isLeashed()
                        && !s.isPassenger() && !area.contains(s.position()) && !claimed(s, now) && matches.test(s)).stream()
                .sorted(Comparator.comparingDouble(s -> s.position().distanceToSqr(centre)))
                .limit(wanted).toList();
        for (Sheep sheep : strays) {
            // A spot inside the area, half a block in from its edge, on its floor.
            double x = area.minX + 0.5 + sheep.getRandom().nextDouble() * Math.max(0, area.getXsize() - 1);
            double z = area.minZ + 0.5 + sheep.getRandom().nextDouble() * Math.max(0, area.getZsize() - 1);
            sheep.getNavigation().moveTo(x, area.minY, z, 1.0);
            sheep.getLookControl().setLookAt(x, area.minY + 0.5, z);
        }
        return strays.size();
    }
}
