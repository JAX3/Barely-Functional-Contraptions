package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.GrassSeederBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * "Grass Seeder": Bone Meal, or a bucket of water with power, turns the dirt its sheep have eaten back to grass and
 * grows short grass; and sheared sheep in its area graze, and so regrow their wool, sooner. Read from the server's
 * config, so it matches the machine.
 */
public class GrassSeederCategory extends AbstractRecipeCategory<GrassSeederDisplay> {
    public static final IRecipeType<GrassSeederDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("grass_seeder"), GrassSeederDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 122;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, GRASS = 0xFF5AA13A, GREEN_TEXT = 0xFF8FE07A, ARROW = 0xFF6A6F80;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public GrassSeederCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder"),
                gui.createDrawableItemLike(ModItems.GRASS_SEEDER.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(GrassSeederDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        graphics.fill(1, 1, WIDTH - 1, 3, GRASS);
        arrow(graphics, 66, 22);
        arrow(graphics, 107, 22);
        arrow(graphics, 30, 51);
        arrow(graphics, 136, 51);
        graphics.fill(6, 68, WIDTH - 6, 69, 0xFF4A4E5A);
    }

    private static void arrow(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 9, y + 2, ARROW);
        graphics.fill(x + 7, y - 2, x + 9, y + 4, ARROW);
        graphics.fill(x + 9, y - 1, x + 10, y + 3, ARROW);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GrassSeederDisplay recipe, IFocusGroup focuses) {
        int blocks = ModConfigs.getInt(ModConfigs.SERVER.seederBoneMealBlocks);
        String perBlock = String.format(Locale.ROOT, "%,d", GrassSeederBlockEntity.energyPerBlock());
        int water = GrassSeederBlockEntity.waterPerBlock();
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 14)
                .setBackground(slot, -1, -1)
                .add(Items.BONE_MEAL)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.bone_meal", blocks).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, 26, 14)
                .setBackground(slot, -1, -1)
                .add(Items.WATER_BUCKET)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.water", perBlock, water).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, 46, 14)
                .setBackground(slot, -1, -1)
                .add(Items.DIRT)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.dirt").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 14)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.GRASS_SEEDER.get());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 14)
                .setBackground(slot, -1, -1)
                .add(Items.GRASS_BLOCK);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 14)
                .setBackground(slot, -1, -1)
                .add(Items.SHORT_GRASS)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.short",
                        Math.round(ModConfigs.getDouble(ModConfigs.SERVER.seederShortGrass) * 100)).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 44)
                .setBackground(slot, -1, -1)
                .add(Items.SHEEP_SPAWN_EGG)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.sheep").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 44)
                .setBackground(slot, -1, -1)
                .add(com.jax3.barelyfunctionalcontraptions.compat.Vanilla.wool(net.minecraft.world.item.DyeColor.WHITE))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.wool").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, GrassSeederDisplay recipe, IFocusGroup focuses) {
        double multiplier = ModConfigs.getDouble(ModConfigs.SERVER.seederGrazeMultiplier);
        String times = String.format(Locale.ROOT, "%.1f", multiplier).replaceAll("\\.0$", "");
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.graze", times)), 96, 20)
                .setPosition(38, 46).setColor(GREEN_TEXT).setTextAlignment(HorizontalAlignment.CENTER);
        int size = GrassSeederBlockEntity.radius() * 2 + 1;
        double seconds = GrassSeederBlockEntity.interval() / 20.0;
        builder.addText(List.of(
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.area", size, size,
                                String.format(Locale.ROOT, "%.1f", seconds).replaceAll("\\.0$", "")),
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.fuel", ModConfigs.getInt(ModConfigs.SERVER.seederBoneMealBlocks)),
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.fuel_power",
                                String.format(Locale.ROOT, "%,d", GrassSeederBlockEntity.energyPerBlock()), GrassSeederBlockEntity.waterPerBlock()),
                        Component.translatable("jei.barelyfunctionalcontraptions.grass_seeder.rest")), WIDTH - 12, 48)
                .setPosition(6, 74).setLineSpacing(2).setColor(JeiTextures.TEXT_DIM).setTextAlignment(HorizontalAlignment.CENTER);
    }
}
