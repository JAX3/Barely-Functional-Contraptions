package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.EchoChamberBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Echo Chamber (work in progress, development builds only): Warden sheep in front of its dish turn the vibrations
 * they hear into power. It lives in the middle block of the machine's bottom layer.
 *
 * <ul>
 *     <li><b>Herd:</b> every half second it counts the grown Warden sheep (fleece on) in its listening pen, a 5x5 area
 *     ({@code radius}) starting one block out from its dish, at its bottom level and the one above. The first makes
 *     {@code powerPerSheep} FE/t (8,000), each after it a little less ({@code falloff}), up to {@code maxSheep} (4: 20,000
 *     FE/t).</li>
 *     <li><b>Hearing:</b> it hears vibrations the way a sculk sensor does, within {@code listenRadius} blocks of the
 *     middle of its dish: note blocks, pistons, doors, footsteps, items landing and so on. Wool in the way blocks them,
 *     as it does for a sculk sensor, and a player wearing Woollen Boots makes none. Its own Warden sheep moving about
 *     aren't counted: they're the ones listening.</li>
 *     <li><b>Noise level:</b> the vibrations it heard over the last {@code windowTicks} (10 seconds), as vibrations per 5
 *     seconds, 0-15. A 1-second redstone clock on a note block is about 5.</li>
 *     <li><b>Sweet spot:</b> from {@code quietBelow} (5) to {@code loudAbove} (10) the noise costs nothing. Quieter,
 *     the output falls away towards nothing in silence; louder, it falls a fifth for each level over.</li>
 *     <li><b>Steadiness:</b> the window is split in five; a noise that's the same in each fifth is perfectly steady
 *     and gets full power, an erratic one as little as half.</li>
 * </ul>
 * It pushes its power out of its power port, the middle of its back, up to {@code output} FE/t. A comparator against
 * any block of it reads the noise level.
 */
public class EchoChamberBlockEntity extends BlockEntity implements AreaDisplay, GameEventListener.Provider<EchoChamberBlockEntity.Ears> {
    public static final int SCAN_INTERVAL = 10;
    public static final int BUCKETS = 5;
    public static final int MAX_LEVEL = 15;
    public static final int AREA_COLOR = 0xFF29DFEB;

    /** What the chamber is doing, for its status line. */
    public enum State {
        NEEDS_HERD(ChatFormatting.GRAY), TOO_QUIET(ChatFormatting.GOLD), LISTENING(ChatFormatting.GREEN), TOO_LOUD(ChatFormatting.RED),
        FULL(ChatFormatting.GOLD);

        public final ChatFormatting color;

        State(ChatFormatting color) {
            this.color = color;
        }

        public static State byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : NEEDS_HERD;
        }

        public String key() {
            return "message.barelyfunctionalcontraptions.echo_chamber.state." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final Ears ears = new Ears();
    private final ChamberEnergy energy = new ChamberEnergy();
    /** Vibrations heard in each fifth of the window, oldest first, and in the fifth being heard now. */
    private int[] buckets = new int[BUCKETS];
    private int hearing, bucketTicks;
    /** Ids of the Warden sheep it counts: their own vibrations don't count. */
    private int[] sheepIds = new int[0];
    private int noise, generating, scanCooldown;
    private double steadiness;
    private boolean showArea;
    private State state = State.NEEDS_HERD;
    private int lastSignal;

    public EchoChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ECHO_CHAMBER.get(), pos, state);
    }

    // ------------------------------------------------------------------ rules

    public static int maxSheep() {
        return ModConfigs.getInt(ModConfigs.SERVER.echoMaxSheep);
    }

    public static int windowTicks() {
        return ModConfigs.getInt(ModConfigs.SERVER.echoWindowTicks);
    }

    public static int quietBelow() {
        return ModConfigs.getInt(ModConfigs.SERVER.echoQuietBelow);
    }

    public static int loudAbove() {
        return ModConfigs.getInt(ModConfigs.SERVER.echoLoudAbove);
    }

    /** FE/t from {@code sheep} Warden sheep, with the noise in the sweet spot and perfectly steady. */
    public static int powerFrom(int sheep) {
        return BlazeFurnaceBlockEntity.combine(Collections.nCopies(Math.max(0, sheep), (double) ModConfigs.getInt(ModConfigs.SERVER.echoPowerPerSheep)),
                maxSheep(), ModConfigs.getDouble(ModConfigs.SERVER.echoFalloff));
    }

    /** How much of the power a noise level gives: 1 in the sweet spot, less when too quiet or too loud. */
    public static double noiseFactor(int noise) {
        int quiet = quietBelow(), loud = Math.max(quiet, loudAbove());
        if (noise < quiet) return (double) Math.max(0, noise) / quiet;
        if (noise > loud) return Math.max(0.0, 1.0 - 0.2 * (noise - loud));
        return 1.0;
    }

    /** FE/t from {@code sheep} Warden sheep hearing a noise level {@code noise} with {@code steadiness} (0-1). */
    public static int power(int sheep, int noise, double steadiness) {
        double steady = 0.5 + 0.5 * Mth.clamp(steadiness, 0.0, 1.0);
        return (int) Math.round(powerFrom(sheep) * noiseFactor(noise) * steady);
    }

    /** How steady a noise is: 1 when every fifth of the window heard the same, falling as they differ; 0 in silence. */
    public static double steadiness(int[] counts) {
        double mean = Arrays.stream(counts).average().orElse(0);
        if (mean <= 0) return 0;
        double variance = Arrays.stream(counts).mapToDouble(c -> (c - mean) * (c - mean)).average().orElse(0);
        return Mth.clamp(1.0 - Math.sqrt(variance) / mean, 0.0, 1.0);
    }

    /** The noise level for vibrations heard over a whole window: vibrations per 5 seconds, 0-15. */
    public static int noiseLevel(int heard) {
        return Mth.clamp((int) Math.round(heard * 100.0 / windowTicks()), 0, MAX_LEVEL);
    }

    /** Whether {@code sheep} is a grown sheep, fleece on, infused with a resource that listens for it. */
    public static boolean isListener(Sheep sheep) {
        if (!sheep.isAlive() || sheep.isSheared() || sheep.isBaby()) return false;
        List<? extends String> sources = ModConfigs.SERVER.echoSources.get();
        return SheepInfusionEvents.getInfusion(sheep).resource().map(Identifier::toString).map(sources::contains).orElse(false);
    }

    /** The front middle bottom block of the machine whose middle block is {@code pos}. */
    public static BlockPos front(BlockPos pos, Direction facing) {
        return pos.relative(facing);
    }

    /** Where it listens from: the middle of its dish, in the front middle column at the height of the second layer. */
    public static Vec3 dish(BlockPos pos, Direction facing) {
        return Vec3.atCenterOf(front(pos, facing).above());
    }

    /** The listening pen: (2 radius + 1) square, starting one block out from its dish, at its bottom level and the one above. */
    public static AABB pen(BlockPos pos, Direction facing) {
        int r = ModConfigs.getInt(ModConfigs.SERVER.echoRadius);
        BlockPos centre = front(pos, facing).relative(facing, r + 1);
        return new AABB(centre.getX() - r, pos.getY(), centre.getZ() - r, centre.getX() + r + 1, pos.getY() + 2, centre.getZ() + r + 1);
    }

    /** Its power port: the middle of its back, on the bottom layer. */
    public static BlockPos port(BlockPos pos, Direction facing) {
        return pos.relative(facing.getOpposite());
    }

    /** The whole machine, so it doesn't hear itself being built or broken. */
    private static AABB body(BlockPos pos) {
        return new AABB(pos).inflate(1, 0, 1).expandTowards(0, 2, 0);
    }

    // ------------------------------------------------------------------ hearing

    /** Its ears: a game event listener at the middle of its dish that counts the vibrations reaching it. */
    public final class Ears implements GameEventListener {
        @Override
        public PositionSource getListenerSource() {
            return new BlockPositionSource(front(worldPosition, facing()).above());
        }

        @Override
        public int getListenerRadius() {
            return ModConfigs.getInt(ModConfigs.SERVER.echoListenRadius);
        }

        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 source) {
            return hear(level, event, context, source);
        }
    }

    @Override
    public Ears getListener() {
        return ears;
    }

    private boolean hear(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 source) {
        if (isRemoved() || VibrationSystem.getGameEventFrequency(event) <= 0) return false;
        if (context.sourceEntity() != null) {
            int id = context.sourceEntity().getId();
            for (int listener : sheepIds) {
                if (listener == id) return false; // its own Warden sheep: they're the ones listening
            }
        }
        if (body(worldPosition).contains(source)) return false;
        // (The game only tells it about vibrations within its listening radius.)
        if (occluded(level, source, dish(worldPosition, facing()))) return false;
        hearing++;
        return true;
    }

    /** Whether wool (anything that occludes vibrations) stands between the two points, as a sculk sensor checks it. */
    public static boolean occluded(Level level, Vec3 origin, Vec3 dest) {
        Vec3 from = new Vec3(Mth.floor(origin.x) + 0.5, Mth.floor(origin.y) + 0.5, Mth.floor(origin.z) + 0.5);
        Vec3 to = new Vec3(Mth.floor(dest.x) + 0.5, Mth.floor(dest.y) + 0.5, Mth.floor(dest.z) + 0.5);
        for (Direction direction : Direction.values()) {
            Vec3 nudged = from.relative(direction, 1.0E-5F);
            if (level.isBlockInLine(new ClipBlockStateContext(nudged, to, s -> s.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType() != HitResult.Type.BLOCK) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, EchoChamberBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState blockState) {
        energy.resize(ModConfigs.getInt(ModConfigs.SERVER.echoCapacity));
        Direction facing = blockState.getValue(EchoChamberBlock.FACING);
        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            scan(level, pos, facing);
        }
        // A fifth of the window has passed: what it heard joins the window and the oldest fifth drops out.
        if (++bucketTicks >= Math.max(1, windowTicks() / BUCKETS)) {
            bucketTicks = 0;
            System.arraycopy(buckets, 1, buckets, 0, BUCKETS - 1);
            buckets[BUCKETS - 1] = hearing;
            hearing = 0;
            noise = noiseLevel(Arrays.stream(buckets).sum());
            steadiness = steadiness(buckets);
            setChanged();
        }

        State previous = state;
        generating = 0;
        if (sheepIds.length == 0) {
            state = State.NEEDS_HERD;
        } else if (energy.getAmountAsInt() >= energy.getCapacityAsInt()) {
            state = State.FULL;
        } else {
            generating = power(sheepIds.length, noise, steadiness);
            energy.generate(generating);
            state = noise < quietBelow() ? State.TOO_QUIET : noise > loudAbove() ? State.TOO_LOUD : State.LISTENING;
        }
        push(level, pos, facing);

        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            for (int part = 0; part < EchoChamberBlock.SIZE * EchoChamberBlock.SIZE; part++) {
                level.updateNeighbourForOutputSignal(EchoChamberBlock.at(front(pos, facing), facing, part, 0), blockState.getBlock());
            }
        }
        if (state != previous) setChanged();
    }

    /** Counts the Warden sheep in the pen, and calls back one that wandered out while there's room for more. */
    public void scan(ServerLevel level, BlockPos pos, Direction facing) {
        AABB pen = pen(pos, facing);
        List<Sheep> herd = level.getEntitiesOfClass(Sheep.class, pen, s -> pen.contains(s.position()) && isListener(s));
        herd.sort(java.util.Comparator.comparingInt(Sheep::getId));
        int[] ids = herd.stream().limit(maxSheep()).mapToInt(Sheep::getId).toArray();
        long now = level.getGameTime();
        SheepAttraction.claim(herd, now);
        SheepAttraction.attract(level, pen, EchoChamberBlockEntity::isListener, maxSheep() - ids.length, now);
        if (!Arrays.equals(ids, sheepIds)) {
            sheepIds = ids;
            setChanged();
        }
    }

    private void push(ServerLevel level, BlockPos pos, Direction facing) {
        int budget = Math.min(ModConfigs.getInt(ModConfigs.SERVER.echoOutput), energy.getAmountAsInt());
        if (budget <= 0) return;
        Direction back = facing.getOpposite();
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, port(pos, facing).relative(back), facing);
        if (handler == null) return;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = handler.insert(budget, tx);
            if (moved > 0 && energy.extract(moved, tx) == moved) tx.commit();
        }
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

    public int noise() {
        return noise;
    }

    public double steadiness() {
        return steadiness;
    }

    public int herd() {
        return sheepIds.length;
    }

    /** The noise level, 0-15. */
    public int comparatorSignal() {
        return noise;
    }

    /** Hears {@code count} vibrations at once in each fifth of the window, as if a steady noise had been going (tests). */
    public void hearSteadily(int count) {
        Arrays.fill(buckets, count);
        noise = noiseLevel(count * BUCKETS);
        steadiness = steadiness(buckets);
    }

    /** Empties its power buffer, as if something had taken it all (the screenshot harness). */
    public void emptyBuffer() {
        energy.resize(0);
        energy.resize(ModConfigs.getInt(ModConfigs.SERVER.echoCapacity));
    }

    /** Vibrations heard so far in the fifth of the window being heard now (tests). */
    public int hearingNow() {
        return hearing;
    }

    /** One line on what it's doing, for a right-click: state, herd, noise, steadiness and output. */
    public Component status() {
        return Component.translatable("message.barelyfunctionalcontraptions.echo_chamber.status",
                Component.translatable(state.key()).withStyle(state.color), sheepIds.length, maxSheep(), noise, quietBelow(), loudAbove(),
                Math.round(steadiness * 100), String.format(java.util.Locale.ROOT, "%,d", generating));
    }

    private Direction facing() {
        BlockState blockState = getBlockState();
        return blockState.hasProperty(EchoChamberBlock.FACING) ? blockState.getValue(EchoChamberBlock.FACING) : Direction.NORTH;
    }

    // ------------------------------------------------------------------ area

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
        return pen(worldPosition, facing());
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("energy").ifPresent(energy::deserialize);
        int[] saved = input.getIntArray("heard").orElse(new int[0]);
        buckets = saved.length == BUCKETS ? saved : new int[BUCKETS];
        noise = input.getIntOr("noise", 0);
        steadiness = steadiness(buckets);
        showArea = input.getBooleanOr("show_area", false);
        state = State.byId(input.getIntOr("state", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output.child("energy"));
        output.putIntArray("heard", buckets);
        output.putInt("noise", noise);
        output.putBoolean("show_area", showArea);
        output.putInt("state", state.ordinal());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients only need the area setting, for its outline. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("show_area", showArea);
        return tag;
    }

    /** Out only: it makes its own power. */
    private final class ChamberEnergy extends SimpleEnergyHandler {
        ChamberEnergy() {
            super(2_000_000, 0, Integer.MAX_VALUE);
        }

        void generate(int amount) {
            int previous = energy;
            energy = (int) Math.min(capacity, (long) energy + Math.max(0, amount));
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
}
