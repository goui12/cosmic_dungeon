package net.goui.cosmicdungeon.playerclass.skill;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClassBuffTest {
    @Test void approvedCatalogAndReservedClasses(){
        assertEquals(ClassBuff.FLURRY,ClassBuff.forClass("bogatyr",1).orElseThrow());
        assertEquals(36000,ClassBuff.FLURRY.duration());
        assertEquals(18000,ClassBuff.SHEATH.duration());
        assertEquals(ClassBuff.AEGIS,ClassBuff.forClass("judicator",2).orElseThrow());
        assertEquals(ClassBuff.BULWARK,ClassBuff.forClass("judicator",3).orElseThrow());
        assertEquals(0,ClassBuff.AEGIS.amplifier());assertEquals(1,ClassBuff.BULWARK.amplifier());
        assertEquals(ClassBuff.BANISH,ClassBuff.forClass("venefex",1).orElseThrow());
        for(String cls:List.of("theurgist","pyroclast","deadeye","metalmancer",""))assertTrue(ClassBuff.forClass(cls,4).isEmpty());
        assertEquals(6000,ClassBuff.COOLDOWN);assertEquals(10,ClassBuff.RADIUS);
    }
    @Test void deadlinesSurviveReentryAndFailClosedOnUnknownData(){
        assertEquals(0,ClassBuffCooldown.deadline(List.of()));
        for(var value:List.of(List.of("-1"),List.of("bad"),List.of("10","20"),List.of("9223372036854775808")))
            assertEquals(Long.MAX_VALUE,ClassBuffCooldown.deadline(value));
        assertFalse(ClassBuffCooldown.ready(Long.MAX_VALUE,Long.MAX_VALUE));
        assertFalse(ClassBuffCooldown.ready(0,-1));assertFalse(ClassBuffCooldown.ready(0,Long.MAX_VALUE));
        assertTrue(ClassBuffCooldown.ready(6100,6100));assertFalse(ClassBuffCooldown.ready(6100,6099));
        assertEquals(6000,ClassBuffCooldown.remaining(6100,100));
        assertEquals(1,ClassBuffCooldown.remaining(6100,6099));assertEquals(0,ClassBuffCooldown.remaining(6100,6100));
        assertEquals("5:00",ClassBuffCooldown.display(6000));assertEquals("0:01",ClassBuffCooldown.display(1));
        assertEquals("1:00",ClassBuffCooldown.display(1181));assertEquals("0:00",ClassBuffCooldown.display(0));
    }
    @Test void eachActorAndClassOwnsItsCooldownButStrengthRanksShareIt(){
        assertNotEquals(ClassBuffs.cooldownKey("player:a","bogatyr"),ClassBuffs.cooldownKey("player:b","bogatyr"));
        assertNotEquals(ClassBuffs.cooldownKey("player:a","bogatyr"),ClassBuffs.cooldownKey("mercenary:a","bogatyr"));
        assertEquals(ClassBuffs.cooldownKey("player:a",ClassBuff.AEGIS.classId()),ClassBuffs.cooldownKey("player:a",ClassBuff.BULWARK.classId()));
    }
}
