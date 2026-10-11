package net.goui.cosmicdungeon.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.network.*;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class D1PartyPanelTest {
    private static final String CONTEXT = "12345678-1234-1234-1234-123456789abc";
    private static PartyPayloads.View view(String phase, String choice, boolean leader, boolean selfReady) {
        var members = new ArrayList<PartyPayloads.Member>();
        for (int i = 0; i < 6; i++)
            members.add(new PartyPayloads.Member(i == 0 ? "Cameron" : "Friend" + i, "theurgist",
                    i == 0 ? selfReady : true, i == 0, false, PartyVitals.UNLOADED,
                    new UUID(0, i + 1).toString(), i == 0 ? choice : "NO"));
        return new PartyPayloads.View(1, new PartyPayloads.State(7, phase, leader, 6, 1, -1), members,
                new PartyPayloads.Invite("", "", false, true),
                new PartyPayloads.Recruitment("My group", false, 1, 3, members.subList(0, 4)),
                "HARD", PartyPayloads.Hire.NONE, List.of(), new PartyPayloads.Sharing(CONTEXT, choice));
    }
    private static List<AbstractWidget> build(D1PartyPanel panel, int width, int height) {
        var widgets = new ArrayList<AbstractWidget>();
        var box = D1PartyPanelLayout.forViewport(width, height);
        panel.build(null, widgets::add, () -> {}, (width - box.width()) / 2, (height - box.height()) / 2, 1, box, "Cameron");
        return widgets;
    }
    private static Button button(List<AbstractWidget> widgets, String label) {
        return (Button) widgets.stream().filter(w -> w.getMessage().getString().equals(label)).findFirst().orElseThrow();
    }
    private static void bounded(List<AbstractWidget> widgets, int width, int height) {
        for (var a : widgets) {
            assertTrue(a.getX() >= 0 && a.getY() >= 0 && a.getX() + a.getWidth() <= width
                    && a.getY() + a.getHeight() <= height, a.getMessage().getString());
            for (var b : widgets) if (a != b)
                assertFalse(a.getX() < b.getX() + b.getWidth() && a.getX() + a.getWidth() > b.getX()
                        && a.getY() < b.getY() + b.getHeight() && a.getY() + a.getHeight() > b.getY(),
                        a.getMessage().getString() + " overlaps " + b.getMessage().getString());
        }
    }
    @Test void nativeWidgetsKeepForwardControlsOnEveryGroupPageAtSupportedGuiScales() {
        for (int[] viewport : new int[][]{{320,240},{426,240},{640,360},{960,600},{1920,1080}}) {
            for (String destination : List.of("Recruit", "Mercenary", "Resources")) {
                var panel = new D1PartyPanel(); panel.setView(view("READY_CHECK", "YES", true, true));
                var widgets = build(panel, viewport[0], viewport[1]); bounded(widgets, viewport[0], viewport[1]);
                button(widgets, destination).onPress(null);
                widgets = build(panel, viewport[0], viewport[1]); bounded(widgets, viewport[0], viewport[1]);
                assertTrue(button(widgets, "Start Adventure!").active, destination);
                assertTrue(button(widgets, "Not Ready").active, destination);
            }
        }
    }
    @Test void initialChoiceHasTwoExplicitOptionsAndDoesNotEnableProgression() {
        var panel = new D1PartyPanel(); panel.setView(view("ASSEMBLY", "UNKNOWN", true, false));
        var widgets = build(panel, 320, 240); bounded(widgets, 320, 240);
        assertTrue(button(widgets, "YES - contribute").active);
        assertTrue(button(widgets, "NO - keep materials").active);
        assertFalse(button(widgets, "Begin ready check").active);
        assertFalse(button(widgets, "Ready").active);
        assertTrue(D1PartyPresentation.needsConsent(view("ASSEMBLY", "UNKNOWN", true, false)));
        assertFalse(D1PartyPresentation.needsConsent(view("ASSEMBLY", "NO", true, false)));
    }
    @Test void beginAndStartRemainLeaderOnlyWhileMercenariesNeverBlockHumanReady() {
        for (String phase : List.of("ASSEMBLY", "READY_CHECK")) {
            assertFalse(D1PartyPresentation.forward(view(phase, "YES", false, true)).enabled());
            assertTrue(D1PartyPresentation.forward(view(phase, "YES", true, true)).enabled());
        }
        assertFalse(D1PartyPresentation.forward(view("READY_CHECK", "YES", true, false)).enabled());
        assertFalse(D1PartyPresentation.forward(view("PREPARING", "YES", true, true)).enabled());
        var original = view("READY_CHECK", "NO", true, true);
        var mercenary = new PartyPayloads.Member("Hire", "theurgist", false, false, true);
        var mixed = new PartyPayloads.View(1, original.state(), List.of(original.members().getFirst(), mercenary), original.invitation());
        assertTrue(D1PartyPresentation.allHumansReady(mixed));
        assertEquals("", D1PartyPresentation.readySuffix(mixed, mercenary));
    }
    @Test void hoverReflectsEachMembersAuthoritativeChoiceAndUnknownNeverMeansYes() {
        for (String choice : List.of("UNKNOWN", "YES", "NO")) {
            var snapshot = view("ACTIVE", choice, true, true);
            var self = snapshot.members().getFirst();
            var teammate = snapshot.members().get(1);
            assertEquals("Group resources: " + (choice.equals("UNKNOWN") ? "Not chosen (no contribution)" : choice),
                    D1PartyPresentation.sharingStatus(self));
            assertEquals("Group resources: NO", D1PartyPresentation.sharingStatus(teammate));
            assertTrue(D1PartyPresentation.memberDetails(snapshot, self).contains(D1PartyPresentation.sharingStatus(self)));
            assertTrue(D1PartyPresentation.memberDetails(snapshot, self).stream().noneMatch(s -> s.contains("Ready")));
        }
    }
    @Test void lobbyHoverShowsReadinessInsteadOfUnloadedVitals() {
        var snapshot = view("READY_CHECK", "YES", true, true);
        var details = D1PartyPresentation.memberDetails(snapshot, snapshot.members().getFirst());
        assertTrue(details.contains("Ready"));
        assertTrue(details.stream().noneMatch(s -> s.contains("Unloaded")));
        var hire = new PartyPayloads.Member("Hire", "theurgist", false, false, true);
        assertTrue(D1PartyPresentation.memberDetails(snapshot, hire).contains("Mercenary"));
    }
    @Test void serverAnswerReturnsFromInitialConsentWithoutBack() {
        var panel = new D1PartyPanel(); panel.setView(view("ASSEMBLY", "UNKNOWN", true, false));
        assertTrue(button(build(panel, 320, 240), "NO - keep materials").active);
        panel.setView(view("ASSEMBLY", "NO", true, false));
        assertTrue(button(build(panel, 320, 240), "Begin ready check").active);
    }
}
