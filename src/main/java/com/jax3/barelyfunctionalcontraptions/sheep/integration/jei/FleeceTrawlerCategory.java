package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
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
 * "Fleece Trawler": Tide sheep (and, on a lure page, that resource's wool) into the trawler, its catch out: fish (and
 * now and then junk) on the plain page, the resource on a lure page; with a line on how fast it hauls or how often a
 * lure pays out.
 */
public class FleeceTrawlerCategory extends AbstractRecipeCategory<FleeceTrawlerDisplay> {
    public static final IRecipeType<FleeceTrawlerDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("fleece_trawler"), FleeceTrawlerDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 74;
    private static final int TOP_Y = 14;
    private static final int PANEL = 0xFF23303A, EDGE = 0xFF111A20, ARROW = 0xFF5FA8A0, WAVE = 0xFF2E7DB0;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public FleeceTrawlerCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("block.barelyfunctionalcontraptions.fleece_trawler"),
                gui.createDrawableItemLike(ModItems.FLEECE_TRAWLER.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(FleeceTrawlerDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        for (int i = 1; i < WIDTH - 1; i++) {
            int h = Math.round(1.5F + 1.5F * (float) Math.sin(i * 0.35));
            graphics.fill(i, HEIGHT - 2 - h, i + 1, HEIGHT - 1, WAVE);
        }
        graphics.fill(52, TOP_Y + 7, 68, TOP_Y + 9, ARROW);
        graphics.fill(66, TOP_Y + 5, 68, TOP_Y + 11, ARROW);
        graphics.fill(106, TOP_Y + 7, 120, TOP_Y + 9, ARROW);
        graphics.fill(118, TOP_Y + 5, 120, TOP_Y + 11, ARROW);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FleeceTrawlerDisplay recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, TOP_Y)
                .setBackground(slot, -1, -1)
                .add(ResourceSheepSpawnEggItem.create(ResourcefulSheep.id("tide"), 1))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.fleece_trawler.sheep",
                        ModConfigs.getInt(ModConfigs.SERVER.trawlerMaxSheep)).withStyle(ChatFormatting.GRAY)));
        if (recipe.lure() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 30, TOP_Y)
                    .setBackground(slot, -1, -1)
                    .add(ResourceWoolItem.create(recipe.lure().id(), 1))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.fleece_trawler.lure",
                            ModConfigs.getInt(ModConfigs.SERVER.trawlerLureUses)).withStyle(ChatFormatting.GRAY)));
        }
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, TOP_Y)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.FLEECE_TRAWLER.get());
        List<ItemStack> out = new ArrayList<>();
        if (recipe.lure() != null) {
            ItemStack caught = recipe.lure().separation().primary().create();
            out.add(caught.copyWithCount(1));
            out.add(caught.copyWithCount(2));
        } else {
            for (var item : List.of(Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH)) out.add(new ItemStack(item));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 128, TOP_Y)
                .setBackground(slot, -1, -1)
                .addItemStacks(out);
        if (recipe.lure() == null) {
            List<ItemStack> junk = new ArrayList<>();
            for (var item : List.of(Items.BOWL, Items.STRING, Items.LILY_PAD, Items.BONE, Items.STICK)) junk.add(new ItemStack(item));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 150, TOP_Y)
                    .setBackground(slot, -1, -1)
                    .addItemStacks(junk)
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.fleece_trawler.junk").withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, FleeceTrawlerDisplay recipe, IFocusGroup focuses) {
        Component line = recipe.lure() == null
                ? Component.translatable("jei.barelyfunctionalcontraptions.fleece_trawler.hauls", ModConfigs.getInt(ModConfigs.SERVER.trawlerHaulTicks) / 20)
                : Component.translatable("jei.barelyfunctionalcontraptions.fleece_trawler.lure_chance",
                        Math.round(ModConfigs.getDouble(ModConfigs.SERVER.trawlerLureChance) * 100), ModConfigs.getInt(ModConfigs.SERVER.trawlerLureUses));
        builder.addText(List.<FormattedText>of(line), WIDTH - 8, 10).setPosition(4, TOP_Y + 26).setColor(JeiTextures.TEXT_DIM)
                .setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(List.<FormattedText>of(Component.translatable("jei.barelyfunctionalcontraptions.fleece_trawler.pool")), WIDTH - 8, 10)
                .setPosition(4, TOP_Y + 38).setColor(JeiTextures.TEXT_DIM).setTextAlignment(HorizontalAlignment.CENTER);
    }
}
