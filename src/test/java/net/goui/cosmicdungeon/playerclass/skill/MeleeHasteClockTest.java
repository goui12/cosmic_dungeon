package net.goui.cosmicdungeon.playerclass.skill;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MeleeHasteClockTest {
    private int attacks(int decisions,int cooldown,boolean melee,boolean hasted){
        var clock=new MeleeHasteClock();int attacks=0;
        for(int i=0;i<decisions;i++){
            clock.advance(10,hasted);
            if(clock.ready()){attacks++;clock.restart(cooldown,melee,true);}
        }
        return attacks;
    }
    @Test void sustainedMeleeGainsTenPercentWithoutIncreasingDecisionFrequency(){
        assertEquals(100,attacks(200,20,true,false));
        assertEquals(110,attacks(200,20,true,true));
        assertEquals(1000,attacks(2000,20,true,false));
        assertEquals(1100,attacks(2000,20,true,true));
    }
    @Test void rangedAndFireworkCadenceRetainsItsNativeInterval(){
        assertEquals(50,attacks(200,40,false,false));
        assertEquals(50,attacks(200,40,false,true));
    }
    @Test void idleFailureAndWeaponSwitchDoNotBankExtraAttacks(){
        var clock=new MeleeHasteClock();clock.restart(20,true,true);
        clock.advance(10,true);clock.advance(10,true);
        assertTrue(clock.ready());
        clock.advance(10,true);clock.restart(20,true,true);
        clock.advance(10,true);assertFalse(clock.ready());
        clock.advance(10,true);assertTrue(clock.ready());
        clock.restart(40,false,true);
        for(int i=0;i<3;i++){clock.advance(10,true);assertFalse(clock.ready());}
        clock.advance(10,true);assertTrue(clock.ready());
        clock.restart(20,true,false);clock.advance(10,false);assertFalse(clock.ready());
        clock.advance(10,false);assertTrue(clock.ready());
        clock.clear();assertTrue(clock.ready());
    }
    @Test void invalidIntervalsFailBeforeAlteringTheClock(){
        var clock=new MeleeHasteClock();
        assertThrows(IllegalArgumentException.class,()->clock.advance(-1,true));
        assertThrows(IllegalArgumentException.class,()->clock.advance(21,true));
        assertThrows(IllegalArgumentException.class,()->clock.restart(0,true,true));
    }
}
