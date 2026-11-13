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
 * A mob's skull worn as the sheep's head (a resource's {@code skull_texture}, e.g. the Skeleton sheep's).
 *
 * <p>The skull is the vanilla mob-head cube (8x8x8 at texture offset 0,0 of a 64x32 texture, as the skull blocks
 * use) squashed to the sheep's 6x6x8 head and a hair larger, so it covers it. Only the head is drawn; the other parts
 * are empty but named like {@link SheepModel}'s, so the skull turns and grazes with the sheep.</p>
 */
public class SheepSkullFaceModel extends SheepModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourcefulSheep.id("sheep_skull_face"), "main");

    public SheepSkullFaceModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Head pivot as SheepModel (0, 6, -8); the sheep's head box is x -3..3, y -4..2, z -6..2 around it.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 6.0F, -8.0F));
        head.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.08F)),
                PartPose.offset(0.0F, -1.0F, -2.0F).scaled(0.75F, 0.75F, 1.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        for (String leg : new String[]{"right_hind_leg", "left_hind_leg", "right_front_leg", "left_front_leg"}) {
            root.addOrReplaceChild(leg, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }
}
