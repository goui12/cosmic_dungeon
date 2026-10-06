package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.minecraft.core.component.DataComponents;
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
    /** Native owners also cover unmarked projectiles and per-recipient cloud effects. */
    public static Entity caster(Entity source){
        if(source instanceof Projectile projectile)return projectile.getOwner();
        if(source instanceof AreaEffectCloud cloud)return cloud.getOwner();
        return source;
    }
    public static MercenaryEntity owner(Entity source){
        if(caster(source) instanceof MercenaryEntity mercenary)return mercenary;
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
        if(MercenaryWolves.protectedCompanion(target)&&MercenaryBrain.friendlySource(source)
                &&!helpful(effect,target.isInvertedHealAndHarm()))return false;
        var mercenary=owner(source);
        if(!marked(source)&&mercenary==null)return true;
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
        return entity.potionCasting().tick(entity,candidates);
    }
    public static void produce(MercenaryEntity entity,ServerLevel level){
        if(!MercenaryBrewing.enabled(entity.contract()))return;
        MercenaryBrewing.restock(entity,true,false);
        MercenaryBrewing.restock(entity,false,false);
    }
}
