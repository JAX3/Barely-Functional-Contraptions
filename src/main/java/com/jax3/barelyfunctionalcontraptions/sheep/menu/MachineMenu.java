package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AreaDisplay;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * What the machines' windows share: shift-clicking between the machine's slots and the player's inventory, the
 * show-area button, and reading 32-bit numbers synced as two shorts.
 *
 * <p>Every machine window lays its slots out the same way: the machine's first ({@code [0, playerStart)}), then the
 * player's 27 inventory slots and 9 hotbar slots ({@code [playerStart, playerStart + 36)}).
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    /** Where the upgrade slots sit: in a panel just right of the 176-wide window. */
    public static final int UPGRADE_X = 184, UPGRADE_Y = 12, UPGRADE_SPACING = 20;
    /** Index of the first upgrade slot, after the player's; -1 for a window without them. */
    private int upgradeStart = -1;

    protected MachineMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    /** Where a shift-clicked player item goes in the machine; see {@link #quickMoveMachine}. */
    @FunctionalInterface
    protected interface IntoMachine {
        /** Moved it (or some of it) in: true; it's the machine's but wouldn't fit: false; not for the machine: null. */
        @Nullable Boolean move(ItemStack stack);
    }

    /**
     * Into slots {@code [from, to)} when {@code accepts} it, but if it doesn't fit there it's shuffled between the
     * player's inventory and hotbar as usual.
     */
    protected IntoMachine tryInto(Predicate<ItemStack> accepts, int from, int to) {
        return stack -> accepts.test(stack) && moveItemStackTo(stack, from, to, false) ? Boolean.TRUE : null;
    }

    /** Into slots {@code [from, to)} when {@code accepts} it, and nowhere else if it doesn't fit. */
    protected IntoMachine onlyInto(Predicate<ItemStack> accepts, int from, int to) {
        return stack -> accepts.test(stack) ? moveItemStackTo(stack, from, to, false) : null;
    }

    /**
     * Shift-click: from the machine to the player (hotbar first from the end, like vanilla); from the player into the
     * machine wherever {@code into} sends it (tried in order), or else between inventory and hotbar.
     *
     * @param quickCraft whether taking from the machine counts as crafting (output slots)
     */
    protected ItemStack quickMoveMachine(Player player, int index, int playerStart, boolean quickCraft, IntoMachine... into) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int inventoryEnd = playerStart + 27, playerEnd = playerStart + 36;
        if (index < playerStart || isUpgradeSlot(slot)) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            if (quickCraft) slot.onQuickCraft(stack, original);
        } else {
            Boolean moved = null;
            // An upgrade card goes into a free upgrade slot the machine takes it in.
            if (upgradeStart >= 0 && stack.getItem() instanceof com.jax3.barelyfunctionalcontraptions.sheep.items.UpgradeItem
                    && moveItemStackTo(stack, upgradeStart, upgradeStart + com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades.SIZE, false)) {
                moved = Boolean.TRUE;
            }
            for (IntoMachine route : into) {
                if (moved != null) break;
                moved = route.move(stack);
                if (moved != null) break;
            }
            if (Boolean.FALSE.equals(moved)) return ItemStack.EMPTY;
            if (moved == null) {
                boolean shuffled = index < inventoryEnd
                        ? moveItemStackTo(stack, inventoryEnd, playerEnd, false)
                        : moveItemStackTo(stack, playerStart, inventoryEnd, false);
                if (!shuffled) return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /**
     * Adds the machine's two upgrade slots (call after the player's inventory). {@code owner} is its block entity on the
     * server; on the client (null, or not {@link com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable}) the
     * slots hold a stand-in the server keeps in step. They take only cards the machine at {@code pos} accepts, one each.
     */
    protected void addUpgradeSlots(@Nullable Object owner, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.Container upgrades = owner instanceof com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable u
                ? u.upgrades() : new net.minecraft.world.SimpleContainer(com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades.SIZE);
        upgradeStart = slots.size();
        for (int i = 0; i < com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades.SIZE; i++) {
            addSlot(new Slot(upgrades, i, UPGRADE_X, UPGRADE_Y + i * UPGRADE_SPACING) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades.accepts(level.getBlockState(pos), stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
    }

    /** Whether this window has upgrade slots. */
    public boolean hasUpgradeSlots() {
        return upgradeStart >= 0;
    }

    /** Whether {@code slot} is one of the upgrade slots. */
    public boolean isUpgradeSlot(Slot slot) {
        return upgradeStart >= 0 && slot.index >= upgradeStart && slot.index < upgradeStart + com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades.SIZE;
    }

    /** The show-area button: toggles {@code area}'s outline when {@code id} is {@code areaButton}. */
    protected static boolean clickAreaButton(Player player, int id, int areaButton, @Nullable AreaDisplay area) {
        if (id != areaButton || area == null) return false;
        area.toggleArea(player);
        return true;
    }

    /** A 32-bit number synced as two shorts, the low half at {@code low}. */
    public static int int32(ContainerData data, int low) {
        return (data.get(low) & 0xFFFF) | ((data.get(low + 1) & 0xFFFF) << 16);
    }
}
