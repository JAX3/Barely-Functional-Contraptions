package com.jax3.barelyfunctionalcontraptions.mycelial.compat.jei;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import com.jax3.barelyfunctionalcontraptions.mycelial.client.screen.MycelialGeneratorScreen;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.mycelial.util.NumberFormatting;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

/**
 * JEI entry point. Loaded only by JEI, so the mod has no hard dependency on it. All displayed values
 * come from the synced server config through {@link MycelialFuelRecipeFactory}.
 */
@JeiPlugin
public class MycelialPowerJeiPlugin implements IModPlugin {
    private static final Identifier UID = MycelialPower.id("jei_mycelial_power");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MycelialGeneratorCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MycelialGeneratorCategory.TYPE, MycelialFuelRecipeFactory.create());

        ItemStack generator = new ItemStack(ModItems.MYCELIAL_GENERATOR.get());
        registration.addIngredientInfo(generator, VanillaTypes.ITEM_STACK,
                Component.translatable("jei.barelyfunctionalcontraptions.info.generator",
                        NumberFormatting.decimal(ServerConfig.get(ServerConfig.BASE_GENERATION))),
                Component.translatable("jei.barelyfunctionalcontraptions.info.water",
                        ServerConfig.get(ServerConfig.WATER_PER_INTERVAL),
                        NumberFormatting.ticksToTime(ServerConfig.get(ServerConfig.WATER_INTERVAL)),
                        NumberFormatting.grouped(ServerConfig.get(ServerConfig.TANK_CAPACITY))),
                myceliumInfo(),
                hugeMushroomInfo());
        registerWrenchInfo(registration);
    }

    /** Wrench info page: what the wrench does. */
    private static void registerWrenchInfo(IRecipeRegistration registration) {
        registration.addIngredientInfo(new ItemStack(ModItems.MYCELIUM_WRENCH.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.barelyfunctionalcontraptions.info.wrench_usage"));
    }

    private static Component hugeMushroomInfo() {
        if (!ServerConfig.get(ServerConfig.MYCELIUM_ENABLED) || !ServerConfig.get(ServerConfig.HUGE_MUSHROOM_ENABLED)) {
            return Component.empty();
        }
        return Component.translatable("jei.barelyfunctionalcontraptions.info.huge_mushroom",
                ServerConfig.get(ServerConfig.HUGE_MUSHROOM_MIN_DISTANCE),
                NumberFormatting.percent(ServerConfig.get(ServerConfig.HUGE_MUSHROOM_CHANCE)));
    }

    private static Component myceliumInfo() {
        if (!ServerConfig.get(ServerConfig.MYCELIUM_ENABLED)) {
            return Component.translatable("jei.barelyfunctionalcontraptions.info.mycelium_disabled");
        }
        double initial = ServerConfig.initialChance();
        double increase = ServerConfig.get(ServerConfig.CHANCE_INCREASE);
        double max = ServerConfig.maxChance();
        int attemptsToMax = increase > 0 ? (int) Math.ceil(Math.max(0.0D, max - initial) / increase) + 1 : -1;
        return Component.translatable("jei.barelyfunctionalcontraptions.info.mycelium",
                NumberFormatting.ticksToTime(ServerConfig.get(ServerConfig.ATTEMPT_INTERVAL)),
                ServerConfig.get(ServerConfig.SEARCH_RADIUS),
                NumberFormatting.percent(initial),
                NumberFormatting.percent(increase),
                NumberFormatting.percent(max),
                attemptsToMax > 0 ? Integer.toString(attemptsToMax) : "-",
                Component.translatable(ServerConfig.get(ServerConfig.RESET_ON_SUCCESS)
                        ? "jei.barelyfunctionalcontraptions.info.mycelium_reset" : "jei.barelyfunctionalcontraptions.info.mycelium_keep"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(MycelialGeneratorCategory.TYPE, ModItems.MYCELIAL_GENERATOR.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // A plain addRecipeClickArea would draw JEI's own "Show Recipes" tooltip on top of the screen's burn
        // tooltip. This click area has no tooltip; the screen adds a "click to show fuels" hint instead.
        registration.addGuiContainerHandler(MycelialGeneratorScreen.class, new IGuiContainerHandler<>() {
            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(MycelialGeneratorScreen screen, double guiMouseX, double guiMouseY) {
                return List.of(new IGuiClickableArea() {
                    @Override
                    public Rect2i getArea() {
                        return new Rect2i(MycelialGeneratorScreen.BURN_X, MycelialGeneratorScreen.BURN_Y,
                                MycelialGeneratorScreen.BURN_W, MycelialGeneratorScreen.BURN_H);
                    }

                    @Override
                    public boolean isTooltipEnabled() {
                        return false;
                    }

                    @Override
                    public void onClick(IFocusFactory focusFactory, IRecipesGui recipesGui) {
                        recipesGui.showTypes(List.of(MycelialGeneratorCategory.TYPE));
                    }
                });
            }
        });
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new GeneratorRecipeTransferHandler(registration.getTransferHelper()),
                MycelialGeneratorCategory.TYPE);
    }
}
