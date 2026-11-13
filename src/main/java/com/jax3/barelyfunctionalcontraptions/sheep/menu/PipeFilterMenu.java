package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeSide;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ConduitBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The filter grid of one pipe side, opened by sneak-right-clicking the side with the Barely Functional Wrench.
 *
 * <p>The nine grid slots are "ghost" slots showing the side's filter (an item, or a fluid's bucket). Clicking a grid
 * slot while carrying an item puts that item (or the fluid in a bucket) in that position; clicking it with nothing
 * carried takes the entry out. Shift-clicking an item in your inventory adds it to the first free position. Nothing is
 * ever taken from you. Two buttons cycle the side's mode ({@link #BUTTON_MODE}) and clear the filter
 * ({@link #BUTTON_CLEAR}). All changes are made on the server and saved with the pipe.</p>
 */
public class PipeFilterMenu extends AbstractContainerMenu {
    public static final int GRID = PipeSide.MAX_FILTER;
    public static final int GRID_X = 62, GRID_Y = 18;
    public static final int INVENTORY_Y = 84;
    public static final int BUTTON_MODE = 0;
    public static final int BUTTON_CLEAR = 1;

    private final @Nullable PipeBlockEntity<?> pipe;
    private final BlockPos pos;
    private final Direction side;
    private final SimpleContainer grid = new SimpleContainer(GRID);
    private final DataSlot mode = DataSlot.standalone();

    /** Client side: the pipe's position and the side, written when the menu was opened. */
    public PipeFilterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, buf.readBlockPos(), buf.readEnum(Direction.class));
    }

    /** Server side. */
    public PipeFilterMenu(int containerId, Inventory inventory, PipeBlockEntity<?> pipe, Direction side) {
        this(containerId, inventory, pipe, pipe.getBlockPos(), side);
    }

    private PipeFilterMenu(int containerId, Inventory inventory, @Nullable PipeBlockEntity<?> pipe, BlockPos pos, Direction side) {
        super(ModMenus.PIPE_FILTER.get(), containerId);
        this.pipe = pipe;
        this.pos = pos;
        this.side = side;
        for (int i = 0; i < GRID; i++) {
            addSlot(new Slot(grid, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlot(mode);
        refresh();
    }

    public Direction side() {
        return side;
    }

    public PipeSide.Mode mode() {
        return PipeSide.Mode.values()[Math.floorMod(mode.get(), PipeSide.Mode.values().length)];
    }

    /** Shows the side's current settings (server side; the slots and mode then sync to the client). */
    private void refresh() {
        if (pipe == null) return;
        PipeSide current = pipe.side(side);
        for (int i = 0; i < GRID; i++) {
            grid.setItem(i, i < current.filter().size() ? pipe.kind().iconOf(current.filter().get(i)) : ItemStack.EMPTY);
        }
        mode.set(current.mode().ordinal());
    }

    private void apply(PipeSide updated) {
        if (pipe == null) return;
        PipeSide.Mode before = pipe.side(side).mode();
        pipe.setSide(side, updated);
        if (updated.mode() != before && pipe.getLevel() != null) ConduitBlock.refreshConnections(pipe.getLevel(), pos);
        refresh();
        broadcastChanges();
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < GRID) {
            if (pipe != null && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
                ItemStack carried = getCarried();
                PipeSide current = pipe.side(side);
                if (carried.isEmpty()) {
                    apply(current.withoutEntry(slotIndex));
                } else {
                    pipe.filterEntry(carried).ifPresent(id -> apply(current.withEntry(slotIndex, id)));
                }
            }
            return; // grid slots never hold real items
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }

    /** Shift-click in your inventory: adds that item to the filter; the item stays where it is. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (pipe != null && index >= GRID && index < slots.size()) {
            ItemStack stack = slots.get(index).getItem();
            if (!stack.isEmpty()) {
                PipeSide current = pipe.side(side);
                pipe.filterEntry(stack).ifPresent(id -> {
                    if (!current.filter().contains(id)) apply(current.withEntry(current.filter().size(), id));
                });
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (pipe == null) return false;
        PipeSide current = pipe.side(side);
        switch (id) {
            case BUTTON_MODE -> apply(current.withMode(current.mode().next()));
            case BUTTON_CLEAR -> apply(current.withoutFilter());
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return pipe != null && !pipe.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
