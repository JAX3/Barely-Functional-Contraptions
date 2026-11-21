package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.BarelyFunctionalContraptions;
import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.CrateBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * "Wool-Lined Crate": any one item into the crate (how many it holds) and a comparator out (how full); then what a
 * sneak-click does with dyed wool (its lining), a Wool Bale (more stacks) and a Crate Strap (no bursting), each with a
 * label under it, and a line on bursting.
 */
public class CrateCategory extends AbstractRecipeCategory<CrateDisplay> {
    public static final IRecipeType<CrateDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("wool_lined_crate"), CrateDisplay.class);
    public static final int WIDTH = 176, HEIGHT = 100;
    private static final int TOP_Y = 12, UPGRADES_Y = 53;
    private static final int PANEL = 0xFF3A2C20, EDGE = 0xFF1E160F, ARROW = 0xFF8A6A48, RULE = 0xFF5A4532;
    private static final int[] UPGRADE_X = {22, 80, 138};

    private final IDrawableStatic slot;
    private final IDrawableStatic bigSlot;

    public CrateCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("block.barelyfunctionalcontraptions.wool_lined_crate"),
                gui.createDrawableItemLike(ModItems.CRATE.get()), WIDTH, HEIGHT);
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
    }

    @Override
    public void draw(CrateDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, WIDTH, HEIGHT, EDGE);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, PANEL);
        // Its wool lining, round the top edge, in every colour.
        DyeColor[] colours = DyeColor.values();
        float w = (WIDTH - 2) / (float) colours.length;
        for (int i = 0; i < colours.length; i++) {
            graphics.fill(1 + Math.round(i * w), 1, 1 + Math.round((i + 1) * w), 3, 0xFF000000 | colours[i].getTextureDiffuseColor());
        }
        // item -> crate -> comparator
        graphics.fill(50, TOP_Y + 7, 68, TOP_Y + 9, ARROW);
        graphics.fill(66, TOP_Y + 5, 68, TOP_Y + 11, ARROW);
        graphics.fill(108, TOP_Y + 7, 126, TOP_Y + 9, ARROW);
        graphics.fill(124, TOP_Y + 5, 126, TOP_Y + 11, ARROW);
        graphics.fill(6, UPGRADES_Y - 6, WIDTH - 6, UPGRADES_Y - 5, RULE);
        graphics.fill(6, 84, WIDTH - 6, 85, RULE);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrateDisplay recipe, IFocusGroup focuses) {
        int stacks = ModConfigs.getInt(ModConfigs.SERVER.crateStacks);
        List<ItemStack> anything = new ArrayList<>();
        for (var item : List.of(Items.COBBLESTONE, Items.REDSTONE, Items.WHEAT, Items.OAK_LOG, Items.IRON_INGOT, Items.ENDER_PEARL)) anything.add(new ItemStack(item));
        builder.addSlot(RecipeIngredientRole.INPUT, 26, TOP_Y)
                .setBackground(slot, -1, -1)
                .addItemStacks(anything)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.crate.items", stacks).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 80, TOP_Y)
                .setBackground(bigSlot, -5, -5)
                .add(ModItems.CRATE.get());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 134, TOP_Y)
                .setBackground(slot, -1, -1)
                .add(Items.COMPARATOR)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.crate.comparator").withStyle(ChatFormatting.GRAY)));
        List<ItemStack> wools = new ArrayList<>();
        for (DyeColor colour : DyeColor.values()) wools.add(new ItemStack(com.jax3.barelyfunctionalcontraptions.compat.Vanilla.wool(colour)));
        builder.addSlot(RecipeIngredientRole.INPUT, UPGRADE_X[0], UPGRADES_Y)
                .setBackground(slot, -1, -1)
                .addItemStacks(wools)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.crate.lining").withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, UPGRADE_X[1], UPGRADES_Y)
                .setBackground(slot, -1, -1)
                .add(com.jax3.barelyfunctionalcontraptions.sheep.items.WoolBaleItem.create(BarelyFunctionalContraptions.id("iron"), 1))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.crate.bale",
                        ModConfigs.getInt(ModConfigs.SERVER.crateStacksPerBale), CrateBlockEntity.maxBales()).withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, UPGRADE_X[2], UPGRADES_Y)
                .setBackground(slot, -1, -1)
                .add(ModItems.CRATE_STRAP.get())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                        Component.translatable("jei.barelyfunctionalcontraptions.crate.strap").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CrateDisplay recipe, IFocusGroup focuses) {
        int stacks = ModConfigs.getInt(ModConfigs.SERVER.crateStacks);
        builder.addText(List.<net.minecraft.network.chat.FormattedText>of(Component.translatable("jei.barelyfunctionalcontraptions.crate.holds", stacks, stacks * 64)), WIDTH - 8, 10)
                .setPosition(4, TOP_Y + 25).setColor(JeiTextures.TEXT_DIM).setTextAlignment(HorizontalAlignment.CENTER);
        String[] labels = {"lining_label", "bale_label", "strap_label"};
        for (int i = 0; i < 3; i++) {
            Component label = i == 1 ? Component.translatable("jei.barelyfunctionalcontraptions.crate." + labels[i], ModConfigs.getInt(ModConfigs.SERVER.crateStacksPerBale))
                    : Component.translatable("jei.barelyfunctionalcontraptions.crate." + labels[i]);
            builder.addText(List.<net.minecraft.network.chat.FormattedText>of(label), 58, 10).setPosition(UPGRADE_X[i] + 8 - 29, UPGRADES_Y + 19).setColor(JeiTextures.TEXT_DIM)
                    .setTextAlignment(HorizontalAlignment.CENTER);
        }
        builder.addText(List.<net.minecraft.network.chat.FormattedText>of(Component.translatable("jei.barelyfunctionalcontraptions.crate.burst")), WIDTH - 8, 12)
                .setPosition(4, 88).setColor(0xFFE35D5D).setTextAlignment(HorizontalAlignment.CENTER);
    }
}
