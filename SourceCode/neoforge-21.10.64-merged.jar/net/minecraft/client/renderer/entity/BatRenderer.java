package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BatRenderer extends MobRenderer<Bat, BatRenderState, BatModel> {
    private static final ResourceLocation BAT_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/bat.png");

    public BatRenderer(EntityRendererProvider.Context p_173929_) {
        super(p_173929_, new BatModel(p_173929_.bakeLayer(ModelLayers.BAT)), 0.25F);
    }

    public ResourceLocation getTextureLocation(BatRenderState p_360545_) {
        return BAT_LOCATION;
    }

    public BatRenderState createRenderState() {
        return new BatRenderState();
    }

    public void extractRenderState(Bat p_365331_, BatRenderState p_361820_, float p_362099_) {
        super.extractRenderState(p_365331_, p_361820_, p_362099_);
        p_361820_.isResting = p_365331_.isResting();
        p_361820_.flyAnimationState.copyFrom(p_365331_.flyAnimationState);
        p_361820_.restAnimationState.copyFrom(p_365331_.restAnimationState);
    }
}
