package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SnowGolemHeadLayer;
import net.minecraft.client.renderer.entity.state.SnowGolemRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.SnowGolem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowGolemRenderer extends MobRenderer<SnowGolem, SnowGolemRenderState, SnowGolemModel> {
    private static final ResourceLocation SNOW_GOLEM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/snow_golem.png");

    public SnowGolemRenderer(EntityRendererProvider.Context p_174393_) {
        super(p_174393_, new SnowGolemModel(p_174393_.bakeLayer(ModelLayers.SNOW_GOLEM)), 0.5F);
        this.addLayer(new SnowGolemHeadLayer(this, p_174393_.getBlockRenderDispatcher()));
    }

    public ResourceLocation getTextureLocation(SnowGolemRenderState p_388676_) {
        return SNOW_GOLEM_LOCATION;
    }

    public SnowGolemRenderState createRenderState() {
        return new SnowGolemRenderState();
    }

    public void extractRenderState(SnowGolem p_364437_, SnowGolemRenderState p_388288_, float p_364064_) {
        super.extractRenderState(p_364437_, p_388288_, p_364064_);
        p_388288_.hasPumpkin = p_364437_.hasPumpkin();
    }
}
