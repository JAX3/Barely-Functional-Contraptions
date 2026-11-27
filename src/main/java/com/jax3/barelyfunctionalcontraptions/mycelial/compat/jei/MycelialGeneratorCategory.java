package com.jax3.barelyfunctionalcontraptions.mycelial.compat.jei;

import com.jax3.barelyfunctionalcontraptions.mycelial.MycelialPower;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.EnergyRounding;
import com.jax3.barelyfunctionalcontraptions.mycelial.config.ServerConfig;
import com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.mycelial.util.NumberFormatting;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/** JEI category listing every mushroom fuel with its generation, duration and water use. */
public class MycelialGeneratorCategory implements IRecipeCategory<MycelialFuelRecipe> {
    public static final IRecipeType<MycelialFuelRecipe> TYPE = IRecipeType.create(MycelialPower.MOD_ID, "mycelial_generator", MycelialFuelRecipe.class);
    public static final Identifier TEXTURE = MycelialPower.id("textures/gui/jei_mycelial_generator.png");

    public static final int WIDTH = 168;
    public static final int HEIGHT = 74;
    private static final int TEXT_X = 30;
    private static final int ENERGY_ROW_Y = 15;

    private final IDrawableStatic background;
    private final IDrawable icon;
    private final Component title;

    public MycelialGeneratorCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.MYCELIAL_GENERATOR.get()));
        this.title = Component.translatable("jei.barelyfunctionalcontraptions.category.mycelial_generator");
    }

    @Override
    public IRecipeType<MycelialFuelRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MycelialFuelRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 6)
                .addItemStacks(recipe.inputs());

        IRecipeSlotBuilder water = builder.addSlot(RecipeIngredientRole.INPUT, 7, 27);
        long amount = Math.max(1, recipe.waterPerItem());
        for (Fluid fluid : recipe.fluids()) {
            water.add(fluid, amount);
        }
        water.setFluidRenderer(Math.max(amount, ServerConfig.get(ServerConfig.TANK_CAPACITY)), false, 14, 40);
    }

    @Override
    public void draw(MycelialFuelRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        // JEI 29 categories have no background method; draw the panel here.
        background.draw(graphics);
        Font font = Minecraft.getInstance().font;
        var def = recipe.definition();
        int y = 4;
        graphics.text(font, Component.translatable("jei.barelyfunctionalcontraptions.burn_time",
                NumberFormatting.ticksToTime(def.burnTicks()), NumberFormatting.grouped(def.burnTicks())), TEXT_X, y, 0xFFE6DBFF, false);
        y += 11;
        graphics.text(font, Component.translatable("jei.barelyfunctionalcontraptions.generation",
                NumberFormatting.decimal(recipe.fePerTick())), TEXT_X, y, 0xFFC77DFF, false);
        y += 11;
        graphics.text(font, Component.translatable("jei.barelyfunctionalcontraptions.total_energy",
                NumberFormatting.compact(recipe.totalEnergy())), TEXT_X, y, 0xFFC77DFF, false);
        y += 11;
        graphics.text(font, Component.translatable("jei.barelyfunctionalcontraptions.water",
                NumberFormatting.grouped(recipe.waterPerItem())), TEXT_X, y, 0xFF7FB6FF, false);
        y += 11;
        graphics.text(font, Component.translatable("jei.barelyfunctionalcontraptions.multipliers",
                NumberFormatting.decimal(def.energyMultiplier()), NumberFormatting.decimal(def.waterMultiplier())), TEXT_X, y, 0xFFB0B0BE, false);
        y += 11;
        graphics.text(font, Component.translatable("jei.barelyfunctionalcontraptions.bonuses",
                NumberFormatting.decimal(def.myceliumBonus())), TEXT_X, y, 0xFF8FD27A, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, MycelialFuelRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        tooltip.addAll(tooltipLines(recipe, mouseX, mouseY));
    }

    private static List<Component> tooltipLines(MycelialFuelRecipe recipe, double mouseX, double mouseY) {
        if (mouseX < TEXT_X || mouseX > WIDTH || mouseY < 4) {
            return List.of();
        }
        int row = (int) ((mouseY - 4) / 11);
        return switch (row) {
            case 0 -> List.of(Component.translatable("jei.barelyfunctionalcontraptions.tooltip.burn").withStyle(ChatFormatting.GRAY));
            case 1, 2 -> List.of(Component.translatable("jei.barelyfunctionalcontraptions.tooltip.output").withStyle(ChatFormatting.GRAY));
            case 3 -> List.of(Component.translatable("jei.barelyfunctionalcontraptions.tooltip.water",
                    NumberFormatting.decimal(recipe.waterPerInterval()), NumberFormatting.grouped(recipe.waterInterval()))
                    .withStyle(ChatFormatting.GRAY));
            case 4 -> List.of(Component.translatable("jei.barelyfunctionalcontraptions.tooltip.multipliers").withStyle(ChatFormatting.GRAY));
            case 5 -> List.of(Component.translatable("jei.barelyfunctionalcontraptions.tooltip.bonuses").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.barelyfunctionalcontraptions.tooltip.mycelium_info").withStyle(ChatFormatting.DARK_GRAY));
            default -> List.of();
        };
    }
}
