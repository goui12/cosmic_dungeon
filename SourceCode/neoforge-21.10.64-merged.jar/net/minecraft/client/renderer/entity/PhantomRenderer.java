package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PhantomRenderer extends MobRenderer<Phantom, PhantomRenderState, PhantomModel> {
    private static final ResourceLocation PHANTOM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/phantom.png");

    public PhantomRenderer(EntityRendererProvider.Context p_174338_) {
        super(p_174338_, new PhantomModel(p_174338_.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
        this.addLayer(new PhantomEyesLayer(this));
    }

    public ResourceLocation getTextureLocation(PhantomRenderState p_363735_) {
        return PHANTOM_LOCATION;
    }

    public PhantomRenderState createRenderState() {
        return new PhantomRenderState();
    }

    public void extractRenderState(Phantom p_362124_, PhantomRenderState p_363079_, float p_363625_) {
        super.extractRenderState(p_362124_, p_363079_, p_363625_);
        p_363079_.flapTime = p_362124_.getUniqueFlapTickOffset() + p_363079_.ageInTicks;
        p_363079_.size = p_362124_.getPhantomSize();
    }

    protected void scale(PhantomRenderState p_364542_, PoseStack p_115682_) {
        float f = 1.0F + 0.15F * p_364542_.size;
        p_115682_.scale(f, f, f);
        p_115682_.translate(0.0F, 1.3125F, 0.1875F);
    }

    protected void setupRotations(PhantomRenderState p_363004_, PoseStack p_115686_, float p_115687_, float p_115688_) {
        super.setupRotations(p_363004_, p_115686_, p_115687_, p_115688_);
        p_115686_.mulPose(Axis.XP.rotationDegrees(p_363004_.xRot));
    }
}
