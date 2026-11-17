package net.goui.cosmicdungeon.npc.tamsin;

import net.goui.cosmicdungeon.playerclass.skill.ClassBuffEffects;
import net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;

/** Additive provenance only. Missing historical caster data stays Unknown; no guessed owner. */
public final class PartyEffectSource {
    public static final String KEY="cosmicdungeon_effect_sources_v1";
    private PartyEffectSource(){}
    public record Before(String id,int amplifier,int duration){}
    public static Entity caster(Entity source){
        if(source instanceof Projectile p)return p.getOwner();
        if(source instanceof AreaEffectCloud c)return c.getOwner();
        return source;
    }
    /** Invoked only after the native winning effect changed, never on rejected or hidden weaker attempts. */
    public static void changed(LivingEntity target,MobEffectInstance effect,Entity source,boolean added){
        if(target.level().isClientSide())return;
        String key=ClassBuffEffects.key(effect.getEffect());
        var root=target.getPersistentData().getCompoundOrEmpty(KEY).copy();
        var tag=root.getCompoundOrEmpty(key).copy();
        long end=effect.isInfiniteDuration()?-1:target.level().getServer().overworld().getGameTime()+effect.getDuration();
        if(!added&&tag.getIntOr("amplifier",-1)==effect.getAmplifier()&&tag.getLongOr("end",Long.MIN_VALUE)==end)return;
        if(!root.contains(key)&&root.keySet().size()>=64)return;
        tag.putInt("version",1);tag.putInt("amplifier",effect.getAmplifier());tag.putLong("end",end);
        var caster=caster(source);
        if(caster instanceof LivingEntity){
            tag.putString("caster",caster.getUUID().toString());tag.putString("name",bounded(caster.getName().getString(),64));
        }else{tag.remove("caster");tag.remove("name");}
        root.put(key,tag);target.getPersistentData().put(KEY,root);
        if(VitalityPotions.isVitality(effect)){
            var old=target.getPersistentData().getCompoundOrEmpty(VitalityPotions.SOURCE).copy();
            old.remove("caster");old.remove("name");old.remove("amplifier");
            target.getPersistentData().put(VitalityPotions.SOURCE,old);
        }
    }
    public static String bounded(String value,int limit){return value.length()>limit?value.substring(0,limit):value;}
    public static String name(LivingEntity target,MobEffectInstance effect){
        String key=ClassBuffEffects.key(effect.getEffect());
        var root=target.getPersistentData().getCompoundOrEmpty(KEY);
        CompoundTag tag=root.getCompoundOrEmpty(key);
        if(!root.contains(key))tag=VitalityPotions.isVitality(effect)
                ?target.getPersistentData().getCompoundOrEmpty(VitalityPotions.SOURCE):ClassBuffEffects.source(target,effect.getEffect());
        if(tag.getIntOr("version",0)!=1||tag.getIntOr("amplifier",-1)!=effect.getAmplifier())return "Unknown";
        String name=tag.getStringOr("name","");
        return name.isBlank()?"Unknown":bounded(name,64);
    }
}
