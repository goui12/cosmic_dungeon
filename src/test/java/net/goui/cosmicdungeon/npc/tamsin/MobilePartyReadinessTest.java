package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MobilePartyReadinessTest {
    private final UUID leader = UUID.randomUUID(), member = UUID.randomUUID(), target = UUID.randomUUID();
    private final D1PartyLobby.Anchor anchor = new D1PartyLobby.Anchor(UUID.randomUUID(), "minecraft:overworld", 0);
    private D1PartyLobby pair() {
        var lobby = new D1PartyLobby();
        assertNull(lobby.create(leader, anchor, 0, "Roamers", 6));
        assertNull(lobby.invite(leader, member, anchor, 0, 100, 6));
        assertNull(lobby.accept(member, lobby.invitation(member).token(), 1));
        assertNull(lobby.joinAccepted(member, 2, 6, true));
        return lobby;
    }
    private void readyBoth(D1PartyLobby lobby) {
        assertNull(lobby.begin(leader, lobby.revision(leader), Map.of(leader, "bogatyr", member, "pyroclast")));
        long revision = lobby.revision(leader);
        assertNull(lobby.ready(leader, revision));
        assertNull(lobby.ready(member, revision));
    }
    @Test void capacityIsCeilingForEveryAllowedRosterSize() {
        for (int maximum = 1; maximum <= 6; maximum++) {
            for (int count = 1; count <= maximum; count++) assertTrue(D1PartyRules.fits(count, maximum));
            assertFalse(D1PartyRules.fits(maximum + 1, maximum));
        }
        assertFalse(D1PartyRules.fits(0, 6));
        assertFalse(D1PartyRules.fits(1, 0));
        assertFalse(D1PartyRules.fits(1, 7));
    }
    @Test void onlyCurrentLeaderCanChangeCapacityWithoutRemovingMembers() {
        var lobby = pair(); var p = lobby.party(leader);
        long revision = p.revision();
        assertNotNull(lobby.capacity(member, revision, 3));
        assertNotNull(lobby.capacity(leader, revision - 1, 3));
        assertNotNull(lobby.capacity(leader, revision, 1));
        assertNotNull(lobby.capacity(leader, revision, 7));
        assertNull(lobby.capacity(leader, revision, 2));
        assertEquals(2, p.capacity());
        assertEquals(List.of(leader, member), p.members());
        assertNotNull(lobby.invite(member, target, anchor, 0, 100, 6));
    }
    @Test void changingMaximumResetsReadyAndRechecksPendingInvitationCapacity() {
        var lobby = pair(); var p = lobby.party(leader);
        assertNull(lobby.invite(member, target, anchor, 0, 100, 6));
        assertNull(lobby.accept(target, lobby.invitation(target).token(), 1));
        readyBoth(lobby);
        long old = p.revision();
        assertNull(lobby.capacity(leader, old, 2));
        assertEquals(D1PartyLobby.Phase.ASSEMBLY, p.phase());
        assertTrue(p.ready().isEmpty());
        assertNotNull(lobby.ready(member, old));
        assertNotNull(lobby.joinAccepted(target, 2, 6, true));
        assertNull(lobby.party(target));
    }
    @Test void personalUnreadyKeepsTeammatesAndRejectsStaleReadyReplay() {
        var lobby = pair(); readyBoth(lobby); var p = lobby.party(leader);
        long old = p.revision();
        assertNull(lobby.unready(member, old));
        assertEquals(Set.of(leader), p.ready());
        assertEquals(D1PartyLobby.Phase.READY_CHECK, p.phase());
        assertNotNull(lobby.ready(member, old));
        assertNull(lobby.ready(member, p.revision()));
        assertEquals(Set.of(leader, member), p.ready());
    }
    @Test void unreadyCancelsCountdownAndRequiresAnExplicitNewLeaderStart() {
        var lobby = pair(); readyBoth(lobby); var p = lobby.party(leader);
        assertNull(lobby.queue(leader, p.revision()));
        lobby.startCountdown(p, 100, 20);
        assertNotNull(lobby.capacity(leader, p.revision(), 3));
        assertNull(lobby.unready(member, p.revision()));
        assertEquals(-1, p.countdownEnd());
        assertTrue(lobby.queued().isEmpty());
        assertEquals(Set.of(leader), p.ready());
        assertNull(lobby.ready(member, p.revision()));
        assertEquals(D1PartyLobby.Phase.READY_CHECK, p.phase());
        assertFalse(lobby.prepare(p, 1000));
        assertNotNull(lobby.queue(member, p.revision()));
        assertNull(lobby.queue(leader, p.revision()));
    }
    @Test void preparationAndRosterChangesInvalidateControls() {
        var lobby = pair(); readyBoth(lobby); var p = lobby.party(leader);
        assertNull(lobby.queue(leader, p.revision()));
        lobby.startCountdown(p, 0, 1);
        assertTrue(lobby.prepare(p, 1));
        assertNotNull(lobby.unready(member, p.revision()));
        assertNotNull(lobby.capacity(leader, p.revision(), 3));
        assertTrue(lobby.remove(member).isEmpty());
        lobby.complete(p);
        assertNotNull(lobby.ready(member, p.revision()));
        assertNotNull(lobby.queue(leader, p.revision()));
    }
    @Test void mobileReadinessRetainsStartingDimensionBoundary() {
        assertTrue(D1PartyRules.sameStartingDimension("minecraft:overworld", "minecraft:overworld"));
        assertFalse(D1PartyRules.sameStartingDimension("minecraft:the_nether", "minecraft:overworld"));
        assertFalse(D1PartyRules.sameStartingDimension("", ""));
        assertFalse(D1PartyRules.sameStartingDimension(null, "minecraft:overworld"));
    }

    @Test void leaderReadyKeepsMenuAndStillRequiresAnExplicitAuthorizedStart() {
        var lobby = pair(); var party = lobby.party(leader);
        assertNull(lobby.begin(leader, party.revision(), Map.of(leader, "bogatyr", member, "theurgist")));
        String error = lobby.ready(leader, party.revision());
        assertNull(error);
        assertFalse(D1PartyRules.closeAfterReady(party, leader, "ready", error));
        assertEquals(D1PartyLobby.Phase.READY_CHECK, party.phase());
        assertNotNull(lobby.queue(leader, party.revision()), "Other members still need to confirm");
        error = lobby.ready(member, party.revision());
        assertTrue(D1PartyRules.closeAfterReady(party, member, "ready", error));
        assertNotNull(lobby.queue(member, party.revision()), "Keeping the menu open grants no authority");
        assertNull(lobby.queue(leader, party.revision()));
    }
    @Test void rejectedOrDifferentActionsNeverDismissTheMenu() {
        var lobby = pair(); var party = lobby.party(leader);
        assertFalse(D1PartyRules.closeAfterReady(party, member, "ready", lobby.ready(member, party.revision())));
        readyBoth(lobby); long old = party.revision();
        assertNull(lobby.unready(member, old));
        assertFalse(D1PartyRules.closeAfterReady(party, member, "ready", lobby.ready(member, old)));
        assertFalse(D1PartyRules.closeAfterReady(party, member, "unready", null));
        assertFalse(D1PartyRules.closeAfterReady(party, leader, "queue", null));
        assertFalse(D1PartyRules.closeAfterReady(party, target, "ready", null));
        assertFalse(D1PartyRules.closeAfterReady(null, member, "ready", null));
    }

}
