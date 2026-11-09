package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.AutomaticShearsBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.AutomaticShearsMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * The Automatic Shears: an FE-powered machine that, every {@code [automatic_shears] shearInterval} ticks, shears every
 * ready sheep standing in the 3x3 area in front of it and keeps the wool in its 9-slot inventory.
 *
 * <ul>
 *     <li>Each sheep sheared costs {@code energyPerShear} FE from its buffer. Energy is accepted on every face except
 *     the front, from any FE cable or generator (a Mycelial Generator placed against it works directly).</li>
 *     <li>Shearing goes through NeoForge's {@link IShearable}, the same path as hand shears and dispensers, so infused
 *     sheep drop resource wool and use up one infusion use exactly as usual.</li>
 *     <li>Which mobs it shears is the entity tag {@code #barelyfunctionalcontraptions:auto_shearable} (sheep by default), so it
 *     never turns a mooshroom into a cow or takes a snow golem's pumpkin.</li>
 *     <li>The inventory is output-only: players, hoppers below and pipes can take items out but cannot put items in.
 *     When it is full it stops shearing; if one sheep's drops don't all fit, the rest are dropped on the ground at the
 *     sheep, so nothing is ever deleted.</li>
 *     <li>Redstone control works like the Mechanical Separator's (ignored / with signal / without signal / disabled).
 *     Energy and the redstone mode are kept when the block is broken.</li>
 * </ul>
 */
public class AutomaticShearsBlockEntity extends BaseContainerBlockEntity {
    public static final int SIZE = 9;
    /** Number of int fields synced to the menu (split into 16-bit halves where needed). */
    public static final int DATA_COUNT = 11;
    /** Ticks the status stays on "Shearing" after a sweep that sheared something. */
    private static final int SHEARING_DISPLAY_TICKS = 12;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final ShearsEnergyStorage energy = new ShearsEnergyStorage(this::setChanged);
    private int cooldown;
    private int interval = 20;
    private int shearingDisplay;
    private MachineStatus status = MachineStatus.IDLE;
    private RedstoneMode redstoneMode = ModConfigs.get(ModConfigs.SERVER.defaultRedstoneMode);
    private boolean powered;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int cost = energyPerShear();
            return switch (index) {
                case 0 -> energy.getEnergy() & 0xFFFF;
                case 1 -> energy.getEnergy() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> Math.max(0, cooldown);
                case 5 -> interval;
                case 6 -> status.ordinal();
                case 7 -> redstoneMode.ordinal();
                case 8 -> powered ? 1 : 0;
                case 9 -> cost & 0xFFFF;
                case 10 -> cost >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative; the client-side menu keeps its own SimpleContainerData copy.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AutomaticShearsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOMATIC_SHEARS.get(), pos, state);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AutomaticShearsBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.refreshLimits();
        powered = level.hasNeighborSignal(pos);
        interval = Math.max(1, ModConfigs.getInt(ModConfigs.SERVER.automaticShearsInterval));
        if (cooldown > interval) cooldown = interval;
        if (shearingDisplay > 0) shearingDisplay--;
        RedstoneMode mode = ModConfigs.getBool(ModConfigs.SERVER.redstoneControlEnabled) ? redstoneMode : ModConfigs.get(ModConfigs.SERVER.defaultRedstoneMode);

        if (!mode.allows(powered)) {
            status = MachineStatus.REDSTONE_DISABLED;
        } else if (!hasSpace()) {
            status = MachineStatus.OUTPUT_FULL;
        } else if (energy.getEnergy() < energyPerShear()) {
            status = MachineStatus.NO_ENERGY;
        } else if (--cooldown > 0) {
            status = shearingDisplay > 0 ? MachineStatus.PROCESSING : MachineStatus.IDLE;
        } else {
            cooldown = interval;
            if (shearArea(level, pos, state.getValue(AutomaticShearsBlock.FACING)) > 0) shearingDisplay = SHEARING_DISPLAY_TICKS;
            status = shearingDisplay > 0 ? MachineStatus.PROCESSING : MachineStatus.IDLE;
        }
        boolean active = status == MachineStatus.IDLE || status == MachineStatus.PROCESSING;
        if (state.getValue(AutomaticShearsBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(AutomaticShearsBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    public int energyPerShear() {
        return ModConfigs.getInt(ModConfigs.SERVER.automaticShearsEnergyPerShear);
    }

    /**
     * The 3x3 area in front of the block: the three block columns 1-3 blocks out, one column either side of the centre
     * line, from one block below the machine to one block above it. A mob counts when its feet are inside.
     */
    public static AABB shearingArea(BlockPos pos, Direction facing) {
        BlockPos centre = pos.relative(facing, 2);
        return new AABB(centre.getX() - 1, pos.getY() - 1, centre.getZ() - 1, centre.getX() + 2, pos.getY() + 2, centre.getZ() + 2);
    }

    /** Shears every ready mob in the area that it has energy and room for. Returns how many were sheared. */
    public int shearArea(ServerLevel level, BlockPos pos, Direction facing) {
        if (!hasSpace()) return 0;
        AABB area = shearingArea(pos, facing);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive()
                && mob.getType().builtInRegistryHolder().is(ModTags.AUTO_SHEARABLE) && area.contains(mob.position()));
        int cost = energyPerShear();
        int sheared = 0;
        for (Mob mob : mobs) {
            if (!(mob instanceof IShearable shearable)) continue;
            ItemStack shears = new ItemStack(Items.SHEARS); // a fresh pair each time: the machine never wears out
            BlockPos at = mob.blockPosition();
            if (!shearable.isShearable(null, shears, level, at)) continue;
            if (!energy.consume(cost)) break; // out of power: the rest wait for the next sweep
            for (ItemStack drop : shearable.onSheared(null, shears, level, at)) {
                ItemStack rest = insert(drop.copy());
                if (!rest.isEmpty()) {
                    // Inventory filled up mid-way: put the rest on the ground at the sheep rather than delete it.
                    ItemEntity entity = new ItemEntity(level, mob.getX(), mob.getY() + 0.5, mob.getZ(), rest);
                    entity.setDefaultPickUpDelay();
                    level.addFreshEntity(entity);
                }
            }
            sheared++;
            if (!hasSpace()) break;
        }
        if (sheared > 0) setChanged();
        return sheared;
    }

    /** True while at least one more item fits somewhere. */
    public boolean hasSpace() {
        for (ItemStack stack : items) {
            if (stack.isEmpty() || stack.getCount() < getMaxStackSize(stack)) return true;
        }
        return false;
    }

    /** Puts {@code stack} into the inventory, merging with matching stacks first. Returns what did not fit. */
    public ItemStack insert(ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
                ItemStack target = items.get(i);
                boolean merge = pass == 0 && !target.isEmpty() && ItemStack.isSameItemSameComponents(stack, target);
                boolean fill = pass == 1 && target.isEmpty();
                if (!merge && !fill) continue;
                int move = Math.min(stack.getCount(), getMaxStackSize(stack) - target.getCount());
                if (move <= 0) continue;
                if (fill) {
                    items.set(i, stack.split(move));
                } else {
                    target.grow(move);
                    stack.shrink(move);
                }
            }
        }
        setChanged();
        return stack;
    }

    // ------------------------------------------------------------------ accessors

    public ShearsEnergyStorage getEnergy() {
        return energy;
    }

    /** FE input on every face except the front. */
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        if (side != null && side == getBlockState().getValue(AutomaticShearsBlock.FACING)) return null;
        return energy;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        redstoneMode = mode;
        setChanged();
    }

    public void cycleRedstoneMode() {
        if (!ModConfigs.getBool(ModConfigs.SERVER.redstoneControlEnabled)) return;
        setRedstoneMode(redstoneMode.next());
    }

    // ------------------------------------------------------------------ container

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    /** Output only: nothing can be put in by players, hoppers or pipes. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.barelyfunctionalcontraptions.automatic_shears");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AutomaticShearsMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        input.child("energy").ifPresent(energy::deserialize);
        cooldown = input.getIntOr("cooldown", 0);
        redstoneMode = RedstoneMode.byId(input.getIntOr("redstone_mode", redstoneMode.ordinal()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        energy.serialize(output.child("energy"));
        output.putInt("cooldown", cooldown);
        output.putInt("redstone_mode", redstoneMode.ordinal());
    }

    /** Broken blocks keep their stored energy and redstone mode (same component as the separator). */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (energy.getEnergy() > 0 || redstoneMode != RedstoneMode.IGNORED) {
            components.set(ModDataComponents.MACHINE_CONTENTS.get(), new MachineContents(energy.getEnergy(), redstoneMode));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        MachineContents contents = components.get(ModDataComponents.MACHINE_CONTENTS.get());
        if (contents != null) {
            energy.set(Math.min(contents.energy(), energy.getCapacityAsInt()));
            redstoneMode = contents.redstoneMode();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("energy");
        output.discard("redstone_mode");
    }
}
