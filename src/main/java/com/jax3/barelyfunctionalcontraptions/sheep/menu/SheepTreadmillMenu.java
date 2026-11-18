package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SheepTreadmillBlockEntity;
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

/** Window of the Sheep Treadmill: its food slot, the player inventory and the treadmill's synced numbers. */
public class SheepTreadmillMenu extends AbstractContainerMenu {
    public static final int FOOD_X = 59, FOOD_Y = 56;
    public static final int INVENTORY_Y = 111;

    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client-side constructor, called with the position written by {@code openMenu(provider, pos)}. */
    public SheepTreadmillMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainer(1), new SimpleContainerData(SheepTreadmillBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public SheepTreadmillMenu(int containerId, Inventory playerInventory, SheepTreadmillBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, data);
    }

    private SheepTreadmillMenu(int containerId, Inventory playerInventory, BlockPos pos, Container food, ContainerData data) {
        super(ModMenus.SHEEP_TREADMILL.get(), containerId);
        checkContainerSize(food, 1);
        checkContainerDataCount(data, SheepTreadmillBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        addSlot(new FoodSlot(food, FOOD_X, FOOD_Y));
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Takes Wheat and Hay Bales only; anything can be taken back out by hand. */
    private static final class FoodSlot extends Slot {
        FoodSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return SheepTreadmillBlockEntity.feedValue(stack) > 0;
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
        } else if (SheepTreadmillBlockEntity.feedValue(stack) > 0 && moveItemStackTo(stack, 0, 1, false)) {
            // Food goes straight onto the treadmill.
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
        return stillValid(access, player, ModBlocks.SHEEP_TREADMILL.get());
    }

    private int int32(int low) {
        return (data.get(low) & 0xFFFF) | ((data.get(low + 1) & 0xFFFF) << 16);
    }

    public int getEnergy() { return int32(0); }
    public int getCapacity() { return int32(2); }
    /** FE/t made on the last tick. */
    public int getGenerating() { return int32(4); }

    public SheepTreadmillBlockEntity.State getState() {
        var states = SheepTreadmillBlockEntity.State.values();
        return states[Math.clamp(data.get(6), 0, states.length - 1)];
    }

    /** The walking sheep's stamina, 0 (worn out) to 1 (fresh). */
    public float getStamina() { return Math.clamp(data.get(7) / 1000F, 0F, 1F); }
    /** Ticks of rest left for a worn-out sheep on the belt. */
    public int getRestLeft() { return data.get(8) & 0xFFFF; }

    public float getEnergyFraction() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : Math.min(1F, getEnergy() / (float) capacity);
    }
}
