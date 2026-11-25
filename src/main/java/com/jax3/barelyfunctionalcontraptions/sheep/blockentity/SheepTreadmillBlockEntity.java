package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.SheepTreadmillBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.TreadmillWalker;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.SheepTreadmillMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * The Sheep Treadmill, on its back block (the one with the generator housing). A grown sheep standing on its belt walks
 * towards the wheat hanging at the front and turns the belt, making {@code [sheep_treadmill] power} FE/t:
 * <ul>
 *     <li>more with Speed, less with Slowness, and more while the sheep is sheared and running light;</li>
 *     <li>it eats from the wheat slot as it walks (a Wheat feeds {@code ticksPerWheat} ticks, a Hay Bale nine times
 *     that); with nothing to eat it stops and wanders off;</li>
 *     <li>after {@code walkTicks} (longer for each Endurance point above 1) the sheep is worn out, steps off and rests
 *     for {@code restTicks} before it will walk again. Its tiredness stays with the sheep, so a fresh one can take over.</li>
 * </ul>
 * While it waits for a sheep, with food in it, it calls the nearest grown sheep over to the belt ({@code attractRadius}).
 * While it walks, the treadmill keeps the sheep in the middle of the belt facing the wheat. It doesn't make power, or
 * use up wheat or the sheep's stamina, while its buffer is full. It pushes its energy into every block around it.
 */
public class SheepTreadmillBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame implements com.jax3.barelyfunctionalcontraptions.sheep.machine.InventoryHolder, MenuProvider {
    public static final int DATA_COUNT = 9;
    /** How far the sheep is above the block's floor: the belt's top. */
    public static final double BELT_TOP = 6.0 / 16.0;

    /** What the treadmill is doing, for its window and comparator: the order matches the window's status keys. */
    public enum State { NO_SHEEP, WALKING, HUNGRY, RESTING, FULL }

    // Its modules: a power buffer and the food slot (in only), offered from either of its blocks.
    private final GeneratorEnergy energy = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.EnergyModule.of(new GeneratorEnergy(20_000, this::setChanged))).handler();
    private final com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory = install(com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory.builder()
            .input("food", stack -> feedValue(stack) > 0).build());
    /** Ticks of walking left on the food eaten last. */
    private int fed;
    private @Nullable UUID walkerId;
    private State state = State.NO_SHEEP;
    private int generating;
    /** The walker's stamina for the window: 0-1000, and its rest left in ticks. */
    private int stamina, restLeft;

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
                case 6 -> state.ordinal();
                case 7 -> stamina;
                case 8 -> Math.min(0xFFFF, restLeft);
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

    public SheepTreadmillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHEEP_TREADMILL.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SheepTreadmillBlockEntity be) {
        be.tick(level, pos, state);
    }

    /** Food: Wheat, or a Hay Bale worth nine. */
    public static int feedValue(ItemStack stack) {
        if (stack.is(Items.WHEAT)) return 1;
        if (stack.is(Items.HAY_BLOCK)) return 9;
        return 0;
    }

    /** The belt's two blocks, a block high: where a sheep can stand on it. */
    public static AABB beltArea(BlockPos back, Direction facing) {
        BlockPos front = back.relative(facing);
        return new AABB(Vec3.atLowerCornerOf(back), Vec3.atLowerCornerOf(back).add(1, 1, 1))
                .minmax(new AABB(Vec3.atLowerCornerOf(front), Vec3.atLowerCornerOf(front).add(1, 1, 1)));
    }

    /** Where the walking sheep is held: in the middle of the belt, on top of it. */
    public static Vec3 lane(BlockPos back, Direction facing) {
        Vec3 centre = Vec3.atCenterOf(back).add(facing.getStepX() * 0.5, 0, facing.getStepZ() * 0.5);
        return new Vec3(centre.x, back.getY() + BELT_TOP, centre.z);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState blockState) {
        energy.resize(ModConfigs.getInt(ModConfigs.SERVER.treadmillCapacity));
        Direction facing = blockState.getValue(SheepTreadmillBlock.FACING);
        long now = level.getGameTime();
        generating = 0;
        stamina = 0;
        restLeft = 0;

        Sheep sheep = walker(level, pos, facing, now);
        if (sheep == null) {
            state = level.getEntitiesOfClass(Sheep.class, beltArea(pos, facing), s -> s.isAlive() && !s.isBaby()).isEmpty()
                    ? State.NO_SHEEP : State.RESTING; // only tired sheep on it
            if (state == State.RESTING) {
                Sheep tired = level.getEntitiesOfClass(Sheep.class, beltArea(pos, facing), s -> s.isAlive() && !s.isBaby()).getFirst();
                restLeft = (int) Math.max(0, tired.getData(ModAttachments.TREADMILL_WALKER).restUntil() - now);
            }
            if ((now + Math.floorMod(pos.hashCode(), ATTRACT_INTERVAL)) % ATTRACT_INTERVAL == 0) { // spread over the ticks
                attract(level, pos, facing, now);
            }
        } else {
            TreadmillWalker walker = sheep.getData(ModAttachments.TREADMILL_WALKER);
            int walkLimit = walkLimit(sheep);
            stamina = (int) (1000L * Math.max(0, walkLimit - walker.walked()) / walkLimit);
            if (energy.getAmountAsInt() >= energy.getCapacityAsInt()) {
                state = State.FULL;
                hold(sheep, pos, facing);
            } else if (fed <= 0 && !eat()) {
                state = State.HUNGRY;
                walkerId = null; // it wanders off to look for food
            } else {
                state = State.WALKING;
                hold(sheep, pos, facing);
                fed--;
                generating = power(sheep);
                energy.generate(generating);
                int walked = walker.walked() + 1;
                if (walked >= walkLimit) {
                    // Worn out: it steps off and rests; the next sheep can take over.
                    sheep.setData(ModAttachments.TREADMILL_WALKER, new TreadmillWalker(0, now + ModConfigs.getInt(ModConfigs.SERVER.treadmillRestTicks)));
                    walkerId = null;
                    nudgeOff(sheep, pos, facing);
                } else {
                    sheep.setData(ModAttachments.TREADMILL_WALKER, new TreadmillWalker(walked, 0));
                }
                setChanged();
            }
        }
        push(level, pos);
        boolean running = state == State.WALKING;
        if (running && !blockState.getValue(SheepTreadmillBlock.RUNNING)) {
            // A sheep has just started walking it: everyone nearby earns "Running Ewe Ragged".
            com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.awardNearby(level, pos,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.AUTO_SHEAR_RADIUS,
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.SHEEP_TREADMILL);
        }
        if (blockState.getValue(SheepTreadmillBlock.RUNNING) != running) {
            level.setBlock(pos, blockState.setValue(SheepTreadmillBlock.RUNNING, running), Block.UPDATE_ALL);
        }
    }

    /** The sheep walking it: the one it already has if it's still on the belt, or a rested grown sheep on it. */
    private @Nullable Sheep walker(ServerLevel level, BlockPos pos, Direction facing, long now) {
        AABB area = beltArea(pos, facing);
        if (walkerId != null && level.getEntity(walkerId) instanceof Sheep current && current.isAlive()
                && current.getBoundingBox().intersects(area.inflate(0.25))) {
            return current;
        }
        walkerId = null;
        List<Sheep> candidates = level.getEntitiesOfClass(Sheep.class, area, s -> s.isAlive() && !s.isBaby() && ready(s, now));
        if (candidates.isEmpty()) return null;
        Sheep chosen = candidates.getFirst();
        walkerId = chosen.getUUID();
        return chosen;
    }

    /** How often a treadmill waiting for a sheep calls one over, in ticks. */
    private static final int ATTRACT_INTERVAL = 20;
    /** How close to the middle of the belt a called sheep must get before it hops on. */
    private static final double HOP_ON = 2.0;
    /** Sheep already called to a treadmill, and until when: so two treadmills don't call the same one. */
    private static final java.util.Map<UUID, Long> CALLED = new java.util.HashMap<>();

    /**
     * With food in it and room for power, the treadmill calls the nearest rested, grown sheep within
     * {@code [sheep_treadmill] attractRadius} over to its belt, as wheat in a player's hand does. Sheep walking or
     * waiting on another treadmill, and ones being led on a lead, are left alone.
     */
    private void attract(ServerLevel level, BlockPos pos, Direction facing, long now) {
        int radius = ModConfigs.getInt(ModConfigs.SERVER.treadmillAttractRadius);
        if (radius <= 0 || (fed <= 0 && feedValue(inventory.get(0)) <= 0) || energy.getAmountAsInt() >= energy.getCapacityAsInt()) return;
        CALLED.values().removeIf(until -> until < now);
        Vec3 lane = lane(pos, facing);
        Sheep nearest = null;
        double best = Double.MAX_VALUE;
        for (Sheep sheep : level.getEntitiesOfClass(Sheep.class, new AABB(lane, lane).inflate(radius), s -> s.isAlive() && !s.isBaby() && !s.isLeashed())) {
            if (sheep.getData(ModAttachments.TREADMILL_WALKER).resting(now) || CALLED.containsKey(sheep.getUUID())) continue;
            if (level.getBlockState(sheep.blockPosition()).getBlock() instanceof SheepTreadmillBlock) continue; // on a treadmill already
            double distance = sheep.distanceToSqr(lane);
            if (distance < best && distance <= radius * radius) {
                best = distance;
                nearest = sheep;
            }
        }
        if (nearest == null) return;
        CALLED.put(nearest.getUUID(), now + ATTRACT_INTERVAL + 5);
        nearest.getLookControl().setLookAt(lane.x, pos.getY() + 1, lane.z);
        if (best <= HOP_ON * HOP_ON && clearWay(level, nearest, lane)) {
            // Beside the belt: pathfinding stops short of a block that isn't a full one, so it hops on.
            nearest.getNavigation().stop();
            nearest.snapTo(lane.x, lane.y, lane.z, facing.toYRot(), nearest.getXRot());
            level.playSound(null, lane.x, lane.y, lane.z, net.minecraft.sounds.SoundEvents.SHEEP_STEP, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, 1.0F);
        } else {
            nearest.getNavigation().moveTo(lane.x, pos.getY(), lane.z, 1.0);
        }
    }

    /** Nothing but the treadmill between the sheep and the belt, so it never hops through a fence or wall. */
    private static boolean clearWay(ServerLevel level, Sheep sheep, Vec3 lane) {
        Vec3 from = sheep.position().add(0, 0.5, 0), to = lane.add(0, 0.5, 0);
        var hit = level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, sheep));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                || level.getBlockState(hit.getBlockPos()).getBlock() instanceof SheepTreadmillBlock;
    }

    /** Whether a sheep has rested enough to walk; a finished rest starts it fresh. */
    private static boolean ready(Sheep sheep, long now) {
        TreadmillWalker walker = sheep.getData(ModAttachments.TREADMILL_WALKER);
        if (walker.resting(now)) return false;
        if (walker.restUntil() != 0) sheep.setData(ModAttachments.TREADMILL_WALKER, TreadmillWalker.FRESH);
        return true;
    }

    /** Ticks this sheep walks before it's worn out: longer with Endurance. */
    public static int walkLimit(Sheep sheep) {
        double bonus = ModConfigs.getDouble(ModConfigs.SERVER.treadmillEnduranceBonus) * Math.max(0, SheepInfusionEvents.getStats(sheep).endurance() - 1);
        return Math.max(1, (int) Math.round(ModConfigs.getInt(ModConfigs.SERVER.treadmillWalkTicks) * (1 + bonus)));
    }

    /** FE/t this sheep makes: more with Speed and while sheared, less with Slowness. */
    public static int power(Sheep sheep) {
        double factor = 1.0;
        double perLevel = ModConfigs.getDouble(ModConfigs.SERVER.treadmillSpeedBonus);
        MobEffectInstance speed = sheep.getEffect(MobEffects.SPEED);
        if (speed != null) factor += perLevel * (speed.getAmplifier() + 1);
        MobEffectInstance slow = sheep.getEffect(MobEffects.SLOWNESS);
        if (slow != null) factor -= perLevel * (slow.getAmplifier() + 1);
        if (sheep.isSheared()) factor += ModConfigs.getDouble(ModConfigs.SERVER.treadmillShearedBonus);
        return (int) Math.round(ModConfigs.getInt(ModConfigs.SERVER.treadmillPower) * Math.max(0.1, factor));
    }

    /** Eats the next Wheat or Hay Bale, if there is one. */
    private boolean eat() {
        ItemStack food = inventory.get(0);
        int value = feedValue(food);
        if (value <= 0) return false;
        food.shrink(1);
        fed += value * ModConfigs.getInt(ModConfigs.SERVER.treadmillTicksPerWheat);
        setChanged();
        return true;
    }

    /** Keeps the sheep in the middle of the belt, facing the wheat. */
    private static void hold(Sheep sheep, BlockPos pos, Direction facing) {
        Vec3 lane = lane(pos, facing);
        sheep.getNavigation().stop();
        sheep.setDeltaMovement(Vec3.ZERO);
        float yaw = facing.toYRot();
        sheep.snapTo(lane.x, lane.y, lane.z, yaw, sheep.getXRot());
        sheep.setYHeadRot(yaw);
        sheep.setYBodyRot(yaw);
    }

    /** A worn-out sheep steps off the back of the belt. */
    private static void nudgeOff(Sheep sheep, BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        sheep.setDeltaMovement(back.getStepX() * 0.3, 0.25, back.getStepZ() * 0.3);
    }

    private void push(ServerLevel level, BlockPos pos) {
        energy.pushToNeighbours(level, pos, ModConfigs.getInt(ModConfigs.SERVER.treadmillOutput), (dir, target) -> level.getBlockState(target).getBlock() instanceof SheepTreadmillBlock); // not its own front, nor other treadmills
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

    public ItemStack food() {
        return inventory.get(0);
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory inventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.sheep_treadmill");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SheepTreadmillMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); // its modules: energy and food
        fed = input.getIntOr("fed", 0);
        walkerId = input.read("walker", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); // its modules
        output.putInt("fed", fed);
        if (walkerId != null) output.store("walker", UUIDUtil.CODEC, walkerId);
    }
}
