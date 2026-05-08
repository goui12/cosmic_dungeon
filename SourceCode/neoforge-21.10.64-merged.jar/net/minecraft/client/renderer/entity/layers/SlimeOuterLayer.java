package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SlimeOuterLayer extends RenderLayer<SlimeRenderState, SlimeModel> {
    private final SlimeModel model;

    public SlimeOuterLayer(RenderLayerParent<SlimeRenderState, SlimeModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new SlimeModel(modelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, SlimeRenderState renderState, float yRot, float xRot) {
        boolean flag = renderState.appearsGlowing() && renderState.isInvisible;
        if (!renderState.isInvisible || flag) {
            int i = LivingEntityRenderer.getOverlayCoords(renderState, 0.0F);
            if (flag) {
                nodeCollector.order(1)
                    .submitModel(
                        this.model,
                        renderState,
                        poseStack,
                        RenderType.outline(SlimeRenderer.SLIME_LOCATION),
                        packedLight,
                        i,
                        -1,
                        null,
                        renderState.outlineColor,
                        null
                    );
            } else {
                nodeCollector.order(1)
                    .submitModel(
                        this.model,
                        renderState,
                        poseStack,
                        RenderType.entityTranslucent(SlimeRenderer.SLIME_LOCATION),
                        packedLight,
                        i,
                        -1,
                        null,
                        renderState.outlineColor,
                        null
                    );
            }
        }
    }
}
