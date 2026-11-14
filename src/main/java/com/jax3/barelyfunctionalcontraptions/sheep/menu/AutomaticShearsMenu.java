package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AutomaticShearsBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RedstoneMode;
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

/** Container menu of the Automatic Shears. Slot coordinates match {@code textures/gui/automatic_shears.png}. */
public class AutomaticShearsMenu extends AbstractContainerMenu {
    /** Top-left item position of the 3x3 storage grid. */
    public static final int GRID_X = 96, GRID_Y = 20;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_REDSTONE = 0;
    public static final int BUTTON_AREA = 1;

    private static final int MACHINE_SLOTS = AutomaticShearsBlockEntity.SIZE;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable AutomaticShearsBlockEntity blockEntity;

    /** Client-side constructor, called with the extra data written by {@code openMenu(provider, pos)}. */
    public AutomaticShearsMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(MACHINE_SLOTS),
                new SimpleContainerData(AutomaticShearsBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public AutomaticShearsMenu(int containerId, Inventory playerInventory, AutomaticShearsBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be, data);
    }

    private AutomaticShearsMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable AutomaticShearsBlockEntity be,
                                Container storage, ContainerData data) {
        super(ModMenus.AUTOMATIC_SHEARS.get(), containerId);
        checkContainerSize(storage, MACHINE_SLOTS);
        checkContainerDataCount(data, AutomaticShearsBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.blockEntity = be;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new OutputSlot(storage, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** A storage slot you can only take from. */
    private static final class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            // Shift-click wool out into the player's inventory.
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (index < PLAYER_START + 27) {
            // Nothing can go into the machine: shuffle between inventory and hotbar instead.
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
        if (id == BUTTON_REDSTONE && blockEntity != null) {
            blockEntity.cycleRedstoneMode();
            return true;
        }
        if (id == BUTTON_AREA && blockEntity != null) {
            blockEntity.toggleArea();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.AUTOMATIC_SHEARS.get());
    }

    // ------------------------------------------------------------------ synced values (client reads these)

    private int combine(int low, int high) {
        return (data.get(low) & 0xFFFF) | ((data.get(high) & 0xFFFF) << 16);
    }

    public int getEnergy() { return combine(0, 1); }
    public int getCapacity() { return combine(2, 3); }
    public int getCooldown() { return data.get(4); }
    public int getInterval() { return Math.max(1, data.get(5)); }
    public MachineStatus getStatus() { return MachineStatus.byId(data.get(6)); }
    public RedstoneMode getRedstoneMode() { return RedstoneMode.byId(data.get(7)); }
    public boolean isPowered() { return data.get(8) != 0; }
    public int getEnergyPerShear() { return combine(9, 10); }
    public boolean isShowingArea() { return data.get(11) != 0; }

    public float getEnergyFraction() {
        int cap = getCapacity();
        return cap <= 0 ? 0 : Math.min(1.0F, getEnergy() / (float) cap);
    }

    /** How far along the wait for the next sweep is, 0..1. */
    public float getSweepFraction() {
        return 1.0F - Math.min(1.0F, getCooldown() / (float) getInterval());
    }
}
