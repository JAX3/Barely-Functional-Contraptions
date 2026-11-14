package com.jax3.barelyfunctionalcontraptions.sheep.registry;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.AutomaticShearsMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.MechanicalSeparatorMenu;
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

    public static final Supplier<MenuType<com.jax3.barelyfunctionalcontraptions.sheep.menu.FluidReservoirMenu>> FLUID_RESERVOIR = REGISTER.register("fluid_reservoir",
            () -> IMenuTypeExtension.create(com.jax3.barelyfunctionalcontraptions.sheep.menu.FluidReservoirMenu::new));

    public static final Supplier<MenuType<com.jax3.barelyfunctionalcontraptions.sheep.menu.EnergyCubeMenu>> HUME_ENERGY_CUBE = REGISTER.register("hume_energy_cube",
            () -> IMenuTypeExtension.create(com.jax3.barelyfunctionalcontraptions.sheep.menu.EnergyCubeMenu::new));

    public static final Supplier<MenuType<com.jax3.barelyfunctionalcontraptions.sheep.menu.FluidTankMenu>> FLUID_TANK = REGISTER.register("fluid_tank",
            () -> IMenuTypeExtension.create(com.jax3.barelyfunctionalcontraptions.sheep.menu.FluidTankMenu::new));

    public static final Supplier<MenuType<com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu>> ZERO_POINT_ENERGY_MODULE = REGISTER.register("zero_point_energy_module",
            () -> IMenuTypeExtension.create(com.jax3.barelyfunctionalcontraptions.sheep.menu.ZeroPointModuleMenu::new));

    public static final Supplier<MenuType<com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu>> PIPE_FILTER = REGISTER.register("pipe_filter",
            () -> IMenuTypeExtension.create(com.jax3.barelyfunctionalcontraptions.sheep.menu.PipeFilterMenu::new));

    public static final Supplier<MenuType<com.jax3.barelyfunctionalcontraptions.sheep.menu.BlazeFurnaceMenu>> BLAZE_FURNACE = REGISTER.register("blaze_furnace",
            () -> IMenuTypeExtension.create(com.jax3.barelyfunctionalcontraptions.sheep.menu.BlazeFurnaceMenu::new));

    public static final Supplier<MenuType<com.jax3.barelyfunctionalcontraptions.sheep.menu.WoolBurnerMenu>> WOOL_BURNER = REGISTER.register("wool_burner",
            () -> IMenuTypeExtension.create(com.jax3.barelyfunctionalcontraptions.sheep.menu.WoolBurnerMenu::new));

    private ModMenus() {}
}
