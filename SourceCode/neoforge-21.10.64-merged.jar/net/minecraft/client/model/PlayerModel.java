package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerModel extends HumanoidModel<AvatarRenderState> {
    protected static final String LEFT_SLEEVE = "left_sleeve";
    protected static final String RIGHT_SLEEVE = "right_sleeve";
    protected static final String LEFT_PANTS = "left_pants";
    protected static final String RIGHT_PANTS = "right_pants";
    private final List<ModelPart> bodyParts;
    public final ModelPart leftSleeve;
    public final ModelPart rightSleeve;
    public final ModelPart leftPants;
    public final ModelPart rightPants;
    public final ModelPart jacket;
    private final boolean slim;

    public PlayerModel(ModelPart root, boolean slim) {
        super(root, RenderType::entityTranslucent);
        this.slim = slim;
        this.leftSleeve = this.leftArm.getChild("left_sleeve");
        this.rightSleeve = this.rightArm.getChild("right_sleeve");
        this.leftPants = this.leftLeg.getChild("left_pants");
        this.rightPants = this.rightLeg.getChild("right_pants");
        this.jacket = this.body.getChild("jacket");
        this.bodyParts = List.of(this.head, this.body, this.leftArm, this.rightArm, this.leftLeg, this.rightLeg);
    }

    public static MeshDefinition createMesh(CubeDeformation cubeDeformation, boolean slim) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        float f = 0.25F;
        if (slim) {
            PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation),
                PartPose.offset(5.0F, 2.0F, 0.0F)
            );
            PartDefinition partdefinition2 = partdefinition.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation),
                PartPose.offset(-5.0F, 2.0F, 0.0F)
            );
            partdefinition1.addOrReplaceChild(
                "left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
            );
            partdefinition2.addOrReplaceChild(
                "right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
            );
        } else {
            PartDefinition partdefinition4 = partdefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation),
                PartPose.offset(5.0F, 2.0F, 0.0F)
            );
            PartDefinition partdefinition6 = partdefinition.getChild("right_arm");
            partdefinition4.addOrReplaceChild(
                "left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
            );
            partdefinition6.addOrReplaceChild(
                "right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
            );
        }

        PartDefinition partdefinition5 = partdefinition.addOrReplaceChild(
            "left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(1.9F, 12.0F, 0.0F)
        );
        PartDefinition partdefinition7 = partdefinition.getChild("right_leg");
        partdefinition5.addOrReplaceChild(
            "left_pants", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
        );
        partdefinition7.addOrReplaceChild(
            "right_pants", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
        );
        PartDefinition partdefinition3 = partdefinition.getChild("body");
        partdefinition3.addOrReplaceChild(
            "jacket", CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
        );
        return meshdefinition;
    }

    public static ArmorModelSet<MeshDefinition> createArmorMeshSet(CubeDeformation innerCubeDeformation, CubeDeformation outerCubeDeformation) {
        return HumanoidModel.createArmorMeshSet(innerCubeDeformation, outerCubeDeformation).map(p_432754_ -> {
            PartDefinition partdefinition = p_432754_.getRoot();
            PartDefinition partdefinition1 = partdefinition.getChild("left_arm");
            PartDefinition partdefinition2 = partdefinition.getChild("right_arm");
            partdefinition1.addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.ZERO);
            partdefinition2.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
            PartDefinition partdefinition3 = partdefinition.getChild("left_leg");
            PartDefinition partdefinition4 = partdefinition.getChild("right_leg");
            partdefinition3.addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.ZERO);
            partdefinition4.addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.ZERO);
            PartDefinition partdefinition5 = partdefinition.getChild("body");
            partdefinition5.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.ZERO);
            return (MeshDefinition)p_432754_;
        });
    }

    public void setupAnim(AvatarRenderState renderState) {
        boolean flag = !renderState.isSpectator;
        this.body.visible = flag;
        this.rightArm.visible = flag;
        this.leftArm.visible = flag;
        this.rightLeg.visible = flag;
        this.leftLeg.visible = flag;
        this.hat.visible = renderState.showHat;
        this.jacket.visible = renderState.showJacket;
        this.leftPants.visible = renderState.showLeftPants;
        this.rightPants.visible = renderState.showRightPants;
        this.leftSleeve.visible = renderState.showLeftSleeve;
        this.rightSleeve.visible = renderState.showRightSleeve;
        super.setupAnim(renderState);
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.leftSleeve.visible = visible;
        this.rightSleeve.visible = visible;
        this.leftPants.visible = visible;
        this.rightPants.visible = visible;
        this.jacket.visible = visible;
    }

    public void translateToHand(AvatarRenderState renderState, HumanoidArm arm, PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        ModelPart modelpart = this.getArm(arm);
        if (this.slim) {
            float f = 0.5F * (arm == HumanoidArm.RIGHT ? 1 : -1);
            modelpart.x += f;
            modelpart.translateAndRotate(poseStack);
            modelpart.x -= f;
        } else {
            modelpart.translateAndRotate(poseStack);
        }
    }

    public ModelPart getRandomBodyPart(RandomSource random) {
        return Util.getRandom(this.bodyParts, random);
    }
}
