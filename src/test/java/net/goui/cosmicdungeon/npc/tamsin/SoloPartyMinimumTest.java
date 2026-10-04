package net.goui.cosmicdungeon.npc.tamsin;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.dungeon.DungeonStartupSchematicPlan;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Legacy config remains readable; current named groups use a maximum and allow solo. */
final class SoloPartyMinimumTest {
    private final UUID leader = new UUID(0, 1), member = new UUID(0, 2);
    private final D1PartyLobby.Anchor anchor = new D1PartyLobby.Anchor(new UUID(0, 900), "minecraft:overworld", 0);
    @Test void legacyConfigStillAcceptsExistingValuesWithoutMigration() {
        ModConfigSpec.ValueSpec spec = Config.SPEC.getSpec().get(List.of("TamsinVane", "minimumPartySize"));
        for (int old = 1; old <= 6; old++) assertTrue(spec.test(old));
        for (int invalid : new int[]{-1, 0, 7}) assertFalse(spec.test(invalid));
        assertEquals(3, spec.getDefault());
    }
    @Test void soloRequiresClassPersonalReadyAndLeaderQueue() {
        var lobby = new D1PartyLobby();
        assertTrue(lobby.canStartSolo(leader, 6));
        assertNull(lobby.party(leader));
        assertNull(lobby.startSolo(leader, anchor, 0, 6));
        var party = lobby.party(leader);
        assertEquals(6, party.capacity());
        assertNotNull(lobby.begin(leader, 0, Map.of(leader, "bogatyr")));
        assertNotNull(lobby.begin(leader, party.revision(), Map.of()));
        assertNotNull(lobby.begin(leader, party.revision(), Map.of(leader, "none")));
        assertNull(lobby.begin(leader, party.revision(), Map.of(leader, "bogatyr")));
        assertNotNull(lobby.queue(leader, party.revision()));
        assertNull(lobby.ready(leader, party.revision()));
        assertNull(lobby.queue(leader, party.revision()));
        lobby.startCountdown(party, 100, 100);
        assertFalse(lobby.prepare(party, 199));
        assertTrue(lobby.prepare(party, 200));
        lobby.complete(party);
        assertNull(lobby.party(leader));
        assertTrue(lobby.queued().isEmpty());
    }
    @Test void soloRejectsInvalidCapacityStaleRequestAndDuplicateMembership() {
        var lobby = new D1PartyLobby();
        for (int invalid : new int[]{-1, 0, 7}) assertNotNull(lobby.startSolo(leader, anchor, 0, invalid));
        assertNotNull(lobby.startSolo(leader, anchor, 1, 6));
        assertNotNull(lobby.startSolo(leader, null, 0, 6));
        assertNull(lobby.startSolo(leader, anchor, 0, 1));
        assertNotNull(lobby.startSolo(leader, anchor, lobby.revision(leader), 1));
        assertEquals(1, lobby.party(leader).capacity());
    }
    @Test void pendingInvitationAndOnePlayerCapacityRemainProtected() {
        var lobby = new D1PartyLobby();
        assertNull(lobby.create(member, anchor, 0, "Inviters"));
        assertNull(lobby.invite(member, leader, anchor, 0, 120, 6));
        var invitation = lobby.invitation(leader);
        assertFalse(lobby.canStartSolo(leader, 6));
        assertNotNull(lobby.startSolo(leader, anchor, 0, 6));
        assertTrue(lobby.decline(leader, invitation.token()));
        assertNull(lobby.startSolo(leader, anchor, 0, 1));
        var target = UUID.randomUUID();
        assertNotNull(lobby.invite(leader, target, anchor, 0, 120, 6));
        assertEquals(List.of(leader), lobby.remove(leader));
    }
    @Test void twoMembersMayReadyWithMaximumSix() {
        var lobby = new D1PartyLobby();
        assertNull(lobby.create(leader, anchor, 0, "Small group", 6));
        assertNull(lobby.invite(leader, member, anchor, 0, 120, 6));
        assertNull(lobby.accept(member, lobby.invitation(member).token(), 1));
        assertNull(lobby.joinAccepted(member, 2, 6, true));
        assertNull(lobby.begin(leader, lobby.revision(leader), Map.of(leader, "bogatyr", member, "bogatyr")));
    }
    @Test void soloPreservesAuthoredSixSlotSchematicPlan() {
        var plan = DungeonStartupSchematicPlan.buildPlan(List.of("bogatyr"));
        assertEquals(36, plan.requests().size());
        assertEquals(5, plan.normalizedClassSlots().stream().filter("blankslot"::equals).count());
    }
    @Test void existingPartyInvitationReadinessAndQueueRegressionsStillPass() { D1PartyChecks.main(new String[0]); }
}
