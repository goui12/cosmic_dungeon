package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.network.BogatyrPayloads.Kind;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class WolfCommandRulesTest {
    @Test void careAllowsOneAffordableWolfWhileRegroupRequiresTheWholePack(){
        assertEquals(1,WolfCommandRules.count(Kind.BREED,9,5));
        assertEquals(3,WolfCommandRules.count(Kind.BREED,9,19));
        assertEquals(0,WolfCommandRules.count(Kind.HEAL,9,4));
        assertEquals(2,WolfCommandRules.count(Kind.HEAL,9,10));
        assertEquals(0,WolfCommandRules.count(Kind.REGROUP,9,8));
        assertEquals(9,WolfCommandRules.count(Kind.REGROUP,9,9));
        assertEquals(0,WolfCommandRules.count(Kind.SUMMON,100,29));
        assertEquals(1,WolfCommandRules.count(Kind.SUMMON,100,600));
        assertEquals(0,WolfCommandRules.count(Kind.SUMMON,0,600));
    }
    @Test void juvenileCooldownInLoveDeadAndNonFiniteWolvesAreNeverPaidTargets(){
        assertTrue(WolfCommandRules.breed(0,false,20,20));
        assertFalse(WolfCommandRules.breed(-1,false,20,20));
        assertFalse(WolfCommandRules.breed(1,false,20,20));
        assertFalse(WolfCommandRules.breed(0,true,20,20));
        assertFalse(WolfCommandRules.breed(0,false,19,20));
        assertTrue(WolfCommandRules.injured(1,20));
        assertFalse(WolfCommandRules.injured(20,20));assertFalse(WolfCommandRules.injured(0,20));
        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){
            assertFalse(WolfCommandRules.injured(bad,20));assertFalse(WolfCommandRules.injured(1,bad));
            assertFalse(WolfCommandRules.breed(0,false,bad,20));assertFalse(WolfCommandRules.breed(0,false,20,bad));
        }
    }
    @Test void LargeUncappedPacksCannotOverflowTheResourceBudget(){
        assertEquals(120,WolfCommandRules.count(Kind.BREED,Integer.MAX_VALUE,600));
        assertEquals(120,WolfCommandRules.count(Kind.HEAL,Integer.MAX_VALUE,600));
        assertEquals(0,WolfCommandRules.count(Kind.REGROUP,Integer.MAX_VALUE,600));
        assertEquals(600,WolfCommandRules.count(Kind.REGROUP,600,600));
        for(var kind:Kind.values()){
            assertThrows(IllegalArgumentException.class,()->WolfCommandRules.count(kind,-1,5));
            assertThrows(IllegalArgumentException.class,()->WolfCommandRules.count(kind,1,-1));
            assertThrows(IllegalArgumentException.class,()->WolfCommandRules.count(kind,1,601));
        }
    }
}
