package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractTexture implements AutoCloseable {
    @Nullable
    protected GpuTexture texture;
    @Nullable
    protected GpuTextureView textureView;

    public void setClamp(boolean clamp) {
        if (this.texture == null) {
            throw new IllegalStateException("Texture does not exist, can't change its clamp before something initializes it");
        } else {
            this.texture.setAddressMode(clamp ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT);
        }
    }

    /**
     * @param mipmap {@code true} if a mipmap is being used (mip level is greater than
     *               0)
     */
    public void setFilter(boolean blur, boolean mipmap) {
        if (this.texture == null) {
            throw new IllegalStateException("Texture does not exist, can't get change its filter before something initializes it");
        } else {
            this.texture.setTextureFilter(blur ? FilterMode.LINEAR : FilterMode.NEAREST, mipmap);
        }
    }

    public void setUseMipmaps(boolean useMipmaps) {
        if (this.texture == null) {
            throw new IllegalStateException("Texture does not exist, can't get change its filter before something initializes it");
        } else {
            this.texture.setUseMipmaps(useMipmaps);
        }
    }

    @Override
    public void close() {
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }

        if (this.textureView != null) {
            this.textureView.close();
            this.textureView = null;
        }
    }

    public GpuTexture getTexture() {
        if (this.texture == null) {
            throw new IllegalStateException("Texture does not exist, can't get it before something initializes it");
        } else {
            return this.texture;
        }
    }

    public GpuTextureView getTextureView() {
        if (this.textureView == null) {
            throw new IllegalStateException("Texture view does not exist, can't get it before something initializes it");
        } else {
            return this.textureView;
        }
    }
}
