package com.jax3.barelyfunctionalcontraptions.sheep.client;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The show-area button in a machine's window (Automatic Shears, Blaze Sheep Furnace): a 16x16 button with a spyglass,
 * framed in the machine's outline colour while the area is shown. It is a real widget, so clicks reach it the normal
 * way (and keyboard focus and narration work); pressing it sends the menu's area button to the server.
 */
public class AreaToggleButton extends AbstractButton {
    private static final ItemStack ICON = new ItemStack(Items.SPYGLASS);
    public static final int SIZE = 16;

    private final Identifier texture;
    private final int u, v;
    private final BooleanSupplier shown;
    private final int frameColor;
    private final int containerId;
    private final int buttonId;

    /**
     * @param texture  the window's texture, with the button's normal background at (u, v) and the hovered one at (u + 16, v)
     * @param shown    whether the area is shown (from the menu's synced data)
     * @param buttonId the menu button that toggles the area
     */
    public AreaToggleButton(int x, int y, Identifier texture, int u, int v, BooleanSupplier shown, int frameColor, int containerId, int buttonId) {
        super(x, y, SIZE, SIZE, Component.translatable("gui.barelyfunctionalcontraptions.area.button"));
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.shown = shown;
        this.frameColor = frameColor;
        this.containerId = containerId;
        this.buttonId = buttonId;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(containerId, buttonId);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), u + (isHoveredOrFocused() ? SIZE : 0), v, SIZE, SIZE, 256, 256);
        graphics.fakeItem(ICON, getX(), getY());
        if (shown.getAsBoolean()) graphics.outline(getX() - 1, getY() - 1, SIZE + 2, SIZE + 2, frameColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
