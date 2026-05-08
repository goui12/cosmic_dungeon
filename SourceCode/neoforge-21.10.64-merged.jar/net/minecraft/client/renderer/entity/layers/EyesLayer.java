package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class EyesLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    public EyesLayer(RenderLayerParent<S, M> p_116981_) {
        super(p_116981_);
    }

    @Override
    public void submit(PoseStack p_433452_, SubmitNodeCollector p_433171_, int p_434650_, S p_435883_, float p_433542_, float p_435619_) {
        p_433171_.order(1)
            .submitModel(
                this.getParentModel(), p_435883_, p_433452_, this.renderType(), p_434650_, OverlayTexture.NO_OVERLAY, -1, null, p_435883_.outlineColor, null
            );
    }

    public abstract RenderType renderType();
}
