package net.goui.cosmicdungeon.npc.tamsin;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class GroupResourceConsentTest {
    private final D1PartyLobby lobby=new D1PartyLobby();
    private final UUID leader=UUID.randomUUID(),member=UUID.randomUUID();
    private final D1PartyLobby.Anchor anchor=new D1PartyLobby.Anchor(UUID.randomUUID(),"minecraft:overworld",0);
    private D1PartyLobby.Party create() {
        assertNull(lobby.create(leader,anchor,0,"Party"));return lobby.party(leader);
    }
    private void join() {
        assertNull(lobby.invite(leader,member,anchor,0,100,6));
        assertNull(lobby.accept(member,lobby.invitation(member).token(),1));
        assertNull(lobby.joinAccepted(member,2,6,true));
    }
    @Test void newMembershipAlwaysAsksAndNeverOptsIn() {
        var p=create();assertEquals("UNKNOWN",p.resourceSharing(leader));
        assertNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),true));
        join();assertEquals("UNKNOWN",p.resourceSharing(member));assertEquals("YES",p.resourceSharing(leader));
        assertNull(lobby.resourceSharing(member,p.revision(),p.id().toString(),true));
        lobby.remove(member);join();assertEquals("UNKNOWN",p.resourceSharing(member));
        String previous=p.id().toString();lobby.remove(leader);p=create();
        assertNotEquals(previous,p.id().toString());assertEquals("UNKNOWN",p.resourceSharing(leader));
    }
    @Test void staleRevisionWrongPartyAndOutsiderCannotChangeChoices() {
        var p=create();long old=p.revision();
        assertNull(lobby.resourceSharing(leader,old,p.id().toString(),true));
        join(); // A roster change, unlike an independent personal answer, invalidates old snapshots.
        assertNotNull(lobby.resourceSharing(leader,old,p.id().toString(),false));
        assertNotNull(lobby.resourceSharing(leader,p.revision(),UUID.randomUUID().toString(),false));
        assertNotNull(lobby.resourceSharing(UUID.randomUUID(),p.revision(),p.id().toString(),true));
        assertEquals("YES",p.resourceSharing(leader));
        assertNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),false));
        assertEquals("NO",p.resourceSharing(leader));
    }
    @Test void unknownBlocksProgressionAndConcurrentPersonalChoicesShareRosterRevision() {
        var p=create();join();var classes=Map.of(leader,"bogatyr",member,"theurgist");
        assertNotNull(lobby.begin(leader,p.revision(),classes));
        long revision=p.revision();
        assertNull(lobby.resourceSharing(leader,revision,p.id().toString(),false));
        assertNotNull(lobby.begin(leader,revision,classes));
        assertNull(lobby.resourceSharing(member,revision,p.id().toString(),true));
        assertEquals(revision,p.revision());
        assertEquals("NO",p.resourceSharing(leader));assertEquals("YES",p.resourceSharing(member));
        assertNull(lobby.begin(leader,revision,classes));
    }
    @Test void acceptedStartLocksChoicesBeforeCountdownAndPreparation() {
        var p=create();assertNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),false));
        assertNull(lobby.hire(leader,p.revision(),"theurgist"));assertEquals("NO",p.resourceSharing(leader));
        assertNull(lobby.begin(leader,p.revision(),Map.of(leader,"bogatyr")));
        assertNull(lobby.ready(leader,p.revision()));assertNull(lobby.queue(leader,p.revision()));
        assertEquals(Set.of(leader),p.ready()); // Mercenary never needs readiness or consent.
        assertNotNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),true));
        assertFalse(p.sharingMutable()); assertEquals("NO",p.resourceSharing(leader));
        assertEquals(Set.of(leader),p.ready());lobby.startCountdown(p,0,1);assertTrue(lobby.prepare(p,1));
        assertNotNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),true));
        assertEquals("NO",p.resourceSharing(leader));
    }
    @Test void soloAndDeferredInvitationUseSameNoDefaultChoice() {
        assertNull(lobby.startSolo(leader,anchor,0,6));var p=lobby.party(leader);
        assertEquals("UNKNOWN",p.resourceSharing(leader));
        assertNull(lobby.invite(leader,member,anchor,0,100,6));
        assertNull(lobby.accept(member,lobby.invitation(member).token(),1));
        assertNull(lobby.joinAccepted(member,2,6,false));assertNull(lobby.party(member));
        assertNull(lobby.joinAccepted(member,3,6,true));assertEquals("UNKNOWN",p.resourceSharing(member));
    }
    @Test void serializedStartRaceLocksLatestChoiceAndRejectsForgedCurrentRevision() {
        var p=create();join();
        assertNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),false));
        assertNull(lobby.resourceSharing(member,p.revision(),p.id().toString(),false));
        assertNull(lobby.begin(leader,p.revision(),Map.of(leader,"bogatyr",member,"theurgist")));
        assertNull(lobby.ready(leader,p.revision()));assertNull(lobby.ready(member,p.revision()));
        long before=p.revision();
        assertNull(lobby.resourceSharing(member,before,p.id().toString(),true));
        assertNull(lobby.queue(leader,before));
        for(long revision:new long[]{before,p.revision()})
            assertNotNull(lobby.resourceSharing(member,revision,p.id().toString(),false));
        assertEquals("YES",p.resourceSharing(member));assertEquals("NO",p.resourceSharing(leader));
        assertNull(lobby.unready(leader,p.revision())); // Start canceled, returning to pre-game.
        assertTrue(p.sharingMutable());assertNull(lobby.resourceSharing(member,p.revision(),p.id().toString(),false));
        assertEquals("NO",p.resourceSharing(member));
    }
    @Test void failedPreparationAndNewPregameGroupUnlockWithoutDefaultOptIn() {
        var p=create();assertNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),true));
        assertNull(lobby.begin(leader,p.revision(),Map.of(leader,"bogatyr")));
        assertNull(lobby.ready(leader,p.revision()));assertNull(lobby.queue(leader,p.revision()));
        lobby.startCountdown(p,0,0);assertTrue(lobby.prepare(p,0));lobby.cancel(p);
        assertTrue(p.sharingMutable());assertNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),false));
        lobby.complete(p);var next=create();
        assertEquals("UNKNOWN",next.resourceSharing(leader));assertTrue(next.sharingMutable());
        assertNotNull(lobby.resourceSharing(leader,p.revision(),p.id().toString(),true));
        assertNull(lobby.resourceSharing(leader,next.revision(),next.id().toString(),false));
    }
}
