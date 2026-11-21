package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CrateBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A Wool-Lined Crate: holds lots of one kind of item ({@link #capacity()}: {@code [crate] stacks} stacks, more for
 * each Wool Bale packed in), shows it on its front, and can be locked to it so it keeps that item when emptied.
 *
 * <p>Past 90% its lid bulges ({@link CrateBlock#BULGING}). Pushing more into it while it's full (a player clicking, or
 * a pipe or hopper trying) can make it burst, scattering a handful of its items, unless it has a Crate Strap
 * ({@link CrateBlock#STRAPPED}). Pipes and hoppers reach its one slot from every side; a comparator reads how full it
 * is.</p>
 */
public class CrateBlockEntity extends BlockEntity {
    /** Two right-clicks this close together (in ticks) put in every matching item the player has. */
    private static final int DOUBLE_CLICK = 8;
    /** Ticks between a left-click taking items and the next one, so holding the button doesn't empty it in a blink. */
    private static final int TAKE_COOLDOWN = 4;

    private ItemStack item = ItemStack.EMPTY;
    private int count;
    private boolean locked;
    private int bales;
    /** Whether something was pushed at it while full since its last burst roll (pipes and hoppers). */
    private boolean pressed;
    private final Map<UUID, Long> lastStore = new HashMap<>(), lastTake = new HashMap<>();
    private final Handler handler = new Handler();

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRATE.get(), pos, state);
    }

    // ------------------------------------------------------------------ what it holds

    public ItemStack item() {
        return item;
    }

    public int count() {
        return count;
    }

    public boolean locked() {
        return locked;
    }

    public int bales() {
        return bales;
    }

    public static int maxBales() {
        return ModConfigs.getInt(ModConfigs.SERVER.crateMaxBales);
    }

    /** How many stacks it holds: its own, and each bale's. */
    public int stacks() {
        return ModConfigs.getInt(ModConfigs.SERVER.crateStacks) + bales * ModConfigs.getInt(ModConfigs.SERVER.crateStacksPerBale);
    }

    /** How many of its item it holds at most (stacks of 64 if it's empty). */
    public int capacity() {
        return capacityFor(item.isEmpty() ? 64 : item.getMaxStackSize());
    }

    private int capacityFor(int stackSize) {
        return (int) Math.min(Integer.MAX_VALUE, (long) stacks() * stackSize);
    }

    /** Whether a pipe or hopper has pushed at it while it's full since its last burst roll. */
    public boolean straining() {
        return pressed;
    }

    public boolean isFull() {
        return count > 0 && count >= capacity();
    }

    /** Whether it can take {@code stack} at all (room aside): its item, or anything if it's empty and not locked. */
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty() || !canStore(stack)) return false;
        return item.isEmpty() ? !locked : ItemStack.isSameItemSameComponents(item, stack);
    }

    /** A crate with something in it can't go in another (like a shulker box in a shulker box). */
    public static boolean canStore(ItemStack stack) {
        CrateContents contents = stack.get(ModDataComponents.CRATE_CONTENTS.get());
        return (contents == null || contents.count() <= 0) && stack.getItem().canFitInsideContainerItems();
    }

    /** How many of {@code stack} it would take right now. */
    public int room(ItemStack stack) {
        if (!accepts(stack)) return 0;
        return Math.max(0, capacityFor(stack.getMaxStackSize()) - count);
    }

    /** Puts in as much of {@code stack} as fits and shrinks it by that; returns how many went in. */
    public int add(ItemStack stack) {
        int moved = Math.min(stack.getCount(), room(stack));
        if (moved <= 0) return 0;
        if (item.isEmpty()) item = stack.copyWithCount(1);
        count += moved;
        stack.shrink(moved);
        changed();
        return moved;
    }

    /** Takes up to {@code amount} out (at most a stack). */
    public ItemStack take(int amount) {
        int moved = Math.min(Math.min(amount, count), item.getMaxStackSize());
        if (moved <= 0) return ItemStack.EMPTY;
        ItemStack out = item.copyWithCount(moved);
        count -= moved;
        if (count <= 0 && !locked) item = ItemStack.EMPTY;
        changed();
        return out;
    }

    public void setLocked(boolean locked) {
        this.locked = locked && !item.isEmpty();
        if (!this.locked && count <= 0) item = ItemStack.EMPTY;
        changed();
    }

    /** Packs one more Wool Bale in, if there's room for one. */
    public boolean addBale() {
        if (bales >= maxBales()) return false;
        bales++;
        changed();
        return true;
    }

    /** Sets everything at once (tests, and the test world). */
    public void setContents(CrateContents contents) {
        item = contents.item();
        count = item.isEmpty() ? 0 : Math.max(0, contents.count());
        locked = contents.locked() && !item.isEmpty();
        bales = Mth.clamp(contents.bales(), 0, maxBales());
        changed();
    }

    public CrateContents contents() {
        return new CrateContents(item, count, locked, bales);
    }

    // ------------------------------------------------------------------ players

    /**
     * A right-click on its front with {@code held}: puts the held stack in; a second click straight after puts in every
     * matching item {@code player} carries. Returns how many went in, or -1 if it couldn't take any because it's full.
     */
    public int store(ServerLevel level, Player player, ItemStack held) {
        long now = level.getGameTime();
        Long last = lastStore.put(player.getUUID(), now);
        boolean all = last != null && now - last <= DOUBLE_CLICK;
        int moved = 0;
        if (!held.isEmpty()) moved += add(held);
        if (all && !item.isEmpty()) {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && accepts(stack)) moved += add(stack);
            }
            inventory.setChanged();
        }
        if (moved > 0) {
            level.playSound(null, worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            return moved;
        }
        if (isFull() && (held.isEmpty() || accepts(held))) {
            level.playSound(null, worldPosition, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 0.6F); // it creaks
            if (!held.isEmpty() || all) rollBurst(level);
            return -1;
        }
        return 0;
    }

    /** A left-click on its front: a stack into {@code player}'s inventory, or one with {@code one}. */
    public void takeFor(ServerLevel level, Player player, boolean one) {
        long now = level.getGameTime();
        Long last = lastTake.get(player.getUUID());
        if (last != null && now - last < TAKE_COOLDOWN && now >= last) return;
        lastTake.put(player.getUUID(), now);
        ItemStack out = take(one ? 1 : item.getMaxStackSize());
        if (out.isEmpty()) return;
        if (!player.getInventory().add(out) && !out.isEmpty()) {
            ItemEntity dropped = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), out);
            dropped.setNoPickUpDelay();
            level.addFreshEntity(dropped);
        }
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3F, 1.0F + level.getRandom().nextFloat() * 0.4F);
    }

    // ------------------------------------------------------------------ bursting

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, CrateBlockEntity be) {
        // Pipes and hoppers pushing at a full crate: one roll a second, however hard they push.
        if (be.pressed && level.getGameTime() % 20 == Math.floorMod(pos.asLong(), 20)) {
            be.pressed = false;
            if (be.isFull()) be.rollBurst(level);
        }
    }

    private void rollBurst(ServerLevel level) {
        if (!ModConfigs.getBool(ModConfigs.SERVER.crateBursting) || getBlockState().getValue(CrateBlock.STRAPPED)) return;
        if (level.getRandom().nextDouble() < ModConfigs.getDouble(ModConfigs.SERVER.crateBurstChance)) burst(level);
    }

    /** Pops open: a handful of its items fly out, its lock breaks, and the wool lining puffs out. */
    public void burst(ServerLevel level) {
        int spill = Math.min(count, ModConfigs.getInt(ModConfigs.SERVER.crateBurstItems));
        var random = level.getRandom();
        for (int i = 0; i < spill; i++) {
            ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, item.copyWithCount(1));
            entity.setDeltaMovement((random.nextDouble() - 0.5) * 0.45, 0.3 + random.nextDouble() * 0.25, (random.nextDouble() - 0.5) * 0.45);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        count -= spill;
        locked = false;
        if (count <= 0) item = ItemStack.EMPTY;
        Block wool = Block.byItem(com.jax3.barelyfunctionalcontraptions.compat.Vanilla.wool(getBlockState().getValue(CrateBlock.LINING)));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, wool.defaultBlockState()), worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5, 40, 0.35, 0.15, 0.35, 0.15);
        level.sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.02);
        level.playSound(null, worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8F, 1.6F);
        ModAdvancements.awardNearby(level, worldPosition, 8, ModAdvancements.BURSTING_AT_THE_SEAMS);
        changed();
    }

    // ------------------------------------------------------------------ pipes, hoppers, comparators

    public ResourceHandler<ItemResource> getItemHandler() {
        return handler;
    }

    /** 0 empty, then 1 to 15 with how full it is. */
    public int comparatorSignal() {
        if (count <= 0) return 0;
        return Math.min(15, 1 + (int) (14L * count / Math.max(1, capacity())));
    }

    /** Its one slot, for pipes and hoppers on every side. */
    private final class Handler implements ResourceHandler<ItemResource> {
        private final Journal journal = new Journal();

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return item.isEmpty() ? ItemResource.EMPTY : ItemResource.of(item);
        }

        @Override
        public long getAmountAsLong(int index) {
            return count;
        }

        /**
         * Full and unstrapped, it says it has room for one more: so hoppers and pipes (which skip anything full) keep
         * pushing at it, its seams strain, and it can burst. The push itself still gets nothing in.
         */
        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            if (resource.isEmpty()) return capacity();
            if (!accepts(resource.toStack())) return 0;
            long capacity = capacityFor(resource.toStack().getMaxStackSize());
            boolean strains = ModConfigs.getBool(ModConfigs.SERVER.crateBursting) && !getBlockState().getValue(CrateBlock.STRAPPED);
            return strains && count >= capacity ? count + 1L : capacity;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty() && accepts(resource.toStack());
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            Objects.checkIndex(index, 1);
            if (resource.isEmpty() || amount <= 0) return 0;
            ItemStack stack = resource.toStack();
            if (!accepts(stack)) return 0;
            int moved = Math.min(amount, Math.max(0, capacityFor(stack.getMaxStackSize()) - count));
            if (moved <= 0) {
                pressed = true; // full: it strains
                return 0;
            }
            journal.updateSnapshots(transaction);
            if (item.isEmpty()) item = stack.copyWithCount(1);
            count += moved;
            return moved;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            Objects.checkIndex(index, 1);
            if (resource.isEmpty() || amount <= 0 || item.isEmpty() || !resource.matches(item)) return 0;
            int moved = Math.min(amount, count);
            if (moved <= 0) return 0;
            journal.updateSnapshots(transaction);
            count -= moved;
            if (count <= 0 && !locked) item = ItemStack.EMPTY;
            return moved;
        }
    }

    private record Snapshot(ItemStack item, int count) {}

    private final class Journal extends SnapshotJournal<Snapshot> {
        @Override
        protected Snapshot createSnapshot() {
            return new Snapshot(item, count);
        }

        @Override
        protected void revertToSnapshot(Snapshot snapshot) {
            item = snapshot.item();
            count = snapshot.count();
        }

        @Override
        protected void onRootCommit(Snapshot original) {
            changed();
        }
    }

    // ------------------------------------------------------------------ saving and syncing

    /** Saves, redraws its front for nearby players, bulges its lid (or not), shows its padlock, and updates comparators. */
    private void changed() {
        setChanged();
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        boolean bulging = count > 0 && (long) count * 10 >= (long) capacity() * 9;
        if (state.getValue(CrateBlock.BULGING) != bulging || state.getValue(CrateBlock.LOCKED) != locked) {
            level.setBlock(worldPosition, state.setValue(CrateBlock.BULGING, bulging).setValue(CrateBlock.LOCKED, locked), Block.UPDATE_ALL);
        } else {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    /** Puts its lid right for what it holds (after it's placed with something in it). */
    public void refreshLid() {
        changed();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        item = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        count = item.isEmpty() ? 0 : input.getIntOr("count", 0);
        locked = input.getBooleanOr("locked", false) && !item.isEmpty();
        bales = input.getIntOr("bales", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!item.isEmpty()) output.store("item", ItemStack.OPTIONAL_CODEC, item);
        output.putInt("count", count);
        output.putBoolean("locked", locked);
        output.putInt("bales", bales);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        CrateContents contents = contents();
        if (!contents.isEmpty()) components.set(ModDataComponents.CRATE_CONTENTS.get(), contents);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        CrateContents contents = components.getOrDefault(ModDataComponents.CRATE_CONTENTS.get(), CrateContents.EMPTY);
        item = contents.item();
        count = item.isEmpty() ? 0 : contents.count();
        locked = contents.locked() && !item.isEmpty();
        bales = contents.bales();
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("item");
        output.discard("count");
        output.discard("locked");
        output.discard("bales");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients get its item, count, lock and bales, for its front. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
