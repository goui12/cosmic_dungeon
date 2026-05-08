package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpinAttackEffectModel extends EntityModel<AvatarRenderState> {
    private static final int BOX_COUNT = 2;
    private final ModelPart[] boxes = new ModelPart[2];

    public SpinAttackEffectModel(ModelPart root) {
        super(root);

        for (int i = 0; i < 2; i++) {
            this.boxes[i] = root.getChild(boxName(i));
        }
    }

    private static String boxName(int index) {
        return "box" + index;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        for (int i = 0; i < 2; i++) {
            float f = -3.2F + 9.6F * (i + 1);
            float f1 = 0.75F * (i + 1);
            partdefinition.addOrReplaceChild(
                boxName(i), CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F + f, -8.0F, 16.0F, 32.0F, 16.0F), PartPose.ZERO.withScale(f1)
            );
        }

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setupAnim(AvatarRenderState renderState) {
        super.setupAnim(renderState);

        for (int i = 0; i < this.boxes.length; i++) {
            float f = renderState.ageInTicks * -(45 + (i + 1) * 5);
            this.boxes[i].yRot = Mth.wrapDegrees(f) * (float) (Math.PI / 180.0);
        }
    }
}
