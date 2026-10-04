package net.goui.cosmicdungeon.npc.tamsin;

import io.netty.buffer.Unpooled;
import net.goui.cosmicdungeon.network.PartyPayloads;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class NamedPartyRecruitmentTest {
    private final UUID leader = UUID.randomUUID(), member = UUID.randomUUID(), target = UUID.randomUUID();
    private final D1PartyLobby.Anchor anchor = new D1PartyLobby.Anchor(UUID.randomUUID(), "minecraft:overworld", 0);
    private D1PartyLobby group() {
        var lobby = new D1PartyLobby();
        assertNull(lobby.create(leader, anchor, 0, "  Cave explorers  "));
        return lobby;
    }
    private void join(D1PartyLobby lobby, UUID sender, UUID recipient) {
        assertNull(lobby.invite(sender, recipient, anchor, 0, 100, 6));
        assertNull(lobby.accept(recipient, lobby.invitation(recipient).token(), 1));
        assertNull(lobby.joinAccepted(recipient, 2, 6, true));
    }
    @Test void namesAndCreationRejectMalformedOrStaleRequests() {
        var lobby = new D1PartyLobby();
        for (String name : new String[]{"", "  ", "x".repeat(33), "line\nbreak", "bad\u00a7a", "hidden\u200B"})
            assertNotNull(lobby.create(leader, anchor, 0, name));
        assertNull(lobby.party(leader));
        assertNotNull(lobby.create(leader, anchor, 10, "Valid"));
        assertNull(lobby.create(leader, anchor, 0, "  Cave explorers  "));
        assertEquals("Cave explorers", lobby.party(leader).name());
        assertNotNull(lobby.create(leader, anchor, lobby.revision(leader), "Duplicate"));
        assertEquals("Cave explorers", lobby.party(leader).name());
    }
    @Test void anyMemberCanInviteANonAdvertisingFriendButUngroupedCannot() {
        var lobby = group();
        assertNotNull(lobby.invite(member, target, anchor, 0, 100, 6));
        join(lobby, leader, member);
        assertFalse(lobby.looking(target));
        join(lobby, member, target);
        assertSame(lobby.party(leader), lobby.party(target));
        assertEquals(leader, lobby.party(target).leader());
        assertEquals(List.of(leader, member, target), lobby.party(leader).members());
    }
    @Test void lfgClearsOnCreationJoinDepartureAndServerStop() {
        var lobby = new D1PartyLobby();
        assertNull(lobby.advertise(leader, anchor, true));
        assertEquals(Set.of(leader), lobby.lookingAt(anchor));
        assertNull(lobby.create(leader, anchor, 0, "Group"));
        assertFalse(lobby.looking(leader));
        assertNotNull(lobby.advertise(leader, anchor, true));
        assertNull(lobby.advertise(member, anchor, true));
        join(lobby, leader, member);
        assertFalse(lobby.looking(member));
        lobby.remove(member);
        assertNull(lobby.advertise(member, anchor, true));
        lobby.remove(member);
        assertFalse(lobby.looking(member));
        lobby.advertise(target, anchor, true);
        lobby.clear();
        assertTrue(lobby.lookingAt(anchor).isEmpty());
    }
    @Test void memberDepartureAndLeaderDisbandCancelAllAffectedInvitations() {
        var lobby = group();
        join(lobby, leader, member);
        assertNull(lobby.invite(member, target, anchor, 0, 100, 6));
        var old = lobby.invitation(target);
        lobby.remove(member);
        assertNull(lobby.invitation(target));
        join(lobby, leader, member);
        assertNull(lobby.invite(member, target, anchor, 0, 100, 6));
        lobby.remove(leader);
        assertNull(lobby.invitation(target));
        assertNull(lobby.party(member));
        assertNull(lobby.create(member, anchor, 0, "New group"));
        assertNotNull(lobby.accept(target, old.token(), 1));
        assertNull(lobby.party(target));
    }
    @Test void invitationDoesNotReserveCapacityOrBypassPreparation() {
        var lobby = group();
        join(lobby, leader, member);
        assertNull(lobby.invite(member, target, anchor, 0, 100, 6));
        assertNull(lobby.accept(target, lobby.invitation(target).token(), 1));
        assertNotNull(lobby.joinAccepted(target, 2, 2, true));
        assertNull(lobby.party(target));
        assertNull(lobby.invite(member, target, anchor, 3, 100, 6));
        assertNull(lobby.accept(target, lobby.invitation(target).token(), 4));
        var party = lobby.party(leader);
        assertNull(lobby.begin(leader, party.revision(), Map.of(leader, "bogatyr", member, "pyroclast")));
        assertNull(lobby.ready(leader, party.revision()));
        assertNull(lobby.ready(member, party.revision()));
        assertNull(lobby.queue(leader, party.revision()));
        lobby.startCountdown(party, 10, 1);
        assertTrue(lobby.prepare(party, 11));
        assertNotNull(lobby.joinAccepted(target, 12, 6, true));
        assertTrue(lobby.remove(member).isEmpty());
        assertEquals(2, party.members().size());
    }
    @Test void recruitmentWireRoundTripAndBounds() {
        var memberView = new PartyPayloads.Member("Cameron", "bogatyr", false, true);
        var recruitment = new PartyPayloads.Recruitment("Cave explorers", false, 1, 3, List.of(memberView));
        var view = new PartyPayloads.View(9, new PartyPayloads.State(23, "ASSEMBLY", true, 6, 0, -1),
                List.of(memberView), new PartyPayloads.Invite("", "", false, true), recruitment);
        var buffer = Unpooled.buffer();
        try {
            PartyPayloads.View.STREAM_CODEC.encode(buffer, view);
            assertEquals(view, PartyPayloads.View.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
        var oversized = new PartyPayloads.Recruitment("Group", false, 0, 1, Collections.nCopies(5, memberView));
        var rejected = Unpooled.buffer();
        try { assertThrows(RuntimeException.class, () -> PartyPayloads.Recruitment.CODEC.encode(rejected, oversized)); }
        finally { rejected.release(); }
    }
}
