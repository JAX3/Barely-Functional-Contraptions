package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Explosive sheep (any sheep whose resource has the {@link Feature#FUSE fuse} feature): a TNT-infused sheep with
 * a fuse on its back.
 *
 * <p>Fire, lava, lightning or flint and steel light its fuse. It hisses like a creeper for {@link #FUSE_TICKS} ticks,
 * then pops: a small blast that breaks no blocks and hurts no one, but gives things nearby a shove and blows its
 * fleece off. The fleece comes off exactly as if it were sheared (NeoForge's {@code IShearable}), so it drops its
 * Explosive Wool and uses up one infusion use. The pop puts out any fire on it. Only a grown sheep with its fleece
 * on has a fuse, so a sheared sheep can't be lit until its wool grows back.</p>
 *
 * <p>Machines light it with {@link #light}, and hear its pops through {@link #addListener}.</p>
 */
public final class ExplosiveSheep {
    private ExplosiveSheep() {}

    /** How long a lit fuse hisses before the sheep pops, in ticks (a creeper's is 30). */
    public static final int FUSE_TICKS = 30;
    /** How far the pop shoves things, in blocks, and how hard. */
    public static final double SHOVE_RADIUS = 3.0, SHOVE_STRENGTH = 0.5;

    /** Something that wants to know when an Explosive sheep pops, e.g. a machine whose chamber catches the blast. */
    @FunctionalInterface
    public interface PopListener {
        /**
         * Called on the server as {@code sheep} pops at {@code at}, with the wool blown off it. A listener may take some
         * or all of it (shrinking the stacks); whatever is left is dropped on the ground.
         */
        void popped(ServerLevel level, Sheep sheep, BlockPos at, List<ItemStack> wool);
    }

    private static final List<PopListener> LISTENERS = new ArrayList<>();
    /** Lit sheep and the game time they pop at. Weak, so a sheep that is unloaded or killed just drops out. */
    private static final Map<Sheep, Long> LIT = new WeakHashMap<>();

    public static void addListener(PopListener listener) {
        LISTENERS.add(listener);
    }

    /** Whether this is a grown Explosive sheep with its fleece on (so it has a fuse). */
    public static boolean hasFuse(Entity entity) {
        return SheepFeatureEvents.has(entity, Feature.FUSE);
    }

    public static boolean isLit(Sheep sheep) {
        return LIT.containsKey(sheep);
    }

    /** Ticks until a lit sheep pops, or -1 if it isn't lit. */
    public static int ticksLeft(Sheep sheep) {
        Long at = LIT.get(sheep);
        return at == null ? -1 : (int) Math.max(0, at - sheep.level().getGameTime());
    }

    /**
     * Lights its fuse, if it has one and it isn't already lit: it hisses, and pops {@link #FUSE_TICKS} ticks later.
     * Returns whether it was lit now.
     */
    public static boolean light(Sheep sheep) {
        return light(sheep, false);
    }

    /** As {@link #light(Sheep)}; {@code quiet} (a Muffler card in the Implosion Chamber that lit it) hushes its hiss and pop. */
    public static boolean light(Sheep sheep, boolean quiet) {
        if (!(sheep.level() instanceof ServerLevel level) || isLit(sheep) || !hasFuse(sheep)) return false;
        LIT.put(sheep, level.getGameTime() + FUSE_TICKS);
        if (quiet) QUIET.put(sheep, Boolean.TRUE);
        else level.playSound(null, sheep, SoundEvents.CREEPER_PRIMED, SoundSource.NEUTRAL, 1.0F, 1.2F);
        return true;
    }

    /** Sheep lit quietly, whose pop makes no bang. */
    private static final Map<Sheep, Boolean> QUIET = new WeakHashMap<>();

    /** Pops it now: the blast, the shove, and its fleece blown off as wool. Returns the wool it dropped. */
    public static List<ItemStack> pop(Sheep sheep) {
        LIT.remove(sheep);
        if (!(sheep.level() instanceof ServerLevel level)) return List.of();
        BlockPos at = sheep.blockPosition();
        Vec3 centre = sheep.position().add(0, sheep.getBbHeight() * 0.5, 0);
        if (QUIET.remove(sheep) == null) level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.6F, 1.5F);
        level.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y, centre.z, 12, 0.4, 0.3, 0.4, 0.02);
        // A shove, but no damage and no broken blocks.
        for (Entity other : level.getEntities(sheep, sheep.getBoundingBox().inflate(SHOVE_RADIUS), e -> e instanceof LivingEntity || e instanceof ItemEntity)) {
            Vec3 away = other.position().subtract(sheep.position());
            double distance = away.length();
            if (distance > SHOVE_RADIUS) continue;
            Vec3 push = (distance < 1.0E-3 ? new Vec3(0, 1, 0) : away.normalize()).scale(SHOVE_STRENGTH * (1 - distance / SHOVE_RADIUS));
            other.push(push.x, push.y + 0.2, push.z);
        }
        sheep.clearFire();
        List<ItemStack> wool = new ArrayList<>();
        ItemStack shears = new ItemStack(Items.SHEARS);
        if (sheep instanceof net.neoforged.neoforge.common.IShearable shearable && shearable.isShearable(null, shears, level, at)) {
            for (ItemStack drop : shearable.onSheared(null, shears, level, at)) wool.add(drop.copy());
        }
        List<ItemStack> blown = wool.stream().map(ItemStack::copy).toList();
        // Listeners (an Implosion Chamber whose pen it's in) may catch the wool; the rest is blown out onto the ground.
        for (PopListener listener : List.copyOf(LISTENERS)) listener.popped(level, sheep, at, wool);
        for (ItemStack left : wool) {
            if (left.isEmpty()) continue;
            ItemEntity item = new ItemEntity(level, centre.x, centre.y, centre.z, left);
            item.setDeltaMovement(level.getRandom().nextGaussian() * 0.15, 0.3 + level.getRandom().nextDouble() * 0.2, level.getRandom().nextGaussian() * 0.15);
            level.addFreshEntity(item);
        }
        return blown;
    }

    /** Fire and lava light it. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Sheep sheep) || sheep.level().isClientSide() || sheep.tickCount % 5 != 0) return;
        if ((sheep.isOnFire() || sheep.isInLava()) && hasFuse(sheep)) light(sheep);
    }

    /** Burns down lit fuses, with sparks and smoke at the wick, and pops the sheep whose fuse has run out. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || LIT.isEmpty()) return;
        long now = level.getGameTime();
        for (Sheep sheep : List.copyOf(LIT.keySet())) {
            if (sheep.level() != level) continue;
            if (!sheep.isAlive() || !hasFuse(sheep)) {
                LIT.remove(sheep); // killed, or sheared before it went off: the fuse goes out
                continue;
            }
            Long at = LIT.get(sheep);
            if (at == null) continue;
            if (now >= at) {
                pop(sheep);
            } else {
                Vec3 wick = sheep.position().add(0, sheep.getBbHeight() + 0.15, 0);
                level.sendParticles(ParticleTypes.SMOKE, wick.x, wick.y, wick.z, 1, 0.05, 0.05, 0.05, 0.01);
                if (now % 2 == 0) level.sendParticles(ParticleTypes.SMALL_FLAME, wick.x, wick.y, wick.z, 1, 0.03, 0.03, 0.03, 0.01);
            }
        }
    }

    /** Lightning lights it (and doesn't burn it: the pop puts the fire out anyway). */
    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof Sheep sheep && hasFuse(sheep)) light(sheep);
    }

    /** Flint and steel (or a fire charge) on it lights it, the way it lights a creeper. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Sheep sheep) || !hasFuse(sheep)) return;
        ItemStack held = event.getItemStack();
        boolean flint = held.is(Items.FLINT_AND_STEEL), charge = held.is(Items.FIRE_CHARGE);
        if (!flint && !charge) return;
        if (!sheep.level().isClientSide() && light(sheep)) {
            sheep.level().playSound(null, sheep, charge ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (charge) held.consume(1, event.getEntity());
            else held.hurtAndBreak(1, event.getEntity(), event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
