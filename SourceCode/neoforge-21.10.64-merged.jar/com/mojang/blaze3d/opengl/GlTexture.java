package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlTexture extends GpuTexture {
    protected final int id;
    private final Int2IntMap fboCache = new Int2IntOpenHashMap();
    protected boolean closed;
    protected boolean modesDirty = true;
    private int views;
    protected final boolean external; // If true, the raw OpenGL texture is not managed by this GpuTexture

    protected GlTexture(int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels, int id) {
        this(usage, label, format, width, height, depthOrLayers, mipLevels, id, false);
    }

    protected GlTexture(int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels, int id, boolean external) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
        this.id = id;
        this.external = external;
    }

    @Override
    public void close() {
        if (!this.closed && !this.external) {
            this.closed = true;
            if (this.views == 0) {
                this.destroyImmediately();
            }
        }
    }

    private void destroyImmediately() {
        GlStateManager._deleteTexture(this.id);

        for (int i : this.fboCache.values()) {
            GlStateManager._glDeleteFramebuffers(i);
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture texture) {
        int i = texture == null ? 0 : ((GlTexture)texture).id;
        var useStencil = texture != null && texture.getFormat().hasStencilAspect();
        return this.fboCache.computeIfAbsent(i, p_411998_ -> {
            int j = directStateAccess.createFrameBufferObject();
            directStateAccess.bindFrameBufferTextures(j, this.id, i, 0, 0, useStencil);
            return j;
        });
    }

    public void flushModeChanges(int target) {
        if (this.modesDirty) {
            GlStateManager._texParameter(target, 10242, GlConst.toGl(this.addressModeU));
            GlStateManager._texParameter(target, 10243, GlConst.toGl(this.addressModeV));
            switch (this.minFilter) {
                case NEAREST:
                    GlStateManager._texParameter(target, 10241, this.useMipmaps ? 9986 : 9728);
                    break;
                case LINEAR:
                    GlStateManager._texParameter(target, 10241, this.useMipmaps ? 9987 : 9729);
            }

            switch (this.magFilter) {
                case NEAREST:
                    GlStateManager._texParameter(target, 10240, 9728);
                    break;
                case LINEAR:
                    GlStateManager._texParameter(target, 10240, 9729);
            }

            this.modesDirty = false;
        }
    }

    public int glId() {
        return this.id;
    }

    @Override
    public void setAddressMode(AddressMode addressModeU, AddressMode addressModeV) {
        super.setAddressMode(addressModeU, addressModeV);
        this.modesDirty = true;
    }

    @Override
    public void setTextureFilter(FilterMode minFilter, FilterMode magFilter, boolean useMipmaps) {
        super.setTextureFilter(minFilter, magFilter, useMipmaps);
        this.modesDirty = true;
    }

    @Override
    public void setUseMipmaps(boolean useMipmaps) {
        super.setUseMipmaps(useMipmaps);
        this.modesDirty = true;
    }

    public void addViews() {
        this.views++;
    }

    public void removeViews() {
        this.views--;
        if (this.closed && this.views == 0) {
            this.destroyImmediately();
        }
    }
}
