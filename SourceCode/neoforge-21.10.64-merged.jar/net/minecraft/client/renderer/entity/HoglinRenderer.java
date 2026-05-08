package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HoglinRenderer extends AbstractHoglinRenderer<Hoglin> {
    private static final ResourceLocation HOGLIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/hoglin/hoglin.png");

    public HoglinRenderer(EntityRendererProvider.Context p_174165_) {
        super(p_174165_, ModelLayers.HOGLIN, ModelLayers.HOGLIN_BABY, 0.7F);
    }

    public ResourceLocation getTextureLocation(HoglinRenderState p_364914_) {
        return HOGLIN_LOCATION;
    }

    public void extractRenderState(Hoglin p_364561_, HoglinRenderState p_362146_, float p_362417_) {
        super.extractRenderState(p_364561_, p_362146_, p_362417_);
        p_362146_.isConverting = p_364561_.isConverting();
    }

    protected boolean isShaking(HoglinRenderState p_363870_) {
        return super.isShaking(p_363870_) || p_363870_.isConverting;
    }
}
