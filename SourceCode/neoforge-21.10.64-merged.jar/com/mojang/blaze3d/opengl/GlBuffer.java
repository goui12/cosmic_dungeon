package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlBuffer extends GpuBuffer {
    protected static final MemoryPool MEMORY_POOl = TracyClient.createMemoryPool("GPU Buffers");
    protected boolean closed;
    @Nullable
    protected final Supplier<String> label;
    private final DirectStateAccess dsa;
    protected final int handle;
    @Nullable
    protected ByteBuffer persistentBuffer;

    protected GlBuffer(
        @Nullable Supplier<String> label, DirectStateAccess dsa, int usage, int size, int handle, @Nullable ByteBuffer persistentBuffer
    ) {
        super(usage, size);
        this.label = label;
        this.dsa = dsa;
        this.handle = handle;
        this.persistentBuffer = persistentBuffer;
        MEMORY_POOl.malloc(handle, size);
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.persistentBuffer != null) {
                this.dsa.unmapBuffer(this.handle, this.usage());
                this.persistentBuffer = null;
            }

            GlStateManager._glDeleteBuffers(this.handle);
            MEMORY_POOl.free(this.handle);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class GlMappedView implements GpuBuffer.MappedView {
        private final Runnable unmap;
        private final GlBuffer buffer;
        private final ByteBuffer data;
        private boolean closed;

        protected GlMappedView(Runnable unmap, GlBuffer buffer, ByteBuffer data) {
            this.unmap = unmap;
            this.buffer = buffer;
            this.data = data;
        }

        @Override
        public ByteBuffer data() {
            return this.data;
        }

        @Override
        public void close() {
            if (!this.closed) {
                this.closed = true;
                this.unmap.run();
            }
        }
    }
}
