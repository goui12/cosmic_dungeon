package net.goui.cosmicdungeon.trade;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TradeEvents {
    private TradeEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            TradeSessionData.handleLogout(sp);
        }
    }

    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&TradeSessionData.get(p)!=null)TradeSessionData.cancel(p,"Player died");
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&TradeSessionData.get(p)!=null)TradeSessionData.cancel(p,"Player changed dimension");
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){
        if(e.getEntity() instanceof ServerPlayer p)TradeCustody.claim(p);
    }
    @SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e){
        var data=net.goui.cosmicdungeon.economy.PlayerCurrencyData.get(e.getServer());
        if(data.cancelReservations("player_trade",System.currentTimeMillis(),"server restarted before trade commit")>0&&!data.flushVerified())
            throw new IllegalStateException("Trade startup cancellation needs save recovery");
    }
    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent e){
        for(var p:e.getServer().getPlayerList().getPlayers())if(TradeSessionData.get(p)!=null)TradeSessionData.cancel(p,"Server stopping");
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server == null || server.getTickCount() % 20 != 0) return;
        TradeSessionData.cleanupExpiredInvites(server);
    }
}
