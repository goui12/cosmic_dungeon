package net.goui.cosmicdungeon.mercenary;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/** One prepared cast, revalidated at release. Existing ten-tick AI pass and candidate list only. */
final class MercenaryPotionCasting {
    private UUID target;
    private int slot=-1;
    private ItemStack prepared=ItemStack.EMPTY;
    private long readyTick;
    private MercenarySkill skill;
    void cancel(){target=null;slot=-1;prepared=ItemStack.EMPTY;skill=null;}
    static boolean refresh(MobEffectInstance old,MobEffectInstance incoming){
        return old==null||old.getAmplifier()<incoming.getAmplifier()
                ||old.getAmplifier()==incoming.getAmplifier()&&!old.isInfiniteDuration()&&old.getDuration()<40;
    }
    private static int score(MercenaryEntity entity,LivingEntity target,ItemStack stack,boolean combat){
        if(!MercenaryBrewing.roleSplash(stack,entity.contract())||!target.isAlive()||!target.isAffectedByPotions()
                ||entity.distanceToSqr(target)>64||!entity.getSensing().hasLineOfSight(target))return -1;
        int score=-1;
        for(var effect:stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).getAllEffects()){
            if(!MercenaryPotions.roleAllows(entity.contract(),effect.getEffect().value(),target.isInvertedHealAndHarm())
                    ||!MercenaryPotions.allows(entity,target,effect.getEffect().value())||!target.canBeAffected(effect))continue;
            boolean helpful=MercenaryPotions.helpful(effect.getEffect().value(),target.isInvertedHealAndHarm());
            if(helpful){
                if(!combat&&!(target instanceof ServerPlayer))continue;
                boolean healing=effect.getEffect().value().isInstantenous()||effect.getEffect().equals(MobEffects.REGENERATION);
                if(healing&&target.getHealth()>=target.getMaxHealth())continue;
                if(!healing&&!refresh(target.getEffect(effect.getEffect()),effect))continue;
                score=Math.max(score,healing?300+(int)(100*(1-target.getHealth()/target.getMaxHealth()))
                        :target instanceof ServerPlayer?160:100);
            }else{
                if(!combat)continue;
                if(!effect.getEffect().value().isInstantenous()
                        &&!refresh(target.getEffect(effect.getEffect()),effect))continue;
                score=Math.max(score,220);
            }
        }
        return score;
    }
    private static MercenarySkill skill(MercenaryEntity entity,LivingEntity target){
        return MercenaryBrain.ally(entity,target)?MercenarySkill.POSITIVE_POTIONS:MercenarySkill.NEGATIVE_POTIONS;
    }
    private static void flash(MercenaryEntity entity,ServerLevel level,boolean release){
        level.sendParticles(release?ParticleTypes.WITCH:ParticleTypes.ENCHANT,entity.getX(),entity.getY()+1,entity.getZ(),
                8,.35,.4,.35,.02);
    }
    boolean tick(MercenaryEntity entity,List<LivingEntity> candidates){
        if(!MercenaryBrewing.enabled(entity.contract())||!(entity.level() instanceof ServerLevel level)
                ||MercenaryBrain.hirer(entity)==null){cancel();return false;}
        boolean combat=candidates.stream().anyMatch(t->MercenaryBrain.enemy(entity,t));
        if(target!=null){
            if(level.getGameTime()<readyTick)return false;
            var victim=level.getEntity(target);
            if(!(victim instanceof LivingEntity living)||slot<0||slot>=entity.supplies().size()
                    ||!ItemStack.isSameItemSameComponents(prepared,entity.supplies().get(slot))
                    ||entity.supplies().get(slot).isEmpty()||score(entity,living,prepared,combat)<0
                    ||skill(entity,living)!=skill){cancel();return false;}
            var shot=new ThrownSplashPotion(level,entity,prepared.copyWithCount(1));
            MercenaryPotions.mark(shot,entity);
            double x=living.getX()-shot.getX(),z=living.getZ()-shot.getZ();
            // Aim at feet; per-recipient effect guards protect every ally/enemy in the splash.
            if(living==entity)shot.shoot(0,-1,0,.75f,0);
            else shot.shoot(x,living.getY()+.1-shot.getY()+Math.sqrt(x*x+z*z)*.2,z,.75f,0);
            if(!level.addFreshEntity(shot)){cancel();return false;}
            entity.supplies().get(slot).shrink(1);
            int cadence=MercenaryPotionBalance.cadence(MercenaryConfig.potionTicks(MercenaryPotions.key(prepared)),
                    MercenarySkills.level(entity,skill),40);
            entity.timers(entity.timers().used(MercenaryPotionBalance.throwKey(skill),cadence)
                    .used(MercenaryPotions.key(prepared),cadence).used(MercenaryPotionBalance.GLOBAL,MercenaryPotionBalance.GLOBAL_GAP));
            if(combat)entity.regeneration().combat();
            flash(entity,level,true);cancel();return true;
        }
        if(!entity.timers().ready(MercenaryPotionBalance.GLOBAL))return false;
        int best=-1;
        for(int i=0;i<entity.supplies().size();i++){
            var stack=entity.supplies().get(i);
            if(!MercenaryBrewing.effectSplash(stack)||!entity.timers().ready(MercenaryPotions.key(stack)))continue;
            for(var candidate:candidates){
                var category=skill(entity,candidate);
                if(!category.supports(entity.contract())||!entity.timers().ready(MercenaryPotionBalance.throwKey(category)))continue;
                int score=score(entity,candidate,stack,combat);
                if(score>best){best=score;slot=i;target=candidate.getUUID();prepared=stack.copyWithCount(1);skill=category;}
            }
        }
        if(best<0){cancel();return false;}
        readyTick=level.getGameTime()+MercenaryPotionBalance.PREPARE;
        flash(entity,level,false);return false;
    }
}
