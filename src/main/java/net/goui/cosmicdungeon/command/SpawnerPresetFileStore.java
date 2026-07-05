package net.goui.cosmicdungeon.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;

final class SpawnerPresetFileStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FORMAT_VERSION = 2;
    private static final int LEGACY_FORMAT_VERSION = 1;

    private SpawnerPresetFileStore() {}

    static Path presetDirectory(MinecraftServer server) {
        return server.getServerDirectory()
                .resolve("cosmicdungeon")
                .resolve("spawner_presets");
    }

    static Path presetPath(MinecraftServer server, String name) {
        return presetDirectory(server).resolve(name + ".json");
    }

    static void writePreset(MinecraftServer server, String name, CosmicSpawnerBlockEntity be) throws IOException {
        Files.createDirectories(presetDirectory(server));

        JsonObject root = new JsonObject();
        root.addProperty("format", "cosmicdungeon_spawner_preset");
        root.addProperty("formatVersion", FORMAT_VERSION);
        root.addProperty("note", "When Cosmic spawner fields change, update SpawnerPresetFileStore + /spawner preset parser/writer.");
        root.addProperty("entityTypeId", be.getSpawnerEntityId());
        root.addProperty("bossOneShot", be.isBossOneShot());
        root.addProperty("spawnerMobCap", be.getSpawnerMobCap());
        root.addProperty("minSpawnDelay", be.getSpawnerMinSpawnDelay());
        root.addProperty("maxSpawnDelay", be.getSpawnerMaxSpawnDelay());

        CosmicSpawnerPreset preset = be.getSpawnerPreset();
        if (preset != null) {
            TagValueOutput output = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING,
                    server.registryAccess()
            );

            preset.save(output);

            CompoundTag tag = output.buildResult();
            root.add(
                    "spawnerPresetNbt",
                    CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, tag).getOrThrow()
            );
        }

        Files.writeString(presetPath(server, name), GSON.toJson(root));
    }

    static LoadedPreset readPreset(MinecraftServer server, String name) throws IOException {
        Path path = presetPath(server, name);
        JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        int formatVersion = root.has("formatVersion") ? root.get("formatVersion").getAsInt() : LEGACY_FORMAT_VERSION;
        if (formatVersion < FORMAT_VERSION) {
            LOGGER.info("Upgrading Cosmic Spawner preset file '{}' from format version {} to {} on next save", path, formatVersion, FORMAT_VERSION);
        }

        LoadedPreset out = new LoadedPreset();
        out.entityTypeId = root.has("entityTypeId") ? root.get("entityTypeId").getAsString() : "minecraft:pig";
        out.bossOneShot = root.has("bossOneShot") && root.get("bossOneShot").getAsBoolean();
        out.spawnerMobCap = root.has("spawnerMobCap") ? Math.max(0, root.get("spawnerMobCap").getAsInt()) : 0;
        out.minSpawnDelay = root.has("minSpawnDelay") ? Math.max(1, root.get("minSpawnDelay").getAsInt()) : 200;
        out.maxSpawnDelay = root.has("maxSpawnDelay")
                ? Math.max(out.minSpawnDelay, root.get("maxSpawnDelay").getAsInt())
                : out.minSpawnDelay;

        if (root.has("spawnerPresetNbt") && !root.get("spawnerPresetNbt").isJsonNull()) {
            CompoundTag tag = CompoundTag.CODEC
                    .parse(JsonOps.INSTANCE, root.get("spawnerPresetNbt"))
                    .getOrThrow();

            out.preset = CosmicSpawnerPreset.load(
                    TagValueInput.create(
                            ProblemReporter.DISCARDING,
                            server.registryAccess(),
                            tag
                    )
            );
        }

        return out;
    }

    static boolean deletePreset(MinecraftServer server, String name) throws IOException {
        return Files.deleteIfExists(presetPath(server, name));
    }

    static java.util.List<String> listPresetNames(MinecraftServer server) throws IOException {
        Files.createDirectories(presetDirectory(server));
        try (Stream<Path> stream = Files.list(presetDirectory(server))) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString())
                    .map(n -> n.substring(0, n.length() - 5))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }
    }

    static void ensureExamplePresets(MinecraftServer server) throws IOException {
        Files.createDirectories(presetDirectory(server));

        try (Stream<Path> existingFiles = Files.list(presetDirectory(server))) {
            if (existingFiles.findAny().isPresent()) return;
        }

        Files.writeString(presetPath(server, "Skeleton_Master"), example("minecraft:skeleton", false, 6, 80, 120));
        Files.writeString(presetPath(server, "Warden_Trial"), example("minecraft:warden", true, 1, 300, 420));
        Files.writeString(presetPath(server, "Pillager_Captain_Elite"), example("minecraft:pillager", false, 8, 60, 100));
    }

    private static String example(String entity, boolean boss, int cap, int minDelay, int maxDelay) {
        JsonObject root = new JsonObject();
        root.addProperty("format", "cosmicdungeon_spawner_preset");
        root.addProperty("formatVersion", FORMAT_VERSION);
        root.addProperty("entityTypeId", entity);
        root.addProperty("bossOneShot", boss);
        root.addProperty("spawnerMobCap", cap);
        root.addProperty("minSpawnDelay", minDelay);
        root.addProperty("maxSpawnDelay", maxDelay);
        root.addProperty("note", String.format(Locale.ROOT, "Example preset for %s.", entity));
        return GSON.toJson(root);
    }

    static final class LoadedPreset {
        CosmicSpawnerPreset preset;
        String entityTypeId;
        boolean bossOneShot;
        int spawnerMobCap;
        int minSpawnDelay;
        int maxSpawnDelay;
    }
}
