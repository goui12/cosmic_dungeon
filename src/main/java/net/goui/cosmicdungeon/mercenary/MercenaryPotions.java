package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.playerclass.bogatyr.CompanionAllies;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;

/** Native thrown potion/cloud physics; filter each actual recipient, including mixed effects. */
public final class MercenaryPotions {
    public static final String SOURCE="cosmicdungeon_mercenary_source_v1";
    private MercenaryPotions(){}
    public static void mark(Entity projectile,MercenaryEntity mercenary){
        projectile.getPersistentData().putString(SOURCE,mercenary.getUUID().toString());
    }
    public static void inherit(Entity cloud,Entity projectile){
        if(projectile.getPersistentData().contains(SOURCE))
            cloud.getPersistentData().putString(SOURCE,projectile.getPersistentData().getStringOr(SOURCE,""));
    }
    public static boolean marked(Entity source){return source!=null&&source.getPersistentData().contains(SOURCE);}
    public static MercenaryEntity owner(Entity source){
        if(source instanceof MercenaryEntity mercenary)return mercenary;
        if(source==null||!(source.level() instanceof ServerLevel level)||!marked(source))return null;
        try{return level.getEntity(UUID.fromString(source.getPersistentData().getStringOr(SOURCE,"")))
                instanceof MercenaryEntity mercenary?mercenary:null;}
        catch(IllegalArgumentException invalid){return null;}
    }
    public static boolean helpful(MobEffect effect,boolean inverted){
        if(effect==MobEffects.INSTANT_HEALTH.value())return !inverted;
        if(effect==MobEffects.INSTANT_DAMAGE.value())return inverted;
        return effect.getCategory()==MobEffectCategory.BENEFICIAL;
    }
    public static boolean permits(boolean helpful,boolean ally,boolean enemy){return helpful?ally:enemy;}
    public static boolean allows(Entity source,LivingEntity target,MobEffect effect){
        if(!marked(source)&&!(source instanceof MercenaryEntity))return true;
        var mercenary=owner(source);
        if(mercenary==null||MercenaryBrain.hirer(mercenary)==null)return false;
        return permits(helpful(effect,target.isInvertedHealAndHarm()),MercenaryBrain.ally(mercenary,target),
                MercenaryBrain.enemy(mercenary,target));
    }
    public static String key(ItemStack stack){
        var contents=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        return contents.potion().flatMap(p->p.unwrapKey().map(k->k.location().toString())).orElse("custom");
    }
    public static boolean useful(MercenaryEntity entity,LivingEntity target,MobEffectInstance incoming){
        var effect=incoming.getEffect().value();
        if(!allows(entity,target,effect))return false;
        if(helpful(effect,target.isInvertedHealAndHarm())
                &&(effect.isInstantenous()||incoming.getEffect().equals(MobEffects.REGENERATION)))
            return target.getHealth()<target.getMaxHealth();
        var old=target.getEffect(incoming.getEffect());
        return old==null||old.getAmplifier()<incoming.getAmplifier()||old.getDuration()<40;
    }
    public static boolean use(MercenaryEntity entity,List<LivingEntity> candidates){
        for(int i=0;i<entity.supplies().size();i++){
            var stack=entity.supplies().get(i);
            if(!(stack.getItem() instanceof PotionItem)||!entity.timers().ready(key(stack)))continue;
            var contents=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
            for(var target:candidates){
                if(entity.distanceToSqr(target)>64||!entity.getSensing().hasLineOfSight(target))continue;
                boolean throwable=stack.is(Items.SPLASH_POTION)||stack.is(Items.LINGERING_POTION);
                if(!throwable&&target!=entity)continue;
                boolean useful=false;
                for(var effect:contents.getAllEffects())if(useful(entity,target,effect)){useful=true;break;}
                if(!useful)continue;
                String key=key(stack);var one=stack.copyWithCount(1);
                if(throwable){
                    AbstractThrownPotion shot=stack.is(Items.LINGERING_POTION)
                            ?new ThrownLingeringPotion(entity.level(),entity,one):new ThrownSplashPotion(entity.level(),entity,one);
                    mark(shot,entity);
                    double x=target.getX()-entity.getX(),z=target.getZ()-entity.getZ();
                    if(target==entity)shot.shoot(0,-1,0,.75f,0);
                    else shot.shoot(x,target.getEyeY()-1.1-entity.getY()+Math.sqrt(x*x+z*z)*.2,z,.75f,1);
                    if(!entity.level().addFreshEntity(shot))continue;
                    stack.shrink(1);
                }else{
                    var plan=MercenaryInventory.copy(entity.supplies());plan.get(i).shrink(1);
                    if(!MercenaryInventory.insert(plan,new ItemStack(Items.GLASS_BOTTLE)))continue;
                    MercenaryInventory.commit(entity.supplies(),plan);
                    contents.forEachEffect(effect->{
                        if(!allows(entity,entity,effect.getEffect().value()))return;
                        if(effect.getEffect().value().isInstantenous())
                            effect.getEffect().value().applyInstantenousEffect((ServerLevel)entity.level(),entity,entity,entity,effect.getAmplifier(),1);
                        else entity.addEffect(effect,entity);
                    },one.getOrDefault(DataComponents.POTION_DURATION_SCALE,1f));
                }
                entity.timers(entity.timers().used(key,MercenaryConfig.potionTicks(key)));return true;
            }
        }
        return false;
    }
    public static void produce(MercenaryEntity entity,ServerLevel level){
        var timers=entity.timers();
        if(timers.brew()==0){
            var brewed=MercenaryInventory.brew(entity.supplies(),level.potionBrewing());
            if(brewed!=null)MercenaryInventory.commit(entity.supplies(),brewed);
            timers=new MercenaryTimers(MercenaryConfig.BREW_TICKS.get(),timers.fallback(),timers.potions());
        }
        if(timers.fallback()==0){
            var healing=PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING);
            MercenaryInventory.insert(entity.supplies(),healing);
            timers=new MercenaryTimers(timers.brew(),MercenaryConfig.FALLBACK_TICKS.get(),timers.potions());
        }
        entity.timers(timers);
    }
}
