package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.GrassSeederBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * The Grass Seeder: keeps the ground of a sheep pen green, so sheared sheep regrow their wool sooner.
 *
 * <ul>
 *     <li><b>Area:</b> the ground it stands on, {@code radius} blocks out (5x5 by default), and the sheep standing on it.</li>
 *     <li><b>Tending:</b> every {@code tickInterval} (2 s) it tends one block: dirt that a sheep has eaten bare goes back
 *     to grass; once there's no bare dirt, it grows short grass on the grass, up to {@code shortGrass} (40%) of it, for
 *     the sheep to eat instead of the grass block.</li>
 *     <li><b>Fuel:</b> each block it tends takes {@code energyPerTick} x {@code tickInterval} FE (800) and
 *     {@code waterPerBlock} mB of water (10), or, without those, an eighth of a Bone Meal ({@code boneMealBlocks}).
 *     Power and water come in on every face (cables, pipes, a bucket); Bone Meal by hand, hopper or pipe.</li>
 *     <li><b>Grazing:</b> while it has fuel, sheared sheep in its area graze {@code grazeMultiplier} (4x) as often as
 *     they would on their own, so their wool grows back sooner.</li>
 * </ul>
 * Its comparator signal reads how green its area is.
 */
public class GrassSeederBlockEntity extends BlockEntity implements MachineContainer, WorldlyContainer, AreaDisplay, net.minecraft.world.MenuProvider {
    public static final int AREA_COLOR = 0xFF6FCF4A;
    /** The ground map in its window covers areas up to 7x7 (radius 3), three bits a block, five blocks to a value. */
    public static final int MAP_MAX = 7, MAP_VALUES = 10, MAP_PER_VALUE = 5, MAP_BITS = 3;
    /** What a block on the ground map is. */
    public static final int TILE_OTHER = 0, TILE_DIRT = 1, TILE_GRASS = 2, TILE_SHORT = 3, TILE_TALL = 4;
    /** Where the ground map starts in its synced values. */
    public static final int MAP_DATA = 20;
    /** Values synced to the window ({@link com.jax3.barelyfunctionalcontraptions.sheep.menu.GrassSeederMenu}). */
    public static final int DATA_COUNT = MAP_DATA + MAP_VALUES;
    /** Blocks it must tend for the players near it to earn Keep Off the Grass. */
    public static final int ADVANCEMENT_BLOCKS = 100;
    private static final int[] SLOTS = {0};
    public static final FluidResource WATER = WaterTank.WATER;

    /** What it's doing, for its window, Jade and its sprinkler. */
    public enum Status {
        /** Turning dirt back to grass or growing short grass. */
        TENDING(0xFF8FE07A),
        /** Its area is as green as it keeps it: waiting for the sheep to eat some. */
        ALL_GREEN(0xFF8FE07A),
        /** It has something to tend but no Bone Meal, and not enough power and water. */
        NO_FUEL(0xFFE35D5D),
        /** Its ground is green and it has nothing to do, so it's growing tall grass: Trim keeps it short. */
        OVERGROWN(0xFFE0B04A),
        /** No grass or dirt in its area to tend. */
        NO_GROUND(0xFFAEB4C6);

        public final int color;

        Status(int color) {
            this.color = color;
        }

        public static Status byId(int id) {
            Status[] all = values();
            return all[Math.floorMod(id, all.length)];
        }
    }

    private final MachineEnergy energy = new MachineEnergy(
            () -> ModConfigs.getInt(ModConfigs.SERVER.seederCapacity), () -> ModConfigs.getInt(ModConfigs.SERVER.seederMaxInput), this::setChanged);
    private final WaterTank tank = new WaterTank(4_000, this::setChanged);
    private ItemStack boneMeal = ItemStack.EMPTY;
    /** Blocks left from the Bone Meal it last opened. */
    private int boneCharges;
    private int cooldown;
    private boolean showArea;
    private Status status = Status.NO_GROUND;
    private int grass, dirt, shortGrass, tallGrass;
    /** Whether it cuts tall grass back to short grass, instead of letting it grow. */
    private boolean trim;
    /** Blocks it has tended in all. */
    private long tended;
    private int lastSignal;
    /** Sheared, grown sheep in its area at the last check. */
    private int grazing;
    private final int[] map = new int[MAP_VALUES];

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            if (index >= MAP_DATA) return map[index - MAP_DATA];
            long tendedCapped = Math.min(Integer.MAX_VALUE, tended);
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> tank.getAmount() & 0xFFFF;
                case 5 -> tank.getAmount() >>> 16;
                case 6 -> tank.capacity() & 0xFFFF;
                case 7 -> tank.capacity() >>> 16;
                case 8 -> status.ordinal();
                case 9 -> showArea ? 1 : 0;
                case 10 -> grass;
                case 11 -> dirt;
                case 12 -> shortGrass;
                case 13 -> boneCharges;
                case 14 -> radius();
                case 15 -> grazing;
                case 16 -> (int) (tendedCapped & 0xFFFF);
                case 17 -> (int) (tendedCapped >>> 16);
                case 18 -> tallGrass;
                case 19 -> trim ? 1 : 0;
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

    public GrassSeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRASS_SEEDER.get(), pos, state);
    }

    public static int radius() {
        return ModConfigs.getInt(ModConfigs.SERVER.seederRadius);
    }

    public static int interval() {
        return ModConfigs.getInt(ModConfigs.SERVER.seederInterval);
    }

    /** FE each block takes when it runs on power. */
    public static int energyPerBlock() {
        return ModConfigs.getInt(ModConfigs.SERVER.seederEnergyPerTick) * interval();
    }

    public static int waterPerBlock() {
        return ModConfigs.getInt(ModConfigs.SERVER.seederWaterPerBlock);
    }

    /** The sheep it watches: standing on its ground, {@code radius} out, at its own level. */
    public static AABB area(BlockPos pos) {
        int r = radius();
        return new AABB(pos.getX() - r, pos.getY(), pos.getZ() - r, pos.getX() + r + 1, pos.getY() + 2, pos.getZ() + r + 1);
    }

    /** The ground it tends: the blocks at its feet, {@code radius} out, not the one under it. */
    public static List<BlockPos> ground(BlockPos pos) {
        int r = radius();
        List<BlockPos> list = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx != 0 || dz != 0) list.add(pos.offset(dx, -1, dz));
            }
        }
        return list;
    }

    /**
     * Each check, a sheared sheep in the area grazes with the chance that makes it graze {@code grazeMultiplier} times as
     * often as it would on its own (a grown sheep on its own: 1 in 1000 a tick), less what it already does.
     */
    public static double grazeChance() {
        double extra = (ModConfigs.getDouble(ModConfigs.SERVER.seederGrazeMultiplier) - 1.0) / 1000.0;
        return extra <= 0 ? 0 : 1.0 - Math.pow(1.0 - Math.min(1.0, extra), interval());
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GrassSeederBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.refreshLimits();
        tank.resize(ModConfigs.getInt(ModConfigs.SERVER.seederTankCapacity));
        if (--cooldown <= 0) {
            cooldown = interval();
            work(level, pos);
        }
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        boolean active = status == Status.TENDING;
        if (state.getValue(GrassSeederBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(GrassSeederBlock.ACTIVE, active), Block.UPDATE_ALL);
    }

    /** What it does to a block on a turn. */
    private enum Job { REGRASS, TRIM, PLANT, OVERGROW }

    /**
     * Tends one block (if anything needs it and it has the fuel), then lets the sheared sheep graze. In order: bare
     * dirt goes back to grass; with Trim on, tall grass is cut back to short grass; short grass is grown up to its share
     * of the grass; and, with nothing else to do and Trim off, it overgrows: now and then a tuft of short grass shoots up
     * into tall grass, which the sheep can't eat and Automatic Shears can't see them in.
     */
    public void work(ServerLevel level, BlockPos pos) {
        List<BlockPos> bare = new ArrayList<>(), plantable = new ArrayList<>(), tall = new ArrayList<>(), tufts = new ArrayList<>();
        grass = dirt = shortGrass = tallGrass = 0;
        for (BlockPos p : ground(pos)) {
            BlockState state = level.getBlockState(p), above = level.getBlockState(p.above());
            boolean isDirt = state.is(Blocks.DIRT), isGrass = state.is(Blocks.GRASS_BLOCK);
            if (!isDirt && !isGrass) continue;
            if (isDirt) dirt++;
            else grass++;
            if (above.is(Blocks.TALL_GRASS)) {
                tallGrass++;
                tall.add(p);
            } else if (isDirt) {
                if (above.isAir()) bare.add(p);
            } else if (above.is(Blocks.SHORT_GRASS)) {
                shortGrass++;
                if (level.getBlockState(p.above(2)).isAir()) tufts.add(p);
            } else if (above.isAir()) {
                plantable.add(p);
            }
        }
        grazing = level.getEntitiesOfClass(Sheep.class, area(pos), s -> s.isAlive() && s.isSheared() && !s.isBaby()).size();
        Job job = null;
        List<BlockPos> targets = List.of();
        if (!bare.isEmpty()) {
            job = Job.REGRASS;
            targets = bare;
        } else if (trim && !tall.isEmpty()) {
            job = Job.TRIM;
            targets = tall;
        } else if (shortGrass < Math.round(ModConfigs.getDouble(ModConfigs.SERVER.seederShortGrass) * grass) && !plantable.isEmpty()) {
            job = Job.PLANT;
            targets = plantable;
        } else if (!trim && ModConfigs.SERVER.seederOvergrowth.get() && !tufts.isEmpty()
                && level.getRandom().nextDouble() < ModConfigs.getDouble(ModConfigs.SERVER.seederOvergrowthChance)) {
            job = Job.OVERGROW;
            targets = tufts;
        }
        if (job == null) {
            status = grass + dirt == 0 ? Status.NO_GROUND : tallGrass > 0 && !trim ? Status.OVERGROWN : Status.ALL_GREEN;
        } else if (!useFuel()) {
            status = Status.NO_FUEL;
        } else {
            BlockPos target = targets.get(level.getRandom().nextInt(targets.size()));
            status = Status.TENDING;
            switch (job) {
                case REGRASS -> {
                    level.setBlock(target, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target.above(), 0);
                    grass++;
                    dirt--;
                }
                case TRIM -> {
                    // Upper half first, so the lower one doesn't pop off as an item.
                    level.setBlock(target.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    level.setBlock(target.above(), Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
                    level.playSound(null, target.above(), net.minecraft.sounds.SoundEvents.GROWING_PLANT_CROP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    tallGrass--;
                    shortGrass++;
                }
                case PLANT -> {
                    level.setBlock(target.above(), Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
                    level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target.above(), 0);
                    shortGrass++;
                }
                case OVERGROW -> {
                    net.minecraft.world.level.block.DoublePlantBlock.placeAt(level, Blocks.TALL_GRASS.defaultBlockState(), target.above(), Block.UPDATE_ALL);
                    level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target.above(), 0);
                    shortGrass--;
                    tallGrass++;
                    status = Status.OVERGROWN;
                }
            }
            tended++;
            setChanged();
            if (tended >= ADVANCEMENT_BLOCKS) {
                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.awardNearby(level, pos,
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.AUTO_SHEAR_RADIUS,
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.KEEP_OFF_THE_GRASS);
            }
        }
        if (status != Status.NO_FUEL && hasFuel()) graze(level, pos);
        updateMap(level, pos);
    }

    /** What each block of its ground is, for the map in its window (areas up to {@link #MAP_MAX} across). */
    private void updateMap(ServerLevel level, BlockPos pos) {
        java.util.Arrays.fill(map, 0);
        int r = radius();
        if (r * 2 + 1 > MAP_MAX) return;
        int i = 0;
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++, i++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos p = pos.offset(dx, -1, dz);
                BlockState state = level.getBlockState(p);
                BlockState above = level.getBlockState(p.above());
                boolean ground = state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK);
                int tile = !ground ? TILE_OTHER : above.is(Blocks.TALL_GRASS) ? TILE_TALL : state.is(Blocks.DIRT) ? TILE_DIRT
                        : above.is(Blocks.SHORT_GRASS) ? TILE_SHORT : TILE_GRASS;
                map[i / MAP_PER_VALUE] |= tile << ((i % MAP_PER_VALUE) * MAP_BITS);
            }
        }
    }

    /** The tile at {@code index} (row by row from the north-west) of a packed ground map. */
    public static int tile(int[] packed, int index) {
        return (packed[index / MAP_PER_VALUE] >> ((index % MAP_PER_VALUE) * MAP_BITS)) & 7;
    }

    /** Sheared, grown sheep in its area graze sooner: they eat the short grass or the grass under them, and regrow. */
    private void graze(ServerLevel level, BlockPos pos) {
        double chance = grazeChance();
        if (chance <= 0) return;
        AABB area = area(pos);
        for (Sheep sheep : level.getEntitiesOfClass(Sheep.class, area, s -> s.isAlive() && s.isSheared() && !s.isBaby())) {
            if (level.getRandom().nextDouble() >= chance) continue;
            graze(level, sheep);
        }
    }

    /** {@code sheep} eats what it's standing in or on, as it does on its own; true if it did. */
    public static boolean graze(ServerLevel level, Sheep sheep) {
        BlockPos at = sheep.blockPosition(), below = at.below();
        boolean grief = net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, sheep);
        if (level.getBlockState(at).is(BlockTags.EDIBLE_FOR_SHEEP)) {
            if (grief) level.destroyBlock(at, false);
        } else if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
            if (grief) {
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, below, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        } else {
            return false;
        }
        level.broadcastEntityEvent(sheep, (byte) 10); // its head goes down to eat
        sheep.ate();
        return true;
    }

    /** Power and water first; then Bone Meal. True if it paid for a block. */
    private boolean useFuel() {
        if (energy.getAmountAsInt() >= energyPerBlock() && tank.getAmount() >= waterPerBlock()) {
            energy.drain(energyPerBlock());
            tank.drain(waterPerBlock());
            return true;
        }
        if (boneCharges > 0) {
            boneCharges--;
            return true;
        }
        if (!boneMeal.isEmpty()) {
            boneMeal.shrink(1);
            boneCharges = ModConfigs.getInt(ModConfigs.SERVER.seederBoneMealBlocks) - 1;
            setChanged();
            return true;
        }
        return false;
    }

    public boolean hasFuel() {
        return (energy.getAmountAsInt() >= energyPerBlock() && tank.getAmount() >= waterPerBlock()) || boneCharges > 0 || !boneMeal.isEmpty();
    }

    // ------------------------------------------------------------------ accessors

    public Status status() {
        return status;
    }

    public int grass() {
        return grass;
    }

    public int dirt() {
        return dirt;
    }

    public int shortGrass() {
        return shortGrass;
    }

    public int tallGrass() {
        return tallGrass;
    }

    public boolean trims() {
        return trim;
    }

    /** Switches Trim (cutting tall grass back) on or off. */
    public void setTrim(boolean on) {
        trim = on;
        setChanged();
    }

    public long tended() {
        return tended;
    }

    public int boneCharges() {
        return boneCharges;
    }

    /** Sheared, grown sheep in its area at the last check. */
    public int grazing() {
        return grazing;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.grass_seeder");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.GrassSeederMenu(containerId, inventory, this, data);
    }

    public ItemStack boneMeal() {
        return boneMeal;
    }

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    public WaterTank getTank() {
        return tank;
    }

    /** How green its area is, 0-15. */
    public int comparatorSignal() {
        int ground = grass + dirt;
        return ground == 0 ? 0 : grass * 15 / ground;
    }

    /** Puts as much of {@code stack} (Bone Meal) in as fits, taking it from the stack; how many went in. */
    public int insertBoneMeal(ItemStack stack) {
        if (!stack.is(Items.BONE_MEAL)) return 0;
        int room = boneMeal.isEmpty() ? stack.getMaxStackSize() : boneMeal.getMaxStackSize() - boneMeal.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) return 0;
        if (boneMeal.isEmpty()) boneMeal = stack.copyWithCount(moved);
        else boneMeal.grow(moved);
        stack.shrink(moved);
        setChanged();
        return moved;
    }

    /** Pours a bucket's worth of water in, if it fits. */
    public boolean fillFromBucket() {
        return tank.fillFromBucket();
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
        int r = radius();
        BlockPos p = worldPosition;
        return new AABB(p.getX() - r, p.getY() - 1, p.getZ() - r, p.getX() + r + 1, p.getY() + 1, p.getZ() + r + 1);
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    // ------------------------------------------------------------------ container (the Bone Meal, in only)

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? boneMeal : ItemStack.EMPTY;
    }

    @Override
    public void putItem(int slot, ItemStack stack) {
        if (slot == 0) boneMeal = stack;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(Items.BONE_MEAL);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return stack.is(Items.BONE_MEAL);
    }

    /** Bone Meal only goes in. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, this);
    }

    // ------------------------------------------------------------------ power and water

    /** Fills its power at once (GameTests, the screenshot harness). */
    public void fillEnergy() {
        energy.refreshLimits();
        energy.fill();
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(energy::deserialize);
        in.child("tank").ifPresent(tank::deserialize);
        boneMeal = in.read("bone_meal", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        boneCharges = in.getIntOr("bone_charges", 0);
        tended = in.getLongOr("tended", 0L);
        trim = in.getBooleanOr("trim", false);
        showArea = in.getBooleanOr("show_area", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        energy.serialize(out.child("energy"));
        tank.serialize(out.child("tank"));
        if (!boneMeal.isEmpty()) out.store("bone_meal", ItemStack.OPTIONAL_CODEC, boneMeal);
        out.putInt("bone_charges", boneCharges);
        out.putLong("tended", tended);
        out.putBoolean("trim", trim);
        out.putBoolean("show_area", showArea);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients only need the area setting, for the outline. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("show_area", showArea);
        return tag;
    }
}
