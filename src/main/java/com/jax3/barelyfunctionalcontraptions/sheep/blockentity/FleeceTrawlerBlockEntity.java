package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.FleeceTrawlerBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Fleece Trawler: fishes the pool of water in front of it, its winch driven by Tide sheep swimming in that pool
 * (no power). Each haul takes {@code [fleece_trawler] haulTicks} with one sheep in a full-size pool; more sheep (up to
 * {@code maxSheep}), a bigger pool, ocean or river water and rain make it quicker. A haul is the vanilla fishing loot,
 * or, with resource wool in its lure slot, now and then that resource. Now and then its net tangles and it stops until
 * it's cut free with shears; very rarely it drags up a confused sheep.
 *
 * <p>Its pool: the water source blocks in the 7x7 in front of it, from its own level to four blocks down. Its slots:
 * the lure (0) and nine for the catch (1 to 9); hoppers and pipes put lures in and take the catch out.</p>
 */
public class FleeceTrawlerBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame
        implements com.jax3.barelyfunctionalcontraptions.sheep.machine.InventoryHolder, AreaDisplay, net.minecraft.world.MenuProvider,
        com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable {
    public static final int LURE = 0, OUTPUTS = 9, SIZE = 1 + OUTPUTS;
    /** Its pool's reach: 7 wide, 7 out from its front, 4 down. */
    public static final int POOL_HALF_WIDTH = 3, POOL_LENGTH = 7, POOL_DEPTH = 4;
    /** A pool this big (7x7x4) fishes at full speed. */
    public static final int FULL_POOL = (POOL_HALF_WIDTH * 2 + 1) * POOL_LENGTH * POOL_DEPTH;

    public enum Status { HAULING, NO_WATER, NO_SHEEP, TANGLED, OUTPUT_FULL }

    /** Its pool's outline colour: sea green. */
    public static final int AREA_COLOR = 0xFF5FE0C8;
    public static final int DATA_COUNT = 9;
    private boolean showArea;
    /** What its window shows: status, sheep, most sheep, pool, haul %, lure hauls left, hauls a lure lasts, area, hauls. */
    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> status.ordinal();
                case 1 -> sheep;
                case 2 -> ModConfigs.getInt(ModConfigs.SERVER.trawlerMaxSheep);
                case 3 -> pool;
                case 4 -> progressPercent();
                case 5 -> inventory.get(LURE).isEmpty() ? 0 : lureUsesLeft;
                case 6 -> ModConfigs.getInt(ModConfigs.SERVER.trawlerLureUses);
                case 7 -> showArea ? 1 : 0;
                case 8 -> Math.min(hauls, Short.MAX_VALUE);
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

    // Its one module: an inventory of a lure slot (resource wool in, never out) and nine catch slots (hoppers take from
    // them; nothing goes in). Saved as an item list.
    private final com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory = install(trawlerSlots(this));
    /** Its two upgrade cards: a Speed card hauls sooner. */
    private final com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades = install(new com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades(this));
    private Status status = Status.NO_WATER;
    private boolean tangled;
    private float progress;
    private int lureUsesLeft;
    private int pool, sheep, hauls;
    private float speed;

    public FleeceTrawlerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLEECE_TRAWLER.get(), pos, state);
    }

    // ------------------------------------------------------------------ its pool and its sheep

    /** The box its pool is looked for in. */
    public AABB poolBox() {
        Direction facing = getBlockState().getValue(FleeceTrawlerBlock.FACING);
        Direction side = facing.getClockWise();
        BlockPos near = worldPosition.relative(facing).relative(side, -POOL_HALF_WIDTH).below(POOL_DEPTH - 1);
        BlockPos far = worldPosition.relative(facing, POOL_LENGTH).relative(side, POOL_HALF_WIDTH);
        return AABB.encapsulatingFullBlocks(near, far);
    }

    /** How many water source blocks are in its pool. */
    public int countPool(ServerLevel level) {
        AABB box = poolBox();
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed((int) box.minX, (int) box.minY, (int) box.minZ, (int) box.maxX - 1, (int) box.maxY - 1, (int) box.maxZ - 1)) {
            var fluid = level.getFluidState(p);
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) count++;
        }
        return count;
    }

    /** Whether {@code sheep} is a grown Tide sheep (or any sheep with fins), in its fleece or not. */
    public static boolean isTideSheep(Sheep sheep) {
        return !sheep.isBaby() && SheepInfusionEvents.getInfusedResource(sheep).map(r -> r.appearance().has(SheepAppearance.Feature.FINS)).orElse(false);
    }

    /** The Tide sheep swimming in its pool (up to {@code maxSheep}). */
    public int countSheep(ServerLevel level) {
        List<Sheep> swimming = level.getEntitiesOfClass(Sheep.class, poolBox().expandTowards(0, 2, 0), s -> isTideSheep(s) && s.isInWater());
        return Math.min(swimming.size(), ModConfigs.getInt(ModConfigs.SERVER.trawlerMaxSheep));
    }

    /** How fast it hauls now: sheep times pool, times water and rain bonuses (0 with no sheep or too little water). */
    private float speed(ServerLevel level) {
        if (pool < ModConfigs.getInt(ModConfigs.SERVER.trawlerMinPool) || sheep <= 0) return 0;
        float s = sheep * (0.5F + 0.5F * Math.min(1F, pool / (float) FULL_POOL));
        var biome = level.getBiome(worldPosition.relative(getBlockState().getValue(FleeceTrawlerBlock.FACING), 2));
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) s *= (float) ModConfigs.getDouble(ModConfigs.SERVER.trawlerWaterBonus);
        if (level.isRainingAt(worldPosition.above())) s *= (float) ModConfigs.getDouble(ModConfigs.SERVER.trawlerRainBonus);
        return s;
    }

    // ------------------------------------------------------------------ hauling

    /** Ticks a haul takes at speed 1, with its cards: a Speed card hauls sooner. */
    public int haulTicks() {
        return upgradeEffects().ticks(ModConfigs.getInt(ModConfigs.SERVER.trawlerHaulTicks));
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades() {
        return upgrades;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FleeceTrawlerBlockEntity be) {
        if (level.getGameTime() % 20 == Math.floorMod(pos.asLong(), 20)) be.look(level);
        if (be.status != Status.HAULING) return;
        be.progress += be.speed;
        if (be.progress >= be.haulTicks()) {
            be.progress = 0;
            be.haul(level, level.getRandom());
        }
        if (level.getGameTime() % 40 == 0) {
            // The winch turning, and the net in the water.
            Vec3 net = Vec3.atCenterOf(pos.relative(state.getValue(FleeceTrawlerBlock.FACING), 3));
            level.sendParticles(ParticleTypes.FISHING, net.x, pos.getY() + 0.1, net.z, 4, 1.5, 0, 1.5, 0.02);
        }
    }

    /** Looks at its pool and its sheep again, and works out how it's doing. */
    public void look(ServerLevel level) {
        pool = countPool(level);
        sheep = countSheep(level);
        speed = speed(level);
        Status before = status;
        status = tangled ? Status.TANGLED : pool < ModConfigs.getInt(ModConfigs.SERVER.trawlerMinPool) ? Status.NO_WATER
                : sheep <= 0 ? Status.NO_SHEEP : !hasRoom() ? Status.OUTPUT_FULL : Status.HAULING;
        if (status != before) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private boolean hasRoom() {
        for (int i = 1; i < SIZE; i++) if (inventory.get(i).isEmpty()) return true;
        return false;
    }

    /** One haul: now and then a tangle (nothing caught) or a stray sheep; then fish, or with a lure, its resource. */
    public void haul(ServerLevel level, RandomSource random) {
        haul(level, random, random.nextDouble() < ModConfigs.getDouble(ModConfigs.SERVER.trawlerTangleChance),
                random.nextDouble() < ModConfigs.getDouble(ModConfigs.SERVER.trawlerStraySheepChance),
                random.nextDouble() < ModConfigs.getDouble(ModConfigs.SERVER.trawlerLureChance));
    }

    /** One haul, with what happens on it already decided (the tests choose). */
    public void haul(ServerLevel level, RandomSource random, boolean tangles, boolean stray, boolean onLure) {
        if (tangles) {
            tangle(level);
            return;
        }
        if (stray) straySheep(level);
        List<ItemStack> catches = onLure ? lureCatch(level, random) : List.of();
        if (catches.isEmpty()) catches = fish(level);
        for (ItemStack stack : catches) store(level, stack);
        hauls++;
        level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.BLOCKS, 0.8F, 0.9F + random.nextFloat() * 0.2F);
        setChanged();
        look(level);
    }

    /** The vanilla fishing loot, as if from a rod at the middle of its pool. */
    private List<ItemStack> fish(ServerLevel level) {
        Vec3 at = Vec3.atCenterOf(worldPosition.relative(getBlockState().getValue(FleeceTrawlerBlock.FACING), 3));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, at)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        return level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params);
    }

    /**
     * With resource wool in its lure slot: one or two of that resource (what its wool separates into), each lure lasting
     * {@code lureUses} resource hauls. Empty with no lure.
     */
    private List<ItemStack> lureCatch(ServerLevel level, RandomSource random) {
        ItemStack lure = inventory.get(LURE);
        Optional<SheepResource> resource = ResourceWoolItem.getResource(lure, false);
        if (resource.isEmpty()) return List.of();
        if (lureUsesLeft <= 0) {
            lure.shrink(1);
            lureUsesLeft = ModConfigs.getInt(ModConfigs.SERVER.trawlerLureUses);
        }
        lureUsesLeft--;
        ItemStack caught = resource.get().separation().primary().create().copyWithCount(1 + random.nextInt(2));
        ModAdvancements.awardNearby(level, worldPosition, 16, ModAdvancements.REEL_EWE_IN);
        return List.of(caught);
    }

    private void store(ServerLevel level, ItemStack stack) {
        for (int i = 1; i < SIZE && !stack.isEmpty(); i++) {
            ItemStack in = inventory.get(i);
            if (in.isEmpty()) {
                inventory.set(i, stack.copy());
                stack.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(in, stack)) {
                int moved = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
                in.grow(moved);
                stack.shrink(moved);
            }
        }
        if (!stack.isEmpty()) {
            // No room for the last of it: it spills onto the block.
            ItemEntity spill = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, stack);
            level.addFreshEntity(spill);
        }
    }

    /** The net tangles: it stops until someone cuts it free with shears. */
    public void tangle(ServerLevel level) {
        tangled = true;
        level.playSound(null, worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        setChanged();
        look(level);
    }

    public boolean tangled() {
        return tangled;
    }

    /** Cut free with shears: back to work, and a little string from the net. */
    public void untangle(ServerLevel level, Player player) {
        tangled = false;
        level.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
        ItemStack string = new ItemStack(Items.STRING, 1 + level.getRandom().nextInt(2));
        if (!player.getInventory().add(string)) {
            level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), string));
        }
        setChanged();
        look(level);
    }

    /** A plain sheep that wandered too near, hauled up dripping onto the bank beside it. */
    private void straySheep(ServerLevel level) {
        Sheep stray = com.jax3.barelyfunctionalcontraptions.compat.Vanilla.SHEEP.create(level, EntitySpawnReason.TRIGGERED);
        if (stray == null) return;
        Direction side = getBlockState().getValue(FleeceTrawlerBlock.FACING).getClockWise();
        BlockPos at = worldPosition.relative(side);
        stray.snapTo(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        level.addFreshEntity(stray);
        level.sendParticles(ParticleTypes.SPLASH, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 20, 0.4, 0.3, 0.4, 0.1);
        level.playSound(null, at, SoundEvents.SHEEP_AMBIENT, SoundSource.NEUTRAL, 1.0F, 0.7F);
    }

    // ------------------------------------------------------------------ for players, windows, panels and tests

    public Status status() {
        return status;
    }

    public String statusKey() {
        return status.name().toLowerCase(Locale.ROOT);
    }

    public int pool() {
        return pool;
    }

    public int sheepCount() {
        return sheep;
    }

    public int hauls() {
        return hauls;
    }

    public int lureUsesLeft() {
        return lureUsesLeft;
    }

    /** How far through the haul it is, 0 to 100. */
    public int progressPercent() {
        return Mth.clamp((int) (100 * progress / haulTicks()), 0, 100);
    }

    /** Puts resource wool in its lure slot from {@code stack}; how many went in. */
    public int addLure(ItemStack stack) {
        if (!canPlaceItem(LURE, stack)) return 0;
        ItemStack lure = inventory.get(LURE);
        int room = lure.isEmpty() ? stack.getMaxStackSize() : lure.getMaxStackSize() - lure.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) return 0;
        if (lure.isEmpty()) inventory.set(LURE, stack.copyWithCount(moved));
        else lure.grow(moved);
        stack.shrink(moved);
        changedContents();
        return moved;
    }

    /** 0 empty, then 1 to 15 with how many of its nine catch slots hold something. */
    public int comparatorSignal() {
        int used = 0;
        for (int i = 1; i < SIZE; i++) if (!inventory.get(i).isEmpty()) used++;
        return used == 0 ? 0 : Math.min(15, 1 + used * 14 / OUTPUTS);
    }

    private void changedContents() {
        setChanged();
        if (level instanceof ServerLevel server) look(server);
    }

    // ------------------------------------------------------------------ its pool's outline, and its window

    @Override
    public boolean showsArea() {
        return showArea;
    }

    @Override
    public void setShowsArea(boolean show) {
        showArea = show;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public AABB displayedArea() {
        return poolBox();
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.fleece_trawler");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.FleeceTrawlerMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ its slots (the inventory module)

    private static com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory trawlerSlots(FleeceTrawlerBlockEntity trawler) {
        var builder = com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory.builder().savedAsList()
                .slot(null, trawler::acceptsLure, false);
        for (int i = 0; i < OUTPUTS; i++) builder.slot(null, stack -> false, true);
        return builder.build();
    }

    /** Only resource wool goes in its lure slot, and only the same as the lure already there. */
    private boolean acceptsLure(ItemStack stack) {
        if (ResourceWoolItem.getResourceId(stack) == null) return false;
        ItemStack lure = inventory.get(LURE);
        return lure.isEmpty() || ItemStack.isSameItemSameComponents(lure, stack);
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory() {
        return inventory;
    }

    @Override
    public void contentsChanged() {
        changedContents();
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) inventory.set(i, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ saving and syncing

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); // its module: the lure and catch
        tangled = input.getBooleanOr("tangled", false);
        progress = input.getFloatOr("progress", 0);
        lureUsesLeft = input.getIntOr("lure_uses_left", 0);
        hauls = input.getIntOr("hauls", 0);
        status = Status.values()[Math.floorMod(input.getIntOr("status", Status.NO_WATER.ordinal()), Status.values().length)];
        pool = input.getIntOr("pool", 0);
        sheep = input.getIntOr("sheep", 0);
        showArea = input.getBooleanOr("show_area", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); // its module
        output.putBoolean("tangled", tangled);
        output.putFloat("progress", progress);
        output.putInt("lure_uses_left", lureUsesLeft);
        output.putInt("hauls", hauls);
        output.putInt("status", status.ordinal());
        output.putInt("pool", pool);
        output.putInt("sheep", sheep);
        output.putBoolean("show_area", showArea);
    }

    /** Clients get everything it saves (Jade and its window read it). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
