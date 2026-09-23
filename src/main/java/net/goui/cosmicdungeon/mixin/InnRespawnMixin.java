package net.goui.cosmicdungeon.mixin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class InnRespawnMixin {
    @Inject(method="findRespawnPositionAndUseSpawnBlock",at=@At("RETURN"),cancellable=true)
    private void cosmicdungeon$fallback(boolean charge,TeleportTransition.PostTeleportTransition after,CallbackInfoReturnable<TeleportTransition> ci){
        ci.setReturnValue(net.goui.cosmicdungeon.npc.inn.InnService.fallback((ServerPlayer)(Object)this,ci.getReturnValue(),after));
    }
}
