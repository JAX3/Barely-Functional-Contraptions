package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WoolPackerBlockEntity;
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

/** Window of the Wool Packer: its wool and bale slots, the player inventory and the packer's synced numbers. */
public class WoolPackerMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 62, INPUT_Y = 31;
    public static final int OUTPUT_X = 126, OUTPUT_Y = 31;
    public static final int INVENTORY_Y = 111;

    private static final int PLAYER_START = 2;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client-side constructor, called with the position written by {@code openMenu(provider, pos)}. */
    public WoolPackerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainer(2), new SimpleContainerData(WoolPackerBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public WoolPackerMenu(int containerId, Inventory playerInventory, WoolPackerBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, data);
    }

    private WoolPackerMenu(int containerId, Inventory playerInventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.WOOL_PACKER.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, WoolPackerBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        addSlot(new Slot(container, WoolPackerBlockEntity.INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WoolPackerBlockEntity.isPackable(stack);
            }
        });
        addSlot(new Slot(container, WoolPackerBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (WoolPackerBlockEntity.isPackable(stack) && moveItemStackTo(stack, 0, 1, false)) {
            // Wool goes straight into the packer.
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
        return stillValid(access, player, ModBlocks.WOOL_PACKER.get());
    }

    private int int32(int low) {
        return (data.get(low) & 0xFFFF) | ((data.get(low + 1) & 0xFFFF) << 16);
    }

    public int getEnergy() { return int32(0); }
    public int getCapacity() { return int32(2); }
    public int getProgress() { return data.get(4) & 0xFFFF; }
    public int getMaxProgress() { return data.get(5) & 0xFFFF; }
    public int getEnergyPerTick() { return int32(6); }
    public MachineStatus getStatus() { return MachineStatus.byId(data.get(8)); }

    public float getEnergyFraction() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : Math.min(1F, getEnergy() / (float) capacity);
    }

    public float getProgressFraction() {
        int max = getMaxProgress();
        return max <= 0 ? 0 : Math.min(1F, getProgress() / (float) max);
    }
}
