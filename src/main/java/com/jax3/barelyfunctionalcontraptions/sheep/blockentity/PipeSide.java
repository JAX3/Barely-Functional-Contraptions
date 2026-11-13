package com.jax3.barelyfunctionalcontraptions.sheep.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/**
 * How one side of a pipe treats the block plugged in there: its {@link Mode} and an optional filter.
 *
 * @param mode   how things move between the pipe and the block on this side
 * @param filter item or fluid ids allowed through this side, in and out; empty lets everything through
 */
public record PipeSide(Mode mode, List<Identifier> filter) {
    public static final PipeSide DEFAULT = new PipeSide(Mode.NORMAL, List.of());
    /** Most entries one side's filter holds. */
    public static final int MAX_FILTER = 9;

    public static final Codec<PipeSide> CODEC = RecordCodecBuilder.create(i -> i.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.NORMAL).forGetter(PipeSide::mode),
            Identifier.CODEC.listOf().optionalFieldOf("filter", List.of()).forGetter(PipeSide::filter)
    ).apply(i, PipeSide::new));

    public boolean isDefault() {
        return mode == Mode.NORMAL && filter.isEmpty();
    }

    /** Whether {@code id} may pass this side. */
    public boolean allows(Identifier id) {
        return filter.isEmpty() || filter.contains(id);
    }

    public PipeSide withMode(Mode newMode) {
        return new PipeSide(newMode, filter);
    }

    /** Adds {@code id} to the filter, or removes it if it's already there. Does nothing when the filter is full. */
    public PipeSide toggle(Identifier id) {
        List<Identifier> list = new ArrayList<>(filter);
        if (!list.remove(id)) {
            if (list.size() >= MAX_FILTER) return this;
            list.add(id);
        }
        return new PipeSide(mode, List.copyOf(list));
    }

    public PipeSide withoutFilter() {
        return new PipeSide(mode, List.of());
    }

    /**
     * What a pipe does with the block on one side.
     * <ul>
     *     <li>{@link #NORMAL}: things are delivered into it, and taken out only if it is a pure source (the Automatic
     *     Shears, a separator's outputs, the Fluid Reservoir), so chests and tanks are never drained.</li>
     *     <li>{@link #PULL}: the pipe takes everything it can out of it, chests and tanks included, and never puts
     *     anything in.</li>
     *     <li>{@link #PUSH}: the pipe only puts things in; it never takes anything out, even from a pure source.</li>
     *     <li>{@link #DISABLED}: no connection on this side at all.</li>
     * </ul>
     */
    public enum Mode implements StringRepresentable {
        NORMAL, PULL, PUSH, DISABLED;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean takesIn() {
            return this == NORMAL || this == PUSH;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component displayName() {
            return Component.translatable("pipe_mode.barelyfunctionalcontraptions." + getSerializedName());
        }
    }
}
