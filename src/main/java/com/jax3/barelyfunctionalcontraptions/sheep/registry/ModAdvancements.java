package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * The mod's advancement tab (data/barelyfunctionalcontraptions/advancement/contraptions/). Crafting each machine uses
 * vanilla's inventory_changed trigger; the three special advancements have a single {@code minecraft:impossible}
 * criterion named "done" and are granted from code here:
 * <ul>
 *     <li>{@link #LINK_GENERATORS}: linking two Mycelial Generators with the Barely Functional Wrench;</li>
 *     <li>{@link #AUTO_SHEAR}: Automatic Shears shearing a sheep within {@link #AUTO_SHEAR_RADIUS} blocks of you;</li>
 *     <li>{@link #ZERO_POINT_DEATH}: being killed by a Zero Point beam.</li>
 * </ul>
 */
public final class ModAdvancements {
    public static final String LINK_GENERATORS = "contraptions/link_generators";
    public static final String AUTO_SHEAR = "contraptions/auto_shear";
    public static final String ZERO_POINT_DEATH = "contraptions/zero_point_death";
    /** Players this close to Automatic Shears when they shear a sheep get {@link #AUTO_SHEAR}. */
    public static final double AUTO_SHEAR_RADIUS = 16;

    private ModAdvancements() {}

    /** Grants every remaining criterion of the mod's advancement {@code path}. Does nothing if it isn't loaded. */
    public static void award(ServerPlayer player, String path) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(ResourcefulSheep.id(path));
        if (advancement == null) return;
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    public static boolean isDone(ServerPlayer player, String path) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(ResourcefulSheep.id(path));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** Grants {@code path} to every player within {@code radius} blocks of {@code pos} in that level. */
    public static void awardNearby(ServerLevel level, BlockPos pos, double radius, String path) {
        AABB area = new AABB(pos).inflate(radius);
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && area.contains(player.position())) award(player, path);
        }
    }

    /** A player killed by a Zero Point beam earns {@link #ZERO_POINT_DEATH}. */
    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player && source.is(ZeroPointEnergyModuleBlockEntity.BEAM_DAMAGE)) {
            award(player, ZERO_POINT_DEATH);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        onDeath(event.getEntity(), event.getSource());
    }
}
