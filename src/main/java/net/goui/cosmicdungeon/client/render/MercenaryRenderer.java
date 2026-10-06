package net.goui.cosmicdungeon.client.render;
import net.goui.cosmicdungeon.mercenary.MercenaryEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
public final class MercenaryRenderer extends HumanoidMobRenderer<MercenaryEntity,MercenaryRenderState,HumanoidModel<MercenaryRenderState>> {
    public MercenaryRenderer(EntityRendererProvider.Context context){
        super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),.5F);
        addLayer(new HumanoidArmorLayer<>(this,
            ModelLayers.PLAYER_ARMOR.map(layer->new HumanoidModel<MercenaryRenderState>(context.bakeLayer(layer))),
            context.getEquipmentRenderer()));
    }
    @Override public boolean shouldRender(MercenaryEntity entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){
        return !entity.dormant()&&super.shouldRender(entity,frustum,x,y,z);
    }
    @Override public MercenaryRenderState createRenderState(){return new MercenaryRenderState();}
    @Override public void extractRenderState(MercenaryEntity entity,MercenaryRenderState state,float partialTick){
        super.extractRenderState(entity,state,partialTick);
        state.captureIdentity(entity.getUUID());
    }
    @Override public ResourceLocation getTextureLocation(MercenaryRenderState state){
        return state.texture();
    }
}
