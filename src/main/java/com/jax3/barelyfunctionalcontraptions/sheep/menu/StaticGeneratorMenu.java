package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Window of the Static Generator: no slots, just its synced numbers (energy, output, the dome's charge, the herd, its
 * combs' stages, state, area setting).
 */
public class StaticGeneratorMenu extends MachineMenu {
    public static final int BUTTON_AREA = 0;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable StaticGeneratorBlockEntity blockEntity;

    public StaticGeneratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainerData(StaticGeneratorBlockEntity.DATA_COUNT));
    }

    public StaticGeneratorMenu(int containerId, Inventory playerInventory, StaticGeneratorBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, data);
    }

    private StaticGeneratorMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable StaticGeneratorBlockEntity be, ContainerData data) {
        super(ModMenus.STATIC_GENERATOR.get(), containerId);
        checkContainerDataCount(data, StaticGeneratorBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.blockEntity = be;
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return clickAreaButton(player, id, BUTTON_AREA, blockEntity);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.STATIC_GENERATOR.get());
    }

    private int wide(int lo) {
        return (data.get(lo) & 0xFFFF) | (data.get(lo + 1) << 16);
    }

    public int getEnergy() { return wide(0); }
    public int getCapacity() { return Math.max(1, wide(2)); }
    public int getGenerating() { return wide(4); }
    public int getCharge() { return wide(6); }
    public int getDomeCapacity() { return Math.max(1, wide(8)); }
    public int getHerd() { return data.get(10); }
    public int getMaxSheep() { return Math.max(1, data.get(11)); }
    public int getCombCount() { return data.get(12); }
    /** Comb {@code i}: whether there is one, whether it's waxed, and its stage (0 fresh - 3 oxidised). */
    public boolean hasComb(int i) { return (data.get(13 + i) & 8) != 0; }
    public boolean isCombWaxed(int i) { return (data.get(13 + i) & 4) != 0; }
    public int getCombAge(int i) { return data.get(13 + i) & 3; }
    public StaticGeneratorBlockEntity.State getState() { return StaticGeneratorBlockEntity.State.byId(data.get(17)); }
    public boolean isShowingArea() { return data.get(18) != 0; }
    /** How full the dome must be to shock, in thousandths. */
    public int getShockAt() { return data.get(19); }
}
