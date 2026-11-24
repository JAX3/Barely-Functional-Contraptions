package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AltarStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity;
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

/** Window of the Infusion Altar: the feeding item in the centre, four catalysts around it, and the player inventory. */
public class InfusionAltarMenu extends MachineMenu {
    /** Item positions: centre, then north, east, south, west. */
    public static final int[][] SLOT_POS = {{74, 39}, {74, 18}, {95, 39}, {74, 60}, {53, 39}};
    public static final int INVENTORY_Y = 111;
    private static final int PLAYER_START = InfusionAltarBlockEntity.SIZE;
    private static final int PLAYER_END = PLAYER_START + 36;

    /** Menu button: show or hide the area a sheep must stand in. */
    public static final int BUTTON_AREA = 0;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Player player;

    public InfusionAltarMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), new SimpleContainer(InfusionAltarBlockEntity.SIZE),
                new SimpleContainerData(InfusionAltarBlockEntity.DATA_COUNT));
    }

    public InfusionAltarMenu(int containerId, Inventory playerInventory, InfusionAltarBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, data);
    }

    private InfusionAltarMenu(int containerId, Inventory playerInventory, BlockPos pos, Container container, ContainerData data) {
        super(ModMenus.INFUSION_ALTAR.get(), containerId);
        checkContainerSize(container, InfusionAltarBlockEntity.SIZE);
        checkContainerDataCount(data, InfusionAltarBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.player = playerInventory.player;
        for (int i = 0; i < InfusionAltarBlockEntity.SIZE; i++) {
            final int slot = i;
            addSlot(new Slot(container, i, SLOT_POS[i][0], SLOT_POS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return slot == InfusionAltarBlockEntity.FEED
                            ? InfusionAltarBlockEntity.resourceFor(player.level(), stack).isPresent()
                            : !stack.isEmpty();
                }
            });
        }
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, PLAYER_START, false,
                tryInto(s -> InfusionAltarBlockEntity.resourceFor(player.level(), s).isPresent(), 0, 1), // feeding items to the centre
                tryInto(s -> InfusionAltarBlockEntity.isCatalyst(player.level(), s), 1, PLAYER_START)); // catalysts round it
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_AREA) return false;
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof InfusionAltarBlockEntity be) be.toggleArea(player);
        });
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.INFUSION_ALTAR.get());
    }


    public int getEnergy() { return int32(data, 0); }
    public int getCapacity() { return int32(data, 2); }
    public int getProgress() { return data.get(4) & 0xFFFF; }
    public int getMaxProgress() { return data.get(5) & 0xFFFF; }
    public int getEnergyPerTick() { return int32(data, 6); }
    public AltarStatus getStatus() { return AltarStatus.byId(data.get(8)); }
    public boolean hasSheep() { return data.get(9) != 0; }
    public boolean isShowingArea() { return data.get(10) != 0; }

    public float getEnergyFraction() {
        int capacity = getCapacity();
        return capacity <= 0 ? 0 : Math.min(1F, getEnergy() / (float) capacity);
    }

    public float getProgressFraction() {
        int max = getMaxProgress();
        return max <= 0 ? 0 : Math.min(1F, getProgress() / (float) max);
    }
}
