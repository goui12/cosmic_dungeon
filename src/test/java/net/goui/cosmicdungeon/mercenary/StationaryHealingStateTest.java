package net.goui.cosmicdungeon.mercenary;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class StationaryHealingStateTest {
    private StationaryHealingState repeatedThrough(long end){
        var state=StationaryHealingState.begin(0,0,0);
        for(long tick=0;tick<=end;tick+=100)state=state.damage(tick,0,0);
        return state;
    }
    @Test void warnsAtThirtySecondsAndRefusesAtFortyOnlyUnderRepeatedDamage(){
        var state=repeatedThrough(500);
        assertFalse(state.sample(599,0,0).warned());
        state=state.damage(600,0,0);
        assertTrue(state.warned());assertTrue(state.healAllowed());
        assertEquals(state,state.sample(601,0,0));
        assertTrue(state.sample(799,0,0).healAllowed());
        state=state.sample(800,0,0);
        assertTrue(state.warned());assertTrue(state.blocked());assertFalse(state.healAllowed());
        assertEquals(2,state.hits());
    }
    @Test void observationStartsAtFirstDamageAndIdleTimeCannotCreateRepeatedHits(){
        var idle=StationaryHealingState.begin(0,0,0);
        assertFalse(idle.sample(10000,0,0).warned());
        var single=idle.damage(10000,0,0);
        assertEquals(10000,single.started());assertEquals(1,single.hits());
        assertFalse(single.sample(10600,0,0).warned());
        assertTrue(single.sample(10800,0,0).healAllowed());
        assertEquals(0,single.sample(10800,0,0).hits());
    }
    @Test void tenSecondGapIsInclusiveAndLongerQuietResetsAnUnblockedObservation(){
        var state=StationaryHealingState.begin(0,0,0).damage(0,0,0).damage(200,0,0);
        assertEquals(0,state.started());assertEquals(2,state.hits());
        state=state.damage(401,0,0);
        assertEquals(401,state.started());assertEquals(1,state.hits());assertFalse(state.warned());
        var warned=repeatedThrough(600);
        assertTrue(warned.warned());
        var reset=warned.sample(801,0,0);
        assertFalse(reset.warned());assertTrue(reset.healAllowed());assertEquals(0,reset.hits());
        assertEquals(801,reset.started());
    }
    @Test void blockSurvivesQuietAndRepeatedAttemptsUntilFullHorizontalDisplacement(){
        var blocked=repeatedThrough(800);
        assertFalse(blocked.healAllowed());
        assertEquals(blocked,blocked.sample(100000,0,0));
        assertFalse(blocked.damage(100000,0,0).healAllowed());
        assertFalse(blocked.sample(100000,.999,0).healAllowed());
        var recovered=blocked.sample(100000,1,0);
        assertTrue(recovered.healAllowed());assertFalse(recovered.warned());assertEquals(0,recovered.hits());
        assertEquals(1,recovered.x());assertEquals(100000,recovered.started());
        var hitAfterMoving=blocked.damage(100000,-1,0);
        assertTrue(hitAfterMoving.healAllowed());assertEquals(1,hitAfterMoving.hits());
        assertEquals(100000,hitAfterMoving.started());
    }
    @Test void displacementUsesBothHorizontalAxesAndDoesNotAccumulateJitter(){
        var blocked=repeatedThrough(800);
        assertTrue(blocked.sample(800,0,1).healAllowed());
        assertTrue(blocked.sample(800,.6,.8).healAllowed());
        assertFalse(blocked.sample(800,.6,.79).healAllowed());
        var jitter=blocked.sample(810,.9,0).sample(820,-.9,0).sample(830,0,.9);
        assertFalse(jitter.healAllowed());assertEquals(0,jitter.x());assertEquals(0,jitter.z());
        // There is no attack, look, Y-axis or weapon signal in this independent model.
        var progressing=repeatedThrough(500).sample(550,.99,0).damage(600,.99,0);
        assertTrue(progressing.warned());assertEquals(0,progressing.started());
    }
    @Test void versionedStateRoundTripsWithoutOfflineTimeAdvancingItsObservation(){
        for(var state:List.of(StationaryHealingState.begin(700,-12.25,83.5),
                repeatedThrough(500),repeatedThrough(600),repeatedThrough(800))){
            long onlineClock=Math.max(800,state.lastDamage());
            assertEquals(state,StationaryHealingState.parse(state.serialize(),onlineClock,999,999));
        }
        var original=repeatedThrough(500);
        var reloaded=StationaryHealingState.parse(original.serialize(),500,0,0);
        assertFalse(reloaded.sample(599,0,0).warned());
        assertTrue(reloaded.damage(600,0,0).warned());
    }
    @Test void malformedUnknownAndFutureStorageFailsClosedButRemainsRecoverable(){
        for(String raw:List.of("","garbage","2|0|0|0|0|2|true|true","1|0|0|0|0|2|true|true|extra",
                "1|-1|0|0|0|2|true|true","1|10|0|0|0|2|true|true","1|0|801|0|0|2|true|true",
                "1|0|0|NaN|0|2|true|true","1|0|0|0|Infinity|2|true|true",
                "1|0|0|0|0|3|true|true","1|0|0|0|0|0|true|false",
                "1|0|0|0|0|2|false|true","1|0|0|0|0|2|TRUE|false",
                "1|9223372036854775808|0|0|0|2|true|true")){
            var safe=StationaryHealingState.parse(raw,800,10,20);
            assertFalse(safe.healAllowed(),raw);
            assertEquals(10,safe.x());assertEquals(20,safe.z());
            assertTrue(safe.sample(800,11,20).healAllowed(),raw);
        }
        assertFalse(StationaryHealingState.parse(null,800,10,20).healAllowed());
    }
    @Test void invalidInputsAreRejectedAndLongClocksDoNotOverflow(){
        assertThrows(IllegalArgumentException.class,()->StationaryHealingState.begin(-1,0,0));
        assertThrows(IllegalArgumentException.class,()->StationaryHealingState.begin(0,Double.NaN,0));
        assertThrows(IllegalArgumentException.class,()->new StationaryHealingState(0,0,0,0,-1,false,false));
        var state=StationaryHealingState.begin(10,0,0).damage(20,0,0);
        assertThrows(IllegalArgumentException.class,()->state.sample(19,0,0));
        assertThrows(IllegalArgumentException.class,()->state.damage(19,0,0));
        assertThrows(IllegalArgumentException.class,()->state.sample(20,0,Double.POSITIVE_INFINITY));
        long start=Long.MAX_VALUE-800;
        var nearLimit=StationaryHealingState.begin(start,0,0);
        for(int offset=0;offset<=800;offset+=100)nearLimit=nearLimit.damage(start+offset,0,0);
        assertFalse(nearLimit.healAllowed());assertTrue(nearLimit.warned());
    }
}
