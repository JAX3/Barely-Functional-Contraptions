package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CubeStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidTankBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Menu of the Fluid Tank: no machine slots, just the group's synced numbers and the player inventory. */
public class FluidTankMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 111;
    private static final int PLAYER_END = 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client-side constructor, called with the position written by {@code openMenu(provider, pos)}. */
    public FluidTankMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainerData(FluidTankBlockEntity.DATA_COUNT));
    }

    public FluidTankMenu(int containerId, Inventory playerInventory, BlockPos pos, ContainerData data) {
        super(ModMenus.FLUID_TANK.get(), containerId);
        checkContainerDataCount(data, FluidTankBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Shift-click moves items between the inventory and the hotbar. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < 27 ? moveItemStackTo(stack, 27, PLAYER_END, false) : moveItemStackTo(stack, 0, 27, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.FLUID_TANK.get());
    }

    private int int32(int low) {
        return (data.get(low) & 0xFFFF) | ((data.get(low + 1) & 0xFFFF) << 16);
    }

    public int getAmount() { return int32(0); }
    public int getCapacity() { return int32(2); }
    public int getRate() { return int32(4); }
    public int getTanks() { return Math.max(1, data.get(6)); }
    public int getInRate() { return int32(7); }
    public int getOutRate() { return int32(9); }
    public CubeStatus getStatus() { return CubeStatus.byId(data.get(11)); }

    /** The group's fluid, or {@link Fluids#EMPTY}. */
    public Fluid getFluid() {
        int id = data.get(12);
        return id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(id);
    }

    public float getFillFraction() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : Math.min(1, getAmount() / (float) capacity);
    }
}
