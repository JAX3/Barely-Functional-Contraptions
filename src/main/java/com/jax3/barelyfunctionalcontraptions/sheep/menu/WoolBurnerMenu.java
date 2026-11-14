package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolBurnerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.burner.WoolBurnerFuels;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Window of the Wool Burner: its fuel slot, the player inventory and the burner's synced numbers. */
public class WoolBurnerMenu extends AbstractContainerMenu {
    public static final int FUEL_X = 59, FUEL_Y = 56;
    public static final int INVENTORY_Y = 111;

    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client-side constructor, called with the position written by {@code openMenu(provider, pos)}. */
    public WoolBurnerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainer(1), new SimpleContainerData(WoolBurnerBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public WoolBurnerMenu(int containerId, Inventory playerInventory, WoolBurnerBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, data);
    }

    private WoolBurnerMenu(int containerId, Inventory playerInventory, BlockPos pos, Container fuel, ContainerData data) {
        super(ModMenus.WOOL_BURNER.get(), containerId);
        checkContainerSize(fuel, 1);
        checkContainerDataCount(data, WoolBurnerBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        addSlot(new FuelSlot(fuel, FUEL_X, FUEL_Y));
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Takes fuel only; anything can be taken back out by hand. */
    private static final class FuelSlot extends Slot {
        FuelSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return WoolBurnerFuels.isFuel(stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (WoolBurnerFuels.isFuel(stack) && moveItemStackTo(stack, 0, 1, false)) {
            // Fuel goes straight into the burner.
        } else if (index < PLAYER_START + 27) {
            if (!moveItemStackTo(stack, PLAYER_START + 27, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_START + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WOOL_BURNER.get());
    }

    private int int32(int low) {
        return (data.get(low) & 0xFFFF) | ((data.get(low + 1) & 0xFFFF) << 16);
    }

    public int getEnergy() { return int32(0); }
    public int getCapacity() { return int32(2); }
    public int getBurnTime() { return data.get(4) & 0xFFFF; }
    public int getBurnTotal() { return data.get(5) & 0xFFFF; }
    /** FE/t of the item burning now. */
    public int getPower() { return int32(6); }
    public boolean isGenerating() { return data.get(8) != 0; }

    public float getEnergyFraction() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : Math.min(1F, getEnergy() / (float) capacity);
    }

    /** How much of the item burning now is left, 1 when just lit, 0 when out. */
    public float getBurnFraction() {
        int total = getBurnTotal();
        return total <= 0 ? 0 : Math.min(1F, getBurnTime() / (float) total);
    }

    public boolean isBufferFull() {
        return getCapacity() > 0 && getEnergy() >= getCapacity();
    }
}
