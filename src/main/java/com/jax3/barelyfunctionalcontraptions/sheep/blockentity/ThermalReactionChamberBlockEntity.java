package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ThermalReactionChamberBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.ThermalReactionChamberMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * The Thermal Reaction Chamber, a late-game generator that makes power from the difference between hot and cold.
 *
 * <ul>
 *     <li><b>Herds:</b> every half second it counts the grown Blaze sheep (fleece on) in the pen against its
 *     <i>hot</i> side and the Snow sheep in the pen against its <i>cold</i> side. Looking at its front, the hot side is
 *     on the left. Each pen is 5x5 ({@code radius}), starting one block out from that face.</li>
 *     <li><b>Pairs:</b> a Blaze sheep matched by a Snow sheep is a pair. The first makes {@code powerPerPair} FE/t
 *     (2,500), each after it a little less ({@code falloff}), up to {@code maxPairs} (6: about 10,000 FE/t).</li>
 *     <li><b>Boosts:</b> a Blaze Rod in the hot slot and Blue Ice in the cold slot each burn {@code boostTicks}. With
 *     both burning, the output is multiplied by {@code boostMultiplier} (x2). One alone tips the balance instead.</li>
 *     <li><b>Water:</b> {@code waterPerTick} mB (250) while it reacts. Running dry stops it, and it vents scalding steam
 *     out of its front for a few seconds.</li>
 *     <li><b>Balance:</b> each unpaired sheep (or lone boost) tips the balance one step towards hot or cold and costs
 *     {@code imbalancePenalty} of the output. Left hot for {@code unstableTicks} (30 s) it <b>melts down</b>: the hot
 *     side's sheep are singed bare and it explodes ({@code meltdownExplodes}; otherwise it shuts down until it cools).
 *     Left cold as long, it <b>freezes solid</b>, encasing itself in Packed Ice that must be mined away before it
 *     restarts. Running balanced counts the danger back down.</li>
 *     <li><b>Fatigue:</b> each sheep can react for {@code fatigueTicks} (an hour) before its fleece is spent and it
 *     stops counting; infusing it again (the Infusion Altar, or by hand) renews it.</li>
 * </ul>
 * It pushes its power into every block around it, up to {@code output} FE/t. Its comparator signal shows how close it
 * is to melting down or freezing (0 = steady), for safety circuits.
 */
public class ThermalReactionChamberBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame implements com.jax3.barelyfunctionalcontraptions.sheep.machine.InventoryHolder, MenuProvider, AreaDisplay {
    public static final int SCAN_INTERVAL = 10;
    public static final int HOT_COLOR = 0xFFFF8A1F, COLD_COLOR = 0xFF7FD8FF;
    public static final int HOT_SLOT = 0, COLD_SLOT = 1;
    public static final int DATA_COUNT = 21;
    /** {@link #balance} is sent to the window offset by this, as data slots can't be negative. */
    public static final int BALANCE_OFFSET = 64;
    /** How long it vents steam after running dry, in ticks. */
    public static final int STEAM_TICKS = 100;
    public static final FluidResource WATER = WaterTank.WATER;

    /** What the chamber is doing, for its window and particles: the order matches the window's status keys. */
    public enum State {
        NEEDS_HERDS(0xFF9AA0A6), REACTING(0xFF55D66B), RUNNING_HOT(0xFFFF9A2E), RUNNING_COLD(0xFF7FC8F0),
        NO_WATER(0xFFE8B23A), FULL(0xFFE8B23A), OVERHEATED(0xFFE35D5D), FROZEN(0xFFBFE8FF);

        public final int color;

        State(int color) {
            this.color = color;
        }

        public static State byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : NEEDS_HERDS;
        }

        public boolean reacting() {
            return this == REACTING || this == RUNNING_HOT || this == RUNNING_COLD;
        }
    }

    // Its modules: water in and power out on every side, and the two boost slots (in only).
    private final WaterTank tank = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.FluidModule.of(new WaterTank(16_000, this::setChanged))).tank();
    private final GeneratorEnergy energy = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.EnergyModule.of(new GeneratorEnergy(2_000_000, this::setChanged))).handler();
    private final com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory.builder()
            .input("hot_boost", stack -> stack.is(Items.BLAZE_ROD)) // Blaze Rods in the hot slot
            .input("cold_boost", stack -> stack.is(Items.BLUE_ICE)) // Blue Ice in the cold one
            .build());
    /** Ticks left on the Blaze Rod and Blue Ice burning now. */
    private int hotBurn, coldBurn;
    /** Ids of the sheep counted on each side (synced, for the particles). */
    private int[] hotIds = new int[0], coldIds = new int[0];
    /** Sheep in the pens whose fleece is spent. */
    private int spent;
    private int pairs, balance, generating;
    /** Ticks it has run hot (above 0) or cold (below 0); at +-unstableTicks it melts down or freezes. */
    private int instability;
    private boolean frozen, overheated;
    private int steamTicks;
    private int scanCooldown;
    private boolean showArea;
    private State state = State.NEEDS_HERDS;
    private int lastSignal;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> generating & 0xFFFF;
                case 5 -> generating >>> 16;
                case 6 -> tank.getAmount() & 0xFFFF;
                case 7 -> tank.getAmount() >>> 16;
                case 8 -> tank.capacity() & 0xFFFF;
                case 9 -> tank.capacity() >>> 16;
                case 10 -> hotIds.length;
                case 11 -> coldIds.length;
                case 12 -> pairs;
                case 13 -> maxPairs();
                case 14 -> Math.clamp(balance + BALANCE_OFFSET, 0, BALANCE_OFFSET * 2);
                case 15 -> 1000 + instability * 1000 / unstableTicks();
                case 16 -> hotBurn * 1000 / boostTicks();
                case 17 -> coldBurn * 1000 / boostTicks();
                case 18 -> state.ordinal();
                case 19 -> showArea ? 1 : 0;
                case 20 -> spent;
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

    public ThermalReactionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_REACTION_CHAMBER.get(), pos, state);
    }

    // ------------------------------------------------------------------ rules

    /** The hot side: on the left as you look at its front. */
    public static Direction hotSide(Direction facing) {
        return facing.getClockWise();
    }

    public static Direction coldSide(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** The pen against {@code side}: (2 radius + 1) square, starting one block out, at its level and the one above. */
    public static AABB pen(BlockPos pos, Direction side) {
        int r = ModConfigs.getInt(ModConfigs.SERVER.thermalRadius);
        BlockPos centre = pos.relative(side, r + 1);
        return new AABB(centre.getX() - r, pos.getY(), centre.getZ() - r, centre.getX() + r + 1, pos.getY() + 2, centre.getZ() + r + 1);
    }

    public static int maxPairs() {
        return ModConfigs.getInt(ModConfigs.SERVER.thermalMaxPairs);
    }

    public static int unstableTicks() {
        return ModConfigs.getInt(ModConfigs.SERVER.thermalUnstableTicks);
    }

    public static int boostTicks() {
        return ModConfigs.getInt(ModConfigs.SERVER.thermalBoostTicks);
    }

    /** FE/t from {@code pairs} balanced pairs, before boosts. */
    public static int powerFrom(int pairs) {
        return BlazeFurnaceBlockEntity.combine(Collections.nCopies(Math.max(0, pairs), (double) ModConfigs.getInt(ModConfigs.SERVER.thermalPowerPerPair)),
                maxPairs(), ModConfigs.getDouble(ModConfigs.SERVER.thermalFalloff));
    }

    /** FE/t from {@code pairs}, with both boosts burning or not, and the balance {@code balance} steps off. */
    public static int power(int pairs, boolean boosted, int balance) {
        double factor = boosted ? ModConfigs.getDouble(ModConfigs.SERVER.thermalBoostMultiplier) : 1.0;
        factor *= Math.max(0.0, 1.0 - ModConfigs.getDouble(ModConfigs.SERVER.thermalImbalancePenalty) * Math.abs(balance));
        return (int) Math.round(powerFrom(pairs) * factor);
    }

    /** Whether {@code sheep} is a grown sheep, fleece on and not spent, infused with one of {@code sources}. */
    private static boolean counts(Sheep sheep, List<? extends String> sources) {
        if (!sheep.isAlive() || sheep.isSheared() || sheep.isBaby()) return false;
        return SheepInfusionEvents.getInfusion(sheep).resource().map(Identifier::toString).map(sources::contains).orElse(false);
    }

    public static boolean isHotSource(Sheep sheep) {
        return counts(sheep, ModConfigs.SERVER.thermalHotSources.get());
    }

    public static boolean isColdSource(Sheep sheep) {
        return counts(sheep, ModConfigs.SERVER.thermalColdSources.get());
    }

    public static boolean isSpent(Sheep sheep) {
        return sheep.getData(ModAttachments.THERMAL_FATIGUE).spent();
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ThermalReactionChamberBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState blockState) {
        tank.resize(ModConfigs.getInt(ModConfigs.SERVER.thermalTankCapacity));
        energy.resize(ModConfigs.getInt(ModConfigs.SERVER.thermalCapacity));
        Direction facing = blockState.getValue(ThermalReactionChamberBlock.FACING);
        State previous = state;
        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            scan(level, pos, facing, previous.reacting());
        }
        generating = 0;
        int limit = unstableTicks();

        if (frozen) {
            // Frozen solid until the Packed Ice round it has been mined away.
            frozen = encased(level, pos);
            state = frozen ? State.FROZEN : State.NEEDS_HERDS;
            balance = 0;
        } else if (overheated) {
            // Cooling down after a meltdown that didn't explode.
            instability = Math.max(0, instability - 1);
            overheated = instability > 0;
            state = overheated ? State.OVERHEATED : State.NEEDS_HERDS;
        } else if (pairs <= 0) {
            state = State.NEEDS_HERDS;
            balance = 0;
            settle();
        } else if (tank.getAmount() < waterPerTick()) {
            if (previous.reacting()) {
                steamTicks = STEAM_TICKS; // flash-boils: a burst of scalding steam out of the front
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.6F);
            }
            state = State.NO_WATER;
            balance = hotIds.length - coldIds.length; // the needle still follows the herds
            settle();
        } else if (energy.getAmountAsInt() >= energy.getCapacityAsInt()) {
            // Nothing is taking its power, so it makes none and uses no water, boosts or fleece. But the herds are still
            // heating and chilling it: the balance stays live, and uneven herds still drive it towards a meltdown or a freeze.
            state = State.FULL;
            balance = hotIds.length - coldIds.length;
            if (!unbalanced(level, pos, facing, limit)) settle();
        } else {
            burnBoosts();
            boolean hotBoost = hotBurn > 0, coldBoost = coldBurn > 0;
            balance = hotIds.length - coldIds.length + (hotBoost && !coldBoost ? 1 : 0) - (coldBoost && !hotBoost ? 1 : 0);
            generating = power(pairs, hotBoost && coldBoost, balance);
            tank.drain(waterPerTick());
            energy.generate(generating);
            state = balance > 0 ? State.RUNNING_HOT : balance < 0 ? State.RUNNING_COLD : State.REACTING;
            if (!unbalanced(level, pos, facing, limit)) settle();
            setChanged();
        }
        if (isRemoved()) return; // it melted down and exploded

        if (steamTicks > 0) vent(level, pos, facing);
        push(level, pos);

        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, blockState.getBlock());
        }
        if (state != previous) {
            setChanged();
            level.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_CLIENTS); // the particles follow the state
            if (state.reacting() && !previous.reacting()) {
                ModAdvancements.awardNearby(level, pos, ModAdvancements.AUTO_SHEAR_RADIUS, ModAdvancements.THERMAL_REACTION_CHAMBER);
            }
        }
        boolean active = state.reacting();
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() instanceof ThermalReactionChamberBlock
                && (current.getValue(ThermalReactionChamberBlock.ACTIVE) != active || current.getValue(ThermalReactionChamberBlock.FROZEN) != frozen)) {
            level.setBlock(pos, current.setValue(ThermalReactionChamberBlock.ACTIVE, active).setValue(ThermalReactionChamberBlock.FROZEN, frozen), Block.UPDATE_ALL);
        }
    }

    private static int waterPerTick() {
        return ModConfigs.getInt(ModConfigs.SERVER.thermalWaterPerTick);
    }

    /**
     * Counts the danger up towards a meltdown (running hot) or a freeze (running cold) while {@link #balance} is off,
     * setting them off at the limit. Returns false when it is balanced, for the caller to settle instead.
     */
    private boolean unbalanced(ServerLevel level, BlockPos pos, Direction facing, int limit) {
        if (balance > 0) {
            instability = Math.min(limit, instability + 1);
            if (instability >= limit) meltdown(level, pos, facing);
        } else if (balance < 0) {
            instability = Math.max(-limit, instability - 1);
            if (instability <= -limit) freeze(level, pos);
        } else {
            return false;
        }
        return true;
    }

    /** Running balanced (or not at all) counts the danger back towards steady. */
    private void settle() {
        if (instability > 0) instability--;
        else if (instability < 0) instability++;
    }

    /** Lights a fresh Blaze Rod or Blue Ice when the last one has burnt out, and burns down the ones alight. */
    private void burnBoosts() {
        if (hotBurn <= 0 && inventory.get(HOT_SLOT).is(Items.BLAZE_ROD)) {
            inventory.get(HOT_SLOT).shrink(1);
            hotBurn = boostTicks();
        }
        if (coldBurn <= 0 && inventory.get(COLD_SLOT).is(Items.BLUE_ICE)) {
            inventory.get(COLD_SLOT).shrink(1);
            coldBurn = boostTicks();
        }
        if (hotBurn > 0) hotBurn--;
        if (coldBurn > 0) coldBurn--;
    }

    /**
     * Counts each pen's herd. While it reacted since the last count, every sheep it counted wears its fleece a little
     * more; one whose fleece is spent stops counting.
     */
    public void scan(ServerLevel level, BlockPos pos, Direction facing, boolean reacted) {
        int max = maxPairs();
        // More sheep than pairs are still counted (to the limit and a few over), so an overstocked side tips the balance.
        int cap = max + 8;
        AABB hotPen = pen(pos, hotSide(facing)), coldPen = pen(pos, coldSide(facing));
        List<Sheep> hot = level.getEntitiesOfClass(Sheep.class, hotPen, s -> hotPen.contains(s.position()) && isHotSource(s));
        List<Sheep> cold = level.getEntitiesOfClass(Sheep.class, coldPen, s -> coldPen.contains(s.position()) && isColdSource(s));
        int worn = 0;
        int[][] ids = new int[2][];
        List<List<Sheep>> sides = List.of(hot, cold);
        for (int side = 0; side < 2; side++) {
            List<Sheep> herd = sides.get(side);
            herd.sort(java.util.Comparator.comparingInt(Sheep::getId));
            int[] counted = new int[Math.min(cap, herd.size())];
            int n = 0;
            for (Sheep sheep : herd) {
                if (isSpent(sheep)) {
                    worn++;
                    continue;
                }
                if (n >= counted.length) break;
                if (reacted) sheep.setData(ModAttachments.THERMAL_FATIGUE, sheep.getData(ModAttachments.THERMAL_FATIGUE).add(SCAN_INTERVAL));
                counted[n++] = sheep.getId();
            }
            ids[side] = Arrays.copyOf(counted, n);
        }
        spent = worn;
        pairs = Math.min(max, Math.min(ids[0].length, ids[1].length));
        callHerds(level, hotPen, coldPen, hot, cold, ids[0].length, ids[1].length, max, attractsSheep());
        if (!Arrays.equals(ids[0], hotIds) || !Arrays.equals(ids[1], coldIds)) {
            hotIds = ids[0];
            coldIds = ids[1];
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Keeps the herds close without upsetting the balance: a side with fewer sheep calls one of its own back (so a
     * stray that wandered out comes home), and when both sides are even and below the most pairs, it calls a sheep to
     * each side only when there is one waiting nearby for both. It never calls a sheep that would leave a side ahead.
     */
    private static void callHerds(ServerLevel level, AABB hotPen, AABB coldPen, List<Sheep> hot, List<Sheep> cold, int hotCount, int coldCount, int max, boolean attractor) {
        long now = level.getGameTime();
        SheepAttraction.claim(hot, now);
        SheepAttraction.claim(cold, now);
        if (!SheepAttraction.enabled() || !attractor) return; // no Sheep Attractor fitted: they're left to wander
        java.util.function.Predicate<Sheep> hotOk = s -> isHotSource(s) && !isSpent(s);
        java.util.function.Predicate<Sheep> coldOk = s -> isColdSource(s) && !isSpent(s);
        if (hotCount < coldCount) {
            SheepAttraction.attract(level, hotPen, hotOk, Math.min(coldCount, max) - hotCount, now);
        } else if (coldCount < hotCount) {
            SheepAttraction.attract(level, coldPen, coldOk, Math.min(hotCount, max) - coldCount, now);
        } else if (hotCount < max) {
            boolean hotWaiting = !level.getEntitiesOfClass(Sheep.class, SheepAttraction.reach(hotPen), s -> !hotPen.contains(s.position()) && !s.isBaby() && !s.isLeashed() && hotOk.test(s)).isEmpty();
            boolean coldWaiting = !level.getEntitiesOfClass(Sheep.class, SheepAttraction.reach(coldPen), s -> !coldPen.contains(s.position()) && !s.isBaby() && !s.isLeashed() && coldOk.test(s)).isEmpty();
            if (hotWaiting && coldWaiting) {
                SheepAttraction.attract(level, hotPen, hotOk, 1, now);
                SheepAttraction.attract(level, coldPen, coldOk, 1, now);
            }
        }
    }

    /** Run hot too long: the hot side's sheep are singed bare and it explodes (or, with explosions off, shuts down). */
    private void meltdown(ServerLevel level, BlockPos pos, Direction facing) {
        for (int id : hotIds) {
            if (level.getEntity(id) instanceof Sheep sheep) {
                sheep.setSheared(true);
                sheep.igniteForTicks(100);
            }
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (ModConfigs.getBool(ModConfigs.SERVER.thermalMeltdownExplodes)) {
            Containers.dropContents(level, pos, this);
            clearContent();
            level.removeBlock(pos, false);
            level.explode(null, x, y, z, (float) ModConfigs.getDouble(ModConfigs.SERVER.thermalMeltdownPower), true, Level.ExplosionInteraction.BLOCK);
            return;
        }
        overheated = true;
        generating = 0;
        instability = unstableTicks();
        hotBurn = 0;
        coldBurn = 0;
        state = State.OVERHEATED;
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.6, z, 30, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 0.6, 0.4, 0.6, 0.03);
    }

    /** Run cold too long: it freezes solid, Packed Ice round every open face, and stops until that is mined away. */
    private void freeze(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            BlockPos at = pos.relative(dir);
            if (level.getBlockState(at).canBeReplaced()) level.setBlock(at, Blocks.PACKED_ICE.defaultBlockState(), Block.UPDATE_ALL);
        }
        frozen = true;
        generating = 0;
        instability = 0;
        hotBurn = 0;
        coldBurn = 0;
        state = State.FROZEN;
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.playSound(null, pos, SoundEvents.POWDER_SNOW_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 40, 0.8, 0.5, 0.8, 0.03);
    }

    /** Whether any Packed Ice still touches it. */
    private static boolean encased(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.PACKED_ICE)) return true;
        }
        return false;
    }

    /** The space in front of it that its steam scalds: two blocks out, as wide and high as the chamber. */
    public static AABB steamArea(BlockPos pos, Direction facing) {
        BlockPos near = pos.relative(facing), far = pos.relative(facing, 2);
        return new AABB(near).minmax(new AABB(far));
    }

    private void vent(ServerLevel level, BlockPos pos, Direction facing) {
        steamTicks--;
        Vec3 mouth = Vec3.atCenterOf(pos).add(facing.getStepX() * 0.6, 0, facing.getStepZ() * 0.6);
        level.sendParticles(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, 3, 0.15, 0.2, 0.15, 0.08);
        if (steamTicks % 10 == 0) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.6F);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, steamArea(pos, facing))) {
                entity.hurtServer(level, level.damageSources().hotFloor(), 3.0F);
            }
        }
    }

    private void push(ServerLevel level, BlockPos pos) {
        energy.pushToNeighbours(level, pos, ModConfigs.getInt(ModConfigs.SERVER.thermalOutput), (dir, target) -> false);
    }

    // ------------------------------------------------------------------ accessors

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    public WaterTank getTank() {
        return tank;
    }

    public State state() {
        return state;
    }

    public int generating() {
        return generating;
    }

    public int pairs() {
        return pairs;
    }

    public int balance() {
        return balance;
    }

    public int instability() {
        return instability;
    }

    public int spent() {
        return spent;
    }

    /** Ticks left on the Blaze Rod and the Blue Ice burning now. */
    public int hotBurn() {
        return hotBurn;
    }

    public int coldBurn() {
        return coldBurn;
    }

    public int hotCount() {
        return hotIds.length;
    }

    public int coldCount() {
        return coldIds.length;
    }

        public boolean frozen() {
        return frozen;
    }

    /** Ids of the Blaze and Snow sheep it counts (client side too, for the embers and snowflakes). */
    public int[] hotSources() {
        return hotIds;
    }

    public int[] coldSources() {
        return coldIds;
    }

    /** How close it is to melting down or freezing: 0 steady, 15 about to go. */
    public int comparatorSignal() {
        return Math.abs(instability) * 15 / Math.max(1, unstableTicks());
    }

    /** Fills its power buffer, as if nothing had taken its power for a while (tests and the screenshot harness). */
    public void fillBuffer() {
        energy.generate(Integer.MAX_VALUE);
    }

    /** Pours a bucket's worth of water in, if it fits. */
    public boolean fillFromBucket() {
        return tank.fillFromBucket();
    }

    /** It calls its sheep back only while a Sheep Attractor is fitted to it. */
    @Override
    public boolean attractsSheep() {
        return level != null && com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepAttractorBlock.fitted(level, worldPosition);
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

    private Direction facing() {
        BlockState blockState = getBlockState();
        return blockState.hasProperty(ThermalReactionChamberBlock.FACING) ? blockState.getValue(ThermalReactionChamberBlock.FACING) : Direction.NORTH;
    }

    @Override
    public AABB displayedArea() {
        return pen(worldPosition, hotSide(facing())).minmax(pen(worldPosition, coldSide(facing())));
    }

    /** Both pens: the hot one in orange, the cold one in light blue. */
    @Override
    public List<AABB> displayedAreas() {
        return List.of(pen(worldPosition, hotSide(facing())), pen(worldPosition, coldSide(facing())));
    }

    @Override
    public int areaColor() {
        return HOT_COLOR;
    }

    @Override
    public int areaColor(int index) {
        return index == 0 ? HOT_COLOR : COLD_COLOR;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.thermal_reaction_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ThermalReactionChamberMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ the boost slots (its inventory module)

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory() {
        return inventory;
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); // its modules: water, energy, boosts
        hotBurn = input.getIntOr("hot_burn", 0);
        coldBurn = input.getIntOr("cold_burn", 0);
        instability = input.getIntOr("instability", 0);
        frozen = input.getBooleanOr("frozen", false);
        overheated = input.getBooleanOr("overheated", false);
        showArea = input.getBooleanOr("show_area", false);
        state = State.byId(input.getIntOr("state", 0));
        hotIds = input.getIntArray("hot_sources").orElse(new int[0]);
        coldIds = input.getIntArray("cold_sources").orElse(new int[0]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); // its modules
        output.putInt("hot_burn", hotBurn);
        output.putInt("cold_burn", coldBurn);
        output.putInt("instability", instability);
        output.putBoolean("frozen", frozen);
        output.putBoolean("overheated", overheated);
        output.putBoolean("show_area", showArea);
    }

    /** Clients get the area setting, the state and the counted sheep (for the particles). */
    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putBoolean("show_area", showArea);
        tag.putInt("state", state.ordinal());
        tag.putIntArray("hot_sources", hotIds);
        tag.putIntArray("cold_sources", coldIds);
    }
}
