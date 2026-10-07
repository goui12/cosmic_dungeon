package net.goui.cosmicdungeon.playerclass.resource;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** One bounded online-player pass per second; no world entity scans or per-tick disk/network I/O. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class ClassResourceEvents {
    private ClassResourceEvents(){}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();if(server.getTickCount()%20!=0)return;
        for(var player:server.getPlayerList().getPlayers())ClassResourceService.pulse(player,server.getTickCount(),true);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p){ClassResourceService.forget(p);ClassResourceService.pulse(p,p.level().getServer().getTickCount(),false);}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p)ClassResourceService.forget(p);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){
        if(event.getEntity() instanceof ServerPlayer p){ClassResourceService.forget(p);ClassResourceService.pulse(p,p.level().getServer().getTickCount(),false);}
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event){
        if(event.getEntity() instanceof ServerPlayer p)ClassResourceService.pulse(p,p.level().getServer().getTickCount(),false);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){ClassResourceService.stop();}
}
