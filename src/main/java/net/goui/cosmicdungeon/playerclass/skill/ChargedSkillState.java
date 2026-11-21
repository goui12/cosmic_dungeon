package net.goui.cosmicdungeon.playerclass.skill;

/** One bounded bar; no overflow bank and no client-supplied power. */
public record ChargedSkillState(int charge,boolean armed) {
    public static int limit(SignatureSkill skill){
        return switch(skill){case CHAIN_LIGHTNING, SMITE->10;default->0;};
    }
    static ChargedSkillState fromSaved(SignatureSkill skill, int count, int armed) {
        // Valid pre-1.8.15 lightning bars used a 100-hit cap. Preserve earned hits and valid armed state.
        if (skill == SignatureSkill.CHAIN_LIGHTNING && count > limit(skill) && count <= 100
                && (armed == 0 || armed == 1 && count == 100)) count = limit(skill);
        return new ChargedSkillState(armed < 0 || armed > 1 ? -1 : count, armed == 1);
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
    public ChargedSkillState arm(SignatureSkill skill) {
        return skill == SignatureSkill.CHAIN_LIGHTNING && ready(skill) ? new ChargedSkillState(charge, true) : this;
    }
    public static ChargedSkillState empty(){return new ChargedSkillState(0,false);}
}
