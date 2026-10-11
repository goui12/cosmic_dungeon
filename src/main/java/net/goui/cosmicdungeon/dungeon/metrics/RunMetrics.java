package net.goui.cosmicdungeon.dungeon.metrics;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Version-one, server-local analytics. Independent of rewards and Minecraft serialization. */
public record RunMetrics(UUID worldId, long runId, UUID partyId, String dungeon, String difficulty,
        long started, long updated, long ended, String outcome, String coverage,
        List<Member> members, List<ResourceEvent> resourceEvents) {
    public static final int MAX_KEYS = 512, MAX_EVENTS = 1024;
    public RunMetrics {
        Objects.requireNonNull(worldId); Objects.requireNonNull(partyId);
        text(dungeon, 128); text(difficulty, 32);
        if (runId <= 0 || started < 0 || updated < started || ended < 0) throw new IllegalArgumentException("run timing");
        if (!Set.of("ACTIVE", "COMPLETED", "ABANDONED", "STARTUP_ABORTED").contains(outcome)
                || (outcome.equals("ACTIVE") != (ended == 0)) || (ended > 0 && ended < started))
            throw new IllegalArgumentException("run outcome");
        if (!Set.of("COMPLETE", "PARTIAL_LEGACY").contains(coverage)) throw new IllegalArgumentException("coverage");
        members = List.copyOf(members); resourceEvents = List.copyOf(resourceEvents);
        if (members.size() > 6 || resourceEvents.size() > MAX_EVENTS
                || members.stream().map(Member::id).distinct().count() != members.size())
            throw new IllegalArgumentException("run bounds");
        var owners = new HashSet<UUID>(); members.forEach(m -> owners.add(m.id()));
        for (var event : resourceEvents)
            if (!owners.contains(event.donor()) || !owners.contains(event.recipient())
                    || event.own() != event.donor().equals(event.recipient()))
                throw new IllegalArgumentException("resource custody");
    }
    public RunMetrics(UUID w, long r, UUID p, String d, String f, long s, long u, long e, String o, String c, List<Member> m) {
        this(w, r, p, d, f, s, u, e, o, c, m, List.of());
    }
    public RunMetrics(UUID w, long r, String d, String f, long s, long u, long e, String o, String c, List<Member> m) {
        this(w, r, UUID.nameUUIDFromBytes(("party:" + w + ":" + r).getBytes(StandardCharsets.UTF_8)), d, f, s, u, e, o, c, m);
    }
    public String instanceId() { return instanceId(worldId, runId); }
    public static String instanceId(UUID world, long run) {
        return UUID.nameUUIDFromBytes((world + ":" + run).getBytes(StandardCharsets.UTF_8)).toString();
    }
    public boolean finalized() { return !outcome.equals("ACTIVE"); }
    public record Member(UUID id, String name, String classId, String status, Stats stats) {
        public Member {
            Objects.requireNonNull(id); Objects.requireNonNull(stats); text(name, 64); text(classId, 64);
            if (!Set.of("ACTIVE", "EXITED", "REMOVED").contains(status)) throw new IllegalArgumentException("member status");
        }
    }
    public record ResourceEvent(UUID transaction, UUID donor, UUID recipient, String skill,
            String item, long quantity, boolean own, long at) {
        public ResourceEvent {
            Objects.requireNonNull(transaction); Objects.requireNonNull(donor); Objects.requireNonNull(recipient);
            text(skill, 128); text(item, 128);
            if (quantity <= 0 || at < 0) throw new IllegalArgumentException("resource event");
        }
    }
    public record Stats(long kills, long deaths, double damage, double healing, long resourceEvents,
            Map<String, Long> mobs, Map<String, Long> used, Map<String, Long> contributed, long spawners) {
        public static final Stats EMPTY = new Stats(0, 0, 0, 0, 0, Map.of(), Map.of(), Map.of(), 0);
        public Stats {
            if (kills < 0 || deaths < 0 || !Double.isFinite(damage) || damage < 0
                    || !Double.isFinite(healing) || healing < 0 || resourceEvents < 0 || spawners < 0)
                throw new IllegalArgumentException("negative/nonfinite statistic");
            mobs = counts(mobs); used = counts(used); contributed = counts(contributed);
        }
        public Stats(long k, long d, double a, double h, long e, Map<String, Long> m, Map<String, Long> u, Map<String, Long> c) {
            this(k, d, a, h, e, m, u, c, 0);
        }
        public Stats plus(Stats b) {
            return new Stats(Math.addExact(kills, b.kills), Math.addExact(deaths, b.deaths), damage + b.damage,
                    healing + b.healing, Math.addExact(resourceEvents, b.resourceEvents), merge(mobs, b.mobs),
                    merge(used, b.used), merge(contributed, b.contributed), Math.addExact(spawners, b.spawners));
        }
    }
    public record History(String instanceId, long runId, String dungeon, String outcome, long ended) {
        public History {
            UUID.fromString(instanceId); text(dungeon, 128);
            if (runId <= 0 || ended <= 0 || !Set.of("COMPLETED", "ABANDONED", "STARTUP_ABORTED").contains(outcome))
                throw new IllegalArgumentException("history");
        }
    }
    public record Totals(UUID worldId, String owner, long runs, long completed, long aborted, Stats stats, List<History> recent) {
        public Totals {
            Objects.requireNonNull(worldId); Objects.requireNonNull(stats);
            if (!"server".equals(owner)) UUID.fromString(owner);
            recent = List.copyOf(recent);
            if (runs < 0 || completed < 0 || aborted < 0 || completed > runs || aborted != runs - completed
                    || recent.size() > runs || recent.size() > 10
                    || recent.stream().anyMatch(h -> !h.instanceId().equals(instanceId(worldId, h.runId())))
                    || recent.stream().map(History::instanceId).distinct().count() != recent.size())
                throw new IllegalArgumentException("totals");
        }
        public Totals add(RunMetrics run, Stats delta) {
            if (!worldId.equals(run.worldId()) || !run.finalized()) throw new IllegalArgumentException("wrong run");
            var history = new ArrayList<>(recent);
            history.removeIf(h -> h.instanceId().equals(run.instanceId()));
            history.add(new History(run.instanceId(), run.runId(), run.dungeon(), run.outcome(), run.ended()));
            history.sort(Comparator.comparingLong(History::ended).reversed().thenComparing(Comparator.comparingLong(History::runId).reversed()));
            return new Totals(worldId, owner, Math.addExact(runs, 1),
                    Math.addExact(completed, run.outcome().equals("COMPLETED") ? 1 : 0),
                    Math.addExact(aborted, run.outcome().equals("COMPLETED") ? 0 : 1), stats.plus(delta),
                    history.subList(0, Math.min(10, history.size())));
        }
    }
    private static Map<String, Long> counts(Map<String, Long> input) {
        if (input.size() > MAX_KEYS) throw new IllegalArgumentException("too many statistic keys");
        var copy = new TreeMap<String, Long>();
        input.forEach((k, v) -> { text(k, 128); if (v == null || v < 0) throw new IllegalArgumentException("count"); copy.put(k, v); });
        return Collections.unmodifiableMap(copy);
    }
    private static Map<String, Long> merge(Map<String, Long> a, Map<String, Long> b) {
        var result = new TreeMap<>(a); b.forEach((k, v) -> result.merge(k, v, Math::addExact)); return result;
    }
    private static void text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max
                || value.chars().anyMatch(c -> c < 32 || c == 65534 || c == 65535))
            throw new IllegalArgumentException("text field");
    }
}
