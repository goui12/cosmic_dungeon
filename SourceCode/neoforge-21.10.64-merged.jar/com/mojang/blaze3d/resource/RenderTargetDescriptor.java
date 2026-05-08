package com.mojang.blaze3d.resource;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record RenderTargetDescriptor(int width, int height, boolean useDepth, int clearColor, boolean useStencil) implements ResourceDescriptor<RenderTarget> {
    public RenderTargetDescriptor(int width, int height, boolean useDepth, int clearColor) {
        this(width, height, useDepth, clearColor, false);
    }

    public RenderTarget allocate() {
        return new TextureTarget(null, this.width, this.height, this.useDepth, this.useStencil);
    }

    public void prepare(RenderTarget p_393765_) {
        if (this.useDepth) {
            RenderSystem.getDevice()
                .createCommandEncoder()
                .clearColorAndDepthTextures(p_393765_.getColorTexture(), this.clearColor, p_393765_.getDepthTexture(), 1.0);
        } else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(p_393765_.getColorTexture(), this.clearColor);
        }
        if (this.useStencil) {
            RenderSystem.getDevice().createCommandEncoder().clearStencilTexture(p_393765_.getDepthTexture(), 0);
        }
    }

    public void free(RenderTarget p_363223_) {
        p_363223_.destroyBuffers();
    }

    @Override
    public boolean canUsePhysicalResource(ResourceDescriptor<?> p_394237_) {
        return !(p_394237_ instanceof RenderTargetDescriptor rendertargetdescriptor)
            ? false
            : this.width == rendertargetdescriptor.width && this.height == rendertargetdescriptor.height && this.useDepth == rendertargetdescriptor.useDepth && this.useStencil == rendertargetdescriptor.useStencil;
    }
}
