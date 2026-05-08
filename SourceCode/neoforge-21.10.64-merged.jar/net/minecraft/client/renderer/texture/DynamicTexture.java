package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DynamicTexture extends AbstractTexture implements Dumpable {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private NativeImage pixels;

    public DynamicTexture(Supplier<String> label, NativeImage pixels) {
        this.pixels = pixels;
        this.createTexture(label);
        this.upload();
    }

    public DynamicTexture(String label, int width, int height, boolean useCalloc) {
        this.pixels = new NativeImage(width, height, useCalloc);
        this.createTexture(label);
    }

    public DynamicTexture(Supplier<String> label, int width, int height, boolean useCalloc) {
        this.pixels = new NativeImage(width, height, useCalloc);
        this.createTexture(label);
    }

    private void createTexture(Supplier<String> label) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.texture = gpudevice.createTexture(label, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
        this.texture.setTextureFilter(FilterMode.NEAREST, false);
        this.textureView = gpudevice.createTextureView(this.texture);
    }

    private void createTexture(String label) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.texture = gpudevice.createTexture(label, 5, TextureFormat.RGBA8, this.pixels.getWidth(), this.pixels.getHeight(), 1, 1);
        this.texture.setTextureFilter(FilterMode.NEAREST, false);
        this.textureView = gpudevice.createTextureView(this.texture);
    }

    public void upload() {
        if (this.pixels != null && this.texture != null) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels);
        } else {
            LOGGER.warn("Trying to upload disposed texture {}", this.getTexture().getLabel());
        }
    }

    @Nullable
    public NativeImage getPixels() {
        return this.pixels;
    }

    public void setPixels(NativeImage pixels) {
        if (this.pixels != null) {
            this.pixels.close();
        }

        this.pixels = pixels;
    }

    @Override
    public void close() {
        if (this.pixels != null) {
            this.pixels.close();
            this.pixels = null;
        }

        super.close();
    }

    @Override
    public void dumpContents(ResourceLocation resourceLocation, Path p_path) throws IOException {
        if (this.pixels != null) {
            String s = resourceLocation.toDebugFileName() + ".png";
            Path path = p_path.resolve(s);
            this.pixels.writeToFile(path);
        }
    }
}
