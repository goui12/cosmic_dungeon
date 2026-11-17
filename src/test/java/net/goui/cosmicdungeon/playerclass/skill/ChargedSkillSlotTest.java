package net.goui.cosmicdungeon.playerclass.skill;

import java.util.List;
import net.goui.cosmicdungeon.client.screen.skills.ClassAbilityClient;
import net.goui.cosmicdungeon.network.ClassSkillPayloads;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class ChargedSkillSlotTest {
    private static ClassSkillPayloads.View view(String cls,int charge,boolean armed,boolean active){
        var slots=new java.util.ArrayList<>(ClassSkillActions.slots());
        slots.set(0,ClassSkillActions.primary(cls,1,charge,armed,active));
        return new ClassSkillPayloads.View(active?10:0,8,cls,slots,List.of());
    }
    @Test void fullLightningEnablesOneGenericArmAndArmedStateDisablesDuplicateInput(){
        assertTrue(ClassAbilityClient.request(view("dragoon",99,false,true),"dragoon",ClassSkillSlot.PRIMARY).isEmpty());
        assertTrue(ClassAbilityClient.request(view("dragoon",100,false,true),"dragoon",ClassSkillSlot.PRIMARY).isPresent());
        assertTrue(ClassAbilityClient.request(view("dragoon",100,true,true),"dragoon",ClassSkillSlot.PRIMARY).isEmpty());
        assertTrue(ClassAbilityClient.unavailable(view("dragoon",100,true,true),"dragoon",ClassSkillSlot.PRIMARY).contains("Armed"));
        assertTrue(ClassAbilityClient.request(view("dragoon",100,false,false),"dragoon",ClassSkillSlot.PRIMARY).isEmpty());
    }
    @Test void smiteNeedsTenAndClassChangesCannotReuseLightningView(){
        assertFalse(ClassSkillActions.primary("judicator",1,9,false,true).enabled());
        assertTrue(ClassSkillActions.primary("judicator",4,10,false,true).title().contains("Last Rites"));
        assertTrue(ClassAbilityClient.request(view("judicator",10,false,true),"judicator",ClassSkillSlot.PRIMARY).isPresent());
        assertTrue(ClassAbilityClient.request(view("dragoon",100,false,true),"judicator",ClassSkillSlot.PRIMARY).isEmpty());
        for(var slot:List.of(ClassSkillSlot.SKILL_1,ClassSkillSlot.SKILL_2,ClassSkillSlot.SKILL_3))
            assertTrue(ClassAbilityClient.request(view("judicator",10,false,true),"judicator",slot).isEmpty());
        assertTrue(ClassSkillActions.primary("pyroclast",100,0,false,true).abilityId().isEmpty());
    }
}
