package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * One Fluid Tank. It holds its own share of fluid ({@code capacityPerTank}); the fluid capability on every face is a
 * view of the whole {@link FluidTankGroup} it is connected to, so fluid goes in and out on any side of any tank and the
 * group's capacity and transfer rate grow with every tank added. Its share is synced to clients, where
 * {@code FluidTankRenderer} draws it behind the glass.
 */
public class FluidTankBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    /** Ticks between sending changed fluid levels to clients. */
    private static final int SYNC_INTERVAL = 4;

    private final TankStorage storage = new TankStorage();
    private @Nullable FluidTankGroup group;
    private final ResourceHandler<FluidResource> view = new GroupView();
    private int ticks;
    private boolean settling;
    private boolean needsSync;
    /** mB that went into / out of this tank since the last rate update (committed changes only, not settling). */
    private long inWindow, outWindow;
    /** Leader only: the group's average mB/t in and out over the last second. */
    private int inRate, outRate;
    /** Synced GUI values: amount (2 x 16 bit), capacity (2), rate (2), size, in (2), out (2), status, fluid id. */
    public static final int DATA_COUNT = 13;

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            FluidTankGroup g = group();
            FluidTankBlockEntity leader = g.leader();
            int amount = (int) Math.min(Integer.MAX_VALUE, g.amount()), capacity = (int) Math.min(Integer.MAX_VALUE, g.capacity());
            return switch (index) {
                case 0 -> amount & 0xFFFF;
                case 1 -> amount >>> 16;
                case 2 -> capacity & 0xFFFF;
                case 3 -> capacity >>> 16;
                case 4 -> g.rate() & 0xFFFF;
                case 5 -> g.rate() >>> 16;
                case 6 -> g.size();
                case 7 -> leader.inRate & 0xFFFF;
                case 8 -> leader.inRate >>> 16;
                case 9 -> leader.outRate & 0xFFFF;
                case 10 -> leader.outRate >>> 16;
                case 11 -> CubeStatus.of(amount, capacity, leader.inRate, leader.outRate).ordinal();
                case 12 -> g.fluid().isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(g.fluid().getFluid());
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

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state);
    }

    /** This tank's own share of the fluid. */
    public TankStorage storage() {
        return storage;
    }

    /** The group this tank belongs to, rebuilt from the world if it changed. */
    public FluidTankGroup group() {
        if (!hasValidGroup()) FluidTankGroup.build(level, this);
        return group;
    }

    boolean hasValidGroup() {
        return group != null && group.isValid();
    }

    void setGroup(FluidTankGroup group) {
        this.group = group;
        group.markChanged();
    }

    /** Forgets the current group (all members rebuild it next time it is used). */
    public void invalidateGroup() {
        if (group != null) group.invalidate();
    }

    /** The capability handed to pipes, buckets and machines on every face. */
    public ResourceHandler<FluidResource> getFluidHandler() {
        return view;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FluidTankBlockEntity be) {
        be.storage.refreshLimits();
        FluidTankGroup group = be.group();
        if (group.leader() == be) {
            group.settle();
            if (ModConfigs.getBool(ModConfigs.SERVER.tankAutoOutput)) group.push(level);
            if (++be.ticks % 20 == 0) {
                long in = 0, out = 0;
                for (FluidTankBlockEntity tank : group.members()) {
                    in += tank.inWindow;
                    out += tank.outWindow;
                    tank.inWindow = 0;
                    tank.outWindow = 0;
                }
                be.inRate = (int) Math.min(Integer.MAX_VALUE, in / 20);
                be.outRate = (int) Math.min(Integer.MAX_VALUE, out / 20);
            }
        }
        if (be.needsSync && Math.floorMod(level.getGameTime() + pos.hashCode(), SYNC_INTERVAL) == 0) {
            be.needsSync = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** Sets this tank's share directly when the group settles (not counted as fluid flowing in or out). */
    void settleTo(FluidResource fluid, int amount) {
        if (storage.getAmountAsInt(0) == amount && (amount == 0 || storage.getResource(0).equals(fluid))) return;
        settling = true;
        try {
            storage.set(0, fluid, amount);
        } finally {
            settling = false;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invalidateGroup();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        invalidateGroup();
    }

    /** The group's average mB/t in and out over the last second (read from the leader). */
    public int inRate() {
        return group().leader().inRate;
    }

    public int outRate() {
        return group().leader().outRate;
    }

    /** This tank's fluid as a stack (its own share), for rendering, tooltips and tests. */
    public FluidStack getFluid() {
        FluidResource resource = storage.getResource(0);
        return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack(storage.getAmountAsInt(0));
    }

    /** How full this tank is, 0..1. */
    public float fillFraction() {
        int capacity = storage.capacity();
        return capacity <= 0 ? 0 : Math.min(1, storage.getAmountAsInt(0) / (float) capacity);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.fluid_tank");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
                                                                         net.minecraft.world.entity.player.Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.FluidTankMenu(containerId, inventory, getBlockPos(), data);
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        settling = true; // loading is not fluid flowing in
        try {
            FluidStack stack = input.read("fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
            int amount = Math.min(stack.getAmount(), storage.capacity());
            storage.set(0, amount > 0 ? FluidResource.of(stack) : FluidResource.EMPTY, amount);
        } finally {
            settling = false;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("fluid", FluidStack.OPTIONAL_CODEC, getFluid());
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** A broken tank keeps its own share of the fluid in the item. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        FluidStack stack = getFluid();
        if (!stack.isEmpty()) components.set(ModDataComponents.FLUID_CONTENTS.get(), SimpleFluidContent.copyOf(stack));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        SimpleFluidContent contents = components.get(ModDataComponents.FLUID_CONTENTS.get());
        if (contents != null && !contents.isEmpty()) {
            FluidStack stack = contents.copy();
            settling = true;
            try {
                storage.set(0, FluidResource.of(stack), Math.min(stack.getAmount(), storage.capacity()));
            } finally {
                settling = false;
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("fluid");
    }

    /** One tank's share; the group applies the transfer rate, so the tank itself has no limit. */
    public final class TankStorage extends FluidStacksResourceHandler {
        TankStorage() {
            super(1, ModConfigs.getInt(ModConfigs.SERVER.tankCapacityPerTank));
        }

        void refreshLimits() {
            int configured = ModConfigs.getInt(ModConfigs.SERVER.tankCapacityPerTank);
            if (configured == capacity) return;
            capacity = configured;
            if (getAmountAsInt(0) > capacity) set(0, getResource(0), capacity);
        }

        public int capacity() {
            return capacity;
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            int delta = getAmountAsInt(0) - previousContents.getAmount();
            if (!settling) {
                if (delta > 0) inWindow += delta;
                else outWindow -= delta;
                if (group != null) group.markChanged();
            }
            needsSync = true;
            setChanged();
        }
    }

    /** The whole group, seen through this tank: one slot with the group's fluid. Resolves the group on every call. */
    private final class GroupView implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            return group().fluid();
        }

        @Override
        public long getAmountAsLong(int index) {
            return group().amount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return group().capacity();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            FluidResource current = group().fluid();
            return current.isEmpty() || current.equals(resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return group().insert(resource, amount, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return group().extract(resource, amount, transaction);
        }
    }
}
