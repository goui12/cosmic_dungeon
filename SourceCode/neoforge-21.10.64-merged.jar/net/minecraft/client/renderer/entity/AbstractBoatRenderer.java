package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractBoatRenderer extends EntityRenderer<AbstractBoat, BoatRenderState> {
    public AbstractBoatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.8F;
    }

    public void submit(BoatRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - renderState.yRot));
        float f = renderState.hurtTime;
        if (f > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(f) * f * renderState.damageTime / 10.0F * renderState.hurtDir));
        }

        if (!renderState.isUnderWater && !Mth.equal(renderState.bubbleAngle, 0.0F)) {
            poseStack.mulPose(new Quaternionf().setAngleAxis(renderState.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        nodeCollector.submitModel(
            this.model(), renderState, poseStack, this.renderType(), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, null
        );
        this.submitTypeAdditions(renderState, poseStack, nodeCollector, renderState.lightCoords);
        poseStack.popPose();
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
    }

    protected void submitTypeAdditions(BoatRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int lightCoords) {
    }

    protected abstract EntityModel<BoatRenderState> model();

    protected abstract RenderType renderType();

    public BoatRenderState createRenderState() {
        return new BoatRenderState();
    }

    public void extractRenderState(AbstractBoat entity, BoatRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.yRot = entity.getYRot(partialTick);
        reusedState.hurtTime = entity.getHurtTime() - partialTick;
        reusedState.hurtDir = entity.getHurtDir();
        reusedState.damageTime = Math.max(entity.getDamage() - partialTick, 0.0F);
        reusedState.bubbleAngle = entity.getBubbleAngle(partialTick);
        reusedState.isUnderWater = entity.isUnderWater();
        reusedState.rowingTimeLeft = entity.getRowingTime(0, partialTick);
        reusedState.rowingTimeRight = entity.getRowingTime(1, partialTick);
    }
}
