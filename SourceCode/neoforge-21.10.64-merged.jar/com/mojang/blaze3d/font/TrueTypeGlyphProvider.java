package com.mojang.blaze3d.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

@OnlyIn(Dist.CLIENT)
public class TrueTypeGlyphProvider implements GlyphProvider {
    @Nullable
    private ByteBuffer fontMemory;
    @Nullable
    private FT_Face face;
    final float oversample;
    private final CodepointMap<TrueTypeGlyphProvider.GlyphEntry> glyphs = new CodepointMap<>(
        TrueTypeGlyphProvider.GlyphEntry[]::new, TrueTypeGlyphProvider.GlyphEntry[][]::new
    );

    public TrueTypeGlyphProvider(ByteBuffer fontMemory, FT_Face face, float size, float oversample, float shiftX, float shiftY, String skip) {
        this.fontMemory = fontMemory;
        this.face = face;
        this.oversample = oversample;
        IntSet intset = new IntArraySet();
        skip.codePoints().forEach(intset::add);
        int i = Math.round(size * oversample);
        FreeType.FT_Set_Pixel_Sizes(face, i, i);
        float f = shiftX * oversample;
        float f1 = -shiftY * oversample;

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            FT_Vector ft_vector = FreeTypeUtil.setVector(FT_Vector.malloc(memorystack), f, f1);
            FreeType.FT_Set_Transform(face, null, ft_vector);
            IntBuffer intbuffer = memorystack.mallocInt(1);
            int j = (int)FreeType.FT_Get_First_Char(face, intbuffer);

            while (true) {
                int k = intbuffer.get(0);
                if (k == 0) {
                    return;
                }

                if (!intset.contains(j)) {
                    this.glyphs.put(j, new TrueTypeGlyphProvider.GlyphEntry(k));
                }

                j = (int)FreeType.FT_Get_Next_Char(face, j, intbuffer);
            }
        }
    }

    @Nullable
    @Override
    public UnbakedGlyph getGlyph(int character) {
        TrueTypeGlyphProvider.GlyphEntry truetypeglyphprovider$glyphentry = this.glyphs.get(character);
        return truetypeglyphprovider$glyphentry != null ? this.getOrLoadGlyphInfo(character, truetypeglyphprovider$glyphentry) : null;
    }

    private UnbakedGlyph getOrLoadGlyphInfo(int character, TrueTypeGlyphProvider.GlyphEntry glyphEntry) {
        UnbakedGlyph unbakedglyph = glyphEntry.glyph;
        if (unbakedglyph == null) {
            FT_Face ft_face = this.validateFontOpen();
            synchronized (ft_face) {
                unbakedglyph = glyphEntry.glyph;
                if (unbakedglyph == null) {
                    unbakedglyph = this.loadGlyph(character, ft_face, glyphEntry.index);
                    glyphEntry.glyph = unbakedglyph;
                }
            }
        }

        return unbakedglyph;
    }

    private UnbakedGlyph loadGlyph(int character, FT_Face face, int index) {
        int i = FreeType.FT_Load_Glyph(face, index, 4194312);
        if (i != 0) {
            FreeTypeUtil.assertError(i, String.format(Locale.ROOT, "Loading glyph U+%06X", character));
        }

        FT_GlyphSlot ft_glyphslot = face.glyph();
        if (ft_glyphslot == null) {
            throw new NullPointerException(String.format(Locale.ROOT, "Glyph U+%06X not initialized", character));
        } else {
            float f = FreeTypeUtil.x(ft_glyphslot.advance());
            FT_Bitmap ft_bitmap = ft_glyphslot.bitmap();
            int j = ft_glyphslot.bitmap_left();
            int k = ft_glyphslot.bitmap_top();
            int l = ft_bitmap.width();
            int i1 = ft_bitmap.rows();
            return (UnbakedGlyph)(l > 0 && i1 > 0 ? new TrueTypeGlyphProvider.Glyph(j, k, l, i1, f, index) : new EmptyGlyph(f / this.oversample));
        }
    }

    FT_Face validateFontOpen() {
        if (this.fontMemory != null && this.face != null) {
            return this.face;
        } else {
            throw new IllegalStateException("Provider already closed");
        }
    }

    @Override
    public void close() {
        if (this.face != null) {
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                FreeTypeUtil.checkError(FreeType.FT_Done_Face(this.face), "Deleting face");
            }

            this.face = null;
        }

        MemoryUtil.memFree(this.fontMemory);
        this.fontMemory = null;
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return this.glyphs.keySet();
    }

    @OnlyIn(Dist.CLIENT)
    class Glyph implements UnbakedGlyph {
        final int width;
        final int height;
        final float bearingX;
        final float bearingY;
        private final GlyphInfo info;
        final int index;

        Glyph(float bearingX, float bearingY, int width, int height, float advance, int index) {
            this.width = width;
            this.height = height;
            this.info = GlyphInfo.simple(advance / TrueTypeGlyphProvider.this.oversample);
            this.bearingX = bearingX / TrueTypeGlyphProvider.this.oversample;
            this.bearingY = bearingY / TrueTypeGlyphProvider.this.oversample;
            this.index = index;
        }

        @Override
        public GlyphInfo info() {
            return this.info;
        }

        @Override
        public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
            return stitcher.stitch(
                this.info,
                new GlyphBitmap() {
                    @Override
                    public int getPixelWidth() {
                        return Glyph.this.width;
                    }

                    @Override
                    public int getPixelHeight() {
                        return Glyph.this.height;
                    }

                    @Override
                    public float getOversample() {
                        return TrueTypeGlyphProvider.this.oversample;
                    }

                    @Override
                    public float getBearingLeft() {
                        return Glyph.this.bearingX;
                    }

                    @Override
                    public float getBearingTop() {
                        return Glyph.this.bearingY;
                    }

                    @Override
                    public void upload(int p_231126_, int p_231127_, GpuTexture p_405861_) {
                        FT_Face ft_face = TrueTypeGlyphProvider.this.validateFontOpen();

                        try (NativeImage nativeimage = new NativeImage(NativeImage.Format.LUMINANCE, Glyph.this.width, Glyph.this.height, false)) {
                            if (nativeimage.copyFromFont(ft_face, Glyph.this.index)) {
                                RenderSystem.getDevice()
                                    .createCommandEncoder()
                                    .writeToTexture(p_405861_, nativeimage, 0, 0, p_231126_, p_231127_, Glyph.this.width, Glyph.this.height, 0, 0);
                            }
                        }
                    }

                    @Override
                    public boolean isColored() {
                        return false;
                    }
                }
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class GlyphEntry {
        final int index;
        @Nullable
        volatile UnbakedGlyph glyph;

        GlyphEntry(int index) {
            this.index = index;
        }
    }
}
