package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ReloadableTexture extends AbstractTexture {
    private final ResourceLocation resourceId;

    public ReloadableTexture(ResourceLocation resourceId) {
        this.resourceId = resourceId;
    }

    public ResourceLocation resourceId() {
        return this.resourceId;
    }

    public void apply(TextureContents textureContents) {
        boolean flag = textureContents.clamp();
        boolean flag1 = textureContents.blur();

        try (NativeImage nativeimage = textureContents.image()) {
            this.doLoad(nativeimage, flag1, flag);
        }
    }

    protected void doLoad(NativeImage image, boolean blur, boolean clamp) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.close();
        this.texture = gpudevice.createTexture(this.resourceId::toString, 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
        this.textureView = gpudevice.createTextureView(this.texture);
        this.setFilter(blur, false);
        this.setClamp(clamp);
        gpudevice.createCommandEncoder().writeToTexture(this.texture, image);
    }

    public abstract TextureContents loadContents(ResourceManager resourceManager) throws IOException;
}
