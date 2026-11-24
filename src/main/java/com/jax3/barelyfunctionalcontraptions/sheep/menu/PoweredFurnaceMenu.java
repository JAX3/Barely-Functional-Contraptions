package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PoweredFurnaceBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Window of the Powered Furnace: its input and output slots, the player inventory and the furnace's synced numbers. */
public class PoweredFurnaceMenu extends MachineMenu {
    public static final int INPUT_X = 56, INPUT_Y = 22;
    public static final int OUTPUT_X = 124, OUTPUT_Y = 31;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_AREA = 0;

    private static final int PLAYER_START = 2;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Level level;
    private final @Nullable PoweredFurnaceBlockEntity blockEntity;

    public PoweredFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(2), new SimpleContainerData(PoweredFurnaceBlockEntity.DATA_COUNT));
    }

    public PoweredFurnaceMenu(int containerId, Inventory playerInventory, PoweredFurnaceBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be, data);
    }

    private PoweredFurnaceMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable PoweredFurnaceBlockEntity be, Container container, ContainerData data) {
        super(ModMenus.POWERED_FURNACE.get(), containerId);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, PoweredFurnaceBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.level = playerInventory.player.level();
        this.blockEntity = be;
        addSlot(new Slot(container, PoweredFurnaceBlockEntity.INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PoweredFurnaceBlockEntity.canSmelt(level, stack);
            }
        });
        addSlot(new Slot(container, PoweredFurnaceBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            /** Taking the results gives the smelting experience, as a Furnace's output slot does. */
            @Override
            public void onTake(Player player, ItemStack stack) {
                if (blockEntity != null && player.level() instanceof ServerLevel server) blockEntity.awardExperience(server, player);
                super.onTake(player, stack);
            }
        });
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, PLAYER_START, false, tryInto(s -> PoweredFurnaceBlockEntity.canSmelt(level, s), 0, 1));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return clickAreaButton(player, id, BUTTON_AREA, blockEntity);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.POWERED_FURNACE.get());
    }


    public int getEnergy() { return int32(data, 0); }
    public int getCapacity() { return int32(data, 2); }
    public int getProgress() { return data.get(4); }
    public int getMaxProgress() { return Math.max(1, data.get(5)); }
    public int getEnergyPerTick() { return data.get(6); }
    public MachineStatus getStatus() { return MachineStatus.byId(data.get(7)); }
    /** Speed in hundredths: 100 alone, more with Blaze sheep. */
    public int getSpeed() { return data.get(8); }
    public int getHeaters() { return data.get(9); }
    public int getMaxHeaters() { return Math.max(1, data.get(10)); }
    public boolean isShowingArea() { return data.get(11) != 0; }
    /** Its cook time as a share of a Furnace's, in hundredths (75). */
    public int getCookPercent() { return data.get(12); }

    public float getEnergyFraction() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : Math.min(1F, getEnergy() / (float) capacity);
    }
}
