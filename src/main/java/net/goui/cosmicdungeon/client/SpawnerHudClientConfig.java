// file: src/main/java/net/goui/cosmicdungeon/client/SpawnerHudClientConfig.java
package net.goui.cosmicdungeon.client;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;
import java.util.Set;

public final class SpawnerHudClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Set<String> VALID_POSITIONS = Set.of("bottom_right", "bottom_left", "top_right", "top_left");

    public static final ModConfigSpec.ConfigValue<String> POSITION = BUILDER
            .comment("Cosmic Spawner developer HUD anchor: bottom_right, bottom_left, top_right, or top_left.")
            .define("spawnerHud.position", "bottom_right", SpawnerHudClientConfig::isValidPosition);

    public static final ModConfigSpec.IntValue OPACITY = BUILDER
            .comment("Cosmic Spawner developer HUD background opacity, from 0 (transparent) to 255 (opaque).")
            .defineInRange("spawnerHud.opacity", 170, 0, 255);

    public static final ModConfigSpec.IntValue HORIZONTAL_OFFSET = BUILDER
            .comment("Extra horizontal offset in scaled screen pixels from the configured HUD edge.")
            .defineInRange("spawnerHud.horizontalOffset", 0, -512, 512);

    public static final ModConfigSpec.IntValue VERTICAL_OFFSET = BUILDER
            .comment("Extra vertical offset in scaled screen pixels from the configured HUD edge.")
            .defineInRange("spawnerHud.verticalOffset", 0, -512, 512);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SpawnerHudClientConfig() {}

    public static Anchor anchor() {
        return Anchor.from(POSITION.get());
    }

    public static int backgroundColor() {
        return (OPACITY.get() << 24) | 0x061018;
    }

    public static int borderColor() {
        int alpha = Math.min(255, Math.max(32, OPACITY.get() + 48));
        return (alpha << 24) | 0x00FFD5;
    }

    private static boolean isValidPosition(Object value) {
        return value instanceof String s && VALID_POSITIONS.contains(normalize(s));
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public enum Anchor {
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        TOP_RIGHT,
        TOP_LEFT;

        static Anchor from(String value) {
            return switch (normalize(value)) {
                case "bottom_left" -> BOTTOM_LEFT;
                case "top_right" -> TOP_RIGHT;
                case "top_left" -> TOP_LEFT;
                default -> BOTTOM_RIGHT;
            };
        }

        public boolean left() {
            return this == BOTTOM_LEFT || this == TOP_LEFT;
        }

        public boolean top() {
            return this == TOP_LEFT || this == TOP_RIGHT;
        }
    }
}
