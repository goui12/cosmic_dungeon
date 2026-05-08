package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AxolotlModel extends EntityModel<AxolotlRenderState> {
    public static final float SWIMMING_LEG_XROT = 1.8849558F;
    public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
    private final ModelPart tail;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart topGills;
    private final ModelPart leftGills;
    private final ModelPart rightGills;

    public AxolotlModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.rightHindLeg = this.body.getChild("right_hind_leg");
        this.leftHindLeg = this.body.getChild("left_hind_leg");
        this.rightFrontLeg = this.body.getChild("right_front_leg");
        this.leftFrontLeg = this.body.getChild("left_front_leg");
        this.tail = this.body.getChild("tail");
        this.topGills = this.head.getChild("top_gills");
        this.leftGills = this.head.getChild("left_gills");
        this.rightGills = this.head.getChild("right_gills");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 11).addBox(-4.0F, -2.0F, -9.0F, 8.0F, 4.0F, 10.0F).texOffs(2, 17).addBox(0.0F, -3.0F, -8.0F, 0.0F, 5.0F, 9.0F),
            PartPose.offset(0.0F, 20.0F, 5.0F)
        );
        CubeDeformation cubedeformation = new CubeDeformation(0.001F);
        PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
            "head", CubeListBuilder.create().texOffs(0, 1).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 5.0F, 5.0F, cubedeformation), PartPose.offset(0.0F, 0.0F, -9.0F)
        );
        CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(3, 37).addBox(-4.0F, -3.0F, 0.0F, 8.0F, 3.0F, 0.0F, cubedeformation);
        CubeListBuilder cubelistbuilder1 = CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, -5.0F, 0.0F, 3.0F, 7.0F, 0.0F, cubedeformation);
        CubeListBuilder cubelistbuilder2 = CubeListBuilder.create().texOffs(11, 40).addBox(0.0F, -5.0F, 0.0F, 3.0F, 7.0F, 0.0F, cubedeformation);
        partdefinition2.addOrReplaceChild("top_gills", cubelistbuilder, PartPose.offset(0.0F, -3.0F, -1.0F));
        partdefinition2.addOrReplaceChild("left_gills", cubelistbuilder1, PartPose.offset(-4.0F, 0.0F, -1.0F));
        partdefinition2.addOrReplaceChild("right_gills", cubelistbuilder2, PartPose.offset(4.0F, 0.0F, -1.0F));
        CubeListBuilder cubelistbuilder3 = CubeListBuilder.create().texOffs(2, 13).addBox(-1.0F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, cubedeformation);
        CubeListBuilder cubelistbuilder4 = CubeListBuilder.create().texOffs(2, 13).addBox(-2.0F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F, cubedeformation);
        partdefinition1.addOrReplaceChild("right_hind_leg", cubelistbuilder4, PartPose.offset(-3.5F, 1.0F, -1.0F));
        partdefinition1.addOrReplaceChild("left_hind_leg", cubelistbuilder3, PartPose.offset(3.5F, 1.0F, -1.0F));
        partdefinition1.addOrReplaceChild("right_front_leg", cubelistbuilder4, PartPose.offset(-3.5F, 1.0F, -8.0F));
        partdefinition1.addOrReplaceChild("left_front_leg", cubelistbuilder3, PartPose.offset(3.5F, 1.0F, -8.0F));
        partdefinition1.addOrReplaceChild(
            "tail", CubeListBuilder.create().texOffs(2, 19).addBox(0.0F, -3.0F, 0.0F, 0.0F, 5.0F, 12.0F), PartPose.offset(0.0F, 0.0F, 1.0F)
        );
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setupAnim(AxolotlRenderState renderState) {
        super.setupAnim(renderState);
        float f = renderState.playingDeadFactor;
        float f1 = renderState.inWaterFactor;
        float f2 = renderState.onGroundFactor;
        float f3 = renderState.movingFactor;
        float f4 = 1.0F - f3;
        float f5 = 1.0F - Math.min(f2, f3);
        this.body.yRot = this.body.yRot + renderState.yRot * (float) (Math.PI / 180.0);
        this.setupSwimmingAnimation(renderState.ageInTicks, renderState.xRot, Math.min(f3, f1));
        this.setupWaterHoveringAnimation(renderState.ageInTicks, Math.min(f4, f1));
        this.setupGroundCrawlingAnimation(renderState.ageInTicks, Math.min(f3, f2));
        this.setupLayStillOnGroundAnimation(renderState.ageInTicks, Math.min(f4, f2));
        this.setupPlayDeadAnimation(f);
        this.applyMirrorLegRotations(f5);
    }

    private void setupLayStillOnGroundAnimation(float ageInTicks, float netHeadYaw) {
        if (!(netHeadYaw <= 1.0E-5F)) {
            float f = ageInTicks * 0.09F;
            float f1 = Mth.sin(f);
            float f2 = Mth.cos(f);
            float f3 = f1 * f1 - 2.0F * f1;
            float f4 = f2 * f2 - 3.0F * f1;
            this.head.xRot += -0.09F * f3 * netHeadYaw;
            this.head.zRot += -0.2F * netHeadYaw;
            this.tail.yRot += (-0.1F + 0.1F * f3) * netHeadYaw;
            float f5 = (0.6F + 0.05F * f4) * netHeadYaw;
            this.topGills.xRot += f5;
            this.leftGills.yRot -= f5;
            this.rightGills.yRot += f5;
            this.leftHindLeg.xRot += 1.1F * netHeadYaw;
            this.leftHindLeg.yRot += 1.0F * netHeadYaw;
            this.leftFrontLeg.xRot += 0.8F * netHeadYaw;
            this.leftFrontLeg.yRot += 2.3F * netHeadYaw;
            this.leftFrontLeg.zRot -= 0.5F * netHeadYaw;
        }
    }

    private void setupGroundCrawlingAnimation(float ageInTicks, float netHeadYaw) {
        if (!(netHeadYaw <= 1.0E-5F)) {
            float f = ageInTicks * 0.11F;
            float f1 = Mth.cos(f);
            float f2 = (f1 * f1 - 2.0F * f1) / 5.0F;
            float f3 = 0.7F * f1;
            float f4 = 0.09F * f1 * netHeadYaw;
            this.head.yRot += f4;
            this.tail.yRot += f4;
            float f5 = (0.6F - 0.08F * (f1 * f1 + 2.0F * Mth.sin(f))) * netHeadYaw;
            this.topGills.xRot += f5;
            this.leftGills.yRot -= f5;
            this.rightGills.yRot += f5;
            float f6 = 0.9424779F * netHeadYaw;
            float f7 = 1.0995574F * netHeadYaw;
            this.leftHindLeg.xRot += f6;
            this.leftHindLeg.yRot += (1.5F - f2) * netHeadYaw;
            this.leftHindLeg.zRot += -0.1F * netHeadYaw;
            this.leftFrontLeg.xRot += f7;
            this.leftFrontLeg.yRot += ((float) (Math.PI / 2) - f3) * netHeadYaw;
            this.rightHindLeg.xRot += f6;
            this.rightHindLeg.yRot += (-1.0F - f2) * netHeadYaw;
            this.rightFrontLeg.xRot += f7;
            this.rightFrontLeg.yRot += ((float) (-Math.PI / 2) - f3) * netHeadYaw;
        }
    }

    private void setupWaterHoveringAnimation(float ageInTicks, float swimmingFactor) {
        if (!(swimmingFactor <= 1.0E-5F)) {
            float f = ageInTicks * 0.075F;
            float f1 = Mth.cos(f);
            float f2 = Mth.sin(f) * 0.15F;
            float f3 = (-0.15F + 0.075F * f1) * swimmingFactor;
            this.body.xRot += f3;
            this.body.y -= f2 * swimmingFactor;
            this.head.xRot -= f3;
            this.topGills.xRot += 0.2F * f1 * swimmingFactor;
            float f4 = (-0.3F * f1 - 0.19F) * swimmingFactor;
            this.leftGills.yRot += f4;
            this.rightGills.yRot -= f4;
            this.leftHindLeg.xRot += ((float) (Math.PI * 3.0 / 4.0) - f1 * 0.11F) * swimmingFactor;
            this.leftHindLeg.yRot += 0.47123894F * swimmingFactor;
            this.leftHindLeg.zRot += 1.7278761F * swimmingFactor;
            this.leftFrontLeg.xRot += ((float) (Math.PI / 4) - f1 * 0.2F) * swimmingFactor;
            this.leftFrontLeg.yRot += 2.042035F * swimmingFactor;
            this.tail.yRot += 0.5F * f1 * swimmingFactor;
        }
    }

    private void setupSwimmingAnimation(float ageInTicks, float xRot, float movingFactor) {
        if (!(movingFactor <= 1.0E-5F)) {
            float f = ageInTicks * 0.33F;
            float f1 = Mth.sin(f);
            float f2 = Mth.cos(f);
            float f3 = 0.13F * f1;
            this.body.xRot += (xRot * (float) (Math.PI / 180.0) + f3) * movingFactor;
            this.head.xRot -= f3 * 1.8F * movingFactor;
            this.body.y -= 0.45F * f2 * movingFactor;
            this.topGills.xRot += (-0.5F * f1 - 0.8F) * movingFactor;
            float f4 = (0.3F * f1 + 0.9F) * movingFactor;
            this.leftGills.yRot += f4;
            this.rightGills.yRot -= f4;
            this.tail.yRot = this.tail.yRot + 0.3F * Mth.cos(f * 0.9F) * movingFactor;
            this.leftHindLeg.xRot += 1.8849558F * movingFactor;
            this.leftHindLeg.yRot += -0.4F * f1 * movingFactor;
            this.leftHindLeg.zRot += (float) (Math.PI / 2) * movingFactor;
            this.leftFrontLeg.xRot += 1.8849558F * movingFactor;
            this.leftFrontLeg.yRot += (-0.2F * f2 - 0.1F) * movingFactor;
            this.leftFrontLeg.zRot += (float) (Math.PI / 2) * movingFactor;
        }
    }

    private void setupPlayDeadAnimation(float playingDeadFactor) {
        if (!(playingDeadFactor <= 1.0E-5F)) {
            this.leftHindLeg.xRot += 1.4137167F * playingDeadFactor;
            this.leftHindLeg.yRot += 1.0995574F * playingDeadFactor;
            this.leftHindLeg.zRot += (float) (Math.PI / 4) * playingDeadFactor;
            this.leftFrontLeg.xRot += (float) (Math.PI / 4) * playingDeadFactor;
            this.leftFrontLeg.yRot += 2.042035F * playingDeadFactor;
            this.body.xRot += -0.15F * playingDeadFactor;
            this.body.zRot += 0.35F * playingDeadFactor;
        }
    }

    private void applyMirrorLegRotations(float factor) {
        if (!(factor <= 1.0E-5F)) {
            this.rightHindLeg.xRot = this.rightHindLeg.xRot + this.leftHindLeg.xRot * factor;
            ModelPart modelpart = this.rightHindLeg;
            modelpart.yRot = modelpart.yRot + -this.leftHindLeg.yRot * factor;
            modelpart = this.rightHindLeg;
            modelpart.zRot = modelpart.zRot + -this.leftHindLeg.zRot * factor;
            this.rightFrontLeg.xRot = this.rightFrontLeg.xRot + this.leftFrontLeg.xRot * factor;
            modelpart = this.rightFrontLeg;
            modelpart.yRot = modelpart.yRot + -this.leftFrontLeg.yRot * factor;
            modelpart = this.rightFrontLeg;
            modelpart.zRot = modelpart.zRot + -this.leftFrontLeg.zRot * factor;
        }
    }
}
