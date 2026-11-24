package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.InfusionAltarBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.InfusionAltarMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModTags;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.AltarRitual;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * The Infusion Altar: infuses the nearest sheep standing beside it (within {@value #REACH} blocks) with the resource
 * whose feeding item is in its centre slot, using the catalysts in its four side slots and Forge Energy.
 *
 * <ul>
 *     <li>Any resource can be infused here; tier 2 and up ({@link AltarRitual#altarOnly()}) can only be infused here.</li>
 *     <li>It takes the resource's {@link AltarRitual#effectiveEnergy()} spread over {@code [infusion_altar] ticks}, and
 *     only uses the items once the infusion finishes. Progress pauses while anything is missing.</li>
 *     <li>The sheep must be able to take the resource just as when fed by hand (grown up, not already infused...).
 *     Its Endurance raises the uses as usual.</li>
 *     <li>Hoppers and pipes can put feeding items and catalysts in from any side, but can't take anything out.</li>
 * </ul>
 */
public class InfusionAltarBlockEntity extends BlockEntity implements MachineContainer, WorldlyContainer, MenuProvider, AreaDisplay {
    /** The area outline's colour: the altar's amethyst purple. */
    public static final int AREA_COLOR = 0xFFB574FF;
    public static final int FEED = 0, SIZE = 1 + AltarRitual.MAX_CATALYSTS;
    public static final int REACH = 2;
    public static final int DATA_COUNT = 11;
    private static final int[] SLOTS = {0, 1, 2, 3, 4};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final MachineEnergy energy = new MachineEnergy(
            () -> ModConfigs.getInt(ModConfigs.SERVER.altarCapacity), () -> ModConfigs.getInt(ModConfigs.SERVER.altarMaxInput), this::setChanged);
    private int progress;
    private int maxProgress;
    private int energyPerTick;
    private AltarStatus status = AltarStatus.IDLE;
    private int sheepId = -1;
    /** The sheep this altar slowed down, whose Slowness it takes off again; not saved: the effect wears off by itself. */
    private int slowedId = -1;
    /** Whether the area a sheep must stand in is outlined; saved, and synced to clients. */
    private boolean showArea;
    /** Whether the centre slot and the catalysts were complete last tick: the redstone pulse fires when they become so. */
    private boolean ready;
    /** Game ticks left of the redstone pulse (one redstone tick is 2). */
    private int pulseTicks;
    /** Length of the pulse: one redstone tick. */
    public static final int PULSE_TICKS = 2;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> Math.min(0xFFFF, progress);
                case 5 -> Math.min(0xFFFF, maxProgress);
                case 6 -> energyPerTick & 0xFFFF;
                case 7 -> energyPerTick >>> 16;
                case 8 -> status.ordinal();
                case 9 -> sheepId >= 0 ? 1 : 0;
                case 10 -> showArea ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public InfusionAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFUSION_ALTAR.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, InfusionAltarBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.refreshLimits();
        maxProgress = ModConfigs.getInt(ModConfigs.SERVER.altarTicks);
        sheepId = -1;
        ItemStack feed = items.get(FEED);
        Optional<SheepResource> resource = resourceFor(level, feed);
        if (feed.isEmpty()) {
            set(AltarStatus.IDLE, 0);
        } else if (resource.isEmpty()) {
            set(AltarStatus.INVALID_INPUT, 0);
        } else {
            SheepResource r = resource.get();
            AltarRitual ritual = r.altar();
            energyPerTick = (ritual.effectiveEnergy() + maxProgress - 1) / maxProgress;
            LivingEntity sheep = findSheep(level, pos, r);
            if (feed.getCount() < r.effectiveFeedingAmount()) {
                set(AltarStatus.NOT_ENOUGH_ITEMS, progress);
            } else if (catalystSlots(ritual) == null) {
                set(AltarStatus.MISSING_CATALYSTS, progress);
            } else if (sheep == null) {
                set(AltarStatus.NO_SHEEP, progress);
            } else if (energy.getAmountAsInt() < EnergyShare.forTick(ritual.effectiveEnergy(), maxProgress, progress)) {
                sheepId = sheep.getId();
                set(AltarStatus.NO_ENERGY, progress);
            } else {
                sheepId = sheep.getId();
                status = AltarStatus.INFUSING;
                energy.drain(EnergyShare.forTick(ritual.effectiveEnergy(), maxProgress, progress));
                progress++;
                if (level.getGameTime() % 4 == 0) beam(level, pos, sheep, r);
                slow(level, sheep);
                if (progress >= maxProgress) {
                    finish(level, pos, sheep, r);
                    progress = 0;
                    unslow(level);
                }
                setChanged();
            }
        }
        boolean active = status == AltarStatus.INFUSING;
        if (!active) unslow(level);
        // A one-redstone-tick pulse when the items for an infusion are all in: the resource and every catalyst.
        boolean nowReady = resource.isPresent() && feed.getCount() >= resource.get().effectiveFeedingAmount()
                && catalystSlots(resource.get().altar()) != null;
        if (nowReady && !ready) pulseTicks = PULSE_TICKS;
        else if (pulseTicks > 0) pulseTicks--;
        ready = nowReady;
        boolean powered = pulseTicks > 0;
        BlockState wanted = state.setValue(InfusionAltarBlock.ACTIVE, active).setValue(InfusionAltarBlock.POWERED, powered);
        if (wanted != state) level.setBlock(pos, wanted, Block.UPDATE_ALL);
    }

    /** How long each refresh of the altar's Slowness lasts; it is refreshed well before it runs out. */
    static final int SLOW_TICKS = 30;

    /** Holds the sheep being infused nearly still with Slowness, and lets go of any other one it was holding. */
    private void slow(ServerLevel level, LivingEntity sheep) {
        if (slowedId != sheep.getId()) unslow(level);
        int amplifier = ModConfigs.getInt(ModConfigs.SERVER.altarSlowness) - 1;
        if (amplifier < 0) return;
        slowedId = sheep.getId();
        MobEffectInstance current = sheep.getEffect(MobEffects.SLOWNESS);
        if (current == null || (isOurs(current) && current.getDuration() < SLOW_TICKS - 10)) {
            sheep.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, amplifier, true, false, false));
        }
    }

    /** Takes the altar's Slowness off the sheep it was on, leaving any Slowness from elsewhere alone. */
    private void unslow(Level level) {
        if (slowedId < 0) return;
        if (level.getEntity(slowedId) instanceof LivingEntity sheep) {
            MobEffectInstance current = sheep.getEffect(MobEffects.SLOWNESS);
            if (current != null && isOurs(current)) sheep.removeEffect(MobEffects.SLOWNESS);
        }
        slowedId = -1;
    }

    /** The altar's Slowness is short, ambient and has no particles; a potion or another mob's is not. */
    private static boolean isOurs(MobEffectInstance effect) {
        return effect.isAmbient() && !effect.isVisible() && effect.getDuration() <= SLOW_TICKS;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) unslow(level);
        super.setRemoved();
    }

    private void set(AltarStatus status, int progress) {
        this.status = status;
        this.progress = progress;
    }

    /** The resource whose feeding item this is, on this side. */
    public static Optional<SheepResource> resourceFor(Level level, ItemStack feed) {
        if (feed.isEmpty()) return Optional.empty();
        return SheepResourceRegistry.get(level).byFeedingItem(feed);
    }

    /**
     * The catalyst slot used for each of the ritual's catalysts, in order, or {@code null} if one is missing. Each
     * slot is used for one catalyst at most.
     */
    public int @Nullable [] catalystSlots(AltarRitual ritual) {
        return matchCatalysts(ritual, items);
    }

    public static int @Nullable [] matchCatalysts(AltarRitual ritual, List<ItemStack> slots) {
        int[] used = new int[ritual.catalysts().size()];
        boolean[] taken = new boolean[SIZE];
        for (int c = 0; c < used.length; c++) {
            SizedIngredient need = ritual.catalysts().get(c);
            used[c] = -1;
            for (int s = 1; s < SIZE; s++) {
                ItemStack stack = slots.get(s);
                if (!taken[s] && need.ingredient().test(stack) && stack.getCount() >= need.count()) {
                    used[c] = s;
                    taken[s] = true;
                    break;
                }
            }
            if (used[c] < 0) return null;
        }
        return used;
    }

    /**
     * Where a sheep must stand to be infused: within {@link #REACH} blocks sideways (5x5 columns) and one block up or
     * down, so a sheep on the floor beside the pedestal or on a step counts.
     */
    public static AABB sheepArea(BlockPos pos) {
        return new AABB(pos).inflate(REACH, 1, REACH);
    }

    @Override
    public boolean showsArea() {
        return showArea;
    }

    @Override
    public void setShowsArea(boolean show) {
        showArea = show;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public AABB displayedArea() {
        return sheepArea(worldPosition);
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    /** The nearest grown, infusable sheep within reach that can take {@code resource} now. */
    private static @Nullable LivingEntity findSheep(ServerLevel level, BlockPos pos, SheepResource resource) {
        AABB area = sheepArea(pos);
        Vec3 centre = Vec3.atCenterOf(pos);
        return level.getEntitiesOfClass(LivingEntity.class, area, e -> ModTags.isInfusable(e) && e.isAlive()
                        && SheepInfusionEvents.checkSheep(e, resource, false) == null)
                .stream().min(Comparator.comparingDouble(e -> e.position().distanceToSqr(centre))).orElse(null);
    }

    private void finish(ServerLevel level, BlockPos pos, LivingEntity sheep, SheepResource resource) {
        int[] slots = catalystSlots(resource.altar());
        if (slots == null) return;
        for (int c = 0; c < slots.length; c++) items.get(slots[c]).shrink(resource.altar().catalysts().get(c).count());
        items.get(FEED).shrink(resource.effectiveFeedingAmount());
        SheepInfusionEvents.infuseFromAltar(level, sheep, resource);
        // Whoever is nearby gets the infusion advancements, as if they had fed it.
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, false);
        if (player instanceof ServerPlayer serverPlayer) {
            com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.onInfused(serverPlayer, resource);
        }
    }

    /** A stream of the resource's colour from the altar's top to the sheep. */
    private static void beam(ServerLevel level, BlockPos pos, LivingEntity sheep, SheepResource resource) {
        Vec3 from = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        Vec3 to = sheep.position().add(0, sheep.getBbHeight() * 0.6, 0);
        for (int i = 0; i <= 6; i++) {
            Vec3 p = from.lerp(to, i / 6.0);
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? resource.woolColor() : resource.accentColor(), 0.9F), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
        }
    }

    // ------------------------------------------------------------------ accessors

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    public AltarStatus status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public int maxProgress() {
        return maxProgress;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.infusion_altar");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfusionAltarMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ container

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SIZE ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public void putItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE) return;
        if (slot == FEED && !ItemStack.isSameItemSameComponents(items.get(FEED), stack)) progress = 0;
        items.set(slot, stack);
    }

    /** The centre takes feeding items; the sides take catalysts of any resource. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (level == null) return false;
        return slot == FEED ? resourceFor(level, stack).isPresent() : slot < SIZE && isCatalyst(level, stack);
    }

    public static boolean isCatalyst(Level level, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (SheepResource r : SheepResourceRegistry.get(level).all()) {
            for (SizedIngredient c : r.altar().catalysts()) if (c.ingredient().test(stack)) return true;
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, this);
    }

    // ------------------------------------------------------------------ energy

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        items.clear();
        ContainerHelper.loadAllItems(in, items);
        in.child("energy").ifPresent(energy::deserialize);
        progress = in.getIntOr("progress", 0);
        ready = in.getBooleanOr("ready", false);
        showArea = in.getBooleanOr("show_area", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out, items);
        energy.serialize(out.child("energy"));
        out.putInt("progress", progress);
        out.putBoolean("ready", ready);
        out.putBoolean("show_area", showArea);
    }

    // ------------------------------------------------------------------ client sync (the area outline)

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients only need the area setting. */
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean("show_area", showArea);
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ValueInput input) {
        showArea = input.getBooleanOr("show_area", false);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        showArea = input.getBooleanOr("show_area", false);
    }
}
