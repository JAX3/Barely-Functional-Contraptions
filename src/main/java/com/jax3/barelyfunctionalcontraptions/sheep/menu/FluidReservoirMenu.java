package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FluidReservoirBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ReservoirStatus;
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
import net.minecraft.world.item.Items;

/** Container menu of the Fluid Reservoir. Slot coordinates match {@code textures/gui/fluid_reservoir.png}. */
public class FluidReservoirMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 134, INPUT_Y = 22;
    public static final int OUTPUT_X = 134, OUTPUT_Y = 56;
    public static final int INVENTORY_Y = 111;

    private static final int MACHINE_SLOTS = FluidReservoirBlockEntity.SIZE;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Client-side constructor, called with the extra data written by {@code openMenu(provider, pos)}. */
    public FluidReservoirMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainer(MACHINE_SLOTS),
                new SimpleContainerData(FluidReservoirBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public FluidReservoirMenu(int containerId, Inventory playerInventory, FluidReservoirBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, data);
    }

    private FluidReservoirMenu(int containerId, Inventory playerInventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.FLUID_RESERVOIR.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, FluidReservoirBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        addSlot(new Slot(container, FluidReservoirBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BUCKET);
            }
        });
        addSlot(new Slot(container, FluidReservoirBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
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
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (stack.is(Items.BUCKET)) {
            if (!moveItemStackTo(stack, FluidReservoirBlockEntity.SLOT_INPUT, FluidReservoirBlockEntity.SLOT_INPUT + 1, false)) return ItemStack.EMPTY;
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
        return stillValid(access, player, ModBlocks.FLUID_RESERVOIR.get());
    }

    private int combine(int low, int high) {
        return (data.get(low) & 0xFFFF) | ((data.get(high) & 0xFFFF) << 16);
    }

    public int getAmount() { return combine(0, 1); }
    public int getCapacity() { return combine(2, 3); }
    public ReservoirStatus getStatus() { return ReservoirStatus.byId(data.get(4)); }
    public int getWaterPerSecond() { return combine(5, 6); }

    public float getFillFraction() {
        int cap = getCapacity();
        return cap <= 0 ? 0 : Math.min(1.0F, getAmount() / (float) cap);
    }
}
