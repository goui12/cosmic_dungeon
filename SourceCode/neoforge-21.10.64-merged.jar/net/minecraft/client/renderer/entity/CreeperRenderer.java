package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreeperRenderer extends MobRenderer<Creeper, CreeperRenderState, CreeperModel> {
    private static final ResourceLocation CREEPER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png");

    public CreeperRenderer(EntityRendererProvider.Context p_173958_) {
        super(p_173958_, new CreeperModel(p_173958_.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        this.addLayer(new CreeperPowerLayer(this, p_173958_.getModelSet()));
    }

    protected void scale(CreeperRenderState p_362568_, PoseStack p_114047_) {
        float f = p_362568_.swelling;
        float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        f *= f;
        f *= f;
        float f2 = (1.0F + f * 0.4F) * f1;
        float f3 = (1.0F + f * 0.1F) / f1;
        p_114047_.scale(f2, f3, f2);
    }

    protected float getWhiteOverlayProgress(CreeperRenderState p_360678_) {
        float f = p_360678_.swelling;
        return (int)(f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
    }

    public ResourceLocation getTextureLocation(CreeperRenderState p_363813_) {
        return CREEPER_LOCATION;
    }

    public CreeperRenderState createRenderState() {
        return new CreeperRenderState();
    }

    public void extractRenderState(Creeper p_364394_, CreeperRenderState p_361451_, float p_364659_) {
        super.extractRenderState(p_364394_, p_361451_, p_364659_);
        p_361451_.swelling = p_364394_.getSwelling(p_364659_);
        p_361451_.isPowered = p_364394_.isPowered();
    }
}
