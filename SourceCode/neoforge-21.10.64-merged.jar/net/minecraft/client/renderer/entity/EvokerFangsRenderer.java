package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EvokerFangsRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EvokerFangsRenderer extends EntityRenderer<EvokerFangs, EvokerFangsRenderState> {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/illager/evoker_fangs.png");
    private final EvokerFangsModel model;

    public EvokerFangsRenderer(EntityRendererProvider.Context p_174100_) {
        super(p_174100_);
        this.model = new EvokerFangsModel(p_174100_.bakeLayer(ModelLayers.EVOKER_FANGS));
    }

    public void submit(EvokerFangsRenderState p_434129_, PoseStack p_433617_, SubmitNodeCollector p_434083_, CameraRenderState p_450960_) {
        float f = p_434129_.biteProgress;
        if (f != 0.0F) {
            p_433617_.pushPose();
            p_433617_.mulPose(Axis.YP.rotationDegrees(90.0F - p_434129_.yRot));
            p_433617_.scale(-1.0F, -1.0F, 1.0F);
            p_433617_.translate(0.0F, -1.501F, 0.0F);
            p_434083_.submitModel(
                this.model,
                p_434129_,
                p_433617_,
                this.model.renderType(TEXTURE_LOCATION),
                p_434129_.lightCoords,
                OverlayTexture.NO_OVERLAY,
                p_434129_.outlineColor,
                null
            );
            p_433617_.popPose();
            super.submit(p_434129_, p_433617_, p_434083_, p_450960_);
        }
    }

    public EvokerFangsRenderState createRenderState() {
        return new EvokerFangsRenderState();
    }

    public void extractRenderState(EvokerFangs p_360791_, EvokerFangsRenderState p_362754_, float p_363764_) {
        super.extractRenderState(p_360791_, p_362754_, p_363764_);
        p_362754_.yRot = p_360791_.getYRot();
        p_362754_.biteProgress = p_360791_.getAnimationProgress(p_363764_);
    }
}
