package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity;
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
 * Window of the Thermal Reaction Chamber: the Blaze Rod and Blue Ice boost slots, the player inventory and the
 * chamber's synced numbers (energy, output, water, herds, balance, danger, boosts, status, area setting). Slot
 * positions match {@code textures/gui/thermal_reaction_chamber.png}.
 */
public class ThermalReactionChamberMenu extends MachineMenu {
    public static final int HOT_X = 67, COLD_X = 95, BOOST_Y = 62;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_AREA = 0;

    private static final int PLAYER_START = 2;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable ThermalReactionChamberBlockEntity blockEntity;

    public ThermalReactionChamberMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(2) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return slot == 0 ? stack.is(net.minecraft.world.item.Items.BLAZE_ROD) : stack.is(net.minecraft.world.item.Items.BLUE_ICE);
            }
        }, new SimpleContainerData(ThermalReactionChamberBlockEntity.DATA_COUNT));
    }

    public ThermalReactionChamberMenu(int containerId, Inventory playerInventory, ThermalReactionChamberBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be, data);
    }

    private ThermalReactionChamberMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable ThermalReactionChamberBlockEntity be,
                                       Container boosts, ContainerData data) {
        super(ModMenus.THERMAL_REACTION_CHAMBER.get(), containerId);
        checkContainerSize(boosts, 2);
        checkContainerDataCount(data, ThermalReactionChamberBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.blockEntity = be;
        addSlot(new BoostSlot(boosts, ThermalReactionChamberBlockEntity.HOT_SLOT, HOT_X, BOOST_Y));
        addSlot(new BoostSlot(boosts, ThermalReactionChamberBlockEntity.COLD_SLOT, COLD_X, BOOST_Y));
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    private static final class BoostSlot extends Slot {
        BoostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, PLAYER_START, false,
                onlyInto(s -> slots.get(0).mayPlace(s), 0, 1), onlyInto(s -> slots.get(1).mayPlace(s), 1, 2));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return clickAreaButton(player, id, BUTTON_AREA, blockEntity);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.THERMAL_REACTION_CHAMBER.get());
    }

    private int wide(int lo) {
        return (data.get(lo) & 0xFFFF) | (data.get(lo + 1) << 16);
    }

    public int getEnergy() { return wide(0); }
    public int getCapacity() { return Math.max(1, wide(2)); }
    public int getGenerating() { return wide(4); }
    public int getWater() { return wide(6); }
    public int getTankCapacity() { return Math.max(1, wide(8)); }
    public int getHot() { return data.get(10); }
    public int getCold() { return data.get(11); }
    public int getPairs() { return data.get(12); }
    public int getMaxPairs() { return Math.max(1, data.get(13)); }
    /** Steps towards hot (above 0) or cold (below 0). */
    public int getBalance() { return data.get(14) - ThermalReactionChamberBlockEntity.BALANCE_OFFSET; }
    /** -1000 (about to freeze) to 1000 (about to melt down). */
    public int getDanger() { return data.get(15) - 1000; }
    /** Burn left on the Blaze Rod and Blue Ice, in thousandths. */
    public int getHotBurn() { return data.get(16); }
    public int getColdBurn() { return data.get(17); }
    public ThermalReactionChamberBlockEntity.State getState() { return ThermalReactionChamberBlockEntity.State.byId(data.get(18)); }
    public boolean isShowingArea() { return data.get(19) != 0; }
    public int getSpent() { return data.get(20); }
}
