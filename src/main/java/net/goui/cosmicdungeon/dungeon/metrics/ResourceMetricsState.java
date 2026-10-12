package net.goui.cosmicdungeon.dungeon.metrics;

import java.util.*;

/** Exact resource afterimage; sequence and counters advance together before SavedData projection. */
public record ResourceMetricsState(UUID worldId, long runId, long sequence, UUID transaction,
        long cursor, Map<UUID, Long> donated, Map<UUID, Member> members, List<RunMetrics.ResourceEvent> recent) {
    public record Member(long events, Map<String, Long> own, Map<String, Long> used, Map<String, Long> contributed) {
        public static final Member EMPTY = new Member(0, Map.of(), Map.of(), Map.of());
        public Member {
            if (events < 0 || events > Integer.MAX_VALUE) throw new IllegalArgumentException("Resource event count");
            own = counts(own); used = counts(used); contributed = counts(contributed);
        }
        private static Map<String, Long> counts(Map<String, Long> input) {
            if (input.size() > RunMetrics.MAX_KEYS) throw new IllegalArgumentException("Resource key bounds");
            var result = new TreeMap<String, Long>();
            input.forEach((key, count) -> {
                if (key == null || !key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || key.length() > 128
                        || count == null || count < 0 || count > Integer.MAX_VALUE)
                    throw new IllegalArgumentException("Resource counter");
                result.put(key, count);
            });
            return Collections.unmodifiableMap(result);
        }
    }
    public ResourceMetricsState {
        Objects.requireNonNull(worldId);
        donated = Map.copyOf(donated); members = Map.copyOf(members); recent = List.copyOf(recent);
        if (runId <= 0 || sequence < 0 || cursor < 0 || (sequence == 0) != (transaction == null)
                || members.size() > 6 || donated.size() > 6 || recent.size() > 64
                || !members.keySet().containsAll(donated.keySet())
                || donated.values().stream().anyMatch(rank -> rank <= 0 || rank > cursor)
                || donated.values().stream().distinct().count() != donated.size()
                || (cursor == 0) != donated.isEmpty()
                || (!donated.isEmpty() && Collections.max(donated.values()) != cursor)
                || (sequence == 0 && (!members.isEmpty() || !recent.isEmpty() || cursor != 0)))
            throw new IllegalArgumentException("Resource state bounds");
        for (var event : recent)
            if (!members.containsKey(event.donor()) || !members.containsKey(event.recipient())
                    || event.own() != event.donor().equals(event.recipient()))
                throw new IllegalArgumentException("Resource state custody");
    }
    public static ResourceMetricsState empty(UUID world, long run) {
        return new ResourceMetricsState(world, run, 0, null, 0, Map.of(), Map.of(), List.of());
    }
    public ResourceMetricsState apply(ResourceMetricsBatch batch) {
        if (!worldId.equals(batch.worldId()) || runId != batch.runId())
            throw new IllegalArgumentException("Resource state identity");
        if (sequence == batch.sequence() && Objects.equals(transaction, batch.transaction())) return this;
        if (batch.sequence() != Math.addExact(sequence, 1) || batch.donationBefore() != cursor)
            throw new IllegalArgumentException("Resource transaction sequence differs");
        var next = new TreeMap<>(members); var ranks = new TreeMap<>(donated);
        for (var event : batch.events()) {
            var recipient = next.getOrDefault(event.recipient(), Member.EMPTY);
            next.put(event.recipient(), new Member(Math.addExact(recipient.events(), 1),
                    event.own() ? add(recipient.own(), event) : recipient.own(),
                    event.own() ? recipient.used() : add(recipient.used(), event), recipient.contributed()));
            if (!event.own()) {
                var donor = next.getOrDefault(event.donor(), Member.EMPTY);
                next.put(event.donor(), new Member(donor.events(), donor.own(), donor.used(), add(donor.contributed(), event)));
            }
        }
        ranks.putAll(batch.donated());
        var history = new ArrayList<>(recent); history.addAll(batch.events());
        return new ResourceMetricsState(worldId, runId, batch.sequence(), batch.transaction(), batch.donationAfter(),
                ranks, next, history.subList(Math.max(0, history.size() - 64), history.size()));
    }
    private static Map<String, Long> add(Map<String, Long> counts, RunMetrics.ResourceEvent event) {
        var next = new TreeMap<>(counts); next.merge(event.item(), event.quantity(), Math::addExact); return next;
    }
}
