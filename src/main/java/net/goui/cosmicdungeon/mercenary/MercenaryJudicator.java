package net.goui.cosmicdungeon.mercenary;

import net.goui.cosmicdungeon.dungeon.DungeonKillCredit;
import net.goui.cosmicdungeon.dungeon.PendingDungeonRecoveryData;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Run-scoped hostile kills and a direct-hit burst; no polling, new save fields or recursive procs. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class MercenaryJudicator {
    private static final ThreadLocal<MercenaryEntity> BURST=new ThreadLocal<>();
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
    static boolean procs(int level,int percentile){
        if(percentile<0||percentile>=100)throw new IllegalArgumentException("Invalid percentile");
        return percentile<Math.clamp(level,0,100);
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
    @SubscribeEvent
    public static void hit(LivingDamageEvent.Post event){
        var source=event.getSource();
        if(!(source.getEntity() instanceof MercenaryEntity merc)||BURST.get()==merc)return;
        boolean melee=source.is(DamageTypes.MOB_ATTACK)&&source.getDirectEntity()==merc;
        boolean arrow=source.is(DamageTypes.ARROW)&&source.getDirectEntity() instanceof AbstractArrow projectile
                &&projectile.getOwner()==merc;
        if(!melee&&!arrow)return;
        float dealt=event.getNewDamage()+event.getReduction(DamageContainer.Reduction.ABSORPTION);
        if(!(dealt>0)||!Float.isFinite(dealt))return;
        var owner=activeOwner(merc);var primary=event.getEntity();
        // A lethal successful hit is still eligible before native die() runs.
        if(owner==null||!MercenaryLightning.enemy(merc,owner,primary))return;
        int skill=MercenarySkills.level(merc,MercenarySkill.COMBAT);
        if(skill==0||!procs(skill,merc.getRandom().nextInt(100)))return;
        var level=(ServerLevel)merc.level();var center=primary.position();
        // Spatially bounded, without a target cap that would silently omit crowded hostiles.
        var targets=level.getEntitiesOfClass(Mob.class,new AABB(center,center).inflate(2),
                target->target.isAlive()&&target.position().distanceToSqr(center)<=4
                        &&MercenaryLightning.enemy(merc,owner,target));
        var previous=BURST.get();BURST.set(merc);
        try{
            for(var target:targets){
                if(activeOwner(merc)!=owner)break;
                if(target.isAlive()&&target.position().distanceToSqr(center)<=4
                        &&MercenaryLightning.enemy(merc,owner,target))
                    target.hurtServer(level,merc.damageSources().mobAttack(merc),skill);
            }
        }finally{if(previous==null)BURST.remove();else BURST.set(previous);}
    }
}
