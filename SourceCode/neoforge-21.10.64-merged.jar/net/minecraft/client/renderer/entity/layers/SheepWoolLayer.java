package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepWoolLayer extends RenderLayer<SheepRenderState, SheepModel> {
    private static final ResourceLocation SHEEP_WOOL_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep_wool.png");
    private final EntityModel<SheepRenderState> adultModel;
    private final EntityModel<SheepRenderState> babyModel;

    public SheepWoolLayer(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL));
        this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, SheepRenderState renderState, float yRot, float xRot) {
        if (!renderState.isSheared) {
            EntityModel<SheepRenderState> entitymodel = renderState.isBaby ? this.babyModel : this.adultModel;
            if (renderState.isInvisible) {
                if (renderState.appearsGlowing()) {
                    nodeCollector.submitModel(
                        entitymodel,
                        renderState,
                        poseStack,
                        RenderType.outline(SHEEP_WOOL_LOCATION),
                        packedLight,
                        LivingEntityRenderer.getOverlayCoords(renderState, 0.0F),
                        -16777216,
                        null,
                        renderState.outlineColor,
                        null
                    );
                }
            } else {
                coloredCutoutModelCopyLayerRender(entitymodel, SHEEP_WOOL_LOCATION, poseStack, nodeCollector, packedLight, renderState, renderState.getWoolColor(), 0);
            }
        }
    }
}
