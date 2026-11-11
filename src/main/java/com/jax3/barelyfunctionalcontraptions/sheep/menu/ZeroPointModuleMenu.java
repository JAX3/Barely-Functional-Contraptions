package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointEnergyModuleBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ZeroPointLink;
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

/** Menu of the Zero Point Energy Module: no machine slots, just the module's synced numbers and the player inventory. */
public class ZeroPointModuleMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 111;
    private static final int PLAYER_END = 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client-side constructor, called with the position written by {@code openMenu(provider, pos)}. */
    public ZeroPointModuleMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainerData(ZeroPointEnergyModuleBlockEntity.DATA_COUNT));
    }

    public ZeroPointModuleMenu(int containerId, Inventory playerInventory, BlockPos pos, ContainerData data) {
        super(ModMenus.ZERO_POINT_ENERGY_MODULE.get(), containerId);
        checkContainerDataCount(data, ZeroPointEnergyModuleBlockEntity.DATA_COUNT);
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
        return stillValid(access, player, ModBlocks.ZERO_POINT_ENERGY_MODULE.get());
    }

    private int int32(int low) {
        return (data.get(low) & 0xFFFF) | ((data.get(low + 1) & 0xFFFF) << 16);
    }

    public int getAmount() { return int32(0); }
    public int getCapacity() { return int32(2); }
    /** This module's share of the pair's FE/t. */
    public int getGenerating() { return int32(4); }
    public int getPairOutput() { return int32(6); }
    public int getLinkDistance() { return data.get(8); }
    public int getOutRate() { return int32(9); }
    /** True for a floor module (emitter up), false for a ceiling module. */
    public boolean facesUp() { return data.get(11) == 1; }
    public ZeroPointLink getLink() { return ZeroPointLink.byId(data.get(12)); }
    /** How far along the column the last scan stopped. */
    public int getStoppedAt() { return data.get(13); }
    public int getMaxRange() { return data.get(14); }

    public float getFillFraction() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : Math.min(1F, getAmount() / (float) capacity);
    }

    /** Linked, but the buffer is full because nothing is taking the energy. */
    public boolean isBufferFull() {
        return getLink() == ZeroPointLink.LINKED && getCapacity() > 0 && getAmount() >= getCapacity();
    }
}
