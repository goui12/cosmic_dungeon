package net.minecraft.client.model;

import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BellModel extends Model<BellModel.State> {
    private static final String BELL_BODY = "bell_body";
    private final ModelPart bellBody;

    public BellModel(ModelPart root) {
        super(root, RenderType::entitySolid);
        this.bellBody = root.getChild("bell_body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
            "bell_body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offset(8.0F, 12.0F, 8.0F)
        );
        partdefinition1.addOrReplaceChild(
            "bell_base", CubeListBuilder.create().texOffs(0, 13).addBox(4.0F, 4.0F, 4.0F, 8.0F, 2.0F, 8.0F), PartPose.offset(-8.0F, -12.0F, -8.0F)
        );
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    public void setupAnim(BellModel.State renderState) {
        super.setupAnim(renderState);
        float f = 0.0F;
        float f1 = 0.0F;
        if (renderState.shakeDirection != null) {
            float f2 = Mth.sin(renderState.ticks / (float) Math.PI) / (4.0F + renderState.ticks / 3.0F);
            switch (renderState.shakeDirection) {
                case NORTH:
                    f = -f2;
                    break;
                case SOUTH:
                    f = f2;
                    break;
                case EAST:
                    f1 = -f2;
                    break;
                case WEST:
                    f1 = f2;
            }
        }

        this.bellBody.xRot = f;
        this.bellBody.zRot = f1;
    }

    @OnlyIn(Dist.CLIENT)
    public record State(float ticks, @Nullable Direction shakeDirection) {
    }
}
