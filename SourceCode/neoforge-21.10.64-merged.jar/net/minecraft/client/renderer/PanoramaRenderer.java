package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PanoramaRenderer {
    public static final ResourceLocation PANORAMA_OVERLAY = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
    private Minecraft minecraft;
    private final CubeMap cubeMap;
    private float spin;

    public PanoramaRenderer(CubeMap cubeMap) {
        this.cubeMap = cubeMap;
        this.minecraft = Minecraft.getInstance();
    }

    public void render(GuiGraphics guiGraphics, int width, int height, boolean spin) {
        if (spin) {
            float f = this.minecraft.getDeltaTracker().getRealtimeDeltaTicks();
            float f1 = (float)(f * this.minecraft.options.panoramaSpeed().get());
            this.spin = wrap(this.spin + f1 * 0.1F, 360.0F);
        }

        this.cubeMap.render(this.minecraft, 10.0F, -this.spin);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PANORAMA_OVERLAY, 0, 0, 0.0F, 0.0F, width, height, 16, 128, 16, 128);
    }

    private static float wrap(float value, float max) {
        return value > max ? value - max : value;
    }

    public void registerTextures(TextureManager textureManager) {
        this.cubeMap.registerTextures(textureManager);
    }
}
