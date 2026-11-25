package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.WoolBurnerBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.burner.WoolBurnerFuel;
import com.jax3.barelyfunctionalcontraptions.sheep.burner.WoolBurnerFuels;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.WoolBurnerMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * The Wool Burner: burns wool, string and Coal Wool (any {@link WoolBurnerFuel}, data-driven) from its one slot for
 * Forge Energy.
 *
 * <ul>
 *     <li>One item burns at a time, making the fuel's {@code power} FE/t for its {@code burn_time} ticks. The next one
 *     is lit as soon as the last burns out.</li>
 *     <li>It doesn't light a new item while its buffer is full, so fuel is never wasted; an item already burning keeps
 *     burning.</li>
 *     <li>The slot takes fuel only, by hand, hopper or pipe; hoppers and pipes can't take it back out. A comparator reads
 *     how full the slot is.</li>
 * </ul>
 * It stores {@code [wool_burner] capacity} FE and pushes it into every block around it.
 */
public class WoolBurnerBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame implements com.jax3.barelyfunctionalcontraptions.sheep.machine.InventoryHolder, MenuProvider {
    public static final int DATA_COUNT = 9;

    // Its modules: a power buffer (out on every side) and a fuel slot (in only: hoppers and pipes can't take it back).
    private final GeneratorEnergy energy = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.EnergyModule.of(new GeneratorEnergy(20_000, this::setChanged))).handler();
    private final com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory.builder()
            .input("fuel", WoolBurnerFuels::isFuel).build());
    private int burnTime;
    private int burnTotal;
    private int power;
    private int generating;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> Math.min(0xFFFF, burnTime);
                case 5 -> Math.min(0xFFFF, burnTotal);
                case 6 -> power & 0xFFFF;
                case 7 -> power >>> 16;
                case 8 -> generating > 0 ? 1 : 0;
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

    public WoolBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOL_BURNER.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, WoolBurnerBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.resize(ModConfigs.getInt(ModConfigs.SERVER.woolBurnerCapacity));
        if (burnTime <= 0 && energy.getAmountAsInt() < energy.getCapacityAsInt()) light();
        generating = 0;
        if (burnTime > 0) {
            burnTime--;
            generating = power;
            energy.generate(power);
            if (burnTime == 0) {
                power = 0;
                burnTotal = 0;
            }
            setChanged();
        }
        push(level, pos);
        boolean lit = burnTime > 0;
        if (state.getValue(WoolBurnerBlock.LIT) != lit) level.setBlock(pos, state.setValue(WoolBurnerBlock.LIT, lit), Block.UPDATE_ALL);
    }

    /** Lights the next fuel item, if there is one. */
    private void light() {
        ItemStack fuel = inventory.get(0);
        Optional<WoolBurnerFuel> next = WoolBurnerFuels.of(fuel);
        if (next.isEmpty()) return;
        fuel.shrink(1);
        burnTime = next.get().burnTime();
        burnTotal = burnTime;
        power = next.get().power();
        setChanged();
    }

    private void push(ServerLevel level, BlockPos pos) {
        energy.pushToNeighbours(level, pos, ModConfigs.getInt(ModConfigs.SERVER.woolBurnerOutput), (dir, target) -> level.getBlockEntity(target) instanceof WoolBurnerBlockEntity); // burners don't charge each other
    }

    // ------------------------------------------------------------------ accessors

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    /** FE/t made on the last tick. */
    public int generating() {
        return generating;
    }

    /** Ticks left on the item burning now. */
    public int burnTime() {
        return burnTime;
    }

    public int burnTotal() {
        return burnTotal;
    }

    public ItemStack fuel() {
        return inventory.get(0);
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.wool_burner");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WoolBurnerMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); // its modules: energy and fuel
        burnTime = input.getIntOr("burn_time", 0);
        burnTotal = input.getIntOr("burn_total", burnTime);
        power = input.getIntOr("power", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); // its modules
        output.putInt("burn_time", burnTime);
        output.putInt("burn_total", burnTotal);
        output.putInt("power", power);
    }
}
