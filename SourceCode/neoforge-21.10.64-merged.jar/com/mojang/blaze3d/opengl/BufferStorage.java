package com.mojang.blaze3d.opengl;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public abstract class BufferStorage {
    public static BufferStorage create(GLCapabilities capabilities, Set<String> enabledExtensions) {
        if (capabilities.GL_ARB_buffer_storage && GlDevice.USE_GL_ARB_buffer_storage) {
            enabledExtensions.add("GL_ARB_buffer_storage");
            return new BufferStorage.Immutable();
        } else {
            return new BufferStorage.Mutable();
        }
    }

    public abstract GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, int usage, int size);

    public abstract GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, int usage, ByteBuffer data);

    public abstract GlBuffer.GlMappedView mapBuffer(DirectStateAccess directStateAccess, GlBuffer buffer, int offset, int size, int usage);

    @OnlyIn(Dist.CLIENT)
    static class Immutable extends BufferStorage {
        @Override
        public GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, int usage, int size) {
            int i = directStateAccess.createBuffer();
            directStateAccess.bufferStorage(i, size, usage);
            ByteBuffer bytebuffer = this.tryMapBufferPersistent(directStateAccess, usage, i, size);
            return new GlBuffer(label, directStateAccess, usage, size, i, bytebuffer);
        }

        @Override
        public GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, int usage, ByteBuffer data) {
            int i = directStateAccess.createBuffer();
            int j = data.remaining();
            directStateAccess.bufferStorage(i, data, usage);
            ByteBuffer bytebuffer = this.tryMapBufferPersistent(directStateAccess, usage, i, j);
            return new GlBuffer(label, directStateAccess, usage, j, i, bytebuffer);
        }

        @Nullable
        private ByteBuffer tryMapBufferPersistent(DirectStateAccess directStateAccess, int usage, int handle, int size) {
            int i = 0;
            if ((usage & 1) != 0) {
                i |= 1;
            }

            if ((usage & 2) != 0) {
                i |= 18;
            }

            ByteBuffer bytebuffer;
            if (i != 0) {
                GlStateManager.clearGlErrors();
                bytebuffer = directStateAccess.mapBufferRange(handle, 0, size, i | 64, usage);
                if (bytebuffer == null) {
                    throw new IllegalStateException("Can't persistently map buffer, opengl error " + GlStateManager._getError());
                }
            } else {
                bytebuffer = null;
            }

            return bytebuffer;
        }

        @Override
        public GlBuffer.GlMappedView mapBuffer(DirectStateAccess directStateAccess, GlBuffer buffer, int offset, int size, int usage) {
            if (buffer.persistentBuffer == null) {
                throw new IllegalStateException("Somehow trying to map an unmappable buffer");
            } else {
                return new GlBuffer.GlMappedView(() -> {
                    if ((usage & 2) != 0) {
                        directStateAccess.flushMappedBufferRange(buffer.handle, offset, size, buffer.usage());
                    }
                }, buffer, MemoryUtil.memSlice(buffer.persistentBuffer, offset, size));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Mutable extends BufferStorage {
        @Override
        public GlBuffer createBuffer(DirectStateAccess p_418247_, @Nullable Supplier<String> p_418177_, int p_418191_, int p_418309_) {
            int i = p_418247_.createBuffer();
            p_418247_.bufferData(i, p_418309_, p_418191_);
            return new GlBuffer(p_418177_, p_418247_, p_418191_, p_418309_, i, null);
        }

        @Override
        public GlBuffer createBuffer(DirectStateAccess p_418102_, @Nullable Supplier<String> p_418521_, int p_418167_, ByteBuffer p_418232_) {
            int i = p_418102_.createBuffer();
            int j = p_418232_.remaining();
            p_418102_.bufferData(i, p_418232_, p_418167_);
            return new GlBuffer(p_418521_, p_418102_, p_418167_, j, i, null);
        }

        @Override
        public GlBuffer.GlMappedView mapBuffer(DirectStateAccess p_418209_, GlBuffer p_418012_, int p_418273_, int p_418050_, int p_418422_) {
            GlStateManager.clearGlErrors();
            ByteBuffer bytebuffer = p_418209_.mapBufferRange(p_418012_.handle, p_418273_, p_418050_, p_418422_, p_418012_.usage());
            if (bytebuffer == null) {
                throw new IllegalStateException("Can't map buffer, opengl error " + GlStateManager._getError());
            } else {
                return new GlBuffer.GlMappedView(() -> p_418209_.unmapBuffer(p_418012_.handle, p_418012_.usage()), p_418012_, bytebuffer);
            }
        }
    }
}
