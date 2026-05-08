package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.ColdPigModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PigVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PigRenderer extends MobRenderer<Pig, PigRenderState, PigModel> {
    private final Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> models;

    public PigRenderer(EntityRendererProvider.Context p_174340_) {
        super(p_174340_, new PigModel(p_174340_.bakeLayer(ModelLayers.PIG)), 0.7F);
        this.models = bakeModels(p_174340_);
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_174340_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.PIG_SADDLE,
                p_397421_ -> p_397421_.saddle,
                new PigModel(p_174340_.bakeLayer(ModelLayers.PIG_SADDLE)),
                new PigModel(p_174340_.bakeLayer(ModelLayers.PIG_BABY_SADDLE))
            )
        );
    }

    private static Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> bakeModels(EntityRendererProvider.Context context) {
        return Maps.newEnumMap(
            Map.of(
                PigVariant.ModelType.NORMAL,
                new AdultAndBabyModelPair<>(new PigModel(context.bakeLayer(ModelLayers.PIG)), new PigModel(context.bakeLayer(ModelLayers.PIG_BABY))),
                PigVariant.ModelType.COLD,
                new AdultAndBabyModelPair<>(
                    new ColdPigModel(context.bakeLayer(ModelLayers.COLD_PIG)), new ColdPigModel(context.bakeLayer(ModelLayers.COLD_PIG_BABY))
                )
            )
        );
    }

    public void submit(PigRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.variant != null) {
            this.model = this.models.get(renderState.variant.modelAndTexture().model()).getModel(renderState.isBaby);
            super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        }
    }

    public ResourceLocation getTextureLocation(PigRenderState renderState) {
        return renderState.variant == null ? MissingTextureAtlasSprite.getLocation() : renderState.variant.modelAndTexture().asset().texturePath();
    }

    public PigRenderState createRenderState() {
        return new PigRenderState();
    }

    public void extractRenderState(Pig entity, PigRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
        reusedState.variant = entity.getVariant().value();
    }
}
