package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrossedArmsItemLayer<S extends HoldingEntityRenderState, M extends EntityModel<S> & VillagerLikeModel> extends RenderLayer<S, M> {
    public CrossedArmsItemLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
        ItemStackRenderState itemstackrenderstate = renderState.heldItem;
        if (!itemstackrenderstate.isEmpty()) {
            poseStack.pushPose();
            this.applyTranslation(renderState, poseStack);
            itemstackrenderstate.submit(poseStack, nodeCollector, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
            poseStack.popPose();
        }
    }

    protected void applyTranslation(S renderState, PoseStack poseStack) {
        this.getParentModel().translateToArms(renderState, poseStack);
        poseStack.mulPose(Axis.XP.rotation(0.75F));
        poseStack.scale(1.07F, 1.07F, 1.07F);
        poseStack.translate(0.0F, 0.13F, -0.34F);
        poseStack.mulPose(Axis.XP.rotation((float) Math.PI));
    }
}
