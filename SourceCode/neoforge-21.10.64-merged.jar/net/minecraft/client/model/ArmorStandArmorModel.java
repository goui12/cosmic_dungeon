package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmorStandArmorModel extends HumanoidModel<ArmorStandRenderState> {
    public ArmorStandArmorModel(ModelPart root) {
        super(root);
    }

    public static ArmorModelSet<LayerDefinition> createArmorLayerSet(CubeDeformation innerCubeDeformation, CubeDeformation outerCubeDeformation) {
        return createArmorMeshSet(ArmorStandArmorModel::createBaseMesh, innerCubeDeformation, outerCubeDeformation).map(p_435881_ -> LayerDefinition.create(p_435881_, 64, 32));
    }

    private static MeshDefinition createBaseMesh(CubeDeformation cubeDeformation) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
            "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, cubeDeformation), PartPose.offset(0.0F, 1.0F, 0.0F)
        );
        partdefinition1.addOrReplaceChild(
            "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, cubeDeformation.extend(0.5F)), PartPose.ZERO
        );
        partdefinition.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(-0.1F)),
            PartPose.offset(-1.9F, 11.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(-0.1F)),
            PartPose.offset(1.9F, 11.0F, 0.0F)
        );
        return meshdefinition;
    }

    public void setupAnim(ArmorStandRenderState renderState) {
        super.setupAnim(renderState);
        this.head.xRot = (float) (Math.PI / 180.0) * renderState.headPose.x();
        this.head.yRot = (float) (Math.PI / 180.0) * renderState.headPose.y();
        this.head.zRot = (float) (Math.PI / 180.0) * renderState.headPose.z();
        this.body.xRot = (float) (Math.PI / 180.0) * renderState.bodyPose.x();
        this.body.yRot = (float) (Math.PI / 180.0) * renderState.bodyPose.y();
        this.body.zRot = (float) (Math.PI / 180.0) * renderState.bodyPose.z();
        this.leftArm.xRot = (float) (Math.PI / 180.0) * renderState.leftArmPose.x();
        this.leftArm.yRot = (float) (Math.PI / 180.0) * renderState.leftArmPose.y();
        this.leftArm.zRot = (float) (Math.PI / 180.0) * renderState.leftArmPose.z();
        this.rightArm.xRot = (float) (Math.PI / 180.0) * renderState.rightArmPose.x();
        this.rightArm.yRot = (float) (Math.PI / 180.0) * renderState.rightArmPose.y();
        this.rightArm.zRot = (float) (Math.PI / 180.0) * renderState.rightArmPose.z();
        this.leftLeg.xRot = (float) (Math.PI / 180.0) * renderState.leftLegPose.x();
        this.leftLeg.yRot = (float) (Math.PI / 180.0) * renderState.leftLegPose.y();
        this.leftLeg.zRot = (float) (Math.PI / 180.0) * renderState.leftLegPose.z();
        this.rightLeg.xRot = (float) (Math.PI / 180.0) * renderState.rightLegPose.x();
        this.rightLeg.yRot = (float) (Math.PI / 180.0) * renderState.rightLegPose.y();
        this.rightLeg.zRot = (float) (Math.PI / 180.0) * renderState.rightLegPose.z();
    }
}
