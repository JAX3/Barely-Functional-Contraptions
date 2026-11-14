package com.jax3.barelyfunctionalcontraptions.sheep.integration.jei;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.items.ResourceWoolItem;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModItems;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.AltarRitual;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jspecify.annotations.Nullable;

/**
 * "Infusion Altar": the feeding item in the centre of a cross of catalysts, as in the altar's window, then the
 * sheep it infuses and the wool it gives. Below: the tier and the power it takes.
 */
public class AltarCategory extends AbstractRecipeCategory<AltarDisplay> {
    public static final IRecipeType<AltarDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("infusion_altar"), AltarDisplay.class);
    public static final int WIDTH = 150, HEIGHT = 86;
    /** Item positions: centre, then north, east, south, west, matching the panel on the JEI sheet. */
    private static final int[][] SLOTS = {{30, 28}, {30, 6}, {52, 28}, {30, 50}, {8, 28}};

    private final IDrawable background;
    private final IDrawable sheep;
    private final IDrawable bigSlot;
    private final IDrawable arrowEmpty;
    private final IDrawable arrowFull;

    public AltarCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("block.barelyfunctionalcontraptions.infusion_altar"),
                gui.createDrawableItemLike(ModItems.INFUSION_ALTAR.get()), WIDTH, HEIGHT);
        this.background = JeiTextures.sprite(gui, 0, 150, WIDTH, HEIGHT);
        this.sheep = JeiTextures.sprite(gui, 0, 16, 32, 24);
        this.bigSlot = JeiTextures.bigSlot(gui);
        this.arrowEmpty = JeiTextures.arrowEmpty(gui);
        this.arrowFull = JeiTextures.arrowFilling(gui, 100);
    }

    @Override
    public void draw(AltarDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        background.draw(graphics);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setRecipe(IRecipeLayoutBuilder builder, AltarDisplay recipe, IFocusGroup focuses) {
        SheepResource r = recipe.resource();
        int amount = r.effectiveFeedingAmount();
        builder.addSlot(RecipeIngredientRole.INPUT, SLOTS[0][0], SLOTS[0][1])
                .addItemStacks(r.feedingItems().stream().map(h -> new ItemStack(h, amount)).toList())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.consumed", amount).withStyle(ChatFormatting.GRAY)));
        List<SizedIngredient> catalysts = r.altar().catalysts();
        for (int i = 0; i < AltarRitual.MAX_CATALYSTS; i++) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, SLOTS[1 + i][0], SLOTS[1 + i][1]);
            if (i < catalysts.size()) {
                SizedIngredient c = catalysts.get(i);
                slot.addItemStacks(c.ingredient().items().map(h -> new ItemStack(h, c.count())).toList())
                        .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                                Component.translatable("jei.barelyfunctionalcontraptions.altar_catalyst").withStyle(ChatFormatting.GRAY)));
            }
        }
        int drop = Math.max(1, r.effectiveWoolDropCount());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 122, 28)
                .setBackground(bigSlot, -5, -5)
                .add(ResourceWoolItem.create(r.id(), drop))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.barelyfunctionalcontraptions.per_shearing", drop).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, AltarDisplay recipe, IFocusGroup focuses) {
        builder.addDrawableWidget(sheep).setPosition(80, 4);
        builder.addDrawableWidget(arrowEmpty).setPosition(84, 30);
        builder.addDrawableWidget(arrowFull).setPosition(84, 30);
        SheepResource r = recipe.resource();
        Component line = Component.translatable("jei.barelyfunctionalcontraptions.altar_line", r.altar().tier(),
                String.format(Locale.ROOT, "%,d", r.altar().effectiveEnergy()));
        builder.addText(List.<FormattedText>of(line), WIDTH - 8, 12)
                .setPosition(4, 72)
                .setColor(JeiTextures.TEXT)
                .setTextAlignment(mezz.jei.api.gui.placement.HorizontalAlignment.CENTER);
    }

    @Override
    public @Nullable Identifier getIdentifier(AltarDisplay recipe) {
        return recipe.resource().id().withSuffix("_altar");
    }
}
