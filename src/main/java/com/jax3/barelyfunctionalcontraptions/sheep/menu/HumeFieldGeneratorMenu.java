package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity.Status;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Window of the Hume Field Generator: no slots, just its synced numbers (what it made, delivered and lost this tick, its
 * average, its range, how many blocks take its power, its status, and the trace of its last ticks).
 */
public class HumeFieldGeneratorMenu extends MachineMenu {
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public HumeFieldGeneratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainerData(HumeFieldGeneratorBlockEntity.DATA_COUNT));
    }

    public HumeFieldGeneratorMenu(int containerId, Inventory playerInventory, HumeFieldGeneratorBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), data);
    }

    private HumeFieldGeneratorMenu(int containerId, Inventory playerInventory, BlockPos pos, ContainerData data) {
        super(ModMenus.HUME_FIELD_GENERATOR.get(), containerId);
        checkContainerDataCount(data, HumeFieldGeneratorBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.HUME_FIELD_GENERATOR.get());
    }

    /** FE/t its field made on the last tick. */
    public int getGenerated() { return int32(data, 0); }
    /** FE/t its neighbours took on the last tick. */
    public int getDelivered() { return int32(data, 2); }
    /** FE/t lost on the last tick. */
    public int getLost() { return Math.max(0, getGenerated() - getDelivered()); }
    /** FE/t it made on average over the last second. */
    public int getAverage() { return int32(data, 4); }
    public int getRangeMin() { return int32(data, 6); }
    public int getRangeMax() { return int32(data, 8); }
    public Status getStatus() { return Status.byId(data.get(10)); }
    /** How many blocks touching it take power. */
    public int getReceivers() { return data.get(11); }
    /** Ticks until its field next pulses (0 if it never does). */
    public int getUntilPulse() { return data.get(12); }

    /** Tick {@code i} of the trace (0 the oldest): what it made as a share of the range's top, 0-1. */
    public float sampleHeight(int i) {
        return ((data.get(HumeFieldGeneratorBlockEntity.TRACE_DATA + i) & 0xFFFF) >>> 8) / 255F;
    }

    /** Tick {@code i} of the trace: the share of what it made that was taken, 0-1. */
    public float sampleTaken(int i) {
        return (data.get(HumeFieldGeneratorBlockEntity.TRACE_DATA + i) & 0xFF) / 255F;
    }
}
