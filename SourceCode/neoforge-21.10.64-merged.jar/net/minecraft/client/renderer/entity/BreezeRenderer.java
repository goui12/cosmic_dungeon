package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.BreezeEyesLayer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeRenderer extends MobRenderer<Breeze, BreezeRenderState, BreezeModel> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/breeze/breeze.png");

    public BreezeRenderer(EntityRendererProvider.Context p_312679_) {
        super(p_312679_, new BreezeModel(p_312679_.bakeLayer(ModelLayers.BREEZE)), 0.5F);
        this.addLayer(new BreezeWindLayer(this, p_312679_.getModelSet()));
        this.addLayer(new BreezeEyesLayer(this, p_312679_.getModelSet()));
    }

    public ResourceLocation getTextureLocation(BreezeRenderState p_365503_) {
        return TEXTURE_LOCATION;
    }

    public BreezeRenderState createRenderState() {
        return new BreezeRenderState();
    }

    public void extractRenderState(Breeze p_362109_, BreezeRenderState p_361497_, float p_365263_) {
        super.extractRenderState(p_362109_, p_361497_, p_365263_);
        p_361497_.idle.copyFrom(p_362109_.idle);
        p_361497_.shoot.copyFrom(p_362109_.shoot);
        p_361497_.slide.copyFrom(p_362109_.slide);
        p_361497_.slideBack.copyFrom(p_362109_.slideBack);
        p_361497_.inhale.copyFrom(p_362109_.inhale);
        p_361497_.longJump.copyFrom(p_362109_.longJump);
    }
}
