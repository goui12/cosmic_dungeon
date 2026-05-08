package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.FileUpload;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.UploadResult;
import com.mojang.realmsclient.util.UploadTokenCache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.User;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldUpload {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int UPLOAD_RETRIES = 20;
    private final RealmsClient client = RealmsClient.getOrCreate();
    private final Path worldFolder;
    private final RealmsSlot realmsSlot;
    private final User user;
    private final long realmId;
    private final RealmsWorldUploadStatusTracker statusCallback;
    private volatile boolean cancelled;
    @Nullable
    private FileUpload uploadTask;

    public RealmsWorldUpload(Path worldFolder, RealmsSlot realmsSlot, User user, long realmId, RealmsWorldUploadStatusTracker statusCallback) {
        this.worldFolder = worldFolder;
        this.realmsSlot = realmsSlot;
        this.user = user;
        this.realmId = realmId;
        this.statusCallback = statusCallback;
    }

    public CompletableFuture<?> packAndUpload() {
        return CompletableFuture.runAsync(
            () -> {
                File file1 = null;

                try {
                    UploadInfo uploadinfo = this.requestUploadInfoWithRetries();
                    file1 = RealmsUploadWorldPacker.pack(this.worldFolder, () -> this.cancelled);
                    this.statusCallback.setUploading();
                    FileUpload fileupload = new FileUpload(
                        file1,
                        this.realmId,
                        this.realmsSlot.slotId,
                        uploadinfo,
                        this.user,
                        SharedConstants.getCurrentVersion().name(),
                        this.realmsSlot.options.version,
                        this.statusCallback.getUploadStatus()
                    );
                    this.uploadTask = fileupload;
                    UploadResult uploadresult = fileupload.upload();
                    String s = uploadresult.getSimplifiedErrorMessage();
                    if (s != null) {
                        throw new RealmsUploadFailedException(s);
                    }

                    UploadTokenCache.invalidate(this.realmId);
                    this.client.updateSlot(this.realmId, this.realmsSlot.slotId, this.realmsSlot.options, this.realmsSlot.settings);
                } catch (IOException ioexception) {
                    throw new RealmsUploadFailedException(ioexception.getMessage());
                } catch (RealmsServiceException realmsserviceexception) {
                    throw new RealmsUploadFailedException(realmsserviceexception.realmsError.errorMessage());
                } catch (CancellationException | InterruptedException interruptedexception) {
                    throw new RealmsUploadCanceledException();
                } finally {
                    if (file1 != null) {
                        LOGGER.debug("Deleting file {}", file1.getAbsolutePath());
                        file1.delete();
                    }
                }
            },
            Util.backgroundExecutor()
        );
    }

    public void cancel() {
        this.cancelled = true;
        if (this.uploadTask != null) {
            this.uploadTask.cancel();
            this.uploadTask = null;
        }
    }

    private UploadInfo requestUploadInfoWithRetries() throws RealmsServiceException, InterruptedException {
        for (int i = 0; i < 20; i++) {
            try {
                UploadInfo uploadinfo = this.client.requestUploadInfo(this.realmId);
                if (this.cancelled) {
                    throw new RealmsUploadCanceledException();
                }

                if (uploadinfo != null) {
                    if (!uploadinfo.isWorldClosed()) {
                        throw new RealmsUploadWorldNotClosedException();
                    }

                    return uploadinfo;
                }
            } catch (RetryCallException retrycallexception) {
                Thread.sleep(retrycallexception.delaySeconds * 1000L);
            }
        }

        throw new RealmsUploadWorldNotClosedException();
    }
}
