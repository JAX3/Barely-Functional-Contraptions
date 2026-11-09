package com.jax3.functionalresources.sheep.integration.jei;

import com.jax3.functionalresources.sheep.ResourcefulSheep;
import com.jax3.functionalresources.sheep.items.ResourceWoolItem;
import com.jax3.functionalresources.sheep.resource.SheepResource;
import java.util.List;
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
import org.jspecify.annotations.Nullable;

/** "Resource Infusion": feeding item + sheep -> resource wool. */
public class InfusionCategory extends AbstractRecipeCategory<InfusionDisplay> {
    public static final IRecipeType<InfusionDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("resource_infusion"), InfusionDisplay.class);
    public static final int WIDTH = 150, HEIGHT = 58;

    private final IDrawable sheep;
    private final IDrawable background;
    private final IDrawable slot;
    private final IDrawable bigSlot;
    private final IDrawable arrowEmpty;
    private final IDrawable arrowFull;
    private final IDrawable plus;

    public InfusionCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.functionalresources.resource_infusion"),
                gui.drawableBuilder(JeiTextures.TEXTURE, 0, 0, 16, 16).setTextureSize(256, 256).build(), WIDTH, HEIGHT);
        this.sheep = gui.drawableBuilder(JeiTextures.TEXTURE, 0, 16, 32, 24).setTextureSize(256, 256).build();
        this.background = JeiTextures.sprite(gui, 0, 88, WIDTH, HEIGHT);   // slate panel, like the machine GUIs
        this.slot = JeiTextures.slot(gui);
        this.bigSlot = JeiTextures.bigSlot(gui);
        this.arrowEmpty = JeiTextures.arrowEmpty(gui);
        this.arrowFull = JeiTextures.arrowFilling(gui, 40);
        this.plus = JeiTextures.plus(gui);
    }

    @Override
    public void draw(InfusionDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                     net.minecraft.client.gui.GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        background.draw(graphics);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionDisplay recipe, IFocusGroup focuses) {
        SheepResource r = recipe.resource();
        int amount = r.effectiveFeedingAmount();
        List<ItemStack> feeding = r.feedingItems().stream().map(h -> new ItemStack(h, amount)).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 8)
                .setBackground(slot, -1, -1)
                .addItemStacks(feeding)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.functionalresources.consumed", amount).withStyle(ChatFormatting.GRAY)));
        int drop = Math.max(1, r.effectiveWoolDropCount());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 8)
                .setBackground(bigSlot, -5, -5)
                .add(ResourceWoolItem.create(r.id(), drop))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.functionalresources.per_shearing", drop).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, InfusionDisplay recipe, IFocusGroup focuses) {
        builder.addDrawableWidget(plus).setPosition(27, 10);
        builder.addDrawableWidget(sheep).setPosition(42, 4);
        builder.addDrawableWidget(arrowEmpty).setPosition(88, 8);
        builder.addDrawableWidget(arrowFull).setPosition(88, 8);
        SheepResource r = recipe.resource();
        int uses = r.effectiveInfusionUses();
        Component usesLine = uses < 0
                ? Component.translatable("jei.functionalresources.uses_permanent")
                : Component.translatable("jei.functionalresources.uses", uses);
        builder.addText(List.<FormattedText>of(usesLine, Component.translatable("jei.functionalresources.category", r.category().displayName())), WIDTH, 22)
                .setPosition(0, 33)
                .setColor(JeiTextures.TEXT)
                .setTextAlignment(mezz.jei.api.gui.placement.HorizontalAlignment.CENTER);
    }

    @Override
    public @Nullable Identifier getIdentifier(InfusionDisplay recipe) {
        return recipe.resource().id();
    }
}
