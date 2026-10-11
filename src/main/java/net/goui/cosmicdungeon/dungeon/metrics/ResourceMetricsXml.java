package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.IOException;
import java.util.*;
import static net.goui.cosmicdungeon.dungeon.metrics.RunMetricsXml.*;

/** Independent v1 resource segments; existing run/totals schemas do not change. */
public final class ResourceMetricsXml {
    public static final int MAX_BYTES = 65536;
    private ResourceMetricsXml() {}
    public static byte[] encode(ResourceMetricsBatch batch) throws IOException {
        return xml(w -> {
            start(w, "resource-transaction", "version", 1, "world", batch.worldId(), "run", batch.runId(),
                    "transaction", batch.transaction(), "sequence", batch.sequence(),
                    "donation_before", batch.donationBefore(), "donation_after", batch.donationAfter());
            for (var rank : new TreeMap<>(batch.donated()).entrySet()) {
                start(w, "donated", "owner", rank.getKey(), "sequence", rank.getValue()); w.writeEndElement();
            }
            for (var e : batch.events()) {
                start(w, "event", "donor", e.donor(), "recipient", e.recipient(), "skill", e.skill(), "item", e.item(),
                        "quantity", e.quantity(), "own", e.own(), "at", e.at()); w.writeEndElement();
            }
            w.writeEndElement();
        }, MAX_BYTES);
    }
    public static ResourceMetricsBatch decode(byte[] bytes) throws IOException {
        try {
            var root = parse(bytes, MAX_BYTES);
            shape(root, "resource-transaction", "version world run transaction sequence donation_before donation_after", "donated event");
            version(root); var donated = new TreeMap<UUID, Long>(); var events = new ArrayList<RunMetrics.ResourceEvent>();
            UUID transaction = uuid(root, "transaction");
            for (var e : children(root)) {
                if (e.getTagName().equals("donated")) {
                    shape(e, "donated", "owner sequence", "");
                    if (donated.put(uuid(e, "owner"), n(e, "sequence")) != null) throw new IOException("Duplicate donor rank");
                } else {
                    shape(e, "event", "donor recipient skill item quantity own at", "");
                    String own = a(e, "own");
                    if (!Set.of("true", "false").contains(own)) throw new IOException("Invalid resource own flag");
                    events.add(new RunMetrics.ResourceEvent(transaction, uuid(e, "donor"), uuid(e, "recipient"), a(e, "skill"),
                            a(e, "item"), n(e, "quantity"), Boolean.parseBoolean(own), n(e, "at")));
                }
            }
            return new ResourceMetricsBatch(uuid(root, "world"), n(root, "run"), transaction, n(root, "sequence"),
                    n(root, "donation_before"), n(root, "donation_after"), donated, events);
        } catch (RuntimeException ex) { throw new IOException("Invalid resource event segment; original retained", ex); }
    }
}
