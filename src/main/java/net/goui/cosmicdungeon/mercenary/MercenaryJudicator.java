package net.goui.cosmicdungeon.mercenary;

import net.goui.cosmicdungeon.dungeon.DungeonKillCredit;
import net.goui.cosmicdungeon.dungeon.PendingDungeonRecoveryData;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.goui.cosmicdungeon.playerclass.skill.ChargedClassSkills;
import net.goui.cosmicdungeon.playerclass.skill.SignatureSkill;

/** Historical Combat kills remain readable; charged Smite replaces the former random hit burst. */
public final class MercenaryJudicator {
    private MercenaryJudicator(){}
    /** Cumulative totals: 0, 1, 3, 6, 12, 24, then doubling. Long arithmetic keeps the next goal safe. */
    static long threshold(int level){
        if(level<0||level>63)throw new IllegalArgumentException("Invalid Combat level");
        return level==0?0:level==1?1:3L<<(level-2);
    }
    static int level(int kills){
        if(kills<0)throw new IllegalArgumentException("Negative Combat progress");
        int level=0;
        while(threshold(level+1)<=kills)level++;
        return level;
    }
    static ServerPlayer activeOwner(MercenaryEntity merc){
        if(!MercenarySkill.COMBAT.supports(merc.contract()))return null;
        var owner=MercenaryBrain.hirer(merc);
        if(owner==null)return null;
        var recovery=PendingDungeonRecoveryData.get(owner.level().getServer());
        var handoff=recovery.handoff(owner.getUUID());
        return recovery.completed(owner.getUUID())>=merc.runId()
                ||handoff!=null&&handoff.run()==merc.runId()&&handoff.kind().equals("cleanup")?null:owner;
    }
    /** Called only by native kill callbacks after death cancellation and duplicate-death guards. */
    static void killed(MercenaryEntity merc,LivingEntity victim,DamageSource source){
        if(source.is(DamageTypes.GENERIC_KILL))return;
        var owner=activeOwner(merc);
        if(owner!=null&&MercenaryLightning.enemy(merc,owner,victim)
                &&owner.getUUID().equals(DungeonKillCredit.resolve(victim,source)))
            MercenarySkills.success(merc,MercenarySkill.COMBAT);
    }
    /** Called only by the existing ten-tick mercenary decision, never from a recursive damage callback. */
    public static void tick(MercenaryEntity merc){
        if(MercenarySkill.COMBAT.supports(merc.contract())
                &&ChargedClassSkills.charge(merc,SignatureSkill.SMITE)>=10)ChargedClassSkills.smite(merc);
    }
}
