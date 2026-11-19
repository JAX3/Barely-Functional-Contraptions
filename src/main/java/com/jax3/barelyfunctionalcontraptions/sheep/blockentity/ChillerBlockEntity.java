package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ChillerBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The Snow Sheep Chiller: freezes water into Ice with the cold of the Snow sheep around it, the Blaze Sheep Furnace's
 * icy opposite. It needs no power.
 *
 * <ul>
 *     <li><b>Cold:</b> every half second it counts the grown Snow sheep (any resource in {@code [chiller] coldSources})
 *     with their fleece on and their feet in {@link #chillingArea}, the furnace's 5x5 area. A sheared sheep or a lamb
 *     gives nothing until its fleece grows back.</li>
 *     <li><b>Speed:</b> one Snow sheep freezes a block of Ice every {@code ticksPerIce} (10 s); each one after the first
 *     adds a little less, like the furnace's heat, up to {@code maxColdSources} (4 sheep: about every 3 s).</li>
 *     <li><b>Water:</b> each Ice takes {@code waterPerIce} mB from its tank, filled by buckets, pipes or a Fluid
 *     Reservoir beside it. The Ice goes in its output slot, which hoppers and pipes can empty from any side.</li>
 * </ul>
 * Its comparator signal reads how full the output slot is.
 */
public class ChillerBlockEntity extends BlockEntity implements WorldlyContainer, AreaDisplay, net.minecraft.world.MenuProvider {
    public static final int SCAN_INTERVAL = 10;
    public static final int AREA_COLOR = 0xFF7FD8FF;
    private static final int[] SLOTS = {0};
    /** Values synced to the window ({@link com.jax3.barelyfunctionalcontraptions.sheep.menu.ChillerMenu}). */
    public static final int DATA_COUNT = 10;
    public static final FluidResource WATER = FluidResource.of(Fluids.WATER);

    private final ChillerTank tank = new ChillerTank();
    private ItemStack output = ItemStack.EMPTY;
    /** Progress towards the next Ice, in hundredths of a tick at one sheep's speed. */
    private int progress;
    /** Speed from the cold, in hundredths (100 = one Snow sheep), set on each scan. */
    private int cold;
    private int[] chillers = new int[0];
    private int scanCooldown;
    private boolean showArea;
    private MachineStatus status = MachineStatus.IDLE;
    private int lastSignal;

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> tank.getAmount();
                case 1 -> tank.capacity();
                case 2 -> Math.min(0xFFFF, progress / 100);
                case 3 -> Math.min(0xFFFF, ticksPerIce());
                case 4 -> cold;
                case 5 -> maxCold();
                case 6 -> chillers.length;
                case 7 -> maxSources();
                case 8 -> status.ordinal();
                case 9 -> showArea ? 1 : 0;
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

    public ChillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHILLER.get(), pos, state);
    }

    /** The chilling area: the same as the furnace's, (2 radius + 1) square columns at its level and the one above. */
    public static AABB chillingArea(BlockPos pos) {
        return BlazeFurnaceBlockEntity.heatingArea(pos, ModConfigs.getInt(ModConfigs.SERVER.chillerRadius));
    }

    public static int ticksPerIce() {
        return ModConfigs.getInt(ModConfigs.SERVER.chillerTicksPerIce);
    }

    public static int waterPerIce() {
        return ModConfigs.getInt(ModConfigs.SERVER.chillerWaterPerIce);
    }

    public static int maxSources() {
        return ModConfigs.getInt(ModConfigs.SERVER.chillerMaxSources);
    }

    /** Speed (hundredths) from {@code count} Snow sheep: the first full, each after it falloff less. */
    public static int coldFrom(int count) {
        return BlazeFurnaceBlockEntity.combine(Collections.nCopies(count, 100.0), maxSources(), ModConfigs.getDouble(ModConfigs.SERVER.chillerFalloff));
    }

    public static int maxCold() {
        return coldFrom(maxSources());
    }

    /** Whether {@code sheep} is a cold source right now: grown, fleece on, infused with a cold resource. */
    public static boolean isColdSource(Sheep sheep) {
        if (!sheep.isAlive() || sheep.isSheared() || sheep.isBaby()) return false;
        List<? extends String> sources = ModConfigs.SERVER.chillerColdSources.get();
        return SheepInfusionEvents.getInfusion(sheep).resource().map(Identifier::toString).map(sources::contains).orElse(false);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ChillerBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        tank.resize(ModConfigs.getInt(ModConfigs.SERVER.chillerTankCapacity));
        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            scan(level, pos, state);
        }
        boolean room = output.isEmpty() || (output.is(Items.ICE) && output.getCount() < output.getMaxStackSize());
        if (cold <= 0) {
            status = MachineStatus.IDLE;
        } else if (!room) {
            status = MachineStatus.OUTPUT_FULL;
        } else if (tank.getAmount() < waterPerIce()) {
            status = MachineStatus.NO_ENERGY; // shown as "No water"
        } else {
            status = MachineStatus.PROCESSING;
            progress += cold;
            if (progress >= ticksPerIce() * 100) {
                progress = 0;
                tank.drain(waterPerIce());
                if (output.isEmpty()) output = new ItemStack(Items.ICE);
                else output.grow(1);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GLASS_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.4F);
            }
            setChanged();
        }
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        boolean active = status == MachineStatus.PROCESSING;
        if (state.getValue(ChillerBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(ChillerBlock.ACTIVE, active), Block.UPDATE_ALL);
    }

    /** Counts the Snow sheep in the area. */
    public void scan(ServerLevel level, BlockPos pos, BlockState state) {
        AABB area = chillingArea(pos);
        List<Sheep> found = level.getEntitiesOfClass(Sheep.class, area, s -> area.contains(s.position()) && isColdSource(s));
        int[] ids = found.stream().limit(maxSources()).mapToInt(Sheep::getId).sorted().toArray();
        // Keep its Snow sheep close: call strays back while it has room for more.
        SheepAttraction.claim(found, level.getGameTime());
        SheepAttraction.attract(level, area, ChillerBlockEntity::isColdSource, maxSources() - found.size(), level.getGameTime());
        cold = coldFrom(ids.length);
        if (!Arrays.equals(ids, chillers)) {
            chillers = ids;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ accessors

    public ChillerTank getTank() {
        return tank;
    }

    public int coldSourceCount() {
        return chillers.length;
    }

    /** Ids of the Snow sheep chilling it (client side too, for the snowflakes). */
    public int[] chillers() {
        return chillers;
    }

    public int cold() {
        return cold;
    }

    public MachineStatus status() {
        return status;
    }

    public ItemStack output() {
        return output;
    }

    public int comparatorSignal() {
        return output.isEmpty() ? 0 : 1 + output.getCount() * 14 / output.getMaxStackSize();
    }

    /** Pours a bucket's worth of water in, if it fits. */
    public boolean fillFromBucket() {
        if (tank.capacity() - tank.getAmount() < 1000) return false;
        tank.fill(1000);
        return true;
    }

    /** It calls its sheep back from just outside its area. */
    @Override
    public boolean attractsSheep() {
        return true;
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
        return chillingArea(worldPosition);
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.chiller");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.ChillerMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ container (the Ice output)

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? output : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || output.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = output.split(count);
        setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack taken = output;
        output = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        output = stack;
        setChanged();
    }

    /** Nothing goes in: the slot is the Ice it makes. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        output = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    /** Hoppers and pipes take the Ice out of any side. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, this);
    }

    // ------------------------------------------------------------------ water

    /** Water only, in only: buckets, pipes and a Fluid Reservoir fill it; the water can't be taken back out. */
    public final class ChillerTank extends FluidStacksResourceHandler {
        ChillerTank() {
            super(1, 8_000);
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

        void fill(int amount) {
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
        output = input.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        progress = input.getIntOr("progress", 0);
        showArea = input.getBooleanOr("show_area", false);
        chillers = input.getIntArray("chillers").orElse(new int[0]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        if (!this.output.isEmpty()) output.store("output", ItemStack.OPTIONAL_CODEC, this.output);
        output.putInt("progress", progress);
        output.putBoolean("show_area", showArea);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients get the area setting and the Snow sheep (for the snowflakes). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("show_area", showArea);
        tag.putIntArray("chillers", chillers);
        return tag;
    }
}
