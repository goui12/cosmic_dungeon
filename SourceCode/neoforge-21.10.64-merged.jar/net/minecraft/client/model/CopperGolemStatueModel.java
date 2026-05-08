package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CopperGolemStatueModel extends Model<Direction> {
    public CopperGolemStatueModel(ModelPart root) {
        super(root, RenderType::entityCutoutNoCull);
    }

    public void setupAnim(Direction renderState) {
        this.root.y = 0.0F;
        this.root.yRot = renderState.getOpposite().toYRot() * (float) (Math.PI / 180.0);
        this.root.zRot = (float) Math.PI;
    }
}
