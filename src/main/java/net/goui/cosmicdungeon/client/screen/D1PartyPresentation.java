package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.PartyPayloads;

/** Presentation of server-confirmed state; never changes readiness locally. */
final class D1PartyPresentation {
    private D1PartyPresentation() {}
    record ReadyControl(String label, String action, boolean enabled) {}

    static boolean sharingMutable(PartyPayloads.View view) {
        return view != null && !view.sharing().context().isEmpty()
                && (view.state().phase().equals("ASSEMBLY") || view.state().phase().equals("READY_CHECK"));
    }
    static boolean needsConsent(PartyPayloads.View view) {
        return sharingMutable(view) && view.sharing().choice().equals("UNKNOWN");
    }
    static String sharingStatus(PartyPayloads.Member member) {
        if (member.mercenary()) return "Group resources: not applicable";
        return "Group resources: " + switch (member.resourceSharing()) {
            case "YES" -> "YES";
            case "NO" -> "NO";
            default -> "Not chosen (no contribution)";
        };
    }
    static net.minecraft.network.chat.Component identity(PartyPayloads.Member member) {
        return PartySkillPresentation.identity(member.name(),ClassSelectorScreen.className(member.classId()),
                member.vitals().skills(),member.leader());
    }
    static java.util.List<String> memberDetails(PartyPayloads.View view, PartyPayloads.Member member) {
        String status = view.state().phase().equals("ACTIVE") ? PartyHealthHud.status(member.vitals())
                : member.mercenary() ? "Mercenary" : member.ready() ? "Ready" : "Not Ready";
        return java.util.List.of(identity(member).getString(),
                status, sharingStatus(member));
    }
    static java.util.List<String> consentExplanation(String choice, boolean mutable) {
        return java.util.List.of("Share eligible ingredients for group crafting.",
                mutable ? "Choose before everyone is ready." : "Your choice is locked for this run.",
                "Sharing: " + (choice.equals("UNKNOWN") ? "Undecided" : choice));
    }

    static net.minecraft.client.gui.components.Button alwaysButton(boolean selected, int x, int y, int width,
            java.util.function.Consumer<String> send) {
        var button = net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal(
                (selected ? "☑ " : "☐ ") + "Always contribute"), pressed -> {
                    send.accept(selected ? "resources_ask" : "resources_always");
                }).bounds(x, y, width, 20).build();
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.literal(
                selected ? "Ask again for future groups." : "Choose Yes for this group and future groups.")));
        return button;
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
