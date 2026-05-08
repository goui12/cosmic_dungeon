package net.minecraft.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Screenshot {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SCREENSHOT_DIR = "screenshots";

    /**
     * Saves a screenshot in the game directory with a time-stamped filename.
     */
    public static void grab(File gameDirectory, RenderTarget renderTarget, Consumer<Component> messageConsumer) {
        grab(gameDirectory, null, renderTarget, 1, messageConsumer);
    }

    public static void grab(File gameDirectory, @Nullable String filename, RenderTarget renderTarget, int downscaleFactor, Consumer<Component> messageConsumer) {
        takeScreenshot(
            renderTarget,
            downscaleFactor,
            p_406180_ -> {
                File file1 = new File(gameDirectory, "screenshots");
                file1.mkdir();
                File file2;
                if (filename == null) {
                    file2 = getFile(file1);
                } else {
                    file2 = new File(file1, filename);
                }

                net.neoforged.neoforge.client.event.ScreenshotEvent event = net.neoforged.neoforge.client.ClientHooks.onScreenshot(p_406180_, file2);
                if (event.isCanceled()) {
                    messageConsumer.accept(event.getCancelMessage());
                    return;
                }
                final File target = event.getScreenshotFile();

                Util.ioPool()
                    .execute(
                        () -> {
                            try {
                                NativeImage $$4x = p_406180_;

                                try {
                                    p_406180_.writeToFile(target);
                                    if (event.getResultMessage() != null) {
                                        messageConsumer.accept(event.getResultMessage());
                                    } else {
                                    Component component = Component.literal(target.getName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(p_392495_ -> p_392495_.withClickEvent(new ClickEvent.OpenFile(target.getAbsoluteFile())));
                                    messageConsumer.accept(Component.translatable("screenshot.success", component));
                                    }
                                } catch (Throwable throwable1) {
                                    if (p_406180_ != null) {
                                        try {
                                            $$4x.close();
                                        } catch (Throwable throwable) {
                                            throwable1.addSuppressed(throwable);
                                        }
                                    }

                                    throw throwable1;
                                }

                                if (p_406180_ != null) {
                                    p_406180_.close();
                                }
                            } catch (Exception exception) {
                                LOGGER.warn("Couldn't save screenshot", (Throwable)exception);
                                messageConsumer.accept(Component.translatable("screenshot.failure", exception.getMessage()));
                            }
                        }
                    );
            }
        );
    }

    public static void takeScreenshot(RenderTarget renderTarget, Consumer<NativeImage> writer) {
        takeScreenshot(renderTarget, 1, writer);
    }

    public static void takeScreenshot(RenderTarget renderTarget, int downscaleFactor, Consumer<NativeImage> writer) {
        int i = renderTarget.width;
        int j = renderTarget.height;
        GpuTexture gputexture = renderTarget.getColorTexture();
        if (gputexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        } else if (i % downscaleFactor == 0 && j % downscaleFactor == 0) {
            GpuBuffer gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, i * j * gputexture.getFormat().pixelSize());
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToBuffer(
                    gputexture,
                    gpubuffer,
                    0,
                    () -> {
                        try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(gpubuffer, true, false)) {
                            int k = j / downscaleFactor;
                            int l = i / downscaleFactor;
                            NativeImage nativeimage = new NativeImage(l, k, false);

                            for (int i1 = 0; i1 < k; i1++) {
                                for (int j1 = 0; j1 < l; j1++) {
                                    if (downscaleFactor == 1) {
                                        int i3 = gpubuffer$mappedview.data().getInt((j1 + i1 * i) * gputexture.getFormat().pixelSize());
                                        nativeimage.setPixelABGR(j1, j - i1 - 1, i3 | 0xFF000000);
                                    } else {
                                        int k1 = 0;
                                        int l1 = 0;
                                        int i2 = 0;

                                        for (int j2 = 0; j2 < downscaleFactor; j2++) {
                                            for (int k2 = 0; k2 < downscaleFactor; k2++) {
                                                int l2 = gpubuffer$mappedview.data()
                                                    .getInt((j1 * downscaleFactor + j2 + (i1 * downscaleFactor + k2) * i) * gputexture.getFormat().pixelSize());
                                                k1 += ARGB.red(l2);
                                                l1 += ARGB.green(l2);
                                                i2 += ARGB.blue(l2);
                                            }
                                        }

                                        int j3 = downscaleFactor * downscaleFactor;
                                        nativeimage.setPixelABGR(j1, k - i1 - 1, ARGB.color(255, k1 / j3, l1 / j3, i2 / j3));
                                    }
                                }
                            }

                            writer.accept(nativeimage);
                        }

                        gpubuffer.close();
                    },
                    0
                );
        } else {
            throw new IllegalArgumentException("Image size is not divisible by downscale factor");
        }
    }

    /**
     * Creates a unique PNG file in the given directory named by a timestamp. Handles cases where the timestamp alone is not enough to create a uniquely named file, though it still might suffer from an unlikely race condition where the filename was unique when this method was called, but another process or thread created a file at the same path immediately after this method returned.
     */
    private static File getFile(File gameDirectory) {
        String s = Util.getFilenameFormattedDateTime();
        int i = 1;

        while (true) {
            File file1 = new File(gameDirectory, s + (i == 1 ? "" : "_" + i) + ".png");
            if (!file1.exists()) {
                return file1;
            }

            i++;
        }
    }
}
