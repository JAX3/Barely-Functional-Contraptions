package com.jax3.barelyfunctionalcontraptions.sheep.items;

import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * A sheep spawn egg for one resource, saved in the {@code barelyfunctionalcontraptions:sheep_resource} component like
 * resource wool. The sheep it spawns is already infused with that resource, for its full number of shearings. Used
 * on a sheep it gives an infused lamb, like a vanilla egg; used on a spawner it sets a plain sheep spawner.
 */
public class ResourceSheepSpawnEggItem extends SpawnEggItem {
    public ResourceSheepSpawnEggItem(Properties properties) {
        super(properties.spawnEgg(EntityTypes.SHEEP));
    }

    public static ItemStack create(Identifier resource, int count) {
        ItemStack stack = new ItemStack(ModItems.RESOURCE_SHEEP_SPAWN_EGG.get(), count);
        stack.set(ModDataComponents.SHEEP_RESOURCE.get(), resource);
        return stack;
    }

    public static Optional<SheepResource> getResource(ItemStack stack, Level level) {
        if (!(stack.getItem() instanceof ResourceSheepSpawnEggItem)) return Optional.empty();
        return SheepResourceRegistry.get(level).byId(stack.get(ModDataComponents.SHEEP_RESOURCE.get()));
    }

    /** Infuses a sheep just spawned from {@code stack} with the egg's resource, if it still exists. */
    public static void infuse(Entity sheep, ItemStack stack) {
        if (!(sheep instanceof LivingEntity living)) return;
        getResource(stack, sheep.level()).ifPresent(r -> SheepInfusionEvents.setInfusion(living,
                SheepInfusionEvents.getInfusion(living).withResource(r.id(), SheepInfusionEvents.infusionUses(living, r))));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // Spawners and the client side work as they do for a vanilla egg.
        if (!(level instanceof ServerLevel serverLevel) || level.getBlockEntity(pos) instanceof Spawner) return super.useOn(context);
        BlockState state = level.getBlockState(pos);
        Direction face = context.getClickedFace();
        BlockPos spawnPos = state.getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(face);
        return spawn(serverLevel, context.getPlayer(), context.getItemInHand(), spawnPos, true,
                !Objects.equals(pos, spawnPos) && face == Direction.UP);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return InteractionResult.FAIL;
        InteractionResult result = spawn(serverLevel, player, stack, pos, false, false);
        if (result == InteractionResult.SUCCESS) player.awardStat(Stats.ITEM_USED.get(this));
        return result;
    }

    private static InteractionResult spawn(ServerLevel level, @Nullable Player user, ItemStack stack, BlockPos pos, boolean tryMoveDown, boolean movedUp) {
        ItemStack used = stack.copy(); // the stack may be used up by the time the sheep is infused
        Sheep sheep = EntityTypes.SHEEP.spawn(level, EntityType.<Sheep>createDefaultStackConfig(level, stack, user).andThen(s -> infuse(s, used)),
                pos, EntitySpawnReason.SPAWN_ITEM_USE, tryMoveDown, movedUp);
        if (sheep == null) return InteractionResult.FAIL;
        stack.consume(1, user);
        level.gameEvent(user, GameEvent.ENTITY_PLACE, pos);
        return InteractionResult.SUCCESS;
    }

    /** Right-clicking a sheep with the egg: an infused lamb, as a vanilla egg gives a lamb. */
    public static Optional<Sheep> spawnLamb(ServerLevel level, Player player, Sheep parent, ItemStack stack) {
        ItemStack used = stack.copy();
        return SpawnEggItem.spawnOffspringFromSpawnEgg(player, parent, EntityTypes.SHEEP, level, parent.position(), stack)
                .map(lamb -> {
                    infuse(lamb, used);
                    return (Sheep) lamb;
                });
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier id = stack.get(ModDataComponents.SHEEP_RESOURCE.get());
        if (id == null) return Component.translatable("item.barelyfunctionalcontraptions.resource_sheep_spawn_egg");
        Component resource = SheepResourceRegistry.client().byId(id).or(() -> SheepResourceRegistry.server().byId(id))
                .map(SheepResource::name).orElse(Component.literal(id.getPath()));
        return Component.translatable("item.barelyfunctionalcontraptions.resource_sheep_spawn_egg.named", resource);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        Identifier id = stack.get(ModDataComponents.SHEEP_RESOURCE.get());
        Optional<SheepResource> resource = id == null ? Optional.empty()
                : SheepResourceRegistry.client().byId(id).or(() -> SheepResourceRegistry.server().byId(id));
        if (resource.isEmpty()) {
            builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.wool.no_resource").withStyle(ChatFormatting.RED));
            return;
        }
        SheepResource r = resource.get();
        builder.accept(Component.translatable("tooltip.barelyfunctionalcontraptions.spawn_egg.infused",
                r.name().copy().withColor(r.woolColor()), r.effectiveInfusionUses()).withStyle(ChatFormatting.GRAY));
    }
}
