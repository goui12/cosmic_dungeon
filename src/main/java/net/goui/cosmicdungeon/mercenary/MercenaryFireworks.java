package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import java.util.function.Predicate;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.playerclass.d1.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;

/** Reuses the existing nearby snapshot; no material acquisition or additional world scan. */
public final class MercenaryFireworks {
    private MercenaryFireworks(){}
    public static boolean enabled(MercenaryEntity entity){
        return entity.contract()!=null&&"pyroclast".equals(entity.contract().classId());
    }
    static void tick(MercenaryEntity entity,int ticks){
        if(enabled(entity))entity.fireworks(entity.fireworks().advance(ticks,
                MercenarySkills.level(entity,MercenarySkill.FIREWORKS)));
    }
    // At most 48 existing candidates and eight possible aim points: <=384 distance comparisons.
    static LivingEntity choose(List<LivingEntity> candidates,Predicate<LivingEntity> eligible,double radius){
        var enemies=candidates.stream().limit(50).filter(eligible).limit(48).toList();
        LivingEntity best=null;int score=-1;
        for(int i=0;i<Math.min(8,enemies.size());i++){
            var center=enemies.get(i);int count=0;
            for(var target:enemies)if(center.distanceToSqr(target)<radius*radius)count++;
            if(count>score){score=count;best=center;}
        }
        return best;
    }
    static LivingEntity target(MercenaryEntity entity,List<LivingEntity> candidates){
        if(!enabled(entity)||entity.fireworks().count()==0)return null;
        var owner=MercenaryBrain.hirer(entity);if(owner==null)return null;
        return choose(candidates,t->MercenaryBrain.enemy(entity,t)&&owner.distanceToSqr(t)<=64&&entity.distanceToSqr(t)<=64
                &&entity.getSensing().hasLineOfSight(t),D1AbilityConfig.ROCKET_RADIUS.get());
    }
    static boolean launch(MercenaryEntity entity,LivingEntity target,ServerLevel level){
        if(!enabled(entity)||entity.fireworks().count()==0||!MercenaryBrain.enemy(entity,target)
                ||entity.distanceToSqr(target)>64||!entity.getSensing().hasLineOfSight(target))return false;
        var ammo=new ItemStack(ModItems.CINDERBITE.get());
        var rocket=new FireworkRocketEntity(level,ammo,entity,entity.getX(),entity.getEyeY()-.15,entity.getZ(),true);
        MercenaryPotions.mark(rocket,entity);
        D1RocketAbilities.capture(rocket);
        rocket.shoot(target.getX()-rocket.getX(),target.getY(.5)-rocket.getY(),target.getZ()-rocket.getZ(),1.6f,0);
        if(!level.addFreshEntity(rocket))return false;
        entity.fireworks(entity.fireworks().spend());
        entity.swing(InteractionHand.MAIN_HAND);
        return true;
    }
}
