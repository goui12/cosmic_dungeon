package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.IOException;
import java.util.*;
import static net.goui.cosmicdungeon.dungeon.metrics.RunMetricsXml.*;

/** Strict independent v1 schema. Unknown/future data is retained and blocks projection. */
public final class ResourceMetricsStateXml {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private ResourceMetricsStateXml() {}
    public static byte[] encode(ResourceMetricsState state) throws IOException {
        return xml(w -> {
            start(w, "resource-state", "version", 1, "world", state.worldId(), "run", state.runId(),
                    "sequence", state.sequence(), "transaction", state.transaction() == null ? "" : state.transaction(), "cursor", state.cursor());
            for (var rank : new TreeMap<>(state.donated()).entrySet()) {
                start(w, "donated", "owner", rank.getKey(), "sequence", rank.getValue()); w.writeEndElement();
            }
            for (var member : new TreeMap<>(state.members()).entrySet()) {
                var m = member.getValue(); start(w, "member", "owner", member.getKey(), "events", m.events());
                for (String kind : List.of("own", "used", "contributed")) {
                    var counts = switch (kind) { case "own" -> m.own(); case "used" -> m.used(); default -> m.contributed(); };
                    for (var entry : new TreeMap<>(counts).entrySet()) {
                        start(w, kind, "item", entry.getKey(), "quantity", entry.getValue()); w.writeEndElement();
                    }
                }
                w.writeEndElement();
            }
            for (var e : state.recent()) {
                start(w, "event", "transaction", e.transaction(), "donor", e.donor(), "recipient", e.recipient(),
                        "skill", e.skill(), "item", e.item(), "quantity", e.quantity(), "own", e.own(), "at", e.at());
                w.writeEndElement();
            }
            w.writeEndElement();
        }, MAX_BYTES);
    }
    public static ResourceMetricsState decode(byte[] bytes) throws IOException {
        try {
            var root = parse(bytes, MAX_BYTES);
            shape(root, "resource-state", "version world run sequence transaction cursor", "donated member event"); version(root);
            var ranks = new TreeMap<UUID, Long>(); var members = new TreeMap<UUID, ResourceMetricsState.Member>();
            var events = new ArrayList<RunMetrics.ResourceEvent>();
            for (var e : children(root)) {
                switch (e.getTagName()) {
                    case "donated" -> {
                        shape(e, "donated", "owner sequence", "");
                        if (ranks.put(uuid(e, "owner"), n(e, "sequence")) != null) throw new IOException("Duplicate resource donor");
                    }
                    case "member" -> {
                        shape(e, "member", "owner events", "own used contributed");
                        var own = new TreeMap<String, Long>(); var used = new TreeMap<String, Long>(); var contributed = new TreeMap<String, Long>();
                        for (var counter : children(e)) {
                            shape(counter, counter.getTagName(), "item quantity", "");
                            var target = switch (counter.getTagName()) { case "own" -> own; case "used" -> used; default -> contributed; };
                            if (target.put(a(counter, "item"), n(counter, "quantity")) != null) throw new IOException("Duplicate resource counter");
                        }
                        if (members.put(uuid(e, "owner"), new ResourceMetricsState.Member(n(e, "events"), own, used, contributed)) != null)
                            throw new IOException("Duplicate resource member");
                    }
                    default -> {
                        shape(e, "event", "transaction donor recipient skill item quantity own at", "");
                        String own = a(e, "own");
                        if (!Set.of("true", "false").contains(own)) throw new IOException("Invalid resource own flag");
                        events.add(new RunMetrics.ResourceEvent(uuid(e, "transaction"), uuid(e, "donor"), uuid(e, "recipient"),
                                a(e, "skill"), a(e, "item"), n(e, "quantity"), Boolean.parseBoolean(own), n(e, "at")));
                    }
                }
            }
            String tx = root.getAttribute("transaction");
            return new ResourceMetricsState(uuid(root, "world"), n(root, "run"), n(root, "sequence"),
                    tx.isEmpty() ? null : UUID.fromString(tx), n(root, "cursor"), ranks, members, events);
        } catch (RuntimeException ex) { throw new IOException("Invalid resource state; original retained", ex); }
    }
}
