package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface CommandEncoder {
    RenderPass createRenderPass(Supplier<String> debugGroup, GpuTextureView colorTexture, OptionalInt clearColor);

    RenderPass createRenderPass(
        Supplier<String> debugGroup, GpuTextureView colorTexture, OptionalInt clearColor, @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth
    );

    void clearColorTexture(GpuTexture texture, int color);

    void clearColorAndDepthTextures(GpuTexture colorTexture, int clearColor, GpuTexture depthTexture, double clearDepth);

    void clearColorAndDepthTextures(
        GpuTexture colorTexture, int clearColor, GpuTexture depthTexture, double clearDepth, int scissorX, int scissorY, int scissorWidth, int scissorHeight
    );

    void clearDepthTexture(GpuTexture depthTexture, double clearDepth);

    void clearStencilTexture(GpuTexture texture, int value);

    void writeToBuffer(GpuBufferSlice slice, ByteBuffer buffer);

    GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write);

    GpuBuffer.MappedView mapBuffer(GpuBufferSlice slice, boolean read, boolean write);

    void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target);

    void writeToTexture(GpuTexture texture, NativeImage image);

    void writeToTexture(
        GpuTexture texture,
        NativeImage image,
        int mipLevel,
        int depthOrLayer,
        int x,
        int y,
        int width,
        int height,
        int sourceX,
        int sourceY
    );

    void writeToTexture(
        GpuTexture texture,
        ByteBuffer buffer,
        NativeImage.Format format,
        int mipLevel,
        int depthOrLayer,
        int x,
        int y,
        int width,
        int height
    );

    void copyTextureToBuffer(GpuTexture texture, GpuBuffer buffer, int offset, Runnable task, int mipLevel);

    void copyTextureToBuffer(
        GpuTexture texture, GpuBuffer buffer, int offset, Runnable task, int mipLevel, int x, int y, int width, int height
    );

    void copyTextureToTexture(
        GpuTexture source, GpuTexture destination, int mipLevel, int x, int y, int sourceX, int sourceY, int width, int height
    );

    void presentTexture(GpuTextureView texture);

    GpuFence createFence();
}
