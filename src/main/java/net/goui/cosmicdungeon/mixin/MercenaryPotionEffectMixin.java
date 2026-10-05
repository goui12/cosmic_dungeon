package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.mercenary.MercenaryPotionCredit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Copy/upgrade/downgrade follows the native effect that actually deals the damage. */
@Mixin(MobEffectInstance.class)
public abstract class MercenaryPotionEffectMixin implements MercenaryPotionCredit {
    @Unique private Dose cosmicdungeon$dose;
    @Unique private boolean cosmicdungeon$replaced;
    @Override public Dose cosmicdungeon$dose(){return cosmicdungeon$dose;}
    @Override public void cosmicdungeon$dose(Dose dose){cosmicdungeon$dose=dose;}
    @Inject(method="setDetailsFrom",at=@At("TAIL"))
    private void cosmicdungeon$copy(MobEffectInstance other,CallbackInfo ci){
        cosmicdungeon$dose=((MercenaryPotionCredit)(Object)other).cosmicdungeon$dose();
    }
    @Inject(method="update",at=@At("HEAD"))
    private void cosmicdungeon$before(MobEffectInstance other,CallbackInfoReturnable<Boolean> cir){
        var self=(MobEffectInstance)(Object)this;
        cosmicdungeon$replaced=other.getAmplifier()>self.getAmplifier()
                ||other.getAmplifier()==self.getAmplifier()&&!self.isInfiniteDuration()
                &&(other.isInfiniteDuration()||other.getDuration()>self.getDuration());
    }
    @Inject(method="update",at=@At("RETURN"))
    private void cosmicdungeon$updated(MobEffectInstance other,CallbackInfoReturnable<Boolean> cir){
        if(cosmicdungeon$replaced)cosmicdungeon$dose=((MercenaryPotionCredit)(Object)other).cosmicdungeon$dose();
    }
    @Redirect(method="tickServer",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean cosmicdungeon$tick(MobEffect effect,ServerLevel level,LivingEntity target,int amplifier){
        return cosmicdungeon$dose==null?effect.applyEffectTick(level,target,amplifier)
                :cosmicdungeon$dose.tick(level,target,()->effect.applyEffectTick(level,target,amplifier));
    }
}
