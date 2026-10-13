package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.custom.D1FireworkItem;
import net.goui.cosmicdungeon.playerclass.skill.ClassSkills;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileWeaponItem.class)
public abstract class SkillProjectileWeaponMixin {
    @Inject(method="createProjectile",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$bowRocket(Level level,LivingEntity shooter,ItemStack weapon,ItemStack ammo,
                                         boolean critical,CallbackInfoReturnable<Projectile> ci){
        if(weapon.getItem() instanceof BowItem
                &&(ammo.is(Items.FIREWORK_ROCKET)||D1FireworkItem.isCustomRocket(ammo))){
            // The native constructor copies every authored component. Existing detonation policy
            // owns damage/effects; do not substitute an arrow or synthesize a rocket payload.
            var rocket=new FireworkRocketEntity(level,ammo,shooter,
                    shooter.getX(),shooter.getEyeY()-.15F,shooter.getZ(),true);
            ClassSkills.capture(rocket,shooter,weapon);
            ci.setReturnValue(rocket);
        }
    }
    @Inject(method="createProjectile",at=@At("RETURN"))
    private void cosmicdungeon$skillShot(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo,
                                         boolean critical, CallbackInfoReturnable<Projectile> ci) {
        ClassSkills.capture(ci.getReturnValue(),shooter,weapon);
    }
}
