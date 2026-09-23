package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ItemLifecyclePolicy;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class OwnedItemPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$ownerOnly(Player player, CallbackInfo ci) {
        // Before pickup listeners: a later TRUE event cannot override the item's owner.
        if (!ItemLifecyclePolicy.mayCollect(((ItemEntity)(Object)this).getTarget(), player.getUUID())) ci.cancel();
    }
}
