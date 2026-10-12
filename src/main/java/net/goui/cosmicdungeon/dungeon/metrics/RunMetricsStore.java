package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static net.goui.cosmicdungeon.dungeon.metrics.RunMetricsXml.*;

/** Single-writer store. A finalization journal contains exact afterimages, never increments to replay. */
public final class RunMetricsStore {
    private static final int MAX_JOURNAL_BYTES = 8 * 1024 * 1024;
    private final Path directory;
    private final UUID worldId;
    private final Consumer<String> written;
    public RunMetricsStore(Path directory) throws IOException { this(directory, null, ignored -> {}); }
    public RunMetricsStore(Path directory, UUID worldId) throws IOException { this(directory, worldId, ignored -> {}); }
    RunMetricsStore(Path directory, UUID requested, Consumer<String> written) throws IOException {
        this.directory = directory.toAbsolutePath().normalize(); this.written = written;
        Files.createDirectories(this.directory);
        Path identity = this.directory.resolve("identity.xml");
        if (Files.exists(identity)) {
            try {
                var e = parse(readBytes(identity, MAX_XML_BYTES), MAX_XML_BYTES);
                shape(e, "identity", "version world", ""); version(e); worldId = uuid(e, "world");
            } catch (RuntimeException ex) { throw new IOException("Invalid metrics identity", ex); }
            if (requested != null && !requested.equals(worldId)) throw new IOException("Metrics world identity mismatch");
        } else {
            try (var entries = Files.list(this.directory)) {
                if (entries.findAny().isPresent()) throw new IOException("Metrics identity missing from nonempty archive; preserve and repair");
            }
            worldId = requested == null ? UUID.randomUUID() : requested;
            atomic("identity.xml", xml(w -> { start(w, "identity", "version", 1, "world", worldId); w.writeEndElement(); }, MAX_XML_BYTES));
        }
        recover();
    }
    public UUID worldId() { return worldId; }
    public synchronized RunMetrics read(long run) throws IOException { recover(); return readRun(run); }
    private RunMetrics readRun(long run) throws IOException {
        Path path = directory.resolve(RunMetrics.instanceId(worldId, run) + ".xml");
        if (!Files.exists(path)) return null;
        var result = decodeRun(readBytes(path, MAX_XML_BYTES));
        if (!result.worldId().equals(worldId) || result.runId() != run) throw new IOException("Metrics run identity mismatch");
        return result;
    }
    public synchronized void checkpoint(RunMetrics run) throws IOException {
        recover(); identity(run); if (run.finalized()) throw new IOException("Use finish for terminal run");
        var old = readRun(run.runId());
        if (old != null && old.finalized()) throw new IOException("Cannot overwrite finalized run");
        atomic(run.instanceId() + ".xml", encodeRun(run));
    }
    public synchronized RunMetrics.Totals totals(UUID member) throws IOException { recover(); return readTotals(member); }
    private RunMetrics.Totals readTotals(UUID member) throws IOException {
        String owner = member == null ? "server" : member.toString();
        Path path = directory.resolve(member == null ? "totals.xml" : "players/" + member + ".xml");
        if (!Files.exists(path)) return new RunMetrics.Totals(worldId, owner, 0, 0, 0, RunMetrics.Stats.EMPTY, List.of());
        var totals = decodeTotals(readBytes(path, MAX_XML_BYTES));
        if (!worldId.equals(totals.worldId()) || !owner.equals(totals.owner())) throw new IOException("Totals identity mismatch");
        return totals;
    }
    public synchronized void finish(RunMetrics run) throws IOException {
        recover(); identity(run); if (!run.finalized()) throw new IOException("Cannot finalize ACTIVE run");
        var old = readRun(run.runId());
        if (old != null && old.finalized()) {
            if (!old.equals(run)) throw new IOException("Conflicting finalized run; original retained");
            return;
        }
        try {
            var images = new LinkedHashMap<String, byte[]>();
            var aggregate = RunMetrics.Stats.EMPTY;
            for (var member : run.members()) {
                aggregate = aggregate.plus(member.stats());
                images.put("players/" + member.id() + ".xml", encodeTotals(readTotals(member.id()).add(run, member.stats())));
            }
            images.put("totals.xml", encodeTotals(readTotals(null).add(run, aggregate)));
            images.put(run.instanceId() + ".xml", encodeRun(run)); // commit marker is always last
            byte[] journal = xml(w -> {
                start(w, "pending", "version", 1, "world", worldId, "run", run.runId());
                for (var entry : images.entrySet()) {
                    start(w, "image", "path", entry.getKey(), "data", Base64.getEncoder().encodeToString(entry.getValue()));
                    w.writeEndElement();
                }
                w.writeEndElement();
            }, MAX_JOURNAL_BYTES);
            atomic("pending.xml", journal); recover();
        } catch (IllegalArgumentException | ArithmeticException ex) { throw new IOException("Metrics totals exceed supported bounds", ex); }
    }
    public synchronized void recover() throws IOException {
        Path journal = directory.resolve("pending.xml");
        if (!Files.exists(journal)) return;
        try {
            var root = parse(readBytes(journal, MAX_JOURNAL_BYTES), MAX_JOURNAL_BYTES);
            shape(root, "pending", "version world run", "image"); version(root);
            if (!worldId.equals(uuid(root, "world"))) throw new IOException("Journal world mismatch");
            long runId = n(root, "run"); String terminal = RunMetrics.instanceId(worldId, runId) + ".xml";
            var images = new LinkedHashMap<String, byte[]>();
            for (var e : children(root)) {
                shape(e, "image", "path data", "");
                String path = a(e, "path");
                if (images.put(path, Base64.getDecoder().decode(a(e, "data"))) != null) throw new IOException("Duplicate journal image");
            }
            if (images.size() < 2 || images.size() > 8 || !new ArrayList<>(images.keySet()).getLast().equals(terminal))
                throw new IOException("Invalid journal commit marker");
            var run = decodeRun(Objects.requireNonNull(images.get(terminal)));
            identity(run);
            if (!run.finalized() || run.runId() != runId) throw new IOException("Invalid journal run");
            Set<String> expected = new HashSet<>(Set.of("totals.xml", terminal));
            for (var m : run.members()) expected.add("players/" + m.id() + ".xml");
            if (!images.keySet().equals(expected)) throw new IOException("Unknown journal destination");
            // Validate all afterimages AND existing schemas before changing any file.
            for (var entry : images.entrySet()) {
                if (entry.getKey().equals(terminal)) {
                    var existing = readRun(runId);
                    if (existing != null && existing.finalized() && !existing.equals(run)) throw new IOException("Conflicting journal finalization");
                } else {
                    UUID owner = entry.getKey().equals("totals.xml") ? null : UUID.fromString(entry.getKey().substring(8, 44));
                    var t = decodeTotals(entry.getValue()); readTotals(owner);
                    if (!worldId.equals(t.worldId()) || !(owner == null ? "server" : owner.toString()).equals(t.owner()))
                        throw new IOException("Journal totals identity mismatch");
                }
            }
            for (var entry : images.entrySet()) atomic(entry.getKey(), entry.getValue());
            Files.delete(journal); written.accept("journal-deleted");
        } catch (IllegalArgumentException | NullPointerException ex) { throw new IOException("Invalid metrics journal; original retained", ex); }
    }
    private void identity(RunMetrics run) throws IOException {
        if (!worldId.equals(run.worldId())) throw new IOException("Foreign world metrics");
    }
    private void atomic(String relative, byte[] bytes) throws IOException {
        Path target = directory.resolve(relative), temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.createDirectories(target.getParent());
        try (var channel = FileChannel.open(temporary, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            var buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
        }
        // Fail closed on filesystems without atomic rename; never truncate the previous durable snapshot.
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        written.accept(relative);
    }
    private static byte[] readBytes(Path file, int limit) throws IOException {
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(limit + 1);
            if (bytes.length > limit) throw new IOException("Oversized metrics file: " + file.getFileName());
            return bytes;
        }
    }
    public synchronized void rebuild(Path destination) throws IOException {
        recover();
        Path target = destination.toAbsolutePath().normalize();
        if (target.startsWith(directory) || directory.startsWith(target)) throw new IOException("Rebuild requires a separate empty directory");
        if (Files.exists(target)) try (var entries = Files.list(target)) {
            if (entries.findAny().isPresent()) throw new IOException("Rebuild destination must be empty");
        }
        var output = new RunMetricsStore(target, worldId);
        // Stream one canonical instance at a time; archive size does not increase runtime heap.
        try (var entries = Files.newDirectoryStream(directory, "*.xml")) {
            for (var entry : entries) {
                String name = entry.getFileName().toString();
                if (!name.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.xml")) continue;
                var run = decodeRun(readBytes(entry, MAX_XML_BYTES)); identity(run);
                if (!name.equals(run.instanceId() + ".xml")) throw new IOException("Mismatched canonical instance filename");
                if (run.finalized()) { copyResourceHistory(run, output); output.finish(run); }
            }
        }
    }
    /** Idempotent immutable transaction segments, independent of active/final aggregate snapshots. */
    public synchronized void writeResources(ResourceMetricsBatch batch) throws IOException {
        recover();
        if (!worldId.equals(batch.worldId())) throw new IOException("Foreign resource world");
        String relative = "resources/" + RunMetrics.instanceId(worldId, batch.runId()) + "/" + batch.transaction() + ".xml";
        Path file = directory.resolve(relative); byte[] encoded = ResourceMetricsXml.encode(batch);
        if (Files.exists(file)) {
            if (!ResourceMetricsXml.decode(readBytes(file, ResourceMetricsXml.MAX_BYTES)).equals(batch))
                throw new IOException("Conflicting resource segment; original retained");
        } else atomic(relative, encoded);
    }
    public synchronized ResourceMetricsState readResourceState(long run) throws IOException {
        recover();
        Path path = directory.resolve("resource-state/" + RunMetrics.instanceId(worldId, run) + ".xml");
        if (!Files.exists(path)) return ResourceMetricsState.empty(worldId, run);
        var state = ResourceMetricsStateXml.decode(readBytes(path, ResourceMetricsStateXml.MAX_BYTES));
        if (!worldId.equals(state.worldId()) || state.runId() != run) throw new IOException("Resource state identity differs");
        return state;
    }
    /** Forced atomic afterimage is authoritative before any in-memory/SavedData counters change. */
    public synchronized ResourceMetricsState commitResources(ResourceMetricsBatch batch) throws IOException {
        var previous = readResourceState(batch.runId()); var next = previous.apply(batch);
        writeResources(batch);
        if (!next.equals(previous))
            atomic("resource-state/" + RunMetrics.instanceId(worldId, batch.runId()) + ".xml", ResourceMetricsStateXml.encode(next));
        return next;
    }
    /** Verify an offline copy one segment at a time; the index bounds heap independently of history length. */
    private void copyResourceHistory(RunMetrics run, RunMetricsStore output) throws IOException {
        String stateName = "resource-state/" + run.instanceId() + ".xml";
        Path stateFile = directory.resolve(stateName), resources = directory.resolve("resources/" + run.instanceId());
        byte[] stateBytes = Files.exists(stateFile) ? readBytes(stateFile, ResourceMetricsStateXml.MAX_BYTES) : null;
        var expected = stateBytes == null ? ResourceMetricsState.empty(worldId, run.runId())
                : ResourceMetricsStateXml.decode(stateBytes);
        if (!worldId.equals(expected.worldId()) || run.runId() != expected.runId())
            throw new IOException("Resource state archive identity differs; originals retained");
        if (!Files.exists(resources)) {
            if (expected.sequence() > 0) throw new IOException("Resource segment directory missing; originals retained");
            if (stateBytes != null) output.atomic(stateName, stateBytes);
            return; // v1 aggregate/embedded history predates segments; never invent its missing detail.
        }
        if (!Files.isDirectory(resources)) throw new IOException("Resource history is not a directory; originals retained");
        var owners = new HashSet<UUID>(); run.members().forEach(member -> owners.add(member.id()));
        if (!owners.containsAll(expected.members().keySet()))
            throw new IOException("Resource state owner differs from canonical run; originals retained");
        Path index = Files.createTempDirectory(output.directory, ".resource-rebuild-");
        try {
            long count = 0;
            try (var files = Files.newDirectoryStream(resources, "*.xml")) {
                for (var file : files) {
                    byte[] bytes = readBytes(file, ResourceMetricsXml.MAX_BYTES);
                    var batch = ResourceMetricsXml.decode(bytes);
                    if (!batch.worldId().equals(worldId) || batch.runId() != run.runId()
                            || !file.getFileName().toString().equals(batch.transaction() + ".xml")
                            || batch.events().stream().anyMatch(e -> !owners.contains(e.donor()) || !owners.contains(e.recipient())))
                        throw new IOException("Resource segment archive identity differs; originals retained");
                    if (stateBytes == null) throw new IOException("Resource segments have no saved state; originals retained");
                    if (batch.sequence() > expected.sequence())
                        throw new IOException("Resource segment exceeds saved sequence; originals retained");
                    try {
                        Files.write(index.resolve(batch.sequence() + ".xml"), bytes,
                                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    } catch (FileAlreadyExistsException ex) {
                        throw new IOException("Duplicate resource segment sequence; originals retained", ex);
                    }
                    count = Math.addExact(count, 1);
                }
            }
            if (count != expected.sequence()) throw new IOException("Missing resource segments; originals retained");
            var rebuilt = ResourceMetricsState.empty(worldId, run.runId());
            while (rebuilt.sequence() < expected.sequence()) {
                long next = Math.addExact(rebuilt.sequence(), 1);
                var batch = ResourceMetricsXml.decode(readBytes(index.resolve(next + ".xml"), ResourceMetricsXml.MAX_BYTES));
                rebuilt = rebuilt.apply(batch);
            }
            if (!rebuilt.equals(expected))
                throw new IOException("Resource segments differ from saved afterimage; originals retained");
            // Only verified bytes reach the destination. Canonical totals are rebuilt separately, never added twice.
            try (var files = Files.newDirectoryStream(index, "*.xml")) {
                for (var file : files) {
                    byte[] bytes = readBytes(file, ResourceMetricsXml.MAX_BYTES);
                    var batch = ResourceMetricsXml.decode(bytes);
                    output.atomic("resources/" + run.instanceId() + "/" + batch.transaction() + ".xml", bytes);
                }
            }
            if (stateBytes != null) output.atomic(stateName, stateBytes);
        } catch (RuntimeException ex) {
            throw new IOException("Invalid resource history; originals retained", ex);
        } finally {
            try (var files = Files.newDirectoryStream(index)) {
                for (var file : files) Files.delete(file);
            }
            Files.delete(index); // only this invocation's temporary copies; authoritative originals are untouched.
        }
    }
    public static void main(String[] args) throws IOException {
        if (args.length != 3 || (!args[0].equals("rebuild") && !args[0].equals("history")))
            throw new IllegalArgumentException("Usage: rebuild <archive> <empty-directory> | history <archive> <player-UUID>");
        var store = new RunMetricsStore(Path.of(args[1]));
        if (args[0].equals("rebuild")) store.rebuild(Path.of(args[2]));
        else System.out.write(encodeTotals(store.totals(UUID.fromString(args[2]))));
    }
}
