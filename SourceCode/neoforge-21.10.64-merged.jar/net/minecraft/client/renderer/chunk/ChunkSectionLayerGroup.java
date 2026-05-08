package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ChunkSectionLayerGroup {
    OPAQUE(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT_MIPPED, ChunkSectionLayer.CUTOUT),
    TRANSLUCENT(ChunkSectionLayer.TRANSLUCENT),
    TRIPWIRE(ChunkSectionLayer.TRIPWIRE);

    private final String label;
    private final ChunkSectionLayer[] layers;

    private ChunkSectionLayerGroup(ChunkSectionLayer... layers) {
        this.layers = layers;
        this.label = this.toString().toLowerCase(Locale.ROOT);
    }

    public String label() {
        return this.label;
    }

    public ChunkSectionLayer[] layers() {
        return this.layers;
    }

    public RenderTarget outputTarget() {
        Minecraft minecraft = Minecraft.getInstance();

        RenderTarget rendertarget = switch (this) {
            case TRANSLUCENT -> minecraft.levelRenderer.getTranslucentTarget();
            case TRIPWIRE -> minecraft.levelRenderer.getWeatherTarget();
            default -> minecraft.getMainRenderTarget();
        };
        return rendertarget != null ? rendertarget : minecraft.getMainRenderTarget();
    }
}
