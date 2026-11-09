package com.jax3.functionalresources.sheep.registry;

import com.jax3.functionalresources.sheep.ResourcefulSheep;
import com.jax3.functionalresources.sheep.menu.AutomaticShearsMenu;
import com.jax3.functionalresources.sheep.menu.MechanicalSeparatorMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, ResourcefulSheep.MOD_ID);

    public static final Supplier<MenuType<MechanicalSeparatorMenu>> MECHANICAL_SEPARATOR = REGISTER.register("mechanical_separator",
            () -> IMenuTypeExtension.create(MechanicalSeparatorMenu::new));

    public static final Supplier<MenuType<AutomaticShearsMenu>> AUTOMATIC_SHEARS = REGISTER.register("automatic_shears",
            () -> IMenuTypeExtension.create(AutomaticShearsMenu::new));

    private ModMenus() {}
}
