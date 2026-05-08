package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface RenderPass extends AutoCloseable {
    void pushDebugGroup(Supplier<String> name);

    void popDebugGroup();

    void setPipeline(RenderPipeline pipeline);

    void bindSampler(String name, @Nullable GpuTextureView texture);

    void setUniform(String name, GpuBuffer buffer);

    void setUniform(String name, GpuBufferSlice bufferSlice);

    void setViewport(int x, int y, int width, int height);

    void enableScissor(int x, int y, int width, int height);

    void disableScissor();

    void setVertexBuffer(int index, GpuBuffer buffer);

    void setIndexBuffer(GpuBuffer indexBuffer, VertexFormat.IndexType indexType);

    void drawIndexed(int firstIndex, int index, int indexCount, int primCount);

    <T> void drawMultipleIndexed(
        Collection<RenderPass.Draw<T>> draws,
        @Nullable GpuBuffer indexBuffer,
        @Nullable VertexFormat.IndexType indexType,
        Collection<String> uniformNames,
        T userData
    );

    void draw(int firstIndex, int indexCount);

    @Override
    void close();

    @OnlyIn(Dist.CLIENT)
    public record Draw<T>(
        int slot,
        GpuBuffer vertexBuffer,
        @Nullable GpuBuffer indexBuffer,
        @Nullable VertexFormat.IndexType indexType,
        int firstIndex,
        int indexCount,
        @Nullable BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer
    ) {
        public Draw(int p_409953_, GpuBuffer p_410253_, GpuBuffer p_410419_, VertexFormat.IndexType p_410538_, int p_410203_, int p_410514_) {
            this(p_409953_, p_410253_, p_410419_, p_410538_, p_410203_, p_410514_, null);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface UniformUploader {
        void upload(String name, GpuBufferSlice bufferSlice);
    }
}
