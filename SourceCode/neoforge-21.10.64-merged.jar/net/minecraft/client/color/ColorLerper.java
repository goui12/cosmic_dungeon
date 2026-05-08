package net.minecraft.client.color;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColorLerper {
    public static final DyeColor[] MUSIC_NOTE_COLORS = new DyeColor[]{
        DyeColor.WHITE,
        DyeColor.LIGHT_GRAY,
        DyeColor.LIGHT_BLUE,
        DyeColor.BLUE,
        DyeColor.CYAN,
        DyeColor.GREEN,
        DyeColor.LIME,
        DyeColor.YELLOW,
        DyeColor.ORANGE,
        DyeColor.PINK,
        DyeColor.RED,
        DyeColor.MAGENTA
    };

    public static int getLerpedColor(ColorLerper.Type type, float time) {
        int i = Mth.floor(time);
        int j = i / type.colorDuration;
        int k = type.colors.length;
        int l = j % k;
        int i1 = (j + 1) % k;
        float f = (i % type.colorDuration + Mth.frac(time)) / type.colorDuration;
        int j1 = type.getColor(type.colors[l]);
        int k1 = type.getColor(type.colors[i1]);
        return ARGB.lerp(f, j1, k1);
    }

    static int getModifiedColor(DyeColor color, float brightness) {
        if (color == DyeColor.WHITE) {
            return -1644826;
        } else {
            int i = color.getTextureDiffuseColor();
            return ARGB.color(255, Mth.floor(ARGB.red(i) * brightness), Mth.floor(ARGB.green(i) * brightness), Mth.floor(ARGB.blue(i) * brightness));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        SHEEP(25, DyeColor.values(), 0.75F),
        MUSIC_NOTE(30, ColorLerper.MUSIC_NOTE_COLORS, 1.25F);

        final int colorDuration;
        private final Map<DyeColor, Integer> colorByDye;
        final DyeColor[] colors;

        private Type(int colorDuration, DyeColor[] colors, float brightness) {
            this.colorDuration = colorDuration;
            this.colorByDye = Maps.newHashMap(
                Arrays.stream(colors)
                    .collect(Collectors.toMap(p_426174_ -> (DyeColor)p_426174_, p_426111_ -> ColorLerper.getModifiedColor(p_426111_, brightness)))
            );
            this.colors = colors;
        }

        public final int getColor(DyeColor dye) {
            return this.colorByDye.get(dye);
        }
    }
}
