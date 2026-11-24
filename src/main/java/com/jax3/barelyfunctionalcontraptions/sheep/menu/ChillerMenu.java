package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ChillerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
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

/**
 * Window of the Snow Sheep Chiller: its Ice slot (take only), the player inventory and the chiller's synced numbers
 * (water, freezing progress, cold, Snow sheep, status, area setting). Slot positions match {@code textures/gui/chiller.png}.
 */
public class ChillerMenu extends MachineMenu {
    public static final int OUTPUT_X = 134, OUTPUT_Y = 41;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_AREA = 0;

    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable ChillerBlockEntity blockEntity;

    public ChillerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(1), new SimpleContainerData(ChillerBlockEntity.DATA_COUNT));
    }

    public ChillerMenu(int containerId, Inventory playerInventory, ChillerBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be, data);
    }

    private ChillerMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable ChillerBlockEntity be, Container output, ContainerData data) {
        super(ModMenus.CHILLER.get(), containerId);
        checkContainerSize(output, 1);
        checkContainerDataCount(data, ChillerBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.blockEntity = be;
        addSlot(new Slot(output, 0, OUTPUT_X, OUTPUT_Y) {
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
        return quickMoveMachine(player, index, PLAYER_START, false);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return clickAreaButton(player, id, BUTTON_AREA, blockEntity);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CHILLER.get());
    }

    public int getWater() { return data.get(0); }
    public int getTankCapacity() { return Math.max(1, data.get(1)); }
    public int getProgress() { return data.get(2); }
    public int getTicksPerIce() { return Math.max(1, data.get(3)); }
    /** Freezing speed in hundredths (100 = one Snow sheep). */
    public int getCold() { return data.get(4); }
    public int getMaxCold() { return Math.max(1, data.get(5)); }
    public int getSources() { return data.get(6); }
    public int getMaxSources() { return Math.max(1, data.get(7)); }
    public MachineStatus getStatus() { return MachineStatus.byId(data.get(8)); }
    public boolean isShowingArea() { return data.get(9) != 0; }
}
