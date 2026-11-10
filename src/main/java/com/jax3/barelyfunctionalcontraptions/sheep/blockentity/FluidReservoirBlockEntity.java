package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.FluidReservoirBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.FluidReservoirMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * The Fluid Reservoir: placed directly on top of an infinite water source, it makes water into its 10-bucket tank.
 *
 * <ul>
 *     <li>"Infinite" means the source below would refill itself if taken, as in vanilla: at least two horizontally
 *     neighbouring water sources and something solid (or more water) underneath. {@code requireInfiniteSource = false}
 *     accepts any water source.</li>
 *     <li>Water comes out through the fluid capability on every side (pipes), is pushed into neighbouring tanks on the
 *     sides and top (a Mycelial Generator next to it never runs dry), fills buckets and other containers on right-click,
 *     and fills empty buckets put in its GUI's input slot.</li>
 *     <li>Nothing can be poured in: it is a water source, not a tank.</li>
 * </ul>
 */
public class FluidReservoirBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0, SLOT_OUTPUT = 1, SIZE = 2;
    public static final int DATA_COUNT = 7;
    public static final int BUCKET = 1000;
    private static final int[] ALL_SLOTS = {SLOT_INPUT, SLOT_OUTPUT};
    /** How often (ticks) the source below is re-checked. */
    private static final int SOURCE_CHECK_INTERVAL = 10;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final ReservoirTank tank = new ReservoirTank(ModConfigs.getInt(ModConfigs.SERVER.reservoirCapacity), this::onTankChanged);
    private ReservoirStatus status = ReservoirStatus.NO_SOURCE;
    private ReservoirStatus sourceStatus = ReservoirStatus.NO_SOURCE;
    private int tickCounter;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int rate = ModConfigs.getInt(ModConfigs.SERVER.reservoirWaterPerSecond);
            return switch (index) {
                case 0 -> tank.getAmount() & 0xFFFF;
                case 1 -> tank.getAmount() >>> 16;
                case 2 -> tank.getCapacity() & 0xFFFF;
                case 3 -> tank.getCapacity() >>> 16;
                case 4 -> status.ordinal();
                case 5 -> rate & 0xFFFF;
                case 6 -> rate >>> 16;
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

    public FluidReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_RESERVOIR.get(), pos, state);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FluidReservoirBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        tank.setCapacity(ModConfigs.getInt(ModConfigs.SERVER.reservoirCapacity));
        if (tickCounter % SOURCE_CHECK_INTERVAL == 0) sourceStatus = checkSource(level, pos);
        if (sourceStatus == ReservoirStatus.FILLING) {
            // Spread the per-second rate evenly over the 20 ticks of a second, without losing the remainder.
            int rate = ModConfigs.getInt(ModConfigs.SERVER.reservoirWaterPerSecond);
            int step = tickCounter % 20;
            tank.generate((int) ((long) rate * (step + 1) / 20 - (long) rate * step / 20));
        }
        tickCounter = (tickCounter + 1) % 20_000;

        fillBucket();
        pushToNeighbours(level, pos);

        status = sourceStatus != ReservoirStatus.FILLING ? sourceStatus
                : tank.getAmount() >= tank.getCapacity() ? ReservoirStatus.FULL : ReservoirStatus.FILLING;
        int fill = fillLevel();
        if (state.getValue(FluidReservoirBlock.FILL) != fill) {
            level.setBlock(pos, state.setValue(FluidReservoirBlock.FILL, fill), Block.UPDATE_CLIENTS);
        }
    }

    /** Whether the block below is a water source the reservoir may draw from, and if not, why. */
    public static ReservoirStatus checkSource(Level level, BlockPos reservoirPos) {
        BlockPos source = reservoirPos.below();
        if (!isWaterSource(level.getFluidState(source))) return ReservoirStatus.NO_SOURCE;
        if (!ModConfigs.getBool(ModConfigs.SERVER.reservoirRequireInfinite)) return ReservoirStatus.FILLING;
        return isInfiniteSource(level, source) ? ReservoirStatus.FILLING : ReservoirStatus.NOT_INFINITE;
    }

    /**
     * Vanilla's rule for a water source that refills itself: two or more horizontally neighbouring water sources, and a
     * solid block or another water source underneath (plus the {@code waterSourceConversion} game rule, via NeoForge).
     */
    public static boolean isInfiniteSource(Level level, BlockPos source) {
        FluidState state = level.getFluidState(source);
        if (!isWaterSource(state)) return false;
        int neighbours = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (isWaterSource(level.getFluidState(source.relative(dir)))) neighbours++;
        }
        if (neighbours < 2) return false;
        BlockPos under = source.below();
        boolean supported = level.getBlockState(under).isSolid() || isWaterSource(level.getFluidState(under));
        if (!supported) return false;
        return !(level instanceof ServerLevel server) || state.canConvertToSource(server, source);
    }

    private static boolean isWaterSource(FluidState state) {
        return state.isSource() && state.is(FluidTags.WATER);
    }

    /** Turns one empty bucket from the input slot into a water bucket in the output slot. */
    private void fillBucket() {
        ItemStack input = items.get(SLOT_INPUT);
        if (!input.is(Items.BUCKET) || !items.get(SLOT_OUTPUT).isEmpty() || tank.getAmount() < BUCKET) return;
        tank.drain(BUCKET);
        input.shrink(1);
        items.set(SLOT_OUTPUT, new ItemStack(Items.WATER_BUCKET));
        setChanged();
    }

    private void pushToNeighbours(ServerLevel level, BlockPos pos) {
        int perTick = ModConfigs.getInt(ModConfigs.SERVER.reservoirOutputPerTick);
        if (perTick <= 0 || tank.getAmount() <= 0) return;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || tank.getAmount() <= 0) continue; // below is the water source
            ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target == null || target instanceof ReservoirTank) continue;
            ResourceHandlerUtil.move(tank, target, r -> true, perTick, null);
        }
    }

    private void onTankChanged() {
        setChanged();
    }

    /** 0 (empty) to 4 (full), for the water level in the block's side windows. */
    public int fillLevel() {
        int amount = tank.getAmount(), capacity = tank.getCapacity();
        if (amount <= 0) return 0;
        return Math.max(1, Math.min(4, (int) Math.round(amount * 4.0 / capacity)));
    }

    // ------------------------------------------------------------------ accessors

    public ReservoirTank getTank() {
        return tank;
    }

    /** The fluid capability: water can be taken out on every side; nothing goes in. */
    public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction side) {
        return tank;
    }

    public ReservoirStatus getStatus() {
        return status;
    }

    // ------------------------------------------------------------------ container

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    /** Only empty buckets, and only into the input slot. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && stack.is(Items.BUCKET);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** Hoppers and pipes only take the filled buckets. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.barelyfunctionalcontraptions.fluid_reservoir");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FluidReservoirMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        tank.serialize(output.child("tank"));
    }
}
