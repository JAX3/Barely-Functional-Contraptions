package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.HerdCounterBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusion;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepStats;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Herd Counter: counts the sheep in the pen in front of its screen and puts the count on a comparator. It needs no
 * power.
 *
 * <ul>
 *     <li><b>Area:</b> {@code radius} (3) blocks out, so the 7x7 in front of its screen, from a block below it to three
 *     above. Every {@code interval} (a second) it counts the sheep in it.</li>
 *     <li><b>Counts:</b> all of them, the grown ones and the lambs, the grown ones ready to shear (fleece on) and the
 *     sheared ones, the ones low on infusion, how many of each resource, and their average and best stats.</li>
 *     <li><b>Mode:</b> what its screen shows and its comparator reads ({@link Mode}): every sheep, the ones ready to
 *     shear, the sheared ones, the lambs, the ones low on infusion, or Full (15 once there are as many as its target).
 *     Each sheep counted adds {@code signalPerSheep} (1), up to 15.</li>
 *     <li><b>Filter:</b> a resource wool or spawn egg (or plain wool, or a plain sheep's egg) narrows every mode to the
 *     sheep of that resource.</li>
 * </ul>
 */
public class HerdCounterBlockEntity extends BlockEntity implements AreaDisplay {
    public static final int AREA_COLOR = 0xFFE8ECF5;
    /** The filter key for sheep with no infusion. */
    public static final String PLAIN = "plain";
    /** Sheep it must count at once for the players near it to earn Count Your Sheep. */
    public static final int ADVANCEMENT_SHEEP = 20;

    /** What its screen shows and its comparator reads. */
    public enum Mode {
        COUNT(s -> true),
        READY(s -> !s.isBaby() && !s.isSheared()),
        SHEARED(s -> !s.isBaby() && s.isSheared()),
        LAMBS(Sheep::isBaby),
        LOW_INFUSION(HerdCounterBlockEntity::lowOnInfusion),
        FULL(s -> true);

        final Predicate<Sheep> counts;

        Mode(Predicate<Sheep> counts) {
            this.counts = counts;
        }

        public static Mode byId(int id) {
            Mode[] all = values();
            return all[Math.floorMod(id, all.length)];
        }

        public Mode next() {
            return byId(ordinal() + 1);
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private Mode mode = Mode.COUNT;
    private ItemStack filter = ItemStack.EMPTY;
    private int target = -1;
    private boolean showArea;
    private int cooldown;
    private int lastSignal;

    /** What it counted last time. */
    private int total, lambs, ready, sheared, low, value;
    private final Map<String, Integer> byResource = new LinkedHashMap<>();
    private float averageYield, averageGrowth, averageEndurance;
    private SheepStats best = SheepStats.WILD;

    public HerdCounterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HERD_COUNTER.get(), pos, state);
    }

    public static int radius() {
        return ModConfigs.getInt(ModConfigs.SERVER.herdRadius);
    }

    /** The pen it watches: the square {@code radius} out, in front of its screen. */
    public static AABB area(BlockPos pos, Direction facing) {
        int r = radius();
        BlockPos centre = pos.relative(facing, r + 1);
        return new AABB(centre.getX() - r, pos.getY() - 1, centre.getZ() - r, centre.getX() + r + 1, pos.getY() + 4, centre.getZ() + r + 1);
    }

    /** Whether {@code sheep} has an infusion with less than {@code lowInfusion} of its uses left. */
    public static boolean lowOnInfusion(Sheep sheep) {
        SheepInfusion infusion = SheepInfusionEvents.getInfusion(sheep);
        if (!infusion.isInfused()) return false;
        return SheepInfusionEvents.getInfusedResource(sheep)
                .map(r -> infusion.usesRemaining() < ModConfigs.getDouble(ModConfigs.SERVER.herdLowInfusion) * SheepInfusionEvents.infusionUses(sheep, r))
                .orElse(false);
    }

    /** The resource a sheep is counted under: its infusion's id, or {@link #PLAIN}. */
    public static String resourceKey(Sheep sheep) {
        return SheepInfusionEvents.getInfusion(sheep).resource().map(Identifier::toString).orElse(PLAIN);
    }

    /**
     * The resource an item stands for as a filter: a resource wool or spawn egg its resource, plain wool or a sheep's
     * spawn egg {@link #PLAIN}; anything else null (not a filter).
     */
    public static @Nullable String filterKey(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier wool = ResourceWoolItem.getResourceId(stack);
        if (wool != null) return wool.toString();
        if (stack.getItem() instanceof ResourceSheepSpawnEggItem) {
            Identifier egg = stack.get(ModDataComponents.SHEEP_RESOURCE.get());
            return egg == null ? null : egg.toString();
        }
        if (stack.is(ItemTags.WOOL) || stack.is(Items.SHEEP_SPAWN_EGG)) return PLAIN;
        return null;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, HerdCounterBlockEntity be) {
        if (--be.cooldown > 0) return;
        be.cooldown = ModConfigs.getInt(ModConfigs.SERVER.herdInterval);
        be.count(level, pos, state);
    }

    /** Counts the sheep in its area, then updates its screen and its comparator. */
    public void count(ServerLevel level, BlockPos pos, BlockState state) {
        AABB area = area(pos, state.getValue(HerdCounterBlock.FACING));
        List<Sheep> sheep = level.getEntitiesOfClass(Sheep.class, area, s -> s.isAlive() && area.contains(s.position()));
        total = sheep.size();
        lambs = ready = sheared = low = 0;
        byResource.clear();
        long yield = 0, growth = 0, endurance = 0;
        int bestYield = 1, bestGrowth = 1, bestEndurance = 1;
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        String key = filterKey(filter);
        int matched = 0;
        for (Sheep s : sheep) {
            if (s.isBaby()) lambs++;
            else if (s.isSheared()) sheared++;
            else ready++;
            if (lowOnInfusion(s)) low++;
            counts.merge(resourceKey(s), 1, Integer::sum);
            SheepStats stats = SheepInfusionEvents.getStats(s);
            yield += stats.yield();
            growth += stats.growth();
            endurance += stats.endurance();
            bestYield = Math.max(bestYield, stats.yield());
            bestGrowth = Math.max(bestGrowth, stats.growth());
            bestEndurance = Math.max(bestEndurance, stats.endurance());
            if ((key == null || key.equals(resourceKey(s))) && mode.counts.test(s)) matched++;
        }
        sorted.addAll(counts.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        for (var e : sorted) byResource.put(e.getKey(), e.getValue());
        averageYield = total == 0 ? 0 : yield / (float) total;
        averageGrowth = total == 0 ? 0 : growth / (float) total;
        averageEndurance = total == 0 ? 0 : endurance / (float) total;
        best = new SheepStats(bestYield, bestGrowth, bestEndurance);
        if (matched != value) {
            value = matched;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        if (total >= ADVANCEMENT_SHEEP) ModAdvancements.awardNearby(level, pos, ModAdvancements.AUTO_SHEAR_RADIUS, ModAdvancements.COUNT_YOUR_SHEEP);
    }

    // ------------------------------------------------------------------ settings

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        cooldown = 0; // recount at once
        sync();
    }

    public ItemStack filter() {
        return filter;
    }

    /** Filters to {@code stack}'s resource (a copy of one is kept), or clears the filter with an empty stack. */
    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        cooldown = 0;
        sync();
    }

    /** How many sheep Full mode waits for. */
    public int target() {
        return target > 0 ? target : ModConfigs.getInt(ModConfigs.SERVER.herdFullTarget);
    }

    public void setTarget(int target) {
        this.target = Math.max(1, Math.min(64, target));
        cooldown = 0;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ readings

    /** What its mode counts (with its filter): the number on its screen. */
    public int value() {
        return value;
    }

    public int total() {
        return total;
    }

    public int lambs() {
        return lambs;
    }

    public int ready() {
        return ready;
    }

    public int sheared() {
        return sheared;
    }

    public int lowInfusion() {
        return low;
    }

    /** Sheep of each resource ({@link #PLAIN} for none), most first. */
    public Map<String, Integer> byResource() {
        return byResource;
    }

    public float averageYield() {
        return averageYield;
    }

    public float averageGrowth() {
        return averageGrowth;
    }

    public float averageEndurance() {
        return averageEndurance;
    }

    public SheepStats best() {
        return best;
    }

    /** Full: 15 once it counts its target, else 0. Every other mode: {@code signalPerSheep} for each sheep, up to 15. */
    public int comparatorSignal() {
        if (mode == Mode.FULL) return value >= target() ? 15 : 0;
        return Math.min(15, value * ModConfigs.getInt(ModConfigs.SERVER.herdSignalPerSheep));
    }

    @Override
    public boolean showsArea() {
        return showArea;
    }

    @Override
    public void setShowsArea(boolean show) {
        showArea = show;
        sync();
    }

    @Override
    public AABB displayedArea() {
        return area(worldPosition, getBlockState().getValue(HerdCounterBlock.FACING));
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        mode = Mode.byId(in.getIntOr("mode", 0));
        filter = in.read("filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        target = in.getIntOr("target", -1);
        showArea = in.getBooleanOr("show_area", false);
        value = in.getIntOr("value", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("mode", mode.ordinal());
        if (!filter.isEmpty()) out.store("filter", ItemStack.OPTIONAL_CODEC, filter);
        out.putInt("target", target);
        out.putBoolean("show_area", showArea);
        out.putInt("value", value);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients get what its screen shows: the count, the mode, the target, the filter's resource and the area setting. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("mode", mode.ordinal());
        tag.putInt("value", value);
        tag.putInt("target", target());
        tag.putBoolean("show_area", showArea);
        String key = filterKey(filter);
        if (key != null) tag.putString("filter_key", key);
        return tag;
    }

    /** The filter's resource as the client knows it (from {@link #getUpdateTag}), or null. */
    private @Nullable String clientFilter;

    @Override
    public void handleUpdateTag(ValueInput input) {
        mode = Mode.byId(input.getIntOr("mode", 0));
        value = input.getIntOr("value", 0);
        target = input.getIntOr("target", -1);
        showArea = input.getBooleanOr("show_area", false);
        clientFilter = input.getString("filter_key").orElse(null);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ValueInput input) {
        handleUpdateTag(input);
    }

    /** The filter's resource key, on either side. */
    public @Nullable String filterResource() {
        String key = filterKey(filter);
        return key != null ? key : clientFilter;
    }
}
