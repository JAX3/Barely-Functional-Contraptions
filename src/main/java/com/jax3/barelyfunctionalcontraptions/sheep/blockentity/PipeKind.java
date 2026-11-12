package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import org.jspecify.annotations.Nullable;

/** What a kind of pipe carries: the capability it plugs into and its config limits. */
public record PipeKind<T extends Resource>(String name, T empty, long slotCapacity, BlockCapability<ResourceHandler<T>, @Nullable Direction> capability,
                                           Supplier<Integer> rate, Supplier<Integer> maxSize,
                                           java.util.function.Predicate<net.minecraft.world.level.block.entity.BlockEntity> storage) {
    public static final PipeKind<FluidResource> FLUID = new PipeKind<>("fluid", FluidResource.EMPTY, 0, Capabilities.Fluid.BLOCK,
            () -> ModConfigs.getInt(ModConfigs.SERVER.pipeTransferRate), () -> ModConfigs.getInt(ModConfigs.SERVER.pipeMaxNetworkSize),
            be -> be instanceof FluidTankBlockEntity);
    public static final PipeKind<ItemResource> ITEM = new PipeKind<>("item", ItemResource.EMPTY, 64, Capabilities.Item.BLOCK,
            () -> ModConfigs.getInt(ModConfigs.SERVER.itemPipeTransferRate), () -> ModConfigs.getInt(ModConfigs.SERVER.itemPipeMaxNetworkSize),
            be -> false);

    /** Most a network of this kind moves per tick: mB for fluids, items for items. */
    public int transferRate() {
        return rate.get();
    }

    /** Storage blocks (Fluid Tanks) are filled after machines, and never from other storage. */
    public boolean isStorage(net.minecraft.world.level.block.entity.@Nullable BlockEntity be) {
        return be != null && storage.test(be);
    }

    public int maxNetworkSize() {
        return maxSize.get();
    }
}
