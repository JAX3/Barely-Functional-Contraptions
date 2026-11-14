package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.AltarStatus;
import com.jax3.barelyfunctionalcontraptions.sheep.blockentity.InfusionAltarBlockEntity;
import com.jax3.barelyfunctionalcontraptions.sheep.menu.InfusionAltarMenu;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.AltarRitual;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Window of the Infusion Altar: the energy buffer on the left, the feeding item in the centre of a cross of four
 * catalyst slots (the catalysts still needed show faded in the empty ones), a progress bar under it, and the tier and
 * FE/t on the right. Art: {@code textures/gui/infusion_altar.png}, see {@code tools/sheep/generate_textures.py}.
 */
public class InfusionAltarScreen extends AbstractContainerScreen<InfusionAltarMenu> {
    private static final Identifier TEXTURE = ResourcefulSheep.id("textures/gui/infusion_altar.png");
    private static final int TEX = 256;
    private static final int GAUGE_X = 11, GAUGE_Y = 19, GAUGE_W = 30, GAUGE_H = 64;
    private static final int FILL_U = 176, FILL_V = 0, SHIMMER_V = 66;
    private static final int BAR_X = 50, BAR_Y = 80, BAR_W = 66, BAR_H = 3, BAR_U = 176, BAR_V = 80;
    private static final int TEXT_COLOR = 0xFFDFE4F5, TEXT_DIM = 0xFFAEB4C6, TIER_COLOR = 0xFFC27CFF, USE_COLOR = 0xFFFFD25A;
    private static final int INFO_CENTRE = 142;
    private static final int LAMP_X = 9, LAMP_Y = 87, LAMP_U = 186, LAMP_V = 194;

    public InfusionAltarScreen(InfusionAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 193);
        this.inventoryLabelY = InfusionAltarMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    private Optional<SheepResource> resource() {
        return minecraft == null || minecraft.level == null ? Optional.empty()
                : InfusionAltarBlockEntity.resourceFor(minecraft.level, menu.getSlot(InfusionAltarBlockEntity.FEED).getItem());
    }

    /** The catalysts the loaded resource needs that aren't in the slots yet. */
    private List<SizedIngredient> missing(AltarRitual ritual) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < InfusionAltarBlockEntity.SIZE; i++) slots.add(menu.getSlot(i).getItem());
        List<SizedIngredient> missing = new ArrayList<>();
        for (SizedIngredient need : ritual.catalysts()) {
            AltarRitual one = new AltarRitual(ritual.tier(), List.of(need), ritual.energy());
            if (InfusionAltarBlockEntity.matchCatalysts(one, slots) == null) missing.add(need);
        }
        return missing;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX, TEX);
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
        int height = menu.getEnergy() > 0 ? Math.max(1, Mth.ceil(menu.getEnergyFraction() * GAUGE_H)) : 0;
        if (height > 0) {
            int offset = GAUGE_H - height;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, FILL_V + offset, GAUGE_W, height, TEX, TEX);
            if (height >= 2) {
                int frame = (int) (time / 3 % 4);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + GAUGE_X, y + GAUGE_Y + offset, FILL_U, SHIMMER_V + frame * 2, GAUGE_W, 2, TEX, TEX);
            }
        }
        int bar = Mth.ceil(menu.getProgressFraction() * BAR_W);
        if (bar > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + BAR_X, y + BAR_Y, BAR_U, BAR_V, bar, BAR_H, TEX, TEX);
        AltarStatus status = menu.getStatus();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + LAMP_X, y + LAMP_Y, LAMP_U + status.lamp * 8, LAMP_V, 8, 8, TEX, TEX);

        // Faded copies of the catalysts still needed, in the empty side slots.
        resource().ifPresent(r -> {
            int ghost = 0;
            for (SizedIngredient need : missing(r.altar())) {
                while (ghost < AltarRitual.MAX_CATALYSTS && menu.getSlot(1 + ghost).hasItem()) ghost++;
                if (ghost >= AltarRitual.MAX_CATALYSTS) break;
                int[] p = InfusionAltarMenu.SLOT_POS[1 + ghost];
                @SuppressWarnings("deprecation")
                ItemStack shown = need.ingredient().items().findFirst().map(h -> new ItemStack(h, need.count())).orElse(ItemStack.EMPTY);
                graphics.fakeItem(shown, x + p[0], y + p[1]);
                graphics.itemDecorations(font, shown, x + p[0], y + p[1]);
                graphics.fill(x + p[0], y + p[1], x + p[0] + 16, y + p[1] + 16, 0x90202028);
                ghost++;
            }
        });
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
        resource().ifPresent(r -> {
            centred(graphics, Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar.tier", r.altar().tier()), 22, TIER_COLOR);
            centred(graphics, Component.literal(fe(menu.getEnergyPerTick())), 40, menu.getStatus() == AltarStatus.INFUSING ? USE_COLOR : TEXT_DIM);
            centred(graphics, Component.literal("FE/t"), 50, TEXT_DIM);
        });
        graphics.text(font, menu.getStatus().displayName(), LAMP_X + 13, LAMP_Y + 1, menu.getStatus().color, false);
    }

    private void centred(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, INFO_CENTRE - font.width(text) / 2, y, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        List<Component> lines = new ArrayList<>();
        Optional<SheepResource> resource = resource();
        if (hoveredSlot != null && hoveredSlot.index < InfusionAltarBlockEntity.SIZE && !hoveredSlot.hasItem()) {
            boolean centre = hoveredSlot.index == InfusionAltarBlockEntity.FEED;
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar." + (centre ? "feed" : "catalyst")));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar." + (centre ? "feed" : "catalyst") + ".desc").withStyle(ChatFormatting.GRAY));
            if (!centre) resource.ifPresent(r -> addRitual(lines, r));
        } else if (hoveredSlot == null && isHovering(GAUGE_X - 2, GAUGE_Y - 2, GAUGE_W + 4, GAUGE_H + 4, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.energy", fe(menu.getEnergy()), fe(menu.getCapacity())));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar.energy.desc").withStyle(ChatFormatting.GRAY));
        } else if (hoveredSlot == null && (isHovering(BAR_X, BAR_Y - 2, BAR_W, BAR_H + 4, mouseX, mouseY) || isHovering(118, 18, 48, 44, mouseX, mouseY))) {
            resource.ifPresentOrElse(r -> {
                lines.add(Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar.infusing", r.name().copy().withColor(r.woolColor()),
                        Math.round(menu.getProgressFraction() * 100)));
                addRitual(lines, r);
            }, () -> lines.add(Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar.status.idle.desc").withStyle(ChatFormatting.GRAY)));
        } else if (hoveredSlot == null && isHovering(LAMP_X, LAMP_Y - 1, 130, 10, mouseX, mouseY)) {
            AltarStatus status = menu.getStatus();
            lines.add(status.displayName().copy().withColor(status.color));
            lines.add(Component.translatable(status.key() + ".desc", InfusionAltarBlockEntity.REACH).withStyle(ChatFormatting.GRAY));
        }
        if (!lines.isEmpty()) graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    /** "Tier 2: 1 Nether Star, 4 Soul Sand, ... and 200,000 FE." */
    @SuppressWarnings("deprecation")
    private void addRitual(List<Component> lines, SheepResource r) {
        AltarRitual ritual = r.altar();
        lines.add(Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar.needs", r.effectiveFeedingAmount(),
                menu.getSlot(InfusionAltarBlockEntity.FEED).getItem().getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
        List<SizedIngredient> missing = missing(ritual);
        for (SizedIngredient c : ritual.catalysts()) {
            Component name = c.ingredient().items().findFirst().map(h -> new ItemStack(h).getHoverName()).orElse(Component.literal("?"));
            lines.add(Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar.catalyst_line", c.count(), name)
                    .withStyle(missing.contains(c) ? ChatFormatting.RED : ChatFormatting.DARK_AQUA));
        }
        lines.add(Component.translatable("gui.barelyfunctionalcontraptions.infusion_altar.energy_line", fe(ritual.effectiveEnergy())).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
