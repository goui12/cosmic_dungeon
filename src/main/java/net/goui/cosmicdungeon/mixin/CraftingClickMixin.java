package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.crafting.CraftingPolicy;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CraftingClickMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleContainerClick",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"),cancellable=true)
    private void cosmicdungeon$freshResult(ServerboundContainerClickPacket packet,CallbackInfo ci) {
        if(CraftingPolicy.refresh(player.containerMenu,player)) {
            // Stale client prediction must not overwrite the replacement/cleared result.
            player.containerMenu.resumeRemoteUpdates();
            player.containerMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
