package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.DolphinModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DolphinCarryingItemLayer extends RenderLayer<DolphinRenderState, DolphinModel> {
    public DolphinCarryingItemLayer(RenderLayerParent<DolphinRenderState, DolphinModel> p_234834_) {
        super(p_234834_);
    }

    public void submit(PoseStack p_116886_, SubmitNodeCollector p_433732_, int p_116888_, DolphinRenderState p_362079_, float p_116890_, float p_116891_) {
        ItemStackRenderState itemstackrenderstate = p_362079_.heldItem;
        if (!itemstackrenderstate.isEmpty()) {
            p_116886_.pushPose();
            float f = 1.0F;
            float f1 = -1.0F;
            float f2 = Mth.abs(p_362079_.xRot) / 60.0F;
            if (p_362079_.xRot < 0.0F) {
                p_116886_.translate(0.0F, 1.0F - f2 * 0.5F, -1.0F + f2 * 0.5F);
            } else {
                p_116886_.translate(0.0F, 1.0F + f2 * 0.8F, -1.0F + f2 * 0.2F);
            }

            itemstackrenderstate.submit(p_116886_, p_433732_, p_116888_, OverlayTexture.NO_OVERLAY, p_362079_.outlineColor);
            p_116886_.popPose();
        }
    }
}
