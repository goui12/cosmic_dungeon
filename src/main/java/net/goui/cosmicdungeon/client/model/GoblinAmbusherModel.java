package net.goui.cosmicdungeon.client.model;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.anim.GoblinAmbusherAnimation;
import net.goui.cosmicdungeon.client.renderstate.GoblinAmbusherRenderState;
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
import net.minecraft.util.Mth;

public class GoblinAmbusherModel extends EntityModel<GoblinAmbusherRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "goblin_ambusher"), "main");

    private final KeyframeAnimation walkAnim;
    private final KeyframeAnimation shootAnim;

    private final ModelPart head;

    public GoblinAmbusherModel(ModelPart root) {
        super(root);
        this.walkAnim  = GoblinAmbusherAnimation.WalkAnimation.bake(root);
        this.shootAnim = GoblinAmbusherAnimation.ShootAnimation.bake(root);

        ModelPart main = root.getChild("main");
        ModelPart body = main.getChild("body");
        this.head = body.getChild("head");
    }

    /** Y-flip fixed geometry (no 180° X flip needed). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition body = main.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(36, 0).addBox(-2.0F, -28.0F, 0.0F, 12.0F, 15.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-4.0F, 11.0F, -3.0F));

        PartDefinition clothes = body.addOrReplaceChild("clothes",
                CubeListBuilder.create().texOffs(114, 0).addBox(-4.0F, 0.0F, -1.0F, 8.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, -15.0F, 0.0F));

        PartDefinition clothes_lvl2 = clothes.addOrReplaceChild("clothes_lvl2",
                CubeListBuilder.create().texOffs(30, 25).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 6.0F, -1.0F));

        PartDefinition clothes_lvl3 = clothes_lvl2.addOrReplaceChild("clothes_lvl3",
                CubeListBuilder.create().texOffs(0, 25).addBox(-3.0F, 0.0F, 0.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition clothes_lvl4 = clothes_lvl3.addOrReplaceChild("clothes_lvl4",
                CubeListBuilder.create().texOffs(28, 27).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition clothes_lvl5 = clothes_lvl4.addOrReplaceChild("clothes_lvl5",
                CubeListBuilder.create().texOffs(11, 28).addBox(-2.0F, 0.0F, 0.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition clothes_lvl6 = clothes_lvl5.addOrReplaceChild("clothes_lvl6",
                CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition clothes2 = body.addOrReplaceChild("clothes2",
                CubeListBuilder.create().texOffs(114, 7).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, -15.0F, 3.0F));

        PartDefinition clothes2_lvl2 = clothes2.addOrReplaceChild("clothes2_lvl2",
                CubeListBuilder.create().texOffs(52, 20).addBox(-3.0F, 0.0F, -1.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 6.0F, 1.0F));

        PartDefinition clothes2_lvl3 = clothes2_lvl2.addOrReplaceChild("clothes2_lvl3",
                CubeListBuilder.create().texOffs(54, 22).addBox(-3.0F, 0.0F, -1.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition clothes2_lvl4 = clothes2_lvl3.addOrReplaceChild("clothes2_lvl4",
                CubeListBuilder.create().texOffs(20, 25).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition clothes2_lvl5 = clothes2_lvl4.addOrReplaceChild("clothes2_lvl5",
                CubeListBuilder.create().texOffs(4, 27).addBox(-2.0F, 0.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition clothes2_lvl6 = clothes2_lvl5.addOrReplaceChild("clothes2_lvl6",
                CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        PartDefinition l_arm = body.addOrReplaceChild("l_arm",
                CubeListBuilder.create().texOffs(66, 10).addBox(0.0F, -2.0F, -1.0F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(10.0F, -26.0F, 1.0F));

        PartDefinition l_hand = l_arm.addOrReplaceChild("l_hand",
                CubeListBuilder.create().texOffs(78, 10).addBox(-1.0F, 0.0F, -3.0F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, 6.0F, 2.0F));

        l_hand.addOrReplaceChild("blowdart",
                CubeListBuilder.create().texOffs(13, 30).addBox(0.0F, 7.0F, -11.0F, 1.0F, 1.0F, 18.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition r_arm = body.addOrReplaceChild("r_arm",
                CubeListBuilder.create().texOffs(90, 10).addBox(-2.0F, -2.0F, 0.0F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, -26.0F, 0.0F));

        r_arm.addOrReplaceChild("r_hand",
                CubeListBuilder.create().texOffs(102, 10).addBox(-2.0F, 0.0F, -3.0F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 6.0F, 3.0F));

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 8.0F)
                        .texOffs(0, 0).addBox(5.0F, -8.0F, -2.0F, 1.0F, 5.0F, 2.0F)
                        .texOffs(114, 14).addBox(-3.0F, -2.0F, -7.0F, 6.0F, 2.0F, 1.0F)
                        .texOffs(114, 17).addBox(-1.0F, -5.0F, -8.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(4.0F, -28.0F, 2.0F));

        PartDefinition r_ear = head.addOrReplaceChild("r_ear",
                CubeListBuilder.create().texOffs(48, 22).addBox(-13.0F, -10.0F, -2.0F, 2.0F, 1.0F, 2.0F)
                        .texOffs(4, 18).addBox(-13.0F, -9.0F, -1.0F, 2.0F, 1.0F, 1.0F)
                        .texOffs(12, 20).addBox(-12.0F, -8.0F, -1.0F, 6.0F, 1.0F, 1.0F)
                        .texOffs(0, 20).addBox(-11.0F, -7.0F, -1.0F, 5.0F, 1.0F, 1.0F)
                        .texOffs(20, 18).addBox(-10.0F, -6.0F, -1.0F, 4.0F, 1.0F, 1.0F)
                        .texOffs(42, 18).addBox(-9.0F, -5.0F, -1.0F, 3.0F, 1.0F, 1.0F)
                        .texOffs(32, 20).addBox(-8.0F, -4.0F, -1.0F, 2.0F, 1.0F, 1.0F)
                        .texOffs(34, 22).addBox(-11.0F, -9.0F, -2.0F, 5.0F, 1.0F, 2.0F)
                        .texOffs(29, 0).addBox(-6.0F, -8.0F, -2.0F, 1.0F, 5.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        r_ear.addOrReplaceChild("r_earring1",
                CubeListBuilder.create().texOffs(8, 22).addBox(1.0F, 2.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(0, 27).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(-9.0F, -6.0F, -1.0F));

        PartDefinition l_ear2 = head.addOrReplaceChild("l_ear2",
                CubeListBuilder.create().texOffs(26, 22).addBox(-7.0F, -10.0F, -2.0F, 2.0F, 1.0F, 2.0F)
                        .texOffs(26, 20).addBox(-7.0F, -9.0F, -1.0F, 2.0F, 1.0F, 1.0F)
                        .texOffs(50, 18).addBox(-12.0F, -8.0F, -1.0F, 6.0F, 1.0F, 1.0F)
                        .texOffs(30, 18).addBox(-12.0F, -7.0F, -1.0F, 5.0F, 1.0F, 1.0F)
                        .texOffs(10, 18).addBox(-12.0F, -6.0F, -1.0F, 4.0F, 1.0F, 1.0F)
                        .texOffs(38, 20).addBox(-12.0F, -5.0F, -1.0F, 3.0F, 1.0F, 1.0F)
                        .texOffs(46, 20).addBox(-12.0F, -4.0F, -1.0F, 2.0F, 1.0F, 1.0F)
                        .texOffs(12, 22).addBox(-12.0F, -9.0F, -2.0F, 5.0F, 1.0F, 2.0F),
                PartPose.offset(18.0F, 0.0F, 0.0F));

        l_ear2.addOrReplaceChild("l_earring2",
                CubeListBuilder.create().texOffs(0, 22).addBox(-3.0F, 2.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(4, 22).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(-8.0F, -6.0F, -1.0F));

        PartDefinition l_leg = main.addOrReplaceChild("l_leg",
                CubeListBuilder.create().texOffs(90, 0).addBox(0.0F, 0.0F, -3.0F, 3.0F, 7.0F, 3.0F),
                PartPose.offset(3.0F, -2.0F, 0.0F));

        PartDefinition l_foot = l_leg.addOrReplaceChild("l_foot",
                CubeListBuilder.create().texOffs(66, 0).addBox(-1.0F, 0.0F, 0.0F, 3.0F, 7.0F, 3.0F),
                PartPose.offset(1.0F, 7.0F, -3.0F));

        l_foot.addOrReplaceChild("l_toe",
                CubeListBuilder.create().texOffs(20, 27).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(1.0F, 6.0F, 0.0F));

        PartDefinition r_leg = main.addOrReplaceChild("r_leg",
                CubeListBuilder.create().texOffs(102, 0).addBox(-3.0F, 0.0F, -3.0F, 3.0F, 7.0F, 3.0F),
                PartPose.offset(-3.0F, -2.0F, 0.0F));

        PartDefinition r_foot = r_leg.addOrReplaceChild("r_foot",
                CubeListBuilder.create().texOffs(78, 0).addBox(-2.0F, 0.0F, 0.0F, 3.0F, 7.0F, 3.0F),
                PartPose.offset(-1.0F, 7.0F, -3.0F));

        r_foot.addOrReplaceChild("r_toe",
                CubeListBuilder.create().texOffs(12, 25).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(GoblinAmbusherRenderState state) {
        // Reset to bind pose
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // Head look (skip during attack)
        if (!state.attackAnimation.isStarted()) {
            float headYaw = Mth.clamp(state.yRot, -45f, 45f);
            float headPitch = Mth.clamp(state.xRot, -25f, 35f);
            this.head.yRot = headYaw * ((float)Math.PI / 180f);
            this.head.xRot = headPitch * ((float)Math.PI / 180f);
        }

        // Walk loop only when not attacking
        if (!state.attackAnimation.isStarted()) {
            this.walkAnim.applyWalk(
                    state.walkAnimationPos,
                    Math.min(state.walkAnimationSpeed, 1.0F),
                    1.4F,
                    1.6F
            );
        }

        // One-shot shoot anim
        this.shootAnim.apply(state.attackAnimation, state.ageInTicks, 1.0F);
    }
}
