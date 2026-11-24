package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.RedstoneMode;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.SeparatorInventory;
import com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlocks;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

/** Container menu of the Mechanical Separator. Slot coordinates match {@code textures/gui/mechanical_separator.png}. */
public class MechanicalSeparatorMenu extends MachineMenu {
    public static final int INPUT_X = 44, INPUT_Y = 56;
    public static final int PRIMARY_X = 108, PRIMARY_Y = 47;
    public static final int SECONDARY_X = 108, SECONDARY_Y = 69;
    public static final int BONUS_X = 130, BONUS_Y = 47;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_REDSTONE = 0;

    private static final int MACHINE_SLOTS = SeparatorInventory.SIZE;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Level level;
    private final @Nullable MechanicalSeparatorBlockEntity blockEntity;

    /** Client-side constructor, called with the extra data written by {@code openMenu(provider, pos)}. */
    public MechanicalSeparatorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null,
                new SeparatorInventory(stack -> SeparationLookup.canProcess(playerInventory.player.level(), stack), () -> {}),
                new SimpleContainerData(MechanicalSeparatorBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public MechanicalSeparatorMenu(int containerId, Inventory playerInventory, MechanicalSeparatorBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be.getInventory(), data);
    }

    private MechanicalSeparatorMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable MechanicalSeparatorBlockEntity be,
                                    SeparatorInventory inventory, ContainerData data) {
        super(ModMenus.MECHANICAL_SEPARATOR.get(), containerId);
        checkContainerDataCount(data, MechanicalSeparatorBlockEntity.DATA_COUNT);
        this.level = playerInventory.player.level();
        this.access = ContainerLevelAccess.create(level, pos);
        this.data = data;
        this.blockEntity = be;

        addSlot(new ResourceHandlerSlot(inventory, inventory::set, SeparatorInventory.INPUT, INPUT_X, INPUT_Y));
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, SeparatorInventory.PRIMARY, PRIMARY_X, PRIMARY_Y));
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, SeparatorInventory.SECONDARY, SECONDARY_X, SECONDARY_Y));
        addSlot(new ResourceHandlerSlot(inventory, inventory::set, SeparatorInventory.BONUS, BONUS_X, BONUS_Y));
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, PLAYER_START, true,
                onlyInto(s -> SeparationLookup.canProcess(level, s), SeparatorInventory.INPUT, SeparatorInventory.INPUT + 1));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_REDSTONE && blockEntity != null) {
            blockEntity.cycleRedstoneMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MECHANICAL_SEPARATOR.get());
    }

    // ------------------------------------------------------------------ synced values (client reads these)


    public int getProgress() { return int32(data, 0); }
    public int getMaxProgress() { return int32(data, 2); }
    public int getEnergy() { return int32(data, 4); }
    public int getCapacity() { return int32(data, 6); }
    public int getEnergyPerTick() { return int32(data, 8); }
    public MachineStatus getStatus() { return MachineStatus.byId(data.get(10)); }
    public RedstoneMode getRedstoneMode() { return RedstoneMode.byId(data.get(11)); }
    public boolean isPowered() { return data.get(12) != 0; }

    public float getProgressFraction() {
        int max = getMaxProgress();
        return max <= 0 ? 0 : Math.min(1.0F, getProgress() / (float) max);
    }

    public float getEnergyFraction() {
        int cap = getCapacity();
        return cap <= 0 ? 0 : Math.min(1.0F, getEnergy() / (float) cap);
    }
}
