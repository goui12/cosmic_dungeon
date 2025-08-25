package net.goui.cosmicdungeon.client.model;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.anim.StoneWardenAnimations;
import net.goui.cosmicdungeon.client.renderstate.StoneWardenRenderState;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class StoneWardenModel extends EntityModel<StoneWardenRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "stone_warden"), "main");

    // Keep only what we actually use like the tutorial model (root + a couple important parts)
    private final ModelPart main;
    private final ModelPart head;

    // Animations baked from tutorial-style constants
    private final KeyframeAnimation walkingAnimation;
    private final KeyframeAnimation attackAnimation;

    public StoneWardenModel(ModelPart root) {
        super(root);
        this.main = root.getChild("main");
        this.head = this.main.getChild("frame").getChild("head");

        // Bake against the root, like the example
        this.walkingAnimation = StoneWardenAnimations.WALKING.bake(root);
        this.attackAnimation  = StoneWardenAnimations.ATTACK_1.bake(root);
    }

    /** Blockbench geometry exactly as you provided (unchanged), under 1.21.8 builders. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition main = partdefinition.addOrReplaceChild("main",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -41.0F, -3.0F));

        PartDefinition r_leg = main.addOrReplaceChild("r_leg", CubeListBuilder.create(), PartPose.offset(-10.0F, 24.0F, 0.0F));

        PartDefinition r_thigh = r_leg.addOrReplaceChild("r_thigh",
                CubeListBuilder.create().texOffs(0, 139).addBox(-10.0F, -4.0F, -6.0F, 10.0F, 18.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(5.0F, 0.0F, 3.0F));

        PartDefinition r_knee = r_leg.addOrReplaceChild("r_knee",
                CubeListBuilder.create().texOffs(228, 147).addBox(-4.0F, -2.0F, 0.0F, 8.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, -2.0F));

        PartDefinition r_shin = r_leg.addOrReplaceChild("r_shin",
                CubeListBuilder.create().texOffs(173, 117).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 25.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(184, 157).addBox(-5.0F, -2.0F, -3.0F, 10.0F, 20.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, -2.0F));

        PartDefinition r_foot = r_shin.addOrReplaceChild("r_foot",
                CubeListBuilder.create().texOffs(0, 166).addBox(-5.0F, 0.0F, -10.0F, 10.0F, 6.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 19.0F, -1.0F));

        PartDefinition l_leg = main.addOrReplaceChild("l_leg", CubeListBuilder.create(), PartPose.offset(10.0F, 24.0F, 0.0F));

        PartDefinition l_thigh = l_leg.addOrReplaceChild("l_thigh",
                CubeListBuilder.create().texOffs(0, 139).addBox(0.0F, -4.0F, 3.0F, 10.0F, 18.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 0.0F, -6.0F));

        PartDefinition l_knee = l_leg.addOrReplaceChild("l_knee",
                CubeListBuilder.create().texOffs(228, 147).addBox(-3.0F, -2.0F, 0.0F, 8.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 16.0F, -2.0F));

        PartDefinition l_shin = l_leg.addOrReplaceChild("l_shin",
                CubeListBuilder.create().texOffs(222, 224).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 25.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(230, 201).addBox(-5.0F, -2.0F, -3.0F, 10.0F, 20.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, -2.0F));

        PartDefinition l_foot = l_shin.addOrReplaceChild("l_foot",
                CubeListBuilder.create().texOffs(0, 166).addBox(-5.0F, 0.0F, -10.0F, 10.0F, 6.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 19.0F, -1.0F));

        PartDefinition frame = main.addOrReplaceChild("frame", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        PartDefinition r_arm = frame.addOrReplaceChild("r_arm", CubeListBuilder.create(), PartPose.offset(-20.0F, -28.0F, 0.0F));

        PartDefinition r_upper_arm = r_arm.addOrReplaceChild("r_upper_arm",
                CubeListBuilder.create().texOffs(178, 192).addBox(-27.0F, -81.0F, 0.0F, 10.0F, 17.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(150, 219).addBox(-25.0F, -64.0F, 2.0F, 6.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(20.0F, 74.0F, -6.0F));

        PartDefinition r_lower_arm = r_arm.addOrReplaceChild("r_lower_arm",
                CubeListBuilder.create().texOffs(212, 118).addBox(-5.0F, 0.0F, -6.0F, 10.0F, 16.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 182).addBox(-3.0F, 16.0F, -4.0F, 6.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 20.0F, 0.0F));

        PartDefinition r_hand = r_lower_arm.addOrReplaceChild("r_hand", CubeListBuilder.create(), PartPose.offset(-3.0F, 17.0F, 0.0F));

        PartDefinition r_hand_core = r_hand.addOrReplaceChild("r_hand_core",
                CubeListBuilder.create().texOffs(228, 183).addBox(2.0F, -1.0F, 1.0F, 4.0F, 8.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 1.0F, -6.0F));

        PartDefinition r_finger_thumb = r_hand.addOrReplaceChild("r_finger_thumb",
                CubeListBuilder.create().texOffs(166, 207).addBox(-1.0F, 0.0F, -1.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, 0.0F, -4.0F));

        PartDefinition r_finger_pointer = r_hand.addOrReplaceChild("r_finger_pointer",
                CubeListBuilder.create().texOffs(210, 196).addBox(-1.0F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 8.0F, -3.0F));

        PartDefinition r_finger_middle = r_hand.addOrReplaceChild("r_finger_middle",
                CubeListBuilder.create().texOffs(210, 196).addBox(-1.0F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 8.0F, -1.0F));

        PartDefinition r_finger_ring = r_hand.addOrReplaceChild("r_finger_ring",
                CubeListBuilder.create().texOffs(210, 196).addBox(-1.0F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 8.0F, 1.0F));

        PartDefinition r_finger_pinky = r_hand.addOrReplaceChild("r_finger_pinky",
                CubeListBuilder.create().texOffs(210, 196).addBox(-1.0F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 8.0F, 3.0F));

        PartDefinition l_arm = frame.addOrReplaceChild("l_arm", CubeListBuilder.create(), PartPose.offset(20.0F, -28.0F, 0.0F));

        PartDefinition l_upper_arm = l_arm.addOrReplaceChild("l_upper_arm",
                CubeListBuilder.create().texOffs(178, 221).addBox(17.0F, -81.0F, 0.0F, 10.0F, 17.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(150, 219).addBox(19.0F, -64.0F, 2.0F, 6.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-20.0F, 74.0F, -6.0F));

        PartDefinition l_lower_arm = l_arm.addOrReplaceChild("l_lower_arm",
                CubeListBuilder.create().texOffs(212, 155).addBox(-5.0F, 0.0F, -6.0F, 10.0F, 16.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(192, 182).addBox(-3.0F, 16.0F, -4.0F, 6.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 20.0F, 0.0F));

        PartDefinition l_hand = l_lower_arm.addOrReplaceChild("l_hand", CubeListBuilder.create(), PartPose.offset(3.0F, 17.0F, 0.0F));

        PartDefinition l_hand_core = l_hand.addOrReplaceChild("l_hand_core",
                CubeListBuilder.create().texOffs(228, 183).addBox(22.0F, -37.0F, 1.0F, 4.0F, 8.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-25.0F, 37.0F, -6.0F));

        PartDefinition l_finger_thumb = l_hand.addOrReplaceChild("l_finger_thumb",
                CubeListBuilder.create().texOffs(166, 207).addBox(-2.0F, 0.0F, -1.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-4.0F, 0.0F, -4.0F));

        PartDefinition l_finger_pointer = l_hand.addOrReplaceChild("l_finger_pointer",
                CubeListBuilder.create().texOffs(210, 196).addBox(-2.0F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 8.0F, -3.0F));

        PartDefinition l_finger_middle = l_hand.addOrReplaceChild("l_finger_middle",
                CubeListBuilder.create().texOffs(210, 196).addBox(-2.0F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 8.0F, -1.0F));

        PartDefinition l_finger_ring = l_hand.addOrReplaceChild("l_finger_ring",
                CubeListBuilder.create().texOffs(210, 196).addBox(-2.0F, 0.0F, 0.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        PartDefinition l_finger_pinky = l_hand.addOrReplaceChild("l_finger_pinky",
                CubeListBuilder.create().texOffs(210, 196).addBox(-2.0F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 8.0F, 3.0F));

        PartDefinition head = frame.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(182, 0).addBox(-11.0F, -22.0F, -6.0F, 22.0F, 22.0F, 15.0F, new CubeDeformation(0.0F))
                        .texOffs(208, 38).addBox(-11.0F, -22.0F, -8.0F, 22.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(248, 65).addBox(-11.0F, -14.0F, -8.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(240, 65).addBox(9.0F, -14.0F, -8.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 64).addBox(3.0F, -9.0F, -8.0F, 6.0F, 9.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(222, 53).addBox(-9.0F, -9.0F, -8.0F, 6.0F, 9.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(240, 50).addBox(-2.0F, -11.0F, -10.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -34.0F, -3.0F));

        PartDefinition body = frame.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 182).addBox(-5.0F, 5.0F, -4.0F, 10.0F, 4.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(100, 220).addBox(-6.0F, 0.0F, -5.0F, 12.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 197).addBox(-14.0F, -6.0F, -5.0F, 28.0F, 6.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(101, 237).addBox(-15.0F, -12.0F, -6.0F, 30.0F, 6.0F, 13.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 218).addBox(-17.0F, -34.0F, -8.0F, 34.0F, 22.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(StoneWardenRenderState state) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        this.walkingAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2f, 2.5f);

        // was: state.attackAnimationState
        this.attackAnimation.apply(state.attackAnimation, state.ageInTicks, 1f);
    }
}
;