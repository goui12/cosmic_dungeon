package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureTarget extends RenderTarget {
    public TextureTarget(@Nullable String name, int width, int height, boolean useDepth) {
        this(name, width, height, useDepth, false);
    }
    public TextureTarget(@Nullable String name, int width, int height, boolean useDepth, boolean enableStencil) {
        super(name, useDepth, enableStencil);
        RenderSystem.assertOnRenderThread();
        this.resize(width, height);
    }
}
