package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.ColdChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.ChickenVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChickenRenderer extends MobRenderer<Chicken, ChickenRenderState, ChickenModel> {
    private final Map<ChickenVariant.ModelType, AdultAndBabyModelPair<ChickenModel>> models;

    public ChickenRenderer(EntityRendererProvider.Context p_173952_) {
        super(p_173952_, new ChickenModel(p_173952_.bakeLayer(ModelLayers.CHICKEN)), 0.3F);
        this.models = bakeModels(p_173952_);
    }

    private static Map<ChickenVariant.ModelType, AdultAndBabyModelPair<ChickenModel>> bakeModels(EntityRendererProvider.Context context) {
        return Maps.newEnumMap(
            Map.of(
                ChickenVariant.ModelType.NORMAL,
                new AdultAndBabyModelPair<>(
                    new ChickenModel(context.bakeLayer(ModelLayers.CHICKEN)), new ChickenModel(context.bakeLayer(ModelLayers.CHICKEN_BABY))
                ),
                ChickenVariant.ModelType.COLD,
                new AdultAndBabyModelPair<>(
                    new ColdChickenModel(context.bakeLayer(ModelLayers.COLD_CHICKEN)),
                    new ColdChickenModel(context.bakeLayer(ModelLayers.COLD_CHICKEN_BABY))
                )
            )
        );
    }

    public void submit(ChickenRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.variant != null) {
            this.model = this.models.get(renderState.variant.modelAndTexture().model()).getModel(renderState.isBaby);
            super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        }
    }

    public ResourceLocation getTextureLocation(ChickenRenderState renderState) {
        return renderState.variant == null ? MissingTextureAtlasSprite.getLocation() : renderState.variant.modelAndTexture().asset().texturePath();
    }

    public ChickenRenderState createRenderState() {
        return new ChickenRenderState();
    }

    public void extractRenderState(Chicken entity, ChickenRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.flap = Mth.lerp(partialTick, entity.oFlap, entity.flap);
        reusedState.flapSpeed = Mth.lerp(partialTick, entity.oFlapSpeed, entity.flapSpeed);
        reusedState.variant = entity.getVariant().value();
    }
}
