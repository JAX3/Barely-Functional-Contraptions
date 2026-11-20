package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.GrassSeederBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
public class GrassSeederBlockEntity extends BlockEntity implements WorldlyContainer, AreaDisplay {
    public static final int AREA_COLOR = 0xFF6FCF4A;
    private static final int[] SLOTS = {0};
    public static final FluidResource WATER = FluidResource.of(Fluids.WATER);

    /** What it's doing, for its readout and its sprinkler. */
    public enum Status {
        /** Turning dirt back to grass or growing short grass. */
        TENDING(0xFF8FE07A),
        /** Its area is as green as it keeps it: waiting for the sheep to eat some. */
        ALL_GREEN(0xFF8FE07A),
        /** It has something to tend but no Bone Meal, and not enough power and water. */
        NO_FUEL(0xFFE35D5D),
        /** No grass or dirt in its area to tend. */
        NO_GROUND(0xFFAEB4C6);

        public final int color;

        Status(int color) {
            this.color = color;
        }
    }

    private final SeederEnergy energy = new SeederEnergy();
    private final SeederTank tank = new SeederTank();
    private ItemStack boneMeal = ItemStack.EMPTY;
    /** Blocks left from the Bone Meal it last opened. */
    private int boneCharges;
    private int cooldown;
    private boolean showArea;
    private Status status = Status.NO_GROUND;
    private int grass, dirt, shortGrass;
    /** Blocks it has tended in all. */
    private long tended;
    private int lastSignal;

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

    /** Tends one block (if anything needs it and it has the fuel), then lets the sheared sheep graze. */
    public void work(ServerLevel level, BlockPos pos) {
        List<BlockPos> bare = new ArrayList<>(), plantable = new ArrayList<>();
        grass = dirt = shortGrass = 0;
        for (BlockPos p : ground(pos)) {
            BlockState state = level.getBlockState(p), above = level.getBlockState(p.above());
            if (state.is(Blocks.DIRT)) {
                dirt++;
                if (above.isAir()) bare.add(p);
            } else if (state.is(Blocks.GRASS_BLOCK)) {
                grass++;
                if (above.is(Blocks.SHORT_GRASS)) shortGrass++;
                else if (above.isAir()) plantable.add(p);
            }
        }
        BlockPos target = null;
        boolean regrass = false;
        if (!bare.isEmpty()) {
            target = bare.get(level.getRandom().nextInt(bare.size()));
            regrass = true;
        } else if (shortGrass < Math.round(ModConfigs.getDouble(ModConfigs.SERVER.seederShortGrass) * grass) && !plantable.isEmpty()) {
            target = plantable.get(level.getRandom().nextInt(plantable.size()));
        }
        if (target == null) {
            status = grass + dirt == 0 ? Status.NO_GROUND : Status.ALL_GREEN;
        } else if (!useFuel()) {
            status = Status.NO_FUEL;
        } else {
            status = Status.TENDING;
            if (regrass) {
                level.setBlock(target, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target.above(), 0);
                grass++;
                dirt--;
            } else {
                level.setBlock(target.above(), Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target.above(), 0);
                shortGrass++;
            }
            tended++;
            setChanged();
        }
        if (status != Status.NO_FUEL && hasFuel()) graze(level, pos);
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
            energy.consume(energyPerBlock());
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

    public long tended() {
        return tended;
    }

    public int boneCharges() {
        return boneCharges;
    }

    public ItemStack boneMeal() {
        return boneMeal;
    }

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    public SeederTank getTank() {
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
        if (tank.capacity() - tank.getAmount() < 1000) return false;
        tank.fill(1000);
        return true;
    }

    /** One line on how it's doing, for a right-click: its status, how green its area is, and its fuel. */
    public Component describe() {
        String key = "message.barelyfunctionalcontraptions.grass_seeder.";
        MutableComponent line = Component.translatable(key + "status." + status.name().toLowerCase(Locale.ROOT)).withColor(status.color);
        line.append(Component.literal("  ")).append(Component.translatable(key + "grass", grass, grass + dirt).withStyle(ChatFormatting.GREEN));
        line.append(Component.literal("  ")).append(Component.translatable(key + "bone_meal", boneMeal.getCount(), boneCharges).withStyle(ChatFormatting.GRAY));
        line.append(Component.literal("  ")).append(Component.translatable(key + "power",
                String.format(Locale.ROOT, "%,d", energy.getAmountAsInt()), String.format(Locale.ROOT, "%,d", tank.getAmount())).withStyle(ChatFormatting.GRAY));
        return line;
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
    public boolean isEmpty() {
        return boneMeal.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? boneMeal : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || boneMeal.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = boneMeal.split(count);
        setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack taken = boneMeal;
        boneMeal = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        boneMeal = stack;
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(Items.BONE_MEAL);
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        boneMeal = ItemStack.EMPTY;
        setChanged();
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

    private final class SeederEnergy extends SimpleEnergyHandler {
        SeederEnergy() {
            super(20_000, 200, 0);
        }

        void refreshLimits() {
            capacity = ModConfigs.getInt(ModConfigs.SERVER.seederCapacity);
            maxInsert = ModConfigs.getInt(ModConfigs.SERVER.seederMaxInput);
            if (energy > capacity) set(capacity);
        }

        void consume(int amount) {
            set(Math.max(0, energy - amount));
        }

        /** Fills it at once (GameTests, the screenshot harness). */
        void fill() {
            set(capacity);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    }

    /** Fills its power at once (GameTests, the screenshot harness). */
    public void fillEnergy() {
        energy.refreshLimits();
        energy.fill();
    }

    /** Water only, in only: buckets, pipes and a Fluid Reservoir fill it. */
    public final class SeederTank extends FluidStacksResourceHandler {
        SeederTank() {
            super(1, 4_000);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.equals(WATER);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            setChanged();
        }

        public int getAmount() {
            return getAmountAsInt(0);
        }

        public int capacity() {
            return capacity;
        }

        public void fill(int amount) {
            int added = Math.max(0, Math.min(amount, capacity - getAmount()));
            if (added > 0) set(0, WATER, getAmount() + added);
        }

        void drain(int amount) {
            int left = Math.max(0, getAmount() - amount);
            set(0, left > 0 ? WATER : FluidResource.EMPTY, left);
        }

        void resize(int newCapacity) {
            if (newCapacity == capacity) return;
            capacity = newCapacity;
            if (getAmount() > capacity) set(0, WATER, capacity);
        }
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
