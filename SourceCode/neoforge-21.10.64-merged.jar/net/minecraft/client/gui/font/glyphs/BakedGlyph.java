package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface BakedGlyph {
    GlyphInfo info();

    @Nullable
    TextRenderable createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset);
}
