package net.goui.cosmicdungeon.playerclass.theurgist;

import java.util.ArrayList;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.*;

/** The approved10-block area buff, retaining native projectile ownership and useful-cast credit. */
public final class VitalitySplash {
    public static final double RADIUS=10;
    private VitalitySplash(){}
    public static boolean ally(Entity caster,LivingEntity target){
        if(!target.isAlive()||caster==null||caster.level()!=target.level())return false;
        if(caster instanceof ServerPlayer p)return ClassSkills.ally(target,p);
        return caster instanceof MercenaryEntity merc&&MercenaryBrain.ally(merc,target);
    }
    public static boolean apply(ThrownSplashPotion projectile,ServerLevel level,ItemStack stack,HitResult hit){
        var effects=new ArrayList<MobEffectInstance>();
        stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects().forEach(effects::add);
        if(effects.size()!=1||!VitalityPotions.isVitality(effects.getFirst()))return false;
        var caster=projectile.getOwner();var origin=hit.getLocation();var effect=effects.getFirst();
        var bounds=new AABB(origin.x-RADIUS,origin.y-RADIUS,origin.z-RADIUS,origin.x+RADIUS,origin.y+RADIUS,origin.z+RADIUS);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,bounds,t->ally(caster,t)&&t.isAffectedByPotions())){
            if(target.distanceToSqr(origin)>RADIUS*RADIUS)continue;
            if(!TheurgistPotionProtection.allows(projectile,target,effect.getEffect().value())
                    ||!MercenaryPotions.allows(projectile,target,effect.getEffect().value()))continue;
            MercenarySkillEffects.timed(projectile,target,effect,
                    ()->SkillPotions.timed(ClassSkills.projectile(projectile),target,effect,caster));
        }
        return true;
    }
}
