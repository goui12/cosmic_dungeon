package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.CowVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CowRenderer extends MobRenderer<Cow, CowRenderState, CowModel> {
    private final Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> models;

    public CowRenderer(EntityRendererProvider.Context p_173956_) {
        super(p_173956_, new CowModel(p_173956_.bakeLayer(ModelLayers.COW)), 0.7F);
        this.models = bakeModels(p_173956_);
    }

    private static Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> bakeModels(EntityRendererProvider.Context context) {
        return Maps.newEnumMap(
            Map.of(
                CowVariant.ModelType.NORMAL,
                new AdultAndBabyModelPair<>(new CowModel(context.bakeLayer(ModelLayers.COW)), new CowModel(context.bakeLayer(ModelLayers.COW_BABY))),
                CowVariant.ModelType.WARM,
                new AdultAndBabyModelPair<>(
                    new CowModel(context.bakeLayer(ModelLayers.WARM_COW)), new CowModel(context.bakeLayer(ModelLayers.WARM_COW_BABY))
                ),
                CowVariant.ModelType.COLD,
                new AdultAndBabyModelPair<>(
                    new CowModel(context.bakeLayer(ModelLayers.COLD_COW)), new CowModel(context.bakeLayer(ModelLayers.COLD_COW_BABY))
                )
            )
        );
    }

    public ResourceLocation getTextureLocation(CowRenderState renderState) {
        return renderState.variant == null ? MissingTextureAtlasSprite.getLocation() : renderState.variant.modelAndTexture().asset().texturePath();
    }

    public CowRenderState createRenderState() {
        return new CowRenderState();
    }

    public void extractRenderState(Cow entity, CowRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.variant = entity.getVariant().value();
    }

    public void submit(CowRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.variant != null) {
            this.model = this.models.get(renderState.variant.modelAndTexture().model()).getModel(renderState.isBaby);
            super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        }
    }
}
