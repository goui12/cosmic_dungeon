package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ProtectedItemLifecycle;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ProtectedItemCloneMixin {
    @Inject(method = "restoreFrom", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/event/EventHooks;onPlayerClone(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/player/Player;Z)V"))
    private void cosmicdungeon$restoreProtected(ServerPlayer original, boolean keepEverything, CallbackInfo ci) {
        ProtectedItemLifecycle.cloneRetained((ServerPlayer)(Object)this, original, keepEverything);
    }
}
