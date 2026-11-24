package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity;
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
 * Window of the Echo Chamber: no slots yet, just its synced numbers (energy, output, herd, noise level and sweet spot,
 * steadiness, drowsiness, the shriek meter or cooldown, state, area setting).
 */
public class EchoChamberMenu extends MachineMenu {
    public static final int BUTTON_AREA = 0;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable EchoChamberBlockEntity blockEntity;

    public EchoChamberMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainerData(EchoChamberBlockEntity.DATA_COUNT));
    }

    public EchoChamberMenu(int containerId, Inventory playerInventory, EchoChamberBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, data);
    }

    private EchoChamberMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable EchoChamberBlockEntity be, ContainerData data) {
        super(ModMenus.ECHO_CHAMBER.get(), containerId);
        checkContainerDataCount(data, EchoChamberBlockEntity.DATA_COUNT);
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
        return stillValid(access, player, ModBlocks.ECHO_CHAMBER.get());
    }

    private int wide(int lo) {
        return (data.get(lo) & 0xFFFF) | (data.get(lo + 1) << 16);
    }

    public int getEnergy() { return wide(0); }
    public int getCapacity() { return Math.max(1, wide(2)); }
    public int getGenerating() { return wide(4); }
    public int getHerd() { return data.get(6); }
    public int getMaxSheep() { return Math.max(1, data.get(7)); }
    public int getNoise() { return data.get(8); }
    public int getQuietBelow() { return data.get(9); }
    public int getLoudAbove() { return data.get(10); }
    /** Steadiness, 0-100. */
    public int getSteadiness() { return data.get(11); }
    /** How close its sheep are to falling asleep, in thousandths (1000 = asleep). */
    public int getDrowsiness() { return data.get(12); }
    /** How full the shriek meter is, or how much of its cooldown is left after a shriek, in thousandths. */
    public int getShriek() { return data.get(13); }
    public EchoChamberBlockEntity.State getState() { return EchoChamberBlockEntity.State.byId(data.get(14) & 0xF); }
    /** Seconds left of its cooldown after a shriek. */
    public int getCooldownSeconds() { return data.get(14) >>> 4; }
    public boolean isShowingArea() { return data.get(15) != 0; }
}
