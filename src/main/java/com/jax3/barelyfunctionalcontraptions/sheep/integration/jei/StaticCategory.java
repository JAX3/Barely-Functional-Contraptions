package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.StaticGeneratorBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceSheepSpawnEggItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
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
 * "Static Generator": sheep (plain, Copper and Redstone) at a Copper Comb, the generator, Honeycomb and an axe for
 * looking after its combs, and a table of the power for each number of sheep, plain and Copper. Everything is read
 * from the server's config, so it matches the machine.
 */
public class StaticCategory extends AbstractRecipeCategory<StaticDisplay> {
    public static final IRecipeType<StaticDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("static_generator"), StaticDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 134;
    private static final int TABLE_Y = 42, LINE = 10;
    private static final int PANEL = 0xFF2C2E36, EDGE = 0xFF17181C, SPARK = 0xFF5FB8FF, COPPER = 0xFFD88A63;
    private static final String COPPER_SHEEP = "barelyfunctionalcontraptions:copper", REDSTONE_SHEEP = "barelyfunctionalcontraptions:redstone";

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public StaticCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.barelyfunctionalcontraptions.static_generator"),
                gui.createDrawableItemLike(ModItems.STATIC_GENERATOR.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(StaticDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        graphics.fill(1, 1, WIDTH - 1, 3, COPPER);
        graphics.fill(WIDTH / 2 - 30, 1, WIDTH / 2 + 30, 3, SPARK);
        graphics.fill(6, TABLE_Y + LINE - 1, WIDTH - 6, TABLE_Y + LINE, 0xFF4A4E5A);
        graphics.fill(6, TABLE_Y - 3, WIDTH - 6, TABLE_Y - 2, 0xFF4A4E5A);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StaticDisplay recipe, IFocusGroup focuses) {
        int max = StaticGeneratorBlockEntity.maxSheep();
        List<ItemStack> sheep = new ArrayList<>();
        sheep.add(new ItemStack(Items.SHEEP_SPAWN_EGG, Math.min(64, max)));
        for (String id : List.of(COPPER_SHEEP, REDSTONE_SHEEP)) {
            net.minecraft.resources.Identifier parsed = net.minecraft.resources.Identifier.tryParse(id);
            if (parsed != null) sheep.add(ResourceSheepSpawnEggItem.create(parsed, Math.min(64, max)));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 14)
                .setBackground(slot, -1, -1)
                .addItemStacks(sheep)
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.static.herd", max,
                            StaticGeneratorBlockEntity.sheepPower((String) null), StaticGeneratorBlockEntity.sheepPower(COPPER_SHEEP),
                            StaticGeneratorBlockEntity.sheepPower(REDSTONE_SHEEP)).withColor(SPARK));
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.static.herd_desc").withStyle(ChatFormatting.GRAY));
                });
        builder.addSlot(RecipeIngredientRole.INPUT, 32, 14)
                .setBackground(slot, -1, -1)
                .add(new ItemStack(ModItems.COPPER_COMB.get(), ModConfigs.getInt(ModConfigs.SERVER.staticMaxCombs)))
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.static.combs", ModConfigs.getInt(ModConfigs.SERVER.staticMaxCombs))
                            .withColor(COPPER));
                    tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.static.combs_desc",
                            ModConfigs.getInt(ModConfigs.SERVER.staticWearTicks) / 1200).withStyle(ChatFormatting.GRAY));
                });
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, 14)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.STATIC_GENERATOR.get());
        builder.addSlot(RecipeIngredientRole.INPUT, 128, 14)
                .setBackground(slot, -1, -1)
                .add(Items.IRON_AXE)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.static.axe").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, 152, 14)
                .setBackground(slot, -1, -1)
                .add(Items.HONEYCOMB)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.static.honeycomb",
                        Math.round(ModConfigs.getDouble(ModConfigs.SERVER.staticWaxedEfficiency) * 100)).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, StaticDisplay recipe, IFocusGroup focuses) {
        int max = StaticGeneratorBlockEntity.maxSheep();
        List<FormattedText> count = new ArrayList<>(), plain = new ArrayList<>(), copper = new ArrayList<>();
        count.add(Component.translatable("jei.barelyfunctionalcontraptions.static.col_sheep"));
        plain.add(Component.translatable("jei.barelyfunctionalcontraptions.static.col_plain"));
        copper.add(Component.translatable("jei.barelyfunctionalcontraptions.static.col_copper"));
        int rows = Math.min(max, 6);
        for (int i = 1; i <= rows; i++) {
            int n = i == rows ? max : i; // a big config: the last row is the most it counts
            count.add(Component.literal(Integer.toString(n)));
            plain.add(Component.literal(fe(power(null, n))));
            copper.add(Component.literal(fe(power(COPPER_SHEEP, n))));
        }
        int height = LINE * count.size();
        builder.addText(count, 40, height).setPosition(6, TABLE_Y).setLineSpacing(1).setColor(JeiTextures.TEXT_DIM)
                .setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(plain, 60, height).setPosition(50, TABLE_Y).setLineSpacing(1).setColor(0xFFFFD27F)
                .setTextAlignment(HorizontalAlignment.CENTER);
        builder.addText(copper, 60, height).setPosition(112, TABLE_Y).setLineSpacing(1).setColor(COPPER)
                .setTextAlignment(HorizontalAlignment.CENTER);
        int waxed = (int) Math.round(ModConfigs.getDouble(ModConfigs.SERVER.staticWaxedEfficiency) * 100);
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.static.stages", waxed)), WIDTH - 8, 10)
                .setPosition(4, HEIGHT - 22).setColor(JeiTextures.TEXT_DIM).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.static.stages_desc"));
        builder.addText(List.of(Component.translatable("jei.barelyfunctionalcontraptions.static.shock")), WIDTH - 8, 10)
                .setPosition(4, HEIGHT - 12).setColor(0xFFE35D5D).setTextAlignment(HorizontalAlignment.CENTER)
                .setTooltip(Component.translatable("jei.barelyfunctionalcontraptions.static.shock_desc",
                        Math.round(ModConfigs.getDouble(ModConfigs.SERVER.staticShockAt) * 100)));
    }

    /** FE/t from {@code n} sheep of {@code resource} (null: plain) at fresh combs. */
    private static int power(String resource, int n) {
        return StaticGeneratorBlockEntity.power(new ArrayList<>(Collections.nCopies(n, (double) StaticGeneratorBlockEntity.sheepPower(resource))));
    }

    private static String fe(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
