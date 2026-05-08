package net.minecraft.world.level;

public interface ColorMapColorUtil {
    static int get(double x, double y, int[] pixels, int defaultValue) {
        y *= x;
        int i = (int)((1.0 - x) * 255.0);
        int j = (int)((1.0 - y) * 255.0);
        int k = j << 8 | i;
        return k >= pixels.length ? defaultValue : pixels[k];
    }
}
