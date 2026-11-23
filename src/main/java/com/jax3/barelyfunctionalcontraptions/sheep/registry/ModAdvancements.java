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
 * vanilla's inventory_changed trigger; the special advancements have a single {@code minecraft:impossible}
 * criterion named "done" and are granted from code here:
 * <ul>
 *     <li>{@link #LINK_GENERATORS}: linking two Mycelial Generators with the Barely Functional Wrench;</li>
 *     <li>{@link #AUTO_SHEAR}: Automatic Shears shearing a sheep within {@link #AUTO_SHEAR_RADIUS} blocks of you;</li>
 *     <li>{@link #ZERO_POINT_DEATH}: being killed by a Zero Point beam;</li>
 *     <li>{@link #BLAZE_FURNACE} and {@link #SHEEP_TREADMILL}: that machine starting to make power near you;</li>
 *     <li>{@link #INFUSE_SHEEP}: infusing any sheep, and {@code contraptions/sheep/<name>} for infusing one with the
 *     mod's resource {@code <name>} (the themed sheep have one each; a data pack can add more the same way).</li>
 * </ul>
 * The "every wool" challenge is plain data: an inventory_changed criterion for each built-in resource wool.
 */
public final class ModAdvancements {
    public static final String LINK_GENERATORS = "contraptions/link_generators";
    public static final String AUTO_SHEAR = "contraptions/auto_shear";
    /** A Blaze Sheep Furnace starting to make power from heat within {@link #AUTO_SHEAR_RADIUS} blocks of you. */
    public static final String BLAZE_FURNACE = "contraptions/blaze_furnace";
    public static final String ZERO_POINT_DEATH = "contraptions/zero_point_death";
    /** A sheep starting to walk a Sheep Treadmill within {@link #AUTO_SHEAR_RADIUS} blocks of you. */
    public static final String SHEEP_TREADMILL = "contraptions/sheep_treadmill";
    /** A Thermal Reaction Chamber starting to react within {@link #AUTO_SHEAR_RADIUS} blocks of you. */
    public static final String THERMAL_REACTION_CHAMBER = "contraptions/thermal_reaction_chamber";
    public static final String ECHO_CHAMBER = "contraptions/echo_chamber";
    public static final String STATIC_SHOCK = "contraptions/static_shock";
    /** Being within {@link #AUTO_SHEAR_RADIUS} blocks of a Grass Seeder that has tended 100 blocks. */
    public static final String KEEP_OFF_THE_GRASS = "contraptions/keep_off_the_grass";
    /** Being within {@link #AUTO_SHEAR_RADIUS} blocks of a Herd Counter counting 20 sheep at once. */
    public static final String COUNT_YOUR_SHEEP = "contraptions/count_your_sheep";
    /** Linking a Machine Status Panel's screen to {@link #MISSION_CONTROL_MACHINES} machines. */
    public static final String MISSION_CONTROL = "contraptions/mission_control";
    public static final int MISSION_CONTROL_MACHINES = 8;
    /** Being near a Wool-Lined Crate when it bursts. */
    public static final String BURSTING_AT_THE_SEAMS = "contraptions/bursting_at_the_seams";
    /** Being near a Crate Controller with {@link #DEWEY_DECIMAL_CRATES} crates in its store. */
    public static final String DEWEY_DECIMAL_SHEEP = "contraptions/dewey_decimal_sheep";
    public static final int DEWEY_DECIMAL_CRATES = 16;
    public static final String INFUSE_SHEEP = "contraptions/infuse_sheep";
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

    /** Infusing a sheep: {@link #INFUSE_SHEEP}, plus the resource's own advancement if there is one. */
    public static void onInfused(ServerPlayer player, com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource resource) {
        award(player, INFUSE_SHEEP);
        if (resource.id().getNamespace().equals(ResourcefulSheep.MOD_ID)) award(player, "contraptions/sheep/" + resource.id().getPath());
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        onDeath(event.getEntity(), event.getSource());
    }
}
