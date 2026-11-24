package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.jax3.barelyfunctionalcontraptions.sheep.blocks.PoweredFurnaceBlock;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.furnace.FurnaceHeatManager;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.PoweredFurnaceMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModBlockEntities;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * The Powered Furnace: smelts anything a Furnace can, on Forge Energy instead of fuel.
 *
 * <ul>
 *     <li><b>Speed:</b> each item takes {@code [powered_furnace] cookTimeMultiplier} (0.35) of its Furnace time, so a
 *     10-second smelt takes 3.5 seconds.</li>
 *     <li><b>Blaze sheep:</b> heat sources (the Blaze Sheep Furnace's: Blaze sheep with their fleece on) with their
 *     feet in the 5x5 area around it speed it up: +{@code blazeBonus} (25%) for the first, each one after it adding a
 *     little less, up to {@code maxHeatSources}. It uses the same power per tick, so the sheep save power too.</li>
 *     <li><b>Power:</b> {@code energyPerTick} FE while smelting; it pauses without power.</li>
 *     <li><b>Experience:</b> kept until a player takes the output out of its window, as a Furnace does.</li>
 * </ul>
 * Hoppers and pipes put items in on any side and take the results out on any side; a comparator reads the input.
 */
public class PoweredFurnaceBlockEntity extends BlockEntity implements com.jax3.barelyfunctionalcontraptions.sheep.upgrade.Upgradeable, MachineContainer, WorldlyContainer, MenuProvider, AreaDisplay {
    /** Its two upgrade cards (see {@link com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades}). */
    private final com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades = new com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades(this);
    public static final int INPUT = 0, OUTPUT = 1;
    public static final int DATA_COUNT = 13;
    public static final int SCAN_INTERVAL = 10;
    public static final int AREA_COLOR = 0xFFFF8A1F;
    private static final int[] SLOTS = {INPUT, OUTPUT};

    private final MachineEnergy energy = new MachineEnergy(
            () -> ModConfigs.getInt(ModConfigs.SERVER.poweredFurnaceCapacity), () -> ModConfigs.getInt(ModConfigs.SERVER.poweredFurnaceMaxInput), this::setChanged);
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    /** Progress in hundredths of a tick, so the Blaze sheep's speed-up can be fractional. */
    private int progress;
    private int maxProgress;
    /** Speed in hundredths: 100 alone, more with Blaze sheep. */
    private int speed = 100;
    private int[] heaters = new int[0];
    private int scanCooldown;
    private float experience;
    private boolean showArea;
    private MachineStatus status = MachineStatus.IDLE;
    private @Nullable RecipeHolder<SmeltingRecipe> cachedRecipe;
    private ItemStack cachedFor = ItemStack.EMPTY;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getAmountAsInt() & 0xFFFF;
                case 1 -> energy.getAmountAsInt() >>> 16;
                case 2 -> energy.getCapacityAsInt() & 0xFFFF;
                case 3 -> energy.getCapacityAsInt() >>> 16;
                case 4 -> Math.min(0xFFFF, progress / 100);
                case 5 -> Math.min(0xFFFF, maxProgress / 100);
                case 6 -> upgradedEnergyPerTick();
                case 7 -> status.ordinal();
                case 8 -> speed;
                case 9 -> heaters.length;
                case 10 -> maxSources();
                case 11 -> showArea ? 1 : 0;
                case 12 -> (int) Math.round(cookMultiplier() * 100);
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

    public PoweredFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWERED_FURNACE.get(), pos, state);
    }

    public static double cookMultiplier() {
        return ModConfigs.getDouble(ModConfigs.SERVER.poweredFurnaceCookTime);
    }

    /** FE a tick while smelting, with its upgrade cards: enough that a smelt costs what the cards say. */
    public int upgradedEnergyPerTick() {
        return upgrades.effects().perTick(energyPerTick());
    }

    public static int energyPerTick() {
        return ModConfigs.getInt(ModConfigs.SERVER.poweredFurnaceEnergyPerTick);
    }

    public static int maxSources() {
        return ModConfigs.getInt(ModConfigs.SERVER.poweredFurnaceMaxHeaters);
    }

    /** Speed in hundredths with {@code count} Blaze sheep: 100, plus the bonus for each with the furnace's falloff. */
    public static int speedWith(int count) {
        double bonus = ModConfigs.getDouble(ModConfigs.SERVER.poweredFurnaceBlazeBonus) * 100;
        return 100 + BlazeFurnaceBlockEntity.combine(Collections.nCopies(count, bonus), maxSources(),
                ModConfigs.getDouble(ModConfigs.SERVER.blazeFurnaceFalloff));
    }

    /** The area Blaze sheep must stand in: the Blaze Sheep Furnace's, around the furnace and on top of it. */
    public static AABB heatingArea(BlockPos pos) {
        return BlazeFurnaceBlockEntity.heatingArea(pos);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PoweredFurnaceBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        energy.refreshLimits(0, upgrades.effects().capacity()); // a Capacity card holds and takes in more
        if (--scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL;
            scan(level, pos, state);
        }
        Optional<RecipeHolder<SmeltingRecipe>> recipe = recipeFor(level, input);
        if (input.isEmpty()) {
            status = MachineStatus.IDLE;
            progress = 0;
        } else if (recipe.isEmpty()) {
            status = MachineStatus.INVALID_INPUT;
            progress = 0;
        } else {
            SmeltingRecipe r = recipe.get().value();
            ItemStack result = r.assemble(new SingleRecipeInput(input));
            maxProgress = Math.max(100, (int) Math.round(r.cookingTime() * cookMultiplier() * upgrades.effects().time() * 100));
            if (!fits(result)) {
                status = MachineStatus.OUTPUT_FULL;
            } else if (energy.getAmountAsInt() < upgradedEnergyPerTick()) {
                status = MachineStatus.NO_ENERGY;
            } else {
                status = MachineStatus.PROCESSING;
                energy.drain(upgradedEnergyPerTick());
                progress += speed;
                if (progress >= maxProgress) {
                    progress = 0;
                    input.shrink(1);
                    if (input.isEmpty()) input = ItemStack.EMPTY;
                    if (output.isEmpty()) output = result.copy();
                    else output.grow(result.getCount());
                    experience += r.experience();
                }
                setChanged();
            }
        }
        boolean lit = status == MachineStatus.PROCESSING;
        if (state.getValue(PoweredFurnaceBlock.LIT) != lit) level.setBlock(pos, state.setValue(PoweredFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
    }

    /** Counts the heat sources (Blaze sheep) in the area and works out the speed. */
    public void scan(ServerLevel level, BlockPos pos, BlockState state) {
        AABB area = heatingArea(pos);
        java.util.List<Entity> found = level.getEntities((Entity) null, area, e -> area.contains(e.position()) && FurnaceHeatManager.heatOf(e).isPresent());
        int[] ids = found.stream().limit(maxSources()).mapToInt(Entity::getId).sorted().toArray();
        // Keep its Blaze sheep close: call strays back while it has room for more.
        SheepAttraction.claim(found, level.getGameTime());
        SheepAttraction.attract(level, area, s -> FurnaceHeatManager.heatOf(s).isPresent(), maxSources() - found.size(), level.getGameTime());
        speed = speedWith(ids.length);
        if (!Arrays.equals(ids, heaters)) {
            heaters = ids;
            setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private Optional<RecipeHolder<SmeltingRecipe>> recipeFor(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (cachedRecipe != null && ItemStack.isSameItemSameComponents(cachedFor, stack)) return Optional.of(cachedRecipe);
        Optional<RecipeHolder<SmeltingRecipe>> found = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
        cachedRecipe = found.orElse(null);
        cachedFor = stack.copyWithCount(1);
        return found;
    }

    /** Whether {@code stack} can be smelted, on either side (the client checks against the recipes it was sent). */
    public static boolean canSmelt(Level level, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (level instanceof ServerLevel server) {
            return server.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), server).isPresent();
        }
        return com.jax3.barelyfunctionalcontraptions.sheep.recipe.SeparationLookup.clientRecipeMap()
                .getRecipesFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).findAny().isPresent();
    }

    private boolean fits(ItemStack result) {
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    /** Gives {@code player} the experience from everything smelted since it was last taken, as a Furnace does. */
    public void awardExperience(ServerLevel level, Player player) {
        int whole = Mth.floor(experience);
        float part = experience - whole;
        if (part > 0 && level.getRandom().nextFloat() < part) whole++;
        experience = 0;
        if (whole > 0) ExperienceOrb.award(level, player.position(), whole);
        setChanged();
    }

    // ------------------------------------------------------------------ accessors

    public EnergyHandler getEnergyHandler() {
        return energy;
    }

    public MachineStatus status() {
        return status;
    }

    public int speed() {
        return speed;
    }

    public int heaterCount() {
        return heaters.length;
    }

    /** Ids of the Blaze sheep heating it (client side too, for the flames). */
    public int[] heaters() {
        return heaters;
    }

    public ItemStack input() {
        return input;
    }

    public ItemStack output() {
        return output;
    }

    public float storedExperience() {
        return experience;
    }

    public int getComparatorSignal() {
        if (input.isEmpty()) return 0;
        return Math.max(1, Math.round(15.0F * input.getCount() / input.getMaxStackSize()));
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
        return heatingArea(worldPosition);
    }

    @Override
    public int areaColor() {
        return AREA_COLOR;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.barelyfunctionalcontraptions.powered_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PoweredFurnaceMenu(containerId, inventory, this, data);
    }

    // ------------------------------------------------------------------ container

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == INPUT ? input : slot == OUTPUT ? output : ItemStack.EMPTY;
    }

    @Override
    public void putItem(int slot, ItemStack stack) {
        if (slot == INPUT) {
            if (!ItemStack.isSameItemSameComponents(input, stack)) progress = 0;
            input = stack;
        } else if (slot == OUTPUT) {
            output = stack;
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT && level != null && canSmelt(level, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** Hoppers and pipes take only the results, never what's waiting to be smelted. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, this);
        if (level instanceof ServerLevel server && experience >= 1) ExperienceOrb.award(server, net.minecraft.world.phys.Vec3.atCenterOf(pos), Mth.floor(experience));
    }

    // ------------------------------------------------------------------ energy

    // ------------------------------------------------------------------ persistence and sync

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        upgrades.load(in);
        in.child("energy").ifPresent(energy::deserialize);
        input = in.read("input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        output = in.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        progress = in.getIntOr("progress", 0);
        experience = in.getFloatOr("experience", 0F);
        showArea = in.getBooleanOr("show_area", false);
        heaters = in.getIntArray("heaters").orElse(new int[0]);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        upgrades.save(out);
        energy.serialize(out.child("energy"));
        if (!input.isEmpty()) out.store("input", ItemStack.OPTIONAL_CODEC, input);
        if (!output.isEmpty()) out.store("output", ItemStack.OPTIONAL_CODEC, output);
        out.putInt("progress", progress);
        out.putFloat("experience", experience);
        out.putBoolean("show_area", showArea);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("show_area", showArea);
        tag.putIntArray("heaters", heaters);
        return tag;
    }

    @Override
    public com.jax3.barelyfunctionalcontraptions.sheep.upgrade.MachineUpgrades upgrades() {
        return upgrades;
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
