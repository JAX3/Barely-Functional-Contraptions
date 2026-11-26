package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ImplosionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import java.util.List;
import java.util.Locale;
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

/**
 * "Implosion Chamber": an Explosive sheep's blast in its pen cracks the input a step, Cobblestone into Gravel or Gravel
 * into Sand. Its numbers (pen, sparks, power, batch) are read from the server's config, so they match the machine.
 */
public class ImplosionCategory extends AbstractRecipeCategory<ImplosionDisplay> {
    public static final IRecipeType<ImplosionDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("implosion_chamber"), ImplosionDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 100;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, EMBER = 0xFFF07A2A, EMBER_DIM = 0xFFA8321A, ARROW = 0xFF6A6F80, TEXT = 0xFFFFD27A;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public ImplosionCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.implosion_chamber"),
                gui.createDrawableItemLike(ModItems.IMPLOSION_CHAMBER.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(ImplosionDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        graphics.fill(1, 1, WIDTH - 1, 3, EMBER_DIM);
        graphics.fill(WIDTH / 2 - 30, 1, WIDTH / 2 + 30, 3, EMBER);
        arrow(graphics, 48, 22);
        arrow(graphics, 112, 22);
        graphics.fill(6, 44, WIDTH - 6, 45, 0xFF4A4E5A);
    }

    private static void arrow(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 9, y + 2, ARROW);
        graphics.fill(x + 7, y - 2, x + 9, y + 4, ARROW);
        graphics.fill(x + 9, y - 1, x + 10, y + 3, ARROW);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ImplosionDisplay recipe, IFocusGroup focuses) {
        int batch = ImplosionChamberBlockEntity.batch();
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 14)
                .setBackground(slot, -1, -1)
                .add(recipe.input())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.implosion_chamber.batch", batch).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 14)
                .setBackground(slot, -1, -1)
                .add(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem.create(ResourcefulSheep.id("explosive"), 1))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.implosion_chamber.sheep").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 14)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.IMPLOSION_CHAMBER.get());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 14)
                .setBackground(slot, -1, -1)
                .add(recipe.output().copyWithCount(batch));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 14)
                .setBackground(slot, -1, -1)
                .add(com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem.create(ResourcefulSheep.id("explosive"), 1))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.implosion_chamber.wool").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ImplosionDisplay recipe, IFocusGroup focuses) {
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.implosion_chamber.per_blast", ImplosionChamberBlockEntity.batch())),
                        WIDTH - 12, 10).setPosition(6, 33).setColor(TEXT).setTextAlignment(HorizontalAlignment.CENTER);
        int size = ImplosionChamberBlockEntity.radius() * 2 + 1;
        String seconds = String.format(Locale.ROOT, "%.1f", ImplosionChamberBlockEntity.sparkInterval() / 20.0).replaceAll("\\.0$", "");
        builder.addText(List.of(
                        Component.translatable("jei.barelyfunctionalcontraptions.implosion_chamber.pen", size, size, seconds,
                                String.format(Locale.ROOT, "%,d", ImplosionChamberBlockEntity.sparkEnergy())),
                        Component.translatable("jei.barelyfunctionalcontraptions.implosion_chamber.regrow")), WIDTH - 12, 46)
                .setPosition(6, 50).setLineSpacing(2).setColor(JeiTextures.TEXT_DIM).setTextAlignment(HorizontalAlignment.CENTER);
    }
}
