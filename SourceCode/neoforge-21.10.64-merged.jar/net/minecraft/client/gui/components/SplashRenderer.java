package net.minecraft.client.gui.components;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SplashRenderer {
    public static final SplashRenderer CHRISTMAS = new SplashRenderer("Merry X-mas!");
    public static final SplashRenderer NEW_YEAR = new SplashRenderer("Happy new year!");
    public static final SplashRenderer HALLOWEEN = new SplashRenderer("OOoooOOOoooo! Spooky!");
    private static final int WIDTH_OFFSET = 123;
    private static final int HEIGH_OFFSET = 69;
    private final String splash;

    public SplashRenderer(String splash) {
        this.splash = splash;
    }

    public void render(GuiGraphics guiGraphics, int width, Font font, float fade) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(width / 2.0F + 123.0F, 69.0F);
        guiGraphics.pose().rotate((float) (-Math.PI / 9));
        float f = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
        f = f * 100.0F / (font.width(this.splash) + 32);
        guiGraphics.pose().scale(f, f);
        guiGraphics.drawCenteredString(font, this.splash, 0, -8, ARGB.color(fade, -256));
        guiGraphics.pose().popMatrix();
    }
}
