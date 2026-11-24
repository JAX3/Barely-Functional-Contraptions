package com.jax3.barelyfunctionalcontraptions.sheep.machine;

import java.util.function.BiPredicate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/** A machine's tank (a {@code WaterTank}, the reservoir's...). Saved under {@code tank}; offered to pipes on the sides {@link #onSides} allows (all by default). */
public final class FluidModule<T extends FluidStacksResourceHandler> implements MachineModule {
    private final T tank;
    private final String key;
    private BiPredicate<BlockState, Direction> sides = (state, side) -> true;

    private FluidModule(T tank, String key) {
        this.tank = tank;
        this.key = key;
    }

    public static <T extends FluidStacksResourceHandler> FluidModule<T> of(T tank) {
        return new FluidModule<>(tank, "tank");
    }

    public FluidModule<T> onSides(BiPredicate<BlockState, Direction> sides) {
        this.sides = sides;
        return this;
    }

    public T tank() {
        return tank;
    }

    public @Nullable T forSide(BlockState state, @Nullable Direction side) {
        return side == null || sides.test(state, side) ? tank : null;
    }

    @Override
    public void save(ValueOutput output) {
        tank.serialize(output.child(key));
    }

    @Override
    public void load(ValueInput input) {
        input.child(key).ifPresent(tank::deserialize);
    }
}
