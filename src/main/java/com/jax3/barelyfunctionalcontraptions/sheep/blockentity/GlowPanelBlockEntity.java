package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.GlowPanelBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Glow Panel: makes Forge Energy from the light falling on its top face.
 *
 * <ul>
 *     <li><b>Sun:</b> with open sky above, {@code sunRate} FE/t scaled by the sky light the daylight detector sees
 *     (lower at dawn and dusk, in rain and in shade, nothing at night).</li>
 *     <li><b>Glow:</b> {@code lightRate} FE/t scaled by the block light on its top: a Glow Lamp on it or above it gives
 *     the full rate (a solid block that doesn't glow shades it), and a glowing Glow Ink sheep standing on it (light 12) most of it. This keeps it going at night and
 *     underground.</li>
 * </ul>
 * It uses whichever of the two gives more, stores up to {@code capacity} FE, and pushes it into the blocks beside and
 * below it (never upwards, where its light comes from); cables and machines can also pull from it.
 */
public class GlowPanelBlockEntity extends BlockEntity {
    /** Where the panel's power is coming from, for the readouts. */
    public enum Source { NONE, SUN, GLOW }

    /** Nothing can be put into the panel from outside; it only fills itself. */
    private final PanelEnergy energy = new PanelEnergy();
    private int generating;
    private Source source = Source.NONE;

    public GlowPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GLOW_PANEL.get(), pos, state);
    }

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    /** FE/t the panel made on its last tick. */
    public int generating() {
        return generating;
    }

    public Source source() {
        return source;
    }

    /**
     * FE/t from the sun at {@code above}: the strength a daylight detector there would read (sky light, weather and
     * the sun's height, so nothing at night), as a share of {@code sunRate}. 0 without open sky.
     */
    public static int sunPower(Level level, BlockPos above) {
        if (!level.dimensionType().hasSkyLight() || !level.canSeeSky(above)) return 0;
        int sun = level.getEffectiveSkyBrightness(above);
        if (sun > 0) {
            float angle = level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, above) * Mth.DEG_TO_RAD;
            float offset = angle < Mth.PI ? 0.0F : Mth.TWO_PI;
            angle += (offset - angle) * 0.2F;
            sun = Math.round(sun * Mth.cos(angle));
        }
        sun = Mth.clamp(sun, 0, 15);
        return ModConfigs.getInt(ModConfigs.SERVER.glowPanelSunRate) * sun / 15;
    }

    /** FE/t from the block light at {@code above} (lamps, glowstone, a glowing sheep...). */
    public static int glowPower(Level level, BlockPos above) {
        return glowPower(level.getBrightness(LightLayer.BLOCK, above));
    }

    /** FE/t from block light {@code light} (0-15). */
    public static int glowPower(int light) {
        return ModConfigs.getInt(ModConfigs.SERVER.glowPanelLightRate) * Math.clamp(light, 0, 15) / 15;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GlowPanelBlockEntity be) {
        int capacity = ModConfigs.getInt(ModConfigs.SERVER.glowPanelCapacity);
        be.energy.resize(capacity);
        BlockPos above = pos.above();
        // A solid block on top (stone, a machine...) shades the panel completely, unless it gives off light itself:
        // a Glow Lamp (or glowstone, a sea lantern...) sitting right on the panel powers it with its own light.
        BlockState top = level.getBlockState(above);
        boolean solid = top.getLightDampening() >= 15;
        int sun = solid ? 0 : sunPower(level, above);
        int glow = solid ? glowPower(top.getLightEmission()) : Math.max(glowPower(level, above), glowPower(top.getLightEmission()));
        be.generating = Math.max(sun, glow);
        be.source = be.generating <= 0 ? Source.NONE : sun >= glow ? Source.SUN : Source.GLOW;
        be.energy.generate(be.generating);
        be.push(level, pos);
        int brightness = GlowPanelBlock.glowFor(be.generating);
        if (state.getValue(GlowPanelBlock.GLOW) != brightness) level.setBlock(pos, state.setValue(GlowPanelBlock.GLOW, brightness), Block.UPDATE_CLIENTS);
    }

    private void push(ServerLevel level, BlockPos pos) {
        int budget = Math.min(ModConfigs.getInt(ModConfigs.SERVER.glowPanelOutput), energy.getAmountAsInt());
        for (Direction dir : Direction.values()) {
            if (budget <= 0) return;
            if (dir == Direction.UP) continue;
            BlockPos target = pos.relative(dir);
            if (level.getBlockEntity(target) instanceof GlowPanelBlockEntity) continue; // panels don't charge each other
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, target, dir.getOpposite());
            if (handler == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                int moved = handler.insert(budget, tx);
                if (moved > 0 && energy.extract(moved, tx) == moved) {
                    tx.commit();
                    budget -= moved;
                }
            }
        }
    }

    private final class PanelEnergy extends SimpleEnergyHandler {
        PanelEnergy() {
            super(10_000, 0, Integer.MAX_VALUE);
        }

        void generate(int amount) {
            int previous = energy;
            energy = Math.min(capacity, energy + Math.max(0, amount));
            if (energy != previous) onEnergyChanged(previous);
        }

        void resize(int newCapacity) {
            if (capacity == newCapacity) return;
            int previous = energy;
            capacity = newCapacity;
            energy = Math.min(energy, capacity);
            if (energy != previous) onEnergyChanged(previous);
        }

        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("energy").ifPresent(energy::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
    }
}
