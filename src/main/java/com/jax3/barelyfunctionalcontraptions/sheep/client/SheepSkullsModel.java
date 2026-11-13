package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Wither sheep's two extra heads ({@code extra_heads}): Wither Skeleton Skulls either side of the sheep's own
 * head, drawn exactly like the placed skull block.
 *
 * <p>Each skull is the vanilla mob-head cube (8x8x8 at texture offset 0,0 of a 64x32 texture, the same layout the
 * skull block uses) scaled down to the sheep's head size, and drawn with the game's own Wither Skeleton texture, so a
 * resource pack that retextures the skull retextures these too. They hang off a copy of the sheep's head, so they
 * graze with it.</p>
 */
public class SheepSkullsModel extends EntityModel<SheepRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourcefulSheep.id("sheep_skulls"), "main");
    public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png");

    /** 8 pixels scaled to about 5, a little smaller than the sheep's 6-pixel head. */
    private static final float SCALE = 0.65F;

    private final ModelPart head;
    private final ModelPart leftSkull;
    private final ModelPart rightSkull;

    public SheepSkullsModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leftSkull = head.getChild("left_skull");
        this.rightSkull = head.getChild("right_skull");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The sheep's head pivot; its head spans x -3..3 (-3.6..3.6 with the fleece), y -4..2 and z -6..2 from here.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 6.0F, -8.0F));
        // The skull cube's pivot is the middle of its bottom face; scaled, its centre sits 2.6 above that.
        CubeListBuilder skull = CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F);
        head.addOrReplaceChild("left_skull", skull, PartPose.offset(6.2F, 1.6F, -2.8F).withScale(SCALE));
        head.addOrReplaceChild("right_skull", skull, PartPose.offset(-6.2F, 1.6F, -2.8F).withScale(SCALE));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(SheepRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // The head, exactly as SheepModel poses it (including lowering it to graze).
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.y += state.headEatPositionScale * 9.0F * state.ageScale;
        head.xRot = state.headEatAngleScale;
        // The skulls turn slightly outwards and look about a little on their own, like the Wither's.
        leftSkull.yRot = -0.25F + Mth.sin(t * 0.05F) * 0.2F;
        leftSkull.xRot = Mth.sin(t * 0.07F + 1.0F) * 0.12F;
        rightSkull.yRot = 0.25F + Mth.sin(t * 0.045F + 2.0F) * 0.2F;
        rightSkull.xRot = Mth.sin(t * 0.06F + 3.0F) * 0.12F;
    }
}
