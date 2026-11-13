package com.jax3.barelyfunctionalcontraptions.sheep.client;

import com.jax3.barelyfunctionalcontraptions.sheep.ResourcefulSheep;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance;
import com.jax3.barelyfunctionalcontraptions.sheep.resource.SheepAppearance.Feature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The extra body parts an infused sheep can grow ({@link SheepAppearance.Feature}): dragon wings, spider legs, ghast
 * tentacles, sculk tendrils and glowing eyes. The Wither's extra heads are {@link SheepSkullsModel}.
 *
 * <p>It uses the vanilla sheep model's coordinates, so the parts sit on the vanilla body: the body spans x -4..4,
 * y 6..12 (the fleece reaches up to about y 4.25) and z -8..8, the head pivots at (0, 6, -8) and faces -z, and the
 * ground is y 24. Parts are shown or hidden in {@link #setupAnim} from the features stored in the render state, and
 * the head parts follow the sheep's head, grazing included.</p>
 *
 * <p>The texture ({@code textures/entity/sheep/sheep_extras.png}, 64x64) is drawn light so that a resource's
 * {@code feature_color} can tint it. The eyes are a separate copy of this model drawn with the emissive
 * {@code eyes} render type ({@link #eyes}).</p>
 */
public class SheepExtrasModel extends EntityModel<SheepRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourcefulSheep.id("sheep_extras"), "main");

    /** Spider legs: z positions along the body, and how far each one fans forwards or backwards. */
    private static final float[][] SPIDER_LEGS = {{-3.0F, 0.45F}, {2.0F, -0.45F}};
    /** Tentacles under the belly: x, z and length. */
    private static final float[][] TENTACLES = {
            {-4.5F, -1.0F, 10}, {-4.5F, 2.5F, 9}, {4.5F, -1.0F, 9}, {4.5F, 2.5F, 10},
            {-1.5F, 0.5F, 11}, {1.5F, 0.5F, 10}, {0.0F, -3.0F, 9}, {0.0F, 3.5F, 9}};

    private final boolean eyesOnly;
    private final ModelPart head;
    private final ModelPart eyes;
    private final ModelPart leftTendril;
    private final ModelPart rightTendril;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final List<ModelPart> leftLegs = new ArrayList<>();
    private final List<ModelPart> rightLegs = new ArrayList<>();
    private final List<ModelPart> tentacles = new ArrayList<>();

    private SheepExtrasModel(ModelPart root, boolean eyesOnly) {
        super(root, eyesOnly ? RenderTypes::eyes : RenderTypes::entityCutout);
        this.eyesOnly = eyesOnly;
        this.head = root.getChild("head");
        this.eyes = head.getChild("eyes");
        this.leftTendril = head.getChild("left_tendril");
        this.rightTendril = head.getChild("right_tendril");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        for (int i = 0; i < SPIDER_LEGS.length; i++) {
            leftLegs.add(root.getChild("left_spider_leg_" + i));
            rightLegs.add(root.getChild("right_spider_leg_" + i));
        }
        for (int i = 0; i < TENTACLES.length; i++) tentacles.add(root.getChild("tentacle_" + i));
    }

    /** The body parts, drawn in the resource's {@code feature_color}. */
    public static SheepExtrasModel parts(ModelPart root) {
        return new SheepExtrasModel(root, false);
    }

    /** Only the glowing eyes, for the emissive pass. */
    public static SheepExtrasModel eyes(ModelPart root) {
        return new SheepExtrasModel(root, true);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head-mounted parts follow the vanilla head (pivot 0, 6, -8; its face is the plane z = -6 in head space).
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 6.0F, -8.0F));
        head.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(40, 8).addBox(-3.0F, -4.0F, -6.02F, 6.0F, 6.0F, 0.0F), PartPose.ZERO);
        // Sculk tendrils, like a warden's: flat fronds standing up from the top of the head, leaning outwards.
        head.addOrReplaceChild("left_tendril", CubeListBuilder.create().texOffs(40, 0).addBox(0.0F, -7.0F, 0.0F, 6.0F, 7.0F, 0.0F),
                PartPose.offsetAndRotation(2.0F, -4.0F, -3.0F, 0.0F, 0.0F, 0.35F));
        head.addOrReplaceChild("right_tendril", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-6.0F, -7.0F, 0.0F, 6.0F, 7.0F, 0.0F),
                PartPose.offsetAndRotation(-2.0F, -4.0F, -3.0F, 0.0F, 0.0F, -0.35F));

        // Dragon wings: a bone along the leading edge with a membrane hanging behind it, hinged on the top of the back.
        PartDefinition leftWing = root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, -1.0F, -1.0F, 16.0F, 1.0F, 1.0F),
                PartPose.offset(3.0F, 4.0F, -4.0F));
        leftWing.addOrReplaceChild("left_membrane", CubeListBuilder.create().texOffs(0, 14).addBox(0.0F, 0.0F, 0.0F, 16.0F, 0.0F, 12.0F), PartPose.offset(0.0F, -0.5F, 0.0F));
        PartDefinition rightWing = root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-16.0F, -1.0F, -1.0F, 16.0F, 1.0F, 1.0F),
                PartPose.offset(-3.0F, 4.0F, -4.0F));
        rightWing.addOrReplaceChild("right_membrane", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-16.0F, 0.0F, 0.0F, 16.0F, 0.0F, 12.0F), PartPose.offset(0.0F, -0.5F, 0.0F));

        // Spider legs: long and thin, out from the sides of the body and down to the ground.
        for (int i = 0; i < SPIDER_LEGS.length; i++) {
            float z = SPIDER_LEGS[i][0];
            root.addOrReplaceChild("left_spider_leg_" + i, CubeListBuilder.create().texOffs(0, 26).addBox(0.0F, -1.0F, -1.0F, 14.0F, 2.0F, 2.0F),
                    PartPose.offset(4.0F, 9.0F, z));
            root.addOrReplaceChild("right_spider_leg_" + i, CubeListBuilder.create().texOffs(0, 26).mirror().addBox(-14.0F, -1.0F, -1.0F, 14.0F, 2.0F, 2.0F),
                    PartPose.offset(-4.0F, 9.0F, z));
        }

        // Ghast tentacles hanging from the belly.
        for (int i = 0; i < TENTACLES.length; i++) {
            float[] t = TENTACLES[i];
            root.addOrReplaceChild("tentacle_" + i, CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, 0.0F, -1.0F, 2.0F, t[2], 2.0F),
                    PartPose.offset(t[0], 11.5F, t[1]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SheepRenderState state) {
        super.setupAnim(state);
        InfusedWoolLayer.Colors colors = state.getRenderData(InfusedWoolLayer.INFUSED_WOOL);
        SheepAppearance appearance = colors == null ? SheepAppearance.NONE : colors.appearance();
        float t = state.ageInTicks;

        // The head, exactly as SheepModel poses it (including lowering it to graze).
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.y += state.headEatPositionScale * 9.0F * state.ageScale;
        head.xRot = state.headEatAngleScale;

        eyes.visible = eyesOnly && appearance.has(Feature.GLOWING_EYES);
        boolean parts = !eyesOnly;
        boolean sculk = parts && appearance.has(Feature.SCULK);
        leftTendril.visible = sculk;
        rightTendril.visible = sculk;
        float tendrilSway = Mth.sin(t * 0.12F) * 0.12F;
        leftTendril.zRot += tendrilSway;
        rightTendril.zRot -= tendrilSway;

        boolean wings = parts && appearance.has(Feature.WINGS);
        leftWing.visible = wings;
        rightWing.visible = wings;
        // Slow flapping that speeds up while walking; folded slightly back along the body.
        float flap = Mth.sin(t * (0.18F + state.walkAnimationSpeed * 0.25F)) * 0.35F;
        leftWing.zRot = -0.7F + flap;
        rightWing.zRot = 0.7F - flap;
        leftWing.yRot = -0.3F;
        rightWing.yRot = 0.3F;

        boolean legs = parts && appearance.has(Feature.EXTRA_LEGS);
        for (int i = 0; i < SPIDER_LEGS.length; i++) {
            float swing = Mth.cos(state.walkAnimationPos * 0.6662F * 2.0F + i * Mth.PI) * 0.4F * state.walkAnimationSpeed;
            ModelPart left = leftLegs.get(i);
            ModelPart right = rightLegs.get(i);
            left.visible = legs;
            right.visible = legs;
            left.zRot = 0.75F;
            right.zRot = -0.75F;
            left.yRot = SPIDER_LEGS[i][1] + swing;
            right.yRot = -SPIDER_LEGS[i][1] - swing;
        }

        boolean tentacles = parts && appearance.has(Feature.TENTACLES);
        for (int i = 0; i < this.tentacles.size(); i++) {
            ModelPart tentacle = this.tentacles.get(i);
            tentacle.visible = tentacles;
            // The ghast's drifting tentacles.
            tentacle.xRot = 0.15F * Mth.sin(t * 0.3F + i) + 0.1F;
            tentacle.zRot = 0.1F * Mth.sin(t * 0.21F + i * 1.7F);
        }
    }
}
