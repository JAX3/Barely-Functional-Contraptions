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
                                           java.util.function.Predicate<net.minecraft.world.level.block.entity.BlockEntity> storage,
                                           java.util.function.Function<T, net.minecraft.resources.Identifier> idOf,
                                           java.util.function.Function<net.minecraft.world.item.ItemStack, java.util.Optional<net.minecraft.resources.Identifier>> filterEntry,
                                           java.util.function.Function<net.minecraft.resources.Identifier, net.minecraft.network.chat.Component> nameOf) {
    public static final PipeKind<FluidResource> FLUID = new PipeKind<>("fluid", FluidResource.EMPTY, 0, Capabilities.Fluid.BLOCK,
            () -> ModConfigs.getInt(ModConfigs.SERVER.pipeTransferRate), () -> ModConfigs.getInt(ModConfigs.SERVER.pipeMaxNetworkSize),
            be -> be instanceof FluidTankBlockEntity,
            fluid -> net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()),
            PipeKind::fluidIn,
            id -> net.minecraft.core.registries.BuiltInRegistries.FLUID.getOptional(id)
                    .map(f -> FluidResource.of(f).getHoverName()).orElse(net.minecraft.network.chat.Component.literal(id.toString())));
    public static final PipeKind<ItemResource> ITEM = new PipeKind<>("item", ItemResource.EMPTY, 64, Capabilities.Item.BLOCK,
            () -> ModConfigs.getInt(ModConfigs.SERVER.itemPipeTransferRate), () -> ModConfigs.getInt(ModConfigs.SERVER.itemPipeMaxNetworkSize),
            be -> false,
            item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()),
            stack -> stack.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())),
            id -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id)
                    .map(i -> new net.minecraft.world.item.ItemStack(i).getHoverName()).orElse(net.minecraft.network.chat.Component.literal(id.toString())));

    /** The icon for a filter entry in the filter grid: the item itself, or for a fluid its bucket. */
    public net.minecraft.world.item.ItemStack iconOf(net.minecraft.resources.Identifier id) {
        if (this == ITEM) {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).map(net.minecraft.world.item.ItemStack::new)
                    .orElse(net.minecraft.world.item.ItemStack.EMPTY);
        }
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.getOptional(id)
                .map(f -> f.getBucket() == net.minecraft.world.item.Items.AIR ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET) : new net.minecraft.world.item.ItemStack(f.getBucket()))
                .orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    /** The fluid a bucket (or any fluid container) holds, as a filter entry. */
    private static java.util.Optional<net.minecraft.resources.Identifier> fluidIn(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return java.util.Optional.empty();
        ResourceHandler<FluidResource> handler = Capabilities.Fluid.ITEM.getCapability(stack, net.neoforged.neoforge.transfer.access.ItemAccess.forStack(stack));
        if (handler == null) return java.util.Optional.empty();
        for (int i = 0; i < handler.size(); i++) {
            FluidResource fluid = handler.getResource(i);
            if (!fluid.isEmpty()) return java.util.Optional.of(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
        }
        return java.util.Optional.empty();
    }

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
