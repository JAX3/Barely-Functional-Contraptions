package com.jax3.barelyfunctionalcontraptions.sheep.menu;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity.Status;
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
 * Window of the Grass Seeder: its Bone Meal slot, the player inventory and the seeder's synced numbers (power, water,
 * status, how green its ground is, the ground map, its sheep grazing). Slot positions match
 * {@code textures/gui/grass_seeder.png}.
 */
public class GrassSeederMenu extends MachineMenu {
    public static final int BONE_X = 128, BONE_Y = 22;
    public static final int INVENTORY_Y = 111;
    public static final int BUTTON_AREA = 0, BUTTON_TRIM = 1;

    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final @Nullable GrassSeederBlockEntity blockEntity;

    public GrassSeederMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos(), null, new SimpleContainer(1), new SimpleContainerData(GrassSeederBlockEntity.DATA_COUNT));
    }

    public GrassSeederMenu(int containerId, Inventory playerInventory, GrassSeederBlockEntity be, ContainerData data) {
        this(containerId, playerInventory, be.getBlockPos(), be, be, data);
    }

    private GrassSeederMenu(int containerId, Inventory playerInventory, BlockPos pos, @Nullable GrassSeederBlockEntity be, Container boneMeal, ContainerData data) {
        super(ModMenus.GRASS_SEEDER.get(), containerId);
        checkContainerSize(boneMeal, 1);
        checkContainerDataCount(data, GrassSeederBlockEntity.DATA_COUNT);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        this.blockEntity = be;
        addSlot(new Slot(boneMeal, 0, BONE_X, BONE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BONE_MEAL);
            }
        });
        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addUpgradeSlots(be, playerInventory.player.level(), pos);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveMachine(player, index, PLAYER_START, false, onlyInto(s -> s.is(Items.BONE_MEAL), 0, 1));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clickAreaButton(player, id, BUTTON_AREA, blockEntity)) return true;
        if (id == BUTTON_TRIM && blockEntity != null) {
            blockEntity.setTrim(!blockEntity.trims());
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.GRASS_SEEDER.get());
    }

    private int wide(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) & 0xFFFF) << 16;
    }

    public int getEnergy() { return wide(0); }
    public int getCapacity() { return Math.max(1, wide(2)); }
    public int getWater() { return wide(4); }
    public int getTankCapacity() { return Math.max(1, wide(6)); }
    public Status getStatus() { return Status.byId(data.get(8)); }
    public boolean isShowingArea() { return data.get(9) != 0; }
    public int getGrass() { return data.get(10); }
    public int getDirt() { return data.get(11); }
    public int getShortGrass() { return data.get(12); }
    public int getBoneCharges() { return data.get(13); }
    public int getRadius() { return data.get(14); }
    /** Sheared, grown sheep in its area. */
    public int getGrazing() { return data.get(15); }
    public int getTended() { return wide(16); }
    public int getTallGrass() { return data.get(18); }
    public boolean isTrimming() { return data.get(19) != 0; }

    /** The ground map, packed two bits a block (see {@link GrassSeederBlockEntity#tile}). */
    public int[] getMap() {
        int[] map = new int[GrassSeederBlockEntity.MAP_VALUES];
        for (int i = 0; i < map.length; i++) map[i] = data.get(GrassSeederBlockEntity.MAP_DATA + i) & 0xFFFF;
        return map;
    }
}
