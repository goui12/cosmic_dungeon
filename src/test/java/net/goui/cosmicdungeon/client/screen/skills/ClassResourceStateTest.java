package net.goui.cosmicdungeon.client.screen.skills;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassResourceStateTest {
    private ClassResourceSnapshot value(long run,int amount,boolean active,boolean alive,boolean eligible,long revision){
        return new ClassResourceSnapshot(run,"brewing_supplies",amount,600,active,alive,eligible,revision);
    }
    @Test void recycleUsesExactSnapshotAndWaitsForActionTokenWithoutPredictingBalance(){
        var state=new ClassResourceState();assertTrue(state.recycle("theurgist").isEmpty());
        var first=value(7,17,true,true,true,4);state.receive(first);
        assertSame(first,state.recycle("theurgist").orElseThrow());assertSame(first,state.snapshot());
        assertTrue(state.awaitingAction());assertTrue(state.recycle("theurgist").isEmpty());
        var regenerated=value(7,18,true,true,true,4);state.receive(regenerated);
        assertSame(regenerated,state.snapshot());assertTrue(state.awaitingAction());
        assertTrue(state.recycle("theurgist").isEmpty(),"A passive snapshot cannot unlock a duplicate request");
        var acknowledged=value(7,22,true,true,true,5);state.receive(acknowledged);
        assertFalse(state.awaitingAction());assertSame(acknowledged,state.recycle("theurgist").orElseThrow());
        state.receive(value(7,1,true,true,true,4));
        assertSame(acknowledged,state.snapshot());assertTrue(state.awaitingAction(),"Older action tokens cannot undo acknowledgement");
    }
    @Test void serverEligibilityAndClassIdentityGateActionsWhileDeadBalancesRemainVisible(){
        var state=new ClassResourceState();
        for(var blocked:new ClassResourceSnapshot[]{
                value(7,17,false,true,true,1),value(7,17,true,false,true,1),
                value(7,17,true,true,false,1),value(7,600,true,true,true,1)}){
            state.receive(blocked);assertSame(blocked,state.snapshot());assertTrue(state.recycle("theurgist").isEmpty());
            assertFalse(state.awaitingAction());
        }
        state.receive(value(7,17,true,true,true,2));
        assertTrue(state.recycle("bogatyr").isEmpty());assertFalse(state.awaitingAction());
        assertTrue(state.recycle("theurgist").isPresent());
    }
    @Test void runResetAndDisconnectDiscardPendingActionsAndOldBalances(){
        var state=new ClassResourceState();state.receive(value(7,99,true,true,true,9));state.recycle("theurgist");
        var fresh=value(8,0,true,true,true,0);state.receive(fresh);
        assertSame(fresh,state.snapshot());assertFalse(state.awaitingAction());
        state.recycle("theurgist");state.receive(value(8,1,false,true,true,0));
        assertFalse(state.awaitingAction());assertFalse(state.snapshot().canRecycle());
        state.receive(new ClassResourceSnapshot(0,"",0,600,false,false,false,0));
        assertNull(state.snapshot());assertFalse(state.awaitingAction());
        state.receive(fresh);state.recycle("theurgist");state.clear();
        assertNull(state.snapshot());assertFalse(state.awaitingAction());
        assertTrue(state.recycle("theurgist").isEmpty());
    }
    @Test void classResourceChangeCannotRetainAnotherRolesPendingAction(){
        var state=new ClassResourceState();state.receive(value(7,9,true,true,true,1));state.recycle("theurgist");
        var kibble=new ClassResourceSnapshot(7,"kibble",5,600,true,true,true,1);state.receive(kibble);
        assertFalse(state.awaitingAction());assertTrue(state.recycle("theurgist").isEmpty());
        assertSame(kibble,state.recycle("bogatyr").orElseThrow());
    }
}
