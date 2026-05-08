package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GraphicsWorkarounds;
import java.nio.ByteBuffer;
import java.util.Set;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GLCapabilities;

@OnlyIn(Dist.CLIENT)
public abstract class DirectStateAccess {
    public static DirectStateAccess create(GLCapabilities capabilities, Set<String> enabledExtensions, GraphicsWorkarounds workarounds) {
        if (capabilities.GL_ARB_direct_state_access && GlDevice.USE_GL_ARB_direct_state_access && !workarounds.isGlOnDx12()) {
            enabledExtensions.add("GL_ARB_direct_state_access");
            return new DirectStateAccess.Core();
        } else {
            return new DirectStateAccess.Emulated();
        }
    }

    abstract int createBuffer();

    abstract void bufferData(int buffer, long size, int usage);

    abstract void bufferData(int buffer, ByteBuffer data, int usage);

    abstract void bufferSubData(int buffer, int offset, ByteBuffer data, int usage);

    abstract void bufferStorage(int buffer, long size, int usage);

    abstract void bufferStorage(int buffer, ByteBuffer data, int usage);

    @Nullable
    abstract ByteBuffer mapBufferRange(int buffer, int offset, int length, int access, int usage);

    abstract void unmapBuffer(int buffer, int usage);

    abstract int createFrameBufferObject();

    abstract void bindFrameBufferTextures(int frameBuffer, int colorTexture, int depthTexture, int level, int target, boolean useStencil);

    public void bindFrameBufferTextures(int frameBuffer, int colorTexture, int depthTexture, int level, int target) {
        bindFrameBufferTextures(frameBuffer, colorTexture, depthTexture, level, target, false);
    }

    abstract void blitFrameBuffers(
        int readFrameBuffer,
        int drawFrameBuffer,
        int srcX0,
        int srcY0,
        int srcX1,
        int srcY1,
        int destX0,
        int destY0,
        int destX1,
        int destY1,
        int mask,
        int filter
    );

    abstract void flushMappedBufferRange(int buffer, int offset, int length, int usage);

    abstract void copyBufferSubData(int readBuffer, int writeBuffer, int readOffset, int writeOffset, int size);

    @OnlyIn(Dist.CLIENT)
    static class Core extends DirectStateAccess {
        @Override
        int createBuffer() {
            GlStateManager.incrementTrackedBuffers();
            return ARBDirectStateAccess.glCreateBuffers();
        }

        @Override
        void bufferData(int p_418123_, long p_418371_, int p_418160_) {
            ARBDirectStateAccess.glNamedBufferData(p_418123_, p_418371_, GlConst.bufferUsageToGlEnum(p_418160_));
        }

        @Override
        void bufferData(int p_418280_, ByteBuffer p_418007_, int p_418178_) {
            ARBDirectStateAccess.glNamedBufferData(p_418280_, p_418007_, GlConst.bufferUsageToGlEnum(p_418178_));
        }

        @Override
        void bufferSubData(int p_418076_, int p_418299_, ByteBuffer p_418117_, int p_434651_) {
            ARBDirectStateAccess.glNamedBufferSubData(p_418076_, (long)p_418299_, p_418117_);
        }

        @Override
        void bufferStorage(int p_418428_, long p_418019_, int p_418289_) {
            ARBDirectStateAccess.glNamedBufferStorage(p_418428_, p_418019_, GlConst.bufferUsageToGlFlag(p_418289_));
        }

        @Override
        void bufferStorage(int p_418345_, ByteBuffer p_418031_, int p_418465_) {
            ARBDirectStateAccess.glNamedBufferStorage(p_418345_, p_418031_, GlConst.bufferUsageToGlFlag(p_418465_));
        }

        @Nullable
        @Override
        ByteBuffer mapBufferRange(int p_418027_, int p_418408_, int p_418310_, int p_418214_, int p_433284_) {
            return ARBDirectStateAccess.glMapNamedBufferRange(p_418027_, p_418408_, p_418310_, p_418214_);
        }

        @Override
        void unmapBuffer(int p_418046_, int p_433042_) {
            ARBDirectStateAccess.glUnmapNamedBuffer(p_418046_);
        }

        @Override
        public int createFrameBufferObject() {
            return ARBDirectStateAccess.glCreateFramebuffers();
        }

        @Override
        public void bindFrameBufferTextures(int p_412474_, int p_412101_, int p_412181_, int p_412742_, int p_412591_, boolean useStencil) {
            ARBDirectStateAccess.glNamedFramebufferTexture(p_412474_, 36064, p_412101_, p_412742_);
            ARBDirectStateAccess.glNamedFramebufferTexture(p_412474_, 36096, p_412181_, p_412742_);
            if (useStencil) {
                ARBDirectStateAccess.glNamedFramebufferTexture(p_412474_, org.lwjgl.opengl.GL32C.GL_STENCIL_ATTACHMENT, p_412181_, p_412742_);
            } else {
                ARBDirectStateAccess.glNamedFramebufferTexture(p_412474_, org.lwjgl.opengl.GL32C.GL_STENCIL_ATTACHMENT, 0, 0);
            }
            if (p_412591_ != 0) {
                GlStateManager._glBindFramebuffer(p_412591_, p_412474_);
            }
        }

        @Override
        public void blitFrameBuffers(
            int p_412346_,
            int p_412174_,
            int p_412752_,
            int p_412365_,
            int p_412477_,
            int p_412615_,
            int p_412700_,
            int p_412178_,
            int p_412260_,
            int p_412584_,
            int p_412685_,
            int p_412482_
        ) {
            ARBDirectStateAccess.glBlitNamedFramebuffer(
                p_412346_, p_412174_, p_412752_, p_412365_, p_412477_, p_412615_, p_412700_, p_412178_, p_412260_, p_412584_, p_412685_, p_412482_
            );
        }

        @Override
        void flushMappedBufferRange(int p_418135_, int p_418262_, int p_418080_, int p_435688_) {
            ARBDirectStateAccess.glFlushMappedNamedBufferRange(p_418135_, p_418262_, p_418080_);
        }

        @Override
        void copyBufferSubData(int p_428836_, int p_428841_, int p_428817_, int p_428818_, int p_428819_) {
            ARBDirectStateAccess.glCopyNamedBufferSubData(p_428836_, p_428841_, p_428817_, p_428818_, p_428819_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Emulated extends DirectStateAccess {
        private int selectBufferBindTarget(int usage) {
            if ((usage & 32) != 0) {
                return 34962;
            } else if ((usage & 64) != 0) {
                return 34963;
            } else {
                return (usage & 128) != 0 ? 35345 : 36663;
            }
        }

        @Override
        int createBuffer() {
            return GlStateManager._glGenBuffers();
        }

        @Override
        void bufferData(int buffer, long size, int usage) {
            int i = this.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(i, buffer);
            GlStateManager._glBufferData(i, size, GlConst.bufferUsageToGlEnum(usage));
            GlStateManager._glBindBuffer(i, 0);
        }

        @Override
        void bufferData(int buffer, ByteBuffer data, int usage) {
            int i = this.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(i, buffer);
            GlStateManager._glBufferData(i, data, GlConst.bufferUsageToGlEnum(usage));
            GlStateManager._glBindBuffer(i, 0);
        }

        @Override
        void bufferSubData(int buffer, int offset, ByteBuffer data, int usage) {
            int i = this.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(i, buffer);
            GlStateManager._glBufferSubData(i, offset, data);
            GlStateManager._glBindBuffer(i, 0);
        }

        @Override
        void bufferStorage(int buffer, long size, int usage) {
            int i = this.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(i, buffer);
            ARBBufferStorage.glBufferStorage(i, size, GlConst.bufferUsageToGlFlag(usage));
            GlStateManager._glBindBuffer(i, 0);
        }

        @Override
        void bufferStorage(int buffer, ByteBuffer data, int usage) {
            int i = this.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(i, buffer);
            ARBBufferStorage.glBufferStorage(i, data, GlConst.bufferUsageToGlFlag(usage));
            GlStateManager._glBindBuffer(i, 0);
        }

        @Nullable
        @Override
        ByteBuffer mapBufferRange(int buffer, int offset, int length, int access, int usage) {
            int i = this.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(i, buffer);
            ByteBuffer bytebuffer = GlStateManager._glMapBufferRange(i, offset, length, access);
            GlStateManager._glBindBuffer(i, 0);
            return bytebuffer;
        }

        @Override
        void unmapBuffer(int buffer, int usage) {
            int i = this.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(i, buffer);
            GlStateManager._glUnmapBuffer(i);
            GlStateManager._glBindBuffer(i, 0);
        }

        @Override
        void flushMappedBufferRange(int buffer, int offset, int length, int usage) {
            int i = this.selectBufferBindTarget(usage);
            GlStateManager._glBindBuffer(i, buffer);
            GL30.glFlushMappedBufferRange(i, offset, length);
            GlStateManager._glBindBuffer(i, 0);
        }

        @Override
        void copyBufferSubData(int readBuffer, int writeBuffer, int readOffset, int writeOffset, int size) {
            GlStateManager._glBindBuffer(36662, readBuffer);
            GlStateManager._glBindBuffer(36663, writeBuffer);
            GL31.glCopyBufferSubData(36662, 36663, readOffset, writeOffset, size);
            GlStateManager._glBindBuffer(36662, 0);
            GlStateManager._glBindBuffer(36663, 0);
        }

        @Override
        public int createFrameBufferObject() {
            return GlStateManager.glGenFramebuffers();
        }

        @Override
        public void bindFrameBufferTextures(int frameBuffer, int colorTexture, int depthTexture, int level, int target, boolean useStencil) {
            int i = target == 0 ? '\u8ca9' : target;
            int j = GlStateManager.getFrameBuffer(i);
            GlStateManager._glBindFramebuffer(i, frameBuffer);
            GlStateManager._glFramebufferTexture2D(i, 36064, 3553, colorTexture, level);
            GlStateManager._glFramebufferTexture2D(i, 36096, 3553, depthTexture, level);
            if (useStencil) {
                GlStateManager._glFramebufferTexture2D(i, org.lwjgl.opengl.GL32C.GL_STENCIL_ATTACHMENT, 3553, depthTexture, level);
            } else {
                GlStateManager._glFramebufferTexture2D(i, org.lwjgl.opengl.GL32C.GL_STENCIL_ATTACHMENT, 3553, 0, 0);
            }
            if (target == 0) {
                GlStateManager._glBindFramebuffer(i, j);
            }
        }

        @Override
        public void blitFrameBuffers(
            int readFrameBuffer,
            int drawFrameBuffer,
            int srcX0,
            int srcY0,
            int srcX1,
            int srcY1,
            int destX0,
            int destY0,
            int destX1,
            int destY1,
            int mask,
            int filter
        ) {
            int i = GlStateManager.getFrameBuffer(36008);
            int j = GlStateManager.getFrameBuffer(36009);
            GlStateManager._glBindFramebuffer(36008, readFrameBuffer);
            GlStateManager._glBindFramebuffer(36009, drawFrameBuffer);
            GlStateManager._glBlitFrameBuffer(srcX0, srcY0, srcX1, srcY1, destX0, destY0, destX1, destY1, mask, filter);
            GlStateManager._glBindFramebuffer(36008, i);
            GlStateManager._glBindFramebuffer(36009, j);
        }
    }
}
