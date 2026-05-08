package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverlayTexture implements AutoCloseable {
    private static final int SIZE = 16;
    public static final int NO_WHITE_U = 0;
    public static final int RED_OVERLAY_V = 3;
    public static final int WHITE_OVERLAY_V = 10;
    public static final int NO_OVERLAY = pack(0, 10);
    private final DynamicTexture texture = new DynamicTexture("Entity Color Overlay", 16, 16, false);

    public OverlayTexture() {
        NativeImage nativeimage = this.texture.getPixels();

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (i < 8) {
                    nativeimage.setPixel(j, i, -1291911168);
                } else {
                    int k = (int)((1.0F - j / 15.0F * 0.75F) * 255.0F);
                    nativeimage.setPixel(j, i, ARGB.color(k, -1));
                }
            }
        }

        this.texture.setClamp(true);
        this.texture.upload();
    }

    @Override
    public void close() {
        this.texture.close();
    }

    public void setupOverlayColor() {
        RenderSystem.setupOverlayColor(this.texture.getTextureView());
    }

    public static int u(float u) {
        return (int)(u * 15.0F);
    }

    public static int v(boolean hurt) {
        return hurt ? 3 : 10;
    }

    public static int pack(int u, int v) {
        return u | v << 16;
    }

    public static int pack(float p_u, boolean hurt) {
        return pack(u(p_u), v(hurt));
    }

    public void teardownOverlayColor() {
        RenderSystem.teardownOverlayColor();
    }
}
