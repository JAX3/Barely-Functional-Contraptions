package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.FleeceTrawlerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
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
 * Window of the Fleece Trawler: its lure slot (resource wool only), its nine catch slots (take only), the player's
 * inventory, and its synced numbers (status, Tide sheep, pool, haul, lure hauls left, the pool outline, hauls).
 */
public class FleeceTrawlerMenu extends AbstractContainerMenu {
    public static final int LURE_X = 26, LURE_Y = 30;
    public static final int CATCH_X = 98, CATCH_Y = 20;
    public static final int INVENTORY_Y = 114;
    public static final int BUTTON_AREA = 0;

    private static final int PLAYER_START = FleeceTrawlerBlockEntity.SIZE;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable FleeceTrawlerBlockEntity blockEntity;

    public FleeceTrawlerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(FleeceTrawlerBlockEntity.SIZE),
                new SimpleContainerData(FleeceTrawlerBlockEntity.DATA_COUNT));
    }

    public FleeceTrawlerMenu(int containerId, Inventory playerInventory, FleeceTrawlerBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be, data);
    }

    private FleeceTrawlerMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable FleeceTrawlerBlockEntity be, Container container, ContainerData data) {
        super(ModMenus.FLEECE_TRAWLER.get(), containerId);
        checkContainerSize(container, FleeceTrawlerBlockEntity.SIZE);
        checkContainerDataCount(data, FleeceTrawlerBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.blockEntity = be;
        addSlot(new Slot(container, FleeceTrawlerBlockEntity.LURE, LURE_X, LURE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ResourceWoolItem.getResourceId(stack) != null;
            }
        });
        for (int i = 0; i < FleeceTrawlerBlockEntity.OUTPUTS; i++) {
            addSlot(new Slot(container, 1 + i, CATCH_X + (i % 3) * 18, CATCH_Y + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Shift-click: the catch and the lure to the inventory; resource wool from the inventory to the lure slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (ResourceWoolItem.getResourceId(stack) != null && moveItemStackTo(stack, 0, 1, false)) {
            // into the lure slot
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
            blockEntity.toggleArea(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.FLEECE_TRAWLER.get());
    }

    public FleeceTrawlerBlockEntity.Status getStatus() {
        FleeceTrawlerBlockEntity.Status[] all = FleeceTrawlerBlockEntity.Status.values();
        return all[Math.floorMod(data.get(0), all.length)];
    }

    public int getSheep() { return data.get(1); }
    public int getMaxSheep() { return Math.max(1, data.get(2)); }
    public int getPool() { return data.get(3); }
    public int getProgress() { return data.get(4); }
    public int getLureUsesLeft() { return data.get(5); }
    public int getLureUses() { return Math.max(1, data.get(6)); }
    public boolean isShowingArea() { return data.get(7) != 0; }
    public int getHauls() { return data.get(8); }
}
