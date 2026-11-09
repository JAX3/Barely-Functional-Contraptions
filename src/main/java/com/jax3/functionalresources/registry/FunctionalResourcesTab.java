package com.jax3.functionalresources.registry;

import com.jax3.functionalresources.FunctionalResources;
import com.jax3.functionalresources.sheep.items.ResourceWoolItem;
import com.jax3.functionalresources.sheep.resource.SheepResource;
import com.jax3.functionalresources.sheep.resource.SheepResourceRegistry;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The single creative tab shared by both modules. */
public final class FunctionalResourcesTab {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FunctionalResources.MOD_ID);

    /** Built-in sheep resources, used to populate the tab before a world (and therefore the data pack) is loaded. */
    public static final List<String> BUILT_IN_WOOL = List.of("iron", "gold", "copper", "diamond", "emerald", "redstone", "lapis",
            "coal", "netherite", "stone", "wood", "quartz", "red_mushroom", "brown_mushroom");

    public static final Supplier<CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + FunctionalResources.MOD_ID))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> new ItemStack(com.jax3.functionalresources.mycelial.registry.ModItems.MYCELIAL_GENERATOR.get()))
            .displayItems((params, output) -> {
                // Power
                output.accept(com.jax3.functionalresources.mycelial.registry.ModItems.MYCELIAL_GENERATOR.get());
                output.accept(com.jax3.functionalresources.mycelial.registry.ModItems.MYCELIUM_WRENCH.get());
                // Processing
                output.accept(com.jax3.functionalresources.sheep.registry.ModItems.MECHANICAL_SEPARATOR.get());
                output.accept(com.jax3.functionalresources.sheep.registry.ModItems.CREATIVE_GENERATOR.get());
                // Resource wool
                SheepResourceRegistry loaded = SheepResourceRegistry.client();
                if (loaded.isEmpty()) {
                    BUILT_IN_WOOL.forEach(name -> output.accept(ResourceWoolItem.create(FunctionalResources.id(name), 1)));
                } else {
                    for (SheepResource resource : loaded.all()) {
                        output.accept(ResourceWoolItem.create(resource.id(), 1));
                    }
                }
            })
            .build());

    private FunctionalResourcesTab() {}
}
