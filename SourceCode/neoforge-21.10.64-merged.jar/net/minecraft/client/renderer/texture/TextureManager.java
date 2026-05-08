package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureManager implements PreparableReloadListener, Tickable, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation INTENTIONAL_MISSING_TEXTURE = ResourceLocation.withDefaultNamespace("");
    private final Map<ResourceLocation, AbstractTexture> byPath = new HashMap<>();
    private final Set<Tickable> tickableTextures = new HashSet<>();
    private final ResourceManager resourceManager;

    public TextureManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        NativeImage nativeimage = MissingTextureAtlasSprite.generateMissingImage();
        this.register(MissingTextureAtlasSprite.getLocation(), new DynamicTexture(() -> "(intentionally-)Missing Texture", nativeimage));
    }

    public void registerAndLoad(ResourceLocation textureId, ReloadableTexture texture) {
        try {
            texture.apply(this.loadContentsSafe(textureId, texture));
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Uploading texture");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Uploaded texture");
            crashreportcategory.setDetail("Resource location", texture.resourceId());
            crashreportcategory.setDetail("Texture id", textureId);
            throw new ReportedException(crashreport);
        }

        this.register(textureId, texture);
    }

    private TextureContents loadContentsSafe(ResourceLocation textureId, ReloadableTexture texture) {
        try {
            return loadContents(this.resourceManager, textureId, texture);
        } catch (Exception exception) {
            LOGGER.error("Failed to load texture {} into slot {}", texture.resourceId(), textureId, exception);
            return TextureContents.createMissing();
        }
    }

    public void registerForNextReload(ResourceLocation textureId) {
        this.register(textureId, new SimpleTexture(textureId));
    }

    public void register(ResourceLocation path, AbstractTexture texture) {
        AbstractTexture abstracttexture = this.byPath.put(path, texture);
        if (abstracttexture != texture) {
            if (abstracttexture != null) {
                this.safeClose(path, abstracttexture);
            }

            if (texture instanceof Tickable tickable) {
                this.tickableTextures.add(tickable);
            }
        }
    }

    private void safeClose(ResourceLocation path, AbstractTexture texture) {
        this.tickableTextures.remove(texture);

        try {
            texture.close();
        } catch (Exception exception) {
            LOGGER.warn("Failed to close texture {}", path, exception);
        }
    }

    public AbstractTexture getTexture(ResourceLocation path) {
        AbstractTexture abstracttexture = this.byPath.get(path);
        if (abstracttexture != null) {
            return abstracttexture;
        } else {
            SimpleTexture simpletexture = new SimpleTexture(path);
            this.registerAndLoad(path, simpletexture);
            return simpletexture;
        }
    }

    @Override
    public void tick() {
        for (Tickable tickable : this.tickableTextures) {
            tickable.tick();
        }
    }

    public void release(ResourceLocation path) {
        AbstractTexture abstracttexture = this.byPath.remove(path);
        if (abstracttexture != null) {
            this.safeClose(path, abstracttexture);
        }
    }

    @Override
    public void close() {
        this.byPath.forEach(this::safeClose);
        this.byPath.clear();
        this.tickableTextures.clear();
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState, Executor exectutor, PreparableReloadListener.PreparationBarrier barrier, Executor applyExectutor
    ) {
        ResourceManager resourcemanager = sharedState.resourceManager();
        List<TextureManager.PendingReload> list = new ArrayList<>();
        this.byPath.forEach((p_389356_, p_389357_) -> {
            if (p_389357_ instanceof ReloadableTexture reloadabletexture) {
                list.add(scheduleLoad(resourcemanager, p_389356_, reloadabletexture, exectutor));
            }
        });
        return CompletableFuture.allOf(list.stream().map(TextureManager.PendingReload::newContents).toArray(CompletableFuture[]::new))
            .thenCompose(barrier::wait)
            .thenAcceptAsync(p_389351_ -> {
                AddRealmPopupScreen.updateCarouselImages(this.resourceManager);

                for (TextureManager.PendingReload texturemanager$pendingreload : list) {
                    texturemanager$pendingreload.texture.apply(texturemanager$pendingreload.newContents.join());
                }
            }, applyExectutor);
    }

    public void dumpAllSheets(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ioexception) {
            LOGGER.error("Failed to create directory {}", path, ioexception);
            return;
        }

        this.byPath.forEach((p_276101_, p_276102_) -> {
            if (p_276102_ instanceof Dumpable dumpable) {
                try {
                    dumpable.dumpContents(p_276101_, path);
                } catch (IOException ioexception1) {
                    LOGGER.error("Failed to dump texture {}", p_276101_, ioexception1);
                }
            }
        });
    }

    private static TextureContents loadContents(ResourceManager resourceManager, ResourceLocation textureId, ReloadableTexture texture) throws IOException {
        try {
            return texture.loadContents(resourceManager);
        } catch (FileNotFoundException filenotfoundexception) {
            if (textureId != INTENTIONAL_MISSING_TEXTURE) {
                LOGGER.warn("Missing resource {} referenced from {}", texture.resourceId(), textureId);
            }

            return TextureContents.createMissing();
        }
    }

    private static TextureManager.PendingReload scheduleLoad(
        ResourceManager resourceManager, ResourceLocation textureId, ReloadableTexture texture, Executor executor
    ) {
        return new TextureManager.PendingReload(texture, CompletableFuture.supplyAsync(() -> {
            try {
                return loadContents(resourceManager, textureId, texture);
            } catch (IOException ioexception) {
                throw new UncheckedIOException(ioexception);
            }
        }, executor));
    }

    @OnlyIn(Dist.CLIENT)
    record PendingReload(ReloadableTexture texture, CompletableFuture<TextureContents> newContents) {
    }
}
