package net.goui.cosmicdungeon.npc.tamsin;

import io.netty.buffer.Unpooled;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.dungeon.DungeonStartupSchematicPlan;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Native config and lobby regressions for the server's explicit solo opt-in. */
final class SoloPartyMinimumTest {
    private final UUID leader = new UUID(0, 1);
    private final UUID member = new UUID(0, 2);
    private final D1PartyLobby.Anchor anchor =
            new D1PartyLobby.Anchor(new UUID(0, 900), "minecraft:overworld", 0);

    @Test void configAcceptsOneThroughSixAndPreservesDefault() {
        ModConfigSpec.ValueSpec spec = Config.SPEC.getSpec().get(List.of("TamsinVane", "minimumPartySize"));
        for (int minimum = 1; minimum <= 6; minimum++) assertTrue(spec.test(minimum));
        for (int minimum : new int[]{-1, 0, 7}) assertFalse(spec.test(minimum));
        assertEquals(3, spec.getDefault());
    }

    @Test void soloRequiresClassAndPersonalReadyBeforeQueueAndEntry() {
        var lobby = new D1PartyLobby();
        assertTrue(lobby.canStartSolo(leader, 1, 6));
        assertNull(lobby.party(leader), "Offering solo must not create a party");
        assertNull(lobby.startSolo(leader, anchor, 0, 1, 6));
        var party = lobby.party(leader);
        assertEquals(List.of(leader), party.members());
        assertEquals(D1PartyLobby.Phase.ASSEMBLY, party.phase());
        assertNotNull(lobby.begin(leader, 0, Map.of(leader, "bogatyr"), 1, 6));
        assertNotNull(lobby.begin(leader, party.revision(), Map.of(), 1, 6));
        assertNotNull(lobby.begin(leader, party.revision(), Map.of(leader, "none"), 1, 6));
        assertNull(lobby.begin(leader, party.revision(), Map.of(leader, "bogatyr"), 1, 6));
        assertNotNull(lobby.queue(leader, party.revision()));
        assertNull(lobby.ready(leader, party.revision()));
        assertNull(lobby.queue(leader, party.revision()));
        assertEquals(List.of(party), lobby.queued());
        lobby.startCountdown(party, 100, 100);
        assertFalse(lobby.prepare(party, 199));
        assertTrue(lobby.prepare(party, 200));
        assertEquals(D1PartyLobby.Phase.PREPARING, party.phase());
        lobby.complete(party);
        assertNull(lobby.party(leader));
        assertTrue(lobby.queued().isEmpty());
    }

    @Test void soloRejectsDisabledConfigurationStaleRequestsAndDuplicateMembership() {
        var lobby = new D1PartyLobby();
        for (int minimum : new int[]{0, 2, 3, 6}) {
            assertFalse(lobby.canStartSolo(leader, minimum, 6));
            assertNotNull(lobby.startSolo(leader, anchor, 0, minimum, 6));
        }
        assertNotNull(lobby.startSolo(leader, anchor, 1, 1, 6));
        assertNotNull(lobby.startSolo(leader, null, 0, 1, 6));
        assertNotNull(lobby.startSolo(leader, anchor, 0, 1, 0));
        assertNotNull(lobby.startSolo(leader, anchor, 0, 1, 7));
        assertNull(lobby.startSolo(leader, anchor, 0, 1, 1));
        var party = lobby.party(leader);
        assertNotNull(lobby.startSolo(leader, anchor, party.revision(), 1, 1));
        assertSame(party, lobby.party(leader));
        assertNotNull(lobby.begin(leader, party.revision(), Map.of(leader, "bogatyr"), 3, 6),
                "A raised live minimum must prevent solo readiness");
    }

    @Test void pendingInvitationAndOnePlayerCapacityRemainProtected() {
        var lobby = new D1PartyLobby();
        assertNotNull(lobby.invite(leader, member, anchor, 0, 120, 1));
        assertNull(lobby.invitation(member));
        assertNull(lobby.invite(member, leader, anchor, 0, 120, 6));
        var invitation = lobby.invitation(leader);
        assertFalse(lobby.canStartSolo(leader, 1, 6));
        assertNotNull(lobby.startSolo(leader, anchor, 0, 1, 6));
        assertSame(invitation, lobby.invitation(leader));
        assertTrue(lobby.decline(leader, invitation.token()));
        assertNull(lobby.startSolo(leader, anchor, 0, 1, 6));
        assertEquals(List.of(leader), lobby.remove(leader));
        assertNull(lobby.party(leader));
    }

    @Test void twoPlayerInvitationsHonorConfiguredMinimumAndSelectorCapacity() {
        var lobby = new D1PartyLobby();
        assertNull(lobby.invite(leader, member, anchor, 0, 120, 6));
        assertNull(lobby.accept(member, lobby.invitation(member).token(), 1));
        assertNull(lobby.joinAccepted(member, 2, 6, true));
        var party = lobby.party(leader);
        var classes = Map.of(leader, "bogatyr", member, "bogatyr");
        assertNotNull(lobby.begin(leader, party.revision(), classes, 3, 6));
        assertNotNull(lobby.begin(leader, party.revision(), classes, 1, 1));
        assertNull(lobby.begin(leader, party.revision(), classes, 2, 6));
    }

    @Test void soloPreservesSixSchematicSlotsAndExistingPacketShape() {
        var plan = DungeonStartupSchematicPlan.buildPlan(List.of("bogatyr"));
        assertEquals(36, plan.requests().size());
        assertEquals(5, plan.normalizedClassSlots().stream().filter("blankslot"::equals).count());
        var view = new PartyPayloads.View(7,
                new PartyPayloads.State(0, "SOLO_AVAILABLE", false, 6, 0, -1),
                List.of(), new PartyPayloads.Invite("", "", false, true));
        var buffer = Unpooled.buffer();
        try {
            PartyPayloads.View.STREAM_CODEC.encode(buffer, view);
            assertEquals(view, PartyPayloads.View.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test void existingPartyInvitationReadinessAndQueueRegressionsStillPass() {
        D1PartyChecks.main(new String[0]);
    }
}
