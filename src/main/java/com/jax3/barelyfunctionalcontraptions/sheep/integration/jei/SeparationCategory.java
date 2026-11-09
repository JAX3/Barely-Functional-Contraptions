package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.Separation;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** "Mechanical Separation": wool -> resource + string (+ chance bonus), with FE and time. */
public class SeparationCategory extends AbstractRecipeCategory<SeparationDisplay> {
    public static final IRecipeType<SeparationDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("mechanical_separation"), SeparationDisplay.class);
    /**
     * One compact row per recipe (wool, arrow, outputs, energy and time), so JEI stacks many on a page and every wool
     * type can be seen at a glance instead of one per page.
     */
    public static final int WIDTH = 200, HEIGHT = 20;
    private static final int INPUT_X = 1, ARROW_X = 22, PRIMARY_X = 50, SECONDARY_X = 70, BONUS_X = 90, TEXT_X = 112;

    private final IGuiHelper gui;
    private final mezz.jei.api.gui.drawable.IDrawableStatic background;
    private final mezz.jei.api.gui.drawable.IDrawableStatic slot;
    private final mezz.jei.api.gui.drawable.IDrawableStatic arrowEmpty;

    public SeparationCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.mechanical_separation"),
                gui.createDrawableItemLike(ModItems.MECHANICAL_SEPARATOR.get()), WIDTH, HEIGHT);
        this.gui = gui;
        this.background = JeiTextures.sprite(gui, 0, 64, WIDTH, HEIGHT);   // slate row, like the separator GUI
        this.slot = JeiTextures.slot(gui);
        this.arrowEmpty = JeiTextures.arrowEmpty(gui);
    }

    @Override
    public void draw(SeparationDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        background.draw(graphics);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SeparationDisplay recipe, IFocusGroup focuses) {
        Separation s = recipe.separation();
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, 1)
                .setBackground(slot, -1, -1)
                .addItemStacks(recipe.inputs());

        double expected = s.expectedPrimaryCount();
        // Cycle through every possible count (e.g. 2, 3, 4) so the slot itself shows the range.
        java.util.List<net.minecraft.world.item.ItemStack> counts = new java.util.ArrayList<>();
        String[] bounds = s.primaryLabel().split("-");
        int lo = Integer.parseInt(bounds[0]), hi = Integer.parseInt(bounds[bounds.length - 1]);
        for (int n = Math.max(1, lo); n <= Math.max(1, hi); n++) counts.add(s.primary().create().copyWithCount(n));
        builder.addSlot(RecipeIngredientRole.OUTPUT, PRIMARY_X, 1)
                .setBackground(slot, -1, -1)
                .addItemStacks(counts)
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.range", s.primaryLabel(),
                            String.format(Locale.ROOT, "%.1f", expected)).withStyle(ChatFormatting.GRAY));
                });
        s.secondary().ifPresent(sec -> builder.addSlot(RecipeIngredientRole.OUTPUT, SECONDARY_X, 1)
                .setBackground(slot, -1, -1)
                .add(sec));
        s.bonus().ifPresent(bonus -> builder.addSlot(RecipeIngredientRole.OUTPUT, BONUS_X, 1)
                .setBackground(slot, -1, -1)
                .add(bonus.item())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.chance",
                        Math.round(bonus.chance() * 100)).withStyle(ChatFormatting.GOLD))));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, SeparationDisplay recipe, IFocusGroup focuses) {
        Separation s = recipe.separation();
        // The separator GUI's purple arrow, filling over the real processing time.
        builder.addDrawableWidget(arrowEmpty).setPosition(ARROW_X, 2);
        builder.addDrawableWidget(JeiTextures.arrowFilling(gui, s.effectiveTicks())).setPosition(ARROW_X, 2);
        // Energy and time, compact, with the full figures in the tooltip.
        builder.addText(List.<FormattedText>of(Component.translatable("jei.barelyfunctionalcontraptions.energy_time_short",
                        fe(s.effectiveEnergy()), String.format(Locale.ROOT, "%.1f", s.effectiveTicks() / 20.0))),
                        WIDTH - TEXT_X - 4, 10)
                .setPosition(TEXT_X + 2, 6)
                .setColor(JeiTextures.TEXT)
                .setTextAlignment(HorizontalAlignment.LEFT);
        builder.addTooltipArea(TEXT_X, 0, WIDTH - TEXT_X, HEIGHT).setTooltip(List.<FormattedText>of(
                Component.translatable("jei.barelyfunctionalcontraptions.energy", fe(s.effectiveEnergy())),
                Component.translatable("jei.barelyfunctionalcontraptions.energy_per_tick", fe(s.energyPerTick())).withStyle(ChatFormatting.GRAY),
                Component.translatable("jei.barelyfunctionalcontraptions.time", String.format(Locale.ROOT, "%.1f", s.effectiveTicks() / 20.0)).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public @Nullable Identifier getIdentifier(SeparationDisplay recipe) {
        return recipe.id();
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
