package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.skill.ChargedClassSkills;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ClassSkillDeathMixin {
    @Shadow protected boolean dead;
    @Inject(method="die",at=@At("TAIL"))
    private void cosmicdungeon$chargedKill(DamageSource source,CallbackInfo ci){
        if(dead)ChargedClassSkills.killed((LivingEntity)(Object)this,source);
    }
}
