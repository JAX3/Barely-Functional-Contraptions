package com.jax3.barelyfunctionalcontraptions.sheep.machine;

import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.WaterTank;
import java.util.function.BiPredicate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A machine's water tank. Saved under {@code tank}; offered to pipes on the sides {@link #onSides} allows (all by default). */
public final class FluidModule implements MachineModule {
    private final WaterTank tank;
    private final String key;
    private BiPredicate<BlockState, Direction> sides = (state, side) -> true;

    private FluidModule(WaterTank tank, String key) {
        this.tank = tank;
        this.key = key;
    }

    public static FluidModule of(WaterTank tank) {
        return new FluidModule(tank, "tank");
    }

    public FluidModule onSides(BiPredicate<BlockState, Direction> sides) {
        this.sides = sides;
        return this;
    }

    public WaterTank tank() {
        return tank;
    }

    public @Nullable WaterTank forSide(BlockState state, @Nullable Direction side) {
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
