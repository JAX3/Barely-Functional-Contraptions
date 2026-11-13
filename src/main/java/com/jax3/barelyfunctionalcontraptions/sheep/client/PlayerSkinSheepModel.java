package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * A sheep wearing a player's skin: the vanilla sheep's shape, with every part cut from a 64x64 player skin.
 *
 * <ul>
 *   <li>the skin's head (and hat layer) becomes the sheep's head, squashed from 8x8x8 to the sheep's 6x6x8;</li>
 *   <li>its body becomes the sheep's body, stretched from 8x12x4 to 8x16x6, back up and chest down;</li>
 *   <li>its legs become the back legs and its arms the front legs (they are the same 4x12x4 size; a slim skin's
 *       3-wide arms are widened to 4).</li>
 * </ul>
 *
 * <p>The parts are named like {@link SheepModel}'s and it extends it, so it walks and grazes exactly like the sheep
 * under it. Every part is a hair larger than the sheep's so the skin covers it.</p>
 */
public class PlayerSkinSheepModel extends SheepModel {
    public static final ModelLayerLocation WIDE = new ModelLayerLocation(ResourcefulSheep.id("player_skin_sheep"), "wide");
    public static final ModelLayerLocation SLIM = new ModelLayerLocation(ResourcefulSheep.id("player_skin_sheep"), "slim");

    /** Just enough to sit over the sheep's own surfaces without flickering. */
    private static final CubeDeformation OVER = new CubeDeformation(0.06F);
    /** The hat and jacket layers, as on a player. */
    private static final CubeDeformation OUTER = new CubeDeformation(0.4F);

    public PlayerSkinSheepModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer(boolean slim) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head: pivot as SheepModel (0, 6, -8); the sheep's head box is x -3..3, y -4..2, z -6..2 around it.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 6.0F, -8.0F));
        head.addOrReplaceChild("skin_head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, OVER)
                        .texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, OUTER),
                PartPose.offset(0.0F, -1.0F, -2.0F).scaled(0.75F, 0.75F, 1.0F));

        // Body: SheepModel's body is box (-4, -10, -7) 8x16x6 turned 90 degrees to lie down; this fills the same box.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        body.addOrReplaceChild("skin_body", CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F, OVER)
                        .texOffs(16, 32).addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F, OUTER),
                PartPose.offset(0.0F, -2.0F, -4.0F).scaled(1.0F, 16.0F / 12.0F, 6.0F / 4.0F));

        // Legs: pivots as QuadrupedModel (legs 12 long). Hind legs = the skin's legs, front legs = its arms.
        leg(root, "right_hind_leg", -3.0F, 7.0F, 0, 16, 0, 32, 4);
        leg(root, "left_hind_leg", 3.0F, 7.0F, 16, 48, 0, 48, 4);
        int arm = slim ? 3 : 4;
        leg(root, "right_front_leg", -3.0F, -5.0F, 40, 16, 40, 32, arm);
        leg(root, "left_front_leg", 3.0F, -5.0F, 32, 48, 48, 48, arm);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void leg(PartDefinition root, String name, float x, float z, int u, int v, int outerU, int outerV, int width) {
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, 12.0F, z));
        float w = width;
        leg.addOrReplaceChild("skin", CubeListBuilder.create()
                        .texOffs(u, v).addBox(-w / 2.0F, 0.0F, -2.0F, w, 12.0F, 4.0F, OVER)
                        .texOffs(outerU, outerV).addBox(-w / 2.0F, 0.0F, -2.0F, w, 12.0F, 4.0F, OUTER),
                PartPose.ZERO.scaled(4.0F / w, 1.0F, 1.0F));
    }
}
