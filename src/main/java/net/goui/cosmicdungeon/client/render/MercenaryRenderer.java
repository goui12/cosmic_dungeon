package net.goui.cosmicdungeon.client.render;
import net.goui.cosmicdungeon.mercenary.MercenaryEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
public final class MercenaryRenderer extends HumanoidMobRenderer<MercenaryEntity,HumanoidRenderState,HumanoidModel<HumanoidRenderState>> {
    public MercenaryRenderer(EntityRendererProvider.Context context){
        super(context,new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),.5F);
        addLayer(new HumanoidArmorLayer<>(this,
            ModelLayers.PLAYER_ARMOR.map(layer->new HumanoidModel<HumanoidRenderState>(context.bakeLayer(layer))),
            context.getEquipmentRenderer()));
    }
    @Override public boolean shouldRender(MercenaryEntity entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z){
        return !entity.dormant()&&super.shouldRender(entity,frustum,x,y,z);
    }
    @Override public HumanoidRenderState createRenderState(){return new HumanoidRenderState();}
    @Override public ResourceLocation getTextureLocation(HumanoidRenderState state){
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }
}
