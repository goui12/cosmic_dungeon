package net.goui.cosmicdungeon.common.color;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum AmethystColor implements StringRepresentable {
    WHITE(0xF9FFFE), ORANGE(0xF9801D), MAGENTA(0xC74EBD), LIGHT_BLUE(0x3AB3DA),
    YELLOW(0xFED83D), LIME(0x80C71F), PINK(0xF38BAA), GRAY(0x474F52),
    LIGHT_GRAY(0x9D9D97), CYAN(0x169C9C), PURPLE(0x8932B8), BLUE(0x3C44AA),
    BROWN(0x835432), GREEN(0x5E7C16), RED(0xB02E26), BLACK(0x1D1D21);

    // Codec used by DataComponents, recipes, loot, etc.
    public static final Codec<AmethystColor> CODEC = StringRepresentable.fromEnum(AmethystColor::values);

    // Store as ARGB (opaque)
    public final int argb;

    AmethystColor(int rgb) { this.argb = 0xFF000000 | rgb; }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    /** Optional helper if you ever need to parse from a string safely. */
    public static AmethystColor byName(String name, AmethystColor fallback) {
        for (AmethystColor c : values()) {
            if (c.getSerializedName().equals(name)) return c;
        }
        return fallback;
    }
}
