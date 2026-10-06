package net.goui.cosmicdungeon.npc.tamsin;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PartyTradePolicyTest {
    @Test void onlySubmittedAdventureBlocksTradingAndInvalidQueueProbeIsReadOnly() {
        var lobby=new D1PartyLobby();var leader=UUID.randomUUID();
        var anchor=new D1PartyLobby.Anchor(UUID.randomUUID(),"minecraft:overworld",0);
        assertTrue(D1PartyTrades.allowed(null));
        assertNull(lobby.create(leader,anchor,0,"Test",1));var party=lobby.party(leader);
        assertTrue(D1PartyTrades.allowed(party));
        assertNull(lobby.begin(leader,party.revision(),Map.of(leader,"bogatyr")));
        assertTrue(D1PartyTrades.allowed(party));
        long revision=party.revision();
        assertNotNull(lobby.queueProblem(leader,revision));
        assertEquals(revision,party.revision());assertTrue(party.ready().isEmpty());
        assertNull(lobby.ready(leader,revision));assertNull(lobby.queueProblem(leader,revision));
        assertTrue(D1PartyTrades.allowed(party));assertEquals(revision,party.revision());
        assertNull(lobby.queue(leader,revision));assertFalse(D1PartyTrades.allowed(party));
        lobby.startCountdown(party,0,1);assertTrue(lobby.prepare(party,1));
        assertFalse(D1PartyTrades.allowed(party));
        lobby.complete(party);assertTrue(D1PartyTrades.allowed(lobby.party(leader)));
    }
}
