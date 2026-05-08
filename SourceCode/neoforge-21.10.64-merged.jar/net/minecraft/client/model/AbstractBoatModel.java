package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractBoatModel extends EntityModel<BoatRenderState> {
    private final ModelPart leftPaddle;
    private final ModelPart rightPaddle;

    public AbstractBoatModel(ModelPart root) {
        super(root);
        this.leftPaddle = root.getChild("left_paddle");
        this.rightPaddle = root.getChild("right_paddle");
    }

    public void setupAnim(BoatRenderState renderState) {
        super.setupAnim(renderState);
        animatePaddle(renderState.rowingTimeLeft, 0, this.leftPaddle);
        animatePaddle(renderState.rowingTimeRight, 1, this.rightPaddle);
    }

    private static void animatePaddle(float rowingTime, int side, ModelPart part) {
        part.xRot = Mth.clampedLerp((float) (-Math.PI / 3), (float) (-Math.PI / 12), (Mth.sin(-rowingTime) + 1.0F) / 2.0F);
        part.yRot = Mth.clampedLerp((float) (-Math.PI / 4), (float) (Math.PI / 4), (Mth.sin(-rowingTime + 1.0F) + 1.0F) / 2.0F);
        if (side == 1) {
            part.yRot = (float) Math.PI - part.yRot;
        }
    }
}
