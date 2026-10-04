package net.goui.cosmicdungeon.dungeon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class DungeonSnapshotFilesTest {
    @TempDir Path root;
    private final DungeonSnapshotFiles files = new DungeonSnapshotFiles();

    private Path authored() throws IOException {
        Path live = Files.createDirectory(root.resolve("authoring"));
        for (String folder : new String[]{"region", "entities", "poi", "data"}) {
            Files.createDirectory(live.resolve(folder));
            Files.write(live.resolve(folder).resolve("fixture.bin"), new byte[]{0, 1, 8, 42, -1});
        }
        return live;
    }

    @Test void eachRunCopiesSavedBlocksEntitiesPoiAndDataWithoutReusingEarlierMobs() throws IOException {
        Path live = authored();
        Path saved = root.resolve("saved");
        files.publish(saved, Map.of("dungeon", live));
        Files.writeString(live.resolve("entities/fixture.bin"), "unsaved template changes");
        Path first = root.resolve("run_1");
        files.copyFresh(saved.resolve("dungeon"), first);
        Files.writeString(first.resolve("entities/fixture.bin"), "zombies and previous dog");
        Path second = root.resolve("run_2");
        files.copyFresh(saved.resolve("dungeon"), second);
        for (String folder : new String[]{"region", "entities", "poi", "data"}) {
            assertArrayEquals(new byte[]{0, 1, 8, 42, -1}, Files.readAllBytes(second.resolve(folder + "/fixture.bin")));
        }
        files.remove(first);
        assertFalse(Files.exists(first));
        assertTrue(Files.isDirectory(second));
        assertTrue(Files.isDirectory(saved));
        assertTrue(Files.isDirectory(live));
    }

    @Test void aLaterExplicitSaveBecomesAnIndependentSnapshot() throws IOException {
        Path live = authored();
        files.publish(root.resolve("save_1"), Map.of("dungeon", live));
        Files.writeString(live.resolve("entities/fixture.bin"), "new villager");
        files.publish(root.resolve("save_2"), Map.of("dungeon", live));
        files.copyFresh(root.resolve("save_2/dungeon"), root.resolve("run_3"));
        assertEquals("new villager", Files.readString(root.resolve("run_3/entities/fixture.bin")));
        assertArrayEquals(new byte[]{0, 1, 8, 42, -1}, Files.readAllBytes(root.resolve("save_1/dungeon/entities/fixture.bin")));
    }

    @Test void existingInstanceContentsAreNeverOverwrittenOrMerged() throws IOException {
        Path live = authored();
        Path occupied = Files.createDirectory(root.resolve("occupied"));
        Files.writeString(occupied.resolve("original"), "keep");
        assertThrows(IOException.class, () -> files.copyFresh(live, occupied));
        assertEquals("keep", Files.readString(occupied.resolve("original")));
        assertFalse(Files.exists(occupied.resolve("entities")));
    }

    @Test void partialAndInterruptedSavesCannotBePublishedOrListed() throws IOException {
        Path live = authored();
        var dimensions = new LinkedHashMap<String, Path>();
        dimensions.put("first", live);
        dimensions.put("missing", root.resolve("absent"));
        Path published = root.resolve("save_failed");
        assertThrows(IOException.class, () -> files.publish(published, dimensions));
        assertFalse(Files.exists(published));
        Path interrupted = Files.createDirectory(root.resolve(".saving-crash"));
        assertFalse(DungeonSnapshotFiles.published(interrupted));
        files.publish(root.resolve("save_ok"), Map.of("dungeon", live));
        assertTrue(DungeonSnapshotFiles.published(root.resolve("save_ok")));
    }

    @Test void aSnapshotCannotBeOverwrittenAndCleanupIsIdempotent() throws IOException {
        Path live = authored();
        Path saved = root.resolve("save");
        files.publish(saved, Map.of("dungeon", live));
        assertThrows(IOException.class, () -> files.publish(saved, Map.of("dungeon", live)));
        files.remove(root.resolve("already_removed"));
        assertTrue(Files.exists(saved.resolve("dungeon/entities/fixture.bin")));
    }
}
