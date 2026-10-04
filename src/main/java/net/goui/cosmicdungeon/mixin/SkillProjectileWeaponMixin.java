package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.skill.ClassSkills;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileWeaponItem.class)
public abstract class SkillProjectileWeaponMixin {
    @Inject(method="createProjectile",at=@At("RETURN"))
    private void cosmicdungeon$skillShot(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo,
                                         boolean critical, CallbackInfoReturnable<Projectile> ci) {
        ClassSkills.capture(ci.getReturnValue(),shooter,weapon);
    }
}
