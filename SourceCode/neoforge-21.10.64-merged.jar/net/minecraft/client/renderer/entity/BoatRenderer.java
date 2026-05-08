package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoatRenderer extends AbstractBoatRenderer {
    private final Model.Simple waterPatchModel;
    private final ResourceLocation texture;
    private final EntityModel<BoatRenderState> model;

    public BoatRenderer(EntityRendererProvider.Context context, ModelLayerLocation modelLayer) {
        super(context);
        this.texture = modelLayer.model().withPath(p_375447_ -> "textures/entity/" + p_375447_ + ".png");
        this.waterPatchModel = new Model.Simple(context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), p_359275_ -> RenderType.waterMask());
        this.model = new BoatModel(context.bakeLayer(modelLayer));
    }

    @Override
    protected EntityModel<BoatRenderState> model() {
        return this.model;
    }

    @Override
    protected RenderType renderType() {
        return this.model.renderType(this.texture);
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int lightCoords) {
        if (!renderState.isUnderWater) {
            nodeCollector.submitModel(
                this.waterPatchModel,
                Unit.INSTANCE,
                poseStack,
                this.waterPatchModel.renderType(this.texture),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                renderState.outlineColor,
                null
            );
        }
    }
}
