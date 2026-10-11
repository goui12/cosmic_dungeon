package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Main-server-thread adapter. Hot events only change existing SavedData; no per-event disk writes. */
public final class DungeonMetricsService {
    private static final String KEY = "metrics_v1:";
    private static final Map<MinecraftServer, Context> CONTEXTS = new WeakHashMap<>();
    private static final class Context {
        RunMetricsStore store; long lastWarning; int cursor;
    }
    private DungeonMetricsService() {}
    private static Context context(MinecraftServer server) { return CONTEXTS.computeIfAbsent(server, ignored -> new Context()); }
    public static RunMetricsStore archive(MinecraftServer server) throws IOException {
        var c = context(server);
        if (c.store == null) c.store = new RunMetricsStore(server.getWorldPath(LevelResource.ROOT).resolve("Dungeon Instance Metrics"));
        return c.store;
    }
    private static String value(D1RunData d, long run, String key, String fallback) {
        var list = d.values(run, KEY + key); return list.isEmpty() ? fallback : list.getFirst();
    }
    private static String className(ServerPlayer player) {
        String name = ClassData.getClassId(player);
        return name == null || name.isBlank() || name.length() > 64 || name.chars().anyMatch(c -> c < 32)
                ? "unknown" : name;
    }
    public static void begin(MinecraftServer server, DungeonRunRegistryData.RunRecord run, boolean fresh) {
        var d = D1RunData.get(server); long id = run.runId();
        if (d.values(id, KEY + "coverage").isEmpty())
            d.setValue(id, KEY + "coverage", fresh && run.dungeonId().equals("dungeon_1") ? "COMPLETE" : "PARTIAL_LEGACY");
        for (UUID owner : run.orderedPlayers()) {
            d.recordUnique(id, KEY + "members", owner.toString());
            var p = server.getPlayerList().getPlayer(owner);
            if (d.values(id, KEY + "name:" + owner).isEmpty())
                d.setValue(id, KEY + "name:" + owner, p == null ? owner.toString() : p.getGameProfile().name());
            if (d.values(id, KEY + "class:" + owner).isEmpty())
                d.setValue(id, KEY + "class:" + owner, p == null ? "unknown" : className(p));
        }
    }
    /** Called after the selected classes have been applied and the lobby entry has succeeded. */
    public static void party(MinecraftServer server, DungeonRunRegistryData.RunRecord run, UUID party) {
        begin(server, run, true); var d = D1RunData.get(server);
        d.setValue(run.runId(), KEY + "party", party.toString());
        for (UUID owner : run.orderedPlayers()) {
            var player = server.getPlayerList().getPlayer(owner);
            if (player != null) d.setValue(run.runId(), KEY + "class:" + owner, className(player));
        }
        checkpoint(server, run);
    }
    public static void recordMobKill(MinecraftServer server, DungeonRunRegistryData.RunRecord run, UUID credited, String entityId) {
        increment(D1RunData.get(server), run.runId(), "mob:" + credited + ":", entityId);
    }
    private static void increment(D1RunData d, long run, String prefix, String entry) {
        String key = KEY + prefix + entry;
        if (d.count(run, key) == 0 && d.metricCounts(run, KEY + prefix).size() >= RunMetrics.MAX_KEYS - 1)
            entry = "cosmicdungeon:unclassified";
        key = KEY + prefix + entry;
        d.setCount(run, key, (int)Math.min(Integer.MAX_VALUE, (long)d.count(run, key) + 1));
    }
    public static void spawner(ServerPlayer player) {
        if (player == null) return;
        var run = D1Members.run(player.level()).orElse(null);
        if (run != null && D1Members.inside(player, run)) {
            var d = D1RunData.get(player.level().getServer()); String key = KEY + "spawners:" + player.getUUID();
            d.setCount(run.runId(), key, (int)Math.min(Integer.MAX_VALUE, (long)d.count(run.runId(), key) + 1));
        }
    }
    public static RunMetrics snapshot(MinecraftServer server, DungeonRunRegistryData.RunRecord run, String outcome) {
        begin(server, run, false);
        try {
            var store = archive(server); var d = D1RunData.get(server); long id = run.runId();
            var members = new ArrayList<RunMetrics.Member>();
            for (String text : d.values(id, KEY + "members")) {
                UUID owner = UUID.fromString(text);
                var mobs = new TreeMap<>(d.metricCounts(id, KEY + "mob:" + owner + ":"));
                long kills = d.count(id, "kills:" + owner), classified = mobs.values().stream().mapToLong(Long::longValue).sum();
                if (kills > classified) mobs.merge("cosmicdungeon:unclassified", kills - classified, Long::sum);
                var stats = new RunMetrics.Stats(kills, (long)RunMemberStats.value(d, id, owner, "deaths"),
                        RunMemberStats.value(d, id, owner, "damage"), RunMemberStats.value(d, id, owner, "healing_received"),
                        d.count(id, KEY + "resources:" + owner), mobs, d.metricCounts(id, KEY + "used:" + owner + ":"),
                        d.metricCounts(id, KEY + "contributed:" + owner + ":"), d.count(id, KEY + "spawners:" + owner));
                members.add(new RunMetrics.Member(owner, value(d, id, "name:" + owner, owner.toString()),
                        value(d, id, "class:" + owner, "unknown"), run.isCompletionExited(owner) ? "EXITED"
                                : run.containsPlayer(owner) ? "ACTIVE" : "REMOVED", stats));
            }
            long now = Math.max(System.currentTimeMillis(), run.startedAtEpochMillis());
            UUID party = UUID.fromString(value(d, id, "party",
                    UUID.nameUUIDFromBytes(("party:" + store.worldId() + ":" + id).getBytes(StandardCharsets.UTF_8)).toString()));
            return new RunMetrics(store.worldId(), id, party, run.dungeonId(), run.difficulty().tier(),
                    run.startedAtEpochMillis(), now, outcome.equals("ACTIVE") ? 0 : now, outcome,
                    value(d, id, "coverage", "PARTIAL_LEGACY"), members);
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
    }
    public static void checkpoint(MinecraftServer server, DungeonRunRegistryData.RunRecord run) {
        try {
            var store = archive(server); var previous = store.read(run.runId());
            if (previous == null || !previous.finalized()) store.checkpoint(snapshot(server, run, "ACTIVE"));
        } catch (IOException | RuntimeException ex) { warn(server, ex); }
    }
    public static String outcome(MinecraftServer server, DungeonRunRegistryData.RunRecord run, DungeonResetReason reason) {
        return DungeonRunRegistryData.get(server).starting(run.runId()) ? "STARTUP_ABORTED"
                : reason == DungeonResetReason.COMPLETED ? "COMPLETED" : "ABANDONED";
    }
    /** False holds cleanup/reset and leaves the source counters available for retry. */
    public static boolean finish(MinecraftServer server, DungeonRunRegistryData.RunRecord run, String outcome) {
        try {
            var store = archive(server); var previous = store.read(run.runId());
            if (previous != null && previous.finalized()) return true;
            store.finish(snapshot(server, run, outcome)); return true;
        } catch (IOException | RuntimeException ex) { warn(server, ex); return false; }
    }
    /** Restore checkpoint floors before ordinary startup recovery; restart alone never ends a run. */
    public static void resume(MinecraftServer server) {
        for (var run : DungeonRunRegistryData.get(server).listAllRuns()) {
            try {
                var saved = archive(server).read(run.runId());
                if (saved != null && saved.finalized()) continue;
                var d = D1RunData.get(server);
                if (saved != null) {
                    d.setValue(run.runId(), KEY + "coverage", saved.coverage());
                    d.setValue(run.runId(), KEY + "party", saved.partyId().toString());
                    for (var m : saved.members()) {
                        d.recordUnique(run.runId(), KEY + "members", m.id().toString());
                        d.setValue(run.runId(), KEY + "name:" + m.id(), m.name());
                        d.setValue(run.runId(), KEY + "class:" + m.id(), m.classId());
                        floor(d, run.runId(), "kills:" + m.id(), m.stats().kills());
                        floor(d, run.runId(), KEY + "spawners:" + m.id(), m.stats().spawners());
                        floor(d, run.runId(), KEY + "resources:" + m.id(), m.stats().resourceEvents());
                        restoreMap(d, run.runId(), KEY + "mob:" + m.id() + ":", m.stats().mobs());
                        restoreMap(d, run.runId(), KEY + "used:" + m.id() + ":", m.stats().used());
                        restoreMap(d, run.runId(), KEY + "contributed:" + m.id() + ":", m.stats().contributed());
                        restoreValue(d, run.runId(), m.id(), "damage", m.stats().damage());
                        restoreValue(d, run.runId(), m.id(), "healing_received", m.stats().healing());
                        restoreValue(d, run.runId(), m.id(), "deaths", m.stats().deaths());
                    }
                }
                begin(server, run, false);
            } catch (IOException | RuntimeException ex) { warn(server, ex); }
        }
    }
    private static void floor(D1RunData d, long run, String key, long floor) {
        d.setCount(run, key, (int)Math.max(d.count(run, key), Math.min(Integer.MAX_VALUE, floor)));
    }
    private static void restoreMap(D1RunData d, long run, String prefix, Map<String, Long> map) {
        map.forEach((key, count) -> floor(d, run, prefix + key, count));
    }
    private static void restoreValue(D1RunData d, long run, UUID owner, String metric, double minimum) {
        double current = RunMemberStats.value(d, run, owner, metric);
        if (minimum > current) RunMemberStats.add(d, run, owner, metric, minimum - current);
    }
    public static void tick(MinecraftServer server) {
        if (server.overworld().getGameTime() % 100 != 0) return;
        var active = DungeonRunRegistryData.get(server).listAllRuns().stream()
                .filter(r -> r.stateEnum() == DungeonRunState.ACTIVE).toList();
        if (!active.isEmpty()) checkpoint(server, active.get(Math.floorMod(context(server).cursor++, active.size())));
    }
    public static void stopping(MinecraftServer server) {
        for (var run : DungeonRunRegistryData.get(server).listAllRuns())
            if (run.stateEnum() == DungeonRunState.ACTIVE) checkpoint(server, run);
    }
    public static void stopped(MinecraftServer server) { CONTEXTS.remove(server); }
    private static void warn(MinecraftServer server, Exception failure) {
        var c = context(server); long now = System.currentTimeMillis();
        if (now - c.lastWarning >= 60000) {
            c.lastWarning = now;
            com.mojang.logging.LogUtils.getLogger().error("Dungeon metrics write/recovery held; original files and cleanup source retained", failure);
        }
    }
}
