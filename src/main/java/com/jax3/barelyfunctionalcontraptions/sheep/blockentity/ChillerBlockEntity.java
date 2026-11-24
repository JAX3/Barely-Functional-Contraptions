package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.ChillerBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

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
public class ChillerBlockEntity extends BlockEntity implements com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable, MachineContainer, WorldlyContainer, AreaDisplay, net.minecraft.world.MenuProvider {
    /** Its two upgrade cards (see {@link com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades}). */
    private final com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades = new com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades(this);
    /** On a client: the blocks its Range cards add to its area, as last synced. */
    private int syncedRange;
    public static final int SCAN_INTERVAL = 10;
    public static final int AREA_COLOR = 0xFF7FD8FF;
    private static final int[] SLOTS = {0};
    /** Values synced to the window ({@link com.jax3.barelyfunctionalcontraptions.sheep.menu.ChillerMenu}). */
    public static final int DATA_COUNT = 10;
    public static final FluidResource WATER = WaterTank.WATER;

    private final WaterTank tank = new WaterTank(8_000, this::setChanged);
    private ItemStack output = ItemStack.EMPTY;
    /** Progress towards the next Ice, in hundredths of a tick at one sheep's speed. */
    private int progress;
    /** Speed from the cold, in hundredths (100 = one Snow sheep), set on each scan. */
    private int cold;
    private int[] chillers = new int[0];
    private int scanCooldown;
    private boolean showArea;
    private MachineStatus status = MachineStatus.IDLE;
    private int lastSignal;

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
        boolean room = output.isEmpty() || (output.is(Items.ICE) && output.getCount() < output.getMaxStackSize());
        if (cold <= 0) {
            status = MachineStatus.IDLE;
        } else if (!room) {
            status = MachineStatus.OUTPUT_FULL;
        } else if (tank.getAmount() < waterPerIce()) {
            status = MachineStatus.NO_ENERGY; // shown as "No water"
        } else {
            status = MachineStatus.PROCESSING;
            progress += cold;
            if (progress >= ticksPerIce() * 100) {
                progress = 0;
                tank.drain(waterPerIce());
                if (output.isEmpty()) output = new ItemStack(Items.ICE);
                else output.grow(1);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GLASS_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.4F);
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

    public ItemStack output() {
        return output;
    }

    public int comparatorSignal() {
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

    // ------------------------------------------------------------------ container (the Ice output)

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? output : ItemStack.EMPTY;
    }

    @Override
    public void putItem(int slot, ItemStack stack) {
        if (slot == 0) output = stack;
    }

    /** Nothing goes in: the slot is the Ice it makes. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    /** Hoppers and pipes take the Ice out of any side. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, this);
    }

    // ------------------------------------------------------------------ water

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        upgrades.load(input);
        syncedRange = input.getIntOr("upgrade_range", 0);
        input.child("tank").ifPresent(tank::deserialize);
        output = input.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        progress = input.getIntOr("progress", 0);
        showArea = input.getBooleanOr("show_area", false);
        chillers = input.getIntArray("chillers").orElse(new int[0]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        upgrades.save(output);
        tank.serialize(output.child("tank"));
        if (!this.output.isEmpty()) output.store("output", ItemStack.OPTIONAL_CODEC, this.output);
        output.putInt("progress", progress);
        output.putBoolean("show_area", showArea);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Clients get the area setting and the Snow sheep (for the snowflakes). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("show_area", showArea);
        tag.putInt("upgrade_range", upgrades.effects().range());
        tag.putIntArray("chillers", chillers);
        return tag;
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades() {
        return upgrades;
    }

    @Override
    public int rangeBonus() {
        return level != null && level.isClientSide() ? syncedRange : upgrades.effects().range();
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!upgrades.isEmpty()) components.set(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.MACHINE_UPGRADES.get(), upgrades.toComponent());
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var cards = components.get(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModDataComponents.MACHINE_UPGRADES.get());
        if (cards != null) upgrades.fromComponent(cards);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("upgrades");
    }
}
