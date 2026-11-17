package net.goui.cosmicdungeon.playerclass.skill;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class ChargedSkillStateTest {
    @Test void fixedBarsDoNotBankOverflowAndOnlyFullLightningArms(){
        for(var skill:new SignatureSkill[]{SignatureSkill.CHAIN_LIGHTNING,SignatureSkill.SMITE}){
            var state=ChargedSkillState.empty();int max=ChargedSkillState.limit(skill);
            for(int i=0;i<max-1;i++)state=state.hit(skill);
            assertFalse(state.ready(skill));assertFalse(state.arm().armed());
            state=state.hit(skill);assertTrue(state.ready(skill));
            for(int i=0;i<1000;i++)state=state.hit(skill);
            assertEquals(max,state.charge());assertEquals(skill==SignatureSkill.CHAIN_LIGHTNING,state.arm().armed());
        }
    }
    @Test void corruptOrWrongTrackCannotBecomeReady(){
        for(var skill:SignatureSkill.values()){
            assertFalse(new ChargedSkillState(-1,false).valid(skill));
            assertFalse(new ChargedSkillState(101,true).valid(skill));
        }
        assertFalse(new ChargedSkillState(10,true).valid(SignatureSkill.SMITE));
        assertFalse(new ChargedSkillState(99,true).valid(SignatureSkill.CHAIN_LIGHTNING));
        assertFalse(ChargedSkillState.empty().ready(SignatureSkill.WOLVES));
    }
    @Test void secondaryScopeRestoresItsPriorStateEvenAfterFailure(){
        assertFalse(SecondarySkillDamage.active());
        assertEquals(4,SecondarySkillDamage.run(()->{
            assertTrue(SecondarySkillDamage.active());
            assertEquals(3,SecondarySkillDamage.run(()->{assertTrue(SecondarySkillDamage.active());return 3;}));
            assertTrue(SecondarySkillDamage.active());return 4;
        }));
        assertFalse(SecondarySkillDamage.active());
        assertThrows(IllegalStateException.class,()->SecondarySkillDamage.run(()->{throw new IllegalStateException("test");}));
        assertFalse(SecondarySkillDamage.active());
    }
}
