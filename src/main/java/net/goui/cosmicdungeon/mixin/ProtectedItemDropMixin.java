package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.item.identity.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ProtectedItemDropMixin {
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$beforeRemoval(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        var player = (ServerPlayer)(Object)this;
        if (!AccessPolicy.isDeveloper(player)
                && !ItemMovementPolicy.mayDrop(ItemMovementRules.flags(player.getInventory().getSelectedItem()).noDrop())) {
            ItemMovementGuard.reject(player); cir.setReturnValue(false);
        }
    }
}
