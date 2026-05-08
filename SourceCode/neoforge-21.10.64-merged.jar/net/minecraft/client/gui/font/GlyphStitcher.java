package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlyphStitcher implements AutoCloseable {
    private final TextureManager textureManager;
    private final ResourceLocation texturePrefix;
    private final List<FontTexture> textures = new ArrayList<>();

    public GlyphStitcher(TextureManager textureManager, ResourceLocation texturePrefix) {
        this.textureManager = textureManager;
        this.texturePrefix = texturePrefix;
    }

    public void reset() {
        int i = this.textures.size();
        this.textures.clear();

        for (int j = 0; j < i; j++) {
            this.textureManager.release(this.textureName(j));
        }
    }

    @Override
    public void close() {
        this.reset();
    }

    @Nullable
    public BakedSheetGlyph stitch(GlyphInfo glyphInfo, GlyphBitmap glyphBitmap) {
        for (FontTexture fonttexture : this.textures) {
            BakedSheetGlyph bakedsheetglyph = fonttexture.add(glyphInfo, glyphBitmap);
            if (bakedsheetglyph != null) {
                return bakedsheetglyph;
            }
        }

        int i = this.textures.size();
        ResourceLocation resourcelocation = this.textureName(i);
        boolean flag = glyphBitmap.isColored();
        GlyphRenderTypes glyphrendertypes = flag
            ? GlyphRenderTypes.createForColorTexture(resourcelocation)
            : GlyphRenderTypes.createForIntensityTexture(resourcelocation);
        FontTexture fonttexture1 = new FontTexture(resourcelocation::toString, glyphrendertypes, flag);
        this.textures.add(fonttexture1);
        this.textureManager.register(resourcelocation, fonttexture1);
        return fonttexture1.add(glyphInfo, glyphBitmap);
    }

    private ResourceLocation textureName(int name) {
        return this.texturePrefix.withSuffix("/" + name);
    }
}
