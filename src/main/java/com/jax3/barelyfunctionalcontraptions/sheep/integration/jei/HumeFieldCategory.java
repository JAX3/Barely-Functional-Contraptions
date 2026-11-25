package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.HumeFieldGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.util.Numbers;
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
import net.minecraft.util.RandomSource;

/**
 * "Hume Field Generator": the generator, a Hume Energy Cube and an Energy Cable for what it powers, its random output
 * range and average, its nauseating pulse, and that it has no buffer, with a sample trace of its output. Read from the
 * server's config, so it matches the machine.
 */
public class HumeFieldCategory extends AbstractRecipeCategory<HumeFieldDisplay> {
    public static final IRecipeType<HumeFieldDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("hume_field_generator"), HumeFieldDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 96;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, PURPLE = 0xFF9A52F0, PURPLE_DIM = 0xFF5C2AA6, FIELD = 0xFFC99BFF;
    private static final int TRACE_X = 18, TRACE_Y = 15, TRACE_W = 50, TRACE_H = 14;

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public HumeFieldCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.hume_field_generator"),
                gui.createDrawableItemLike(ModItems.HUME_FIELD_GENERATOR.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(HumeFieldDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        // The slate panel with a purple edge, like the generator's glow.
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        graphics.fill(1, 1, WIDTH - 1, 3, PURPLE_DIM);
        graphics.fill(WIDTH / 2 - 30, 1, WIDTH / 2 + 30, 3, PURPLE);
        // A sample of its output tick by tick (the same every time): never the same twice, always within its range.
        RandomSource random = RandomSource.create(1776);
        float low = HumeFieldGeneratorBlockEntity.rangeMin() / (float) Math.max(1, HumeFieldGeneratorBlockEntity.rangeMax());
        for (int i = 0; i < TRACE_W / 2; i++) {
            int h = Math.max(1, Math.round(TRACE_H * (low + (1 - low) * random.nextFloat())));
            graphics.fill(TRACE_X + i * 2, TRACE_Y + TRACE_H - h, TRACE_X + i * 2 + 1, TRACE_Y + TRACE_H, i == TRACE_W / 2 - 1 ? FIELD : PURPLE);
        }
        graphics.fill(102, 21, 136, 22, PURPLE_DIM); // its power, out to what it powers
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HumeFieldDisplay recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 14)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.HUME_FIELD_GENERATOR.get());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 140, 14)
                .setBackground(slot, -1, -1)
                .add(ModItems.HUME_ENERGY_CUBE.get())
                .add(ModItems.ENERGY_CABLE.get())
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.powers").withColor(FIELD));
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.powers_desc").withStyle(ChatFormatting.GRAY));
                });
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, HumeFieldDisplay recipe, IFocusGroup focuses) {
        int min = HumeFieldGeneratorBlockEntity.rangeMin(), max = HumeFieldGeneratorBlockEntity.rangeMax();
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.range", Numbers.grouped(min), Numbers.grouped(max))),
                        WIDTH - 8, 10).setPosition(4, 46).setColor(FIELD).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.range_desc"));
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.average", Numbers.grouped((min + max) / 2))),
                        WIDTH - 8, 10).setPosition(4, 57).setColor(0xFFFFD27F).setTextAlignment(HorizontalAlignment.CENTER);
        int interval = com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.humeFieldPulseInterval);
        int radius = com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.humeFieldNauseaRadius);
        if (interval > 0 && radius > 0) {
            int seconds = com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.getInt(com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs.SERVER.humeFieldNauseaSeconds);
            builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.pulse", Math.max(1, Math.round(interval / 20F)), radius)),
                            WIDTH - 8, 10).setPosition(4, 68).setColor(0xFF9A7ACF).setTextAlignment(HorizontalAlignment.CENTER)
                    .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.pulse_desc", seconds, radius));
        }
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.no_buffer")), WIDTH - 8, 10)
                .setPosition(4, HEIGHT - 14).setColor(0xFFE35D5D).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.hume_field.no_buffer_desc"));
    }
}
