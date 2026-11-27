package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.CopperCombBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.blocks.StaticGeneratorBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.StaticGeneratorMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * The Static Generator, a mid-game generator: sheep rubbing against Copper Combs in its pen build up static, which its
 * dome collects and arcs into power.
 *
 * <ul>
 *     <li><b>Pen and combs:</b> a 5x5 pen centred on it ({@code radius}), at its level and the one above. It uses the
 *     nearest {@code maxCombs} (4) Copper Combs in the pen.</li>
 *     <li><b>Herd:</b> every half second it counts the grown sheep with their fleece on standing within
 *     {@code combReach} (1.5 blocks) of a comb. Each gives {@code powerPerSheep} (100 FE/t), or what
 *     {@code sheepPower} lists for its resource (Copper 150, Redstone 125), times its comb's efficiency, up to
 *     {@code maxSheep} (6): 600 FE/t from plain sheep, 900 from Copper sheep. ({@code falloff}, 0 by default, can make
 *     each one after the best add a little less.)</li>
 *     <li><b>Combs:</b> a comb gives 100% fresh, then 75, 50 and 25% as it oxidises; waxed, 85% of that. Each comb a
 *     sheep is rubbing against wears, and after {@code wearTicks} (20 minutes) of it oxidises a stage, unless it's
 *     waxed. An axe scrapes it back.</li>
 *     <li><b>Dome:</b> the charge builds in its dome, which arcs into its buffer every {@code arcTicks} (3 seconds).
 *     With the buffer full the dome keeps charging, up to {@code domeCapacity}.</li>
 *     <li><b>Shocks:</b> with the dome more than {@code shockAt} (half) full, it shocks any player or mob in its pen
 *     (not sheep), knocking them back. Woollen Boots take half off.</li>
 * </ul>
 * It pushes its power out of every face of its base but the top, up to {@code output} FE/t. A comparator reads how
 * charged its dome is.
 */
public class StaticGeneratorBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame implements AreaDisplay, MenuProvider {
    public static final int SCAN_INTERVAL = 10, SHOCK_INTERVAL = 10;
    public static final int AREA_COLOR = 0xFF5FB8FF;
    public static final int DATA_COUNT = 20;
    public static final int MAX_SHOWN_COMBS = 4;
    private static final double[] STAGE_EFFICIENCY = {1.0, 0.75, 0.5, 0.25};

    /** What the generator is doing, for its window's status line and lamp. */
    public enum State {
        NEEDS_COMBS(0xFF9AA0A6), NEEDS_HERD(0xFF9AA0A6), CHARGING(0xFF5FB8FF), FULL(0xFFE8B23A), OVERCHARGED(0xFFE35D5D);

        /** ARGB, for its window's status line. */
        public final int color;

        State(int color) {
            this.color = color;
        }

        public static State byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : NEEDS_COMBS;
        }
    }

    // Its one module, a power buffer (out only).
    private final GeneratorEnergy energy = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.EnergyModule.of(new GeneratorEnergy(500_000, this::setChanged))).handler();
    /** The combs it uses, nearest first, and how long each has been worked towards its next stage of oxidation. */
    private List<BlockPos> combs = new ArrayList<>();
    private final Map<BlockPos, Integer> wear = new HashMap<>();
    /** The combs sheep are rubbing against now. */
    private List<BlockPos> working = new ArrayList<>();
    private int herd, generating, charge, arcTimer, scanCooldown, shockCooldown;
    private boolean showArea;
    private State state = State.NEEDS_COMBS;
    private int lastSignal;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 13 && index < 13 + MAX_SHOWN_COMBS) return combCode(index - 13);
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> generating & 0xFFFF;
                case 5 -> generating >>> 16;
                case 6 -> charge & 0xFFFF;
                case 7 -> charge >>> 16;
                case 8 -> domeCapacity() & 0xFFFF;
                case 9 -> domeCapacity() >>> 16;
                case 10 -> herd;
                case 11 -> maxSheep();
                case 12 -> combs.size();
                case 17 -> state.ordinal();
                case 18 -> showArea ? 1 : 0;
                case 19 -> (int) Math.round(ModConfigs.getDouble(ModConfigs.SERVER.staticShockAt) * 1000);
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

    public StaticGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATIC_GENERATOR.get(), pos, state);
    }

    // ------------------------------------------------------------------ rules

    public static int maxSheep() {
        return ModConfigs.getInt(ModConfigs.SERVER.staticMaxSheep);
    }

    public static int domeCapacity() {
        return ModConfigs.getInt(ModConfigs.SERVER.staticDomeCapacity);
    }

    /** The pen: (2 radius + 1) square centred on it, at its level and the one above. */
    public static AABB pen(BlockPos pos) {
        int r = ModConfigs.getInt(ModConfigs.SERVER.staticRadius);
        return new AABB(pos.getX() - r, pos.getY(), pos.getZ() - r, pos.getX() + r + 1, pos.getY() + 2, pos.getZ() + r + 1);
    }

    /** How much of a sheep's charge a comb passes on: by its stage of oxidation, and a little less if waxed. */
    public static double efficiency(BlockState comb) {
        if (!(comb.getBlock() instanceof CopperCombBlock)) return 0;
        double e = STAGE_EFFICIENCY[comb.getValue(CopperCombBlock.AGE)];
        return comb.getValue(CopperCombBlock.WAXED) ? e * ModConfigs.getDouble(ModConfigs.SERVER.staticWaxedEfficiency) : e;
    }

    /** FE/t of charge from {@code sheep} against a fresh comb: its resource's entry in sheepPower, or powerPerSheep. */
    public static int sheepPower(Sheep sheep) {
        return sheepPower(SheepInfusionEvents.getInfusion(sheep).resource().map(Identifier::toString).orElse(null));
    }

    /** FE/t of charge from a sheep of resource {@code id} (null for a plain sheep) against a fresh comb. */
    public static int sheepPower(@org.jspecify.annotations.Nullable String id) {
        if (id != null) {
            for (String entry : ModConfigs.SERVER.staticSheepPower.get()) {
                int eq = entry.indexOf('=');
                if (eq > 0 && entry.substring(0, eq).trim().equals(id)) {
                    try {
                        return Integer.parseInt(entry.substring(eq + 1).trim());
                    } catch (NumberFormatException ignored) {
                        // a bad entry counts as none
                    }
                }
            }
        }
        return ModConfigs.getInt(ModConfigs.SERVER.staticPowerPerSheep);
    }

    /** Whether {@code sheep} can be rubbed: grown, with its fleece on. */
    public static boolean rubbable(Sheep sheep) {
        return sheep.isAlive() && !sheep.isSheared() && !sheep.isBaby();
    }

    /** FE/t from sheep giving {@code charges} each (already times their comb's efficiency): the best in full, then less. */
    public static int power(List<Double> charges) {
        return BlazeFurnaceBlockEntity.combine(charges, maxSheep(), ModConfigs.getDouble(ModConfigs.SERVER.staticFalloff));
    }

    /** How much of a shock {@code entity}'s Woollen Boots take off, in quarters: 2 (half) with them on, else 0. */
    public static int insulation(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WOOLLEN_BOOTS.get()) ? 2 : 0;
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, StaticGeneratorBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState blockState) {
        energy.resize(ModConfigs.getInt(ModConfigs.SERVER.staticCapacity));
        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            scan(level, pos);
        }
        State previous = state;
        int cap = domeCapacity();
        charge = Math.min(cap, charge + generating);
        if (generating > 0) wearCombs(level);

        // The dome arcs into the buffer every few seconds.
        if (++arcTimer >= ModConfigs.getInt(ModConfigs.SERVER.staticArcTicks)) {
            arcTimer = 0;
            int moved = Math.min(charge, energy.getCapacityAsInt() - energy.getAmountAsInt());
            if (moved > 0) {
                charge -= moved;
                energy.generate(moved);
                arc(level, pos);
            }
        }

        boolean overcharged = charge >= cap * ModConfigs.getDouble(ModConfigs.SERVER.staticShockAt) && charge > 0;
        if (overcharged && --shockCooldown <= 0) {
            shockCooldown = SHOCK_INTERVAL;
            shock(level, pos);
        }
        state = combs.isEmpty() ? State.NEEDS_COMBS : generating <= 0 ? State.NEEDS_HERD : overcharged ? State.OVERCHARGED
                : energy.getAmountAsInt() >= energy.getCapacityAsInt() ? State.FULL : State.CHARGING;
        push(level, pos);

        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, blockState.getBlock());
            level.updateNeighbourForOutputSignal(pos.above(), blockState.getBlock());
        }
        if (state != previous) setChanged();
        boolean active = generating > 0;
        if (blockState.getValue(StaticGeneratorBlock.ACTIVE) != active) {
            level.setBlock(pos, blockState.setValue(StaticGeneratorBlock.ACTIVE, active), Block.UPDATE_ALL);
            BlockState top = level.getBlockState(pos.above());
            if (top.getBlock() instanceof StaticGeneratorBlock) level.setBlock(pos.above(), top.setValue(StaticGeneratorBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    /**
     * Finds its combs and the sheep rubbing against them, and works out the charge. Each sheep counts once, at the best
     * comb it's next to; it calls back sheep that wander just out of the pen while there's room for more.
     */
    public void scan(ServerLevel level, BlockPos pos) {
        int r = ModConfigs.getInt(ModConfigs.SERVER.staticRadius);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, 0, -r), pos.offset(r, 1, r))) {
            if (level.getBlockState(p).getBlock() instanceof CopperCombBlock) found.add(p.immutable());
        }
        found.sort(Comparator.comparingDouble(p -> p.distSqr(pos)));
        combs = new ArrayList<>(found.subList(0, Math.min(found.size(), ModConfigs.getInt(ModConfigs.SERVER.staticMaxCombs))));
        wear.keySet().retainAll(combs);

        AABB pen = pen(pos);
        double reach = ModConfigs.getDouble(ModConfigs.SERVER.staticCombReach);
        List<Sheep> sheep = level.getEntitiesOfClass(Sheep.class, pen, s -> pen.contains(s.position()) && rubbable(s));
        List<Double> charges = new ArrayList<>();
        List<BlockPos> worked = new ArrayList<>();
        for (Sheep s : sheep) {
            double best = 0;
            BlockPos at = null;
            for (BlockPos comb : combs) {
                Vec3 c = Vec3.atBottomCenterOf(comb);
                double dx = s.getX() - c.x, dz = s.getZ() - c.z;
                if (dx * dx + dz * dz > reach * reach || Math.abs(s.getY() - c.y) > 1.0) continue;
                double e = efficiency(level.getBlockState(comb));
                if (e > best) {
                    best = e;
                    at = comb;
                }
            }
            if (at != null) {
                charges.add(sheepPower(s) * best);
                if (!worked.contains(at)) worked.add(at);
            }
        }
        charges.sort(Comparator.reverseOrder());
        herd = Math.min(charges.size(), maxSheep());
        generating = power(charges);
        working = worked;
        long now = level.getGameTime();
        SheepAttraction.claim(sheep, now);
        if (!combs.isEmpty()) SheepAttraction.attract(level, pen, StaticGeneratorBlockEntity::rubbable, maxSheep() - herd, now);
        setChanged();
    }

    /** Each comb being rubbed wears; worked long enough, it oxidises a stage (waxed combs don't). */
    private void wearCombs(ServerLevel level) {
        int limit = ModConfigs.getInt(ModConfigs.SERVER.staticWearTicks);
        for (BlockPos comb : working) {
            BlockState state = level.getBlockState(comb);
            if (!(state.getBlock() instanceof CopperCombBlock) || state.getValue(CopperCombBlock.WAXED) || state.getValue(CopperCombBlock.AGE) >= 3) continue;
            int worn = wear.merge(comb, 1, Integer::sum);
            if (worn >= limit) {
                wear.remove(comb);
                level.setBlock(comb, CopperCombBlock.aged(state), Block.UPDATE_ALL);
                level.levelEvent(3005, comb, 0); // a puff of verdigris
            }
        }
    }

    /** The dome discharging: a crackle of sparks from the dome down to a comb. */
    private void arc(ServerLevel level, BlockPos pos) {
        Vec3 dome = Vec3.atBottomCenterOf(pos.above()).add(0, 0.65, 0);
        level.playSound(null, pos.above(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.6F, 1.6F + level.getRandom().nextFloat() * 0.3F);
        BlockPos target = working.isEmpty() ? (combs.isEmpty() ? null : combs.get(0)) : working.get(level.getRandom().nextInt(working.size()));
        if (target == null) return;
        Vec3 to = Vec3.atBottomCenterOf(target).add(0, 0.9, 0);
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            Vec3 p = dome.lerp(to, i / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.06, 0.06, 0.06, 0.02);
        }
    }

    /** Knocks back every player and mob in the pen (not sheep), away from the dome, less with Woollen Boots on. */
    private void shock(ServerLevel level, BlockPos pos) {
        AABB area = pen(pos).expandTowards(0, 2, 0);
        Vec3 dome = Vec3.atBottomCenterOf(pos.above()).add(0, 0.65, 0);
        double strength = ModConfigs.getDouble(ModConfigs.SERVER.staticShockStrength);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> !(e instanceof Sheep) && e.isAlive() && !e.isSpectator())) {
            if (entity instanceof Player player && player.getAbilities().instabuild) continue;
            double push = strength * (1.0 - insulation(entity) / 4.0);
            Vec3 away = entity.position().subtract(dome.x, entity.getY(), dome.z);
            if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
            away = away.normalize();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 12, 0.25, 0.4, 0.25, 0.1);
            level.playSound(null, entity.blockPosition(), SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 1.0F, 2.0F);
            if (push <= 0) continue;
            entity.push(away.x * push, 0.35 * push, away.z * push);
            com.jax3.barelyfunctionalcontraptions.compat.Mc.syncMotion(entity);
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.award(player,
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.STATIC_SHOCK);
            }
            charge = Math.max(0, charge - domeCapacity() / 20); // each shock bleeds a little charge off
        }
    }

    private void push(ServerLevel level, BlockPos pos) {
        energy.pushToNeighbours(level, pos, ModConfigs.getInt(ModConfigs.SERVER.staticOutput), (dir, target) -> dir == Direction.UP); // not up into the dome
    }

    // ------------------------------------------------------------------ accessors

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    public State state() {
        return state;
    }

    public int generating() {
        return generating;
    }

    public int herd() {
        return herd;
    }

    public int charge() {
        return charge;
    }

    public List<BlockPos> combs() {
        return combs;
    }

    /** Ticks comb {@code pos} has been worked towards its next stage. */
    public int wear(BlockPos pos) {
        return wear.getOrDefault(pos, 0);
    }

    /** How charged its dome is: 0 empty, 15 full. */
    public int comparatorSignal() {
        return (int) ((long) charge * 15 / Math.max(1, domeCapacity()));
    }

    /** Sets the dome's charge (tests and the screenshot harness). */
    public void setCharge(int charge) {
        this.charge = Math.max(0, Math.min(domeCapacity(), charge));
    }

    /** Fills its buffer, as if nothing had taken its power for a while (tests and the screenshot harness). */
    public void fillBuffer() {
        energy.generate(Integer.MAX_VALUE);
    }

    /** One comb's state for the window: bit 3 set when there is one, bit 2 when it's waxed, bits 0-1 its stage. */
    private int combCode(int i) {
        if (level == null || i >= combs.size()) return 0;
        BlockState comb = level.getBlockState(combs.get(i));
        if (!(comb.getBlock() instanceof CopperCombBlock)) return 0;
        return 8 | (comb.getValue(CopperCombBlock.WAXED) ? 4 : 0) | comb.getValue(CopperCombBlock.AGE);
    }

    // ------------------------------------------------------------------ window and area

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.static_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new StaticGeneratorMenu(containerId, inventory, this, data);
    }

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
        return pen(worldPosition);
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); // its modules
        charge = input.getIntOr("charge", 0);
        showArea = input.getBooleanOr("show_area", false);
        state = State.byId(input.getIntOr("state", 0));
        wear.clear();
        int[] worn = input.getIntArray("comb_wear").orElse(new int[0]); // x, y, z, ticks for each comb
        for (int i = 0; i + 3 < worn.length; i += 4) wear.put(new BlockPos(worn[i], worn[i + 1], worn[i + 2]), worn[i + 3]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); // its modules
        output.putInt("charge", charge);
        output.putBoolean("show_area", showArea);
        output.putInt("state", state.ordinal());
        int[] worn = new int[wear.size() * 4];
        int i = 0;
        for (var entry : wear.entrySet()) {
            worn[i++] = entry.getKey().getX();
            worn[i++] = entry.getKey().getY();
            worn[i++] = entry.getKey().getZ();
            worn[i++] = entry.getValue();
        }
        output.putIntArray("comb_wear", worn);
    }

    /** Clients get the area setting, for its outline, and the charge, for the dome's sparks. */
    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putBoolean("show_area", showArea);
        tag.putInt("charge", charge);
    }

}
