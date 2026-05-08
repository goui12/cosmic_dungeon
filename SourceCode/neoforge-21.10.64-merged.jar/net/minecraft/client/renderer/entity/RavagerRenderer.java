package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.RavagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.RavagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Ravager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RavagerRenderer extends MobRenderer<Ravager, RavagerRenderState, RavagerModel> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/illager/ravager.png");

    public RavagerRenderer(EntityRendererProvider.Context p_174362_) {
        super(p_174362_, new RavagerModel(p_174362_.bakeLayer(ModelLayers.RAVAGER)), 1.1F);
    }

    public ResourceLocation getTextureLocation(RavagerRenderState p_363518_) {
        return TEXTURE_LOCATION;
    }

    public RavagerRenderState createRenderState() {
        return new RavagerRenderState();
    }

    public void extractRenderState(Ravager p_361648_, RavagerRenderState p_360518_, float p_364693_) {
        super.extractRenderState(p_361648_, p_360518_, p_364693_);
        p_360518_.stunnedTicksRemaining = p_361648_.getStunnedTick() > 0.0F ? p_361648_.getStunnedTick() - p_364693_ : 0.0F;
        p_360518_.attackTicksRemaining = p_361648_.getAttackTick() > 0.0F ? p_361648_.getAttackTick() - p_364693_ : 0.0F;
        if (p_361648_.getRoarTick() > 0) {
            p_360518_.roarAnimation = (20 - p_361648_.getRoarTick() + p_364693_) / 20.0F;
        } else {
            p_360518_.roarAnimation = 0.0F;
        }
    }
}
