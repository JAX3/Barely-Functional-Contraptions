package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ItemPipeBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;

/**
 * Item Pipe. Works like the Energy Cable and Fluid Pipe, for items: pipes join every pipe they touch into one network
 * and plug into any block with an item connection (chests, hoppers, the Mechanical Separator, the Automatic Shears,
 * other mods' inventories). Items pushed into the network go straight out to the blocks plugged into it, and the
 * network pulls from inventories that only give items out, such as the Automatic Shears and the separator's outputs.
 * See {@link com.jax3.barelyfunctionalcontraptions.sheep.blockentity.PipeNetwork}.
 */
public class ItemPipeBlock extends ConduitBlock {
    public ItemPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canPlugInto(Level level, BlockPos other, Direction face) {
        return level.getCapability(Capabilities.Item.BLOCK, other, face) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.ITEM_PIPE.get(), (l, pos, s, be) -> be.serverTick(serverLevel));
    }
}
