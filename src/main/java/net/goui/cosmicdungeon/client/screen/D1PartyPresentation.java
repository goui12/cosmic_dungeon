package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.PartyPayloads;

/** Presentation of server-confirmed state; never changes readiness locally. */
final class D1PartyPresentation {
    private D1PartyPresentation() {}
    record ReadyControl(String label, String action, boolean enabled) {}

    static ReadyControl readiness(PartyPayloads.View view, String viewerName) {
        var member = view == null || viewerName == null ? null : view.members().stream()
                .filter(candidate -> candidate.name().equals(viewerName)).findFirst().orElse(null);
        boolean ready = member != null && member.ready();
        String phase = view == null ? "" : view.state().phase();
        boolean enabled = member != null && (phase.equals("READY_CHECK") || (ready && phase.equals("QUEUED")));
        return new ReadyControl(ready ? "not ready" : "Ready", ready ? "unready" : "ready", enabled);
    }
}
