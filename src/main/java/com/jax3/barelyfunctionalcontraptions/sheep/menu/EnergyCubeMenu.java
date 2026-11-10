package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CubeStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCubeBlockEntity;
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

/** Menu of the Hume Energy Cube: no machine slots, just the group's synced numbers and the player inventory. */
public class EnergyCubeMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 111;
    private static final int PLAYER_END = 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client-side constructor, called with the position written by {@code openMenu(provider, pos)}. */
    public EnergyCubeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainerData(EnergyCubeBlockEntity.DATA_COUNT));
    }

    public EnergyCubeMenu(int containerId, Inventory playerInventory, BlockPos pos, ContainerData data) {
        super(ModMenus.HUME_ENERGY_CUBE.get(), containerId);
        checkContainerDataCount(data, EnergyCubeBlockEntity.DATA_COUNT);
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
        return stillValid(access, player, ModBlocks.HUME_ENERGY_CUBE.get());
    }

    private long long64(int first) {
        long value = 0;
        for (int i = 0; i < 4; i++) value |= (long) (data.get(first + i) & 0xFFFF) << (16 * i);
        return value;
    }

    private int int32(int low) {
        return (data.get(low) & 0xFFFF) | ((data.get(low + 1) & 0xFFFF) << 16);
    }

    public long getAmount() { return long64(0); }
    public long getCapacity() { return long64(4); }
    public int getRate() { return int32(8); }
    public int getCubes() { return Math.max(1, data.get(10)); }
    public int getInRate() { return int32(11); }
    public int getOutRate() { return int32(13); }
    public CubeStatus getStatus() { return CubeStatus.byId(data.get(15)); }

    public float getFillFraction() {
        long capacity = getCapacity();
        return capacity <= 0 ? 0 : (float) Math.min(1.0, getAmount() / (double) capacity);
    }
}
