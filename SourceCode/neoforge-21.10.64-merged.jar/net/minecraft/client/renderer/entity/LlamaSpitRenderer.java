package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.LlamaSpitModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LlamaSpitRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaSpitRenderer extends EntityRenderer<LlamaSpit, LlamaSpitRenderState> {
    private static final ResourceLocation LLAMA_SPIT_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/llama/spit.png");
    private final LlamaSpitModel model;

    public LlamaSpitRenderer(EntityRendererProvider.Context p_174296_) {
        super(p_174296_);
        this.model = new LlamaSpitModel(p_174296_.bakeLayer(ModelLayers.LLAMA_SPIT));
    }

    public void submit(LlamaSpitRenderState p_451269_, PoseStack p_435861_, SubmitNodeCollector p_433562_, CameraRenderState p_450953_) {
        p_435861_.pushPose();
        p_435861_.translate(0.0F, 0.15F, 0.0F);
        p_435861_.mulPose(Axis.YP.rotationDegrees(p_451269_.yRot - 90.0F));
        p_435861_.mulPose(Axis.ZP.rotationDegrees(p_451269_.xRot));
        p_433562_.submitModel(
            this.model,
            p_451269_,
            p_435861_,
            this.model.renderType(LLAMA_SPIT_LOCATION),
            p_451269_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_451269_.outlineColor,
            null
        );
        p_435861_.popPose();
        super.submit(p_451269_, p_435861_, p_433562_, p_450953_);
    }

    public LlamaSpitRenderState createRenderState() {
        return new LlamaSpitRenderState();
    }

    public void extractRenderState(LlamaSpit p_363068_, LlamaSpitRenderState p_363885_, float p_363897_) {
        super.extractRenderState(p_363068_, p_363885_, p_363897_);
        p_363885_.xRot = p_363068_.getXRot(p_363897_);
        p_363885_.yRot = p_363068_.getYRot(p_363897_);
    }
}
