package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BannerFlagModel extends Model<Float> {
    private final ModelPart flag;

    public BannerFlagModel(ModelPart root) {
        super(root, RenderType::entitySolid);
        this.flag = root.getChild("flag");
    }

    public static LayerDefinition createFlagLayer(boolean isStanding) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "flag",
            CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F),
            PartPose.offset(0.0F, isStanding ? -44.0F : -20.5F, isStanding ? 0.0F : 10.5F)
        );
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setupAnim(Float renderState) {
        super.setupAnim(renderState);
        this.flag.xRot = (-0.0125F + 0.01F * Mth.cos((float) (Math.PI * 2) * renderState)) * (float) Math.PI;
    }
}
