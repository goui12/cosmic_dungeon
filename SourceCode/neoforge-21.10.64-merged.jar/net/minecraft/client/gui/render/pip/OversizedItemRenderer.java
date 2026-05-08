package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OversizedItemRenderer extends PictureInPictureRenderer<OversizedItemRenderState> {
    private boolean usedOnThisFrame;
    @Nullable
    private Object modelOnTextureIdentity;

    public OversizedItemRenderer(MultiBufferSource.BufferSource p_428518_) {
        super(p_428518_);
    }

    public boolean usedOnThisFrame() {
        return this.usedOnThisFrame;
    }

    public void resetUsedOnThisFrame() {
        this.usedOnThisFrame = false;
    }

    public void invalidateTexture() {
        this.modelOnTextureIdentity = null;
    }

    @Override
    public Class<OversizedItemRenderState> getRenderStateClass() {
        return OversizedItemRenderState.class;
    }

    protected void renderToTexture(OversizedItemRenderState p_428332_, PoseStack p_428499_) {
        p_428499_.scale(1.0F, -1.0F, -1.0F);
        GuiItemRenderState guiitemrenderstate = p_428332_.guiItemRenderState();
        ScreenRectangle screenrectangle = guiitemrenderstate.oversizedItemBounds();
        Objects.requireNonNull(screenrectangle);
        float f = (screenrectangle.left() + screenrectangle.right()) / 2.0F;
        float f1 = (screenrectangle.top() + screenrectangle.bottom()) / 2.0F;
        float f2 = guiitemrenderstate.x() + 8.0F;
        float f3 = guiitemrenderstate.y() + 8.0F;
        p_428499_.translate((f2 - f) / 16.0F, (f1 - f3) / 16.0F, 0.0F);
        TrackingItemStackRenderState trackingitemstackrenderstate = guiitemrenderstate.itemStackRenderState();
        boolean flag = !trackingitemstackrenderstate.usesBlockLight();
        if (flag) {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        } else {
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        }

        FeatureRenderDispatcher featurerenderdispatcher = Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeStorage submitnodestorage = featurerenderdispatcher.getSubmitNodeStorage();
        trackingitemstackrenderstate.submit(p_428499_, submitnodestorage, 15728880, OverlayTexture.NO_OVERLAY, 0);
        featurerenderdispatcher.renderAllFeatures();
        this.modelOnTextureIdentity = trackingitemstackrenderstate.getModelIdentity();
    }

    public void blitTexture(OversizedItemRenderState p_428390_, GuiRenderState p_428372_) {
        super.blitTexture(p_428390_, p_428372_);
        this.usedOnThisFrame = true;
    }

    public boolean textureIsReadyToBlit(OversizedItemRenderState p_428573_) {
        TrackingItemStackRenderState trackingitemstackrenderstate = p_428573_.guiItemRenderState().itemStackRenderState();
        return !trackingitemstackrenderstate.isAnimated() && trackingitemstackrenderstate.getModelIdentity().equals(this.modelOnTextureIdentity);
    }

    @Override
    protected float getTranslateY(int p_428340_, int p_428276_) {
        return p_428340_ / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "oversized_item";
    }
}
