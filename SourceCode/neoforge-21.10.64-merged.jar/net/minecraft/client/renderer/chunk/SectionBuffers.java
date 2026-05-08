package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SectionBuffers implements AutoCloseable {
    private GpuBuffer vertexBuffer;
    @Nullable
    private GpuBuffer indexBuffer;
    private int indexCount;
    private VertexFormat.IndexType indexType;

    public SectionBuffers(GpuBuffer vertexBuffer, @Nullable GpuBuffer indexBuffer, int indexCount, VertexFormat.IndexType indexType) {
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.indexCount = indexCount;
        this.indexType = indexType;
    }

    public GpuBuffer getVertexBuffer() {
        return this.vertexBuffer;
    }

    @Nullable
    public GpuBuffer getIndexBuffer() {
        return this.indexBuffer;
    }

    public void setIndexBuffer(@Nullable GpuBuffer indexBuffer) {
        this.indexBuffer = indexBuffer;
    }

    public int getIndexCount() {
        return this.indexCount;
    }

    public VertexFormat.IndexType getIndexType() {
        return this.indexType;
    }

    public void setIndexType(VertexFormat.IndexType indexType) {
        this.indexType = indexType;
    }

    public void setIndexCount(int indexCount) {
        this.indexCount = indexCount;
    }

    public void setVertexBuffer(GpuBuffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
        if (this.indexBuffer != null) {
            this.indexBuffer.close();
        }
    }
}
