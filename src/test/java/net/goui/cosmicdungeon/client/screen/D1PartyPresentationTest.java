package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import net.goui.cosmicdungeon.network.PartyPayloads;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class D1PartyPresentationTest {
    private PartyPayloads.View view(String phase, boolean selfReady, boolean teammateReady) {
        return new PartyPayloads.View(1, new PartyPayloads.State(10, phase, true, 6, 1, 5),
                List.of(new PartyPayloads.Member("Cameron", "pyroclast", selfReady, true),
                        new PartyPayloads.Member("Dad", "bogatyr", teammateReady, false)),
                new PartyPayloads.Invite("", "", false, false));
    }

    @Test void toggleFollowsTheViewerRatherThanTheTeammate() {
        var control = D1PartyPresentation.readiness(view("READY_CHECK", false, true), "Cameron");
        assertEquals(new D1PartyPresentation.ReadyControl("Ready", "ready", true), control);
        control = D1PartyPresentation.readiness(view("READY_CHECK", true, false), "Cameron");
        assertEquals(new D1PartyPresentation.ReadyControl("Not Ready", "unready", true), control);
    }

    @Test void queuedPlayerCanWithdrawButPreparingPlayerCannot() {
        assertEquals(new D1PartyPresentation.ReadyControl("Not Ready", "unready", true),
                D1PartyPresentation.readiness(view("QUEUED", true, true), "Cameron"));
        assertFalse(D1PartyPresentation.readiness(view("PREPARING", true, true), "Cameron").enabled());
        assertFalse(D1PartyPresentation.readiness(view("QUEUED", false, true), "Cameron").enabled());
    }

    @Test void setupAndUnknownViewerCannotSendPrematureReady() {
        for (String phase : List.of("SOLO_AVAILABLE", "UNGROUPED", "ASSEMBLY"))
            assertFalse(D1PartyPresentation.readiness(view(phase, false, false), "Cameron").enabled());
        assertFalse(D1PartyPresentation.readiness(view("READY_CHECK", true, true), "SomeoneElse").enabled());
        assertFalse(D1PartyPresentation.readiness(null, null).enabled());
    }

    @Test void mercenariesNeverParticipateInReadyPresentationOrHumanLookup() {
        var human = new PartyPayloads.Member("Cameron", "pyroclast", true, true);
        var hire = new PartyPayloads.Member("Cameron", "theurgist", false, false, true);
        var original = view("READY_CHECK", true, true);
        var mixed = new PartyPayloads.View(1, original.state(), List.of(hire, human), original.invitation());
        assertEquals("", D1PartyPresentation.readySuffix(mixed, hire));
        assertEquals(" / Ready", D1PartyPresentation.readySuffix(mixed, human));
        assertEquals("Ready 1/1", D1PartyPresentation.readySummary(mixed));
        assertTrue(D1PartyPresentation.allHumansReady(mixed));
        assertEquals("unready", D1PartyPresentation.readiness(mixed, "Cameron").action());
    }

    @Test void activeDungeonHidesReadinessEvenForOfflineOrUnreadyRows() {
        var active = view("ACTIVE", true, false);
        active.members().forEach(member -> assertEquals("", D1PartyPresentation.readySuffix(active, member)));
        assertEquals("Dungeon 1", D1PartyPresentation.readySummary(active));
        assertFalse(D1PartyPresentation.readiness(active, "Cameron").enabled());
    }

    @Test void mapIsSquareAndFitsNormalGuiScalesWithControlsOutsideIt() {
        for (int[] viewport : new int[][]{{320, 240}, {426, 240}, {640, 360}, {960, 600}, {1920, 1080}}) {
            var map = TamsinMapLayout.forViewport(viewport[0], viewport[1]);
            assertTrue(map.width() <= viewport[0] - 16);
            assertTrue(map.height() <= viewport[1] - 16);
            assertTrue(map.mapEdge() <= TamsinMapLayout.TEXTURE_SIZE);
            assertTrue(map.mapEdge() + 36 <= map.width());
            assertTrue(44 + map.mapEdge() + 7 + 9 < map.height() - 27);
        }
        assertEquals(516, TamsinMapLayout.forViewport(1920, 1080).mapEdge());
    }
}
