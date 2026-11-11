package com.jax3.barelyfunctionalcontraptions.sheep.blocks;

import net.minecraft.util.StringRepresentable;

/** What an Energy Cable is joined to on one side: nothing, another cable, or a block with an energy connection. */
public enum CableConnection implements StringRepresentable {
    NONE("none"),
    CABLE("cable"),
    MACHINE("machine");

    private final String name;

    CableConnection(String name) {
        this.name = name;
    }

    public boolean connected() {
        return this != NONE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
