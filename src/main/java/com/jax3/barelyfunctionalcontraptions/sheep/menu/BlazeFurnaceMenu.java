package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.BlazeFurnaceBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceBoostDataMap;
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
import org.jspecify.annotations.Nullable;

/**
 * Window of the Blaze Sheep Furnace: its boost slot, the player inventory and the furnace's synced numbers (energy,
 * heat, heat sources, boost, area setting). Slot positions match {@code textures/gui/blaze_furnace.png}.
 */
public class BlazeFurnaceMenu extends AbstractContainerMenu {
    public static final int BOOST_X = 152, BOOST_Y = 87;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_AREA = 0;

    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable BlazeFurnaceBlockEntity blockEntity;

    /** Client-side constructor, called with the position written by {@code openMenu(provider, pos)}. */
    public BlazeFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(1), new SimpleContainerData(BlazeFurnaceBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public BlazeFurnaceMenu(int containerId, Inventory playerInventory, BlazeFurnaceBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be, data);
    }

    private BlazeFurnaceMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable BlazeFurnaceBlockEntity be,
                             Container boost, ContainerData data) {
        super(ModMenus.BLAZE_FURNACE.get(), containerId);
        checkContainerSize(boost, 1);
        checkContainerDataCount(data, BlazeFurnaceBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.blockEntity = be;
        addSlot(new BoostSlot(boost, BOOST_X, BOOST_Y));
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Takes only boost items (Blaze Rods by default); anything can be taken back out. */
    private static final class BoostSlot extends Slot {
        BoostSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return FurnaceBoostDataMap.of(stack) != null;
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
        } else if (FurnaceBoostDataMap.of(stack) != null && moveItemStackTo(stack, 0, 1, false)) {
            // Boost items go straight into the furnace.
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
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_AREA && blockEntity != null) {
            blockEntity.toggleArea();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BLAZE_FURNACE.get());
    }

    // ------------------------------------------------------------------ synced values (the client reads these)

    private int int32(int low) {
        return (data.get(low) & 0xFFFF) | ((data.get(low + 1) & 0xFFFF) << 16);
    }

    public int getEnergy() { return int32(0); }
    public int getCapacity() { return int32(2); }
    /** FE/t made, boost included. */
    public int getGenerating() { return int32(4); }
    /** FE/t from heat alone. */
    public int getHeat() { return int32(6); }
    /** The top of the heat column: the most heat the hottest heat sources can give. */
    public int getMaxHeat() { return int32(8); }
    public int getHeaters() { return data.get(10); }
    public int getMaxHeaters() { return Math.max(1, data.get(11)); }
    public int getBoostTicks() { return data.get(12) & 0xFFFF; }
    public int getBoostDuration() { return data.get(13) & 0xFFFF; }
    public double getBoostMultiplier() { return data.get(14) / 100.0; }
    public boolean isShowingArea() { return data.get(15) != 0; }
    /** FE/t one of the hottest heat sources gives. */
    public int getBestPower() { return int32(16); }

    public float getEnergyFraction() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : Math.min(1F, getEnergy() / (float) capacity);
    }

    public float getHeatFraction() {
        int max = getMaxHeat();
        return max <= 0 ? 0 : Math.min(1F, getHeat() / (float) max);
    }

    public boolean isBufferFull() {
        return getCapacity() > 0 && getEnergy() >= getCapacity();
    }
}
