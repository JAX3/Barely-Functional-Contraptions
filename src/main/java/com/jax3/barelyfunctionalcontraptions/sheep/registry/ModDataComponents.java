package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineContents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ResourcefulSheep.MOD_ID);

    /** Which sheep resource a piece of {@code barelyfunctionalcontraptions:resource_wool} contains. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> SHEEP_RESOURCE = REGISTER.registerComponentType(
            "sheep_resource", b -> b.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).cacheEncoding());

    /** A creative tab's section banner or padding placeholder (see {@link com.jax3.barelyfunctionalcontraptions.registry.TabSection}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.jax3.barelyfunctionalcontraptions.registry.TabSection>> TAB_SECTION = REGISTER.registerComponentType(
            "tab_section", b -> b.persistent(com.jax3.barelyfunctionalcontraptions.registry.TabSection.CODEC)
                    .networkSynchronized(com.jax3.barelyfunctionalcontraptions.registry.TabSection.STREAM_CODEC));

    /** Stored energy/redstone mode kept on a broken Mechanical Separator item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineContents>> MACHINE_CONTENTS = REGISTER.registerComponentType(
            "machine_contents", b -> b.persistent(MachineContents.CODEC).networkSynchronized(MachineContents.STREAM_CODEC));

    /** The upgrade cards kept on a broken or picked-up machine's item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> MACHINE_UPGRADES = REGISTER.registerComponentType(
            "machine_upgrades", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
                    .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));

    /** Fluid kept on a broken Fluid Tank item (that tank's own share of its group). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.neoforged.neoforge.fluids.SimpleFluidContent>> FLUID_CONTENTS = REGISTER.registerComponentType(
            "fluid_contents", b -> b.persistent(net.neoforged.neoforge.fluids.SimpleFluidContent.CODEC)
                    .networkSynchronized(net.neoforged.neoforge.fluids.SimpleFluidContent.STREAM_CODEC));

    /** What a broken Wool-Lined Crate held: its item, how many, its lock and its bales. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents>> CRATE_CONTENTS = REGISTER.registerComponentType(
            "crate_contents", b -> b.persistent(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents.CODEC)
                    .networkSynchronized(com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateContents.STREAM_CODEC));

    private ModDataComponents() {}
}
