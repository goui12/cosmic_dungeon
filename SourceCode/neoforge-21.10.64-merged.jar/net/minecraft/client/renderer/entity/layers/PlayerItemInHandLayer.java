package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerItemInHandLayer<S extends AvatarRenderState, M extends EntityModel<S> & ArmedModel & HeadedModel> extends ItemInHandLayer<S, M> {
    private static final float X_ROT_MIN = (float) (-Math.PI / 6);
    private static final float X_ROT_MAX = (float) (Math.PI / 2);

    public PlayerItemInHandLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    protected void submitArmWithItem(
        S renderState, ItemStackRenderState stackRenderState, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight
    ) {
        if (!stackRenderState.isEmpty()) {
            InteractionHand interactionhand = arm == renderState.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (renderState.isUsingItem && renderState.useItemHand == interactionhand && renderState.attackTime < 1.0E-5F && !renderState.heldOnHead.isEmpty()) {
                this.renderItemHeldToEye(renderState, arm, poseStack, nodeCollector, packedLight);
            } else {
                super.submitArmWithItem(renderState, stackRenderState, arm, poseStack, nodeCollector, packedLight);
            }
        }
    }

    private void renderItemHeldToEye(S renderState, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight) {
        poseStack.pushPose();
        this.getParentModel().root().translateAndRotate(poseStack);
        ModelPart modelpart = this.getParentModel().getHead();
        float f = modelpart.xRot;
        modelpart.xRot = Mth.clamp(modelpart.xRot, (float) (-Math.PI / 6), (float) (Math.PI / 2));
        modelpart.translateAndRotate(poseStack);
        modelpart.xRot = f;
        CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
        boolean flag = arm == HumanoidArm.LEFT;
        poseStack.translate((flag ? -2.5F : 2.5F) / 16.0F, -0.0625F, 0.0F);
        renderState.heldOnHead.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        poseStack.popPose();
    }
}
