package com.jax3.barelyfunctionalcontraptions.mycelial.blockentity;

import com.jax3.barelyfunctionalcontraptions.mycelial.block.MycelialGeneratorBlock;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.AutomationItemHandler;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorEnergyStorage;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorFluidTank;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.GeneratorItemHandler;
import com.jax3.barelyfunctionalcontraptions.mycelial.capability.SidedFluidHandler;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.EnergyRounding;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.energy.EnergyAccumulator;
import com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelDefinition;
import com.jax3.barelyfunctionalcontraptions.mycelial.fuel.FuelRegistry;
import com.jax3.barelyfunctionalcontraptions.mycelial.menu.GeneratorStatus;
import com.jax3.barelyfunctionalcontraptions.mycelial.menu.MycelialGeneratorMenu;
import com.jax3.barelyfunctionalcontraptions.mycelial.mycelium.MyceliumChance;
import com.jax3.barelyfunctionalcontraptions.mycelial.mycelium.MyceliumSpreadManager;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Server-authoritative state and logic of one Mycelial Generator.
 * <p>
 * Each server tick the generator:
 * <ol>
 *     <li>starts a new mushroom when the previous one is exhausted and it can run;</li>
 *     <li>produces {@code base x fuel} FE/t (each generator stands alone), consumes water on its interval
 *     and advances the mycelium attempt timer;</li>
 *     <li>pushes energy to neighbouring receivers.</li>
 * </ol>
 * Nothing here runs on the client.
 */
public class MycelialGeneratorBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame implements MenuProvider {
    private static final Direction[] DIRECTIONS = Direction.values();

    // Its modules (an inventory, a tank and a power buffer), saved the way the generator always saved them; pipes and
    // cables see the sided views below (see itemsFor, fluidFor and energyFor).
    private final GeneratorItemHandler inventory = new GeneratorItemHandler(this::setChanged);
    private final AutomationItemHandler automationItems = new AutomationItemHandler(inventory);
    private final GeneratorFluidTank tank = new GeneratorFluidTank(this::setChanged);
    private final SidedFluidHandler externalFluid = new SidedFluidHandler(tank);
    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(this::setChanged);
    {
        install(new com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineModule() { // the inventory
            @Override
            public void save(ValueOutput output) {
                output.putChild("Inventory", inventory);
            }

            @Override
            public void load(ValueInput input) {
                input.readChild("Inventory", inventory);
            }

            @Override
            public void dropContents(Level level, BlockPos pos) {
                for (int slot = 0; slot < inventory.size(); slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                        inventory.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                }
            }
        });
        install(new com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineModule() { // the tank
            @Override
            public void save(ValueOutput output) {
                output.putChild("Tank", tank);
            }

            @Override
            public void load(ValueInput input) {
                input.readChild("Tank", tank);
            }
        });
        install(new com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineModule() { // the power buffer
            @Override
            public void save(ValueOutput output) {
                output.putInt("Energy", energy.getAmountAsInt());
            }

            @Override
            public void load(ValueInput input) {
                energy.setEnergy(input.getIntOr("Energy", 0));
            }
        });
    }
    private final EnergyAccumulator accumulator = new EnergyAccumulator();

    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<EnergyHandler, Direction>[] energyCaches = new BlockCapabilityCache[6];

    // Fuel (snapshot of the definition that was consumed, so config edits never alter an item mid-burn)
    private int burnRemaining;
    private int burnTotal;
    private String fuelItemId = "";
    private double fuelEnergyMultiplier = 1.0D;
    private double fuelWaterMultiplier = 1.0D;
    private double fuelMyceliumBonus = 1.0D;

    // Water
    private int waterTimer;
    private double waterRemainder;

    // Mycelium
    private double chance = -1.0D;
    private int attemptTimer;
    private int convertedThisSession;
    private int hugeMushroomsThisSession;

    // Runtime (not persisted except 'active')
    private boolean active;
    private GeneratorState state = GeneratorState.NO_FUEL;
    private double lastExactOutput;
    private long extractedWindow;
    private int windowTicks;
    private double extractionRate;
    private int outputTimer;
    private int containerTimer;
    private int lastComparatorSignal = -1;

    public MycelialGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MYCELIAL_GENERATOR.get(), pos, blockState);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, MycelialGeneratorBlockEntity generator) {
        if (level instanceof ServerLevel serverLevel) {
            generator.tickServer(serverLevel, blockState);
        }
    }

    private void tickServer(ServerLevel level, BlockState blockState) {
        tank.syncCapacity();
        energy.clampToCapacity();
        if (chance < 0) {
            chance = ServerConfig.initialChance();
        }

        if (++containerTimer >= ServerConfig.get(ServerConfig.CONTAINER_INTERVAL)) {
            containerTimer = 0;
            processFluidContainer();
        }

        boolean wasActive = active;

        // While no fuel is burning, check water against the fuel waiting in the slot.
        FuelDefinition pending = burnRemaining <= 0 ? FuelRegistry.get(inventory.getStackInSlot(GeneratorItemHandler.FUEL_SLOT)) : null;
        double waterMultiplier = pending != null ? pending.waterMultiplier() : fuelWaterMultiplier;
        boolean waterOk = hasWaterFor(nextWaterDrain(waterMultiplier));
        boolean energyOk = !ServerConfig.get(ServerConfig.PAUSE_WHEN_ENERGY_FULL) || energy.space() > 0;

        if (burnRemaining <= 0 && pending != null && waterOk && energyOk) {
            startNextFuel();
        }

        if (burnRemaining <= 0 && (pending == null || (waterOk && energyOk))) {
            active = false;
            state = GeneratorState.NO_FUEL;
        } else if (!waterOk) {
            active = false;
            state = GeneratorState.NO_WATER;
        } else if (!energyOk) {
            active = false;
            state = GeneratorState.ENERGY_FULL;
        } else {
            active = true;
            state = GeneratorState.RUNNING;
        }

        if (active) {
            runGeneration();
            tickWater();
            tickMycelium(level);
        } else {
            lastExactOutput = 0.0D;
            if (wasActive && ServerConfig.get(ServerConfig.RESET_ON_STOP)) {
                resetChance();
            }
        }

        outputEnergy(level);
        trackExtraction();
        updateBlockState(level, blockState);
        updateComparators(level, blockState);
    }

    private void updateComparators(ServerLevel level, BlockState blockState) {
        int stored = energy.getAmountAsInt();
        int signal = stored <= 0 ? 0 : 1 + (int) Math.floor(14.0D * stored / Math.max(1, energy.getCapacityAsInt()));
        if (signal != lastComparatorSignal) {
            lastComparatorSignal = signal;
            level.updateNeighbourForOutputSignal(worldPosition, blockState.getBlock());
        }
    }

    private void startNextFuel() {
        ItemStack stack = inventory.getStackInSlot(GeneratorItemHandler.FUEL_SLOT);
        FuelDefinition definition = FuelRegistry.get(stack);
        if (definition == null) {
            return;
        }
        fuelItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        fuelEnergyMultiplier = definition.energyMultiplier();
        fuelWaterMultiplier = definition.waterMultiplier();
        fuelMyceliumBonus = definition.myceliumBonus();
        burnTotal = definition.burnTicks();
        burnRemaining = burnTotal;
        inventory.shrink(GeneratorItemHandler.FUEL_SLOT);
        setChanged();
    }

    // ------------------------------------------------------------------ energy

    /** Exact FE/t this generator produces burning its current fuel: the base rate times the fuel's multiplier. */
    public double computeOutput() {
        return Math.max(0.0D, ServerConfig.get(ServerConfig.BASE_GENERATION) * fuelEnergyMultiplier);
    }

    private void runGeneration() {
        double exact = computeOutput();
        lastExactOutput = exact;
        EnergyRounding rounding = ServerConfig.get(ServerConfig.ROUNDING);
        int produced = accumulator.produce(exact, rounding);
        energy.receiveInternal(produced);
        // Anything that doesn't fit is discarded (only possible when pauseWhenEnergyFull = false).
        burnRemaining--;
        setChanged();
    }

    private void outputEnergy(ServerLevel level) {
        int interval = ServerConfig.get(ServerConfig.OUTPUT_INTERVAL);
        if (++outputTimer < interval) {
            return;
        }
        outputTimer = 0;
        if (!ServerConfig.get(ServerConfig.AUTO_OUTPUT) || energy.getAmountAsInt() <= 0) {
            return;
        }
        long budgetLong = (long) ServerConfig.get(ServerConfig.OUTPUT_TRANSFER_RATE) * interval;
        int budget = (int) Math.min(Integer.MAX_VALUE, budgetLong);
        for (Direction direction : DIRECTIONS) {
            if (budget <= 0 || energy.getAmountAsInt() <= 0) {
                break;
            }
            BlockPos neighbourPos = worldPosition.relative(direction);
            if (!level.isLoaded(neighbourPos)) {
                continue;
            }
            EnergyHandler target = energyCache(level, direction).getCapability();
            if (target == null || target == energy) {
                continue;
            }
            int offer = Math.min(budget, energy.getAmountAsInt());
            int accepted;
            // The neighbour's insert is transactional; our buffer is only reduced once it has committed.
            try (Transaction transaction = Transaction.openRoot()) {
                accepted = Math.max(0, Math.min(offer, target.insert(offer, transaction)));
                transaction.commit();
            }
            if (accepted > 0) {
                energy.extractInternal(accepted);
                budget -= accepted;
            }
        }
    }

    private BlockCapabilityCache<EnergyHandler, Direction> energyCache(ServerLevel level, Direction direction) {
        int index = direction.ordinal();
        BlockCapabilityCache<EnergyHandler, Direction> cache = energyCaches[index];
        if (cache == null) {
            cache = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, worldPosition.relative(direction), direction.getOpposite());
            energyCaches[index] = cache;
        }
        return cache;
    }

    private void trackExtraction() {
        extractedWindow += energy.takeExtractedThisTick();
        if (++windowTicks >= 20) {
            extractionRate = extractedWindow / (double) windowTicks;
            extractedWindow = 0;
            windowTicks = 0;
        }
    }

    // ------------------------------------------------------------------ water

    /** mB the next consumption will drain (exact, before carrying the remainder). */
    private double waterPerInterval() {
        return waterPerInterval(fuelWaterMultiplier);
    }

    private double waterPerInterval(double waterMultiplier) {
        return Math.min(ServerConfig.get(ServerConfig.WATER_PER_INTERVAL) * waterMultiplier, Integer.MAX_VALUE);
    }

    private int nextWaterDrain(double waterMultiplier) {
        double exact = waterPerInterval(waterMultiplier) + waterRemainder;
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(exact));
    }

    private boolean hasWaterFor(int amount) {
        return !ServerConfig.get(ServerConfig.REQUIRE_WATER) || amount <= 0 || tank.getFluidAmount() >= amount;
    }

    private void tickWater() {
        if (++waterTimer < ServerConfig.get(ServerConfig.WATER_INTERVAL)) {
            return;
        }
        waterTimer = 0;
        double exact = waterPerInterval() + waterRemainder;
        int drain = (int) Math.min(Integer.MAX_VALUE, Math.floor(exact));
        waterRemainder = Math.max(0.0D, Math.min(0.999999D, exact - drain));
        if (drain > 0) {
            tank.drainInternal(drain);
        }
    }

    /**
     * Empties one fluid container from the bucket slot into the tank. The container is copied into a
     * temporary one-slot handler so the transfer API can turn it into its empty form; everything happens in
     * one transaction that is only committed if the emptied container fits into the output slot.
     */
    private void processFluidContainer() {
        ItemStack container = inventory.getStackInSlot(GeneratorItemHandler.CONTAINER_IN_SLOT);
        if (container.isEmpty()) {
            return;
        }
        ItemStacksResourceHandler scratch = new ItemStacksResourceHandler(1);
        scratch.set(0, ItemResource.of(container), 1);
        ResourceHandler<FluidResource> source = ItemAccess.forHandlerIndex(scratch, 0).getCapability(Capabilities.Fluid.ITEM);
        if (source == null) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = ResourceHandlerUtil.move(source, tank, resource -> true, Integer.MAX_VALUE, transaction);
            if (moved <= 0) {
                return;
            }
            ItemStack emptied = scratch.getResource(0).toStack(scratch.getAmountAsInt(0));
            if (!inventory.canOutput(emptied)) {
                return; // output slot full: abort, nothing changes
            }
            transaction.commit();
            inventory.shrink(GeneratorItemHandler.CONTAINER_IN_SLOT);
            inventory.addOutput(emptied);
        }
    }

    // ------------------------------------------------------------------ mycelium

    private void tickMycelium(ServerLevel level) {
        if (!ServerConfig.get(ServerConfig.MYCELIUM_ENABLED)) {
            attemptTimer = 0;
            return;
        }
        if (++attemptTimer < ServerConfig.get(ServerConfig.ATTEMPT_INTERVAL)) {
            return;
        }
        attemptTimer = 0;
        List<BlockPos> targets = MyceliumSpreadManager.findTargets(level, worldPosition);
        if (targets.isEmpty()) {
            return; // not an eligible attempt: the chance is unchanged
        }
        double current = getOwnChance();
        double max = ServerConfig.maxChance();
        if (MyceliumChance.succeeds(current, level.getRandom().nextDouble())) {
            int conversions = ServerConfig.get(ServerConfig.MAX_CONVERSIONS);
            MyceliumSpreadManager.ConversionResult outcome = MyceliumSpreadManager.convert(level, worldPosition, targets, conversions, level.getRandom());
            int converted = outcome.converted();
            hugeMushroomsThisSession += outcome.hugeMushrooms();
            if (converted > 0) {
                convertedThisSession += converted;
                if (ServerConfig.get(ServerConfig.RESET_ON_SUCCESS)) {
                    resetChance();
                }
            }
            // If every target was protected nothing changed, so the chance is left as is.
        } else {
            chance = MyceliumChance.afterFailure(current, ServerConfig.get(ServerConfig.CHANCE_INCREASE), fuelMyceliumBonus, max);
        }
        setChanged();
    }

    private void resetChance() {
        chance = ServerConfig.initialChance();
        setChanged();
    }

    public double getOwnChance() {
        return chance < 0 ? ServerConfig.initialChance() : chance;
    }

    public void setOwnChance(double value) {
        if (value != chance) {
            chance = value;
            setChanged();
        }
    }

    // ------------------------------------------------------------------ block state

    /**
     * The front glow (the block state's ACTIVE/LIGHT) stays on while the generator is running AND while it is
     * only paused because its energy buffer is full. Otherwise a full buffer that is being drained by a cable
     * toggles between running and paused every few ticks, which made the glow (and the block light) flicker.
     * The glow only turns off when the generator really stops: no fuel or no water.
     */
    private void updateBlockState(ServerLevel level, BlockState blockState) {
        boolean lit = isLit();
        int light = lit ? ServerConfig.get(ServerConfig.ACTIVE_LIGHT_LEVEL) : 0;
        if (blockState.getValue(MycelialGeneratorBlock.ACTIVE) != lit || blockState.getValue(MycelialGeneratorBlock.LIGHT) != light) {
            level.setBlock(worldPosition, blockState.setValue(MycelialGeneratorBlock.ACTIVE, lit)
                    .setValue(MycelialGeneratorBlock.LIGHT, light), Block.UPDATE_CLIENTS);
        }
    }

    /** Whether the front window glows: running, or fuelled and paused only by a full energy buffer. */
    public boolean isLit() {
        return state == GeneratorState.RUNNING || state == GeneratorState.ENERGY_FULL;
    }

    // ------------------------------------------------------------------ GUI status

    /** Builds the snapshot sent to players viewing this generator. */
    public GeneratorStatus createStatus() {
        double increase = Math.max(0.0D, ServerConfig.get(ServerConfig.CHANCE_INCREASE)) * fuelMyceliumBonus;
        FluidStack fluid = tank.getFluid();
        String fluidId = fluid.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
        double perTick = active ? lastExactOutput : (burnRemaining > 0 ? computeOutput() : 0.0D);
        return new GeneratorStatus(
                energy.getAmountAsInt(), energy.getCapacityAsInt(), perTick, active, extractionRate,
                fluidId, tank.getFluidAmount(), tank.getCapacity(), waterPerInterval(), ServerConfig.get(ServerConfig.WATER_INTERVAL),
                burnRemaining > 0 ? fuelItemId : "", burnRemaining, burnTotal,
                ServerConfig.get(ServerConfig.MYCELIUM_ENABLED), getOwnChance(), increase, attemptTimer,
                ServerConfig.get(ServerConfig.ATTEMPT_INTERVAL), convertedThisSession, ServerConfig.get(ServerConfig.TRACK_CONVERSIONS),
                state.ordinal(), hugeMushroomsThisSession);
    }

    // ------------------------------------------------------------------ accessors

    public boolean isActive() {
        return active;
    }

    public double getLastExactOutput() {
        return lastExactOutput;
    }

    public GeneratorState getState() {
        return state;
    }

    public GeneratorItemHandler getInventory() {
        return inventory;
    }

    public ResourceHandler<ItemResource> getAutomationItemHandler() {
        return automationItems;
    }

    public GeneratorFluidTank getTank() {
        return tank;
    }

    public ResourceHandler<FluidResource> getExternalFluidHandler() {
        return externalFluid;
    }

    public GeneratorEnergyStorage getEnergyStorage() {
        return energy;
    }

    public int getBurnRemaining() {
        return burnRemaining;
    }

    public int getConvertedThisSession() {
        return convertedThisSession;
    }

    public int getAttemptTimer() {
        return attemptTimer;
    }

    // ------------------------------------------------------------------ what cables and pipes see (every side)

    @Override
    public @Nullable EnergyHandler energyFor(@Nullable Direction side) {
        return energy;
    }

    @Override
    public @Nullable ResourceHandler<FluidResource> fluidFor(@Nullable Direction side) {
        return externalFluid;
    }

    @Override
    public @Nullable ResourceHandler<ItemResource> itemsFor(@Nullable Direction side) {
        return automationItems;
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.mycelial_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MycelialGeneratorMenu(containerId, playerInventory, this);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); // its modules: inventory, tank, energy
        output.store("EnergyRemainder", Codec.DOUBLE, accumulator.getRemainder());

        ValueOutput fuel = output.child("Fuel");
        fuel.putInt("Remaining", burnRemaining);
        fuel.putInt("Total", burnTotal);
        fuel.putString("Item", fuelItemId);
        fuel.store("EnergyMultiplier", Codec.DOUBLE, fuelEnergyMultiplier);
        fuel.store("WaterMultiplier", Codec.DOUBLE, fuelWaterMultiplier);
        fuel.store("MyceliumBonus", Codec.DOUBLE, fuelMyceliumBonus);

        output.putInt("WaterTimer", waterTimer);
        output.store("WaterRemainder", Codec.DOUBLE, waterRemainder);
        output.store("MyceliumChance", Codec.DOUBLE, getOwnChance());
        output.putInt("MyceliumTimer", attemptTimer);
        output.putBoolean("Active", active);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); // its modules
        accumulator.setRemainder(input.read("EnergyRemainder", Codec.DOUBLE).orElse(0.0D));

        ValueInput fuel = input.childOrEmpty("Fuel");
        burnRemaining = Math.max(0, fuel.getIntOr("Remaining", 0));
        burnTotal = Math.max(burnRemaining, fuel.getIntOr("Total", 0));
        fuelItemId = fuel.read("Item", Codec.STRING).orElse("");
        fuelEnergyMultiplier = clampNonNegative(fuel.read("EnergyMultiplier", Codec.DOUBLE).orElse(1.0D));
        fuelWaterMultiplier = clampNonNegative(fuel.read("WaterMultiplier", Codec.DOUBLE).orElse(1.0D));
        fuelMyceliumBonus = clampNonNegative(fuel.read("MyceliumBonus", Codec.DOUBLE).orElse(1.0D));

        waterTimer = Math.max(0, input.getIntOr("WaterTimer", 0));
        waterRemainder = Math.max(0.0D, Math.min(0.999999D, input.read("WaterRemainder", Codec.DOUBLE).orElse(0.0D)));
        chance = input.read("MyceliumChance", Codec.DOUBLE).map(value -> MyceliumChance.clamp(value, 1.0D)).orElse(-1.0D);
        attemptTimer = Math.max(0, input.getIntOr("MyceliumTimer", 0));
        active = input.getBooleanOr("Active", false);
    }

    private static double clampNonNegative(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0.0D;
    }
}
