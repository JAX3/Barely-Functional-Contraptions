package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HerdCounterBlockEntity.Mode;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
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
 * "Herd Counter": sheep (any, or one resource through its filter) counted by the Herd Counter onto a comparator, and a
 * table of its modes and what each counts. Read from the server's config, so it matches the block.
 */
public class HerdCounterCategory extends AbstractRecipeCategory<HerdCounterDisplay> {
    public static final IRecipeType<HerdCounterDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("herd_counter"), HerdCounterDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 112;
    private static final int TABLE_Y = 40;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, LIT = 0xFF7CFF8A, ARROW = 0xFF6A6F80;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public HerdCounterCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.herd_counter"),
                gui.createDrawableItemLike(ModItems.HERD_COUNTER.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(HerdCounterDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        graphics.fill(1, 1, WIDTH - 1, 3, LIT);
        arrow(graphics, 52, 21);
        arrow(graphics, 110, 21);
        graphics.fill(6, TABLE_Y - 3, WIDTH - 6, TABLE_Y - 2, 0xFF4A4E5A);
    }

    private static void arrow(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 12, y + 2, ARROW);
        graphics.fill(x + 10, y - 2, x + 12, y + 4, ARROW);
        graphics.fill(x + 12, y - 1, x + 13, y + 3, ARROW);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HerdCounterDisplay recipe, IFocusGroup focuses) {
        int size = HerdCounterBlockEntity.radius() * 2 + 1;
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 14)
                .setBackground(slot, -1, -1)
                .add(Items.SHEEP_SPAWN_EGG)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.herd_counter.sheep", size, size).withStyle(ChatFormatting.GRAY)));
        List<ItemStack> filters = new ArrayList<>();
        for (String id : List.of("iron", "gold", "copper")) {
            filters.add(ResourceWoolItem.create(ResourcefulSheep.id(id), 1));
            filters.add(ResourceSheepSpawnEggItem.create(ResourcefulSheep.id(id), 1));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 30, 14)
                .setBackground(slot, -1, -1)
                .addItemStacks(filters)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.herd_counter.filter").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 14)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.HERD_COUNTER.get());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 128, 14)
                .setBackground(slot, -1, -1)
                .add(Items.COMPARATOR)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.herd_counter.comparator",
                        ModConfigs.getInt(ModConfigs.SERVER.herdSignalPerSheep)).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 14)
                .setBackground(slot, -1, -1)
                .add(Items.REDSTONE_LAMP);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, HerdCounterDisplay recipe, IFocusGroup focuses) {
        List<FormattedText> labels = new ArrayList<>(), meanings = new ArrayList<>();
        int target = ModConfigs.getInt(ModConfigs.SERVER.herdFullTarget);
        for (Mode mode : Mode.values()) {
            labels.add(Component.translatable("screen.barelyfunctionalcontraptions.herd_counter." + mode.key()));
            meanings.add(Component.translatable("jei.barelyfunctionalcontraptions.herd_counter.mode." + mode.key(), target));
        }
        int height = 11 * labels.size();
        builder.addText(labels, 44, height).setPosition(6, TABLE_Y).setLineSpacing(2).setColor(LIT).setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(meanings, WIDTH - 56, height).setPosition(50, TABLE_Y).setLineSpacing(2).setColor(JeiTextures.TEXT_DIM);
    }
}
