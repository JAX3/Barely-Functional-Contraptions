package com.jax3.barelyfunctionalcontraptions.sheep.upgrade;

import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.items.UpgradeItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A machine's two upgrade slots: one card each, only cards its block takes (see {@link UpgradeType#machines()}).
 * Saved with the machine and kept on its item when it's broken or picked up (as {@code machine_upgrades}). Pipes and
 * hoppers never see these slots: they belong to the window only.
 */
public class MachineUpgrades extends SimpleContainer {
    public static final int SIZE = 2;
    private final BlockEntity owner;

    public MachineUpgrades(BlockEntity owner) {
        super(SIZE);
        this.owner = owner;
    }

    /** Whether a machine with state {@code state} takes {@code stack} as an upgrade. */
    public static boolean accepts(BlockState state, ItemStack stack) {
        return stack.getItem() instanceof UpgradeItem card && state.is(card.type().machines());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(owner.getBlockState(), stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(owner, player);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        owner.setChanged();
        // Clients draw the working area, which a Range card changes.
        var level = owner.getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3);
            // Cards only go in through the window, so whoever put one in is standing by it.
            if (level instanceof net.minecraft.server.level.ServerLevel server && !isEmpty()) {
                com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.awardNearby(server, owner.getBlockPos(), 8,
                        com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.SOME_ASSEMBLY_REQUIRED);
                if (cards().stream().filter(c -> c.getItem() instanceof UpgradeItem card && card.type() == UpgradeType.SPEED).count() >= 2) {
                    com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.awardNearby(server, owner.getBlockPos(), 8,
                            com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAdvancements.MAXIMUM_OVERDRIVE);
                }
            }
        }
    }

    /** What the installed cards do: each kind's effect, a second card of a kind at {@code secondCardStrength}. */
    public UpgradeEffects effects() {
        return effectsOf(getItems());
    }

    /** {@link #effects()} for a list of cards. */
    public static UpgradeEffects effectsOf(List<ItemStack> cards) {
        Map<UpgradeType, Integer> counts = new EnumMap<>(UpgradeType.class);
        for (ItemStack stack : cards) {
            if (stack.getItem() instanceof UpgradeItem card) counts.merge(card.type(), 1, Integer::sum);
        }
        if (counts.isEmpty()) return UpgradeEffects.NONE;
        double second = ModConfigs.getDouble(ModConfigs.SERVER.secondUpgradeStrength);
        double time = 1, energy = 1, capacity = 1, range = 0, yield = 0;
        boolean muffled = false;
        for (var entry : counts.entrySet()) {
            UpgradeType type = entry.getKey();
            for (int n = 0; n < entry.getValue(); n++) {
                double strength = n == 0 ? 1 : second;
                time *= 1 + (type.time() - 1) * strength;
                energy *= 1 + (type.energy() - 1) * strength;
                capacity *= 1 + (type.capacity() - 1) * strength;
                range += type.range() * strength;
                yield += type.yield() * strength;
                if (type == UpgradeType.MUFFLER) muffled = true;
            }
        }
        return new UpgradeEffects(time, energy, capacity, (int) Math.round(range), yield, muffled);
    }

    /** The installed cards, for a tooltip or Jade. */
    public List<ItemStack> cards() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : getItems()) if (!stack.isEmpty()) list.add(stack);
        return list;
    }

    // ---------------------------------------------------------------- what clients need (the outline, its sounds)

    /** On a client: the range and muffling last synced from the server (its own copy of the cards isn't kept). */
    private int syncedRange;
    private boolean syncedMuffled;

    private boolean onClient() {
        return owner.getLevel() != null && owner.getLevel().isClientSide();
    }

    /** Blocks its Range cards add to its working area. */
    public int range() {
        return onClient() ? syncedRange : effects().range();
    }

    /** Whether a Muffler card silences it. */
    public boolean muffled() {
        return onClient() ? syncedMuffled : effects().muffled();
    }

    /** Adds what clients need to a block entity's update tag. */
    public void writeSync(net.minecraft.nbt.CompoundTag tag) {
        UpgradeEffects effects = effects();
        tag.putInt("upgrade_range", effects.range());
        tag.putBoolean("upgrade_muffled", effects.muffled());
    }

    /** Reads what {@link #writeSync} wrote (on a client). */
    public void readSync(ValueInput input) {
        syncedRange = input.getIntOr("upgrade_range", 0);
        syncedMuffled = input.getBooleanOr("upgrade_muffled", false);
    }

    public ItemContainerContents toComponent() {
        return ItemContainerContents.fromItems(getItems());
    }

    public void fromComponent(ItemContainerContents contents) {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        contents.copyInto(items);
        for (int i = 0; i < SIZE; i++) getItems().set(i, items.get(i));
    }

    public void save(ValueOutput output) {
        if (!isEmpty()) output.store("upgrades", ItemContainerContents.CODEC, toComponent());
    }

    public void load(ValueInput input) {
        clearContent();
        input.read("upgrades", ItemContainerContents.CODEC).ifPresent(this::fromComponent);
    }
}
