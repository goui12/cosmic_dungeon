package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ChunkSectionLayer {
    SOLID(RenderPipelines.SOLID, 4194304, true, false),
    CUTOUT_MIPPED(RenderPipelines.CUTOUT_MIPPED, 4194304, true, false),
    CUTOUT(RenderPipelines.CUTOUT, 786432, false, false),
    TRANSLUCENT(RenderPipelines.TRANSLUCENT, 786432, true, true),
    TRIPWIRE(RenderPipelines.TRIPWIRE, 1536, true, true);

    private final RenderPipeline pipeline;
    private final int bufferSize;
    private final boolean useMipmaps;
    private final boolean sortOnUpload;
    private final String label;

    private ChunkSectionLayer(RenderPipeline pipeline, int bufferSize, boolean useMipmaps, boolean sortOnUpload) {
        this.pipeline = pipeline;
        this.bufferSize = bufferSize;
        this.useMipmaps = useMipmaps;
        this.sortOnUpload = sortOnUpload;
        this.label = this.toString().toLowerCase(Locale.ROOT);
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    public int bufferSize() {
        return this.bufferSize;
    }

    public String label() {
        return this.label;
    }

    public boolean sortOnUpload() {
        return this.sortOnUpload;
    }

    public GpuTextureView textureView() {
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstracttexture = texturemanager.getTexture(TextureAtlas.LOCATION_BLOCKS);
        abstracttexture.setUseMipmaps(this.useMipmaps);
        return abstracttexture.getTextureView();
    }
}
