package net.goui.cosmicdungeon.playerclass.judicator;

import java.util.ArrayList;
import net.goui.cosmicdungeon.mercenary.MercenaryBrain;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.AABB;

/** Shared player/mercenary Smite damage; charge and proficiency stay with the authoritative caller. */
public final class SmiteCombat {
    public record Rank(String id,String name,int radius,float damage) {}
    private static final Rank[] RANKS={
        new Rank("gravehammer","Gravehammer",3,2.5f),
        new Rank("bonegrinder","Bonegrinder",4,2.5f),
        new Rank("deathpurge","Deathpurge",5,5),
        new Rank("last_rites","Last Rites",6,5)
    };
    private SmiteCombat(){}
    public static Rank rank(int level){return RANKS[Math.clamp(level,1,4)-1];}
    public static int cast(LivingEntity caster,int skillLevel){
        if(!(caster.level() instanceof ServerLevel level)||!ChargedClassSkills.active(caster,SignatureSkill.SMITE))return 0;
        var rank=rank(skillLevel);var center=caster.position();double radiusSquared=rank.radius()*rank.radius();
        var targets=new ArrayList<Mob>();
        MercenaryBrain.nearby(level,Mob.class,new AABB(center,center).inflate(rank.radius()),256,target->{
            if(target.isAlive()&&target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)
                    &&target.position().distanceToSqr(center)<=radiusSquared&&ChargedClassSkills.enemy(caster,target))targets.add(target);
        });
        return SecondarySkillDamage.run(()->{
            int damaged=0;
            for(var target:targets){
                if(!ChargedClassSkills.active(caster,SignatureSkill.SMITE))break;
                if(!target.isAlive()||target.position().distanceToSqr(center)>radiusSquared
                        ||!target.getType().is(EntityTypeTags.SENSITIVE_TO_SMITE)||!ChargedClassSkills.enemy(caster,target))continue;
                float before=target.getHealth()+target.getAbsorptionAmount();
                target.hurtServer(level,caster.damageSources().indirectMagic(caster,caster),rank.damage());
                if(target.getHealth()+target.getAbsorptionAmount()<before)damaged++;
            }
            return damaged;
        });
    }
}
