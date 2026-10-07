package net.goui.cosmicdungeon.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import net.neoforged.neoforge.event.EventHooks;

/** Stamp positioned native success before ANY respawn listener can persist the replacement. */
@Mixin(PlayerList.class)
public abstract class ResurrectionReceiptMixin {
    @Redirect(method="respawn",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/event/EventHooks;firePlayerRespawnEvent(Lnet/minecraft/server/level/ServerPlayer;Z)V"))
    private void cosmicdungeon$receiptBeforeListeners(ServerPlayer replacement,boolean conqueredEnd){
        net.goui.cosmicdungeon.mercenary.MercenaryResurrection.beforeRespawnListeners(replacement);
        EventHooks.firePlayerRespawnEvent(replacement,conqueredEnd);
    }
}
