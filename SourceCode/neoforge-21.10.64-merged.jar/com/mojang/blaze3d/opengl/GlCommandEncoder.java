package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlCommandEncoder implements CommandEncoder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final GlDevice device;
    private final int readFbo;
    private final int drawFbo;
    @Nullable
    private RenderPipeline lastPipeline;
    private boolean inRenderPass;
    @Nullable
    private GlProgram lastProgram;

    protected GlCommandEncoder(GlDevice device) {
        this.device = device;
        this.readFbo = device.directStateAccess().createFrameBufferObject();
        this.drawFbo = device.directStateAccess().createFrameBufferObject();
    }

    @Override
    public RenderPass createRenderPass(Supplier<String> debugGroup, GpuTextureView colorTexture, OptionalInt clearColor) {
        return this.createRenderPass(debugGroup, colorTexture, clearColor, null, OptionalDouble.empty());
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> debugGroup, GpuTextureView colorTexture, OptionalInt clearColor, @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            if (clearDepth.isPresent() && depthTexture == null) {
                LOGGER.warn("Depth clear value was provided but no depth texture is being used");
            }

            if (colorTexture.isClosed()) {
                throw new IllegalStateException("Color texture is closed");
            } else if ((colorTexture.texture().usage() & 8) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
            } else if (colorTexture.texture().getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
            } else {
                if (depthTexture != null) {
                    if (depthTexture.isClosed()) {
                        throw new IllegalStateException("Depth texture is closed");
                    }

                    if ((depthTexture.texture().usage() & 8) == 0) {
                        throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
                    }

                    if (depthTexture.texture().getDepthOrLayers() > 1) {
                        throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
                    }
                }

                this.inRenderPass = true;
                this.device.debugLabels().pushDebugGroup(debugGroup);
                int i = ((GlTexture)colorTexture.texture()).getFbo(this.device.directStateAccess(), depthTexture == null ? null : depthTexture.texture());
                GlStateManager._glBindFramebuffer(36160, i);
                int j = 0;
                if (clearColor.isPresent()) {
                    int k = clearColor.getAsInt();
                    GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
                    j |= 16384;
                }

                if (depthTexture != null && clearDepth.isPresent()) {
                    GL11.glClearDepth(clearDepth.getAsDouble());
                    j |= 256;
                }

                if (j != 0) {
                    GlStateManager._disableScissorTest();
                    GlStateManager._depthMask(true);
                    GlStateManager._colorMask(true, true, true, true);
                    GlStateManager._clear(j);
                }

                GlStateManager._viewport(0, 0, colorTexture.getWidth(0), colorTexture.getHeight(0));
                this.lastPipeline = null;
                return new GlRenderPass(this, depthTexture != null);
            }
        }
    }

    @Override
    public void clearColorTexture(GpuTexture texture, int color) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            this.verifyColorTexture(texture);
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)texture).id, 0, 0, 36160);
            GL11.glClearColor(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
            GlStateManager._disableScissorTest();
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._clear(16384);
            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, int clearColor, GpuTexture depthTexture, double clearDepth) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            this.verifyColorTexture(colorTexture);
            this.verifyDepthTexture(depthTexture);
            int i = ((GlTexture)colorTexture).getFbo(this.device.directStateAccess(), depthTexture);
            GlStateManager._glBindFramebuffer(36160, i);
            GlStateManager._disableScissorTest();
            GL11.glClearDepth(clearDepth);
            GL11.glClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._clear(16640);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture, int clearColor, GpuTexture depthTexture, double clearDepth, int scissorX, int scissorY, int scissorWidth, int scissorHeight
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            this.verifyColorTexture(colorTexture);
            this.verifyDepthTexture(depthTexture);
            this.verifyRegion(colorTexture, scissorX, scissorY, scissorWidth, scissorHeight);
            int i = ((GlTexture)colorTexture).getFbo(this.device.directStateAccess(), depthTexture);
            GlStateManager._glBindFramebuffer(36160, i);
            GlStateManager._scissorBox(scissorX, scissorY, scissorWidth, scissorHeight);
            GlStateManager._enableScissorTest();
            GL11.glClearDepth(clearDepth);
            GL11.glClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._clear(16640);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    private void verifyRegion(GpuTexture texture, int regionX, int regionY, int regionWidth, int regionHeight) {
        if (regionX < 0 || regionX >= texture.getWidth(0)) {
            throw new IllegalArgumentException("regionX should not be outside of the texture");
        } else if (regionY < 0 || regionY >= texture.getHeight(0)) {
            throw new IllegalArgumentException("regionY should not be outside of the texture");
        } else if (regionWidth <= 0) {
            throw new IllegalArgumentException("regionWidth should be greater than 0");
        } else if (regionX + regionWidth > texture.getWidth(0)) {
            throw new IllegalArgumentException("regionWidth + regionX should be less than the texture width");
        } else if (regionHeight <= 0) {
            throw new IllegalArgumentException("regionHeight should be greater than 0");
        } else if (regionY + regionHeight > texture.getHeight(0)) {
            throw new IllegalArgumentException("regionWidth + regionX should be less than the texture height");
        }
    }

    @Override
    public void clearDepthTexture(GpuTexture depthTexture, double clearDepth) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            this.verifyDepthTexture(depthTexture);
            boolean hasStencil = depthTexture.getFormat().hasStencilAspect();
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)depthTexture).id, 0, 36160, hasStencil);
            GL11.glDrawBuffer(0);
            GL11.glClearDepth(clearDepth);
            GlStateManager._depthMask(true);
            GlStateManager._disableScissorTest();
            GlStateManager._clear(256);
            GL11.glDrawBuffer(36064);
            GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void clearStencilTexture(GpuTexture texture, int value) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else if (!texture.getFormat().hasStencilAspect()) {
            throw new IllegalStateException("Trying to clear stencil in a texture that has no stencil component!");
        } else {
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)texture).id, 0, GlConst.GL_FRAMEBUFFER, true);
            GL11.glDrawBuffer(GlConst.GL_NONE);
            GL11.glClearStencil(value);
            GlStateManager._depthMask(true);
            GlStateManager._clear(GL11.GL_STENCIL_BUFFER_BIT);
            GL11.glDrawBuffer(GlConst.GL_COLOR_ATTACHMENT0);
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
        }
    }

    private void verifyColorTexture(GpuTexture texture) {
        if (!texture.getFormat().hasColorAspect()) {
            throw new IllegalStateException("Trying to clear a non-color texture as color");
        } else if (texture.isClosed()) {
            throw new IllegalStateException("Color texture is closed");
        } else if ((texture.usage() & 8) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
        } else if (texture.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
        }
    }

    private void verifyDepthTexture(GpuTexture texture) {
        if (!texture.getFormat().hasDepthAspect()) {
            throw new IllegalStateException("Trying to clear a non-depth texture as depth");
        } else if (texture.isClosed()) {
            throw new IllegalStateException("Depth texture is closed");
        } else if ((texture.usage() & 8) == 0) {
            throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
        } else if (texture.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
        }
    }

    @Override
    public void writeToBuffer(GpuBufferSlice slice, ByteBuffer buffer) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            GlBuffer glbuffer = (GlBuffer)slice.buffer();
            if (glbuffer.closed) {
                throw new IllegalStateException("Buffer already closed");
            } else if ((glbuffer.usage() & 8) == 0) {
                throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
            } else {
                int i = buffer.remaining();
                if (i > slice.length()) {
                    throw new IllegalArgumentException(
                        "Cannot write more data than the slice allows (attempting to write " + i + " bytes into a slice of length " + slice.length() + ")"
                    );
                } else if (slice.length() + slice.offset() > glbuffer.size()) {
                    throw new IllegalArgumentException(
                        "Cannot write more data than this buffer can hold (attempting to write "
                            + i
                            + " bytes at offset "
                            + slice.offset()
                            + " to "
                            + glbuffer.size()
                            + " size buffer)"
                    );
                } else {
                    this.device.directStateAccess().bufferSubData(glbuffer.handle, slice.offset(), buffer, glbuffer.usage());
                }
            }
        }
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write) {
        return this.mapBuffer(buffer.slice(), read, write);
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBufferSlice slice, boolean read, boolean write) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            GlBuffer glbuffer = (GlBuffer)slice.buffer();
            if (glbuffer.closed) {
                throw new IllegalStateException("Buffer already closed");
            } else if (!read && !write) {
                throw new IllegalArgumentException("At least read or write must be true");
            } else if (read && (glbuffer.usage() & 1) == 0) {
                throw new IllegalStateException("Buffer is not readable");
            } else if (write && (glbuffer.usage() & 2) == 0) {
                throw new IllegalStateException("Buffer is not writable");
            } else if (slice.offset() + slice.length() > glbuffer.size()) {
                throw new IllegalArgumentException(
                    "Cannot map more data than this buffer can hold (attempting to map "
                        + slice.length()
                        + " bytes at offset "
                        + slice.offset()
                        + " from "
                        + glbuffer.size()
                        + " size buffer)"
                );
            } else {
                int i = 0;
                if (read) {
                    i |= 1;
                }

                if (write) {
                    i |= 34;
                }

                return this.device.getBufferStorage().mapBuffer(this.device.directStateAccess(), glbuffer, slice.offset(), slice.length(), i);
            }
        }
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            GlBuffer glbuffer = (GlBuffer)source.buffer();
            if (glbuffer.closed) {
                throw new IllegalStateException("Source buffer already closed");
            } else if ((glbuffer.usage() & 16) == 0) {
                throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
            } else {
                GlBuffer glbuffer1 = (GlBuffer)target.buffer();
                if (glbuffer1.closed) {
                    throw new IllegalStateException("Target buffer already closed");
                } else if ((glbuffer1.usage() & 8) == 0) {
                    throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
                } else if (source.length() != target.length()) {
                    throw new IllegalArgumentException(
                        "Cannot copy from slice of size " + source.length() + " to slice of size " + target.length() + ", they must be equal"
                    );
                } else if (source.offset() + source.length() > glbuffer.size()) {
                    throw new IllegalArgumentException(
                        "Cannot copy more data than the source buffer holds (attempting to copy "
                            + source.length()
                            + " bytes at offset "
                            + source.offset()
                            + " from "
                            + glbuffer.size()
                            + " size buffer)"
                    );
                } else if (target.offset() + target.length() > glbuffer1.size()) {
                    throw new IllegalArgumentException(
                        "Cannot copy more data than the target buffer can hold (attempting to copy "
                            + target.length()
                            + " bytes at offset "
                            + target.offset()
                            + " to "
                            + glbuffer1.size()
                            + " size buffer)"
                    );
                } else {
                    this.device
                        .directStateAccess()
                        .copyBufferSubData(glbuffer.handle, glbuffer1.handle, source.offset(), target.offset(), source.length());
                }
            }
        }
    }

    @Override
    public void writeToTexture(GpuTexture texture, NativeImage image) {
        int i = texture.getWidth(0);
        int j = texture.getHeight(0);
        if (image.getWidth() != i || image.getHeight() != j) {
            throw new IllegalArgumentException(
                "Cannot replace texture of size " + i + "x" + j + " with image of size " + image.getWidth() + "x" + image.getHeight()
            );
        } else if (texture.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
        } else if ((texture.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
        } else {
            this.writeToTexture(texture, image, 0, 0, 0, 0, i, j, 0, 0);
        }
    }

    @Override
    public void writeToTexture(
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
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (mipLevel >= 0 && mipLevel < texture.getMipLevels()) {
            if (sourceX + width > image.getWidth() || sourceY + height > image.getHeight()) {
                throw new IllegalArgumentException(
                    "Copy source ("
                        + image.getWidth()
                        + "x"
                        + image.getHeight()
                        + ") is not large enough to read a rectangle of "
                        + width
                        + "x"
                        + height
                        + " from "
                        + sourceX
                        + "x"
                        + sourceY
                );
            } else if (x + width > texture.getWidth(mipLevel) || y + height > texture.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + width
                        + "x"
                        + height
                        + ") is not large enough to write a rectangle of "
                        + width
                        + "x"
                        + height
                        + " at "
                        + x
                        + "x"
                        + y
                        + " (at mip level "
                        + mipLevel
                        + ")"
                );
            } else if (texture.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((texture.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            } else if (depthOrLayer >= texture.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + texture.getDepthOrLayers());
            } else {
                int i;
                if ((texture.usage() & 16) != 0) {
                    i = GlConst.CUBEMAP_TARGETS[depthOrLayer % 6];
                    GL11.glBindTexture(34067, ((GlTexture)texture).id);
                } else {
                    i = 3553;
                    GlStateManager._bindTexture(((GlTexture)texture).id);
                }

                GlStateManager._pixelStore(3314, image.getWidth());
                GlStateManager._pixelStore(3316, sourceX);
                GlStateManager._pixelStore(3315, sourceY);
                GlStateManager._pixelStore(3317, image.format().components());
                GlStateManager._texSubImage2D(
                    i, mipLevel, x, y, width, height, GlConst.toGl(image.format()), 5121, image.getPointer()
                );
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + texture.getMipLevels());
        }
    }

    @Override
    public void writeToTexture(
        GpuTexture texture,
        ByteBuffer buffer,
        NativeImage.Format format,
        int mipLevel,
        int depthOrLayer,
        int x,
        int y,
        int width,
        int height
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (mipLevel >= 0 && mipLevel < texture.getMipLevels()) {
            if (width * height * format.components() > buffer.remaining()) {
                throw new IllegalArgumentException(
                    "Copy would overrun the source buffer (remaining length of "
                        + buffer.remaining()
                        + ", but copy is "
                        + width
                        + "x"
                        + height
                        + " of format "
                        + format
                        + ")"
                );
            } else if (x + width > texture.getWidth(mipLevel) || y + height > texture.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + texture.getWidth(mipLevel)
                        + "x"
                        + texture.getHeight(mipLevel)
                        + ") is not large enough to write a rectangle of "
                        + width
                        + "x"
                        + height
                        + " at "
                        + x
                        + "x"
                        + y
                );
            } else if (texture.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((texture.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            } else if (depthOrLayer >= texture.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + texture.getDepthOrLayers());
            } else {
                int i;
                if ((texture.usage() & 16) != 0) {
                    i = GlConst.CUBEMAP_TARGETS[depthOrLayer % 6];
                    GL11.glBindTexture(34067, ((GlTexture)texture).id);
                } else {
                    i = 3553;
                    GlStateManager._bindTexture(((GlTexture)texture).id);
                }

                GlStateManager._pixelStore(3314, width);
                GlStateManager._pixelStore(3316, 0);
                GlStateManager._pixelStore(3315, 0);
                GlStateManager._pixelStore(3317, format.components());
                GlStateManager._texSubImage2D(i, mipLevel, x, y, width, height, GlConst.toGl(format), 5121, buffer);
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + texture.getMipLevels());
        }
    }

    @Override
    public void copyTextureToBuffer(GpuTexture texture, GpuBuffer buffer, int offset, Runnable task, int mipLevel) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            this.copyTextureToBuffer(texture, buffer, offset, task, mipLevel, 0, 0, texture.getWidth(mipLevel), texture.getHeight(mipLevel));
        }
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture texture, GpuBuffer buffer, int offset, Runnable task, int mipLevel, int x, int y, int width, int height
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (mipLevel >= 0 && mipLevel < texture.getMipLevels()) {
            if (texture.getWidth(mipLevel) * texture.getHeight(mipLevel) * texture.getFormat().pixelSize() + offset > buffer.size()) {
                throw new IllegalArgumentException(
                    "Buffer of size "
                        + buffer.size()
                        + " is not large enough to hold "
                        + width
                        + "x"
                        + height
                        + " pixels ("
                        + texture.getFormat().pixelSize()
                        + " bytes each) starting from offset "
                        + offset
                );
            } else if ((texture.usage() & 2) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
            } else if ((buffer.usage() & 8) == 0) {
                throw new IllegalArgumentException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
            } else if (x + width > texture.getWidth(mipLevel) || y + height > texture.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Copy source texture ("
                        + texture.getWidth(mipLevel)
                        + "x"
                        + texture.getHeight(mipLevel)
                        + ") is not large enough to read a rectangle of "
                        + width
                        + "x"
                        + height
                        + " from "
                        + x
                        + ","
                        + y
                );
            } else if (texture.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            } else if (buffer.isClosed()) {
                throw new IllegalStateException("Destination buffer is closed");
            } else if (texture.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            } else {
                GlStateManager.clearGlErrors();
                this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, ((GlTexture)texture).glId(), 0, mipLevel, 36008);
                GlStateManager._glBindBuffer(35051, ((GlBuffer)buffer).handle);
                GlStateManager._pixelStore(3330, width);
                GlStateManager._readPixels(
                    x,
                    y,
                    width,
                    height,
                    GlConst.toGlExternalId(texture.getFormat()),
                    GlConst.toGlType(texture.getFormat()),
                    offset
                );
                RenderSystem.queueFencedTask(task);
                GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, mipLevel);
                GlStateManager._glBindFramebuffer(36008, 0);
                GlStateManager._glBindBuffer(35051, 0);
                int i = GlStateManager._getError();
                if (i != 0) {
                    throw new IllegalStateException("Couldn't perform copyTobuffer for texture " + texture.getLabel() + ": GL error " + i);
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + texture.getMipLevels());
        }
    }

    @Override
    public void copyTextureToTexture(
        GpuTexture source, GpuTexture destination, int mipLevel, int x, int y, int sourceX, int sourceY, int width, int height
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (mipLevel >= 0 && mipLevel < source.getMipLevels() && mipLevel < destination.getMipLevels()) {
            if (x + width > destination.getWidth(mipLevel) || y + height > destination.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + destination.getWidth(mipLevel)
                        + "x"
                        + destination.getHeight(mipLevel)
                        + ") is not large enough to write a rectangle of "
                        + width
                        + "x"
                        + height
                        + " at "
                        + x
                        + "x"
                        + y
                );
            } else if (sourceX + width > source.getWidth(mipLevel) || sourceY + height > source.getHeight(mipLevel)) {
                throw new IllegalArgumentException(
                    "Source texture ("
                        + source.getWidth(mipLevel)
                        + "x"
                        + source.getHeight(mipLevel)
                        + ") is not large enough to read a rectangle of "
                        + width
                        + "x"
                        + height
                        + " at "
                        + sourceX
                        + "x"
                        + sourceY
                );
            } else if (source.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            } else if (destination.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((source.usage() & 2) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
            } else if ((destination.usage() & 1) == 0) {
                throw new IllegalArgumentException("Texture needs USAGE_COPY_DST to be a destination for a copy");
            } else if (source.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            } else if (destination.getDepthOrLayers() > 1) {
                throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            } else {
                GlStateManager.clearGlErrors();
                GlStateManager._disableScissorTest();
                boolean flag = source.getFormat().hasDepthAspect();
                int i = ((GlTexture)source).glId();
                int j = ((GlTexture)destination).glId();
                var hasStencil = source.getFormat().hasStencilAspect();
                this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, flag ? 0 : i, flag ? i : 0, 0, 0, hasStencil);
                this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, flag ? 0 : j, flag ? j : 0, 0, 0, hasStencil);
                var bufferMask = 0;
                if (source.getFormat().hasColorAspect()) {
                    bufferMask |= GlConst.GL_COLOR_BUFFER_BIT;
                }
                if (source.getFormat().hasDepthAspect()) {
                    bufferMask |= GlConst.GL_DEPTH_BUFFER_BIT;
                }
                if (source.getFormat().hasStencilAspect()) {
                    bufferMask |= GL11.GL_STENCIL_BUFFER_BIT;
                }
                this.device
                    .directStateAccess()
                    .blitFrameBuffers(
                        this.readFbo,
                        this.drawFbo,
                        sourceX,
                        sourceY,
                        width,
                        height,
                        x,
                        y,
                        width,
                        height,
                        bufferMask,
                        9728
                    );
                int k = GlStateManager._getError();
                if (k != 0) {
                    throw new IllegalStateException(
                        "Couldn't perform copyToTexture for texture " + source.getLabel() + " to " + destination.getLabel() + ": GL error " + k
                    );
                }
            }
        } else {
            throw new IllegalArgumentException(
                "Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + source.getMipLevels() + " and < " + destination.getMipLevels()
            );
        }
    }

    @Override
    public void presentTexture(GpuTextureView texture) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (!texture.texture().getFormat().hasColorAspect()) {
            throw new IllegalStateException("Cannot present a non-color texture!");
        } else if ((texture.texture().usage() & 8) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT to presented to the screen");
        } else if (texture.texture().getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for presentation");
        } else {
            GlStateManager._disableScissorTest();
            GlStateManager._viewport(0, 0, texture.getWidth(0), texture.getHeight(0));
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)texture.texture()).glId(), 0, 0, 0);
            this.device
                .directStateAccess()
                .blitFrameBuffers(
                    this.drawFbo, 0, 0, 0, texture.getWidth(0), texture.getHeight(0), 0, 0, texture.getWidth(0), texture.getHeight(0), 16384, 9728
                );
        }
    }

    @Override
    public GpuFence createFence() {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            return new GlFence();
        }
    }

    protected <T> void executeDrawMultiple(
        GlRenderPass renderPass,
        Collection<RenderPass.Draw<T>> draws,
        @Nullable GpuBuffer buffer,
        @Nullable VertexFormat.IndexType indexType,
        Collection<String> uniforms,
        T data
    ) {
        if (this.trySetup(renderPass, uniforms)) {
            if (indexType == null) {
                indexType = VertexFormat.IndexType.SHORT;
            }

            for (RenderPass.Draw<T> draw : draws) {
                VertexFormat.IndexType vertexformat$indextype = draw.indexType() == null ? indexType : draw.indexType();
                renderPass.setIndexBuffer(draw.indexBuffer() == null ? buffer : draw.indexBuffer(), vertexformat$indextype);
                renderPass.setVertexBuffer(draw.slot(), draw.vertexBuffer());
                if (GlRenderPass.VALIDATION) {
                    if (renderPass.indexBuffer == null) {
                        throw new IllegalStateException("Missing index buffer");
                    }

                    if (renderPass.indexBuffer.isClosed()) {
                        throw new IllegalStateException("Index buffer has been closed!");
                    }

                    if (renderPass.vertexBuffers[0] == null) {
                        throw new IllegalStateException("Missing vertex buffer at slot 0");
                    }

                    if (renderPass.vertexBuffers[0].isClosed()) {
                        throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
                    }
                }

                BiConsumer<T, RenderPass.UniformUploader> biconsumer = draw.uniformUploaderConsumer();
                if (biconsumer != null) {
                    biconsumer.accept(data, (p_417605_, p_417606_) -> {
                        if (renderPass.pipeline.program().getUniform(p_417605_) instanceof Uniform.Ubo(int i)) {
                            GL32.glBindBufferRange(35345, i, ((GlBuffer)p_417606_.buffer()).handle, p_417606_.offset(), p_417606_.length());
                        }
                    });
                }

                this.drawFromBuffers(renderPass, 0, draw.firstIndex(), draw.indexCount(), vertexformat$indextype, renderPass.pipeline, 1);
            }
        }
    }

    protected void executeDraw(GlRenderPass renderPass, int firstIndex, int index, int indexCount, @Nullable VertexFormat.IndexType indexType, int primCount) {
        if (this.trySetup(renderPass, Collections.emptyList())) {
            if (GlRenderPass.VALIDATION) {
                if (indexType != null) {
                    if (renderPass.indexBuffer == null) {
                        throw new IllegalStateException("Missing index buffer");
                    }

                    if (renderPass.indexBuffer.isClosed()) {
                        throw new IllegalStateException("Index buffer has been closed!");
                    }

                    if ((renderPass.indexBuffer.usage() & 64) == 0) {
                        throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
                    }
                }

                GlRenderPipeline glrenderpipeline = renderPass.pipeline;
                if (renderPass.vertexBuffers[0] == null && glrenderpipeline != null && !glrenderpipeline.info().getVertexFormat().getElements().isEmpty()) {
                    throw new IllegalStateException("Vertex format contains elements but vertex buffer at slot 0 is null");
                }

                if (renderPass.vertexBuffers[0] != null && renderPass.vertexBuffers[0].isClosed()) {
                    throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
                }

                if (renderPass.vertexBuffers[0] != null && (renderPass.vertexBuffers[0].usage() & 32) == 0) {
                    throw new IllegalStateException("Vertex buffer must have GpuBuffer.USAGE_VERTEX!");
                }
            }

            this.drawFromBuffers(renderPass, firstIndex, index, indexCount, indexType, renderPass.pipeline, primCount);
        }
    }

    private void drawFromBuffers(
        GlRenderPass renderPass,
        int firstIndex,
        int index,
        int indexCount,
        @Nullable VertexFormat.IndexType indexType,
        GlRenderPipeline pipeline,
        int primCount
    ) {
        this.device.vertexArrayCache().bindVertexArray(pipeline.info().getVertexFormat(), (GlBuffer)renderPass.vertexBuffers[0]);
        if (indexType != null) {
            GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle);
            if (primCount > 1) {
                if (firstIndex > 0) {
                    GL32.glDrawElementsInstancedBaseVertex(
                        GlConst.toGl(pipeline.info().getVertexFormatMode()),
                        indexCount,
                        GlConst.toGl(indexType),
                        (long)index * indexType.bytes,
                        primCount,
                        firstIndex
                    );
                } else {
                    GL31.glDrawElementsInstanced(
                        GlConst.toGl(pipeline.info().getVertexFormatMode()), indexCount, GlConst.toGl(indexType), (long)index * indexType.bytes, primCount
                    );
                }
            } else if (firstIndex > 0) {
                GL32.glDrawElementsBaseVertex(
                    GlConst.toGl(pipeline.info().getVertexFormatMode()), indexCount, GlConst.toGl(indexType), (long)index * indexType.bytes, firstIndex
                );
            } else {
                GlStateManager._drawElements(
                    GlConst.toGl(pipeline.info().getVertexFormatMode()), indexCount, GlConst.toGl(indexType), (long)index * indexType.bytes
                );
            }
        } else if (primCount > 1) {
            GL31.glDrawArraysInstanced(GlConst.toGl(pipeline.info().getVertexFormatMode()), firstIndex, indexCount, primCount);
        } else {
            GlStateManager._drawArrays(GlConst.toGl(pipeline.info().getVertexFormatMode()), firstIndex, indexCount);
        }
    }

    private boolean trySetup(GlRenderPass renderPass, Collection<String> uniforms) {
        if (GlRenderPass.VALIDATION) {
            if (renderPass.pipeline == null) {
                throw new IllegalStateException("Can't draw without a render pipeline");
            }

            if (renderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
                throw new IllegalStateException("Pipeline contains invalid shader program");
            }

            for (RenderPipeline.UniformDescription renderpipeline$uniformdescription : renderPass.pipeline.info().getUniforms()) {
                GpuBufferSlice gpubufferslice = renderPass.uniforms.get(renderpipeline$uniformdescription.name());
                if (!uniforms.contains(renderpipeline$uniformdescription.name())) {
                    if (gpubufferslice == null) {
                        throw new IllegalStateException(
                            "Missing uniform " + renderpipeline$uniformdescription.name() + " (should be " + renderpipeline$uniformdescription.type() + ")"
                        );
                    }

                    if (renderpipeline$uniformdescription.type() == UniformType.UNIFORM_BUFFER) {
                        if (gpubufferslice.buffer().isClosed()) {
                            throw new IllegalStateException("Uniform buffer " + renderpipeline$uniformdescription.name() + " is already closed");
                        }

                        if ((gpubufferslice.buffer().usage() & 128) == 0) {
                            throw new IllegalStateException("Uniform buffer " + renderpipeline$uniformdescription.name() + " must have GpuBuffer.USAGE_UNIFORM");
                        }
                    }

                    if (renderpipeline$uniformdescription.type() == UniformType.TEXEL_BUFFER) {
                        if (gpubufferslice.offset() != 0 || gpubufferslice.length() != gpubufferslice.buffer().size()) {
                            throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                        }

                        if (renderpipeline$uniformdescription.textureFormat() == null) {
                            throw new IllegalStateException(
                                "Invalid uniform texel buffer " + renderpipeline$uniformdescription.name() + " (missing a texture format)"
                            );
                        }
                    }
                }
            }

            for (Entry<String, Uniform> entry1 : renderPass.pipeline.program().getUniforms().entrySet()) {
                if (entry1.getValue() instanceof Uniform.Sampler) {
                    String s1 = entry1.getKey();
                    GlTextureView gltextureview = (GlTextureView)renderPass.samplers.get(s1);
                    if (gltextureview == null) {
                        throw new IllegalStateException("Missing sampler " + s1);
                    }

                    if (gltextureview.isClosed()) {
                        throw new IllegalStateException("Sampler " + s1 + " (" + gltextureview.texture().getLabel() + ") has been closed!");
                    }

                    if ((gltextureview.texture().usage() & 4) == 0) {
                        throw new IllegalStateException("Sampler " + s1 + " (" + gltextureview.texture().getLabel() + ") must have USAGE_TEXTURE_BINDING!");
                    }
                }
            }

            if (renderPass.pipeline.info().wantsDepthTexture() && !renderPass.hasDepthTexture()) {
                LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", renderPass.pipeline.info().getLocation());
            }
        } else if (renderPass.pipeline == null || renderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
            return false;
        }

        RenderPipeline renderpipeline = renderPass.pipeline.info();
        GlProgram glprogram = renderPass.pipeline.program();
        this.applyPipelineState(renderpipeline);
        boolean flag1 = this.lastProgram != glprogram;
        if (flag1) {
            GlStateManager._glUseProgram(glprogram.getProgramId());
            this.lastProgram = glprogram;
        }

        for (Entry<String, Uniform> entry : glprogram.getUniforms().entrySet()) {
            String s = entry.getKey();
            boolean flag = renderPass.dirtyUniforms.contains(s);
            switch ((Uniform)entry.getValue()) {
                case Uniform.Ubo(int j2):
                    int k = j2;
                    if (flag) {
                        GpuBufferSlice gpubufferslice1 = renderPass.uniforms.get(s);
                        GL32.glBindBufferRange(35345, k, ((GlBuffer)gpubufferslice1.buffer()).handle, gpubufferslice1.offset(), gpubufferslice1.length());
                    }
                    break;
                case Uniform.Utb(int l, int i1, TextureFormat textureformat, int i2):
                    int j1 = i2;
                    if (flag1 || flag) {
                        GlStateManager._glUniform1i(l, i1);
                    }

                    GlStateManager._activeTexture(33984 + i1);
                    GL11C.glBindTexture(35882, j1);
                    if (flag) {
                        GpuBufferSlice gpubufferslice2 = renderPass.uniforms.get(s);
                        GL31.glTexBuffer(35882, GlConst.toGlInternalId(textureformat), ((GlBuffer)gpubufferslice2.buffer()).handle);
                    }
                    break;
                case Uniform.Sampler(int $$22, int l1):
                    int k1 = l1;
                    GlTextureView gltextureview1 = (GlTextureView)renderPass.samplers.get(s);
                    if (gltextureview1 == null) {
                        break;
                    }

                    if (flag1 || flag) {
                        GlStateManager._glUniform1i($$22, k1);
                    }

                    GlStateManager._activeTexture(33984 + k1);
                    GlTexture gltexture = gltextureview1.texture();
                    int j;
                    if ((gltexture.usage() & 16) != 0) {
                        j = 34067;
                        GL11.glBindTexture(34067, gltexture.id);
                    } else {
                        j = 3553;
                        GlStateManager._bindTexture(gltexture.id);
                    }

                    GlStateManager._texParameter(j, 33084, gltextureview1.baseMipLevel());
                    GlStateManager._texParameter(j, 33085, gltextureview1.baseMipLevel() + gltextureview1.mipLevels() - 1);
                    gltexture.flushModeChanges(j);
                    break;
                default:
                    throw new MatchException(null, null);
            }
        }

        renderPass.dirtyUniforms.clear();
        if (renderPass.isScissorEnabled()) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(renderPass.getScissorX(), renderPass.getScissorY(), renderPass.getScissorWidth(), renderPass.getScissorHeight());
        } else {
            GlStateManager._disableScissorTest();
        }

        var stencilTestOpt = renderPass.pipeline.info().getStencilTest();
        if (stencilTestOpt.isPresent()) {
            var stencilTest = stencilTestOpt.get();
            GlStateManager._enableStencilTest();
            var front = stencilTest.front();
            var back = stencilTest.back();
            if (front.equals(back)) {
                GlStateManager._stencilFunc(GlConst.toGl(front.compare()), stencilTest.referenceValue(), stencilTest.readMask());
                GlStateManager._stencilOp(GlConst.toGl(front.fail()), GlConst.toGl(front.depthFail()), GlConst.toGl(front.pass()));
            } else {
                GlStateManager._stencilFuncFront(GlConst.toGl(front.compare()), stencilTest.referenceValue(), stencilTest.readMask());
                GlStateManager._stencilFuncBack(GlConst.toGl(back.compare()), stencilTest.referenceValue(), stencilTest.readMask());
                GlStateManager._stencilOpFront(GlConst.toGl(front.fail()), GlConst.toGl(front.depthFail()), GlConst.toGl(front.pass()));
                GlStateManager._stencilOpBack(GlConst.toGl(back.fail()), GlConst.toGl(back.depthFail()), GlConst.toGl(back.pass()));
            }
            GlStateManager._stencilMask(stencilTest.writeMask());
        } else {
            GlStateManager._disableStencilTest();
        }

        return true;
    }

    private void applyPipelineState(RenderPipeline pipeline) {
        if (this.lastPipeline != pipeline) {
            this.lastPipeline = pipeline;
            if (pipeline.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.toGl(pipeline.getDepthTestFunction()));
            } else {
                GlStateManager._disableDepthTest();
            }

            if (pipeline.isCull()) {
                GlStateManager._enableCull();
            } else {
                GlStateManager._disableCull();
            }

            if (pipeline.getBlendFunction().isPresent()) {
                GlStateManager._enableBlend();
                BlendFunction blendfunction = pipeline.getBlendFunction().get();
                GlStateManager._blendFuncSeparate(
                    GlConst.toGl(blendfunction.sourceColor()),
                    GlConst.toGl(blendfunction.destColor()),
                    GlConst.toGl(blendfunction.sourceAlpha()),
                    GlConst.toGl(blendfunction.destAlpha())
                );
            } else {
                GlStateManager._disableBlend();
            }

            GlStateManager._polygonMode(1032, GlConst.toGl(pipeline.getPolygonMode()));
            GlStateManager._depthMask(pipeline.isWriteDepth());
            GlStateManager._colorMask(pipeline.isWriteColor(), pipeline.isWriteColor(), pipeline.isWriteColor(), pipeline.isWriteAlpha());
            if (pipeline.getDepthBiasConstant() == 0.0F && pipeline.getDepthBiasScaleFactor() == 0.0F) {
                GlStateManager._disablePolygonOffset();
            } else {
                GlStateManager._polygonOffset(pipeline.getDepthBiasScaleFactor(), pipeline.getDepthBiasConstant());
                GlStateManager._enablePolygonOffset();
            }

            switch (pipeline.getColorLogic()) {
                case NONE:
                    GlStateManager._disableColorLogicOp();
                    break;
                case OR_REVERSE:
                    GlStateManager._enableColorLogicOp();
                    GlStateManager._logicOp(5387);
            }
        }
    }

    public void finishRenderPass() {
        this.inRenderPass = false;
        GlStateManager._glBindFramebuffer(36160, 0);
        this.device.debugLabels().popDebugGroup();
    }

    protected GlDevice getDevice() {
        return this.device;
    }
}
