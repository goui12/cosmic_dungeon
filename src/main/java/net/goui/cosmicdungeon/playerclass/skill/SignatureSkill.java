package net.goui.cosmicdungeon.playerclass.skill;

import java.util.*;
import net.goui.cosmicdungeon.mercenary.MercenarySkill;

/** Shared identity, separate actor progress units. Existing weapon/mercenary curves are not reinterpreted. */
public enum SignatureSkill {
    POSITIVE_POTIONS("positive_potions","theurgist","Positive Potions","PP",MercenarySkill.POSITIVE_POTIONS),
    NEGATIVE_POTIONS("negative_potions","venefex","Negative Potions","NP",MercenarySkill.NEGATIVE_POTIONS),
    WOLVES("wolves","bogatyr","Wolves","WLF",MercenarySkill.WOLVES),
    FIREWORKS("fireworks","pyroclast","Fireworks","FW",MercenarySkill.FIREWORKS),
    CHAIN_LIGHTNING("chain_lightning","dragoon","Chain Lightning","CL",MercenarySkill.CHAIN_LIGHTNING),
    SMITE("smite","judicator","Smite","SM",null);
    public static final int PLAYER_CAP=100;
    private final String id, classId, title, abbreviation;
    private final MercenarySkill legacy;
    SignatureSkill(String id,String classId,String title,String abbreviation,MercenarySkill legacy) {
        this.id=id;this.classId=classId;this.title=title;this.abbreviation=abbreviation;this.legacy=legacy;
    }
    public String id(){return id;}
    public String classId(){return classId;}
    public String title(){return title;}
    public String abbreviation(){return abbreviation;}
    public MercenarySkill legacy(){return legacy;}
    public static Optional<SignatureSkill> forClass(String cls){
        return Arrays.stream(values()).filter(s->s.classId.equals(cls)).findFirst();
    }
    public static Optional<SignatureSkill> byId(String id){
        return Arrays.stream(values()).filter(s->s.id.equals(id)).findFirst();
    }
    public int cap(){return this==SMITE?4:PLAYER_CAP;}
    /** Positive potions retain exact old XP units; newly introduced tracks count successful actions. */
    public long threshold(int level,int base,int step){
        if(level<0||level>cap())throw new IllegalArgumentException("Invalid signature level");
        if(this==POSITIVE_POTIONS)return ClassSkillRules.threshold(level,base,step);
        if(this==SMITE)return level<=1?0:5L*level*(level-1);
        return (long)level*(level+1)/2;
    }
    public int level(int progress,int base,int step){
        if(progress<0)throw new IllegalArgumentException("Negative signature progress");
        int level=this==SMITE?1:0;
        while(level<cap()&&threshold(level+1,base,step)<=progress)level++;
        return level;
    }
    public int withinLevel(int progress,int base,int step){
        return level(progress,base,step)==cap()?0:(int)(progress-threshold(level(progress,base,step),base,step));
    }
    public int nextLevelCost(int progress,int base,int step){
        int level=level(progress,base,step);
        return level==cap()?0:(int)Math.min(Integer.MAX_VALUE,threshold(level+1,base,step)-threshold(level,base,step));
    }
}
