package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiBannerResultRenderer extends PictureInPictureRenderer<GuiBannerResultRenderState> {
    private final MaterialSet materials;

    public GuiBannerResultRenderer(MultiBufferSource.BufferSource bufferSource, MaterialSet materials) {
        super(bufferSource);
        this.materials = materials;
    }

    @Override
    public Class<GuiBannerResultRenderState> getRenderStateClass() {
        return GuiBannerResultRenderState.class;
    }

    protected void renderToTexture(GuiBannerResultRenderState renderState, PoseStack poseStack) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        poseStack.translate(0.0F, 0.25F, 0.0F);
        FeatureRenderDispatcher featurerenderdispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeStorage submitnodestorage = featurerenderdispatcher.getSubmitNodeStorage();
        BannerRenderer.submitPatterns(
            this.materials,
            poseStack,
            submitnodestorage,
            15728880,
            OverlayTexture.NO_OVERLAY,
            renderState.flag(),
            0.0F,
            ModelBakery.BANNER_BASE,
            true,
            renderState.baseColor(),
            renderState.resultBannerPatterns(),
            false,
            null,
            0
        );
        featurerenderdispatcher.renderAllFeatures();
    }

    @Override
    protected String getTextureLabel() {
        return "banner result";
    }
}
