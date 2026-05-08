package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.FrogModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.FrogRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.frog.Frog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FrogRenderer extends MobRenderer<Frog, FrogRenderState, FrogModel> {
    public FrogRenderer(EntityRendererProvider.Context p_234619_) {
        super(p_234619_, new FrogModel(p_234619_.bakeLayer(ModelLayers.FROG)), 0.3F);
    }

    public ResourceLocation getTextureLocation(FrogRenderState p_365294_) {
        return p_365294_.texture;
    }

    public FrogRenderState createRenderState() {
        return new FrogRenderState();
    }

    public void extractRenderState(Frog p_364938_, FrogRenderState p_364657_, float p_361424_) {
        super.extractRenderState(p_364938_, p_364657_, p_361424_);
        p_364657_.isSwimming = p_364938_.isInWater();
        p_364657_.jumpAnimationState.copyFrom(p_364938_.jumpAnimationState);
        p_364657_.croakAnimationState.copyFrom(p_364938_.croakAnimationState);
        p_364657_.tongueAnimationState.copyFrom(p_364938_.tongueAnimationState);
        p_364657_.swimIdleAnimationState.copyFrom(p_364938_.swimIdleAnimationState);
        p_364657_.texture = p_364938_.getVariant().value().assetInfo().texturePath();
    }
}
