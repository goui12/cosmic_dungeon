package net.goui.cosmicdungeon.dungeon.metrics;

import java.util.*;

/** One committed craft's immutable event segment; aggregate cursor is per dungeon run. */
public record ResourceMetricsBatch(UUID worldId, long runId, UUID transaction, long sequence,
        long donationBefore, long donationAfter, Map<UUID, Long> donated, List<RunMetrics.ResourceEvent> events) {
    public ResourceMetricsBatch {
        Objects.requireNonNull(worldId); Objects.requireNonNull(transaction);
        donated = Map.copyOf(donated); events = List.copyOf(events);
        if (runId <= 0 || sequence <= 0 || donationBefore < 0 || donationAfter < donationBefore
                || donationAfter - donationBefore > 64 || events.isEmpty() || events.size() > 64 || donated.size() > 5)
            throw new IllegalArgumentException("Resource segment bounds");
        var seen = new HashSet<String>(); var donors = new HashSet<UUID>(); var recipients = new HashSet<UUID>();
        long shared = 0, units = 0;
        for (var event : events) {
            if (!event.transaction().equals(transaction) || event.own() != event.donor().equals(event.recipient())
                    || !seen.add(event.donor() + ":" + event.item())) throw new IllegalArgumentException("Resource event identity");
            recipients.add(event.recipient()); units = Math.addExact(units, event.quantity());
            if (!event.own()) { shared = Math.addExact(shared, event.quantity()); donors.add(event.donor()); }
        }
        if (units > 64 || recipients.size() != 1 || shared != donationAfter - donationBefore || !donors.equals(donated.keySet())
                || donated.values().stream().anyMatch(v -> v <= donationBefore || v > donationAfter)
                || new HashSet<>(donated.values()).size() != donated.size()
                || (!donated.isEmpty() && Collections.max(donated.values()) != donationAfter))
            throw new IllegalArgumentException("Resource segment custody/cursor");
    }
}
