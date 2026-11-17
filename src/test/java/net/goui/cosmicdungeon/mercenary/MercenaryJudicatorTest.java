package net.goui.cosmicdungeon.mercenary;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryJudicatorTest {
    @Test void cumulativeKillsMatchEveryApprovedAnchorAndDoublingBoundary(){
        int[] totals={0,1,3,6,12,24,48,96};
        for(int level=0;level<totals.length;level++){
            assertEquals(totals[level],MercenarySkill.COMBAT.thresholdFor(level));
            assertEquals(level,MercenarySkill.COMBAT.levelFor(totals[level]));
        }
        for(int level=1;level<=31;level++){
            int total=(int)MercenaryJudicator.threshold(level);
            assertEquals(level-1,MercenaryJudicator.level(total-1));
            assertEquals(level,MercenaryJudicator.level(total));
            if(level>=2)assertEquals(2L*total,MercenaryJudicator.threshold(level+1));
        }
        assertEquals(31,MercenaryJudicator.level(Integer.MAX_VALUE));
        assertEquals(3221225472L,MercenaryJudicator.threshold(32));
        assertEquals(6917529027641081856L,MercenaryJudicator.threshold(63));
        assertThrows(IllegalArgumentException.class,()->MercenaryJudicator.threshold(64));
        assertThrows(IllegalArgumentException.class,()->MercenaryJudicator.threshold(-1));
        assertThrows(IllegalArgumentException.class,()->MercenaryJudicator.level(-1));
    }
    @Test void hudUsesDistanceToNextDoublingGoalWithoutOverflow(){
        assertEquals("Combat - Level 0 (0/1)",MercenarySkill.description("combat",0));
        assertEquals("Combat - Level 1 (0/2)",MercenarySkill.description("combat",1));
        assertEquals("Combat - Level 2 (1/3)",MercenarySkill.description("combat",4));
        assertEquals("Combat - Level 4 (11/12)",MercenarySkill.description("combat",23));
        assertEquals("Combat - Level 5 (0/24)",MercenarySkill.description("combat",24));
        assertEquals("Combat - Level 31 (536870911/1610612736)",
                MercenarySkill.description("combat",Integer.MAX_VALUE));
    }
}
