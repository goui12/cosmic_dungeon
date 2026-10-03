package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Includes thrown tridents, whose playerTouch delegates to AbstractArrow. */
@Mixin(AbstractArrow.class)
public abstract class OwnedProjectilePickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$ownerOnly(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer
                && !ClassItemOwnership.mayAcquire(serverPlayer,
                        ((AbstractArrow)(Object)this).getPickupItemStackOrigin())) {
            ci.cancel();
        }
    }
}
