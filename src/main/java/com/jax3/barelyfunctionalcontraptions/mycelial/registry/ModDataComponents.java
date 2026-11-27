package com.jax3.barelyfunctionalcontraptions.mycelial.registry;

import com.mojang.serialization.Codec;
import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MycelialPower.MOD_ID);

    /** The Machine Status Panel a Barely Functional Wrench is linking machines to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINKING_PANEL =
            COMPONENTS.register("linking_panel", () -> DataComponentType.<GlobalPos>builder()
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC)
                    .build());

    /** The Crate Controller a Barely Functional Wrench is linking crates to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINKING_CONTROLLER =
            COMPONENTS.register("linking_controller", () -> DataComponentType.<GlobalPos>builder()
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }
}
