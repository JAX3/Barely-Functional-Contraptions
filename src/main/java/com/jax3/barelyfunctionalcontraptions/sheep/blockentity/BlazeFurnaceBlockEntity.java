package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.BlazeFurnaceBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceBoostDataMap;
import com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeat;
import com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeatManager;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * The Blaze Sheep Furnace: makes Forge Energy from the heat of the sheep around it.
 *
 * <ul>
 *     <li><b>Heat:</b> every half second it looks for heat sources ({@link FurnaceHeat}, data-driven; the Blaze sheep
 *     by default) with their feet in {@link #heatingArea}: the 3x3 columns centred on the furnace, at its own height
 *     and the block above (so sheep standing on it and right around it count). A sheared sheep or a lamb has no flames
 *     and gives nothing until its fleece grows back.</li>
 *     <li><b>Falloff:</b> the hottest {@code maxHeatSources} count, and each one after the first adds a little less
 *     (80, 150, 210, 260 FE/t for 1-4 Blaze sheep). A source in water or rain gives its {@code wet_multiplier} (half).</li>
 *     <li><b>Boost:</b> boost items ({@link FurnaceBoostDataMap}; a Blaze Rod doubles the output for 30 seconds) go in
 *     its one slot by hand, hopper or pipe. One burns at a time, only while there is heat to boost.</li>
 * </ul>
 * It stores {@code capacity} FE and pushes it into every block around it; cables and machines can also pull from it.
 * Its comparator signal is one level per 20 FE/t it is making.
 */
public class BlazeFurnaceBlockEntity extends BlockEntity implements WorldlyContainer, AreaDisplay, net.minecraft.world.MenuProvider {
    /** Ticks between looks for heat sources. */
    public static final int SCAN_INTERVAL = 10;
    /** FE/t per comparator level. */
    public static final int FE_PER_SIGNAL = 20;
    public static final int AREA_COLOR = 0xFFFF8A1F;
    private static final int[] SLOTS = {0};
    /** Number of values synced to the window ({@link com.jax3.barelyfunctionalcontraptions.sheep.menu.BlazeFurnaceMenu}). */
    public static final int DATA_COUNT = 18;

    private final FurnaceEnergy energy = new FurnaceEnergy();
    private ItemStack boostItems = ItemStack.EMPTY;
    private int boostTicks;
    /** How long the boost burning now lasts in all, for the window's countdown. */
    private int boostDuration;
    private double boostMultiplier = 1.0;
    private boolean showArea;

    /** FE/t from heat alone, set on each scan. */
    private int heat;
    /** FE/t made on the last tick, boost included. */
    private int generating;
    /** The entities heating it, by id: synced to clients for the flame particles. */
    private int[] heaters = new int[0];
    private int scanCooldown;
    private int lastSignal;

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            int maxHeat = maxHeat();
            int best = bestPower();
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> generating & 0xFFFF;
                case 5 -> generating >>> 16;
                case 6 -> heat & 0xFFFF;
                case 7 -> heat >>> 16;
                case 8 -> maxHeat & 0xFFFF;
                case 9 -> maxHeat >>> 16;
                case 10 -> heaters.length;
                case 11 -> ModConfigs.getInt(ModConfigs.SERVER.blazeFurnaceMaxHeaters);
                case 12 -> Math.min(0xFFFF, boostTicks);
                case 13 -> Math.min(0xFFFF, boostDuration);
                case 14 -> (int) Math.round(boostMultiplier() * 100);
                case 15 -> showArea ? 1 : 0;
                case 16 -> best & 0xFFFF;
                case 17 -> best >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative; the client-side menu keeps its own SimpleContainerData copy.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /** The hottest loaded heat source's power: what one of the best heat sources gives. */
    public static int bestPower() {
        int best = 0;
        for (var entry : FurnaceHeatManager.sources()) best = Math.max(best, entry.getValue().power());
        return best;
    }

    /** Most heat a furnace can get from the hottest heat sources (before a boost): the top of the window's column. */
    public static int maxHeat() {
        int max = ModConfigs.getInt(ModConfigs.SERVER.blazeFurnaceMaxHeaters);
        return combine(java.util.Collections.nCopies(max, (double) bestPower()), max, ModConfigs.getDouble(ModConfigs.SERVER.blazeFurnaceFalloff));
    }

    public BlazeFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAZE_FURNACE.get(), pos, state);
    }

    /**
     * The heating area: the columns within {@code [blaze_furnace] radius} blocks of the furnace (3x3 at the default
     * radius 1), at its height and the block above. A mob counts when its feet are inside. The area outline shows the
     * same box. The config is synced, so clients draw the outline at the server's radius.
     */
    public static AABB heatingArea(BlockPos pos) {
        return heatingArea(pos, ModConfigs.getInt(ModConfigs.SERVER.blazeFurnaceRadius));
    }

    /** The heating area at {@code radius}: (2 radius + 1) x (2 radius + 1) columns, two blocks tall. */
    public static AABB heatingArea(BlockPos pos, int radius) {
        int r = Math.max(0, radius);
        return new AABB(pos.getX() - r, pos.getY(), pos.getZ() - r, pos.getX() + r + 1, pos.getY() + 2, pos.getZ() + r + 1);
    }

    /**
     * FE/t from heat sources giving {@code powers} FE/t each: the hottest {@code max} of them, each one after the first
     * adding {@code falloff} less of its power than the one before.
     */
    public static int combine(List<Double> powers, int max, double falloff) {
        List<Double> sorted = new ArrayList<>(powers);
        sorted.sort(Comparator.reverseOrder());
        double total = 0;
        for (int i = 0; i < Math.min(max, sorted.size()); i++) {
            total += sorted.get(i) * Math.max(0.0, 1.0 - falloff * i);
        }
        return (int) Math.round(total);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BlazeFurnaceBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.resize(ModConfigs.getInt(ModConfigs.SERVER.blazeFurnaceCapacity));
        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            scan(level, pos, state);
        }
        // A boost item burns only while there is heat to boost.
        if (boostTicks <= 0 && heat > 0) startBoost();
        double multiplier = boostTicks > 0 ? boostMultiplier : 1.0;
        if (boostTicks > 0 && heat > 0) {
            boostTicks--;
            if (boostTicks == 0) {
                boostMultiplier = 1.0;
                setChanged();
            }
        }
        generating = (int) Math.round(heat * multiplier);
        energy.generate(generating);
        push(level, pos);
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        boolean lit = generating > 0;
        if (state.getValue(BlazeFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(BlazeFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    /** Looks for heat sources in the area and works out the heat. */
    public void scan(ServerLevel level, BlockPos pos, BlockState state) {
        AABB area = heatingArea(pos);
        List<Double> powers = new ArrayList<>();
        List<Entity> found = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, area, e -> area.contains(e.position()))) {
            Optional<FurnaceHeat> source = FurnaceHeatManager.heatOf(entity);
            if (source.isEmpty()) continue;
            powers.add(source.get().powerFor(entity));
            found.add(entity);
        }
        int max = ModConfigs.getInt(ModConfigs.SERVER.blazeFurnaceMaxHeaters);
        int newHeat = combine(powers, max, ModConfigs.getDouble(ModConfigs.SERVER.blazeFurnaceFalloff));
        int[] ids = found.stream().limit(max).mapToInt(Entity::getId).sorted().toArray();
        boolean changed = !Arrays.equals(ids, heaters);
        heat = newHeat;
        heaters = ids;
        if (changed) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            if (ids.length > 0) {
                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.awardNearby(level, pos,
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.AUTO_SHEAR_RADIUS,
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.BLAZE_FURNACE);
            }
        }
    }

    private void startBoost() {
        FurnaceBoostDataMap.Boost boost = FurnaceBoostDataMap.of(boostItems);
        if (boost == null) return;
        boostItems.shrink(1);
        boostTicks = boost.duration();
        boostDuration = boost.duration();
        boostMultiplier = boost.multiplier();
        setChanged();
    }

    private void push(ServerLevel level, BlockPos pos) {
        int budget = Math.min(ModConfigs.getInt(ModConfigs.SERVER.blazeFurnaceOutput), energy.getAmountAsInt());
        for (Direction dir : Direction.values()) {
            if (budget <= 0) return;
            BlockPos target = pos.relative(dir);
            if (level.getBlockEntity(target) instanceof BlazeFurnaceBlockEntity) continue; // furnaces don't charge each other
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, target, dir.getOpposite());
            if (handler == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int moved = handler.insert(budget, tx);
                if (moved > 0 && energy.extract(moved, tx) == moved) {
                    tx.commit();
                    budget -= moved;
                }
            }
        }
    }

    // ------------------------------------------------------------------ accessors

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    /** FE/t made on the last tick, boost included. */
    public int generating() {
        return generating;
    }

    /** FE/t from heat alone. */
    public int heat() {
        return heat;
    }

    /** How many heat sources it is using. */
    public int heaterCount() {
        return heaters.length;
    }

    /** Ids of the entities heating it (client side too). */
    public int[] heaters() {
        return heaters;
    }

    public int boostTicks() {
        return boostTicks;
    }

    public double boostMultiplier() {
        return boostTicks > 0 ? boostMultiplier : 1.0;
    }

    public ItemStack boostItems() {
        return boostItems;
    }

    public int comparatorSignal() {
        return Math.min(15, (generating + FE_PER_SIGNAL - 1) / FE_PER_SIGNAL);
    }

    /**
     * Puts boost items from {@code stack} into the slot. Returns how many went in (the stack is shrunk by that many,
     * unless {@code player} is in creative mode).
     */
    public int insertBoost(ItemStack stack, @Nullable Player player) {
        if (!canPlaceItem(0, stack)) return 0;
        int room = boostItems.isEmpty() ? stack.getMaxStackSize() : boostItems.getMaxStackSize() - boostItems.getCount();
        int move = Math.min(room, stack.getCount());
        if (move <= 0) return 0;
        if (boostItems.isEmpty()) boostItems = stack.copyWithCount(move);
        else boostItems.grow(move);
        if (player == null || !player.getAbilities().instabuild) stack.shrink(move);
        setChanged();
        return move;
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
        return heatingArea(worldPosition);
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.blaze_furnace");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.BlazeFurnaceMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ container (the boost slot)

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return boostItems.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? boostItems : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || boostItems.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = boostItems.split(count);
        setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack taken = boostItems;
        boostItems = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        boostItems = stack;
        setChanged();
    }

    /** Only boost items go in, and only on top of the same item. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && FurnaceBoostDataMap.of(stack) != null
                && (boostItems.isEmpty() || ItemStack.isSameItemSameComponents(boostItems, stack));
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        boostItems = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** Hoppers and pipes can't take boost items back out. */
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

    private final class FurnaceEnergy extends SimpleEnergyHandler {
        FurnaceEnergy() {
            super(20_000, 0, Integer.MAX_VALUE);
        }

        void generate(int amount) {
            int previous = energy;
            energy = Math.min(capacity, energy + Math.max(0, amount));
            if (energy != previous) onEnergyChanged(previous);
        }

        void resize(int newCapacity) {
            if (capacity == newCapacity) return;
            int previous = energy;
            capacity = newCapacity;
            energy = Math.min(energy, capacity);
            if (energy != previous) onEnergyChanged(previous);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("energy").ifPresent(energy::deserialize);
        boostItems = input.read("boost_items", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        boostTicks = input.getIntOr("boost_ticks", 0);
        boostDuration = input.getIntOr("boost_duration", boostTicks);
        boostMultiplier = input.getDoubleOr("boost_multiplier", 1.0);
        showArea = input.getBooleanOr("show_area", false);
        heaters = input.getIntArray("heaters").orElse(new int[0]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        if (!boostItems.isEmpty()) output.store("boost_items", ItemStack.OPTIONAL_CODEC, boostItems);
        output.putInt("boost_ticks", boostTicks);
        output.putInt("boost_duration", boostDuration);
        output.putDouble("boost_multiplier", boostMultiplier);
        output.putBoolean("show_area", showArea);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients get the area setting and the heaters (for the flames); entity ids only make sense while loaded. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("show_area", showArea);
        tag.putIntArray("heaters", heaters);
        return tag;
    }
}
