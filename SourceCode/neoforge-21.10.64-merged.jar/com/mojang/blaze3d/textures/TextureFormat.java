package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
@net.neoforged.neoforge.internal.NonExhaustiveEnum(reason = "Additional texture formats may be added")
public enum TextureFormat {
    RGBA8(4),
    RED8(1),
    RED8I(1),
    DEPTH32(4),
    // Neo: Add depth+stencil formats
    DEPTH24_STENCIL8(4),
    DEPTH32_STENCIL8(8);

    private final int pixelSize;

    private TextureFormat(int p_pixelSize) {
        this.pixelSize = p_pixelSize;
    }

    public int pixelSize() {
        return this.pixelSize;
    }

    public boolean hasColorAspect() {
        return this == RGBA8 || this == RED8;
    }

    public boolean hasDepthAspect() {
        return this == DEPTH32 || this == DEPTH24_STENCIL8 || this == DEPTH32_STENCIL8;
    }

    public boolean hasStencilAspect() {
        return this == DEPTH24_STENCIL8 || this == DEPTH32_STENCIL8;
    }
}
