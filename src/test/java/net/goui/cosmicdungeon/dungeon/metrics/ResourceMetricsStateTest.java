package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ResourceMetricsStateTest {
    @TempDir Path temporary;
    private static final UUID WORLD = new UUID(1, 1), OWNER = new UUID(2, 2), DONOR = new UUID(3, 3);
    private static ResourceMetricsBatch batch(long sequence, long cursor) {
        UUID transaction = new UUID(0, sequence);
        return new ResourceMetricsBatch(WORLD, 1, transaction, sequence, cursor, cursor + 1, Map.of(DONOR, cursor + 1),
                List.of(new RunMetrics.ResourceEvent(transaction, OWNER, OWNER, "Brew", "minecraft:glass_bottle", 1, true, sequence),
                        new RunMetrics.ResourceEvent(transaction, DONOR, OWNER, "Brew", "minecraft:nether_wart", 1, false, sequence)));
    }
    @Test void ownAndSharedCountersRemainSeparateAndReplayDoesNotAddAgain() throws Exception {
        var store = new RunMetricsStore(temporary, WORLD); var input = batch(1, 0);
        var state = store.commitResources(input);
        assertEquals(1, state.members().get(OWNER).own().get("minecraft:glass_bottle"));
        assertEquals(1, state.members().get(OWNER).used().get("minecraft:nether_wart"));
        assertEquals(1, state.members().get(DONOR).contributed().get("minecraft:nether_wart"));
        assertTrue(state.members().get(DONOR).used().isEmpty());
        assertEquals(2, state.members().get(OWNER).events());
        assertEquals(state, new RunMetricsStore(temporary).commitResources(input));
        assertEquals(state, ResourceMetricsStateXml.decode(ResourceMetricsStateXml.encode(state)));
        assertEquals(ResourceMetricsState.empty(WORLD, 2),
                ResourceMetricsStateXml.decode(ResourceMetricsStateXml.encode(ResourceMetricsState.empty(WORLD, 2))));
    }
    @Test void crashAfterSegmentOrAfterStateRecoversExactlyOnce() throws Exception {
        for (String point : List.of("resources/", "resource-state/")) {
            var armed = new AtomicBoolean(false); Path path = temporary.resolve(point.replace("/", ""));
            var store = new RunMetricsStore(path, WORLD, written -> {
                if (armed.get() && written.startsWith(point)) throw new SimulatedCrash();
            });
            armed.set(true); assertThrows(SimulatedCrash.class, () -> store.commitResources(batch(1, 0)));
            var resumed = new RunMetricsStore(path); var state = resumed.commitResources(batch(1, 0));
            assertEquals(1, state.sequence()); assertEquals(1, state.cursor());
            assertEquals(1, state.members().get(DONOR).contributed().get("minecraft:nether_wart"));
            assertEquals(state, resumed.commitResources(batch(1, 0)));
        }
    }
    @Test void staleSequenceAndConflictingReplayCannotChangeFiles() throws Exception {
        var store = new RunMetricsStore(temporary, WORLD); var first = batch(1, 0); var state = store.commitResources(first);
        assertThrows(IllegalArgumentException.class, () -> store.commitResources(batch(3, 1)));
        var conflicting = new ResourceMetricsBatch(WORLD, 1, first.transaction(), 1, 0, 1, first.donated(),
                List.of(new RunMetrics.ResourceEvent(first.transaction(), DONOR, OWNER, "Brew", "minecraft:redstone", 1, false, 1)));
        assertThrows(IOException.class, () -> store.commitResources(conflicting));
        assertEquals(state, store.readResourceState(1));
        assertEquals(first, ResourceMetricsXml.decode(Files.readAllBytes(temporary.resolve("resources/"
                + RunMetrics.instanceId(WORLD, 1) + "/" + first.transaction() + ".xml"))));
    }
    @Test void historyTailIsBoundedWhileAllSegmentsAndTotalsSurvive() throws Exception {
        var store = new RunMetricsStore(temporary, WORLD);
        for (int i = 1; i <= 80; i++) store.commitResources(batch(i, i - 1));
        var state = new RunMetricsStore(temporary).readResourceState(1);
        assertEquals(80, state.sequence()); assertEquals(64, state.recent().size());
        assertEquals(80, state.members().get(OWNER).used().get("minecraft:nether_wart"));
        try (var files = Files.list(temporary.resolve("resources/" + RunMetrics.instanceId(WORLD, 1)))) {
            assertEquals(80, files.count());
        }
    }
    @Test void futureStateIsRetainedWithoutAddingAnotherSegment() throws Exception {
        var store = new RunMetricsStore(temporary, WORLD); var state = store.commitResources(batch(1, 0));
        Path file = temporary.resolve("resource-state/" + RunMetrics.instanceId(WORLD, 1) + ".xml");
        byte[] future = new String(ResourceMetricsStateXml.encode(state), StandardCharsets.UTF_8)
                .replace("version=\"1\"", "version=\"2\"").getBytes(StandardCharsets.UTF_8);
        Files.write(file, future);
        assertThrows(IOException.class, () -> store.commitResources(batch(2, 1)));
        assertArrayEquals(future, Files.readAllBytes(file));
        assertFalse(Files.exists(temporary.resolve("resources/" + RunMetrics.instanceId(WORLD, 1) + "/" + batch(2, 1).transaction() + ".xml")));
    }
    @Test void xmlRejectsEntitiesOversizeAndUnknownData() throws Exception {
        var state = ResourceMetricsState.empty(WORLD, 1);
        String xml = new String(ResourceMetricsStateXml.encode(state), StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> ResourceMetricsStateXml.decode(xml.replace("</resource-state>", "<future/></resource-state>").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> ResourceMetricsStateXml.decode(new byte[ResourceMetricsStateXml.MAX_BYTES + 1]));
        assertThrows(IOException.class, () -> ResourceMetricsStateXml.decode("<!DOCTYPE x [<!ENTITY a SYSTEM 'file:///not-readable'>]><x>&a;</x>".getBytes(StandardCharsets.UTF_8)));
    }
    @Test void counterOverflowFailsBeforeAnyDurableMutation() {
        var member = new ResourceMetricsState.Member(1, Map.of(), Map.of("minecraft:nether_wart", (long)Integer.MAX_VALUE), Map.of());
        var old = new ResourceMetricsState(WORLD, 1, 1, new UUID(0, 1), 0, Map.of(), Map.of(OWNER, member), List.of());
        assertThrows(IllegalArgumentException.class, () -> old.apply(batch(2, 0)));
    }
    @Test void representativeStateStaysSmallAndReportsMeasuredCodecCost() throws Exception {
        var items = new TreeMap<String, Long>(); for (int i = 0; i < 64; i++) items.put("example:ingredient_" + i, 100L);
        var members = new TreeMap<UUID, ResourceMetricsState.Member>();
        for (int i = 0; i < 6; i++) members.put(new UUID(0, i), new ResourceMetricsState.Member(100, items, items, items));
        var state = new ResourceMetricsState(WORLD, 1, 1, UUID.randomUUID(), 0, Map.of(), members, List.of());
        long start = System.nanoTime(); var bytes = ResourceMetricsStateXml.encode(state);
        assertEquals(state, ResourceMetricsStateXml.decode(bytes)); assertTrue(bytes.length < 128 * 1024);
        System.out.println("RESOURCE_STATE_BASELINE members=6 item_types_each=64 xml_bytes=" + bytes.length
                + " encode_decode_micros=" + (System.nanoTime() - start) / 1000);
    }
    private static final class SimulatedCrash extends RuntimeException {}
}
