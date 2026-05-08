package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
    private final RenderLayerParent<S, M> renderer;

    public RenderLayer(RenderLayerParent<S, M> renderer) {
        this.renderer = renderer;
    }

    protected static <S extends LivingEntityRenderState> void coloredCutoutModelCopyLayerRender(
        Model<? super S> model,
        ResourceLocation textureLocation,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        S renderState,
        int tintColor,
        int outlineColor
    ) {
        if (!renderState.isInvisible) {
            renderColoredCutoutModel(model, textureLocation, poseStack, nodeCollector, packedLight, renderState, tintColor, outlineColor);
        }
    }

    protected static <S extends LivingEntityRenderState> void renderColoredCutoutModel(
        Model<? super S> model,
        ResourceLocation textureLocation,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        S renderState,
        int tintColor,
        int outlineColor
    ) {
        nodeCollector.order(outlineColor)
            .submitModel(
                model,
                renderState,
                poseStack,
                RenderType.entityCutoutNoCull(textureLocation),
                packedLight,
                LivingEntityRenderer.getOverlayCoords(renderState, 0.0F),
                tintColor,
                null,
                renderState.outlineColor,
                null
            );
    }

    public M getParentModel() {
        return this.renderer.getModel();
    }

    public abstract void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot);
}
