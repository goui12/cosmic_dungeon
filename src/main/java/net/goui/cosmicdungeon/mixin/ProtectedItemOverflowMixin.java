package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ProtectedItemLifecycle;
import net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ProtectedItemOverflowMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$returnOverflow(ItemStack detached, boolean includeName, CallbackInfoReturnable<ItemEntity> cir) {
        if ((Object)this instanceof ServerPlayer player && ProtectedItemLifecycle.retain(player, detached)) {
            ProtectedItemRecovery.returnDetached(player, detached);
            cir.setReturnValue(null);
        }
    }
}
