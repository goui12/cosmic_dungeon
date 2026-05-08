package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatRenderer extends AgeableMobRenderer<Cat, CatRenderState, CatModel> {
    public CatRenderer(EntityRendererProvider.Context p_173943_) {
        super(p_173943_, new CatModel(p_173943_.bakeLayer(ModelLayers.CAT)), new CatModel(p_173943_.bakeLayer(ModelLayers.CAT_BABY)), 0.4F);
        this.addLayer(new CatCollarLayer(this, p_173943_.getModelSet()));
    }

    public ResourceLocation getTextureLocation(CatRenderState p_362035_) {
        return p_362035_.texture;
    }

    public CatRenderState createRenderState() {
        return new CatRenderState();
    }

    public void extractRenderState(Cat p_361533_, CatRenderState p_362709_, float p_362752_) {
        super.extractRenderState(p_361533_, p_362709_, p_362752_);
        p_362709_.texture = p_361533_.getVariant().value().assetInfo().texturePath();
        p_362709_.isCrouching = p_361533_.isCrouching();
        p_362709_.isSprinting = p_361533_.isSprinting();
        p_362709_.isSitting = p_361533_.isInSittingPose();
        p_362709_.lieDownAmount = p_361533_.getLieDownAmount(p_362752_);
        p_362709_.lieDownAmountTail = p_361533_.getLieDownAmountTail(p_362752_);
        p_362709_.relaxStateOneAmount = p_361533_.getRelaxStateOneAmount(p_362752_);
        p_362709_.isLyingOnTopOfSleepingPlayer = p_361533_.isLyingOnTopOfSleepingPlayer();
        p_362709_.collarColor = p_361533_.isTame() ? p_361533_.getCollarColor() : null;
    }

    protected void setupRotations(CatRenderState p_363397_, PoseStack p_113945_, float p_113946_, float p_113947_) {
        super.setupRotations(p_363397_, p_113945_, p_113946_, p_113947_);
        float f = p_363397_.lieDownAmount;
        if (f > 0.0F) {
            p_113945_.translate(0.4F * f, 0.15F * f, 0.1F * f);
            p_113945_.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp(f, 0.0F, 90.0F)));
            if (p_363397_.isLyingOnTopOfSleepingPlayer) {
                p_113945_.translate(0.15F * f, 0.0F, 0.0F);
            }
        }
    }
}
