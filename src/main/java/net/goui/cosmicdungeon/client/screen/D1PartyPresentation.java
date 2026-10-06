package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.PartyPayloads;

/** Presentation of server-confirmed state; never changes readiness locally. */
final class D1PartyPresentation {
    private D1PartyPresentation() {}
    record ReadyControl(String label, String action, boolean enabled) {}

    static boolean showsReadiness(PartyPayloads.View view, PartyPayloads.Member member) {
        return !member.mercenary() && !view.state().phase().equals("ACTIVE");
    }
    static String readySuffix(PartyPayloads.View view, PartyPayloads.Member member) {
        return showsReadiness(view, member) ? member.ready() ? " / Ready" : " / Not Ready" : "";
    }
    static String readySummary(PartyPayloads.View view) {
        long humans = view.members().stream().filter(member -> !member.mercenary()).count();
        long ready = view.members().stream().filter(member -> !member.mercenary() && member.ready()).count();
        return view.state().phase().equals("ACTIVE") ? "Dungeon 1" : "Ready " + ready + "/" + humans;
    }
    static boolean allHumansReady(PartyPayloads.View view) {
        return view.members().stream().filter(member -> !member.mercenary()).allMatch(PartyPayloads.Member::ready);
    }
    static ReadyControl readiness(PartyPayloads.View view, String viewerName) {
        var member = view == null || viewerName == null ? null : view.members().stream()
                .filter(candidate -> !candidate.mercenary() && candidate.name().equals(viewerName)).findFirst().orElse(null);
        boolean ready = member != null && member.ready();
        String phase = view == null ? "" : view.state().phase();
        boolean enabled = member != null && (phase.equals("READY_CHECK") || (ready && phase.equals("QUEUED")));
        return new ReadyControl(ready ? "Not Ready" : "Ready", ready ? "unready" : "ready", enabled);
    }
}
