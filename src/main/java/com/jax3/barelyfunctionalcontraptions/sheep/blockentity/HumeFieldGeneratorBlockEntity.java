package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.HumeFieldGeneratorBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Work in progress: the Hume Field Generator's workings, held by its bottom north-west block.
 *
 * <ul>
 *     <li>Every tick its field makes a <b>random</b> amount of FE between {@code [hume_field_generator] minPerTick} and
 *     {@code maxPerTick}.</li>
 *     <li>It has <b>no energy buffer</b>: that tick's FE goes straight into whatever touches any of its twelve blocks
 *     (cables, cubes, machines), and whatever they don't take is lost. Nothing can be pulled from it.</li>
 *     <li>It offers the FE to its neighbours in turn, starting with a different one each tick, so no one of them always
 *     gets first pick.</li>
 * </ul>
 *
 * It's on the machine frame with no modules switched on: nothing to save, store or connect.
 */
public class HumeFieldGeneratorBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame {
    /** Each block touching the machine, relative to this block, and the face of it the machine touches. */
    private static final List<Neighbour> NEIGHBOURS = neighbours();

    private record Neighbour(BlockPos offset, Direction side) {}

    private int generated;
    private int delivered;
    private long totalDelivered;
    private int start;

    public HumeFieldGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HUME_FIELD_GENERATOR.get(), pos, state);
    }

    private static List<Neighbour> neighbours() {
        List<Neighbour> list = new ArrayList<>();
        for (int part = 0; part < 4; part++) {
            for (int layer = 0; layer < HumeFieldGeneratorBlock.LAYERS; layer++) {
                BlockPos inside = new BlockPos(part & 1, layer, part >> 1);
                for (Direction dir : Direction.values()) {
                    BlockPos out = inside.relative(dir);
                    boolean ownBlock = out.getX() >= 0 && out.getX() <= 1 && out.getZ() >= 0 && out.getZ() <= 1
                            && out.getY() >= 0 && out.getY() < HumeFieldGeneratorBlock.LAYERS;
                    if (!ownBlock) list.add(new Neighbour(out, dir.getOpposite()));
                }
            }
        }
        return List.copyOf(list);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, HumeFieldGeneratorBlockEntity be) {
        be.generated = roll(level);
        be.delivered = be.generated > 0 ? be.offer(level, pos, be.generated) : 0;
        be.totalDelivered += be.delivered;
    }

    /** This tick's FE: a random amount in the configured range (either way round). */
    private static int roll(ServerLevel level) {
        int a = ModConfigs.getInt(ModConfigs.SERVER.humeFieldMinPerTick), b = ModConfigs.getInt(ModConfigs.SERVER.humeFieldMaxPerTick);
        return level.getRandom().nextIntBetweenInclusive(Math.min(a, b), Math.max(a, b));
    }

    /** Offers {@code amount} FE to the blocks touching the machine in turn; how much they took (the rest is lost). */
    private int offer(ServerLevel level, BlockPos origin, int amount) {
        int left = amount;
        int count = NEIGHBOURS.size();
        start = (start + 1) % count;
        for (int i = 0; i < count && left > 0; i++) {
            Neighbour n = NEIGHBOURS.get((start + i) % count);
            BlockPos at = origin.offset(n.offset());
            if (!level.isLoaded(at)) continue;
            EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, at, n.side());
            if (target == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int taken = target.insert(left, tx);
                tx.commit();
                left -= Math.max(0, taken);
            }
        }
        return amount - left;
    }

    // ------------------------------------------------------------------ readouts

    /** FE its field made on the last tick. */
    public int generated() {
        return generated;
    }

    /** FE its neighbours took on the last tick (the rest of {@link #generated} was lost). */
    public int delivered() {
        return delivered;
    }

    /** FE its neighbours have taken since it was loaded. */
    public long totalDelivered() {
        return totalDelivered;
    }
}
