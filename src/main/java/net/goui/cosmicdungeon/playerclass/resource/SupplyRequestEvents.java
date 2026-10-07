package net.goui.cosmicdungeon.playerclass.resource;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** One online pass per second; consent expires on logout/death/respawn and invalid run membership. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class SupplyRequestEvents {
    private SupplyRequestEvents(){}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(event.getServer().getTickCount()%20==0)SupplyRequests.syncAll(event.getServer());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p){SupplyTransfers.reconcile(p);SupplyRequests.forget(p);SupplyRequests.sync(p,true);}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p)SupplyRequests.forget(p);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){
        if(event.getEntity() instanceof ServerPlayer p){SupplyRequests.invalidate(p);SupplyTransfers.reconcile(p);SupplyRequests.sync(p,true);}
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){
        if(event.getEntity() instanceof ServerPlayer p)SupplyRequests.syncAll(p.level().getServer());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void protectPendingDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&SupplyTransfers.blocked(p)){
            event.setCanceled(true);p.setHealth(Math.max(1,p.getHealth()));
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void died(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer p){SupplyRequests.invalidate(p);SupplyRequests.sync(p,true);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void pickup(ItemEntityPickupEvent.Pre event){
        if(event.getPlayer() instanceof ServerPlayer p&&SupplyTransfers.blocked(p))event.setCanPickup(TriState.FALSE);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){SupplyRequests.stopped();SupplyTransfers.stopped();}
}
