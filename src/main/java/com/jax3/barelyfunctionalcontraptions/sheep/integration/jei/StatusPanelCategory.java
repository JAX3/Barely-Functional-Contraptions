package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.MachineReports.Level;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StatusPanelBlockEntity;
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
 * "Machine Status Panel": the Barely Functional Wrench and the machines it links to the panel, the panel, and a
 * comparator out (how many have a problem); then what each colour on its screen means.
 */
public class StatusPanelCategory extends AbstractRecipeCategory<StatusPanelDisplay> {
    public static final IRecipeType<StatusPanelDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("status_panel"), StatusPanelDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 100;
    private static final int TABLE_Y = 42;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, ARROW = 0xFF6A6F80;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public StatusPanelCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.status_panel"),
                gui.createDrawableItemLike(ModItems.STATUS_PANEL.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(StatusPanelDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        int x = 1;
        for (Level level : Level.values()) {
            graphics.fill(x, 1, x + (WIDTH - 2) / 4, 3, level.color);
            x += (WIDTH - 2) / 4;
        }
        graphics.fill(54, 21, 66, 23, ARROW);
        graphics.fill(64, 19, 66, 25, ARROW);
        graphics.fill(108, 21, 120, 23, ARROW);
        graphics.fill(118, 19, 120, 25, ARROW);
        graphics.fill(6, TABLE_Y - 3, WIDTH - 6, TABLE_Y - 2, 0xFF4A4E5A);
        for (int i = 0; i < 4; i++) graphics.fill(10, TABLE_Y + 3 + i * 11, 15, TABLE_Y + 8 + i * 11, Level.values()[i].color);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StatusPanelDisplay recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 14)
                .setBackground(slot, -1, -1)
                .add(com.jax3.barelyfunctionalcontraptions.mycelial.registry.ModItems.MYCELIUM_WRENCH.get())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.status_panel.wrench", StatusPanelBlockEntity.range()).withStyle(ChatFormatting.GRAY)));
        List<ItemStack> machines = new ArrayList<>();
        for (var item : List.of(ModItems.POWERED_FURNACE, ModItems.STATIC_GENERATOR, ModItems.GRASS_SEEDER, ModItems.AUTOMATIC_SHEARS,
                ModItems.ECHO_CHAMBER, ModItems.HUME_FIELD_GENERATOR, ModItems.IMPLOSION_CHAMBER, ModItems.HUME_ENERGY_CUBE, ModItems.HERD_COUNTER, ModItems.BLAZE_FURNACE)) {
            machines.add(new ItemStack(item.get()));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 30, 14)
                .setBackground(slot, -1, -1)
                .addItemStacks(machines)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.status_panel.machines").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 14)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.STATUS_PANEL.get());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 14)
                .setBackground(slot, -1, -1)
                .add(Items.COMPARATOR)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.status_panel.comparator").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 14)
                .setBackground(slot, -1, -1)
                .add(Items.REDSTONE_LAMP);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, StatusPanelDisplay recipe, IFocusGroup focuses) {
        List<FormattedText> lines = new ArrayList<>();
        for (Level level : Level.values()) lines.add(Component.translatable("jei.barelyfunctionalcontraptions.status_panel.level." + level.name().toLowerCase(java.util.Locale.ROOT)));
        builder.addText(lines, WIDTH - 26, 11 * lines.size()).setPosition(20, TABLE_Y).setLineSpacing(2).setColor(JeiTextures.TEXT_DIM)
                .setTextAlignment(HorizontalAlignment.LEFT);
    }
}
