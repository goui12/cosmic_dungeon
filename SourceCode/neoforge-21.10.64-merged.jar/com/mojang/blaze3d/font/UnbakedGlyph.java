package com.mojang.blaze3d.font;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface UnbakedGlyph {
    GlyphInfo info();

    BakedGlyph bake(UnbakedGlyph.Stitcher stitcher);

    @OnlyIn(Dist.CLIENT)
    public interface Stitcher {
        BakedGlyph stitch(GlyphInfo info, GlyphBitmap bitmap);

        BakedGlyph getMissing();
    }
}
