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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * The Glow Panel: makes Forge Energy from the light falling on its top face.
 *
 * <ul>
 *     <li><b>Sun:</b> with open sky above, {@code sunRate} FE/t scaled by the sky light the daylight detector sees
 *     (lower at dawn and dusk, in rain and in shade, nothing at night).</li>
 *     <li><b>Glow:</b> {@code lightRate} FE/t scaled by the block light on its top (a solid block that doesn't glow
 *     shades it). A <b>Glow Ink sheep</b> with its fleece on standing on it, or on the panels round it (the 3x3 columns
 *     centred on it, up to {@link #SHEEP_REACH} blocks above its top), gives the full rate: its glow soaks into the
 *     cells. This keeps it going at night and underground.</li>
 * </ul>
 * It uses whichever of the two gives more, stores up to {@code capacity} FE, and pushes it into the blocks beside and
 * below it (never upwards, where its light comes from); cables and machines can also pull from it.
 */
public class GlowPanelBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame {
    /** Where the panel's power is coming from, for the readouts. */
    public enum Source { NONE, SUN, GLOW }

    /** Its one module, a power buffer: nothing can be put into the panel from outside; it only fills itself. */
    private final GeneratorEnergy energy = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.EnergyModule.of(new GeneratorEnergy(10_000, this::setChanged))).handler();
    private int generating;
    private Source source = Source.NONE;
    /** How far above its top a Glow Ink sheep still lights it fully, in blocks. */
    public static final int SHEEP_REACH = 2;
    /** Ticks between looks for a Glow Ink sheep (sheep move slowly; panels are many). */
    private static final int SHEEP_SCAN = 10;
    /** Whether a Glow Ink sheep was over it at the last look. */
    private boolean sheepLit;

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

    /** Whether it has a Glow Ink sheep over it (as of its last look). */
    public boolean sheepLit() {
        return sheepLit;
    }

    /** Whether a grown Glow Ink sheep with its fleece on stands in the 3x3 columns over the panel at {@code pos}. */
    public static boolean glowSheepOver(Level level, BlockPos pos) {
        var area = new net.minecraft.world.phys.AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 1 + SHEEP_REACH, pos.getZ() + 2);
        return !level.getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class, area,
                s -> area.contains(s.position()) && com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepFeatureEvents.has(s,
                        com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature.GLOWING)).isEmpty();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GlowPanelBlockEntity be) {
        int capacity = ModConfigs.getInt(ModConfigs.SERVER.glowPanelCapacity);
        be.energy.resize(capacity);
        BlockPos above = pos.above();
        // A solid block on top (stone, a machine...) shades the panel completely, unless it gives off light itself:
        // glowstone or a sea lantern sitting right on the panel powers it with its own light.
        BlockState top = level.getBlockState(above);
        boolean solid = top.getLightDampening() >= 15;
        if (Math.floorMod(level.getGameTime() + pos.asLong(), SHEEP_SCAN) == 0) be.sheepLit = !solid && glowSheepOver(level, pos);
        int sun = solid ? 0 : sunPower(level, above);
        int glow = solid ? glowPower(top.getLightEmission()) : Math.max(glowPower(level, above), glowPower(top.getLightEmission()));
        if (be.sheepLit) glow = glowPower(15); // a Glow Ink sheep over it: its full glow rate
        be.generating = Math.max(sun, glow);
        be.source = be.generating <= 0 ? Source.NONE : sun >= glow ? Source.SUN : Source.GLOW;
        be.energy.generate(be.generating);
        be.push(level, pos);
        int brightness = GlowPanelBlock.glowFor(be.generating);
        if (state.getValue(GlowPanelBlock.GLOW) != brightness) level.setBlock(pos, state.setValue(GlowPanelBlock.GLOW, brightness), Block.UPDATE_CLIENTS);
    }

    private void push(ServerLevel level, BlockPos pos) {
        energy.pushToNeighbours(level, pos, ModConfigs.getInt(ModConfigs.SERVER.glowPanelOutput), (dir, target) -> dir == Direction.UP || level.getBlockEntity(target) instanceof GlowPanelBlockEntity); // not up, and panels don't charge each other
    }
}
