package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ChillerBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * The Snow Sheep Chiller: freezes water into Ice with the cold of the Snow sheep around it, the Blaze Sheep Furnace's
 * icy opposite. It needs no power.
 *
 * <ul>
 *     <li><b>Cold:</b> every half second it counts the grown Snow sheep (any resource in {@code [chiller] coldSources})
 *     with their fleece on and their feet in {@link #chillingArea}, the furnace's 5x5 area. A sheared sheep or a lamb
 *     gives nothing until its fleece grows back.</li>
 *     <li><b>Speed:</b> one Snow sheep freezes a block of Ice every {@code ticksPerIce} (10 s); each one after the first
 *     adds a little less, like the furnace's heat, up to {@code maxColdSources} (4 sheep: about every 3 s).</li>
 *     <li><b>Water:</b> each Ice takes {@code waterPerIce} mB from its tank, filled by buckets, pipes or a Fluid
 *     Reservoir beside it. The Ice goes in its output slot, which hoppers and pipes can empty from any side.</li>
 * </ul>
 * Its comparator signal reads how full the output slot is.
 */
public class ChillerBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame
        implements com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable, com.jax3.barelyfunctionalcontraptions.sheep.machine.InventoryHolder, AreaDisplay, net.minecraft.world.MenuProvider {
    /** Its two upgrade cards (see {@link com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades}). */
    public static final int SCAN_INTERVAL = 10;
    public static final int AREA_COLOR = 0xFF7FD8FF;
    /** Values synced to the window ({@link com.jax3.barelyfunctionalcontraptions.sheep.menu.ChillerMenu}). */
    public static final int DATA_COUNT = 10;
    public static final FluidResource WATER = WaterTank.WATER;

    // Its modules: water in on every side, two upgrade slots (Range, Muffler), and the Ice it makes (hoppers take it out).
    private final WaterTank tank = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.FluidModule.of(new WaterTank(8_000, this::setChanged))).tank();
    private final com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades = install(new com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades(this));
    private final com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory.builder()
            .output("output")
            .build());
    /** Progress towards the next Ice, in hundredths of a tick at one sheep's speed. */
    private int progress;
    /** Speed from the cold, in hundredths (100 = one Snow sheep), set on each scan. */
    private int cold;
    private int[] chillers = new int[0];
    private int scanCooldown;
    private boolean showArea;
    private MachineStatus status = MachineStatus.IDLE;
    private int lastSignal;
    /** Ticks its Snow sheep have chilled it with the Ice slot full, towards frosting over. */
    private int fullTicks;
    /** This chiller's own frost time in seconds, for tests ({@link com.jax3.barelyfunctionalcontraptions.sheep.machine.Quirks#NONE}: the config's). */
    private double frostSecondsOverride = com.jax3.barelyfunctionalcontraptions.sheep.machine.Quirks.NONE;

    public void setFrostSecondsOverride(int seconds) {
        frostSecondsOverride = seconds;
    }

    /** Ticks left to thaw, while frosted over (0: not frosted). */
    private int thawTicks;

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> tank.getAmount();
                case 1 -> tank.capacity();
                case 2 -> Math.min(0xFFFF, progress / 100);
                case 3 -> Math.min(0xFFFF, ticksPerIce());
                case 4 -> cold;
                case 5 -> maxCold();
                case 6 -> chillers.length;
                case 7 -> maxSources();
                case 8 -> status.ordinal();
                case 9 -> showArea ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ChillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHILLER.get(), pos, state);
    }

    /** The chilling area: the same as the furnace's, (2 radius + 1) square columns at its level and the one above. */
    public static AABB chillingArea(BlockPos pos) {
        return chillingArea(pos, 0);
    }

    /** {@link #chillingArea(BlockPos)} grown by {@code extra} blocks on every side (Range upgrade cards). */
    public static AABB chillingArea(BlockPos pos, int extra) {
        return BlazeFurnaceBlockEntity.heatingArea(pos, ModConfigs.getInt(ModConfigs.SERVER.chillerRadius) + extra);
    }

    public static int ticksPerIce() {
        return ModConfigs.getInt(ModConfigs.SERVER.chillerTicksPerIce);
    }

    public static int waterPerIce() {
        return ModConfigs.getInt(ModConfigs.SERVER.chillerWaterPerIce);
    }

    public static int maxSources() {
        return ModConfigs.getInt(ModConfigs.SERVER.chillerMaxSources);
    }

    /** Speed (hundredths) from {@code count} Snow sheep: the first full, each after it falloff less. */
    public static int coldFrom(int count) {
        return BlazeFurnaceBlockEntity.combine(Collections.nCopies(count, 100.0), maxSources(), ModConfigs.getDouble(ModConfigs.SERVER.chillerFalloff));
    }

    public static int maxCold() {
        return coldFrom(maxSources());
    }

    /** Whether {@code sheep} is a cold source right now: grown, fleece on, infused with a cold resource. */
    public static boolean isColdSource(Sheep sheep) {
        if (!sheep.isAlive() || sheep.isSheared() || sheep.isBaby()) return false;
        List<? extends String> sources = ModConfigs.SERVER.chillerColdSources.get();
        return SheepInfusionEvents.getInfusion(sheep).resource().map(Identifier::toString).map(sources::contains).orElse(false);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ChillerBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        tank.resize(ModConfigs.getInt(ModConfigs.SERVER.chillerTankCapacity));
        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            scan(level, pos, state);
        }
        ItemStack output = inventory.get(0);
        boolean room = output.isEmpty() || (output.is(Items.ICE) && output.getCount() < output.getMaxStackSize());
        if (thawTicks > 0) {
            // Frosted over: it thaws only while there's room for Ice, at once if a Blaze sheep warms it.
            if (room && (--thawTicks <= 0 || warmed(level, pos))) {
                thawTicks = 0;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.4, 0.2, 0.4, 0.0);
                if (!muffled()) level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.6F);
            }
            status = thawTicks > 0 ? MachineStatus.FROSTED : MachineStatus.IDLE;
            if (level.getGameTime() % 10 == 0) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.4, 0.2, 0.4, 0.0);
            }
            setChanged();
        } else if (cold <= 0) {
            status = MachineStatus.IDLE;
            fullTicks = 0;
        } else if (!room) {
            int frostAfter = (frostSecondsOverride >= 0 ? (int) frostSecondsOverride
                    : com.jax3.barelyfunctionalcontraptions.sheep.machine.Quirks.off(frostSecondsOverride) ? 0 : ModConfigs.getInt(ModConfigs.SERVER.chillerFrostSeconds)) * 20;
            if (frostAfter > 0 && ++fullTicks >= frostAfter) frostOver(level, pos);
            status = MachineStatus.OUTPUT_FULL;
        } else if (tank.getAmount() < waterPerIce()) {
            status = MachineStatus.NO_ENERGY; // shown as "No water"
        } else {
            status = MachineStatus.PROCESSING;
            fullTicks = 0;
            progress += cold;
            if (progress >= ticksPerIce() * 100) {
                progress = 0;
                tank.drain(waterPerIce());
                if (output.isEmpty()) inventory.set(0, new ItemStack(Items.ICE));
                else output.grow(1);
                if (!muffled()) level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GLASS_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.4F);
            }
            setChanged();
        }
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        boolean active = status == MachineStatus.PROCESSING;
        if (state.getValue(ChillerBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(ChillerBlock.ACTIVE, active), Block.UPDATE_ALL);
    }

    /** Left full too long with its Snow sheep still chilling it, it frosts over: it stops until it thaws. */
    private void frostOver(ServerLevel level, BlockPos pos) {
        fullTicks = 0;
        thawTicks = ModConfigs.getInt(ModConfigs.SERVER.chillerThawSeconds) * 20;
        progress = 0;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.5, 0.4, 0.5, 0.02);
        if (!muffled()) level.playSound(null, pos, net.minecraft.sounds.SoundEvents.POWDER_SNOW_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
        setChanged();
    }

    /** Whether a heat source (a Blaze sheep, by default) stands in its area, to thaw it at once. */
    private boolean warmed(ServerLevel level, BlockPos pos) {
        AABB area = chillingArea(pos, rangeBonus());
        return !level.getEntities((net.minecraft.world.entity.Entity) null, area,
                e -> area.contains(e.position()) && com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeatManager.heatOf(e).isPresent()).isEmpty();
    }

    public boolean isFrosted() {
        return thawTicks > 0;
    }

    /** Seconds left to thaw (counting only while there's room for Ice). */
    public int thawSecondsLeft() {
        return (thawTicks + 19) / 20;
    }

    /** Counts the Snow sheep in the area. */
    public void scan(ServerLevel level, BlockPos pos, BlockState state) {
        AABB area = chillingArea(pos, rangeBonus());
        List<Sheep> found = level.getEntitiesOfClass(Sheep.class, area, s -> area.contains(s.position()) && isColdSource(s));
        int[] ids = found.stream().limit(maxSources()).mapToInt(Sheep::getId).sorted().toArray();
        // Keep its Snow sheep close: call strays back while it has room for more.
        SheepAttraction.claim(found, level.getGameTime());
        // With a Range card it calls them back only every other second, so they wander a bit more.
        if (rangeBonus() == 0 || level.getGameTime() / 20 % 2 == 0) {
            SheepAttraction.attract(level, area, ChillerBlockEntity::isColdSource, maxSources() - found.size(), level.getGameTime());
        }
        cold = coldFrom(ids.length);
        if (!Arrays.equals(ids, chillers)) {
            chillers = ids;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ accessors

    public WaterTank getTank() {
        return tank;
    }

    public int coldSourceCount() {
        return chillers.length;
    }

    /** Ids of the Snow sheep chilling it (client side too, for the snowflakes). */
    public int[] chillers() {
        return chillers;
    }

    public int cold() {
        return cold;
    }

    public MachineStatus status() {
        return status;
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory() {
        return inventory;
    }

    public ItemStack output() {
        return inventory.get(0);
    }

    public int comparatorSignal() {
        ItemStack output = inventory.get(0);
        return output.isEmpty() ? 0 : 1 + output.getCount() * 14 / output.getMaxStackSize();
    }

    /** Pours a bucket's worth of water in, if it fits. */
    public boolean fillFromBucket() {
        return tank.fillFromBucket();
    }

    /** It calls its sheep back from just outside its area. */
    @Override
    public boolean attractsSheep() {
        return true;
    }

    @Override
    public boolean showsArea() {
        return showArea;
    }

    @Override
    public void setShowsArea(boolean show) {
        showArea = show;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public AABB displayedArea() {
        return chillingArea(worldPosition, rangeBonus());
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.barelyfunctionalcontraptions.chiller");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.ChillerMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); // its modules: water, upgrades, the Ice
        progress = input.getIntOr("progress", 0);
        fullTicks = Math.max(0, input.getIntOr("full_ticks", 0));
        thawTicks = Math.max(0, input.getIntOr("thaw_ticks", 0));
        showArea = input.getBooleanOr("show_area", false);
        chillers = input.getIntArray("chillers").orElse(new int[0]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); // its modules
        output.putInt("progress", progress);
        output.putInt("full_ticks", fullTicks);
        output.putInt("thaw_ticks", thawTicks);
        output.putBoolean("show_area", showArea);
    }

    /** Clients get the area setting and the Snow sheep (for the snowflakes). */
    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putBoolean("show_area", showArea);
        tag.putIntArray("chillers", chillers);
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades() {
        return upgrades;
    }
}
