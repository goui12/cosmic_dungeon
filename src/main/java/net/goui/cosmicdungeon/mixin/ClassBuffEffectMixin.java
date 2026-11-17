package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.skill.ClassBuffEffects;
import net.goui.cosmicdungeon.npc.tamsin.PartyEffectSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class ClassBuffEffectMixin {
    @Unique private final java.util.ArrayDeque<PartyEffectSource.Before> cosmicdungeon$effectBefore=new java.util.ArrayDeque<>();
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"))
    private void cosmicdungeon$beforeEffect(MobEffectInstance incoming,Entity source,CallbackInfoReturnable<Boolean> ci){
        var old=((LivingEntity)(Object)this).getEffect(incoming.getEffect());
        cosmicdungeon$effectBefore.push(new PartyEffectSource.Before(ClassBuffEffects.key(incoming.getEffect()),
                old==null?-1:old.getAmplifier(),old==null?0:old.getDuration()));
    }
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("RETURN"))
    private void cosmicdungeon$afterEffect(MobEffectInstance incoming,Entity source,CallbackInfoReturnable<Boolean> ci){
        cosmicdungeon$effectBefore.pop();
    }
    @Inject(method="onEffectAdded",at=@At("TAIL"))
    private void cosmicdungeon$buffAdded(MobEffectInstance effect,Entity source,CallbackInfo ci){
        ClassBuffEffects.changed((LivingEntity)(Object)this,effect);
        PartyEffectSource.changed((LivingEntity)(Object)this,effect,source,true);
    }
    @Inject(method="onEffectUpdated",at=@At("TAIL"))
    private void cosmicdungeon$buffUpdated(MobEffectInstance effect,boolean forced,Entity source,CallbackInfo ci){
        if(!forced)return;
        var before=cosmicdungeon$effectBefore.peek();
        // Native cosmetic updates may come from a rejected weaker potion; duration is compared
        // directly, so entity unload/reload and paused effect clocks cannot falsify the winner.
        if(before!=null&&before.id().equals(ClassBuffEffects.key(effect.getEffect()))
                &&before.amplifier()==effect.getAmplifier()&&before.duration()==effect.getDuration())return;
        ClassBuffEffects.changed((LivingEntity)(Object)this,effect);
        PartyEffectSource.changed((LivingEntity)(Object)this,effect,source,true);
    }
}
