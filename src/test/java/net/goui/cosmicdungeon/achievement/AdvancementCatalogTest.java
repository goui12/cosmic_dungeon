package net.goui.cosmicdungeon.achievement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdvancementCatalogTest {
    private static final Path PROJECT = projectRoot();
    private static final Path ROOT = PROJECT.resolve("src/generated/resources_server/data/cosmicdungeon/advancement");
    private static final Path LANGUAGE = PROJECT.resolve("src/main/resources/assets/cosmicdungeon/lang/en_us.json");

    private static Path projectRoot() {
        // NeoForge runs unit tests from a generated game directory below the project.
        for (Path path = Path.of("").toAbsolutePath(); path != null; path = path.getParent())
            if (Files.isRegularFile(path.resolve("build.gradle")) && Files.isDirectory(path.resolve("src/main"))) return path;
        throw new IllegalStateException("Cannot locate the project's generated advancement fixtures");
    }
    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test void publicCatalogHasOnlyDocumentedAchievementsAndBloomRecords() throws Exception {
        Set<String> expected = new HashSet<>(Set.of("root", "blooms"));
        for (var id : CosmicAchievementIds.ALL) expected.add(id.getPath());
        for (String bloom : new String[]{"quiet_assurance", "gentle_lies", "waning_mercy",
                "constricting_bonds", "unspoken_resignation", "elegy"})
            expected.add("blooms/bloom_of_" + bloom);
        assertEquals(30, CosmicAchievementIds.ALL.size());
        assertEquals(38, expected.size());
        Set<String> actual = new HashSet<>();
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                if (json(path).has("display")) actual.add(ROOT.relativize(path).toString().replace('\\', '/').replace(".json", ""));
            }
        }
        assertEquals(expected, actual);
        assertFalse(expected.contains("achievements/first_player_trade"));
        assertFalse(expected.contains("pyroclast/boom"));
    }

    @Test void everyDisplayedAdvancementHasReadableCopyAndAnIntentionalIcon() throws Exception {
        JsonObject language = json(LANGUAGE);
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject advancement = json(path);
                if (!advancement.has("display")) continue;
                JsonObject display = advancement.getAsJsonObject("display");
                for (String field : new String[]{"title", "description"}) {
                    JsonObject component = display.getAsJsonObject(field);
                    String key = component.get("translate").getAsString();
                    assertTrue(language.has(key), key);
                    String value = language.get(key).getAsString();
                    assertFalse(value.isBlank(), path + " " + field);
                    assertFalse(value.toLowerCase().contains("placeholder"), path.toString());
                }
                assertNotEquals("minecraft:paper", display.getAsJsonObject("icon").get("id").getAsString(), path.toString());
            }
        }
    }

    @Test void retiredIdsRetainCriteriaWithoutPlayerFacingAwards() throws Exception {
        for (String id : new String[]{"dungeon_monsters", "player_classes", "pyroclast",
                "pyroclast/boom", "achievements/first_player_trade"}) {
            JsonObject advancement = json(ROOT.resolve(id + ".json"));
            assertFalse(advancement.has("display"), id);
            assertFalse(advancement.has("rewards"), id);
            String criterion = id.contains("/") ? "triggered" : "tick";
            assertEquals(Set.of(criterion), advancement.getAsJsonObject("criteria").keySet(), id);
            assertEquals("minecraft:impossible", advancement.getAsJsonObject("criteria").getAsJsonObject(criterion).get("trigger").getAsString(), id);
        }
    }

    @Test void retainedProgressCriteriaAndSecretTaxRemainCompatible() throws Exception {
        for (var id : CosmicAchievementIds.ALL) {
            JsonObject advancement = json(ROOT.resolve(id.getPath() + ".json"));
            assertEquals(Set.of("triggered"), advancement.getAsJsonObject("criteria").keySet(), id.toString());
        }
        try (var paths = Files.list(ROOT.resolve("blooms"))) {
            for (Path path : paths.toList()) assertEquals(Set.of("shared"), json(path).getAsJsonObject("criteria").keySet());
        }
        assertTrue(json(ROOT.resolve("achievements/the_tamsin_tax.json")).getAsJsonObject("display").get("hidden").getAsBoolean());
    }
}
