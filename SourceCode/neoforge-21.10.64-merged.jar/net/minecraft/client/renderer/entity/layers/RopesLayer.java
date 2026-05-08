package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HappyGhastModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RopesLayer<M extends HappyGhastModel> extends RenderLayer<HappyGhastRenderState, M> {
    private final RenderType ropes;
    private final HappyGhastModel adultModel;
    private final HappyGhastModel babyModel;

    public RopesLayer(RenderLayerParent<HappyGhastRenderState, M> renderer, EntityModelSet entityModels, ResourceLocation texture) {
        super(renderer);
        this.ropes = RenderType.entityCutoutNoCull(texture);
        this.adultModel = new HappyGhastModel(entityModels.bakeLayer(ModelLayers.HAPPY_GHAST_ROPES));
        this.babyModel = new HappyGhastModel(entityModels.bakeLayer(ModelLayers.HAPPY_GHAST_BABY_ROPES));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, HappyGhastRenderState renderState, float yRot, float xRot) {
        if (renderState.isLeashHolder && renderState.bodyItem.is(ItemTags.HARNESSES)) {
            HappyGhastModel happyghastmodel = renderState.isBaby ? this.babyModel : this.adultModel;
            nodeCollector.submitModel(happyghastmodel, renderState, poseStack, this.ropes, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor, null);
        }
    }
}
