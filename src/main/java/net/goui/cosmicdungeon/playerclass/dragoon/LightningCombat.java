package net.goui.cosmicdungeon.playerclass.dragoon;

import java.util.*;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;

/** Existing radius/LOS/target limits and triggering-hit multiplier, shared by both actor types. */
public final class LightningCombat {
    private LightningCombat(){}
    public static int cast(LivingEntity caster,Mob initial,DamageSource source,float dealt){
        if(!(caster.level() instanceof ServerLevel level)||!Float.isFinite(dealt)||dealt<=0
                ||!ChargedClassSkills.active(caster,SignatureSkill.CHAIN_LIGHTNING))return 0;
        var owner=caster instanceof MercenaryEntity merc?MercenaryBrain.hirer(merc):null;
        if(caster instanceof MercenaryEntity&&(owner==null||caster.distanceToSqr(owner)>100))return 0;
        float damage=(float)Math.min(Float.MAX_VALUE,(double)dealt*Config.CHAIN_DAMAGE.get());
        if(!(damage>0))return 0;
        double range=Config.CHAIN_RADIUS.get(),radiusSquared=range*range;
        var candidates=new ArrayList<Mob>();
        MercenaryBrain.nearby(level,Mob.class,caster.getBoundingBox().inflate(range),Config.CHAIN_CANDIDATE_LIMIT.get(),target->{
            if(target!=initial&&target.isAlive()&&ChargedClassSkills.enemy(caster,target)
                    &&caster.distanceToSqr(target)<=radiusSquared&&(owner==null||owner.distanceToSqr(target)<=radiusSquared))
                candidates.add(target);
        });
        candidates.sort(Comparator.<Mob>comparingDouble(caster::distanceToSqr).thenComparing(Mob::getUUID));
        var targets=new ArrayList<Mob>();
        for(var target:candidates){
            if(caster.hasLineOfSight(target))targets.add(target);
            if(targets.size()>=Config.CHAIN_TARGET_LIMIT.get())break;
        }
        return SecondarySkillDamage.run(()->{
            int hits=0;LivingEntity from=caster;
            for(var target:targets){
                if(!ChargedClassSkills.active(caster,SignatureSkill.CHAIN_LIGHTNING))break;
                if(!target.isAlive()||!ChargedClassSkills.enemy(caster,target))continue;
                float before=target.getHealth()+target.getAbsorptionAmount();
                target.hurtServer(level,source,damage);
                if(target.getHealth()+target.getAbsorptionAmount()<before){
                    hits++;DragoonPassiveEvents.spawnLightningArc(level,from,target);from=target;
                }
            }
            return hits;
        });
    }
}
