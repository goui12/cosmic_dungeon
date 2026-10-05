package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ThrownSplashPotion.class)
public abstract class SkillSplashPotionMixin {
    @Redirect(method="onHitAsPotion",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffect;applyInstantenousEffect(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V"))
    private void cosmicdungeon$instant(MobEffect effect,ServerLevel level,Entity source,Entity owner,LivingEntity target,int amplifier,double proximity) {
        if(!net.goui.cosmicdungeon.mercenary.MercenaryPotions.allows((Entity)(Object)this,target,effect))return;
        net.goui.cosmicdungeon.mercenary.MercenarySkillEffects.instant((Entity)(Object)this,target,effect,
                ()->SkillPotions.instant(ClassSkills.projectile((Entity)(Object)this),effect,level,source,owner,target,amplifier,proximity));
    }
    @Redirect(method="onHitAsPotion",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean cosmicdungeon$timed(LivingEntity target,MobEffectInstance effect,Entity source) {
        if(!net.goui.cosmicdungeon.mercenary.MercenaryPotions.allows((Entity)(Object)this,target,effect.getEffect().value()))return false;
        return net.goui.cosmicdungeon.mercenary.MercenarySkillEffects.timed((Entity)(Object)this,target,effect,
                ()->SkillPotions.timed(ClassSkills.projectile((Entity)(Object)this),target,effect,source));
    }
}
