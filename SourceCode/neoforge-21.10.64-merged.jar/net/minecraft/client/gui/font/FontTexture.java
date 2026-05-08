package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.file.Path;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FontTexture extends AbstractTexture implements Dumpable {
    private static final int SIZE = 256;
    private final GlyphRenderTypes renderTypes;
    private final boolean colored;
    private final FontTexture.Node root;

    public FontTexture(Supplier<String> label, GlyphRenderTypes renderTypes, boolean colored) {
        this.colored = colored;
        this.root = new FontTexture.Node(0, 0, 256, 256);
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.texture = gpudevice.createTexture(label, 7, colored ? TextureFormat.RGBA8 : TextureFormat.RED8, 256, 256, 1, 1);
        this.texture.setTextureFilter(FilterMode.NEAREST, false);
        this.textureView = gpudevice.createTextureView(this.texture);
        this.renderTypes = renderTypes;
    }

    @Nullable
    public BakedSheetGlyph add(GlyphInfo glyphInfo, GlyphBitmap glyphBitmap) {
        if (glyphBitmap.isColored() != this.colored) {
            return null;
        } else {
            FontTexture.Node fonttexture$node = this.root.insert(glyphBitmap);
            if (fonttexture$node != null) {
                glyphBitmap.upload(fonttexture$node.x, fonttexture$node.y, this.getTexture());
                float f = 256.0F;
                float f1 = 256.0F;
                float f2 = 0.01F;
                return new BakedSheetGlyph(
                    glyphInfo,
                    this.renderTypes,
                    this.getTextureView(),
                    (fonttexture$node.x + 0.01F) / 256.0F,
                    (fonttexture$node.x - 0.01F + glyphBitmap.getPixelWidth()) / 256.0F,
                    (fonttexture$node.y + 0.01F) / 256.0F,
                    (fonttexture$node.y - 0.01F + glyphBitmap.getPixelHeight()) / 256.0F,
                    glyphBitmap.getLeft(),
                    glyphBitmap.getRight(),
                    glyphBitmap.getTop(),
                    glyphBitmap.getBottom()
                );
            } else {
                return null;
            }
        }
    }

    @Override
    public void dumpContents(ResourceLocation resourceLocation, Path path) {
        if (this.texture != null) {
            String s = resourceLocation.toDebugFileName();
            TextureUtil.writeAsPNG(path, s, this.texture, 0, p_285145_ -> (p_285145_ & 0xFF000000) == 0 ? -16777216 : p_285145_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Node {
        final int x;
        final int y;
        private final int width;
        private final int height;
        @Nullable
        private FontTexture.Node left;
        @Nullable
        private FontTexture.Node right;
        private boolean occupied;

        Node(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Nullable
        FontTexture.Node insert(GlyphBitmap glyphBitmap) {
            if (this.left != null && this.right != null) {
                FontTexture.Node fonttexture$node = this.left.insert(glyphBitmap);
                if (fonttexture$node == null) {
                    fonttexture$node = this.right.insert(glyphBitmap);
                }

                return fonttexture$node;
            } else if (this.occupied) {
                return null;
            } else {
                int i = glyphBitmap.getPixelWidth();
                int j = glyphBitmap.getPixelHeight();
                if (i > this.width || j > this.height) {
                    return null;
                } else if (i == this.width && j == this.height) {
                    this.occupied = true;
                    return this;
                } else {
                    int k = this.width - i;
                    int l = this.height - j;
                    if (k > l) {
                        this.left = new FontTexture.Node(this.x, this.y, i, this.height);
                        this.right = new FontTexture.Node(this.x + i + 1, this.y, this.width - i - 1, this.height);
                    } else {
                        this.left = new FontTexture.Node(this.x, this.y, this.width, j);
                        this.right = new FontTexture.Node(this.x, this.y + j + 1, this.width, this.height - j - 1);
                    }

                    return this.left.insert(glyphBitmap);
                }
            }
        }
    }
}
