package net.minecraft.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ARGB {
    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 0xFF;
    }

    public static int green(int color) {
        return color >> 8 & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    public static int color(int red, int green, int blue) {
        return color(255, red, green, blue);
    }

    public static int color(Vec3 p_color) {
        return color(as8BitChannel((float)p_color.x()), as8BitChannel((float)p_color.y()), as8BitChannel((float)p_color.z()));
    }

    public static int multiply(int color1, int color2) {
        if (color1 == -1) {
            return color2;
        } else {
            return color2 == -1
                ? color1
                : color(
                    alpha(color1) * alpha(color2) / 255,
                    red(color1) * red(color2) / 255,
                    green(color1) * green(color2) / 255,
                    blue(color1) * blue(color2) / 255
                );
        }
    }

    public static int scaleRGB(int color, float scale) {
        return scaleRGB(color, scale, scale, scale);
    }

    public static int scaleRGB(int p_color, float redScale, float greenScale, float blueScale) {
        return color(
            alpha(p_color),
            Math.clamp((long)((int)(red(p_color) * redScale)), 0, 255),
            Math.clamp((long)((int)(green(p_color) * greenScale)), 0, 255),
            Math.clamp((long)((int)(blue(p_color) * blueScale)), 0, 255)
        );
    }

    public static int scaleRGB(int p_color, int scale) {
        return color(
            alpha(p_color),
            Math.clamp((long)red(p_color) * scale / 255L, 0, 255),
            Math.clamp((long)green(p_color) * scale / 255L, 0, 255),
            Math.clamp((long)blue(p_color) * scale / 255L, 0, 255)
        );
    }

    public static int greyscale(int p_color) {
        int i = (int)(red(p_color) * 0.3F + green(p_color) * 0.59F + blue(p_color) * 0.11F);
        return color(i, i, i);
    }

    public static int lerp(float delta, int color1, int color2) {
        int i = Mth.lerpInt(delta, alpha(color1), alpha(color2));
        int j = Mth.lerpInt(delta, red(color1), red(color2));
        int k = Mth.lerpInt(delta, green(color1), green(color2));
        int l = Mth.lerpInt(delta, blue(color1), blue(color2));
        return color(i, j, k, l);
    }

    public static int opaque(int color) {
        return color | 0xFF000000;
    }

    public static int transparent(int color) {
        return color & 16777215;
    }

    public static int color(int alpha, int color) {
        return alpha << 24 | color & 16777215;
    }

    public static int color(float alpha, int color) {
        return as8BitChannel(alpha) << 24 | color & 16777215;
    }

    public static int white(float alpha) {
        return as8BitChannel(alpha) << 24 | 16777215;
    }

    public static int colorFromFloat(float alpha, float red, float green, float blue) {
        return color(as8BitChannel(alpha), as8BitChannel(red), as8BitChannel(green), as8BitChannel(blue));
    }

    public static Vector3f vector3fFromRGB24(int color) {
        float f = red(color) / 255.0F;
        float f1 = green(color) / 255.0F;
        float f2 = blue(color) / 255.0F;
        return new Vector3f(f, f1, f2);
    }

    public static int average(int color1, int color2) {
        return color(
            (alpha(color1) + alpha(color2)) / 2,
            (red(color1) + red(color2)) / 2,
            (green(color1) + green(color2)) / 2,
            (blue(color1) + blue(color2)) / 2
        );
    }

    public static int as8BitChannel(float value) {
        return Mth.floor(value * 255.0F);
    }

    public static float alphaFloat(int color) {
        return from8BitChannel(alpha(color));
    }

    public static float redFloat(int color) {
        return from8BitChannel(red(color));
    }

    public static float greenFloat(int color) {
        return from8BitChannel(green(color));
    }

    public static float blueFloat(int color) {
        return from8BitChannel(blue(color));
    }

    private static float from8BitChannel(int value) {
        return value / 255.0F;
    }

    public static int toABGR(int color) {
        return color & -16711936 | (color & 0xFF0000) >> 16 | (color & 0xFF) << 16;
    }

    public static int fromABGR(int color) {
        return toABGR(color);
    }

    public static int setBrightness(int p_color, float brightness) {
        int i = red(p_color);
        int j = green(p_color);
        int k = blue(p_color);
        int l = alpha(p_color);
        int i1 = Math.max(Math.max(i, j), k);
        int j1 = Math.min(Math.min(i, j), k);
        float f = i1 - j1;
        float f1;
        if (i1 != 0) {
            f1 = f / i1;
        } else {
            f1 = 0.0F;
        }

        float f2;
        if (f1 == 0.0F) {
            f2 = 0.0F;
        } else {
            float f3 = (i1 - i) / f;
            float f4 = (i1 - j) / f;
            float f5 = (i1 - k) / f;
            if (i == i1) {
                f2 = f5 - f4;
            } else if (j == i1) {
                f2 = 2.0F + f3 - f5;
            } else {
                f2 = 4.0F + f4 - f3;
            }

            f2 /= 6.0F;
            if (f2 < 0.0F) {
                f2++;
            }
        }

        if (f1 == 0.0F) {
            i = j = k = Math.round(brightness * 255.0F);
            return color(l, i, j, k);
        } else {
            float f8 = (f2 - (float)Math.floor(f2)) * 6.0F;
            float f9 = f8 - (float)Math.floor(f8);
            float f10 = brightness * (1.0F - f1);
            float f6 = brightness * (1.0F - f1 * f9);
            float f7 = brightness * (1.0F - f1 * (1.0F - f9));
            switch ((int)f8) {
                case 0:
                    i = Math.round(brightness * 255.0F);
                    j = Math.round(f7 * 255.0F);
                    k = Math.round(f10 * 255.0F);
                    break;
                case 1:
                    i = Math.round(f6 * 255.0F);
                    j = Math.round(brightness * 255.0F);
                    k = Math.round(f10 * 255.0F);
                    break;
                case 2:
                    i = Math.round(f10 * 255.0F);
                    j = Math.round(brightness * 255.0F);
                    k = Math.round(f7 * 255.0F);
                    break;
                case 3:
                    i = Math.round(f10 * 255.0F);
                    j = Math.round(f6 * 255.0F);
                    k = Math.round(brightness * 255.0F);
                    break;
                case 4:
                    i = Math.round(f7 * 255.0F);
                    j = Math.round(f10 * 255.0F);
                    k = Math.round(brightness * 255.0F);
                    break;
                case 5:
                    i = Math.round(brightness * 255.0F);
                    j = Math.round(f10 * 255.0F);
                    k = Math.round(f6 * 255.0F);
            }

            return color(l, i, j, k);
        }
    }
}
