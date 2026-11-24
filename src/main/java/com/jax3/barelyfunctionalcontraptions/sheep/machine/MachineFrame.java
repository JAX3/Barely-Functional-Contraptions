package com.jax3.barelyfunctionalcontraptions.sheep.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * The base of every machine: a block entity that carries the {@link MachineModule}s the machine switches on, an
 * {@link EnergyModule}, a {@link FluidModule}, upgrade slots ({@code MachineUpgrades}) and a {@link MachineInventory},
 * each only if it needs it. A machine installs them in its field initialisers with {@link #install}; the frame then
 * <ul>
 *     <li>saves and loads each of them with the machine,</li>
 *     <li>keeps what they keep on the machine's item when it's broken or picked up, and gives it back when it's placed,</li>
 *     <li>adds what clients need to the machine's update tag (and the machine's own with {@link #writeClientData}),</li>
 *     <li>drops what they hold when the machine is broken,</li>
 *     <li>offers its energy, fluid and items to cables, pipes and hoppers ({@link #registerCapabilities}).</li>
 * </ul>
 * The machine itself only does its job.
 */
public abstract class MachineFrame extends BlockEntity {
    private final List<MachineModule> modules = new ArrayList<>();
    private @Nullable EnergyModule<?> energyModule;
    private @Nullable FluidModule<?> fluidModule;
    private @Nullable MachineInventory inventoryModule;

    protected MachineFrame(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Switches a module on for this machine (in a field initialiser); returns it. */
    protected final <M extends MachineModule> M install(M module) {
        modules.add(module);
        if (module instanceof EnergyModule<?> energy) energyModule = energy;
        if (module instanceof FluidModule<?> fluid) fluidModule = fluid;
        if (module instanceof MachineInventory inventory) inventoryModule = inventory;
        return module;
    }

    /** Its modules, in the order installed. */
    public final List<MachineModule> modules() {
        return List.copyOf(modules);
    }

    // ------------------------------------------------------------------ cables, pipes and hoppers

    /** What an energy cable on {@code side} sees, or null without an energy module (or on a side it doesn't offer). */
    public @Nullable EnergyHandler energyFor(@Nullable Direction side) {
        return energyModule == null ? null : energyModule.forSide(getBlockState(), side);
    }

    /** What a fluid pipe on {@code side} sees, or null without a fluid module. */
    public @Nullable ResourceHandler<FluidResource> fluidFor(@Nullable Direction side) {
        return fluidModule == null ? null : fluidModule.forSide(getBlockState(), side);
    }

    /** What an item pipe or hopper on {@code side} sees: its inventory and slot rules, or null without one. */
    public @Nullable ResourceHandler<ItemResource> itemsFor(@Nullable Direction side) {
        return inventoryModule != null && this instanceof WorldlyContainer container
                ? new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(container, side) : null;
    }

    /** Offers each of {@code types}' energy, fluid and items through the frame (machines without a module offer none). */
    @SafeVarargs
    public static void registerCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<? extends MachineFrame>... types) {
        for (BlockEntityType<? extends MachineFrame> type : types) {
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, MachineFrame::energyFor);
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, MachineFrame::fluidFor);
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, MachineFrame::itemsFor);
        }
    }

    // ------------------------------------------------------------------ saving, the item, clients, breaking

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (MachineModule module : modules) {
            module.load(input);
            module.readSync(input);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (MachineModule module : modules) module.save(output);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        for (MachineModule module : modules) module.collectComponents(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        for (MachineModule module : modules) module.applyComponents(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        for (MachineModule module : modules) module.removeComponents(output);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        for (MachineModule module : modules) module.writeSync(tag);
        writeClientData(tag);
        return tag;
    }

    /** The machine's own part of its update tag (its area setting, the sheep it uses...), read back in its loadAdditional. */
    protected void writeClientData(CompoundTag tag) {}

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) for (MachineModule module : modules) module.dropContents(level, pos);
    }
}
