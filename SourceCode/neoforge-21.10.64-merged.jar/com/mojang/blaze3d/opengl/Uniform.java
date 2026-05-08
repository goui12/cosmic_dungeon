package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.TextureFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public sealed interface Uniform extends AutoCloseable permits Uniform.Ubo, Uniform.Utb, Uniform.Sampler {
    @Override
    default void close() {
    }

    @OnlyIn(Dist.CLIENT)
    public record Sampler(int location, int samplerIndex) implements Uniform {
    }

    @OnlyIn(Dist.CLIENT)
    public record Ubo(int blockBinding) implements Uniform {
    }

    @OnlyIn(Dist.CLIENT)
    public record Utb(int location, int samplerIndex, TextureFormat format, int texture) implements Uniform {
        public Utb(int p_418342_, int p_418376_, TextureFormat p_418362_) {
            this(p_418342_, p_418376_, p_418362_, GlStateManager._genTexture());
        }

        @Override
        public void close() {
            GlStateManager._deleteTexture(this.texture);
        }
    }
}
