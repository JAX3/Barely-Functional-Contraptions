package com.jax3.barelyfunctionalcontraptions.sheep.machine;

import java.util.function.BiPredicate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * A machine's energy buffer: a {@code MachineEnergy} for a machine that uses power, a {@code GeneratorEnergy} for one
 * that makes it. Saved under {@code energy}; offered to cables on the sides {@link #onSides} allows (every side unless
 * told otherwise).
 */
public final class EnergyModule<E extends SimpleEnergyHandler> implements MachineModule {
    private final E handler;
    private final String key;
    private BiPredicate<BlockState, Direction> sides = (state, side) -> true;

    private EnergyModule(E handler, String key) {
        this.handler = handler;
        this.key = key;
    }

    public static <E extends SimpleEnergyHandler> EnergyModule<E> of(E handler) {
        return new EnergyModule<>(handler, "energy");
    }

    /** Only these sides (given the machine's block state) connect to cables. */
    public EnergyModule<E> onSides(BiPredicate<BlockState, Direction> sides) {
        this.sides = sides;
        return this;
    }

    public E handler() {
        return handler;
    }

    /** What a cable on {@code side} sees: the buffer, or nothing on a side that doesn't connect. Null side: the buffer. */
    public @Nullable EnergyHandler forSide(BlockState state, @Nullable Direction side) {
        return side == null || sides.test(state, side) ? handler : null;
    }

    @Override
    public void save(ValueOutput output) {
        handler.serialize(output.child(key));
    }

    @Override
    public void load(ValueInput input) {
        input.child(key).ifPresent(handler::deserialize);
    }
}
