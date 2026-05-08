package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureAtlas extends AbstractTexture implements Dumpable, Tickable {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Deprecated
    public static final ResourceLocation LOCATION_BLOCKS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    @Deprecated
    public static final ResourceLocation LOCATION_PARTICLES = ResourceLocation.withDefaultNamespace("textures/atlas/particles.png");
    private List<SpriteContents> sprites = List.of();
    private List<TextureAtlasSprite.Ticker> animatedTextures = List.of();
    private Map<ResourceLocation, TextureAtlasSprite> texturesByName = Map.of();
    @Nullable
    private TextureAtlasSprite missingSprite;
    private final ResourceLocation location;
    private final int maxSupportedTextureSize;
    private int width;
    private int height;
    private int mipLevel;

    public TextureAtlas(ResourceLocation location) {
        this.location = location;
        this.maxSupportedTextureSize = RenderSystem.getDevice().getMaxTextureSize();
    }

    private void createTexture(int width, int height, int mipLevel) {
        LOGGER.info("Created: {}x{}x{} {}-atlas", width, height, mipLevel, this.location);
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.close();
        this.texture = gpudevice.createTexture(this.location::toString, 7, TextureFormat.RGBA8, width, height, 1, mipLevel + 1);
        this.textureView = gpudevice.createTextureView(this.texture);
        this.width = width;
        this.height = height;
        this.mipLevel = mipLevel;
    }

    public void upload(SpriteLoader.Preparations preparations) {
        this.createTexture(preparations.width(), preparations.height(), preparations.mipLevel());
        this.clearTextureData();
        this.setFilter(false, this.mipLevel > 1);
        this.texturesByName = Map.copyOf(preparations.regions());
        this.missingSprite = this.texturesByName.get(MissingTextureAtlasSprite.getLocation());
        if (this.missingSprite == null) {
            throw new IllegalStateException("Atlas '" + this.location + "' (" + this.texturesByName.size() + " sprites) has no missing texture sprite");
        } else {
            List<SpriteContents> list = new ArrayList<>();
            List<TextureAtlasSprite.Ticker> list1 = new ArrayList<>();

            for (TextureAtlasSprite textureatlassprite : preparations.regions().values()) {
                list.add(textureatlassprite.contents());

                try {
                    textureatlassprite.uploadFirstFrame(this.texture);
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "Stitching texture atlas");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Texture being stitched together");
                    crashreportcategory.setDetail("Atlas path", this.location);
                    crashreportcategory.setDetail("Sprite", textureatlassprite);
                    throw new ReportedException(crashreport);
                }

                TextureAtlasSprite.Ticker textureatlassprite$ticker = textureatlassprite.createTicker();
                if (textureatlassprite$ticker != null) {
                    list1.add(textureatlassprite$ticker);
                }
            }

            this.sprites = List.copyOf(list);
            this.animatedTextures = List.copyOf(list1);
            if (SharedConstants.DEBUG_DUMP_TEXTURE_ATLAS) {
                Path path = TextureUtil.getDebugTexturePath();

                try {
                    Files.createDirectories(path);
                    this.dumpContents(this.location, path);
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to dump atlas contents to {}", path);
                }
            }

            net.neoforged.neoforge.client.ClientHooks.onTextureAtlasStitched(this);
        }
    }

    @Override
    public void dumpContents(ResourceLocation resourceLocation, Path path) throws IOException {
        String s = resourceLocation.toDebugFileName();
        TextureUtil.writeAsPNG(path, s, this.getTexture(), this.mipLevel, p_406255_ -> p_406255_);
        dumpSpriteNames(path, s, this.texturesByName);
    }

    private static void dumpSpriteNames(Path outputDir, String outputFilename, Map<ResourceLocation, TextureAtlasSprite> sprites) {
        Path path = outputDir.resolve(outputFilename + ".txt");

        try (Writer writer = Files.newBufferedWriter(path)) {
            for (Entry<ResourceLocation, TextureAtlasSprite> entry : sprites.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
                TextureAtlasSprite textureatlassprite = entry.getValue();
                writer.write(
                    String.format(
                        Locale.ROOT,
                        "%s\tx=%d\ty=%d\tw=%d\th=%d%n",
                        entry.getKey(),
                        textureatlassprite.getX(),
                        textureatlassprite.getY(),
                        textureatlassprite.contents().width(),
                        textureatlassprite.contents().height()
                    )
                );
            }
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to write file {}", path, ioexception);
        }
    }

    public void cycleAnimationFrames() {
        if (this.texture != null) {
            for (TextureAtlasSprite.Ticker textureatlassprite$ticker : this.animatedTextures) {
                textureatlassprite$ticker.tickAndUpload(this.texture);
            }
        }
    }

    @Override
    public void tick() {
        this.cycleAnimationFrames();
    }

    public TextureAtlasSprite getSprite(ResourceLocation name) {
        TextureAtlasSprite textureatlassprite = this.texturesByName.getOrDefault(name, this.missingSprite);
        if (textureatlassprite == null) {
            throw new IllegalStateException("Tried to lookup sprite, but atlas is not initialized");
        } else {
            return textureatlassprite;
        }
    }

    public TextureAtlasSprite missingSprite() {
        return Objects.requireNonNull(this.missingSprite, "Atlas not initialized");
    }

    public void clearTextureData() {
        this.sprites.forEach(SpriteContents::close);
        this.animatedTextures.forEach(TextureAtlasSprite.Ticker::close);
        this.sprites = List.of();
        this.animatedTextures = List.of();
        this.texturesByName = Map.of();
        this.missingSprite = null;
    }

    public ResourceLocation location() {
        return this.location;
    }

    public int maxSupportedTextureSize() {
        return this.maxSupportedTextureSize;
    }

    int getWidth() {
        return this.width;
    }

    int getHeight() {
        return this.height;
    }

    public Map<ResourceLocation, TextureAtlasSprite> getTextures() {
        return java.util.Collections.unmodifiableMap(texturesByName);
    }
}
