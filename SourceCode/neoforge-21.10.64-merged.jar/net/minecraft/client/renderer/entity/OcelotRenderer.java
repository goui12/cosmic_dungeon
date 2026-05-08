package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Ocelot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OcelotRenderer extends AgeableMobRenderer<Ocelot, FelineRenderState, OcelotModel> {
    private static final ResourceLocation CAT_OCELOT_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/cat/ocelot.png");

    public OcelotRenderer(EntityRendererProvider.Context p_174330_) {
        super(p_174330_, new OcelotModel(p_174330_.bakeLayer(ModelLayers.OCELOT)), new OcelotModel(p_174330_.bakeLayer(ModelLayers.OCELOT_BABY)), 0.4F);
    }

    public ResourceLocation getTextureLocation(FelineRenderState p_361442_) {
        return CAT_OCELOT_LOCATION;
    }

    public FelineRenderState createRenderState() {
        return new FelineRenderState();
    }

    public void extractRenderState(Ocelot p_363307_, FelineRenderState p_364547_, float p_363203_) {
        super.extractRenderState(p_363307_, p_364547_, p_363203_);
        p_364547_.isCrouching = p_363307_.isCrouching();
        p_364547_.isSprinting = p_363307_.isSprinting();
    }
}
