package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepSkin;
import com.jax3.barelyfunctionalcontraptions.sheep.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * Draws a sheep that was fed a Melon Slice in the skin it was given ({@link PlayerSkinSheepModel}), over the sheep's
 * own body and under its fleece. The skin is looked up the way a placed Player Head looks its skin up: the default
 * skin for that UUID (Steve, Alex...) until the real one has downloaded. Lambs keep their own look.
 */
public class PlayerSkinSheepLayer extends RenderLayer<SheepRenderState, SheepModel> {
    public static final ContextKey<Skin> SKIN = new ContextKey<>(ResourcefulSheep.id("player_skin"));

    /** The skin texture to draw, and whether it has slim arms. */
    public record Skin(Identifier texture, boolean slim) {}

    private final PlayerSkinSheepModel wide;
    private final PlayerSkinSheepModel slim;

    public PlayerSkinSheepLayer(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityModelSet models) {
        super(renderer);
        this.wide = new PlayerSkinSheepModel(models.bakeLayer(PlayerSkinSheepModel.WIDE));
        this.slim = new PlayerSkinSheepModel(models.bakeLayer(PlayerSkinSheepModel.SLIM));
    }

    static void extract(Sheep sheep, SheepRenderState state) {
        if (state.isBaby || !sheep.hasData(ModAttachments.SHEEP_SKIN)) return;
        SheepSkin skin = sheep.getData(ModAttachments.SHEEP_SKIN);
        if (skin.profile().isEmpty()) return;
        var info = Minecraft.getInstance().playerSkinRenderCache().getOrDefault(skin.profile().get());
        state.setRenderData(SKIN, new Skin(info.playerSkin().body().texturePath(), info.playerSkin().model() == PlayerModelType.SLIM));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, SheepRenderState state, float yRot, float xRot) {
        Skin skin = state.getRenderData(SKIN);
        if (skin == null) return;
        coloredCutoutModelCopyLayerRender(skin.slim() ? slim : wide, skin.texture(), poseStack, collector, lightCoords, state, -1, 0);
    }
}
