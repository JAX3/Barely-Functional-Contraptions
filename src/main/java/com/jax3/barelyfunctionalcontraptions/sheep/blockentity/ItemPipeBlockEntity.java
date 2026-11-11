package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** One Item Pipe: carries items through its {@link PipeNetwork}. */
public class ItemPipeBlockEntity extends PipeBlockEntity<ItemResource> {
    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE.get(), pos, state);
    }

    @Override
    public PipeKind<ItemResource> kind() {
        return PipeKind.ITEM;
    }
}
