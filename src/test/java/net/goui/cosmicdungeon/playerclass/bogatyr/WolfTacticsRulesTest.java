package net.goui.cosmicdungeon.playerclass.bogatyr;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WolfTacticsRulesTest {
    @Test void strategicPrioritizesRangedThenDescendingMaxHealthWithoutASkeletonTier(){
        assertTrue(WolfTacticsRules.compareStrategic(true,10,"ranged",false,1000,"melee")<0);
        assertTrue(WolfTacticsRules.compareStrategic(false,1000,"melee",true,10,"ranged")>0);
        assertTrue(WolfTacticsRules.compareStrategic(true,20,"skeleton",true,40,"pillager")>0);
        assertTrue(WolfTacticsRules.compareStrategic(false,40,"large",false,20,"small")<0);
        assertTrue(WolfTacticsRules.compareStrategic(true,40,"large",true,20,"small")<0);
    }
    @Test void equalPriorityUsesStableIdentityAndInvalidHealthNeverOutranksFiniteHealth(){
        assertTrue(WolfTacticsRules.compareStrategic(true,20,"a",true,20,"b")<0);
        assertTrue(WolfTacticsRules.compareStrategic(true,20,"b",true,20,"a")>0);
        assertEquals(0,WolfTacticsRules.compareStrategic(false,20,"same",false,20,"same"));
        for(double health:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1,0})
            assertTrue(WolfTacticsRules.compareStrategic(true,health,"a",true,20,"b")>0);
    }
    @Test void dangerCloseIncludesItsExactBoundaryButRejectsInvalidDistances(){
        assertTrue(WolfTacticsRules.inside(0));assertTrue(WolfTacticsRules.inside(Math.nextDown(256d)));
        assertTrue(WolfTacticsRules.inside(256));assertFalse(WolfTacticsRules.inside(Math.nextUp(256d)));
        for(double distance:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            assertFalse(WolfTacticsRules.inside(distance));
    }
    @Test void rescueRequiresALivingPlayerAtOrBelowThreeHearts(){
        assertTrue(WolfTacticsRules.critical(Float.MIN_VALUE));
        assertTrue(WolfTacticsRules.critical(Math.nextDown(6f)));assertTrue(WolfTacticsRules.critical(6));
        assertFalse(WolfTacticsRules.critical(Math.nextUp(6f)));
        for(float health:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})
            assertFalse(WolfTacticsRules.critical(health));
    }
    @Test void healingRequiresOneHundredTicksAndNeverFiresEarlyOnClockRollback(){
        assertFalse(WolfTacticsRules.healDue(1000,1000));
        assertFalse(WolfTacticsRules.healDue(1099,1000));assertTrue(WolfTacticsRules.healDue(1100,1000));
        assertTrue(WolfTacticsRules.healDue(1101,1000));assertFalse(WolfTacticsRules.healDue(999,1000));
        assertFalse(WolfTacticsRules.healDue(Long.MIN_VALUE,Long.MAX_VALUE));
        assertTrue(WolfTacticsRules.healDue(Long.MAX_VALUE,Long.MIN_VALUE));
        assertTrue(WolfTacticsRules.healDue(Long.MIN_VALUE+100,Long.MIN_VALUE));
        assertFalse(WolfTacticsRules.healDue(Long.MIN_VALUE+99,Long.MIN_VALUE));
        assertTrue(WolfTacticsRules.healDue(Long.MAX_VALUE,Long.MAX_VALUE-100));
        assertFalse(WolfTacticsRules.healDue(Long.MAX_VALUE,Long.MAX_VALUE-99));
        assertFalse(WolfTacticsRules.healDue(Long.MAX_VALUE,Long.MAX_VALUE));
    }
}
