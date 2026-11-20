package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.EchoChamberBlockEntity;
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

/**
 * "Echo Chamber" (development builds only, like the machine): Warden sheep in front of the dish, the chamber, a note
 * block for the noise, the sweet spot, and a table of the power for each number of sheep, with a steady noise and an
 * erratic one. Everything is read from the server's config, so it matches the machine.
 */
public class EchoCategory extends AbstractRecipeCategory<EchoDisplay> {
    public static final IRecipeType<EchoDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("echo_chamber"), EchoDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 128;
    private static final int TABLE_Y = 56, LINE = 10;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, SCULK = 0xFF29DFEB, SCULK_DIM = 0xFF0F7E8A;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public EchoCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.echo_chamber"),
                gui.createDrawableItemLike(ModItems.ECHO_CHAMBER.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(EchoDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        // The slate panel with a sculk-cyan edge, like the chamber's glow.
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        graphics.fill(1, 1, WIDTH - 1, 3, SCULK_DIM);
        graphics.fill(WIDTH / 2 - 30, 1, WIDTH / 2 + 30, 3, SCULK);
        // Table rules.
        graphics.fill(6, TABLE_Y + LINE - 1, WIDTH - 6, TABLE_Y + LINE, 0xFF4A4E5A);
        graphics.fill(6, TABLE_Y - 3, WIDTH - 6, TABLE_Y - 2, 0xFF4A4E5A);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EchoDisplay recipe, IFocusGroup focuses) {
        int max = EchoChamberBlockEntity.maxSheep();
        int side = ModConfigs.getInt(ModConfigs.SERVER.echoRadius) * 2 + 1;
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 14)
                .setBackground(slot, -1, -1)
                .addItemStacks(eggs(ModConfigs.SERVER.echoSources.get(), max))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.echo.herd", max, side + "x" + side).withColor(SCULK));
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.echo.herd_desc").withStyle(ChatFormatting.GRAY));
                });
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 14)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.ECHO_CHAMBER.get());
        builder.addSlot(RecipeIngredientRole.INPUT, 140, 14)
                .setBackground(slot, -1, -1)
                .add(Items.NOTE_BLOCK)
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.echo.noise",
                            ModConfigs.getInt(ModConfigs.SERVER.echoListenRadius)).withColor(SCULK));
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.echo.noise_desc").withStyle(ChatFormatting.GRAY));
                });
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, EchoDisplay recipe, IFocusGroup focuses) {
        int max = EchoChamberBlockEntity.maxSheep();
        int quiet = EchoChamberBlockEntity.quietBelow(), loud = EchoChamberBlockEntity.loudAbove();
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.echo.sweet_spot", quiet, loud)), WIDTH - 8, 10)
                .setPosition(4, 38).setColor(0xFF8CF08C).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.echo.sweet_spot_desc", quiet, loud));
        List<FormattedText> sheep = new ArrayList<>(), steady = new ArrayList<>(), erratic = new ArrayList<>();
        sheep.add(Component.translatable("jei.barelyfunctionalcontraptions.echo.col_sheep"));
        steady.add(Component.translatable("jei.barelyfunctionalcontraptions.echo.col_steady"));
        erratic.add(Component.translatable("jei.barelyfunctionalcontraptions.echo.col_erratic"));
        int rows = Math.min(max, 4);
        for (int i = 1; i <= rows; i++) {
            int n = i == rows ? max : i; // a big config: the last row is the most it counts
            sheep.add(Component.literal(Integer.toString(n)));
            steady.add(Component.literal(fe(EchoChamberBlockEntity.power(n, quiet, 1.0))));
            erratic.add(Component.literal(fe(EchoChamberBlockEntity.power(n, quiet, 0.0))));
        }
        int height = LINE * sheep.size();
        builder.addText(sheep, 40, height).setPosition(6, TABLE_Y).setLineSpacing(1).setColor(JeiTextures.TEXT_DIM)
                .setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(steady, 60, height).setPosition(50, TABLE_Y).setLineSpacing(1).setColor(0xFFFFD27F)
                .setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(erratic, 60, height).setPosition(112, TABLE_Y).setLineSpacing(1).setColor(0xFFC9A25A)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.echo.erratic_desc"));
        int sleep = ModConfigs.getInt(ModConfigs.SERVER.echoSleepTicks) / 20, shriek = ModConfigs.getInt(ModConfigs.SERVER.echoShriekTicks) / 20;
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.echo.quiet")), WIDTH / 2 - 4, 10).setPosition(4, HEIGHT - 12)
                .setColor(0xFF3FA9B5).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.echo.sleep", sleep));
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.echo.loud")), WIDTH / 2 - 4, 10).setPosition(WIDTH / 2, HEIGHT - 12)
                .setColor(0xFFE35D5D).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.echo.shriek", shriek,
                        ModConfigs.getInt(ModConfigs.SERVER.echoShriekCooldown) / 20));
    }

    /** Each listening resource's spawn egg, in a stack of up to {@code max}. */
    private static List<ItemStack> eggs(List<? extends String> sources, int max) {
        List<ItemStack> eggs = new ArrayList<>();
        for (String id : sources) {
            net.minecraft.resources.Identifier parsed = net.minecraft.resources.Identifier.tryParse(id);
            if (parsed != null) eggs.add(ResourceSheepSpawnEggItem.create(parsed, Math.min(64, max)));
        }
        return eggs;
    }

    private static String fe(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
