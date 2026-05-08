package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.UnbakedGlyph;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EmptyGlyph implements UnbakedGlyph {
    final GlyphInfo info;

    public EmptyGlyph(float width) {
        this.info = GlyphInfo.simple(width);
    }

    @Override
    public GlyphInfo info() {
        return this.info;
    }

    @Override
    public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
        return new BakedGlyph() {
            @Override
            public GlyphInfo info() {
                return EmptyGlyph.this.info;
            }

            @Nullable
            @Override
            public TextRenderable createGlyph(float p_439459_, float p_440447_, int p_439996_, int p_440639_, Style p_439498_, float p_439464_, float p_439851_) {
                return null;
            }
        };
    }
}
