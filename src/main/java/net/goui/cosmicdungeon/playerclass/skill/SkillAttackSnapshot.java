package net.goui.cosmicdungeon.playerclass.skill;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Immutable firing identity, plus an action budget carried by the same entity save. */
public final class SkillAttackSnapshot {
    private SkillAttackSnapshot() {}
    public static CompoundTag create(UUID owner, String cls, String skill, long run) {
        if(owner==null || run<=0 || !ClassSkillRules.known(cls,skill)) throw new IllegalArgumentException("Invalid skill shot");
        var data=new CompoundTag();data.putString("owner",owner.toString());data.putString("class",cls);
        data.putString("skill",skill);data.putLong("run",run);return data;
    }
    public static boolean matches(CompoundTag data, UUID owner, String cls, long run) {
        return owner!=null && run>0 && run==data.getLongOr("run",-1)
                && owner.toString().equals(data.getStringOr("owner",""))
                && cls.equals(data.getStringOr("class",""))
                && ClassSkillRules.known(cls,data.getStringOr("skill",""))
                && data.getIntOr("xp_spent",0)>=0 && data.getIntOr("xp_spent",0)<=1000;
    }
}
