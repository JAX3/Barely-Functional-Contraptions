package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.ThermalReactionChamberBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import java.util.ArrayList;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/**
 * "Thermal Reaction": Blaze sheep and a Blaze Rod on the hot side, Snow sheep and Blue Ice on the cold side, water
 * through the Thermal Reaction Chamber in the middle, and a table of the power it makes for each number of pairs, with
 * and without both boosts. Everything is read from the server's config, so it matches the machine.
 */
public class ThermalCategory extends AbstractRecipeCategory<ThermalDisplay> {
    public static final IRecipeType<ThermalDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("thermal_reaction"), ThermalDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 146;
    private static final int ROW = 6, TABLE_Y = 60, LINE = 10;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, HOT_EDGE = 0xFFE8561A, COLD_EDGE = 0xFF3F8FCF;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public ThermalCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.thermal_reaction"),
                gui.createDrawableItemLike(ModItems.THERMAL_REACTION_CHAMBER.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(ThermalDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        // The slate panel, warm on the left and cold on the right like the chamber itself.
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        graphics.fill(1, 1, WIDTH / 2, 3, HOT_EDGE);
        graphics.fill(WIDTH / 2, 1, WIDTH - 1, 3, COLD_EDGE);
        // Table rules.
        graphics.fill(6, TABLE_Y + LINE - 1, WIDTH - 6, TABLE_Y + LINE, 0xFF4A4E5A);
        graphics.fill(6, TABLE_Y - 3, WIDTH - 6, TABLE_Y - 2, 0xFF4A4E5A);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalDisplay recipe, IFocusGroup focuses) {
        int max = ThermalReactionChamberBlockEntity.maxPairs();
        int seconds = ThermalReactionChamberBlockEntity.boostTicks() / 20;
        String boost = multiplier(ModConfigs.getDouble(ModConfigs.SERVER.thermalBoostMultiplier));
        String pen = pen();
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 14)
                .setBackground(slot, -1, -1)
                .addItemStacks(eggs(ModConfigs.SERVER.thermalHotSources.get(), max))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.thermal.hot_herd", max, pen).withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.thermal.herd_desc").withStyle(ChatFormatting.GRAY));
                });
        builder.addSlot(RecipeIngredientRole.INPUT, 32, 14)
                .setBackground(slot, -1, -1)
                .add(Items.BLAZE_ROD)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.thermal.boost", seconds, boost).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 14)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.THERMAL_REACTION_CHAMBER.get());
        builder.addSlot(RecipeIngredientRole.INPUT, 128, 14)
                .setBackground(slot, -1, -1)
                .add(Items.BLUE_ICE)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.thermal.boost", seconds, boost).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, 152, 14)
                .setBackground(slot, -1, -1)
                .addItemStacks(eggs(ModConfigs.SERVER.thermalColdSources.get(), max))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.thermal.cold_herd", max, pen).withStyle(ChatFormatting.AQUA));
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.thermal.herd_desc").withStyle(ChatFormatting.GRAY));
                });
        int water = ModConfigs.getInt(ModConfigs.SERVER.thermalWaterPerTick);
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 38)
                .setBackground(slot, -1, -1)
                .setFluidRenderer(Math.max(1, water), false, 16, 16)
                .addFluidStack(Fluids.WATER, water)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.thermal.water", water).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ThermalDisplay recipe, IFocusGroup focuses) {
        int max = ThermalReactionChamberBlockEntity.maxPairs();
        List<FormattedText> pairs = new ArrayList<>(), plain = new ArrayList<>(), boosted = new ArrayList<>();
        pairs.add(Component.translatable("jei.barelyfunctionalcontraptions.thermal.col_pairs"));
        plain.add(Component.translatable("jei.barelyfunctionalcontraptions.thermal.col_power"));
        boosted.add(Component.translatable("jei.barelyfunctionalcontraptions.thermal.col_boosted"));
        for (int n = 1; n <= max && pairs.size() <= ROW; n++) {
            pairs.add(Component.literal(Integer.toString(n)));
            plain.add(Component.literal(fe(ThermalReactionChamberBlockEntity.power(n, false, 0))));
            boosted.add(Component.literal(fe(ThermalReactionChamberBlockEntity.power(n, true, 0))));
        }
        if (max > ROW) { // a big config: the last row is the most it counts
            pairs.set(ROW, Component.literal(Integer.toString(max)));
            plain.set(ROW, Component.literal(fe(ThermalReactionChamberBlockEntity.power(max, false, 0))));
            boosted.set(ROW, Component.literal(fe(ThermalReactionChamberBlockEntity.power(max, true, 0))));
        }
        int height = LINE * pairs.size();
        builder.addText(pairs, 40, height).setPosition(6, TABLE_Y).setLineSpacing(1).setColor(JeiTextures.TEXT_DIM)
                .setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(plain, 60, height).setPosition(50, TABLE_Y).setLineSpacing(1).setColor(0xFFFFD27F)
                .setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(boosted, 60, height).setPosition(112, TABLE_Y).setLineSpacing(1).setColor(0xFFFFB46A)
                .setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.thermal.balance")), WIDTH - 8, 10)
                .setPosition(4, HEIGHT - 12).setColor(JeiTextures.TEXT_DIM).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.thermal.balance_desc"));
        // Hovering the side labels explains the risks.
        int seconds = ThermalReactionChamberBlockEntity.unstableTicks() / 20;
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.thermal.hot")), 44, 10).setPosition(4, 36)
                .setColor(0xFFFFB46A).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.thermal.meltdown", seconds));
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.thermal.cold")), 44, 10).setPosition(128, 36)
                .setColor(0xFFBFE8FF).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.thermal.freeze", seconds));
    }

    /** Each source resource's spawn egg, in a stack of up to {@code max} (the most pairs). */
    private static List<ItemStack> eggs(List<? extends String> sources, int max) {
        List<ItemStack> eggs = new ArrayList<>();
        for (String id : sources) {
            net.minecraft.resources.Identifier parsed = net.minecraft.resources.Identifier.tryParse(id);
            if (parsed != null) eggs.add(ResourceSheepSpawnEggItem.create(parsed, Math.min(64, max)));
        }
        return eggs;
    }

    private static String pen() {
        int side = ModConfigs.getInt(ModConfigs.SERVER.thermalRadius) * 2 + 1;
        return side + "x" + side;
    }

    private static String multiplier(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String fe(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
