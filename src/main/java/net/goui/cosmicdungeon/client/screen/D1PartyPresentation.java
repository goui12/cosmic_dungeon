package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.PartyPayloads;

/** Presentation of server-confirmed state; never changes readiness locally. */
final class D1PartyPresentation {
    private D1PartyPresentation() {}
    record ReadyControl(String label, String action, boolean enabled) {}

    static boolean needsConsent(PartyPayloads.View view) {
        return view != null && !view.sharing().context().isEmpty() && view.sharing().choice().equals("UNKNOWN");
    }
    static ReadyControl forward(PartyPayloads.View view) {
        boolean assembly = view.state().phase().equals("ASSEMBLY");
        return new ReadyControl(assembly ? "Begin ready check" : "Start Adventure!", assembly ? "begin" : "queue",
                view.state().leader() && (assembly || view.state().phase().equals("READY_CHECK") && allHumansReady(view)));
    }
    static String sharingStatus(PartyPayloads.Member member) {
        if (member.mercenary()) return "Group resources: not applicable";
        return "Group resources: " + switch (member.resourceSharing()) {
            case "YES" -> "YES";
            case "NO" -> "NO";
            default -> "Not chosen (no contribution)";
        };
    }
    static java.util.List<String> memberDetails(PartyPayloads.View view, PartyPayloads.Member member) {
        String status = view.state().phase().equals("ACTIVE") ? PartyHealthHud.status(member.vitals())
                : member.mercenary() ? "Mercenary" : member.ready() ? "Ready" : "Not Ready";
        return java.util.List.of(member.name() + " / " + ClassSelectorScreen.className(member.classId()).getString(),
                status, sharingStatus(member));
    }
    static java.util.List<String> consentExplanation(String choice) {
        return java.util.List.of("May your group use ingredients from your inventory, sharing contributions as evenly as possible?",
                "Only an explicit YES enables sharing. You can change this in Resources.",
                "Your choice: " + (choice.equals("UNKNOWN") ? "not chosen; no contribution" : choice));
    }

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
