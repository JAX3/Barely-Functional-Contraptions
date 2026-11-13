package com.jax3.barelyfunctionalcontraptions.sheep.entity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModTags;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles feeding resources to sheep and sneak-inspecting them.
 *
 * <p>The decision logic runs on both sides so the client can play the arm-swing immediately, but only the server
 * changes state (attachment, item count). Items that do not match any resource definition are ignored entirely, so
 * vanilla behaviour (wheat breeding, dyeing, shearing...) is untouched.</p>
 */
public final class SheepInfusionEvents {
    private SheepInfusionEvents() {}

    public static SheepInfusion getInfusion(Entity entity) {
        return entity.getData(ModAttachments.SHEEP_INFUSION);
    }

    public static void setInfusion(Entity entity, SheepInfusion infusion) {
        entity.setData(ModAttachments.SHEEP_INFUSION, infusion);
    }

    public static Optional<SheepResource> getInfusedResource(Entity entity) {
        return getInfusion(entity).resource().flatMap(id -> SheepResourceRegistry.get(entity.level()).byId(id));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living) || !ModTags.isInfusable(target)) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();

        if (stack.isEmpty()) {
            if (player.isShiftKeyDown() && ModConfigs.getBool(ModConfigs.SERVER.sneakToInspect)
                    && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                if (!level.isClientSide()) player.sendOverlayMessage(describe(living));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        if (stack.is(net.minecraft.world.item.Items.MELON_SLICE) && target instanceof Sheep sheep && MELON_SKIN_PLAYER.equals(player.getUUID())) {
            feedMelonSlice(event, player, sheep, stack);
            return;
        }
        if (!ModConfigs.getBool(ModConfigs.SERVER.feedingEnabled)) return;
        Optional<SheepResource> match = SheepResourceRegistry.get(level).byFeedingItem(stack);
        if (match.isEmpty()) return; // not a resource: let vanilla handle it
        SheepResource resource = match.get();

        // From here on this interaction belongs to us, whatever the outcome.
        event.setCanceled(true);
        Component refusal = checkCanInfuse(player, living, resource, stack);
        if (refusal != null) {
            if (!level.isClientSide()) player.sendOverlayMessage(refusal.copy().withStyle(ChatFormatting.RED));
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level instanceof ServerLevel serverLevel) {
            infuse(serverLevel, player, living, resource, stack);
        }
    }

    /** The player whose skin and head a sheep takes when fed a Melon Slice. */
    public static final java.util.UUID MELON_SKIN_PLAYER = java.util.UUID.fromString("62776558-b5f6-4b75-a2ef-009f8dc9fc52");

    /**
     * Feeding a sheep a Melon Slice gives it the skin and head of one particular player ({@link #MELON_SKIN_PLAYER}).
     * Only that player can do it: for anyone else a Melon Slice does nothing and isn't used up.
     * The sheep stores that player's UUID and clients look the skin up the way a placed Player Head does. One slice is
     * used up (not in creative). This is only a look: it works on any sheep, infused or not, and doesn't change what
     * it drops. A sheep that already has the skin doesn't eat another slice.
     */
    private static void feedMelonSlice(PlayerInteractEvent.EntityInteract event, Player player, Sheep sheep, ItemStack stack) {
        var attachment = com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments.SHEEP_SKIN;
        if (sheep.hasData(attachment) && sheep.getData(attachment).profile()
                .map(p -> MELON_SKIN_PLAYER.equals(p.partialProfile().id())).orElse(false)) {
            return; // already wearing it: leave the slice alone
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        sheep.setData(attachment, SheepSkin.of(net.minecraft.world.item.component.ResolvableProfile.createUnresolved(MELON_SKIN_PLAYER)));
        stack.consume(1, player);
        level.playSound(null, sheep.getX(), sheep.getY(), sheep.getZ(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, sheep.getX(), sheep.getY() + sheep.getBbHeight() * 0.7, sheep.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.skin.applied"));
        sheep.gameEvent(GameEvent.EAT, player);
    }

    /**
     * Breeding: a lamb takes after its parents. If they are infused it is born infused with one of their resources
     * (either one at random when they differ), with that resource's full number of shearings. Its wool, like any
     * lamb's, only drops once it has grown up. The parents keep their own infusions.
     */
    @SubscribeEvent
    public static void onBabySpawn(net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent event) {
        var child = event.getChild();
        if (child == null || !(child.level() instanceof ServerLevel level) || !ModTags.isInfusable(child)) return;
        Optional<SheepResource> inherited = inheritedResource(event.getParentA(), event.getParentB(), level.getRandom());
        if (inherited.isEmpty()) return;
        SheepResource resource = inherited.get();
        setInfusion(child, SheepInfusion.NONE.withResource(resource.id(), resource.effectiveInfusionUses()));
        if (ModConfigs.getBool(ModConfigs.SERVER.feedingParticles)) {
            double x = child.getX(), y = child.getY() + 0.5, z = child.getZ();
            level.sendParticles(new DustParticleOptions(resource.woolColor(), 1.0F), x, y, z, 14, 0.3, 0.25, 0.3, 0.02);
        }
    }

    /**
     * The resource a lamb of these two parents inherits, if any: following {@code breedingKeepsInfusion} and
     * {@code breedingNeedsBothParents}, one of the parents' (loaded) infusions, picked at random when both have one.
     */
    public static Optional<SheepResource> inheritedResource(Entity parentA, Entity parentB, net.minecraft.util.RandomSource random) {
        if (!ModConfigs.getBool(ModConfigs.SERVER.breedingKeepsInfusion)) return Optional.empty();
        Optional<SheepResource> a = getInfusion(parentA).isInfused() ? getInfusedResource(parentA) : Optional.empty();
        Optional<SheepResource> b = getInfusion(parentB).isInfused() ? getInfusedResource(parentB) : Optional.empty();
        if (a.isPresent() && b.isPresent()) return random.nextBoolean() ? a : b;
        if (ModConfigs.getBool(ModConfigs.SERVER.breedingNeedsBothParents)) return Optional.empty();
        return a.isPresent() ? a : b;
    }

    /** Returns {@code null} if the sheep may be infused, otherwise the reason it cannot. */
    static Component checkCanInfuse(Player player, LivingEntity sheep, SheepResource resource, ItemStack stack) {
        boolean creativeBypass = player.hasInfiniteMaterials() && ModConfigs.getBool(ModConfigs.SERVER.creativeIgnoresCooldown);
        if (sheep.isBaby() && !ModConfigs.getBool(ModConfigs.SERVER.allowBabies)) {
            return Component.translatable("message.barelyfunctionalcontraptions.too_young");
        }
        if (sheep instanceof Sheep vanillaSheep && vanillaSheep.isSheared() && !ModConfigs.getBool(ModConfigs.SERVER.allowFeedingShearedSheep)) {
            return Component.translatable("message.barelyfunctionalcontraptions.needs_wool");
        }
        SheepInfusion current = getInfusion(sheep);
        if (!creativeBypass && current.onCooldown(sheep.level().getGameTime())) {
            long seconds = (current.cooldownUntil() - sheep.level().getGameTime() + 19) / 20;
            return Component.translatable("message.barelyfunctionalcontraptions.cooldown", seconds);
        }
        // An infusion whose wool type isn't loaded (pack removed, mod missing) never blocks a new one.
        if (current.isInfused() && getInfusedResource(sheep).isPresent()) {
            boolean same = current.resource().get().equals(resource.id());
            if (same) {
                int max = resource.effectiveInfusionUses();
                boolean canRefresh = ModConfigs.getBool(ModConfigs.SERVER.sameResourceRefreshesUses) && max > 0 && current.usesRemaining() < max;
                if (!canRefresh) return Component.translatable("message.barelyfunctionalcontraptions.already_infused", resource.name());
            } else if (!creativeBypass && !ModConfigs.getBool(ModConfigs.SERVER.allowReplacingInfusion)) {
                return Component.translatable("message.barelyfunctionalcontraptions.already_infused_other",
                        getInfusedResource(sheep).map(SheepResource::name).orElse(Component.literal(current.resource().get().toString())));
            }
        }
        int needed = resource.effectiveFeedingAmount();
        if (stack.getCount() < needed && !(player.hasInfiniteMaterials() && !ModConfigs.getBool(ModConfigs.SERVER.creativeConsumesItems))) {
            return Component.translatable("message.barelyfunctionalcontraptions.needs_more", needed, stack.getHoverName());
        }
        return null;
    }

    static void infuse(ServerLevel level, Player player, LivingEntity sheep, SheepResource resource, ItemStack stack) {
        SheepInfusion current = getInfusion(sheep);
        setInfusion(sheep, current.withResource(resource.id(), resource.effectiveInfusionUses()));

        boolean consume = !player.hasInfiniteMaterials() || ModConfigs.getBool(ModConfigs.SERVER.creativeConsumesItems);
        if (consume) stack.shrink(resource.effectiveFeedingAmount());

        sheep.gameEvent(GameEvent.EAT, player);
        if (ModConfigs.getBool(ModConfigs.SERVER.feedingParticles)) {
            level.playSound(null, sheep, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.2F);
            level.playSound(null, sheep, SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.3F);
            double x = sheep.getX(), y = sheep.getY() + sheep.getBbHeight() * 0.7, z = sheep.getZ();
            level.sendParticles(new DustParticleOptions(resource.woolColor(), 1.4F), x, y, z, 24, 0.45, 0.35, 0.45, 0.02);
            level.sendParticles(new DustParticleOptions(resource.accentColor(), 0.9F), x, y, z, 12, 0.4, 0.3, 0.4, 0.02);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 6, 0.4, 0.3, 0.4, 0.0);
        }
        player.sendOverlayMessage(Component.translatable("message.barelyfunctionalcontraptions.infused", resource.name()).withStyle(ChatFormatting.GREEN));
    }

    /** Action-bar description used by sneak-inspection. */
    public static Component describe(LivingEntity sheep) {
        SheepInfusion infusion = getInfusion(sheep);
        if (!infusion.isInfused()) {
            if (infusion.onCooldown(sheep.level().getGameTime())) {
                long seconds = (infusion.cooldownUntil() - sheep.level().getGameTime() + 19) / 20;
                return Component.translatable("message.barelyfunctionalcontraptions.inspect.cooldown", seconds).withStyle(ChatFormatting.GRAY);
            }
            return Component.translatable("message.barelyfunctionalcontraptions.inspect.none").withStyle(ChatFormatting.GRAY);
        }
        Component name = getInfusedResource(sheep).map(r -> (Component) r.name().copy().withColor(r.woolColor()))
                .orElse(Component.literal(infusion.resource().get().toString()));
        if (infusion.usesRemaining() < 0) {
            return Component.translatable("message.barelyfunctionalcontraptions.inspect.permanent", name);
        }
        return Component.translatable("message.barelyfunctionalcontraptions.inspect.infused", name, infusion.usesRemaining());
    }
}
