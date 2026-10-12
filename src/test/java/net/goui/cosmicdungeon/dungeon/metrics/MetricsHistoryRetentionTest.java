package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Copies only isolated fixture directories; never opens the configured world archive. */
class MetricsHistoryRetentionTest {
    @TempDir Path temporary;
    private static final UUID WORLD = new UUID(2500, 1);
    private static final UUID OWNER = new UUID(2500, 2);
    private static final List<UUID> MEMBERS = List.of(OWNER, new UUID(2500, 3),
            new UUID(2500, 4), new UUID(2500, 5), new UUID(2500, 6), new UUID(2500, 7));

    private static ResourceMetricsBatch batch(long run, long sequence, int itemTypes) {
        UUID tx = new UUID(run, sequence);
        long before = Math.multiplyExact(sequence - 1, 5);
        var ranks = new LinkedHashMap<UUID, Long>();
        var events = new ArrayList<RunMetrics.ResourceEvent>();
        String suffix = Long.toString((sequence - 1) % itemTypes);
        events.add(new RunMetrics.ResourceEvent(tx, OWNER, OWNER, "Brew & <Light>",
                "example:own_" + suffix, 1, true, 1000 + sequence));
        for (int i = 1; i < MEMBERS.size(); i++) {
            UUID donor = MEMBERS.get(i);
            ranks.put(donor, before + i);
            events.add(new RunMetrics.ResourceEvent(tx, donor, OWNER, "Brew & <Light>",
                    "example:shared_" + i + "_" + suffix, 1, false, 1000 + sequence));
        }
        return new ResourceMetricsBatch(WORLD, run, tx, sequence, before, before + 5, ranks, events);
    }
    private static RunMetrics terminal(long id, String outcome, ResourceMetricsState state, boolean heavy) {
        var members = new ArrayList<RunMetrics.Member>();
        for (int i = 0; i < MEMBERS.size(); i++) {
            UUID owner = MEMBERS.get(i);
            var resources = state.members().getOrDefault(owner, ResourceMetricsState.Member.EMPTY);
            var mobs = new TreeMap<String, Long>();
            if (heavy) for (int key = 0; key < 128; key++) mobs.put("example:hostile_" + key, 1L);
            var stats = new RunMetrics.Stats(heavy ? 128 : id + i, i, 12.5, 4.5,
                    resources.events(), mobs, resources.used(), resources.contributed(), i);
            members.add(new RunMetrics.Member(owner, "Member & <" + i + ">", "theurgist",
                    i == 0 ? "EXITED" : "ACTIVE", stats));
        }
        return new RunMetrics(WORLD, id, new UUID(2501, id), "dungeon_1", "HARD",
                1000 + id, 3000 + id, 3000 + id, outcome, "COMPLETE", members, state.recent());
    }
    private static RunMetrics finish(RunMetricsStore store, long id, String outcome, int crafts, boolean heavy)
            throws Exception {
        ResourceMetricsState state = ResourceMetricsState.empty(WORLD, id);
        for (int sequence = 1; sequence <= crafts; sequence++)
            state = store.commitResources(batch(id, sequence, heavy ? 64 : 1));
        var run = terminal(id, outcome, state, heavy);
        store.finish(run);
        return run;
    }
    private static String instance(long id) { return RunMetrics.instanceId(WORLD, id); }
    private static Path segment(Path directory, long id, long sequence) {
        return directory.resolve("resources/" + instance(id) + "/" + new UUID(id, sequence) + ".xml");
    }
    private static Map<String, byte[]> image(Path directory) throws IOException {
        var result = new TreeMap<String, byte[]>();
        if (!Files.exists(directory)) return result;
        try (var paths = Files.walk(directory)) {
            for (Path file : paths.filter(Files::isRegularFile).toList())
                result.put(directory.relativize(file).toString().replace('\\', '/'), Files.readAllBytes(file));
        }
        return result;
    }
    private static void sameImage(Map<String, byte[]> expected, Path directory) throws IOException {
        var actual = image(directory);
        assertEquals(expected.keySet(), actual.keySet(), "Every original file remains");
        for (var entry : expected.entrySet())
            assertArrayEquals(entry.getValue(), actual.get(entry.getKey()), entry.getKey());
    }
    private static void copy(Path source, Path destination) throws IOException {
        Files.createDirectories(destination);
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path)) Files.createDirectories(target);
                else Files.copy(path, target);
            }
        }
    }
    private static List<ResourceMetricsBatch> segments(Path directory, long run) throws IOException {
        Path folder = directory.resolve("resources/" + instance(run));
        var result = new ArrayList<ResourceMetricsBatch>();
        try (var paths = Files.list(folder)) {
            for (Path path : paths.toList()) {
                var batch = ResourceMetricsXml.decode(Files.readAllBytes(path));
                assertEquals(WORLD, batch.worldId());
                assertEquals(run, batch.runId());
                assertEquals(batch.transaction() + ".xml", path.getFileName().toString());
                result.add(batch);
            }
        }
        result.sort(Comparator.comparingLong(ResourceMetricsBatch::sequence));
        return result;
    }

    @Test void copiedRestoredArchiveKeepsAllRunsEventsTotalsAndLastTenWithoutReplay() throws Exception {
        Path source = temporary.resolve("source"), copied = temporary.resolve("copied");
        Path rebuilt = temporary.resolve("rebuilt"), restored = temporary.resolve("restored");
        var store = new RunMetricsStore(source, WORLD);
        var runs = new ArrayList<RunMetrics>();
        for (long id = 1; id <= 12; id++)
            runs.add(finish(store, id, id % 2 == 0 ? "COMPLETED" : "ABANDONED", 13, false));
        var original = image(source);
        var expected = store.totals(OWNER);
        assertEquals(12, expected.runs()); assertEquals(6, expected.completed()); assertEquals(6, expected.aborted());
        assertEquals(10, expected.recent().size());
        assertEquals(12, expected.recent().getFirst().runId());
        assertEquals(3, expected.recent().getLast().runId());
        copy(source, copied);
        new RunMetricsStore(copied).rebuild(rebuilt);
        sameImage(original, source); sameImage(original, copied);
        copy(rebuilt, restored);
        var reopened = new RunMetricsStore(restored);
        assertEquals(store.totals(null), reopened.totals(null));
        for (UUID owner : MEMBERS) assertEquals(store.totals(owner), reopened.totals(owner));
        for (RunMetrics run : runs) {
            assertEquals(run, reopened.read(run.runId()));
            assertEquals(store.readResourceState(run.runId()), reopened.readResourceState(run.runId()));
            var full = segments(restored, run.runId());
            assertEquals(13, full.size());
            assertEquals(78, full.stream().mapToInt(b -> b.events().size()).sum());
            assertEquals(64, reopened.readResourceState(run.runId()).recent().size());
            for (int i = 0; i < full.size(); i++) assertEquals(batch(run.runId(), i + 1, 1), full.get(i));
        }
        sameImage(original, restored);
        var beforeReplay = image(restored);
        for (RunMetrics run : runs) {
            reopened.finish(run);
            assertEquals(store.readResourceState(run.runId()),
                    reopened.commitResources(batch(run.runId(), 13, 1)));
        }
        reopened.recover();
        sameImage(beforeReplay, restored);
        assertEquals(expected, reopened.totals(OWNER));
    }

    @Test void growthMeasurementsCoverCompletedAbortedAndResourceHeavyRuns() throws Exception {
        for (String scenario : List.of("completed", "aborted", "resource-heavy")) {
            Path directory = temporary.resolve(scenario);
            var store = new RunMetricsStore(directory, WORLD);
            long identityBytes = Files.size(directory.resolve("identity.xml"));
            boolean heavy = scenario.equals("resource-heavy");
            int crafts = heavy ? 120 : 8;
            var run = finish(store, 1, scenario.equals("aborted") ? "ABANDONED" : "COMPLETED", crafts, heavy);
            var files = image(directory);
            assertEquals(crafts + 10, files.size());
            var full = segments(directory, 1);
            assertEquals(crafts, full.size());
            assertEquals(crafts * 6, full.stream().mapToInt(b -> b.events().size()).sum());
            assertEquals(Math.min(64, crafts * 6), run.resourceEvents().size());
            long canonical = 0, totals = 0, state = 0, eventBytes = 0, disk = 0;
            for (var entry : files.entrySet()) {
                long bytes = entry.getValue().length; disk += bytes;
                String path = entry.getKey();
                if (path.equals(instance(1) + ".xml")) canonical += bytes;
                else if (path.equals("totals.xml") || path.startsWith("players/")) totals += bytes;
                else if (path.startsWith("resource-state/")) state += bytes;
                else if (path.startsWith("resources/")) eventBytes += bytes;
            }
            assertEquals(identityBytes + canonical + totals + state + eventBytes, disk);
            System.out.println("METRICS_HISTORY_COST scenario=" + scenario + " members=6 crafts=" + crafts
                    + " full_events=" + crafts * 6 + " recent_events=" + run.resourceEvents().size()
                    + " identity_bytes=" + identityBytes + " canonical_bytes=" + canonical
                    + " totals_bytes=" + totals + " state_bytes=" + state + " segment_bytes=" + eventBytes
                    + " disk_bytes=" + disk + " segment_bytes_per_event=" + (eventBytes / (double)(crafts * 6))
                    + " filesystem=logical_file_bytes excludes=allocation_saveddata_world backups=0");
            Path copied = temporary.resolve(scenario + "-copied"), rebuilt = temporary.resolve(scenario + "-rebuilt");
            copy(directory, copied); new RunMetricsStore(copied).rebuild(rebuilt);
            sameImage(files, directory); sameImage(files, copied); sameImage(files, rebuilt);
        }
    }

    @Test void legacyAggregateOnlyAndEmbeddedResourceRowsRemainReadableWithoutInventedSegments() throws Exception {
        Path source = temporary.resolve("legacy");
        var store = new RunMetricsStore(source, WORLD);
        var stats = new RunMetrics.Stats(4, 1, 12.5, 4.5, 7, Map.of("minecraft:zombie", 4L),
                Map.of("minecraft:nether_wart", 3L), Map.of("minecraft:glowstone_dust", 4L), 2);
        var members = List.of(new RunMetrics.Member(OWNER, "Historical", "theurgist", "EXITED", stats),
                new RunMetrics.Member(MEMBERS.get(1), "Historical donor", "theurgist", "EXITED", RunMetrics.Stats.EMPTY));
        var old = new RunMetrics(WORLD, 1, new UUID(2501, 1), "dungeon_1", "HARD",
                1001, 3001, 3001, "COMPLETED", "PARTIAL_LEGACY", members, List.of());
        var embedded = new RunMetrics.ResourceEvent(new UUID(2502, 1), MEMBERS.get(1), OWNER,
                "Historical brew", "minecraft:nether_wart", 3, false, 2000);
        var withRows = new RunMetrics(WORLD, 2, new UUID(2501, 2), "dungeon_1", "HARD",
                1002, 3002, 3002, "ABANDONED", "COMPLETE", members, List.of(embedded));
        store.finish(old); store.finish(withRows);
        Files.createDirectories(source.resolve("resources/" + instance(1)));
        var original = image(source);
        Path copied = temporary.resolve("legacy-copied"), rebuilt = temporary.resolve("legacy-rebuilt");
        copy(source, copied); new RunMetricsStore(copied).rebuild(rebuilt);
        var reopened = new RunMetricsStore(rebuilt);
        assertEquals(old, reopened.read(1)); assertEquals(withRows, reopened.read(2));
        assertEquals(store.totals(null), reopened.totals(null));
        assertEquals(store.totals(OWNER), reopened.totals(OWNER));
        assertFalse(Files.exists(rebuilt.resolve("resources")));
        assertFalse(Files.exists(rebuilt.resolve("resource-state")));
        sameImage(original, source); sameImage(original, copied); sameImage(original, rebuilt);
    }

    @Test void segmentedPartialLegacyRetainsCanonicalCountersIndependentOfSegmentAfterimage() throws Exception {
        Path source = temporary.resolve("mixed-legacy");
        var store = new RunMetricsStore(source, WORLD);
        ResourceMetricsState state = ResourceMetricsState.empty(WORLD, 1);
        for (int sequence = 1; sequence <= 3; sequence++) state = store.commitResources(batch(1, sequence, 1));
        var current = terminal(1, "COMPLETED", state, false);
        var members = new ArrayList<>(current.members());
        var owner = members.getFirst(); var stats = owner.stats();
        var used = new TreeMap<>(stats.used()); used.put("example:historical_only", 9L);
        members.set(0, new RunMetrics.Member(owner.id(), owner.name(), owner.classId(), owner.status(),
                new RunMetrics.Stats(stats.kills(), stats.deaths(), stats.damage(), stats.healing(),
                        stats.resourceEvents() + 2, stats.mobs(), used, stats.contributed(), stats.spawners())));
        var mixed = new RunMetrics(current.worldId(), current.runId(), current.partyId(), current.dungeon(),
                current.difficulty(), current.started(), current.updated(), current.ended(), current.outcome(),
                "PARTIAL_LEGACY", members, List.of());
        store.finish(mixed);
        var original = image(source);
        Path rebuilt = temporary.resolve("mixed-legacy-rebuilt");
        store.rebuild(rebuilt);
        var reopened = new RunMetricsStore(rebuilt);
        assertEquals(mixed, reopened.read(1));
        assertEquals(store.totals(OWNER), reopened.totals(OWNER));
        assertEquals(state, reopened.readResourceState(1));
        sameImage(original, source); sameImage(original, rebuilt);
    }

    private RunMetricsStore defectiveFixture(String name) throws Exception {
        Path source = temporary.resolve(name);
        var store = new RunMetricsStore(source, WORLD);
        finish(store, 1, "COMPLETED", 3, false);
        return store;
    }
    private void rejectedWithoutChangingOriginals(RunMetricsStore store, Path source, String name) throws Exception {
        var original = image(source);
        assertThrows(IOException.class, () -> store.rebuild(temporary.resolve(name + "-rejected")));
        sameImage(original, source);
    }
    @Test void missingPositiveStateSegmentDirectoryFailsVisiblyAndPreservesOriginals() throws Exception {
        String name = "missing-directory"; var store = defectiveFixture(name); Path source = temporary.resolve(name);
        Path folder = source.resolve("resources/" + instance(1));
        try (var files = Files.list(folder)) { for (Path file : files.toList()) Files.delete(file); }
        Files.delete(folder);
        rejectedWithoutChangingOriginals(store, source, name);
    }
    @Test void segmentsWithoutTheirStateFailVisiblyAndPreserveOriginals() throws Exception {
        String name = "missing-state"; var store = defectiveFixture(name); Path source = temporary.resolve(name);
        Files.delete(source.resolve("resource-state/" + instance(1) + ".xml"));
        rejectedWithoutChangingOriginals(store, source, name);
    }
    @Test void missingMiddleSegmentFailsVisiblyAndPreservesOriginals() throws Exception {
        String name = "missing-middle"; var store = defectiveFixture(name); Path source = temporary.resolve(name);
        Files.delete(segment(source, 1, 2));
        rejectedWithoutChangingOriginals(store, source, name);
    }
    @Test void partialSegmentFailsVisiblyAndPreservesOriginals() throws Exception {
        String name = "partial"; var store = defectiveFixture(name); Path source = temporary.resolve(name);
        Path file = segment(source, 1, 2); var bytes = Files.readAllBytes(file);
        Files.write(file, Arrays.copyOf(bytes, bytes.length / 2));
        rejectedWithoutChangingOriginals(store, source, name);
    }
    @Test void conflictingSequenceFailsVisiblyAndPreservesOriginals() throws Exception {
        String name = "conflicting"; var store = defectiveFixture(name); Path source = temporary.resolve(name);
        var original = batch(1, 2, 1); UUID tx = new UUID(2503, 2);
        var events = original.events().stream().map(e -> new RunMetrics.ResourceEvent(tx, e.donor(),
                e.recipient(), e.skill(), e.item(), e.quantity(), e.own(), e.at())).toList();
        var conflict = new ResourceMetricsBatch(WORLD, 1, tx, 2, original.donationBefore(),
                original.donationAfter(), original.donated(), events);
        Files.write(source.resolve("resources/" + instance(1) + "/" + tx + ".xml"), ResourceMetricsXml.encode(conflict));
        rejectedWithoutChangingOriginals(store, source, name);
    }
    @Test void conflictingAfterimageFailsVisiblyAndPreservesOriginals() throws Exception {
        String name = "afterimage"; var store = defectiveFixture(name); Path source = temporary.resolve(name);
        var old = store.readResourceState(1);
        var members = new TreeMap<>(old.members()); var owner = members.get(OWNER);
        var used = new TreeMap<>(owner.used()); used.merge("example:shared_1_0", 1L, Long::sum);
        members.put(OWNER, new ResourceMetricsState.Member(owner.events(), owner.own(), used, owner.contributed()));
        var conflicting = new ResourceMetricsState(old.worldId(), old.runId(), old.sequence(), old.transaction(),
                old.cursor(), old.donated(), members, old.recent());
        Files.write(source.resolve("resource-state/" + instance(1) + ".xml"), ResourceMetricsStateXml.encode(conflicting));
        rejectedWithoutChangingOriginals(store, source, name);
    }
}
