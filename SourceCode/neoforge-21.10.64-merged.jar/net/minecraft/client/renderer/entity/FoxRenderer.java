package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.FoxModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.FoxHeldItemLayer;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Fox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FoxRenderer extends AgeableMobRenderer<Fox, FoxRenderState, FoxModel> {
    private static final ResourceLocation RED_FOX_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fox/fox.png");
    private static final ResourceLocation RED_FOX_SLEEP_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fox/fox_sleep.png");
    private static final ResourceLocation SNOW_FOX_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fox/snow_fox.png");
    private static final ResourceLocation SNOW_FOX_SLEEP_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/fox/snow_fox_sleep.png");

    public FoxRenderer(EntityRendererProvider.Context p_174127_) {
        super(p_174127_, new FoxModel(p_174127_.bakeLayer(ModelLayers.FOX)), new FoxModel(p_174127_.bakeLayer(ModelLayers.FOX_BABY)), 0.4F);
        this.addLayer(new FoxHeldItemLayer(this));
    }

    protected void setupRotations(FoxRenderState p_363483_, PoseStack p_114731_, float p_114732_, float p_114733_) {
        super.setupRotations(p_363483_, p_114731_, p_114732_, p_114733_);
        if (p_363483_.isPouncing || p_363483_.isFaceplanted) {
            p_114731_.mulPose(Axis.XP.rotationDegrees(-p_363483_.xRot));
        }
    }

    public ResourceLocation getTextureLocation(FoxRenderState p_365240_) {
        if (p_365240_.variant == Fox.Variant.RED) {
            return p_365240_.isSleeping ? RED_FOX_SLEEP_TEXTURE : RED_FOX_TEXTURE;
        } else {
            return p_365240_.isSleeping ? SNOW_FOX_SLEEP_TEXTURE : SNOW_FOX_TEXTURE;
        }
    }

    public FoxRenderState createRenderState() {
        return new FoxRenderState();
    }

    public void extractRenderState(Fox p_364137_, FoxRenderState p_365146_, float p_361192_) {
        super.extractRenderState(p_364137_, p_365146_, p_361192_);
        HoldingEntityRenderState.extractHoldingEntityRenderState(p_364137_, p_365146_, this.itemModelResolver);
        p_365146_.headRollAngle = p_364137_.getHeadRollAngle(p_361192_);
        p_365146_.isCrouching = p_364137_.isCrouching();
        p_365146_.crouchAmount = p_364137_.getCrouchAmount(p_361192_);
        p_365146_.isSleeping = p_364137_.isSleeping();
        p_365146_.isSitting = p_364137_.isSitting();
        p_365146_.isFaceplanted = p_364137_.isFaceplanted();
        p_365146_.isPouncing = p_364137_.isPouncing();
        p_365146_.variant = p_364137_.getVariant();
    }
}
