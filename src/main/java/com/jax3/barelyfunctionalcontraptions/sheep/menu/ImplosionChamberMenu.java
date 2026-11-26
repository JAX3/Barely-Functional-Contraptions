package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity.Status;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity.Target;
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
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Window of the Implosion Chamber: its input, the Gravel it holds to crack again (to look at only), its output, the
 * player inventory and its synced numbers (power, status, what it makes, the next spark, its sheep, blasts). Slot
 * positions match {@code textures/gui/implosion_chamber.png}.
 */
public class ImplosionChamberMenu extends MachineMenu {
    public static final int INPUT_X = 50, CRACKED_X = 88, OUTPUT_X = 126, SLOT_Y = 26;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_AREA = 0, BUTTON_TARGET = 1;

    private static final int PLAYER_START = 3;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable ImplosionChamberBlockEntity blockEntity;

    public ImplosionChamberMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(3), new SimpleContainerData(ImplosionChamberBlockEntity.DATA_COUNT));
    }

    public ImplosionChamberMenu(int containerId, Inventory playerInventory, ImplosionChamberBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be, data);
    }

    private ImplosionChamberMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable ImplosionChamberBlockEntity be, Container slots, ContainerData data) {
        super(ModMenus.IMPLOSION_CHAMBER.get(), containerId);
        checkContainerSize(slots, 3);
        checkContainerDataCount(data, ImplosionChamberBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.blockEntity = be;
        addSlot(new Slot(slots, ImplosionChamberBlockEntity.INPUT, INPUT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.COBBLESTONE) || (stack.is(Items.GRAVEL) && getTarget() == Target.SAND);
            }
        });
        addSlot(new Slot(slots, ImplosionChamberBlockEntity.CRACKED, CRACKED_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false; // the Gravel it's holding to crack again: to look at
            }
        });
        addSlot(new Slot(slots, ImplosionChamberBlockEntity.OUTPUT, OUTPUT_X, SLOT_Y) {
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
        if (index == ImplosionChamberBlockEntity.CRACKED) return ItemStack.EMPTY;
        return quickMoveMachine(player, index, PLAYER_START, false,
                onlyInto(s -> s.is(Items.COBBLESTONE) || (s.is(Items.GRAVEL) && getTarget() == Target.SAND), 0, 1));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clickAreaButton(player, id, BUTTON_AREA, blockEntity)) return true;
        if (id == BUTTON_TARGET && blockEntity != null) {
            blockEntity.setTarget(blockEntity.target() == Target.SAND ? Target.GRAVEL : Target.SAND);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.IMPLOSION_CHAMBER.get());
    }

    private int wide(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) & 0xFFFF) << 16;
    }

    public int getEnergy() { return wide(0); }
    public int getCapacity() { return Math.max(1, wide(2)); }
    public Status getStatus() { return Status.byId(data.get(4)); }
    public boolean isShowingArea() { return data.get(5) != 0; }
    public Target getTarget() { return Target.byId(data.get(6)); }
    /** Ticks until its next spark. */
    public int getCooldown() { return data.get(7); }
    public int getInterval() { return Math.max(1, data.get(8)); }
    public int getReadySheep() { return data.get(9); }
    public int getRegrowingSheep() { return data.get(10); }
    public int getBlasts() { return wide(11); }
    public int getCracked() { return wide(13); }
    /** Ticks its porthole has left to glow after a blast. */
    public int getFlash() { return data.get(15); }
}
