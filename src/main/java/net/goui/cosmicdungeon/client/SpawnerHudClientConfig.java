// file: src/main/java/net/goui/cosmicdungeon/client/SpawnerHudClientConfig.java
package net.goui.cosmicdungeon.client;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;
import java.util.Set;

public final class SpawnerHudClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Set<String> VALID_POSITIONS = Set.of("bottom_right", "bottom_left", "top_right", "top_left");
    public static final String EQUIPMENT_MODE_ICONS = "icons";
    public static final String EQUIPMENT_MODE_TEXT = "text";
    private static final Set<String> VALID_EQUIPMENT_MODES = Set.of(EQUIPMENT_MODE_ICONS, EQUIPMENT_MODE_TEXT);

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

    public static final ModConfigSpec.IntValue MAX_WIDTH = BUILDER
            .comment("Cosmic Spawner developer HUD maximum width in scaled GUI pixels.")
            .defineInRange("spawnerHud.maxWidth", 220, 120, 480);

    public static final ModConfigSpec.ConfigValue<String> EQUIPMENT_MODE = BUILDER
            .comment("Cosmic Spawner developer HUD equipment display mode: icons or text.")
            .define("spawnerHud.equipmentMode", EQUIPMENT_MODE_ICONS, SpawnerHudClientConfig::isValidEquipmentMode);

    public static final ModConfigSpec.BooleanValue MOB_TYPE = field("mobType", true);
    public static final ModConfigSpec.BooleanValue MOB_NAME = field("mobName", true);
    public static final ModConfigSpec.BooleanValue COORDINATES = field("coordinates", false);
    public static final ModConfigSpec.BooleanValue BOSS_ONE_SHOT = field("bossOneShot", false);
    public static final ModConfigSpec.BooleanValue BOSS_SPAWNED = field("bossSpawned", false);
    public static final ModConfigSpec.BooleanValue CAP = field("cap", true);
    public static final ModConfigSpec.BooleanValue DELAY = field("delay", true);
    public static final ModConfigSpec.BooleanValue SPAWN_COUNT = field("spawnCount", false);
    public static final ModConfigSpec.BooleanValue SPAWN_RANGE = field("spawnRange", false);
    public static final ModConfigSpec.BooleanValue REQUIRED_PLAYER_RANGE = field("requiredPlayerRange", false);
    public static final ModConfigSpec.BooleanValue MAX_NEARBY_ENTITIES = field("maxNearbyEntities", false);
    public static final ModConfigSpec.BooleanValue PRESET_PRESENT = field("presetPresent", false);
    public static final ModConfigSpec.BooleanValue EQUIPMENT = field("equipment", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SpawnerHudClientConfig() {}

    private static ModConfigSpec.BooleanValue field(String name, boolean defaultValue) {
        return BUILDER
                .comment("Show the " + name + " field in the Cosmic Spawner developer HUD.")
                .define("spawnerHud.fields." + name, defaultValue);
    }

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

    private static boolean isValidEquipmentMode(Object value) {
        return value instanceof String s && VALID_EQUIPMENT_MODES.contains(normalize(s));
    }

    public static void save() {
        SPEC.save();
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
