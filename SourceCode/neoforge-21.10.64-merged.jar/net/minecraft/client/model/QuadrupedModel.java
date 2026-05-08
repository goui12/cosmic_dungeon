package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuadrupedModel<T extends LivingEntityRenderState> extends EntityModel<T> {
    protected final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart rightHindLeg;
    protected final ModelPart leftHindLeg;
    protected final ModelPart rightFrontLeg;
    protected final ModelPart leftFrontLeg;

    protected QuadrupedModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static MeshDefinition createBodyMesh(int yOffset, boolean mirrorRight, boolean mirrorLeft, CubeDeformation cubeDeformation) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F, cubeDeformation),
            PartPose.offset(0.0F, 18 - yOffset, -6.0F)
        );
        partdefinition.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(28, 8).addBox(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F, cubeDeformation),
            PartPose.offsetAndRotation(0.0F, 17 - yOffset, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );
        createLegs(partdefinition, mirrorRight, mirrorLeft, yOffset, cubeDeformation);
        return meshdefinition;
    }

    static void createLegs(PartDefinition part, boolean mirrorRight, boolean mirrorLeft, int yOffset, CubeDeformation cubeDeformation) {
        CubeListBuilder cubelistbuilder = CubeListBuilder.create()
            .mirror(mirrorLeft)
            .texOffs(0, 16)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, (float)yOffset, 4.0F, cubeDeformation);
        CubeListBuilder cubelistbuilder1 = CubeListBuilder.create()
            .mirror(mirrorRight)
            .texOffs(0, 16)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, (float)yOffset, 4.0F, cubeDeformation);
        part.addOrReplaceChild("right_hind_leg", cubelistbuilder, PartPose.offset(-3.0F, 24 - yOffset, 7.0F));
        part.addOrReplaceChild("left_hind_leg", cubelistbuilder1, PartPose.offset(3.0F, 24 - yOffset, 7.0F));
        part.addOrReplaceChild("right_front_leg", cubelistbuilder, PartPose.offset(-3.0F, 24 - yOffset, -5.0F));
        part.addOrReplaceChild("left_front_leg", cubelistbuilder1, PartPose.offset(3.0F, 24 - yOffset, -5.0F));
    }

    public void setupAnim(T renderState) {
        super.setupAnim(renderState);
        this.head.xRot = renderState.xRot * (float) (Math.PI / 180.0);
        this.head.yRot = renderState.yRot * (float) (Math.PI / 180.0);
        float f = renderState.walkAnimationPos;
        float f1 = renderState.walkAnimationSpeed;
        this.rightHindLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * f1;
        this.leftHindLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * f1;
        this.rightFrontLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * f1;
        this.leftFrontLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * f1;
    }
}
