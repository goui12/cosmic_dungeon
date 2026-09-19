package net.goui.cosmicdungeon.vendor;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class CommerceRecoveryEvents {
    private CommerceRecoveryEvents(){}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)CommerceTransactions.reconcile(p);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)CommerceTransactions.reconcile(p);}
}
