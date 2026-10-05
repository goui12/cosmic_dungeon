package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.dungeon.PendingDungeonRecoveryData;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.goui.cosmicdungeon.playerclass.dragoon.DragoonPassiveEvents;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Damage events bank hits; only the existing ten-tick AI decision may spend a charge. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class MercenaryLightning {
    private static final ThreadLocal<MercenaryEntity> CASTING=new ThreadLocal<>();
    private MercenaryLightning(){}
    static boolean enabled(MercenaryEntity merc){return MercenarySkill.CHAIN_LIGHTNING.supports(merc.contract());}
    static ServerPlayer activeOwner(MercenaryEntity merc){
        if(!enabled(merc))return null;
        var owner=MercenaryBrain.hirer(merc);
        if(owner==null)return null;
        var recovery=PendingDungeonRecoveryData.get(owner.level().getServer());
        var plan=recovery.handoff(owner.getUUID());
        return recovery.completed(owner.getUUID())>=merc.runId()
                ||plan!=null&&plan.run()==merc.runId()&&plan.kind().equals("cleanup")?null:owner;
    }
    // Post-damage may already have killed the victim; type/team eligibility must still credit that hit.
    static boolean enemy(MercenaryEntity merc,ServerPlayer owner,LivingEntity target){
        if(target==merc||target.level()!=merc.level()||target.isSpectator()
                ||target instanceof MercenaryEntity||CompanionAllies.friendly(target)
                ||target.isAlliedTo(merc)||target.isAlliedTo(owner))return false;
        return target instanceof Enemy||target instanceof Mob mob&&mob.getTarget()!=null
                &&MercenaryBrain.ally(merc,mob.getTarget());
    }
    @SubscribeEvent
    public static void hit(LivingDamageEvent.Post event){
        var source=event.getSource();
        if(!(source.getEntity() instanceof MercenaryEntity merc)||CASTING.get()==merc)return;
        boolean direct=source.is(DamageTypes.MOB_ATTACK)&&source.getDirectEntity()==merc;
        boolean projectile=(source.is(DamageTypes.ARROW)||source.is(DamageTypes.TRIDENT))
                &&MercenaryPotions.owner(source.getDirectEntity())==merc;
        if(!direct&&!projectile)return;
        float dealt=event.getNewDamage()+event.getReduction(DamageContainer.Reduction.ABSORPTION);
        var owner=activeOwner(merc);
        if(owner!=null&&dealt>0&&enemy(merc,owner,event.getEntity()))
            merc.lightning(merc.lightning().primary(dealt,event.getEntity().getUUID()));
    }
    static void tick(MercenaryEntity merc,ServerLevel level,int ticks){
        var owner=activeOwner(merc);
        if(owner==null)return;
        merc.lightning(merc.lightning().advance(ticks));
        int skill=MercenarySkills.level(merc,MercenarySkill.CHAIN_LIGHTNING);
        if(!merc.lightning().ready(skill)||merc.distanceToSqr(owner)>100)return;
        float damage=(float)Math.min(Float.MAX_VALUE,(double)merc.lightning().damage()*Config.CHAIN_DAMAGE.get());
        if(!(damage>0))return;
        var targets=targets(merc,owner,level);
        if(targets.isEmpty())return;
        int cost=MercenaryLightningState.threshold(skill);
        merc.lightning(merc.lightning().spend(skill));
        int hits=burst(merc,owner,level,targets,damage);
        if(hits>0)MercenarySkills.success(merc,MercenarySkill.CHAIN_LIGHTNING);
        else merc.lightning(merc.lightning().refund(cost));
    }
    static List<Mob> targets(MercenaryEntity merc,ServerPlayer owner,ServerLevel level){
        double range=Config.CHAIN_RADIUS.get(),radiusSquared=range*range;
        var candidates=new ArrayList<Mob>();
        MercenaryBrain.nearby(level,Mob.class,merc.getBoundingBox().inflate(range),Config.CHAIN_CANDIDATE_LIMIT.get(),mob->{
            if(mob.isAlive()&&enemy(merc,owner,mob)&&merc.distanceToSqr(mob)<=radiusSquared
                    &&owner.distanceToSqr(mob)<=radiusSquared
                    &&!merc.lightning().excluded().filter(mob.getUUID()::equals).isPresent())candidates.add(mob);
        });
        candidates.sort(Comparator.<Mob>comparingDouble(merc::distanceToSqr).thenComparing(Mob::getUUID));
        var targets=new ArrayList<Mob>();
        for(var target:candidates){
            if(merc.hasLineOfSight(target))targets.add(target);
            if(targets.size()>=Config.CHAIN_TARGET_LIMIT.get())break;
        }
        return targets;
    }
    private static int burst(MercenaryEntity merc,ServerPlayer owner,ServerLevel level,List<Mob> targets,float damage){
        int hits=0;LivingEntity from=merc;var previous=CASTING.get();CASTING.set(merc);
        try{
            for(var target:targets){
                if(activeOwner(merc)!=owner)break;
                if(!target.isAlive()||!enemy(merc,owner,target))continue;
                float before=target.getHealth()+target.getAbsorptionAmount();
                target.hurtServer(level,merc.damageSources().mobAttack(merc),damage);
                if(target.getHealth()+target.getAbsorptionAmount()<before){
                    merc.lightning(merc.lightning().secondary());hits++;
                    DragoonPassiveEvents.spawnLightningArc(level,from,target);from=target;
                }
            }
        }finally{if(previous==null)CASTING.remove();else CASTING.set(previous);}
        return hits;
    }
}
