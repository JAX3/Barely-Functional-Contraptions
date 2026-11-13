package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import net.minecraft.util.StringRepresentable;

/**
 * What a conduit is joined to on one side: nothing, another conduit, or a block it plugs into. Pipes also mark a
 * plugged-in side set to pull from the block ({@link #PULL}) or only push into it ({@link #PUSH}).
 */
public enum CableConnection implements StringRepresentable {
    NONE("none"),
    CABLE("cable"),
    MACHINE("machine"),
    PULL("pull"),
    PUSH("push");

    private final String name;

    CableConnection(String name) {
        this.name = name;
    }

    public boolean connected() {
        return this != NONE;
    }

    /** Plugged into a block (in any mode), as opposed to joined to another conduit. */
    public boolean plugged() {
        return this == MACHINE || this == PULL || this == PUSH;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
