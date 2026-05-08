package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.StriderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.StriderRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Strider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StriderRenderer extends AgeableMobRenderer<Strider, StriderRenderState, StriderModel> {
    private static final ResourceLocation STRIDER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/strider/strider.png");
    private static final ResourceLocation COLD_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/strider/strider_cold.png");
    private static final float SHADOW_RADIUS = 0.5F;

    public StriderRenderer(EntityRendererProvider.Context p_174411_) {
        super(p_174411_, new StriderModel(p_174411_.bakeLayer(ModelLayers.STRIDER)), new StriderModel(p_174411_.bakeLayer(ModelLayers.STRIDER_BABY)), 0.5F);
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_174411_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.STRIDER_SADDLE,
                p_397713_ -> p_397713_.saddle,
                new StriderModel(p_174411_.bakeLayer(ModelLayers.STRIDER_SADDLE)),
                new StriderModel(p_174411_.bakeLayer(ModelLayers.STRIDER_BABY_SADDLE))
            )
        );
    }

    public ResourceLocation getTextureLocation(StriderRenderState p_363553_) {
        return p_363553_.isSuffocating ? COLD_LOCATION : STRIDER_LOCATION;
    }

    protected float getShadowRadius(StriderRenderState p_365429_) {
        float f = super.getShadowRadius(p_365429_);
        return p_365429_.isBaby ? f * 0.5F : f;
    }

    public StriderRenderState createRenderState() {
        return new StriderRenderState();
    }

    public void extractRenderState(Strider p_362147_, StriderRenderState p_360861_, float p_363151_) {
        super.extractRenderState(p_362147_, p_360861_, p_363151_);
        p_360861_.saddle = p_362147_.getItemBySlot(EquipmentSlot.SADDLE).copy();
        p_360861_.isSuffocating = p_362147_.isSuffocating();
        p_360861_.isRidden = p_362147_.isVehicle();
    }

    protected boolean isShaking(StriderRenderState p_360940_) {
        return super.isShaking(p_360940_) || p_360940_.isSuffocating;
    }
}
