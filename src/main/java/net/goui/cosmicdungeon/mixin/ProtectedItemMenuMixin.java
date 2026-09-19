package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ItemMovementGuard;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after vanilla thread, menu and slot checks, before the click removes any item.
 * Rejecting here also avoids accepting the client's predicted changed-slot hashes. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ProtectedItemMenuMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "handleContainerClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"),
            cancellable = true)
    private void cosmicdungeon$protect(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (ItemMovementGuard.denyClick(player, packet.slotNum(), packet.buttonNum(), packet.clickType())) {
            ItemMovementGuard.reject(player); ci.cancel();
        }
    }
}
