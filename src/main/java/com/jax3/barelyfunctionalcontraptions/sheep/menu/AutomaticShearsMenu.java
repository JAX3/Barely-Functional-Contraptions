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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Container menu of the Automatic Shears. Slot coordinates match {@code textures/gui/automatic_shears.png}. */
public class AutomaticShearsMenu extends MachineMenu {
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
        addUpgradeSlots(be, playerInventory.player.level(), pos);
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
        return quickMoveMachine(player, index, PLAYER_START, true);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_REDSTONE && blockEntity != null) {
            blockEntity.cycleRedstoneMode();
            return true;
        }
        return clickAreaButton(player, id, BUTTON_AREA, blockEntity);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.AUTOMATIC_SHEARS.get());
    }

    // ------------------------------------------------------------------ synced values (client reads these)


    public int getEnergy() { return int32(data, 0); }
    public int getCapacity() { return int32(data, 2); }
    public int getCooldown() { return data.get(4); }
    public int getInterval() { return Math.max(1, data.get(5)); }
    public MachineStatus getStatus() { return MachineStatus.byId(data.get(6)); }
    public RedstoneMode getRedstoneMode() { return RedstoneMode.byId(data.get(7)); }
    public boolean isPowered() { return data.get(8) != 0; }
    public int getEnergyPerShear() { return int32(data, 9); }
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
