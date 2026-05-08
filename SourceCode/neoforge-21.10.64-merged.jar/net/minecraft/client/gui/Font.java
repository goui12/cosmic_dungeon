package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringDecomposer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class Font implements net.neoforged.neoforge.client.extensions.IFontExtension {
    private static final float EFFECT_DEPTH = 0.01F;
    private static final float OVER_EFFECT_DEPTH = 0.01F;
    private static final float UNDER_EFFECT_DEPTH = -0.01F;
    public static final float SHADOW_DEPTH = 0.03F;
    public static final int NO_SHADOW = 0;
    public final int lineHeight = 9;
    private final RandomSource random = RandomSource.create();
    final Font.Provider provider;
    private final StringSplitter splitter;
    /** Neo: enables linear filtering on text */
    public boolean enableTextTextureLinearFiltering = false;

    public Font(Font.Provider provider) {
        this.provider = provider;
        this.splitter = new StringSplitter(
            (p_438720_, p_438721_) -> this.getGlyphSource(p_438721_.getFont()).getGlyph(p_438720_).info().getAdvance(p_438721_.isBold())
        );
    }

    private GlyphSource getGlyphSource(FontDescription fontDescription) {
        return this.provider.glyphs(fontDescription);
    }

    /**
     * Apply Unicode Bidirectional Algorithm to string and return a new possibly reordered string for visual rendering.
     */
    public String bidirectionalShaping(String text) {
        try {
            Bidi bidi = new Bidi(new ArabicShaping(8).shape(text), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (ArabicShapingException arabicshapingexception) {
            return text;
        }
    }

    public void drawInBatch(
        String text,
        float x,
        float y,
        int color,
        boolean drawShadow,
        Matrix4f pose,
        MultiBufferSource bufferSource,
        Font.DisplayMode mode,
        int backgroundColor,
        int packedLightCoords
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(text, x, y, color, drawShadow, backgroundColor);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, mode, packedLightCoords));
    }

    public void drawInBatch(
        Component text,
        float x,
        float y,
        int color,
        boolean drawShadow,
        Matrix4f pose,
        MultiBufferSource bufferSource,
        Font.DisplayMode mode,
        int backgroundColor,
        int packedLightCoords
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(text.getVisualOrderText(), x, y, color, drawShadow, backgroundColor);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, mode, packedLightCoords));
    }

    public void drawInBatch(
        FormattedCharSequence text,
        float x,
        float y,
        int color,
        boolean drawShadow,
        Matrix4f pose,
        MultiBufferSource bufferSource,
        Font.DisplayMode mode,
        int backgroundColor,
        int packedLightCoords
    ) {
        Font.PreparedText font$preparedtext = this.prepareText(text, x, y, color, drawShadow, backgroundColor);
        font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, mode, packedLightCoords));
    }

    public void drawInBatch8xOutline(
        FormattedCharSequence text,
        float x,
        float y,
        int color,
        int backgroundColor,
        Matrix4f pose,
        MultiBufferSource bufferSource,
        int packedLightCoords
    ) {
        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(0.0F, 0.0F, backgroundColor, false);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) {
                    float[] afloat = new float[]{x};
                    int k = i;
                    int l = j;
                    text.accept((p_438717_, p_438718_, p_438719_) -> {
                        boolean flag = p_438718_.isBold();
                        BakedGlyph bakedglyph = this.getGlyph(p_438719_, p_438718_);
                        font$preparedtextbuilder.x = afloat[0] + k * bakedglyph.info().getShadowOffset();
                        font$preparedtextbuilder.y = y + l * bakedglyph.info().getShadowOffset();
                        afloat[0] += bakedglyph.info().getAdvance(flag);
                        return font$preparedtextbuilder.accept(p_438717_, p_438718_.withColor(backgroundColor), bakedglyph);
                    });
                }
            }
        }

        Font.GlyphVisitor font$glyphvisitor = Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, Font.DisplayMode.NORMAL, packedLightCoords);

        for (TextRenderable textrenderable : font$preparedtextbuilder.glyphs) {
            font$glyphvisitor.acceptGlyph(textrenderable);
        }

        Font.PreparedTextBuilder font$preparedtextbuilder1 = new Font.PreparedTextBuilder(x, y, color, false);
        text.accept(font$preparedtextbuilder1);
        font$preparedtextbuilder1.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, Font.DisplayMode.POLYGON_OFFSET, packedLightCoords));
    }

    BakedGlyph getGlyph(int character, Style style) {
        GlyphSource glyphsource = this.getGlyphSource(style.getFont());
        BakedGlyph bakedglyph = glyphsource.getGlyph(character);
        if (style.isObfuscated() && character != 32) {
            int i = Mth.ceil(bakedglyph.info().getAdvance(false));
            bakedglyph = glyphsource.getRandomGlyph(this.random, i);
        }

        return bakedglyph;
    }

    public Font.PreparedText prepareText(String text, float x, float y, int color, boolean dropShadow, int backgroundColor) {
        if (this.isBidirectional()) {
            text = this.bidirectionalShaping(text);
        }

        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(x, y, color, backgroundColor, dropShadow);
        StringDecomposer.iterateFormatted(text, Style.EMPTY, font$preparedtextbuilder);
        return font$preparedtextbuilder;
    }

    public Font.PreparedText prepareText(FormattedCharSequence text, float x, float y, int color, boolean dropShadow, int backgroundColor) {
        Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(x, y, color, backgroundColor, dropShadow);
        text.accept(font$preparedtextbuilder);
        return font$preparedtextbuilder;
    }

    /**
     * Returns the width of this string. Equivalent of FontMetrics.stringWidth(String s).
     */
    public int width(String text) {
        return Mth.ceil(this.splitter.stringWidth(text));
    }

    public int width(FormattedText text) {
        return Mth.ceil(this.splitter.stringWidth(text));
    }

    public int width(FormattedCharSequence text) {
        return Mth.ceil(this.splitter.stringWidth(text));
    }

    public String plainSubstrByWidth(String text, int maxWidth, boolean tail) {
        return tail ? this.splitter.plainTailByWidth(text, maxWidth, Style.EMPTY) : this.splitter.plainHeadByWidth(text, maxWidth, Style.EMPTY);
    }

    public String plainSubstrByWidth(String text, int maxWidth) {
        return this.splitter.plainHeadByWidth(text, maxWidth, Style.EMPTY);
    }

    public FormattedText substrByWidth(FormattedText text, int maxWidth) {
        return this.splitter.headByWidth(text, maxWidth, Style.EMPTY);
    }

    /**
     * Returns the height (in pixels) of the given string if it is wordwrapped to the given max width.
     */
    public int wordWrapHeight(String text, int maxWidth) {
        return 9 * this.splitter.splitLines(text, maxWidth, Style.EMPTY).size();
    }

    public int wordWrapHeight(FormattedText text, int maxWidth) {
        return 9 * this.splitter.splitLines(text, maxWidth, Style.EMPTY).size();
    }

    public List<FormattedCharSequence> split(FormattedText text, int maxWidth) {
        return Language.getInstance().getVisualOrder(this.splitter.splitLines(text, maxWidth, Style.EMPTY));
    }

    public List<FormattedText> splitIgnoringLanguage(FormattedText text, int maxWidth) {
        return this.splitter.splitLines(text, maxWidth, Style.EMPTY);
    }

    public boolean isBidirectional() {
        return Language.getInstance().isDefaultRightToLeft();
    }

    public StringSplitter getSplitter() {
        return this.splitter;
    }

    @Override public Font self() { return this; }

    @OnlyIn(Dist.CLIENT)
    public static enum DisplayMode {
        NORMAL,
        SEE_THROUGH,
        POLYGON_OFFSET;
    }

    @OnlyIn(Dist.CLIENT)
    public interface GlyphVisitor {
        static Font.GlyphVisitor forMultiBufferSource(
            final MultiBufferSource bufferSource, final Matrix4f pose, final Font.DisplayMode displayMode, final int packedLight
        ) {
            return new Font.GlyphVisitor() {
                @Override
                public void acceptGlyph(TextRenderable glyph) {
                    this.render(glyph);
                }

                @Override
                public void acceptEffect(TextRenderable effect) {
                    this.render(effect);
                }

                private void render(TextRenderable renderable) {
                    VertexConsumer vertexconsumer = bufferSource.getBuffer(renderable.renderType(displayMode));
                    renderable.render(pose, vertexconsumer, packedLight, false);
                }
            };
        }

        void acceptGlyph(TextRenderable glyph);

        void acceptEffect(TextRenderable effect);
    }

    @OnlyIn(Dist.CLIENT)
    public interface PreparedText {
        void visit(Font.GlyphVisitor visitor);

        @Nullable
        ScreenRectangle bounds();
    }

    @OnlyIn(Dist.CLIENT)
    class PreparedTextBuilder implements FormattedCharSink, Font.PreparedText {
        private final boolean drawShadow;
        private final int color;
        private final int backgroundColor;
        float x;
        float y;
        private float left = Float.MAX_VALUE;
        private float top = Float.MAX_VALUE;
        private float right = -Float.MAX_VALUE;
        private float bottom = -Float.MAX_VALUE;
        private float backgroundLeft = Float.MAX_VALUE;
        private float backgroundTop = Float.MAX_VALUE;
        private float backgroundRight = -Float.MAX_VALUE;
        private float backgroundBottom = -Float.MAX_VALUE;
        final List<TextRenderable> glyphs = new ArrayList<>();
        @Nullable
        private List<TextRenderable> effects;

        public PreparedTextBuilder(float x, float y, int color, boolean dropShadow) {
            this(x, y, color, 0, dropShadow);
        }

        public PreparedTextBuilder(float x, float y, int color, int backgroundColor, boolean dropShadow) {
            this.x = x;
            this.y = y;
            this.drawShadow = dropShadow;
            this.color = color;
            this.backgroundColor = backgroundColor;
            this.markBackground(x, y, 0.0F);
        }

        private void markSize(float left, float top, float right, float bottom) {
            this.left = Math.min(this.left, left);
            this.top = Math.min(this.top, top);
            this.right = Math.max(this.right, right);
            this.bottom = Math.max(this.bottom, bottom);
        }

        private void markBackground(float x, float y, float advance) {
            if (ARGB.alpha(this.backgroundColor) != 0) {
                this.backgroundLeft = Math.min(this.backgroundLeft, x - 1.0F);
                this.backgroundTop = Math.min(this.backgroundTop, y - 1.0F);
                this.backgroundRight = Math.max(this.backgroundRight, x + advance);
                this.backgroundBottom = Math.max(this.backgroundBottom, y + 9.0F);
                this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
            }
        }

        private void addGlyph(TextRenderable glyph) {
            this.glyphs.add(glyph);
            this.markSize(glyph.left(), glyph.top(), glyph.right(), glyph.bottom());
        }

        private void addEffect(TextRenderable effect) {
            if (this.effects == null) {
                this.effects = new ArrayList<>();
            }

            this.effects.add(effect);
            this.markSize(effect.left(), effect.top(), effect.right(), effect.bottom());
        }

        /**
         * Accepts a single code point from a {@link net.minecraft.util.FormattedCharSequence}.
         * @return {@code true} to accept more characters, {@code false} to stop traversing the sequence.
         *
         * @param positionInCurrentSequence Contains the relative position of the
         *                                  character in the current sub-sequence. If
         *                                  multiple formatted char sequences have been
         *                                  combined, this value will reset to {@code 0}
         *                                  after each sequence has been fully consumed.
         */
        @Override
        public boolean accept(int positionInCurrentSequence, Style style, int codePoint) {
            BakedGlyph bakedglyph = Font.this.getGlyph(codePoint, style);
            return this.accept(positionInCurrentSequence, style, bakedglyph);
        }

        public boolean accept(int positionInCurrentSequence, Style style, BakedGlyph glyph) {
            GlyphInfo glyphinfo = glyph.info();
            boolean flag = style.isBold();
            TextColor textcolor = style.getColor();
            int i = this.getTextColor(textcolor);
            int j = this.getShadowColor(style, i);
            float f = glyphinfo.getAdvance(flag);
            float f1 = positionInCurrentSequence == 0 ? this.x - 1.0F : this.x;
            float f2 = glyphinfo.getShadowOffset();
            float f3 = flag ? glyphinfo.getBoldOffset() : 0.0F;
            TextRenderable textrenderable = glyph.createGlyph(this.x, this.y, i, j, style, f3, f2);
            if (textrenderable != null) {
                this.addGlyph(textrenderable);
            }

            this.markBackground(this.x, this.y, f);
            if (style.isStrikethrough()) {
                this.addEffect(Font.this.provider.effect().createEffect(f1, this.y + 4.5F - 1.0F, this.x + f, this.y + 4.5F, 0.01F, i, j, f2));
            }

            if (style.isUnderlined()) {
                this.addEffect(Font.this.provider.effect().createEffect(f1, this.y + 9.0F - 1.0F, this.x + f, this.y + 9.0F, 0.01F, i, j, f2));
            }

            this.x += f;
            return true;
        }

        @Override
        public void visit(Font.GlyphVisitor visitor) {
            if (ARGB.alpha(this.backgroundColor) != 0) {
                visitor.acceptEffect(
                    Font.this.provider
                        .effect()
                        .createEffect(
                            this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom, -0.01F, this.backgroundColor, 0, 0.0F
                        )
                );
            }

            for (TextRenderable textrenderable : this.glyphs) {
                visitor.acceptGlyph(textrenderable);
            }

            if (this.effects != null) {
                for (TextRenderable textrenderable1 : this.effects) {
                    visitor.acceptEffect(textrenderable1);
                }
            }
        }

        private int getTextColor(@Nullable TextColor textColor) {
            if (textColor != null) {
                int i = ARGB.alpha(this.color);
                int j = textColor.getValue();
                return ARGB.color(i, j);
            } else {
                return this.color;
            }
        }

        private int getShadowColor(Style style, int textColor) {
            Integer integer = style.getShadowColor();
            if (integer != null) {
                float f = ARGB.alphaFloat(textColor);
                float f1 = ARGB.alphaFloat(integer);
                return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * f1), integer) : integer;
            } else {
                return this.drawShadow ? ARGB.scaleRGB(textColor, 0.25F) : 0;
            }
        }

        @Nullable
        @Override
        public ScreenRectangle bounds() {
            if (!(this.left >= this.right) && !(this.top >= this.bottom)) {
                int i = Mth.floor(this.left);
                int j = Mth.floor(this.top);
                int k = Mth.ceil(this.right);
                int l = Mth.ceil(this.bottom);
                return new ScreenRectangle(i, j, k - i, l - j);
            } else {
                return null;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface Provider {
        GlyphSource glyphs(FontDescription fontDescription);

        EffectGlyph effect();
    }
}
