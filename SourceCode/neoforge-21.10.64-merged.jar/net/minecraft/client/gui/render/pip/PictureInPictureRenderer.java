package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class PictureInPictureRenderer<T extends PictureInPictureRenderState> implements AutoCloseable {
    protected final MultiBufferSource.BufferSource bufferSource;
    @Nullable
    private GpuTexture texture;
    @Nullable
    private GpuTextureView textureView;
    @Nullable
    private GpuTexture depthTexture;
    @Nullable
    private GpuTextureView depthTextureView;
    private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer(
        "PIP - " + this.getClass().getSimpleName(), -1000.0F, 1000.0F, true
    );

    protected PictureInPictureRenderer(MultiBufferSource.BufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    public void prepare(T renderState, GuiRenderState guiRenderState, int guiScale) {
        int i = (renderState.x1() - renderState.x0()) * guiScale;
        int j = (renderState.y1() - renderState.y0()) * guiScale;
        boolean flag = this.texture == null || this.texture.getWidth(0) != i || this.texture.getHeight(0) != j;
        if (!flag && this.textureIsReadyToBlit(renderState)) {
            this.blitTexture(renderState, guiRenderState);
        } else {
            this.prepareTexturesAndProjection(flag, i, j);
            RenderSystem.outputColorTextureOverride = this.textureView;
            RenderSystem.outputDepthTextureOverride = this.depthTextureView;
            PoseStack posestack = new PoseStack();
            posestack.translate(i / 2.0F, this.getTranslateY(j, guiScale), 0.0F);
            float f = guiScale * renderState.scale();
            posestack.scale(f, f, -f);
            this.renderToTexture(renderState, posestack);
            this.bufferSource.endBatch();
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            this.blitTexture(renderState, guiRenderState);
        }
    }

    protected void blitTexture(T renderState, GuiRenderState guiRenderState) {
        guiRenderState.submitBlitToCurrentLayer(
            new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(this.textureView),
                renderState.pose(),
                renderState.x0(),
                renderState.y0(),
                renderState.x1(),
                renderState.y1(),
                0.0F,
                1.0F,
                1.0F,
                0.0F,
                -1,
                renderState.scissorArea(),
                null
            )
        );
    }

    private void prepareTexturesAndProjection(boolean resetTexture, int width, int height) {
        if (this.texture != null && resetTexture) {
            this.texture.close();
            this.texture = null;
            this.textureView.close();
            this.textureView = null;
            this.depthTexture.close();
            this.depthTexture = null;
            this.depthTextureView.close();
            this.depthTextureView = null;
        }

        GpuDevice gpudevice = RenderSystem.getDevice();
        if (this.texture == null) {
            this.texture = gpudevice.createTexture(() -> "UI " + this.getTextureLabel() + " texture", 12, TextureFormat.RGBA8, width, height, 1, 1);
            this.texture.setTextureFilter(FilterMode.NEAREST, false);
            this.textureView = gpudevice.createTextureView(this.texture);
            // Neo: copy stencil setting from main target
            TextureFormat depthFormat = net.minecraft.client.Minecraft.getInstance().getMainRenderTarget().getDepthTexture().getFormat();
            this.depthTexture = gpudevice.createTexture(
                () -> "UI " + this.getTextureLabel() + " depth texture", 8, depthFormat, width, height, 1, 1
            );
            this.depthTextureView = gpudevice.createTextureView(this.depthTexture);
        }

        gpudevice.createCommandEncoder().clearColorAndDepthTextures(this.texture, 0, this.depthTexture, 1.0);
        RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(width, height), ProjectionType.ORTHOGRAPHIC);
    }

    protected boolean textureIsReadyToBlit(T renderState) {
        return false;
    }

    protected float getTranslateY(int height, int guiScale) {
        return height;
    }

    @Override
    public void close() {
        if (this.texture != null) {
            this.texture.close();
        }

        if (this.textureView != null) {
            this.textureView.close();
        }

        if (this.depthTexture != null) {
            this.depthTexture.close();
        }

        if (this.depthTextureView != null) {
            this.depthTextureView.close();
        }

        this.projectionMatrixBuffer.close();
    }

    public abstract Class<T> getRenderStateClass();

    protected abstract void renderToTexture(T renderState, PoseStack poseStack);

    protected abstract String getTextureLabel();

    /**
     * Neo: This is used to check if this renderer can be reused for a given state, texture width and texture height on
     * a subsequent frame. In Vanilla, a renderer would be used for multiple different states even within the same frame,
     * leading to crashes and the last state being used for all blits of that renderer in that frame.
     */
    public boolean canBeReusedFor(T state, int textureWidth, int textureHeight) {
        return texture == null || (texture.getWidth(0) == textureWidth && texture.getHeight(0) == textureHeight);
    }
}
