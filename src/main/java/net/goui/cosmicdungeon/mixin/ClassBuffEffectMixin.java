package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.skill.ClassBuffEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ClassBuffEffectMixin {
    @Inject(method="onEffectAdded",at=@At("TAIL"))
    private void cosmicdungeon$buffAdded(MobEffectInstance effect,Entity source,CallbackInfo ci){
        ClassBuffEffects.changed((LivingEntity)(Object)this,effect);
    }
    @Inject(method="onEffectUpdated",at=@At("TAIL"))
    private void cosmicdungeon$buffUpdated(MobEffectInstance effect,boolean forced,Entity source,CallbackInfo ci){
        if(forced)ClassBuffEffects.changed((LivingEntity)(Object)this,effect);
    }
}
