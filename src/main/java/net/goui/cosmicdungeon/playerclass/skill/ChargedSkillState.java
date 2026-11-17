package net.goui.cosmicdungeon.playerclass.skill;

/** One bounded bar; no overflow bank and no client-supplied power. */
public record ChargedSkillState(int charge,boolean armed) {
    public static int limit(SignatureSkill skill){
        return switch(skill){case CHAIN_LIGHTNING->100;case SMITE->10;default->0;};
    }
    public boolean valid(SignatureSkill skill){
        int max=limit(skill);
        return max>0&&charge>=0&&charge<=max&&(!armed||skill==SignatureSkill.CHAIN_LIGHTNING&&charge==max);
    }
    public boolean ready(SignatureSkill skill){return valid(skill)&&charge==limit(skill);}
    public ChargedSkillState hit(SignatureSkill skill){
        if(!valid(skill))return this;
        return new ChargedSkillState(Math.min(limit(skill),charge+1),armed);
    }
    public ChargedSkillState arm(){return ready(SignatureSkill.CHAIN_LIGHTNING)?new ChargedSkillState(charge,true):this;}
    public static ChargedSkillState empty(){return new ChargedSkillState(0,false);}
}
