package com.jax3.barelyfunctionalcontraptions.registry;

import com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResourceRegistry;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The single creative tab shared by both modules. */
public final class ContraptionsCreativeTab {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BarelyFunctionalContraptions.MOD_ID);

    /** Built-in sheep resources, used to populate the tab before a world (and therefore the data pack) is loaded. */
    public static final List<String> BUILT_IN_WOOL = List.of("iron", "gold", "copper", "diamond", "emerald", "redstone", "lapis",
            "coal", "netherite", "stone", "wood", "quartz", "red_mushroom", "brown_mushroom");

    public static final Supplier<CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + BarelyFunctionalContraptions.MOD_ID))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> new ItemStack(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIAL_GENERATOR.get()))
            .displayItems((params, output) -> {
                // Power
                output.accept(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIAL_GENERATOR.get());
                output.accept(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get());
                // Processing
                output.accept(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.MECHANICAL_SEPARATOR.get());
                output.accept(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.AUTOMATIC_SHEARS.get());
                output.accept(com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems.CREATIVE_GENERATOR.get());
                // Resource wool
                SheepResourceRegistry loaded = SheepResourceRegistry.client();
                if (loaded.isEmpty()) {
                    BUILT_IN_WOOL.forEach(name -> output.accept(ResourceWoolItem.create(BarelyFunctionalContraptions.id(name), 1)));
                } else {
                    for (SheepResource resource : loaded.all()) {
                        output.accept(ResourceWoolItem.create(resource.id(), 1));
                    }
                }
            })
            .build());

    private ContraptionsCreativeTab() {}
}
