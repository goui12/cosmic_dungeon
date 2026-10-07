package net.goui.cosmicdungeon.playerclass.resource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OnlineResourceClockTest {
    @Test void onePerSecondWithoutDuplicateCredit(){
        var clock=new OnlineResourceClock();assertFalse(clock.elapsed(1,"kibble",100));
        assertFalse(clock.elapsed(1,"kibble",119));assertTrue(clock.elapsed(1,"kibble",120));
        assertFalse(clock.elapsed(1,"kibble",120));assertTrue(clock.elapsed(1,"kibble",140));
    }
    @Test void passiveSyncCannotConsumeEarnedTime(){
        var clock=new OnlineResourceClock();clock.bind(1,"kibble",100);
        clock.bind(1,"kibble",120);assertTrue(clock.elapsed(1,"kibble",120));
        clock.bind(1,"kibble",139);assertTrue(clock.elapsed(1,"kibble",140));
    }
    @Test void disconnectInactiveNewRunAndClockRollbackNeverCatchUp(){
        var clock=new OnlineResourceClock();clock.bind(1,"kibble",0);assertTrue(clock.elapsed(1,"kibble",9999));
        assertFalse(clock.elapsed(1,"kibble",9999));
        clock.clear();assertFalse(clock.elapsed(1,"kibble",99999));
        assertFalse(clock.elapsed(2,"kibble",100020));assertFalse(clock.elapsed(2,"brewing_supplies",100040));
        assertFalse(clock.elapsed(2,"brewing_supplies",1));assertFalse(clock.elapsed(0,"",1000));
        assertFalse(clock.elapsed(2,"brewing_supplies",2000));
    }
}
