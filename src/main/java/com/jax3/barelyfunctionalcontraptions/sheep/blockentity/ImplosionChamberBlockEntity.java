package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ImplosionChamberBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.ExplosiveSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.machine.EnergyModule;
import com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineInventory;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * The Implosion Chamber: cracks Cobblestone into Gravel, and Gravel into Sand, with the blasts of the Explosive sheep
 * in its pen.
 *
 * <ul>
 *     <li><b>Pen:</b> the 5x5 round it ({@code radius} 2), at its own level and one up.</li>
 *     <li><b>Sparks:</b> every {@code sparkInterval} ticks (5 s), while it has stone to crack, room for what it makes
 *     and {@code sparkEnergy} FE (1,000), it lights the fuse of the nearest Explosive sheep in its pen that has its
 *     fleece on. A sheep lit by anything else (fire, a player's flint and steel) counts just the same.</li>
 *     <li><b>Blasts:</b> when an Explosive sheep pops in its pen, its chamber catches the blast and cracks up to
 *     {@code batch} (8) items a step: first Gravel it cracked earlier into Sand, then Cobblestone from its input into
 *     Gravel. Set to stop at Gravel ({@link #setTarget}), its Gravel goes straight to the output instead. The sheep's
 *     wool comes off as usual (it lands in the pen), and it can't blast again until its wool grows back, so a
 *     Grass Seeder in the pen keeps it busy.</li>
 *     <li><b>Slots:</b> an input (Cobblestone, or Gravel to make Sand) that hoppers and pipes fill, the Gravel it has
 *     cracked and is waiting to crack again, the output, and the Explosive Wool its sheep's blasts blow off (it catches
 *     it rather than leave it on the ground); hoppers and pipes empty both outputs.</li>
 * </ul>
 * Its comparator signal reads how full its output is.
 */
public class ImplosionChamberBlockEntity extends com.jax3.barelyfunctionalcontraptions.sheep.machine.MachineFrame
        implements com.jax3.barelyfunctionalcontraptions.sheep.machine.InventoryHolder, AreaDisplay, net.minecraft.world.MenuProvider,
        com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable {
    public static final int AREA_COLOR = 0xFFE3533A;
    public static final int INPUT = 0, CRACKED = 1, OUTPUT = 2, WOOL = 3;
    /** Ticks its porthole glows after a blast. */
    public static final int FLASH_TICKS = 20;
    /** Values synced to its window ({@link com.jax3.barelyfunctionalcontraptions.sheep.menu.ImplosionChamberMenu}). */
    public static final int DATA_COUNT = 16;

    /** What it's doing, for its window, Jade and the status panel. */
    public enum Status {
        /** It lit a sheep or caught a blast lately. */
        CRACKING(0xFF8FE07A),
        /** It has stone, but every Explosive sheep in its pen is waiting for its wool to grow back. */
        WAITING(0xFFE0B04A),
        /** No Explosive sheep in its pen. */
        NO_SHEEP(0xFFE35D5D),
        /** Nothing to crack. */
        IDLE(0xFFAEB4C6),
        /** Its output is full. */
        FULL(0xFFE35D5D),
        /** Not enough power for a spark. */
        NO_POWER(0xFFE35D5D);

        public final int color;

        Status(int color) {
            this.color = color;
        }

        public static Status byId(int id) {
            Status[] all = values();
            return all[Math.floorMod(id, all.length)];
        }
    }

    /** What it makes: cracking stops at Gravel, or goes on to Sand. */
    public enum Target {
        SAND, GRAVEL;

        public static Target byId(int id) {
            Target[] all = values();
            return all[Math.floorMod(id, all.length)];
        }
    }

    /** Chambers that are loaded, to find the one whose pen a sheep popped in. */
    private static final Set<ImplosionChamberBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    static {
        ExplosiveSheep.addListener(ImplosionChamberBlockEntity::onPop);
    }

    private final MachineEnergy energy = install(EnergyModule.of(new MachineEnergy(
            () -> ModConfigs.getInt(ModConfigs.SERVER.implosionCapacity), () -> ModConfigs.getInt(ModConfigs.SERVER.implosionMaxInput), this::setChanged))).handler();
    private final MachineInventory inventory = install(MachineInventory.builder()
            .input("input", this::acceptsInput)
            .slot("cracked", stack -> false, false)
            .output("output")
            .output("wool")
            .build());
    /** Its two upgrade cards: Speed (sparks sooner), Range (a wider pen) and Muffler (quiet blasts). */
    private final com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades = install(new com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades(this));

    private Target target = Target.SAND;
    private Status status = Status.IDLE;
    private int cooldown;
    private int flash;
    private boolean showArea;
    private long blasts;
    private long cracked;
    private int lastSignal = -1;
    /** Explosive sheep in its pen at the last spark: ready (fleece on) and regrowing their wool. */
    private int readySheep, regrowingSheep;

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            long blastsCapped = Math.min(Integer.MAX_VALUE, blasts), crackedCapped = Math.min(Integer.MAX_VALUE, cracked);
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> status.ordinal();
                case 5 -> showArea ? 1 : 0;
                case 6 -> target.ordinal();
                case 7 -> Math.max(0, cooldown);
                case 8 -> sparkTicks();
                case 9 -> readySheep;
                case 10 -> regrowingSheep;
                case 11 -> (int) (blastsCapped & 0xFFFF);
                case 12 -> (int) (blastsCapped >>> 16);
                case 13 -> (int) (crackedCapped & 0xFFFF);
                case 14 -> (int) (crackedCapped >>> 16);
                case 15 -> flash;
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

    public ImplosionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities.IMPLOSION_CHAMBER.get(), pos, state);
    }

    // ------------------------------------------------------------------ settings

    public static int radius() {
        return ModConfigs.getInt(ModConfigs.SERVER.implosionRadius);
    }

    public static int sparkInterval() {
        return ModConfigs.getInt(ModConfigs.SERVER.implosionSparkInterval);
    }

    public static int sparkEnergy() {
        return ModConfigs.getInt(ModConfigs.SERVER.implosionSparkEnergy);
    }

    public static int batch() {
        return ModConfigs.getInt(ModConfigs.SERVER.implosionBatch);
    }

    /** What a blast cracks {@code item} into: Cobblestone into Gravel, Gravel into Sand; null for anything else. */
    public static Item crackedInto(ItemStack stack) {
        if (stack.is(Items.COBBLESTONE)) return Items.GRAVEL;
        if (stack.is(Items.GRAVEL)) return Items.SAND;
        return null;
    }

    private boolean acceptsInput(ItemStack stack) {
        return stack.is(Items.COBBLESTONE) || (stack.is(Items.GRAVEL) && target == Target.SAND);
    }

    /** Ticks between its sparks, with its cards (a Speed card sparks sooner). */
    public int sparkTicks() {
        return upgradeEffects().ticks(sparkInterval());
    }

    /** FE a spark takes, with its cards. */
    public int sparkCost() {
        return upgradeEffects().energy(sparkEnergy());
    }

    /** Its pen, with a Range card's extra blocks. */
    public AABB pen() {
        return pen(worldPosition, rangeBonus());
    }

    /** Its pen: the {@code radius} round it, at its own level and one up. */
    public static AABB pen(BlockPos pos) {
        return pen(pos, 0);
    }

    /** Its pen, {@code bonus} blocks wider each side. */
    public static AABB pen(BlockPos pos, int bonus) {
        int r = radius() + bonus;
        return new AABB(pos.getX() - r, pos.getY(), pos.getZ() - r, pos.getX() + r + 1, pos.getY() + 2, pos.getZ() + r + 1);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ImplosionChamberBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        LOADED.add(this);
        energy.refreshLimits();
        if (flash > 0) flash--;
        boolean glowing = flash > 0;
        if (state.getValue(ImplosionChamberBlock.BLASTING) != glowing) {
            level.setBlock(pos, state.setValue(ImplosionChamberBlock.BLASTING, glowing), Block.UPDATE_CLIENTS);
        }
        if (--cooldown <= 0) {
            cooldown = sparkTicks();
            spark(level, pos);
        }
        int signal = comparatorSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** Lights the nearest ready Explosive sheep in its pen, if it has work, room and power; sets its status either way. */
    private void spark(ServerLevel level, BlockPos pos) {
        List<Sheep> explosive = level.getEntitiesOfClass(Sheep.class, pen(), s -> s.isAlive() && !s.isBaby()
                && com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents.getInfusedResource(s)
                        .map(r -> r.appearance().has(com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature.FUSE)).orElse(false));
        Status was = status;
        readySheep = (int) explosive.stream().filter(s -> !s.isSheared()).count();
        regrowingSheep = explosive.size() - readySheep;
        if (!hasWork()) {
            status = Status.IDLE;
        } else if (!hasRoom()) {
            status = Status.FULL;
        } else if (explosive.isEmpty()) {
            status = Status.NO_SHEEP;
        } else if (explosive.stream().anyMatch(ExplosiveSheep::isLit)) {
            status = Status.CRACKING; // one is already hissing: wait for it
        } else {
            Vec3 centre = Vec3.atCenterOf(pos);
            Sheep ready = explosive.stream().filter(ExplosiveSheep::hasFuse)
                    .min(Comparator.comparingDouble(s -> s.distanceToSqr(centre))).orElse(null);
            if (ready == null) {
                status = Status.WAITING;
            } else if (!energy.consume(sparkCost())) {
                status = Status.NO_POWER;
            } else {
                ExplosiveSheep.light(ready, muffled());
                if (!muffled()) level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
                status = Status.CRACKING;
            }
        }
        if (status != was) setChanged();
    }

    /** A sheep popped somewhere: the loaded chamber whose pen it was in (the nearest, if pens overlap) catches it. */
    private static void onPop(ServerLevel level, Sheep sheep, BlockPos at, List<ItemStack> wool) {
        ImplosionChamberBlockEntity catcher = null;
        double best = Double.MAX_VALUE;
        for (ImplosionChamberBlockEntity be : List.copyOf(LOADED)) {
            if (be.isRemoved() || be.getLevel() != level || !be.pen().contains(sheep.position().add(0, 0.1, 0))) continue;
            double d = be.getBlockPos().distToCenterSqr(sheep.position());
            if (d < best) {
                best = d;
                catcher = be;
            }
        }
        if (catcher != null) {
            catcher.catchBlast(level);
            catcher.catchWool(wool);
        }
    }

    /** Takes as much of the blown-off {@code wool} into its wool slot as fits (shrinking the stacks). */
    public void catchWool(List<ItemStack> wool) {
        for (ItemStack stack : wool) {
            if (stack.isEmpty()) continue;
            ItemStack held = inventory.get(WOOL);
            if (held.isEmpty()) {
                inventory.set(WOOL, stack.split(stack.getMaxStackSize()));
            } else if (ItemStack.isSameItemSameComponents(held, stack)) {
                int n = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                held.grow(n);
                stack.shrink(n);
            }
        }
        setChanged();
    }

    /**
     * Catches a blast: cracks up to {@code batch} of its earlier Gravel into Sand, then up to {@code batch} of its input a
     * step (Cobblestone into Gravel, Gravel into Sand). Returns how many items it cracked.
     */
    public int catchBlast(ServerLevel level) {
        int batch = batch(), done = 0;
        ItemStack crackedStack = inventory.get(CRACKED);
        // First the Gravel it cracked last time, into Sand (or, stopping at Gravel, straight out as it is).
        if (!crackedStack.isEmpty()) {
            Item into = target == Target.SAND ? Items.SAND : Items.GRAVEL;
            int n = Math.min(Math.min(batch, crackedStack.getCount()), room(into));
            if (n > 0) {
                addOutput(into, n);
                crackedStack.shrink(n);
                if (crackedStack.isEmpty()) inventory.set(CRACKED, ItemStack.EMPTY);
                if (into == Items.SAND) done += n;
            }
        }
        // Then its input, one step.
        ItemStack input = inventory.get(INPUT);
        Item into = input.isEmpty() ? null : crackedInto(input);
        if (into != null) {
            int n = Math.min(batch, input.getCount());
            if (into == Items.GRAVEL && target == Target.SAND) {
                // Cobblestone to Gravel, kept to crack into Sand with the next blast.
                ItemStack held = inventory.get(CRACKED);
                n = Math.min(n, held.isEmpty() ? Items.GRAVEL.getDefaultMaxStackSize() : held.getMaxStackSize() - held.getCount());
                if (n > 0) {
                    if (held.isEmpty()) inventory.set(CRACKED, new ItemStack(Items.GRAVEL, n));
                    else held.grow(n);
                }
            } else {
                n = Math.min(n, room(into));
                if (n > 0) addOutput(into, n);
            }
            if (n > 0) {
                input.shrink(n);
                if (input.isEmpty()) inventory.set(INPUT, ItemStack.EMPTY);
                done += n;
            }
        }
        blasts++;
        cracked += done;
        flash = FLASH_TICKS;
        status = Status.CRACKING;
        Vec3 c = Vec3.atCenterOf(worldPosition);
        if (!muffled()) level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.5F, 0.7F);
        level.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.6, c.z, 10, 0.25, 0.2, 0.25, 0.02);
        if (done > 0) {
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                    net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState()), c.x, c.y + 0.6, c.z, 16, 0.3, 0.3, 0.3, 0.1);
        }
        setChanged();
        return done;
    }

    private int room(Item item) {
        ItemStack out = inventory.get(OUTPUT);
        if (out.isEmpty()) return item.getDefaultMaxStackSize();
        return out.is(item) ? out.getMaxStackSize() - out.getCount() : 0;
    }

    private void addOutput(Item item, int count) {
        ItemStack out = inventory.get(OUTPUT);
        if (out.isEmpty()) inventory.set(OUTPUT, new ItemStack(item, count));
        else out.grow(count);
    }

    /** Whether it has anything to crack. */
    public boolean hasWork() {
        return !inventory.get(INPUT).isEmpty() || !inventory.get(CRACKED).isEmpty();
    }

    /** Whether the next blast has somewhere to put what it makes. */
    public boolean hasRoom() {
        ItemStack held = inventory.get(CRACKED);
        if (!held.isEmpty()) return room(target == Target.SAND ? Items.SAND : Items.GRAVEL) > 0;
        ItemStack input = inventory.get(INPUT);
        if (input.isEmpty()) return true;
        Item into = crackedInto(input);
        if (into == Items.GRAVEL && target == Target.SAND) return held.getCount() < held.getMaxStackSize() || held.isEmpty();
        return into != null && room(into) > 0;
    }

    // ------------------------------------------------------------------ accessors

    public Status status() {
        return status;
    }

    public Target target() {
        return target;
    }

    /** Sets what it makes. Stopping at Gravel sends any Gravel it holds out as it is with the next blast. */
    public void setTarget(Target target) {
        this.target = target;
        setChanged();
    }

    public long blasts() {
        return blasts;
    }

    public long crackedTotal() {
        return cracked;
    }

    public boolean flashing() {
        return flash > 0;
    }

    @Override
    public MachineInventory inventory() {
        return inventory;
    }

    public int readySheep() {
        return readySheep;
    }

    public int regrowingSheep() {
        return regrowingSheep;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.implosion_chamber");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
                                                                       net.minecraft.world.entity.player.Player player) {
        return new com.jax3.barelyfunctionalcontraptions.sheep.menu.ImplosionChamberMenu(containerId, inventory, this, data);
    }

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    /** Puts as much of {@code stack} into its input as fits, taking it from the stack; how many went in. */
    public int insertInput(ItemStack stack) {
        if (!acceptsInput(stack)) return 0;
        ItemStack input = inventory.get(INPUT);
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) return 0;
        int room = input.isEmpty() ? stack.getMaxStackSize() : input.getMaxStackSize() - input.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) return 0;
        if (input.isEmpty()) inventory.set(INPUT, stack.copyWithCount(moved));
        else input.grow(moved);
        stack.shrink(moved);
        setChanged();
        return moved;
    }

    /** How full its output is, 0-15. */
    public int comparatorSignal() {
        ItemStack out = inventory.get(OUTPUT);
        return out.isEmpty() ? 0 : Math.max(1, out.getCount() * 15 / out.getMaxStackSize());
    }

    /** One line on how it's doing, for right-clicking it. */
    public Component summary() {
        return Component.translatable("message.barelyfunctionalcontraptions.implosion_chamber.summary",
                Component.translatable("gui.barelyfunctionalcontraptions.implosion_chamber.status." + status.name().toLowerCase(java.util.Locale.ROOT)),
                Component.translatable("gui.barelyfunctionalcontraptions.implosion_chamber.target." + target.name().toLowerCase(java.util.Locale.ROOT)),
                blasts, cracked);
    }

    /** Fills its power at once (GameTests, the screenshot harness). */
    public void fillEnergy() {
        energy.refreshLimits();
        energy.fill();
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
        return pen();
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades() {
        return upgrades;
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(this);
    }

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in); // its modules: energy and slots
        target = Target.byId(in.getIntOr("target", 0));
        blasts = in.getLongOr("blasts", 0L);
        cracked = in.getLongOr("cracked_total", 0L);
        showArea = in.getBooleanOr("show_area", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out); // its modules
        out.putInt("target", target.ordinal());
        out.putLong("blasts", blasts);
        out.putLong("cracked_total", cracked);
        out.putBoolean("show_area", showArea);
    }

    /** Clients need the area setting and its cards (a Range card widens the outline), for the outline. */
    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putBoolean("show_area", showArea);
    }
}
