package net.goui.cosmicdungeon.auth;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum Rank {
    DUNGEONEER,
    DEVELOPER;

    public static final Codec<Rank> CODEC = Codec.STRING.xmap(
            Rank::fromString,
            r -> r.name().toLowerCase(Locale.ROOT)
    );

    public static Rank fromString(String s) {
        if (s == null) return DUNGEONEER;
        final String v = s.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "developer", "dev", "admin" -> DEVELOPER;
            default -> DUNGEONEER;
        };
    }

    public boolean isDeveloper() {
        return this == DEVELOPER;
    }
}
