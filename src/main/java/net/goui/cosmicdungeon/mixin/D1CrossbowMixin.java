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

/** Extend the native rocket path only for our two dedicated D1 rocket items. */
@Mixin(CrossbowItem.class)
public abstract class D1CrossbowMixin {
    @Inject(method = "getSupportedHeldProjectiles", at = @At("RETURN"), cancellable = true)
    private void cosmicdungeon$heldRockets(CallbackInfoReturnable<Predicate<ItemStack>> ci) {
        ci.setReturnValue(ci.getReturnValue().or(D1FireworkItem::isCustomRocket));
    }

    @Inject(method = "getShootingPower", at = @At("HEAD"), cancellable = true)
    private static void cosmicdungeon$rocketSpeed(ChargedProjectiles projectiles, CallbackInfoReturnable<Float> ci) {
        if (D1FireworkItem.containsCustomRocket(projectiles)) ci.setReturnValue(1.6F);
    }

    @Inject(method = "createProjectile", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$rocketEntity(Level level, LivingEntity shooter, ItemStack weapon,
                                           ItemStack ammo, boolean critical, CallbackInfoReturnable<Projectile> ci) {
        if (D1FireworkItem.isCustomRocket(ammo)) {
            ci.setReturnValue(new FireworkRocketEntity(level, ammo, shooter,
                    shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ(), true));
        }
    }

    @Inject(method = "getDurabilityUse", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$rocketDurability(ItemStack ammo, CallbackInfoReturnable<Integer> ci) {
        if (D1FireworkItem.isCustomRocket(ammo)) ci.setReturnValue(3);
    }

    // Keep native held-ammo selection (rockets in offhand), consumption, multishot and enchantments.
    // TODO(D1 native TEST): load each rocket, fire multishot, reload charged crossbows and verify
    // 1.6 launch speed/three durability, effects once, shields/obstruction and denied class/run.
}
