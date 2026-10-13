package net.goui.cosmicdungeon.mixin;

import java.util.function.Predicate;
import net.goui.cosmicdungeon.item.custom.D1FireworkItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retain native rockets/effects while supporting legal inventory stock and revalidating loaded ammunition. */
@Mixin(CrossbowItem.class)
public abstract class D1CrossbowMixin {
    @Inject(method = "getSupportedHeldProjectiles", at = @At("RETURN"), cancellable = true)
    private void cosmicdungeon$heldRockets(CallbackInfoReturnable<Predicate<ItemStack>> ci) {
        ci.setReturnValue(ci.getReturnValue().or(D1FireworkItem::isCustomRocket));
    }

    @Inject(method="getAllSupportedProjectiles",at=@At("RETURN"),cancellable=true)
    private void cosmicdungeon$inventoryRockets(CallbackInfoReturnable<Predicate<ItemStack>> ci){
        ci.setReturnValue(ci.getReturnValue().or(ammo->ammo.is(net.minecraft.world.item.Items.FIREWORK_ROCKET)
                ||D1FireworkItem.isCustomRocket(ammo)));
    }
    @Inject(method="performShooting",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$loadedPermission(Level level,LivingEntity shooter,net.minecraft.world.InteractionHand hand,
            ItemStack weapon,float velocity,float inaccuracy,LivingEntity target,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
        if(shooter instanceof net.minecraft.server.level.ServerPlayer player
                &&!net.goui.cosmicdungeon.ammunition.AmmoSelectionService.loadedAllowed(player,weapon))ci.cancel();
    }

    @Inject(method = "getShootingPower", at = @At("HEAD"), cancellable = true)
    private static void cosmicdungeon$rocketSpeed(ChargedProjectiles projectiles, CallbackInfoReturnable<Float> ci) {
        if (D1FireworkItem.containsCustomRocket(projectiles)) ci.setReturnValue(1.6F);
    }

    @Inject(method = "createProjectile", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$rocketEntity(Level level, LivingEntity shooter, ItemStack weapon,
                                           ItemStack ammo, boolean critical, CallbackInfoReturnable<Projectile> ci) {
        if (D1FireworkItem.isCustomRocket(ammo)) {
            var rocket = new FireworkRocketEntity(level, ammo, shooter,
                    shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ(), true);
            net.goui.cosmicdungeon.playerclass.skill.ClassSkills.capture(rocket, shooter, weapon);
            ci.setReturnValue(rocket);
        }
    }

    @Inject(method = "createProjectile", at = @At("RETURN"))
    private void cosmicdungeon$skillShot(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo,
                                        boolean critical, CallbackInfoReturnable<Projectile> ci) {
        net.goui.cosmicdungeon.playerclass.skill.ClassSkills.capture(ci.getReturnValue(), shooter, weapon);
    }

    @Inject(method = "getDurabilityUse", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$rocketDurability(ItemStack ammo, CallbackInfoReturnable<Integer> ci) {
        if (D1FireworkItem.isCustomRocket(ammo)) ci.setReturnValue(3);
    }

    // Native charging, consumption, multishot and enchantments remain unchanged.
    // Native CI covers inventory/hand loading, Multishot, preserved charged loads and authority changes.
    // Licensed controls/visuals/world acceptance remains TESTING in FIXSET_F5_20261013.md.
}
