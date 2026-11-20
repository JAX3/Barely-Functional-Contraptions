package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Window of the Herd Counter: a ghost filter slot (clicking it with a resource wool or spawn egg sets the filter from a
 * copy, without taking the item; clicking it empty-handed clears it), the player inventory, and buttons for its mode,
 * its Full target and its area. What it counted comes from the block entity, which the server keeps synced. Slot
 * positions match {@code textures/gui/herd_counter.png}.
 */
public class HerdCounterMenu extends AbstractContainerMenu {
    public static final int FILTER_X = 77, FILTER_Y = 80;
    public static final int INVENTORY_Y = 111;
    /** Buttons: 0-5 pick a mode; then the area, and the target down/up by one and by five. */
    public static final int BUTTON_AREA = 6, BUTTON_TARGET_DOWN = 7, BUTTON_TARGET_UP = 8, BUTTON_TARGET_DOWN_5 = 9, BUTTON_TARGET_UP_5 = 10, BUTTON_STEADY = 11;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final @Nullable HerdCounterBlockEntity blockEntity;
    private final SimpleContainer filterShown = new SimpleContainer(1);

    public HerdCounterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null);
    }

    public HerdCounterMenu(int containerId, Inventory playerInventory, HerdCounterBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be);
    }

    private HerdCounterMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable HerdCounterBlockEntity be) {
        super(ModMenus.HERD_COUNTER.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.pos = pos;
        this.blockEntity = be;
        if (be != null) filterShown.setItem(0, be.filter().copy());
        addSlot(new Slot(filterShown, 0, FILTER_X, FILTER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
    }

    /** The filter slot is a ghost: it shows a copy of what it was clicked with, and never takes or gives items. */
    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId != 0) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        if (blockEntity == null) return; // the server decides; the slot is synced back
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            blockEntity.setFilter(ItemStack.EMPTY);
        } else if (HerdCounterBlockEntity.filterKey(carried) != null) {
            blockEntity.setFilter(carried);
        }
        filterShown.setItem(0, blockEntity.filter().copy());
        broadcastChanges();
    }

    /** Shift-clicking a filter item from the inventory sets the filter; nothing ever moves. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index > 0 && blockEntity != null) {
            ItemStack stack = slots.get(index).getItem();
            if (HerdCounterBlockEntity.filterKey(stack) != null) {
                blockEntity.setFilter(stack);
                filterShown.setItem(0, blockEntity.filter().copy());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) return false;
        if (id >= 0 && id < Mode.values().length) {
            blockEntity.setMode(Mode.byId(id));
        } else if (id == BUTTON_AREA) {
            blockEntity.toggleArea(player);
        } else if (id == BUTTON_STEADY) {
            blockEntity.setSteady(!blockEntity.steady());
        } else if (id >= BUTTON_TARGET_DOWN && id <= BUTTON_TARGET_UP_5) {
            int step = switch (id) {
                case BUTTON_TARGET_DOWN -> -1;
                case BUTTON_TARGET_UP -> 1;
                case BUTTON_TARGET_DOWN_5 -> -5;
                default -> 5;
            };
            blockEntity.setTarget(blockEntity.target() + step);
        } else {
            return false;
        }
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) blockEntity.count(level, pos, level.getBlockState(pos));
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.HERD_COUNTER.get());
    }

    public BlockPos pos() {
        return pos;
    }
}
