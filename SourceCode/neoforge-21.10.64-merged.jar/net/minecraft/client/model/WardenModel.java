package net.minecraft.client.model;

import java.util.Set;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.WardenAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WardenModel extends EntityModel<WardenRenderState> {
    private static final float DEFAULT_ARM_X_Y = 13.0F;
    private static final float DEFAULT_ARM_Z = 1.0F;
    protected final ModelPart bone;
    protected final ModelPart body;
    protected final ModelPart head;
    protected final ModelPart rightTendril;
    protected final ModelPart leftTendril;
    protected final ModelPart leftLeg;
    protected final ModelPart leftArm;
    protected final ModelPart leftRibcage;
    protected final ModelPart rightArm;
    protected final ModelPart rightLeg;
    protected final ModelPart rightRibcage;
    private final KeyframeAnimation attackAnimation;
    private final KeyframeAnimation sonicBoomAnimation;
    private final KeyframeAnimation diggingAnimation;
    private final KeyframeAnimation emergeAnimation;
    private final KeyframeAnimation roarAnimation;
    private final KeyframeAnimation sniffAnimation;

    public WardenModel(ModelPart root) {
        super(root, RenderType::entityCutoutNoCull);
        this.bone = root.getChild("bone");
        this.body = this.bone.getChild("body");
        this.head = this.body.getChild("head");
        this.rightLeg = this.bone.getChild("right_leg");
        this.leftLeg = this.bone.getChild("left_leg");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightTendril = this.head.getChild("right_tendril");
        this.leftTendril = this.head.getChild("left_tendril");
        this.rightRibcage = this.body.getChild("right_ribcage");
        this.leftRibcage = this.body.getChild("left_ribcage");
        this.attackAnimation = WardenAnimation.WARDEN_ATTACK.bake(root);
        this.sonicBoomAnimation = WardenAnimation.WARDEN_SONIC_BOOM.bake(root);
        this.diggingAnimation = WardenAnimation.WARDEN_DIG.bake(root);
        this.emergeAnimation = WardenAnimation.WARDEN_EMERGE.bake(root);
        this.roarAnimation = WardenAnimation.WARDEN_ROAR.bake(root);
        this.sniffAnimation = WardenAnimation.WARDEN_SNIFF.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
            "body", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -13.0F, -4.0F, 18.0F, 21.0F, 11.0F), PartPose.offset(0.0F, -21.0F, 0.0F)
        );
        partdefinition2.addOrReplaceChild(
            "right_ribcage", CubeListBuilder.create().texOffs(90, 11).addBox(-2.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F), PartPose.offset(-7.0F, -2.0F, -4.0F)
        );
        partdefinition2.addOrReplaceChild(
            "left_ribcage",
            CubeListBuilder.create().texOffs(90, 11).mirror().addBox(-7.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F).mirror(false),
            PartPose.offset(7.0F, -2.0F, -4.0F)
        );
        PartDefinition partdefinition3 = partdefinition2.addOrReplaceChild(
            "head", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -16.0F, -5.0F, 16.0F, 16.0F, 10.0F), PartPose.offset(0.0F, -13.0F, 0.0F)
        );
        partdefinition3.addOrReplaceChild(
            "right_tendril", CubeListBuilder.create().texOffs(52, 32).addBox(-16.0F, -13.0F, 0.0F, 16.0F, 16.0F, 0.0F), PartPose.offset(-8.0F, -12.0F, 0.0F)
        );
        partdefinition3.addOrReplaceChild(
            "left_tendril", CubeListBuilder.create().texOffs(58, 0).addBox(0.0F, -13.0F, 0.0F, 16.0F, 16.0F, 0.0F), PartPose.offset(8.0F, -12.0F, 0.0F)
        );
        partdefinition2.addOrReplaceChild(
            "right_arm", CubeListBuilder.create().texOffs(44, 50).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F), PartPose.offset(-13.0F, -13.0F, 1.0F)
        );
        partdefinition2.addOrReplaceChild(
            "left_arm", CubeListBuilder.create().texOffs(0, 58).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F), PartPose.offset(13.0F, -13.0F, 1.0F)
        );
        partdefinition1.addOrReplaceChild(
            "right_leg", CubeListBuilder.create().texOffs(76, 48).addBox(-3.1F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F), PartPose.offset(-5.9F, -13.0F, 0.0F)
        );
        partdefinition1.addOrReplaceChild(
            "left_leg", CubeListBuilder.create().texOffs(76, 76).addBox(-2.9F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F), PartPose.offset(5.9F, -13.0F, 0.0F)
        );
        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    public static LayerDefinition createTendrilsLayer() {
        return createBodyLayer().apply(p_435990_ -> {
            p_435990_.getRoot().retainExactParts(Set.of("left_tendril", "right_tendril"));
            return p_435990_;
        });
    }

    public static LayerDefinition createHeartLayer() {
        return createBodyLayer().apply(p_434144_ -> {
            p_434144_.getRoot().retainExactParts(Set.of("body"));
            return p_434144_;
        });
    }

    public static LayerDefinition createBioluminescentLayer() {
        return createBodyLayer().apply(p_433229_ -> {
            p_433229_.getRoot().retainExactParts(Set.of("head", "left_arm", "right_arm", "left_leg", "right_leg"));
            return p_433229_;
        });
    }

    public static LayerDefinition createPulsatingSpotsLayer() {
        return createBodyLayer().apply(p_434628_ -> {
            p_434628_.getRoot().retainExactParts(Set.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg"));
            return p_434628_;
        });
    }

    public void setupAnim(WardenRenderState renderState) {
        super.setupAnim(renderState);
        this.animateHeadLookTarget(renderState.yRot, renderState.xRot);
        this.animateWalk(renderState.walkAnimationPos, renderState.walkAnimationSpeed);
        this.animateIdlePose(renderState.ageInTicks);
        this.animateTendrils(renderState, renderState.ageInTicks);
        this.attackAnimation.apply(renderState.attackAnimationState, renderState.ageInTicks);
        this.sonicBoomAnimation.apply(renderState.sonicBoomAnimationState, renderState.ageInTicks);
        this.diggingAnimation.apply(renderState.diggingAnimationState, renderState.ageInTicks);
        this.emergeAnimation.apply(renderState.emergeAnimationState, renderState.ageInTicks);
        this.roarAnimation.apply(renderState.roarAnimationState, renderState.ageInTicks);
        this.sniffAnimation.apply(renderState.sniffAnimationState, renderState.ageInTicks);
    }

    private void animateHeadLookTarget(float yaw, float pitch) {
        this.head.xRot = pitch * (float) (Math.PI / 180.0);
        this.head.yRot = yaw * (float) (Math.PI / 180.0);
    }

    private void animateIdlePose(float ageInTicks) {
        float f = ageInTicks * 0.1F;
        float f1 = Mth.cos(f);
        float f2 = Mth.sin(f);
        this.head.zRot += 0.06F * f1;
        this.head.xRot += 0.06F * f2;
        this.body.zRot += 0.025F * f2;
        this.body.xRot += 0.025F * f1;
    }

    private void animateWalk(float limbSwing, float limbSwingAmount) {
        float f = Math.min(0.5F, 3.0F * limbSwingAmount);
        float f1 = limbSwing * 0.8662F;
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Math.min(0.35F, f);
        this.head.zRot += 0.3F * f3 * f;
        this.head.xRot = this.head.xRot + 1.2F * Mth.cos(f1 + (float) (Math.PI / 2)) * f4;
        this.body.zRot = 0.1F * f3 * f;
        this.body.xRot = 1.0F * f2 * f4;
        this.leftLeg.xRot = 1.0F * f2 * f;
        this.rightLeg.xRot = 1.0F * Mth.cos(f1 + (float) Math.PI) * f;
        this.leftArm.xRot = -(0.8F * f2 * f);
        this.leftArm.zRot = 0.0F;
        this.rightArm.xRot = -(0.8F * f3 * f);
        this.rightArm.zRot = 0.0F;
        this.resetArmPoses();
    }

    private void resetArmPoses() {
        this.leftArm.yRot = 0.0F;
        this.leftArm.z = 1.0F;
        this.leftArm.x = 13.0F;
        this.leftArm.y = -13.0F;
        this.rightArm.yRot = 0.0F;
        this.rightArm.z = 1.0F;
        this.rightArm.x = -13.0F;
        this.rightArm.y = -13.0F;
    }

    private void animateTendrils(WardenRenderState renderState, float ageInTicks) {
        float f = renderState.tendrilAnimation * (float)(Math.cos(ageInTicks * 2.25) * Math.PI * 0.1F);
        this.leftTendril.xRot = f;
        this.rightTendril.xRot = -f;
    }
}
