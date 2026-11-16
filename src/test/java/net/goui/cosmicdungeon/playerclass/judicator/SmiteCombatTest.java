package net.goui.cosmicdungeon.playerclass.judicator;

import net.goui.cosmicdungeon.playerclass.skill.SignatureSkill;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SmiteCombatTest {
    @Test void namedRanksHaveTheApprovedSphericalRangeAndHeartDamage(){
        String[] names={"Gravehammer","Bonegrinder","Deathpurge","Last Rites"};
        float[] damage={2.5f,2.5f,5,5};
        for(int level=1;level<=4;level++){
            var rank=SmiteCombat.rank(level);
            assertEquals(names[level-1],rank.name());
            assertEquals(level+2,rank.radius());
            assertEquals(damage[level-1],rank.damage());
        }
        assertEquals("last_rites",SmiteCombat.rank(4).id());
        assertEquals(SmiteCombat.rank(1),SmiteCombat.rank(Integer.MIN_VALUE));
        assertEquals(SmiteCombat.rank(4),SmiteCombat.rank(Integer.MAX_VALUE));
    }
    @Test void successfulActivationsUnlockAtTenThirtyAndSixtyRatherThanEveryKill(){
        var smite=SignatureSkill.SMITE;
        for(int progress:new int[]{0,9,10,29,30,59,60,1000}){
            int expected=progress<10?1:progress<30?2:progress<60?3:4;
            assertEquals(expected,smite.level(progress,250,50));
            assertEquals(expected+2,SmiteCombat.rank(smite.level(progress,250,50)).radius());
        }
        assertEquals(10,smite.threshold(2,250,50));
        assertEquals(30,smite.threshold(3,250,50));
        assertEquals(60,smite.threshold(4,250,50));
    }
}
