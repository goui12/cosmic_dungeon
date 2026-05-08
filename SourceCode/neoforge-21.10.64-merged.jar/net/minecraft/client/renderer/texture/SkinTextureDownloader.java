package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SkinTextureDownloader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SKIN_WIDTH = 64;
    private static final int SKIN_HEIGHT = 64;
    private static final int LEGACY_SKIN_HEIGHT = 32;
    private final Proxy proxy;
    private final TextureManager textureManager;
    private final Executor mainThreadExecutor;

    public SkinTextureDownloader(Proxy proxy, TextureManager textureManager, Executor mainThreadExecutor) {
        this.proxy = proxy;
        this.textureManager = textureManager;
        this.mainThreadExecutor = mainThreadExecutor;
    }

    public CompletableFuture<ClientAsset.Texture> downloadAndRegisterSkin(ResourceLocation textureLocation, Path path, String url, boolean isLegacySkin) {
        ClientAsset.DownloadedTexture clientasset$downloadedtexture = new ClientAsset.DownloadedTexture(textureLocation, url);
        return CompletableFuture.<NativeImage>supplyAsync(() -> {
                NativeImage nativeimage;
                try {
                    nativeimage = this.downloadSkin(path, clientasset$downloadedtexture.url());
                } catch (IOException ioexception) {
                    throw new UncheckedIOException(ioexception);
                }

                return isLegacySkin ? processLegacySkin(nativeimage, clientasset$downloadedtexture.url()) : nativeimage;
            }, Util.nonCriticalIoPool().forName("downloadTexture"))
            .thenCompose(p_450741_ -> this.registerTextureInManager(clientasset$downloadedtexture, p_450741_));
    }

    private NativeImage downloadSkin(Path path, String url) throws IOException {
        if (Files.isRegularFile(path)) {
            LOGGER.debug("Loading HTTP texture from local cache ({})", path);

            NativeImage nativeimage1;
            try (InputStream inputstream = Files.newInputStream(path)) {
                nativeimage1 = NativeImage.read(inputstream);
            }

            return nativeimage1;
        } else {
            HttpURLConnection httpurlconnection = null;
            LOGGER.debug("Downloading HTTP texture from {} to {}", url, path);
            URI uri = URI.create(url);

            NativeImage $$7;
            try {
                httpurlconnection = (HttpURLConnection)uri.toURL().openConnection(this.proxy);
                httpurlconnection.setDoInput(true);
                httpurlconnection.setDoOutput(false);
                httpurlconnection.connect();
                int i = httpurlconnection.getResponseCode();
                if (i / 100 != 2) {
                    throw new IOException("Failed to open " + uri + ", HTTP error code: " + i);
                }

                byte[] abyte = httpurlconnection.getInputStream().readAllBytes();

                try {
                    FileUtil.createDirectoriesSafe(path.getParent());
                    Files.write(path, abyte);
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to cache texture {} in {}", url, path);
                }

                $$7 = NativeImage.read(abyte);
            } finally {
                if (httpurlconnection != null) {
                    httpurlconnection.disconnect();
                }
            }

            return $$7;
        }
    }

    private CompletableFuture<ClientAsset.Texture> registerTextureInManager(ClientAsset.Texture texture, NativeImage image) {
        return CompletableFuture.supplyAsync(() -> {
            DynamicTexture dynamictexture = new DynamicTexture(texture.texturePath()::toString, image);
            this.textureManager.register(texture.texturePath(), dynamictexture);
            return texture;
        }, this.mainThreadExecutor);
    }

    private static NativeImage processLegacySkin(NativeImage image, String url) {
        int i = image.getHeight();
        int j = image.getWidth();
        if (j == 64 && (i == 32 || i == 64)) {
            boolean flag = i == 32;
            if (flag) {
                NativeImage nativeimage = new NativeImage(64, 64, true);
                nativeimage.copyFrom(image);
                image.close();
                image = nativeimage;
                nativeimage.fillRect(0, 32, 64, 32, 0);
                nativeimage.copyRect(4, 16, 16, 32, 4, 4, true, false);
                nativeimage.copyRect(8, 16, 16, 32, 4, 4, true, false);
                nativeimage.copyRect(0, 20, 24, 32, 4, 12, true, false);
                nativeimage.copyRect(4, 20, 16, 32, 4, 12, true, false);
                nativeimage.copyRect(8, 20, 8, 32, 4, 12, true, false);
                nativeimage.copyRect(12, 20, 16, 32, 4, 12, true, false);
                nativeimage.copyRect(44, 16, -8, 32, 4, 4, true, false);
                nativeimage.copyRect(48, 16, -8, 32, 4, 4, true, false);
                nativeimage.copyRect(40, 20, 0, 32, 4, 12, true, false);
                nativeimage.copyRect(44, 20, -8, 32, 4, 12, true, false);
                nativeimage.copyRect(48, 20, -16, 32, 4, 12, true, false);
                nativeimage.copyRect(52, 20, -8, 32, 4, 12, true, false);
            }

            setNoAlpha(image, 0, 0, 32, 16);
            if (flag) {
                doNotchTransparencyHack(image, 32, 0, 64, 32);
            }

            setNoAlpha(image, 0, 16, 64, 32);
            setNoAlpha(image, 16, 48, 48, 64);
            return image;
        } else {
            image.close();
            throw new IllegalStateException("Discarding incorrectly sized (" + j + "x" + i + ") skin texture from " + url);
        }
    }

    private static void doNotchTransparencyHack(NativeImage image, int minX, int minY, int maxX, int maxY) {
        for (int i = minX; i < maxX; i++) {
            for (int j = minY; j < maxY; j++) {
                int k = image.getPixel(i, j);
                if (ARGB.alpha(k) < 128) {
                    return;
                }
            }
        }

        for (int l = minX; l < maxX; l++) {
            for (int i1 = minY; i1 < maxY; i1++) {
                image.setPixel(l, i1, image.getPixel(l, i1) & 16777215);
            }
        }
    }

    private static void setNoAlpha(NativeImage image, int minX, int minY, int maxX, int maxY) {
        for (int i = minX; i < maxX; i++) {
            for (int j = minY; j < maxY; j++) {
                image.setPixel(i, j, ARGB.opaque(image.getPixel(i, j)));
            }
        }
    }
}
