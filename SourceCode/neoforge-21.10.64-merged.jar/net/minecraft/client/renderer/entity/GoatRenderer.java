package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.GoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.goat.Goat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GoatRenderer extends AgeableMobRenderer<Goat, GoatRenderState, GoatModel> {
    private static final ResourceLocation GOAT_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/goat/goat.png");

    public GoatRenderer(EntityRendererProvider.Context p_174153_) {
        super(p_174153_, new GoatModel(p_174153_.bakeLayer(ModelLayers.GOAT)), new GoatModel(p_174153_.bakeLayer(ModelLayers.GOAT_BABY)), 0.7F);
    }

    public ResourceLocation getTextureLocation(GoatRenderState p_362356_) {
        return GOAT_LOCATION;
    }

    public GoatRenderState createRenderState() {
        return new GoatRenderState();
    }

    public void extractRenderState(Goat p_360853_, GoatRenderState p_361258_, float p_362364_) {
        super.extractRenderState(p_360853_, p_361258_, p_362364_);
        p_361258_.hasLeftHorn = p_360853_.hasLeftHorn();
        p_361258_.hasRightHorn = p_360853_.hasRightHorn();
        p_361258_.rammingXHeadRot = p_360853_.getRammingXHeadRot();
    }
}
