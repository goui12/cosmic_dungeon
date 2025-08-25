package net.goui.cosmicdungeon.classsystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClassConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "cosmicdungeon-classes.json";
    private static ClassConfig CACHED;

    private ClassConfigLoader() {}

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    public static synchronized ClassConfig get() {
        if (CACHED == null) CACHED = loadOrCreateDefault();
        return CACHED;
    }

    public static synchronized void reload() {
        CACHED = loadOrCreateDefault();
    }

    private static ClassConfig loadOrCreateDefault() {
        Path path = configPath();
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, defaultJson(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write default " + FILE_NAME, e);
            }
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            String cleaned = stripLineComments(raw); // allow // comments for artists
            ClassConfig cfg = GSON.fromJson(cleaned, ClassConfig.class);
            if (cfg == null || cfg.classes == null) {
                throw new RuntimeException("Invalid or empty config in " + path);
            }
            return cfg;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + FILE_NAME + ": " + e.getMessage(), e);
        }
    }

    private static String stripLineComments(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (String line : s.split("\n")) {
            int idx = line.indexOf("//");
            if (idx >= 0) line = line.substring(0, idx);
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static String defaultJson() {
        return """
        {
          // Mode:
          //  "base"      => set base value
          //  "add"       => AttributeModifier ADD_VALUE
          //  "mul_base"  => AttributeModifier MULTIPLY_BASE
          //  "mul_total" => AttributeModifier MULTIPLY_TOTAL
          "classes": [
            {
              "name": "default",
              "rules": [
                { "attribute": "minecraft:attack_speed",          "mode": "base", "value": 4.0,  "label": "CD Base: attack_speed" },
                { "attribute": "minecraft:attack_damage",         "mode": "base", "value": 1.0,  "label": "CD Base: attack_damage" },
                { "attribute": "minecraft:movement_speed",        "mode": "base", "value": 0.1,  "label": "CD Base: movement_speed" },
                { "attribute": "minecraft:max_health",            "mode": "base", "value": 20.0, "label": "CD Base: max_health" },
                { "attribute": "minecraft:knockback_resistance",  "mode": "base", "value": 0.0,  "label": "CD Base: kb_res" }
              ]
            },
            {
              "name": "pyroclast",
              "rules": [
                { "attribute": "minecraft:attack_speed",  "mode": "add", "value": 0.5,  "label": "CD Pyro +AS" },
                { "attribute": "minecraft:attack_damage", "mode": "add", "value": 1.0,  "label": "CD Pyro +AD" }
              ]
            },
            {
              "name": "judicator",
              "rules": [
                { "attribute": "minecraft:max_health",            "mode": "add", "value": 4.0,  "label": "CD Judi +HP" },
                { "attribute": "minecraft:knockback_resistance",  "mode": "add", "value": 0.2,  "label": "CD Judi +KBRes" }
              ]
            },
            {
              "name": "theurgist",
              "rules": [
                { "attribute": "minecraft:movement_speed", "mode": "add", "value": 0.02, "label": "CD Theu +MS" },
                { "attribute": "minecraft:attack_speed",   "mode": "add", "value": 0.3,  "label": "CD Theu +AS" }
              ]
            }
          ]
        }
        """;
    }
}
