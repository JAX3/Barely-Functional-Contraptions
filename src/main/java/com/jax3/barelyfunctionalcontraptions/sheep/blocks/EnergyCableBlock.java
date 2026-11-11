package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EnergyCableBlockEntity;
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
 * Energy Cable. Cables join every cable they touch into one network, and plug into any block with a Forge Energy
 * connection (generators, the Hume Energy Cube, machines, other mods' blocks). Energy that a generator or cube pushes
 * into the network goes straight out to the blocks plugged into it; the network also pulls from generators and cubes
 * that don't push on their own. See {@link com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CableNetwork}.
 */
public class EnergyCableBlock extends ConduitBlock {
    public EnergyCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canPlugInto(Level level, BlockPos other, Direction face) {
        return level.getCapability(Capabilities.Energy.BLOCK, other, face) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.ENERGY_CABLE.get(), (l, pos, s, be) -> be.serverTick(serverLevel));
    }
}
