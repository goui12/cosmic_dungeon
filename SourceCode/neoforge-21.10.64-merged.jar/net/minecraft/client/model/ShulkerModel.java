package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShulkerModel extends EntityModel<ShulkerRenderState> {
    public static final String LID = "lid";
    private static final String BASE = "base";
    private final ModelPart lid;
    private final ModelPart head;

    public ShulkerModel(ModelPart root) {
        super(root, RenderType::entityCutoutNoCullZOffset);
        this.lid = root.getChild("lid");
        this.head = root.getChild("head");
    }

    private static MeshDefinition createShellMesh() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "lid", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 12.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "base", CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 8.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        return meshdefinition;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = createShellMesh();
        meshdefinition.getRoot()
            .addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 52).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public static LayerDefinition createBoxLayer() {
        MeshDefinition meshdefinition = createShellMesh();
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setupAnim(ShulkerRenderState renderState) {
        super.setupAnim(renderState);
        float f = (0.5F + renderState.peekAmount) * (float) Math.PI;
        float f1 = -1.0F + Mth.sin(f);
        float f2 = 0.0F;
        if (f > (float) Math.PI) {
            f2 = Mth.sin(renderState.ageInTicks * 0.1F) * 0.7F;
        }

        this.lid.setPos(0.0F, 16.0F + Mth.sin(f) * 8.0F + f2, 0.0F);
        if (renderState.peekAmount > 0.3F) {
            this.lid.yRot = f1 * f1 * f1 * f1 * (float) Math.PI * 0.125F;
        } else {
            this.lid.yRot = 0.0F;
        }

        this.head.xRot = renderState.xRot * (float) (Math.PI / 180.0);
        this.head.yRot = (renderState.yHeadRot - 180.0F - renderState.yBodyRot) * (float) (Math.PI / 180.0);
    }
}
