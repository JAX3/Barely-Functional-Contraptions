package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.config.ModConfigs;
import com.jax3.barelyfunctionalcontraptions.sheep.entity.SheepInfusionEvents;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepResource;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.animal.sheep.Sheep;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;

/**
 * Renders resource-infused wool on sheep, with small blocks of the resource (Block of Iron, Block of Diamond...)
 * growing out of the fleece.
 *
 * <p>A render-state modifier ({@link #extract}) runs after vanilla extraction: for infused sheep that have wool it
 * stores the colours in the render state and marks the state as sheared so the vanilla wool layer is skipped. This
 * layer then draws the same vanilla fur model (so every animation still lines up) with a greyscale "resource wool"
 * texture tinted with the resource colour, plus a glinting accent overlay. Any resource, including ones added by
 * data packs, is therefore rendered without needing its own texture.</p>
 */
public class InfusedWoolLayer extends RenderLayer<SheepRenderState, SheepModel> {
    public static final ContextKey<Colors> INFUSED_WOOL = new ContextKey<>(ResourcefulSheep.id("infused_wool"));
    private static final Identifier WOOL_TEXTURE = ResourcefulSheep.id("textures/entity/sheep/infused_wool.png");
    /** The melon sheep's fleece (see {@link SheepInfusionEvents#isMelonSheep}), drawn untinted. */
    private static final Identifier MELON_WOOL_TEXTURE = ResourcefulSheep.id("textures/entity/sheep/melon_wool.png");
    private static final Identifier ACCENT_TEXTURE = ResourcefulSheep.id("textures/entity/sheep/infused_wool_accent.png");

    /** Lambs use vanilla's baby fleece (its own shape and texture), tinted with the resource's wool colour. */
    private static final Identifier BABY_WOOL_TEXTURE = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool_baby.png");

    /** Extra body parts ({@link SheepExtrasModel}) and the skull face and leg bones of {@link Feature#BONES}. */
    private static final Identifier EXTRAS_TEXTURE = ResourcefulSheep.id("textures/entity/sheep/sheep_extras.png");
    private static final Identifier BONES_TEXTURE = ResourcefulSheep.id("textures/entity/sheep/sheep_bones.png");
    /** A copy of the vanilla sheep body, a hair larger so the bones texture sits just over the skin. */
    public static final ModelLayerLocation BONES_LAYER = new ModelLayerLocation(ResourcefulSheep.id("sheep_bones"), "main");

    private final EntityModel<SheepRenderState> model;
    private final EntityModel<SheepRenderState> babyModel;
    private final SheepExtrasModel extras;
    private final SheepExtrasModel eyes;
    private final SheepModel bones;
    private final SheepSkullsModel skulls;
    private final SheepSkullFaceModel skullFace;

    public InfusedWoolLayer(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityModelSet models) {
        super(renderer);
        this.model = new SheepFurModel(models.bakeLayer(ModelLayers.SHEEP_WOOL));
        this.babyModel = new SheepFurModel(models.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
        this.extras = SheepExtrasModel.parts(models.bakeLayer(SheepExtrasModel.LAYER));
        this.eyes = SheepExtrasModel.eyes(models.bakeLayer(SheepExtrasModel.LAYER));
        this.bones = new SheepModel(models.bakeLayer(BONES_LAYER));
        this.skulls = new SheepSkullsModel(models.bakeLayer(SheepSkullsModel.LAYER));
        this.skullFace = new SheepSkullFaceModel(models.bakeLayer(SheepSkullFaceModel.LAYER));
    }

    /** The vanilla sheep body ({@code SheepModel.createBodyLayer}) inflated by a tiny amount. */
    public static LayerDefinition createBonesLayer() {
        CubeDeformation g = new CubeDeformation(0.04F);
        var mesh = net.minecraft.client.model.QuadrupedModel.createBodyMesh(12, false, true, g);
        var root = mesh.getRoot();
        root.addOrReplaceChild("head", net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, -6.0F, 6.0F, 6.0F, 8.0F, g),
                net.minecraft.client.model.geom.PartPose.offset(0.0F, 6.0F, -8.0F));
        root.addOrReplaceChild("body", net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(28, 8).addBox(-4.0F, -10.0F, -7.0F, 8.0F, 16.0F, 6.0F, g),
                net.minecraft.client.model.geom.PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * What to draw: wool colours ({@code accent} is 0 when the sparkle overlay is disabled), the resource block that
     * sprouts from the fleece, and how many of them are showing.
     */
    public record Colors(int wool, int accent, ItemStackRenderState growth, int growthCount, SheepAppearance appearance) {}

    // ---------------------------------------------------------------- blocks: small blocks clustered in the fleece

    /**
     * Where block growths sprout on the fleece, in sheep model pixels (x across, z along the body), with a size,
     * a yaw and a lean, ordered so the first ones stay while the infusion runs down.
     */
    private static final float[][] BLOCK_SPOTS = {
            // x,    z,    scale, yaw,  lean
            {-2.2F, -4.5F, 0.58F, 20F, 10F},
            {2.4F, 2.5F, 0.54F, -35F, -12F},
            {1.6F, -6.8F, 0.42F, 55F, -8F},
            {-2.6F, 4.6F, 0.44F, 10F, 14F},
            {0.2F, -0.8F, 0.38F, 70F, 6F},
            {3.6F, -2.6F, 0.32F, -15F, -20F},
    };
    /** Top of the fleece in model pixels, sunk slightly into the wool. */
    private static final float BLOCK_FLEECE_TOP = 4.25F + 0.3F;

    static void extract(Sheep sheep, SheepRenderState state) {
        if (state.isSheared) return;
        // The melon sheep: a grown sheep given the melon skin wears melon rind instead of wool, infused or not.
        boolean melon = !state.isBaby && SheepInfusionEvents.isMelonSheep(sheep);
        Optional<SheepResource> resource = SheepInfusionEvents.getInfusedResource(sheep);
        if (resource.isEmpty()) {
            if (melon) {
                state.setRenderData(INFUSED_WOOL, new Colors(-1, 0, new ItemStackRenderState(), 0, SheepAppearance.NONE.withWoolTexture(MELON_WOOL_TEXTURE)));
                state.isSheared = true;
            }
            return;
        }
        int wool = ARGB.opaque(resource.get().woolColor());
        if (ModConfigs.getBool(ModConfigs.SERVER.preserveWoolColour)) {
            float strength = (float) ModConfigs.getDouble(ModConfigs.CLIENT.dyeTintStrength);
            wool = ARGB.srgbLerp(strength, wool, ARGB.opaque(state.getWoolColor()));
        }
        if (state.isBaby) {
            // A lamb born infused (see SheepInfusionEvents.onBabySpawn): its fleece takes the resource's colour; the
            // sparkle and resource blocks only appear once it has grown up.
            state.setRenderData(INFUSED_WOOL, new Colors(wool, 0, new ItemStackRenderState(), 0, SheepAppearance.NONE));
            state.isSheared = true;
            return;
        }
        int accent = ModConfigs.getBool(ModConfigs.CLIENT.showSheepSparkleOverlay) ? ARGB.opaque(resource.get().accentColor()) : 0;

        ItemStackRenderState growth = new ItemStackRenderState();
        int count = 0;
        if (ModConfigs.getBool(ModConfigs.CLIENT.showResourceGrowths)) {
            ItemStack stack = resource.get().growthStack();
            if (!stack.isEmpty()) {
                // The item-frame (FIXED) transform: a block sits as a small cube.
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(growth, stack, ItemDisplayContext.FIXED, sheep.level(), sheep, sheep.getId());
                // More blocks while the infusion is fresh; they drop off as it is sheared away.
                int uses = SheepInfusionEvents.getInfusion(sheep).usesRemaining();
                int max = resource.get().effectiveInfusionUses();
                count = uses < 0 || max <= 0 ? BLOCK_SPOTS.length : Mth.clamp(Mth.ceil(BLOCK_SPOTS.length * (float) uses / max), 2, BLOCK_SPOTS.length);
            }
        }
        SheepAppearance appearance = resource.get().appearance();
        if (melon) {
            // Melon rind as drawn, without the resource's tint or sparkle; its features and growths still show.
            appearance = appearance.withWoolTexture(MELON_WOOL_TEXTURE);
            wool = -1;
            accent = 0;
        }
        // Burning sheep use the vanilla on-fire flames; they never actually burn.
        if (appearance.has(Feature.ON_FIRE)) state.displayFireAnimation = true;
        state.setRenderData(INFUSED_WOOL, new Colors(wool, accent, growth, count, appearance));
        state.isSheared = true; // hides the vanilla wool layer; the undercoat layer is unaffected
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, SheepRenderState state, float yRot, float xRot) {
        Colors colors = state.getRenderData(INFUSED_WOOL);
        if (colors == null) return;
        if (state.isBaby) {
            coloredCutoutModelCopyLayerRender(babyModel, BABY_WOOL_TEXTURE, poseStack, collector, lightCoords, state, colors.wool(), 1);
            return;
        }
        SheepAppearance appearance = colors.appearance();
        // Glowing fleece is drawn at full brightness; sculk only lights up its strands.
        int woolLight = appearance.has(Feature.GLOWING) ? LightCoordsUtil.FULL_BRIGHT : lightCoords;
        int accentLight = appearance.has(Feature.GLOWING) || appearance.has(Feature.SCULK) ? LightCoordsUtil.FULL_BRIGHT : lightCoords;
        coloredCutoutModelCopyLayerRender(model, appearance.woolTexture().orElse(WOOL_TEXTURE), poseStack, collector, woolLight, state, colors.wool(), 1);
        if (colors.accent() != 0) {
            coloredCutoutModelCopyLayerRender(model, ACCENT_TEXTURE, poseStack, collector, accentLight, state, colors.accent(), 2);
        }
        submitFeatures(poseStack, collector, lightCoords, state, colors);
        if (state.isInvisible || colors.growthCount() <= 0 || colors.growth().isEmpty()) return;
        submitBlocks(poseStack, collector, woolLight, state, colors);
    }

    /** The extra body parts, bones and glowing eyes a resource's {@code appearance} asks for. */
    private void submitFeatures(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, SheepRenderState state, Colors colors) {
        SheepAppearance appearance = colors.appearance();
        if ((appearance.features().isEmpty() && appearance.skullTexture().isEmpty()) || state.isInvisible) return;
        int tint = ARGB.opaque(appearance.featureColor());
        if (appearance.has(Feature.BONES)) {
            coloredCutoutModelCopyLayerRender(bones, BONES_TEXTURE, poseStack, collector, lightCoords, state, tint, 1);
        }
        if (appearance.skullTexture().isPresent()) {
            // A mob's own skull texture over the head, untinted (the Skeleton sheep wears a Skeleton Skull).
            coloredCutoutModelCopyLayerRender(skullFace, appearance.skullTexture().get(), poseStack, collector, lightCoords, state, -1, 1);
        }
        if (appearance.has(Feature.EXTRA_HEADS)) {
            // The game's own Wither Skeleton Skull texture, untinted.
            coloredCutoutModelCopyLayerRender(skulls, SheepSkullsModel.TEXTURE, poseStack, collector, lightCoords, state, -1, 1);
        }
        if (appearance.has(Feature.WINGS) || appearance.has(Feature.EXTRA_LEGS)
                || appearance.has(Feature.TENTACLES) || appearance.has(Feature.SCULK)) {
            int light = appearance.has(Feature.GLOWING) ? LightCoordsUtil.FULL_BRIGHT : lightCoords;
            coloredCutoutModelCopyLayerRender(extras, EXTRAS_TEXTURE, poseStack, collector, light, state, tint, 1);
        }
        if (appearance.has(Feature.GLOWING_EYES)) {
            collector.order(3).submitModel(eyes, state, poseStack, RenderTypes.eyes(EXTRAS_TEXTURE), LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, ARGB.opaque(colors.accent() != 0 ? colors.accent() : 0xFF2020), null, state.outlineColor);
        }
    }

    /** Blocks: little blocks of the resource sprouting from the fleece (the original look). */
    private void submitBlocks(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, SheepRenderState state, Colors colors) {
        // Model space has +Y pointing down, so items are flipped upright with a negative scale (as vanilla does for
        // items worn on heads).
        for (int i = 0; i < colors.growthCount(); i++) {
            float[] spot = BLOCK_SPOTS[i];
            float scale = spot[2];
            poseStack.pushPose();
            getParentModel().root().translateAndRotate(poseStack);
            poseStack.translate(spot[0] / 16.0F, BLOCK_FLEECE_TOP / 16.0F - scale * 0.22F, (spot[1] + 2.0F) / 16.0F);
            poseStack.rotateDegrees(Axis.YP, spot[3]);
            poseStack.rotateDegrees(Axis.ZP, spot[4]);
            poseStack.scale(scale, -scale, -scale);
            colors.growth().submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
