package net.goui.cosmicdungeon.client.screen.requests;

import java.util.*;
import net.goui.cosmicdungeon.client.screen.requests.SupplyRequestAction.Decision;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupplyRequestsStateTest {
    private final UUID first=UUID.randomUUID(),second=UUID.randomUUID();
    private SupplyRequestsSnapshot snapshot(long run,long revision){
        return new SupplyRequestsSnapshot(run,revision,true,true,true,List.of(
                SupplyRequestsSnapshotTest.card(first,4,true),SupplyRequestsSnapshotTest.card(second,0,false)));
    }
    private SupplyRequestsState ready(){
        var state=new SupplyRequestsState();state.connected(true);state.receive(snapshot(8,2));return state;
    }
    @Test void decisionsCaptureExactGenerationAndDisplayedIdsWithoutOptimisticRemoval(){
        var state=ready();var before=state.snapshot();
        var action=state.decide(Decision.ACCEPT,first).orElseThrow();
        assertEquals(8,action.runId());assertEquals(2,action.revision());assertEquals(List.of(first),action.requestIds());
        assertEquals(Decision.ACCEPT,action.decision());assertSame(before,state.snapshot());
        assertTrue(state.pending());assertTrue(state.decide(Decision.DENY,second).isEmpty());assertFalse(state.canRequest());
        state.receive(before);assertFalse(state.pending(),"Unchanged rejection response must unlock input");
        assertTrue(state.decide(Decision.DENY,second).isPresent(),"Zero-yield requests can still be denied");
        state.receive(before);assertTrue(state.decide(Decision.ACCEPT,second).isEmpty());
        assertTrue(state.decide(Decision.ACCEPT,UUID.randomUUID()).isEmpty());
    }
    @Test void bulkKeepsDisplayedOrderAndConsentDoesNotExpandWhenCardsArrive(){
        var state=ready();var action=state.decide(Decision.ACCEPT_ALL,null).orElseThrow();
        assertEquals(List.of(first,second),action.requestIds());
        var third=UUID.randomUUID();state.receive(new SupplyRequestsSnapshot(8,3,true,true,false,List.of(
                SupplyRequestsSnapshotTest.card(third,9,true),SupplyRequestsSnapshotTest.card(first,3,true))));
        assertEquals(List.of(first,second),action.requestIds(),"Existing consent cannot acquire new unseen IDs");
        var deny=state.decide(Decision.DENY_ALL,null).orElseThrow();assertEquals(List.of(third,first),deny.requestIds());
        assertThrows(UnsupportedOperationException.class,()->deny.requestIds().clear());
    }
    @Test void requestRequiresAuthoritativeAvailabilityAndSameGenerationAcksRetrySafely(){
        var state=ready();var request=state.decide(Decision.REQUEST,null).orElseThrow();
        assertTrue(request.requestIds().isEmpty());assertTrue(state.decide(Decision.REQUEST,null).isEmpty());
        state.receive(state.snapshot());assertTrue(state.canRequest());
        state.receive(new SupplyRequestsSnapshot(8,3,true,true,false,List.of()));assertFalse(state.canRequest());
        assertTrue(state.decide(Decision.REQUEST,null).isEmpty());
        state.receive(new SupplyRequestsSnapshot(8,4,true,false,false,List.of(SupplyRequestsSnapshotTest.card(first,0,false))));
        assertFalse(state.enabled(Decision.ACCEPT_ALL,null));assertTrue(state.enabled(Decision.DENY_ALL,null));
        state.connected(false);assertFalse(state.enabled(Decision.DENY_ALL,null));
    }
    @Test void staleViewsCannotResurrectCardsAndRunChangesOrLogoutReleaseState(){
        var state=ready();var newer=new SupplyRequestsSnapshot(8,5,true,true,false,List.of());state.receive(newer);
        state.receive(snapshot(8,2));assertSame(newer,state.snapshot());
        state.receive(snapshot(9,0));assertEquals(9,state.snapshot().runId());assertTrue(state.canRequest());
        state.decide(Decision.REQUEST,null);state.clear();
        assertFalse(state.pending());assertTrue(state.snapshot().cards().isEmpty());assertEquals(0,state.snapshot().runId());
        assertFalse(state.canRequest());assertTrue(state.decide(Decision.DENY_ALL,null).isEmpty());
    }
    @Test void malformedActionsCannotRepresentAmbiguousOrUnboundedConsent(){
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestAction(8,1,Decision.REQUEST,List.of(first)));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestAction(8,1,Decision.ACCEPT,List.of()));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestAction(8,1,Decision.DENY,List.of(first,second)));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestAction(8,1,Decision.ACCEPT_ALL,List.of(first,first)));
        assertThrows(IllegalArgumentException.class,()->new SupplyRequestAction(0,1,Decision.REQUEST,List.of()));
    }
}
