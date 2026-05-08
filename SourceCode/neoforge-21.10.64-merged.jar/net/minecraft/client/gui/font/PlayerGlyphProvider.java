package net.minecraft.client.gui.font;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PlayerGlyphProvider {
    private static final float WIDTH = 8.0F;
    private static final float HEIGHT = 8.0F;
    static final GlyphInfo GLYPH_INFO = GlyphInfo.simple(8.0F);
    final PlayerSkinRenderCache playerSkinRenderCache;
    private final LoadingCache<FontDescription.PlayerSprite, GlyphSource> wrapperCache = CacheBuilder.newBuilder()
        .expireAfterAccess(PlayerSkinRenderCache.CACHE_DURATION)
        .build(
            new CacheLoader<FontDescription.PlayerSprite, GlyphSource>() {
                public GlyphSource load(FontDescription.PlayerSprite p_443321_) {
                    final Supplier<PlayerSkinRenderCache.RenderInfo> supplier = PlayerGlyphProvider.this.playerSkinRenderCache
                        .createLookup(p_443321_.profile());
                    final boolean flag = p_443321_.hat();
                    return new SingleSpriteSource(
                        new BakedGlyph() {
                            @Override
                            public GlyphInfo info() {
                                return PlayerGlyphProvider.GLYPH_INFO;
                            }

                            @Override
                            public TextRenderable createGlyph(
                                float p_443374_, float p_443164_, int p_443037_, int p_443142_, Style p_442732_, float p_443597_, float p_442907_
                            ) {
                                return new PlayerGlyphProvider.Instance(supplier, flag, p_443374_, p_443164_, p_443037_, p_443142_, p_442907_);
                            }
                        }
                    );
                }
            }
        );

    public PlayerGlyphProvider(PlayerSkinRenderCache playerSkinRenderCache) {
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    public GlyphSource sourceForPlayer(FontDescription.PlayerSprite player) {
        return this.wrapperCache.getUnchecked(player);
    }

    @OnlyIn(Dist.CLIENT)
    record Instance(Supplier<PlayerSkinRenderCache.RenderInfo> skin, boolean hat, float x, float y, int color, int shadowColor, float shadowOffset)
        implements PlainTextRenderable {
        @Override
        public void renderSprite(Matrix4f pose, VertexConsumer consumer, int packedLight, float x, float y, float z, int color) {
            float f = x + this.left();
            float f1 = x + this.right();
            float f2 = y + this.top();
            float f3 = y + this.bottom();
            renderQuad(pose, consumer, packedLight, f, f1, f2, f3, z, color, 8.0F, 8.0F, 8, 8, 64, 64);
            if (this.hat) {
                renderQuad(pose, consumer, packedLight, f, f1, f2, f3, z, color, 40.0F, 8.0F, 8, 8, 64, 64);
            }
        }

        private static void renderQuad(
            Matrix4f pose,
            VertexConsumer consumer,
            int packedLight,
            float x1,
            float x2,
            float y1,
            float y2,
            float z,
            int color,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight
        ) {
            float f = (u + 0.0F) / textureWidth;
            float f1 = (u + width) / textureWidth;
            float f2 = (v + 0.0F) / textureHeight;
            float f3 = (v + height) / textureHeight;
            consumer.addVertex(pose, x1, y1, z).setUv(f, f2).setColor(color).setLight(packedLight);
            consumer.addVertex(pose, x1, y2, z).setUv(f, f3).setColor(color).setLight(packedLight);
            consumer.addVertex(pose, x2, y2, z).setUv(f1, f3).setColor(color).setLight(packedLight);
            consumer.addVertex(pose, x2, y1, z).setUv(f1, f2).setColor(color).setLight(packedLight);
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return this.skin.get().glyphRenderTypes().select(displayMode);
        }

        @Override
        public RenderPipeline guiPipeline() {
            return this.skin.get().glyphRenderTypes().guiPipeline();
        }

        @Override
        public GpuTextureView textureView() {
            return this.skin.get().textureView();
        }

        @Override
        public float left() {
            return 0.0F;
        }

        @Override
        public float right() {
            return 8.0F;
        }

        @Override
        public float top() {
            return -1.0F;
        }

        @Override
        public float bottom() {
            return 7.0F;
        }
    }
}
