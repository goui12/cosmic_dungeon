package net.goui.cosmicdungeon.playerclass.skill;

import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Strongest-only class applications and durable attribution; native duration/attributes remain authoritative. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class ClassBuffEffects {
    public static final String SOURCE="cosmicdungeon_class_buffs_v1";
    private ClassBuffEffects(){}
    public static boolean family(Holder<MobEffect> effect){
        return effect.is(ModMobEffects.FLURRY_OF_FANGS.getKey())||effect.equals(MobEffects.STRENGTH)
                ||effect.equals(MobEffects.RESISTANCE);
    }
    public static String key(Holder<MobEffect> effect){return BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).toString();}
    public static boolean useful(LivingEntity target,ClassBuff buff){
        if(buff==ClassBuff.BANISH)return target.getActiveEffects().stream().anyMatch(e->e.getEffect().value().getCategory()==MobEffectCategory.HARMFUL);
        var old=target.getEffect(buff.effect());return old==null||buff.amplifier()>old.getAmplifier();
    }
    public static CompoundTag source(LivingEntity target,Holder<MobEffect> effect){
        return target.getPersistentData().getCompoundOrEmpty(SOURCE).getCompoundOrEmpty(key(effect)).copy();
    }
    private static boolean owned(LivingEntity target,Holder<MobEffect> effect){
        var old=target.getEffect(effect);var tag=source(target,effect);
        return old!=null&&tag.getIntOr("version",0)==1&&!tag.getStringOr("skill","").isBlank()
                &&tag.getIntOr("amplifier",-1)==old.getAmplifier();
    }
    @SubscribeEvent public static void applicable(MobEffectEvent.Applicable event){
        var incoming=event.getEffectInstance();var holder=incoming.getEffect();
        if(!family(holder))return;
        var old=event.getEntity().getEffect(holder);
        if(old!=null&&(holder.is(ModMobEffects.FLURRY_OF_FANGS.getKey())||owned(event.getEntity(),holder))
                &&incoming.getAmplifier()<=old.getAmplifier())
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
    /** Called after actual native add/update, including hidden-effect promotion and forceAddEffect. */
    public static void changed(LivingEntity target,MobEffectInstance effect){
        if(target.level().isClientSide()||!family(effect.getEffect()))return;
        var root=target.getPersistentData().getCompoundOrEmpty(SOURCE).copy();
        String key=key(effect.getEffect());var old=root.getCompoundOrEmpty(key).copy();
        boolean previousClassBuff=!old.getStringOr("skill","").isBlank();
        // Do not let an overwritten class rank hide underneath its replacement.
        if(previousClassBuff){
            var actual=target.getEffect(effect.getEffect());
            if(actual!=null)target.getActiveEffectsMap().put(effect.getEffect(),withoutHidden(actual));
        }
        for(String field:new String[]{"caster","name","skill","title","amplifier"})old.remove(field);
        if(root.contains(key)){root.put(key,old);target.getPersistentData().put(SOURCE,root);}
    }
    private static MobEffectInstance withoutHidden(MobEffectInstance effect){
        return new MobEffectInstance(effect.getEffect(),effect.getDuration(),effect.getAmplifier(),
                effect.isAmbient(),effect.isVisible(),effect.showIcon());
    }
    public static boolean apply(LivingEntity caster,LivingEntity target,ClassBuff buff){
        if(target.level().isClientSide()||!useful(target,buff))return false;
        if(buff==ClassBuff.BANISH){
            boolean changed=false;
            for(var effect:java.util.List.copyOf(target.getActiveEffects()))
                if(effect.getEffect().value().getCategory()==MobEffectCategory.HARMFUL)
                    changed=target.removeEffect(effect.getEffect())||changed;
            return changed;
        }
        var effect=new MobEffectInstance(buff.effect(),buff.duration(),buff.amplifier(),false,true,true);
        if(!target.addEffect(effect,caster))return false;
        var actual=target.getEffect(buff.effect());
        if(actual==null||actual.getAmplifier()!=buff.amplifier())return false;
        target.getActiveEffectsMap().put(buff.effect(),withoutHidden(actual));
        var root=target.getPersistentData().getCompoundOrEmpty(SOURCE).copy();
        var tag=root.getCompoundOrEmpty(key(buff.effect())).copy();
        tag.putInt("version",1);tag.putInt("amplifier",buff.amplifier());
        tag.putString("caster",caster.getUUID().toString());tag.putString("name",caster.getName().getString());
        tag.putString("skill",buff.id());tag.putString("title",buff.title());
        root.put(key(buff.effect()),tag);target.getPersistentData().put(SOURCE,root);
        return true;
    }
}
