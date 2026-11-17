package net.goui.cosmicdungeon.network;

import io.netty.buffer.Unpooled;
import java.util.*;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkillSlot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PartyTimingCodecTest {
    private static PartyVitals view(long now,String caster,long expires,int charge){
        return new PartyVitals(20,23,"ACTIVE",List.of(new PartyVitals.Effect("cosmicdungeon:vitality",1,
                "Favilla Vitas","+15% maximum health",caster,expires)),0,now,
                new PartySkills("CL",3,List.of(new PartySkills.Ability("Chain Lightning",0,0,charge,100,false),
                        new PartySkills.Ability("Blackiron Sheath",6100,6000,0,0,false))));
    }
    @Test void transportClockDoesNotChurnButStateAndWinningSourceDo(){
        var first=view(100,"Alaric",54100,50);var later=view(120,"Alaric",54100,50);
        assertEquals(first,later);assertEquals(first.hashCode(),later.hashCode());
        assertNotEquals(first,view(120,"Other",54100,50));
        assertNotEquals(first,view(120,"Alaric",54120,50));
        assertNotEquals(first,view(120,"Alaric",54100,51));
    }
    @Test void fullCodecRetainsClockProvenanceAndIndependentBars(){
        var value=view(100,"Alaric",54100,50);var buf=Unpooled.buffer();
        try{
            PartyVitals.CODEC.encode(buf,value);var decoded=PartyVitals.CODEC.decode(buf);
            assertEquals(value,decoded);assertEquals(100,decoded.sampledAt());
            assertEquals(6100,decoded.skills().abilities().get(1).readyAt());
            assertEquals("+15% maximum health",decoded.effects().getFirst().amount());
            assertEquals(0,buf.readableBytes());
        }finally{buf.release();}
    }
    @Test void unavailableStateCannotPretendToHaveReadyAbilitiesAndBoundsAreEnforced(){
        var skills=view(100,"Alaric",54100,50).skills();
        assertThrows(IllegalArgumentException.class,()->PartyVitals.OFFLINE.withSkills(skills,100));
        assertThrows(IllegalArgumentException.class,()->new PartySkills.Ability("Buff",6100,6001,0,0,false));
        assertThrows(IllegalArgumentException.class,()->new PartySkills.Ability("Charge",0,0,101,100,false));
        assertThrows(IllegalArgumentException.class,()->new PartyVitals.Effect("minecraft:speed",0,"","","Unknown",-2));
        assertEquals(3,PartyVitals.OFFLINE.withSkills(new PartySkills("CL",3,List.of()),100).skills().level());
    }
    @Test void cooldownEqualityIgnoresOnlyElapsedTicksForTheSameDeadline(){
        var first=new ClassSkillPayloads.SlotView(ClassSkillSlot.SKILL_1,"buff","Buff","Description",false,6000,0,0,6100);
        var later=new ClassSkillPayloads.SlotView(ClassSkillSlot.SKILL_1,"buff","Buff","Description",false,5980,0,0,6100);
        assertEquals(first,later);assertEquals(first.hashCode(),later.hashCode());
        assertNotEquals(first,new ClassSkillPayloads.SlotView(ClassSkillSlot.SKILL_1,"buff","Buff","Description",true,0,0,0,0));
        assertNotEquals(first,new ClassSkillPayloads.SlotView(ClassSkillSlot.SKILL_1,"buff","Buff","Description",false,6000,0,0,6200));
    }
}
