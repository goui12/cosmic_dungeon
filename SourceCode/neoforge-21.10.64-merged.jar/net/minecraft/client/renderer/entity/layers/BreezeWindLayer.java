package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeWindLayer extends RenderLayer<BreezeRenderState, BreezeModel> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/breeze/breeze_wind.png");
    private final BreezeModel model;

    public BreezeWindLayer(RenderLayerParent<BreezeRenderState, BreezeModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new BreezeModel(modelSet.bakeLayer(ModelLayers.BREEZE_WIND));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, BreezeRenderState renderState, float yRot, float xRot) {
        RenderType rendertype = RenderType.breezeWind(TEXTURE_LOCATION, this.xOffset(renderState.ageInTicks) % 1.0F, 0.0F);
        nodeCollector.order(1)
            .submitModel(this.model, renderState, poseStack, rendertype, packedLight, OverlayTexture.NO_OVERLAY, -1, null, renderState.outlineColor, null);
    }

    private float xOffset(float tickCount) {
        return tickCount * 0.02F;
    }
}
