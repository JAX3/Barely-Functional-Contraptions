package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateControllerBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * "Crate Controller": crates (touching it, or linked with the wrench) and a hopper or pipe into the controller, a
 * comparator and a Machine Status Panel out; then, numbered, where items put into it go.
 */
public class CrateControllerCategory extends AbstractRecipeCategory<CrateControllerDisplay> {
    public static final IRecipeType<CrateControllerDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("crate_controller"), CrateControllerDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 100;
    private static final int TOP_Y = 10, ORDER_Y = 46;
    private static final int PANEL = 0xFF3A2C20, EDGE = 0xFF1E160F, ARROW = 0xFF8A6A48, RULE = 0xFF5A4532, BRASS = 0xFFC9923A;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public CrateControllerCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("block.barelyfunctionalcontraptions.crate_controller"),
                gui.createDrawableItemLike(ModItems.CRATE_CONTROLLER.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(CrateControllerDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        graphics.fill(1, 1, WIDTH - 1, 3, BRASS);
        // crates, hopper -> controller -> comparator, panel
        graphics.fill(52, TOP_Y + 7, 68, TOP_Y + 9, ARROW);
        graphics.fill(66, TOP_Y + 5, 68, TOP_Y + 11, ARROW);
        graphics.fill(106, TOP_Y + 7, 122, TOP_Y + 9, ARROW);
        graphics.fill(120, TOP_Y + 5, 122, TOP_Y + 11, ARROW);
        graphics.fill(6, ORDER_Y - 5, WIDTH - 6, ORDER_Y - 4, RULE);
        // numbered markers for where items go
        for (int i = 0; i < 3; i++) graphics.fill(8, ORDER_Y + 3 + i * 12, 12, ORDER_Y + 7 + i * 12, BRASS);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrateControllerDisplay recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, TOP_Y)
                .setBackground(slot, -1, -1)
                .add(ModItems.CRATE.get())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.crate_controller.crates",
                        CrateControllerBlockEntity.maxCrates(), CrateControllerBlockEntity.range()).withStyle(ChatFormatting.GRAY)));
        List<ItemStack> feeds = new ArrayList<>();
        feeds.add(new ItemStack(Items.HOPPER));
        feeds.add(new ItemStack(ModItems.ITEM_PIPE.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, 30, TOP_Y)
                .setBackground(slot, -1, -1)
                .addItemStacks(feeds)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.crate_controller.pipes").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, TOP_Y)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.CRATE_CONTROLLER.get());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 130, TOP_Y)
                .setBackground(slot, -1, -1)
                .add(Items.COMPARATOR)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.crate_controller.comparator").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, TOP_Y)
                .setBackground(slot, -1, -1)
                .add(ModItems.STATUS_PANEL.get())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.crate_controller.panel").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CrateControllerDisplay recipe, IFocusGroup focuses) {
        builder.addText(List.<FormattedText>of(Component.translatable("jei.barelyfunctionalcontraptions.crate_controller.joining")), WIDTH - 8, 10)
                .setPosition(4, TOP_Y + 22).setColor(JeiTextures.TEXT_DIM).setTextAlignment(HorizontalAlignment.CENTER);
        List<FormattedText> order = new ArrayList<>();
        for (String step : new String[]{"holding", "empty", "bulging"}) order.add(Component.translatable("jei.barelyfunctionalcontraptions.crate_controller.order." + step));
        builder.addText(order, WIDTH - 22, 36).setPosition(16, ORDER_Y + 1).setLineSpacing(3).setColor(JeiTextures.TEXT_DIM)
                .setTextAlignment(HorizontalAlignment.LEFT);
        builder.addText(List.<FormattedText>of(Component.translatable("jei.barelyfunctionalcontraptions.crate_controller.strain")), WIDTH - 8, 10)
                .setPosition(4, ORDER_Y + 39).setColor(0xFFE35D5D).setTextAlignment(HorizontalAlignment.CENTER);
    }
}
