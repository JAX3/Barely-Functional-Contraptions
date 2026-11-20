package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Window of the Machine Status Panel (its screen's top-left panel): no slots, just buttons for each linked machine (move
 * up or down, unlink, alert on or off) and for the panel (sort, theme, what its comparator reads). Renaming a machine
 * goes through {@link com.jax3.barelyfunctionalcontraptions.sheep.network.RenamePanelLinkPayload}. What it shows comes
 * from the block entity, which the server keeps synced.
 */
public class StatusPanelMenu extends AbstractContainerMenu {
    /** Per-machine buttons are {@code action * ROW + link}. */
    public static final int ROW = 100, UP = 1, DOWN = 2, UNLINK = 3, ALERT = 4;
    public static final int SORT = 1000, THEME = 1001, SIGNAL = 1002;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final @Nullable StatusPanelBlockEntity blockEntity;

    public StatusPanelMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null);
    }

    public StatusPanelMenu(int containerId, Inventory playerInventory, StatusPanelBlockEntity be) {
        this(containerId, playerInventory, be.getBlockPos(), be);
    }

    private StatusPanelMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable StatusPanelBlockEntity be) {
        super(ModMenus.STATUS_PANEL.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.pos = pos;
        this.blockEntity = be;
    }

    public BlockPos pos() {
        return pos;
    }

    public @Nullable StatusPanelBlockEntity blockEntity() {
        return blockEntity;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null || !(player.level() instanceof ServerLevel level)) return false;
        switch (id) {
            case SORT -> blockEntity.cycleSort(level);
            case THEME -> blockEntity.cycleTheme(level);
            case SIGNAL -> blockEntity.toggleSignal(level);
            default -> {
                int link = id % ROW;
                switch (id / ROW) {
                    case UP -> blockEntity.move(level, link, -1);
                    case DOWN -> blockEntity.move(level, link, 1);
                    case UNLINK -> blockEntity.unlink(level, link);
                    case ALERT -> blockEntity.toggleAlert(level, link);
                    default -> {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.STATUS_PANEL.get());
    }
}
