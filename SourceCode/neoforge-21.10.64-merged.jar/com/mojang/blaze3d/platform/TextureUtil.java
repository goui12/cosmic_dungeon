package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class TextureUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MIN_MIPMAP_LEVEL = 0;
    private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

    public static ByteBuffer readResource(InputStream inputStream) throws IOException {
        ReadableByteChannel readablebytechannel = Channels.newChannel(inputStream);
        return readablebytechannel instanceof SeekableByteChannel seekablebytechannel
            ? readResource(readablebytechannel, (int)seekablebytechannel.size() + 1)
            : readResource(readablebytechannel, 8192);
    }

    private static ByteBuffer readResource(ReadableByteChannel channel, int size) throws IOException {
        ByteBuffer bytebuffer = MemoryUtil.memAlloc(size);

        try {
            while (channel.read(bytebuffer) != -1) {
                if (!bytebuffer.hasRemaining()) {
                    bytebuffer = MemoryUtil.memRealloc(bytebuffer, bytebuffer.capacity() * 2);
                }
            }

            return bytebuffer;
        } catch (IOException ioexception) {
            MemoryUtil.memFree(bytebuffer);
            throw ioexception;
        }
    }

    public static void writeAsPNG(Path p_path, String filename, GpuTexture texture, int mipLevel, IntUnaryOperator pixelUpdater) {
        RenderSystem.assertOnRenderThread();
        int i = 0;

        for (int j = 0; j <= mipLevel; j++) {
            i += texture.getFormat().pixelSize() * texture.getWidth(j) * texture.getHeight(j);
        }

        GpuBuffer gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", 9, i);
        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
        Runnable runnable = () -> {
            try (GpuBuffer.MappedView gpubuffer$mappedview = commandencoder.mapBuffer(gpubuffer, true, false)) {
                int i1 = 0;

                for (int j1 = 0; j1 <= mipLevel; j1++) {
                    int k1 = texture.getWidth(j1);
                    int l1 = texture.getHeight(j1);

                    try (NativeImage nativeimage = new NativeImage(k1, l1, false)) {
                        for (int i2 = 0; i2 < l1; i2++) {
                            for (int j2 = 0; j2 < k1; j2++) {
                                int k2 = gpubuffer$mappedview.data().getInt(i1 + (j2 + i2 * k1) * texture.getFormat().pixelSize());
                                nativeimage.setPixelABGR(j2, i2, pixelUpdater.applyAsInt(k2));
                            }
                        }

                        Path path = p_path.resolve(filename + "_" + j1 + ".png");
                        nativeimage.writeToFile(path);
                        LOGGER.debug("Exported png to: {}", path.toAbsolutePath());
                    } catch (IOException ioexception) {
                        LOGGER.debug("Unable to write: ", (Throwable)ioexception);
                    }

                    i1 += texture.getFormat().pixelSize() * k1 * l1;
                }
            }

            gpubuffer.close();
        };
        AtomicInteger atomicinteger = new AtomicInteger();
        int k = 0;

        for (int l = 0; l <= mipLevel; l++) {
            commandencoder.copyTextureToBuffer(texture, gpubuffer, k, () -> {
                if (atomicinteger.getAndIncrement() == mipLevel) {
                    runnable.run();
                }
            }, l);
            k += texture.getFormat().pixelSize() * texture.getWidth(l) * texture.getHeight(l);
        }
    }

    public static Path getDebugTexturePath(Path basePath) {
        return basePath.resolve("screenshots").resolve("debug");
    }

    public static Path getDebugTexturePath() {
        return getDebugTexturePath(Path.of("."));
    }
}
