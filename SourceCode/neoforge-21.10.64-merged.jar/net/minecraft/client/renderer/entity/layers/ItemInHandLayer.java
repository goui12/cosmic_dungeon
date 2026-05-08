package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel> extends RenderLayer<S, M> {
    public ItemInHandLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
        this.submitArmWithItem(renderState, renderState.rightHandItem, HumanoidArm.RIGHT, poseStack, nodeCollector, packedLight);
        this.submitArmWithItem(renderState, renderState.leftHandItem, HumanoidArm.LEFT, poseStack, nodeCollector, packedLight);
    }

    protected void submitArmWithItem(
        S renderState, ItemStackRenderState stackRenderState, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight
    ) {
        if (!stackRenderState.isEmpty()) {
            poseStack.pushPose();
            this.getParentModel().translateToHand(renderState, arm, poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean flag = arm == HumanoidArm.LEFT;
            poseStack.translate((flag ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            stackRenderState.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
            poseStack.popPose();
        }
    }
}
