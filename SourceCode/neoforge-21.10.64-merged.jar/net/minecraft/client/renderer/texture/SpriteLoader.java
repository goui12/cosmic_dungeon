package net.minecraft.client.renderer.texture;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceLocation location;
    private final int maxSupportedTextureSize;
    private final int minWidth;
    private final int minHeight;

    public SpriteLoader(ResourceLocation location, int maxSupportedTextureSize, int minWidth, int minHeight) {
        this.location = location;
        this.maxSupportedTextureSize = maxSupportedTextureSize;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    public static SpriteLoader create(TextureAtlas atlas) {
        return new SpriteLoader(atlas.location(), atlas.maxSupportedTextureSize(), atlas.getWidth(), atlas.getHeight());
    }

    private SpriteLoader.Preparations stitch(List<SpriteContents> contents, int mipLevel, Executor executor) {
        SpriteLoader.Preparations spriteloader$preparations;
        try (Zone zone = Profiler.get().zone(() -> "stitch " + this.location)) {
            int i = this.maxSupportedTextureSize;
            Stitcher<SpriteContents> stitcher = new Stitcher<>(i, i, mipLevel);
            int j = Integer.MAX_VALUE;
            int k = 1 << mipLevel;

            for (SpriteContents spritecontents : contents) {
                j = Math.min(j, Math.min(spritecontents.width(), spritecontents.height()));
                int l = Math.min(Integer.lowestOneBit(spritecontents.width()), Integer.lowestOneBit(spritecontents.height()));
                if (l < k) {
                    LOGGER.warn(
                        "Texture {} with size {}x{} limits mip level from {} to {}",
                        spritecontents.name(),
                        spritecontents.width(),
                        spritecontents.height(),
                        Mth.log2(k),
                        Mth.log2(l)
                    );
                    k = l;
                }

                stitcher.registerSprite(spritecontents);
            }

            int j1 = Math.min(j, k);
            int k1 = Mth.log2(j1);
            int l1;
            if (false) { // Forge: Do not lower the mipmap level
                LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", this.location, mipLevel, k1, j1);
                l1 = k1;
            } else {
                l1 = mipLevel;
            }

            try {
                stitcher.stitch();
            } catch (StitcherException stitcherexception) {
                CrashReport crashreport = CrashReport.forThrowable(stitcherexception, "Stitching");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Stitcher");
                crashreportcategory.setDetail(
                    "Sprites",
                    stitcherexception.getAllSprites()
                        .stream()
                        .map(p_249576_ -> String.format(Locale.ROOT, "%s[%dx%d]", p_249576_.name(), p_249576_.width(), p_249576_.height()))
                        .collect(Collectors.joining(","))
                );
                crashreportcategory.setDetail("Max Texture Size", i);
                throw new ReportedException(crashreport);
            }

            int i1 = Math.max(stitcher.getWidth(), this.minWidth);
            int i2 = Math.max(stitcher.getHeight(), this.minHeight);
            Map<ResourceLocation, TextureAtlasSprite> map = this.getStitchedSprites(stitcher, i1, i2);
            TextureAtlasSprite textureatlassprite = map.get(MissingTextureAtlasSprite.getLocation());
            CompletableFuture<Void> completablefuture;
            if (l1 > 0) {
                completablefuture = CompletableFuture.runAsync(() -> map.values().forEach(p_251202_ -> p_251202_.contents().increaseMipLevel(l1)), executor);
            } else {
                completablefuture = CompletableFuture.completedFuture(null);
            }

            spriteloader$preparations = new SpriteLoader.Preparations(i1, i2, l1, textureatlassprite, map, completablefuture);
        }

        return spriteloader$preparations;
    }

    private static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(
        SpriteResourceLoader spriteResourceLoader, List<Function<SpriteResourceLoader, SpriteContents>> factories, Executor executor
    ) {
        List<CompletableFuture<SpriteContents>> list = factories.stream()
            .map(p_293678_ -> CompletableFuture.supplyAsync(() -> (SpriteContents)p_293678_.apply(spriteResourceLoader), executor))
            .toList();
        return Util.sequence(list).thenApply(p_252234_ -> p_252234_.stream().filter(Objects::nonNull).toList());
    }

    public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(
        ResourceManager resourceManager, ResourceLocation location, int mipLevel, Executor executor, Set<MetadataSectionType<?>> sectionTypes
    ) {
        SpriteResourceLoader spriteresourceloader = SpriteResourceLoader.create(sectionTypes);
        return CompletableFuture.<List<Function<SpriteResourceLoader, SpriteContents>>>supplyAsync(
                () -> SpriteSourceList.load(resourceManager, location).list(resourceManager, sectionTypes), executor
            )
            .thenCompose(p_293671_ -> runSpriteSuppliers(spriteresourceloader, (List<Function<SpriteResourceLoader, SpriteContents>>)p_293671_, executor))
            .thenApply(p_261393_ -> this.stitch((List<SpriteContents>)p_261393_, mipLevel, executor));
    }

    private Map<ResourceLocation, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> stitcher, int x, int y) {
        Map<ResourceLocation, TextureAtlasSprite> map = new HashMap<>();
        stitcher.gatherSprites(
            (p_251421_, p_250533_, p_251913_) -> map.put(
                p_251421_.name(), new TextureAtlasSprite(this.location, p_251421_, x, y, p_250533_, p_251913_)
            )
        );
        return map;
    }

    @OnlyIn(Dist.CLIENT)
    public record Preparations(
        int width,
        int height,
        int mipLevel,
        TextureAtlasSprite missing,
        Map<ResourceLocation, TextureAtlasSprite> regions,
        CompletableFuture<Void> readyForUpload
    ) {
        @Nullable
        public TextureAtlasSprite getSprite(ResourceLocation name) {
            return this.regions.get(name);
        }
    }
}
