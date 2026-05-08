package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BakedSheetGlyph implements BakedGlyph, EffectGlyph {
    public static final float Z_FIGHTER = 0.001F;
    private final GlyphInfo info;
    final GlyphRenderTypes renderTypes;
    final GpuTextureView textureView;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float left;
    private final float right;
    private final float up;
    private final float down;

    public BakedSheetGlyph(
        GlyphInfo info,
        GlyphRenderTypes renderTypes,
        GpuTextureView textureView,
        float u0,
        float u1,
        float v0,
        float v1,
        float left,
        float right,
        float up,
        float down
    ) {
        this.info = info;
        this.renderTypes = renderTypes;
        this.textureView = textureView;
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;
        this.left = left;
        this.right = right;
        this.up = up;
        this.down = down;
    }

    float left(BakedSheetGlyph.GlyphInstance glyph) {
        return glyph.x
            + this.left
            + (glyph.style.isItalic() ? Math.min(this.shearTop(), this.shearBottom()) : 0.0F)
            - extraThickness(glyph.style.isBold());
    }

    float top(BakedSheetGlyph.GlyphInstance glyph) {
        return glyph.y + this.up - extraThickness(glyph.style.isBold());
    }

    float right(BakedSheetGlyph.GlyphInstance glyph) {
        return glyph.x
            + this.right
            + (glyph.hasShadow() ? glyph.shadowOffset : 0.0F)
            + (glyph.style.isItalic() ? Math.max(this.shearTop(), this.shearBottom()) : 0.0F)
            + extraThickness(glyph.style.isBold());
    }

    float bottom(BakedSheetGlyph.GlyphInstance glyph) {
        return glyph.y + this.down + (glyph.hasShadow() ? glyph.shadowOffset : 0.0F) + extraThickness(glyph.style.isBold());
    }

    void renderChar(BakedSheetGlyph.GlyphInstance glyph, Matrix4f pose, VertexConsumer consumer, int packedLight, boolean noDepth) {
        Style style = glyph.style();
        boolean flag = style.isItalic();
        float f = glyph.x();
        float f1 = glyph.y();
        int i = glyph.color();
        boolean flag1 = style.isBold();
        float f3 = noDepth ? 0.0F : 0.001F;
        float f2;
        if (glyph.hasShadow()) {
            int j = glyph.shadowColor();
            this.render(flag, f + glyph.shadowOffset(), f1 + glyph.shadowOffset(), 0.0F, pose, consumer, j, flag1, packedLight);
            if (flag1) {
                this.render(
                    flag, f + glyph.boldOffset() + glyph.shadowOffset(), f1 + glyph.shadowOffset(), f3, pose, consumer, j, true, packedLight
                );
            }

            f2 = noDepth ? 0.0F : 0.03F;
        } else {
            f2 = 0.0F;
        }

        this.render(flag, f, f1, f2, pose, consumer, i, flag1, packedLight);
        if (flag1) {
            this.render(flag, f + glyph.boldOffset(), f1, f2 + f3, pose, consumer, i, true, packedLight);
        }
    }

    private void render(
        boolean italic,
        float x,
        float y,
        float z,
        Matrix4f pose,
        VertexConsumer consumer,
        int color,
        boolean bold,
        int packedLight
    ) {
        float f = x + this.left;
        float f1 = x + this.right;
        float f2 = y + this.up;
        float f3 = y + this.down;
        float f4 = italic ? this.shearTop() : 0.0F;
        float f5 = italic ? this.shearBottom() : 0.0F;
        float f6 = extraThickness(bold);
        consumer.addVertex(pose, f + f4 - f6, f2 - f6, z).setColor(color).setUv(this.u0, this.v0).setLight(packedLight);
        consumer.addVertex(pose, f + f5 - f6, f3 + f6, z).setColor(color).setUv(this.u0, this.v1).setLight(packedLight);
        consumer.addVertex(pose, f1 + f5 + f6, f3 + f6, z).setColor(color).setUv(this.u1, this.v1).setLight(packedLight);
        consumer.addVertex(pose, f1 + f4 + f6, f2 - f6, z).setColor(color).setUv(this.u1, this.v0).setLight(packedLight);
    }

    private static float extraThickness(boolean bold) {
        return bold ? 0.1F : 0.0F;
    }

    private float shearBottom() {
        return 1.0F - 0.25F * this.down;
    }

    private float shearTop() {
        return 1.0F - 0.25F * this.up;
    }

    void renderEffect(BakedSheetGlyph.EffectInstance effect, Matrix4f pose, VertexConsumer consumer, int packedLight, boolean noDepth) {
        float f = noDepth ? 0.0F : effect.depth;
        if (effect.hasShadow()) {
            this.buildEffect(effect, effect.shadowOffset(), f, effect.shadowColor(), consumer, packedLight, pose);
            f += noDepth ? 0.0F : 0.03F;
        }

        this.buildEffect(effect, 0.0F, f, effect.color, consumer, packedLight, pose);
    }

    private void buildEffect(
        BakedSheetGlyph.EffectInstance effect, float shadowOffset, float depthOffset, int shadowColor, VertexConsumer consumer, int packedLight, Matrix4f pose
    ) {
        consumer.addVertex(pose, effect.x0 + shadowOffset, effect.y1 + shadowOffset, depthOffset)
            .setColor(shadowColor)
            .setUv(this.u0, this.v0)
            .setLight(packedLight);
        consumer.addVertex(pose, effect.x1 + shadowOffset, effect.y1 + shadowOffset, depthOffset)
            .setColor(shadowColor)
            .setUv(this.u0, this.v1)
            .setLight(packedLight);
        consumer.addVertex(pose, effect.x1 + shadowOffset, effect.y0 + shadowOffset, depthOffset)
            .setColor(shadowColor)
            .setUv(this.u1, this.v1)
            .setLight(packedLight);
        consumer.addVertex(pose, effect.x0 + shadowOffset, effect.y0 + shadowOffset, depthOffset)
            .setColor(shadowColor)
            .setUv(this.u1, this.v0)
            .setLight(packedLight);
    }

    @Override
    public GlyphInfo info() {
        return this.info;
    }

    @Override
    public TextRenderable createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
        return new BakedSheetGlyph.GlyphInstance(x, y, color, shadowColor, this, style, boldOffset, shadowOffset);
    }

    @Override
    public TextRenderable createEffect(
        float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset
    ) {
        return new BakedSheetGlyph.EffectInstance(this, x0, y0, x1, y1, depth, color, shadowColor, shadowOffset);
    }

    @OnlyIn(Dist.CLIENT)
    record EffectInstance(BakedSheetGlyph glyph, float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset)
        implements TextRenderable {
        @Override
        public float left() {
            return this.x0;
        }

        @Override
        public float top() {
            return this.y0;
        }

        @Override
        public float right() {
            return this.x1 + (this.hasShadow() ? this.shadowOffset : 0.0F);
        }

        @Override
        public float bottom() {
            return this.y1 + (this.hasShadow() ? this.shadowOffset : 0.0F);
        }

        boolean hasShadow() {
            return this.shadowColor() != 0;
        }

        @Override
        public void render(Matrix4f p_440671_, VertexConsumer p_439075_, int p_439912_, boolean p_439548_) {
            this.glyph.renderEffect(this, p_440671_, p_439075_, p_439912_, false);
        }

        /**
         * Neo: returns the {@link RenderType} to use for the given {@link Font.DisplayMode} and blur setting
         */
        public RenderType renderType(Font.DisplayMode p_181388_, boolean blur) {
            return this.glyph.renderTypes.select(p_181388_, blur);
        }

        /** @deprecated Neo: Use {@link #renderType(Font.DisplayMode, boolean)} instead */
        @Deprecated
        @Override
        public RenderType renderType(Font.DisplayMode p_440152_) {
            return this.glyph.renderTypes.select(p_440152_);
        }

        @Override
        public GpuTextureView textureView() {
            return this.glyph.textureView;
        }

        @Override
        public RenderPipeline guiPipeline() {
            return this.glyph.renderTypes.guiPipeline();
        }
    }

    @OnlyIn(Dist.CLIENT)
    record GlyphInstance(float x, float y, int color, int shadowColor, BakedSheetGlyph glyph, Style style, float boldOffset, float shadowOffset)
        implements TextRenderable {
        @Override
        public float left() {
            return this.glyph.left(this);
        }

        @Override
        public float top() {
            return this.glyph.top(this);
        }

        @Override
        public float right() {
            return this.glyph.right(this);
        }

        @Override
        public float bottom() {
            return this.glyph.bottom(this);
        }

        boolean hasShadow() {
            return this.shadowColor() != 0;
        }

        @Override
        public void render(Matrix4f p_439203_, VertexConsumer p_440403_, int p_439729_, boolean p_439133_) {
            this.glyph.renderChar(this, p_439203_, p_440403_, p_439729_, p_439133_);
        }

        /**
         * Neo: returns the {@link RenderType} to use for the given {@link Font.DisplayMode} and blur setting
         */
        public RenderType renderType(Font.DisplayMode p_181388_, boolean blur) {
            return this.glyph.renderTypes.select(p_181388_, blur);
        }

        /** @deprecated Neo: Use {@link #renderType(Font.DisplayMode, boolean)} instead */
        @Deprecated
        @Override
        public RenderType renderType(Font.DisplayMode p_439720_) {
            return this.glyph.renderTypes.select(p_439720_);
        }

        @Override
        public GpuTextureView textureView() {
            return this.glyph.textureView;
        }

        @Override
        public RenderPipeline guiPipeline() {
            return this.glyph.renderTypes.guiPipeline();
        }
    }
}
